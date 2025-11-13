package ru.vsuet.backend.service

import jakarta.persistence.EntityManager
import jakarta.persistence.Table
import jakarta.persistence.criteria.*
import jakarta.persistence.metamodel.EntityType
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import ru.vsuet.backend.model.dto.QueryRequest
import java.io.ByteArrayOutputStream
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException

@Service
class DatabaseService(
    private val entityManager: EntityManager,
    private val jdbcTemplate: JdbcTemplate
) {

    fun executeSQLQuery(sqlQuery: String): MutableList<Map<String, Any?>> {
        validateSQLQuery(sqlQuery)
        return executeSafeSQLQuery(sqlQuery)
    }

    private fun executeSafeSQLQuery(sql: String): MutableList<Map<String, Any?>> {
        return jdbcTemplate.query(sql) { rs, _ ->
            extractRow(rs)
        }
    }

    private fun extractRow(rs: ResultSet): Map<String, Any?> {
        val metaData = rs.metaData
        val columnCount = metaData.columnCount
        val row = mutableMapOf<String, Any?>()

        for (i in 1..columnCount) {
            val columnName = metaData.getColumnLabel(i)
            val value = getObject(rs, i)
            row[columnName] = value
        }

        return row.filterValues { it != null }
    }

    private fun getObject(rs: ResultSet, columnIndex: Int): Any? {
        return try {
            val value = rs.getObject(columnIndex)
            when {
                value == null -> null
                value is java.sql.Timestamp -> value.toLocalDateTime().toString()
                value is java.sql.Date -> value.toLocalDate().toString()
                value is Number || value is Boolean || value is String -> value
                else -> value.toString()
            }
        } catch (e: SQLException) {
            null
        }
    }

    private fun validateSQLQuery(sql: String) {
        val normalized = sql.trim().lowercase()

        if (!normalized.startsWith("select")) {
            throw IllegalArgumentException("Только SELECT запросы разрешены")
        }

        val forbiddenCommands = listOf(
            "insert", "update", "delete", "drop", "create", "alter", "truncate",
            "grant", "revoke", "commit", "rollback", "savepoint", "exec",
            "execute", "xp_", "sp_", "dbms_", "declare", "set", "use", "backup"
        )

        forbiddenCommands.forEach { cmd ->
            if (normalized.contains(" $cmd ") || normalized.endsWith(" $cmd") ||
                normalized.startsWith("$cmd ") || normalized == cmd) {
                throw IllegalArgumentException("Запрещенная команда: $cmd")
            }
        }

        val dangerousPatterns = listOf(";", "--", "/*", "*/", "@@", "'", "\"", "char(", "chr(")
        dangerousPatterns.forEach { pattern ->
            if (normalized.contains(pattern)) {
                throw IllegalArgumentException("Запрос содержит потенциально опасные символы: $pattern")
            }
        }

        if (sql.length > 500) {
            throw IllegalArgumentException("Запрос слишком длинный (максимум 500 символов)")
        }

        val systemTables = listOf("information_schema", "sys.", "pg_catalog", "sqlite_master")
        systemTables.forEach { table ->
            if (normalized.contains(table)) {
                throw IllegalArgumentException("Доступ к системным таблицам запрещен")
            }
        }
    }

    private fun executeSafeSQLQuery(connection: Connection, sql: String): List<Map<String, Any>> {
        val results = mutableListOf<Map<String, Any>>()

        connection.createStatement().use { stmt ->
            stmt.queryTimeout = 30

            val resultSet = stmt.executeQuery(sql)
            val metaData = resultSet.metaData
            val columnCount = metaData.columnCount
            val columnNames = (1..columnCount).map {
                metaData.getColumnLabel(it).toString()
            }

            while (resultSet.next()) {
                val row = mutableMapOf<String, Any>()
                for (i in 1..columnCount) {
                    val value = resultSet.getObject(i)
                    row[columnNames[i - 1]] = when {
                        value == null -> null
                        value is java.sql.Timestamp -> value.toLocalDateTime().toString()
                        value is java.sql.Date -> value.toLocalDate().toString()
                        value is Number || value is Boolean || value is String -> value
                        else -> value.toString()
                    } as Any
                }
                results.add(row)
            }
        }

        return results
    }

    fun executeQuery(queryRequest: QueryRequest): List<Map<String, Any>> {
        val tableName = queryRequest.tableName
        val entity = findEntityByTableName(tableName)
            ?: throw IllegalArgumentException("Entity for table $tableName not found")

        val entityJavaType = entity.javaType
        val columns = if (queryRequest.columns.isEmpty()) {
            entity.attributes.map { it.name }
        } else queryRequest.columns

        val cb = entityManager.criteriaBuilder
        val cq: CriteriaQuery<Array<Any>> = cb.createQuery(Array<Any>::class.java)
        val root = cq.from(entityJavaType)

        val selections = columns.mapNotNull {
            try {
                root.get<Any>(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
        cq.multiselect(selections)

        if (queryRequest.filters.isNotEmpty()) {
            val predicates = queryRequest.filters.map { (key, value) ->
                val (field, operator) = parseFilterKey(key)
                val path = getPath<Any>(root, field)
                createPredicate(cb, path, operator, value)
            }.toTypedArray()
            cq.where(cb.and(*predicates))
        }

        if (!queryRequest.sortBy.isNullOrBlank()) {
            val sortPath = root.get<Any>(queryRequest.sortBy)
            val sortOrder = if (queryRequest.sortDirection.equals("DESC", ignoreCase = true))
                cb.desc(sortPath)
            else cb.asc(sortPath)
            cq.orderBy(sortOrder)
        }

        val query = entityManager.createQuery(cq)
        val resultList = query.resultList

        return resultList.map { row ->
            val values = if (row is Array<*>) row else arrayOf(row)
            columns.zip(values.map { v ->
                when (v) {
                    null -> ""
                    is Number, is Boolean, is String -> v
                    is java.time.LocalDateTime -> v.toString()
                    is java.time.LocalDate -> v.toString()
                    else -> {
                        try {
                            val idField = v.javaClass.declaredFields.firstOrNull { it.name.endsWith("Id", true) }
                            idField?.let {
                                it.isAccessible = true
                                it.get(v)?.toString() ?: v.toString()
                            } ?: v.toString()
                        } catch (_: Exception) {
                            v.toString()
                        }
                    }
                }
            }).toMap()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> getPath(root: Root<*>, pathString: String): Path<T> {
        val parts = pathString.split(".")
        var path: Path<*> = root
        var join: From<*, *>? = null

        for (i in parts.indices) {
            val part = parts[i]
            if (i == 0) {
                path = root.get<Any>(part)
            } else {
                if (path is From<*, *>) {
                    join = path
                }
                path = (join ?: path as Path<Any>).get<Any>(part)
            }
        }

        return path as Path<T>
    }

    @Suppress("UNCHECKED_CAST")
    private fun createPredicate(cb: CriteriaBuilder, path: Path<*>, operator: String, value: Any): Predicate {
        return when (operator) {
            "=" -> cb.equal(path, value)
            "!=" -> cb.notEqual(path, value)
            ">" -> cb.greaterThan(path as Path<Comparable<Any>>, value as Comparable<Any>)
            "<" -> cb.lessThan(path as Path<Comparable<Any>>, value as Comparable<Any>)
            ">=" -> cb.greaterThanOrEqualTo(path as Path<Comparable<Any>>, value as Comparable<Any>)
            "<=" -> cb.lessThanOrEqualTo(path as Path<Comparable<Any>>, value as Comparable<Any>)
            "LIKE" -> {
                if (path.javaType != String::class.java) {
                    throw IllegalArgumentException("Оператор LIKE используется только для строковых данных")
                }
                cb.like(path as Path<String>, "%${value.toString()}%")
            }
            else -> throw IllegalArgumentException("Неподдерживаемая операция: $operator")
        }
    }

    private fun parseFilterKey(key: String): Pair<String, String> {
        val operators = listOf(">=", "<=", "!=", "=", ">", "<", "LIKE")
        for (op in operators) {
            if (key.endsWith(op)) {
                return key.removeSuffix(op) to op
            }
        }
        return key to "="
    }

    fun exportToExcel(queryRequest: QueryRequest): ByteArray {
        val data = executeQuery(queryRequest)
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Data")

        if (data.isNotEmpty()) {
            val headers = data[0].keys.toList()
            val headerRow = sheet.createRow(0)
            headers.forEachIndexed { i, h -> headerRow.createCell(i).setCellValue(h) }

            data.forEachIndexed { rowIndex, row ->
                val rowExcel = sheet.createRow(rowIndex + 1)
                headers.forEachIndexed { colIndex, header ->
                    val value = row[header]
                    val cell = rowExcel.createCell(colIndex)
                    when (value) {
                        is Number -> cell.setCellValue(value.toDouble())
                        is Boolean -> cell.setCellValue(value)
                        else -> cell.setCellValue(value?.toString() ?: "")
                    }
                }
            }
            headers.indices.forEach { sheet.autoSizeColumn(it) }
        }

        val out = ByteArrayOutputStream()
        workbook.use { it.write(out) }
        return out.toByteArray()
    }

    fun exportToCsv(queryRequest: QueryRequest): ByteArray {
        val data = executeQuery(queryRequest)
        val out = ByteArrayOutputStream()

        if (data.isNotEmpty()) {
            val headers = data[0].keys.toList()
            val writer = out.bufferedWriter(Charsets.UTF_8)

            writer.appendLine(headers.joinToString(","))

            data.forEach { row ->
                val line = headers.joinToString(",") { h ->
                    val value = row[h]?.toString()?.replace("\n", " ") ?: ""
                    "\"${value.replace("\"", "\"\"")}\""
                }
                writer.appendLine(line)
            }

            writer.flush()
        }

        return out.toByteArray()
    }

    fun getTableColumns(tableName: String): List<String> {
        val entity = findEntityByTableName(tableName)
            ?: throw IllegalArgumentException("Entity for table $tableName not found")

        val columns = mutableListOf<String>()

        entity.attributes.forEach { attr ->
            val attrName = attr.name
            val javaType = attr.javaType

            if (isBasicType(javaType)) {
                columns.add(attrName)
            } else {
                val relatedEntity = entityManager.metamodel.entities
                    .firstOrNull { it.javaType == javaType }

                if (relatedEntity != null) {
                    val relatedColumns = relatedEntity.attributes
                        .map { "${attrName}.${it.name}" }
                    columns.addAll(relatedColumns)
                } else {
                    columns.add(attrName)
                }
            }
        }

        return columns
    }

    private fun isBasicType(javaType: Class<*>): Boolean {
        return javaType.isPrimitive ||
                javaType == String::class.java ||
                Number::class.java.isAssignableFrom(javaType) ||
                javaType == Boolean::class.java ||
                javaType == java.time.LocalDate::class.java ||
                javaType == java.time.LocalDateTime::class.java
    }

    private fun findEntityByTableName(tableName: String): EntityType<*>? {
        return findEntityByTableAnnotation(tableName) ?: findEntityByConvention(tableName)
    }

    private fun findEntityByTableAnnotation(tableName: String): EntityType<*>? {
        val entities = entityManager.metamodel.entities
        return entities.firstOrNull { entity ->
            val table = getTableAnnotation(entity)
            table?.name?.equals(tableName, ignoreCase = true) ?: false
        }
    }

    private fun findEntityByConvention(tableName: String): EntityType<*>? {
        val entities = entityManager.metamodel.entities
        val normalized = normalizeEntityName(tableName)
        return entities.firstOrNull {
            it.name.equals(normalized, true) ||
                    it.name.contains(normalized, true) ||
                    normalized.contains(it.name, true)
        }
    }

    private fun getTableAnnotation(entity: EntityType<*>): Table? {
        return try {
            AnnotatedElementUtils.findMergedAnnotation(entity.javaType, Table::class.java)
        } catch (_: Exception) {
            null
        }
    }

    private fun normalizeEntityName(name: String): String {
        var n = name.substringAfter(".").trim()
        n = when {
            n.endsWith("ies", true) -> n.dropLast(3) + "y"
            n.endsWith("ses", true) || n.endsWith("xes", true) || n.endsWith("zes", true) -> n.dropLast(2)
            n.endsWith("s", true) && !n.endsWith("ss", true) -> n.dropLast(1)
            else -> n
        }
        n = n.split("_").joinToString("") { it.replaceFirstChar(Char::uppercase) }
        return n.replaceFirstChar(Char::uppercase)
    }

    fun exportSqlToExcel(sql: String): ByteArray {
        val data = executeSQLQueryRaw(sql)
        return createExcelFile(data)
    }

    fun exportSqlToCsv(sql: String): ByteArray {
        val data = executeSQLQueryRaw(sql)
        return createCsvFile(data)
    }

    private fun executeSQLQueryRaw(sql: String): List<Map<String, Any?>> {
        validateSQLQuery(sql)
        return jdbcTemplate.query(sql) { rs, _ ->
            extractRowRaw(rs)
        }
    }

    private fun extractRowRaw(rs: ResultSet): Map<String, Any?> {
        val metaData = rs.metaData
        val columnCount = metaData.columnCount
        val row = mutableMapOf<String, Any?>()

        for (i in 1..columnCount) {
            val columnName = metaData.getColumnLabel(i)
            val value = safeGetObject(rs, i)
            row[columnName] = value
        }

        return row
    }


    private fun safeGetObject(rs: ResultSet, columnIndex: Int): Any? {
        return try {
            val value = rs.getObject(columnIndex)
            when {
                value == null -> null
                value is java.sql.Timestamp -> value.toLocalDateTime()
                value is java.sql.Date -> value.toLocalDate()
                value is java.time.LocalDateTime -> value
                value is java.time.LocalDate -> value
                value is Number -> value
                value is Boolean -> value
                else -> value.toString()
            }
        } catch (e: SQLException) {
            "ERROR: ${e.message}"
        }
    }

    private fun createExcelFile(data: List<Map<String, Any?>>): ByteArray {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Export Data")

        if (data.isNotEmpty()) {
            val headers = data[0].keys.toList()
            val headerRow = sheet.createRow(0)

            headers.forEachIndexed { i, header ->
                headerRow.createCell(i).setCellValue(header)
            }

            data.forEachIndexed { rowIndex, row ->
                val excelRow = sheet.createRow(rowIndex + 1)
                headers.forEachIndexed { colIndex, header ->
                    val cell = excelRow.createCell(colIndex)
                    val value = row[header]

                    when (value) {
                        null -> cell.setCellValue("")
                        is Number -> cell.setCellValue(value.toDouble())
                        is Boolean -> cell.setCellValue(value)
                        is java.time.LocalDateTime -> cell.setCellValue(value.toString())
                        is java.time.LocalDate -> cell.setCellValue(value.toString())
                        else -> cell.setCellValue(value.toString())
                    }
                }
            }

            headers.indices.forEach { sheet.autoSizeColumn(it) }
        }

        ByteArrayOutputStream().use { out ->
            workbook.use { it.write(out) }
            return out.toByteArray()
        }
    }

    private fun createCsvFile(data: List<Map<String, Any?>>): ByteArray {
        val out = ByteArrayOutputStream()

        out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
        if (data.isNotEmpty()) {
            val headers = data[0].keys.toList()
            val writer = out.bufferedWriter(Charsets.UTF_8)

            // Записываем заголовки
            writer.appendLine(headers.joinToString(",") { escapeCsvValue(it) })

            // Записываем данные
            data.forEach { row ->
                val line = headers.joinToString(",") { header ->
                    escapeCsvValue(row[header]?.toString() ?: "")
                }
                writer.appendLine(line)
            }

            writer.flush()
        }

        return out.toByteArray()
    }

    private fun escapeCsvValue(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
