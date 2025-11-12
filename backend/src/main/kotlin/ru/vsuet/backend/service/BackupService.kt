package ru.vsuet.backend.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import ru.vsuet.backend.model.dto.ArchiveRequest
import ru.vsuet.backend.model.dto.BackupRequest
import ru.vsuet.backend.model.dto.BackupFile
import java.io.File

@Service
class BackupService(
    @Value("\${spring.datasource.username}") private val dbUser: String,
    @Value("\${spring.datasource.password}") private val dbPassword: String,
    @Value("\${spring.datasource.url}") private val dbUrl: String,
    @Value("\${app.backup.path:/backups/}") private val backupPath: String,
    @Value("\${app.postgres.container-name}") private val containerName: String
) {

    private val dbName: String
        get() {
            val urlWithoutParams = dbUrl.substringBefore("?")
            return urlWithoutParams.substringAfterLast("/")
        }

    fun createBackup(backupRequest: BackupRequest): String {
        val timestamp = System.currentTimeMillis()
        val backupFile = "backup_$timestamp.sql"

        val command = arrayOf(
            "docker", "exec", containerName,
            "pg_dump", "-U", dbUser, "-F", "c", "-f", "/backup/$backupFile", dbName
        )

        val processBuilder = ProcessBuilder(*command)
        processBuilder.environment()["PGPASSWORD"] = dbPassword
        processBuilder.inheritIO()
        val process = processBuilder.start()
        val exitCode = process.waitFor()

        if (exitCode != 0) throw RuntimeException("Резервное копирование завершено с exit code $exitCode")

        return "Резервное копирование создано успешно"
    }

    fun restoreBackup(backupFile: String): String {
        val command = arrayOf(
            "docker", "exec", "-i", containerName,
            "pg_restore", "-U", dbUser, "-d", dbName, "-c", "/backup/$backupFile"
        )

        val processBuilder = ProcessBuilder(*command)
        processBuilder.environment()["PGPASSWORD"] = dbPassword
        processBuilder.inheritIO()
        val process = processBuilder.start()
        val exitCode = process.waitFor()

        if (exitCode != 0) throw RuntimeException("Восстановление завершено с exit code $exitCode")

        return "База данных восстановлена успешно из: $backupFile"
    }

    fun listBackups(): List<BackupFile> {
        val command = arrayOf(
            "docker", "exec", containerName,
            "find", "/backup", "-name", "backup_*.sql", "-type", "f", "-printf", "%f %s %T@\\n"
        )

        val process = ProcessBuilder(*command)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        if (exitCode != 0) return emptyList()

        return output.lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split(" ", limit = 3)
                if (parts.size < 3) return@mapNotNull null
                val name = parts[0]
                val size = parts[1].toLongOrNull() ?: return@mapNotNull null
                val modified = (parts[2].toDoubleOrNull() ?: return@mapNotNull null) * 1000 // convert to milliseconds
                BackupFile(name, size, modified.toLong())
            }
            .sortedByDescending { it.modified }
    }

    fun archiveOldData(archiveRequest: ArchiveRequest): String {
// Проверяем существование исходной таблицы
        if (!tableExists(archiveRequest.sourceTableName)) {
            throw IllegalArgumentException("Таблица ${archiveRequest.sourceTableName} не существует")
        }


// Получаем структуру исходной таблицы (column -> data_type)
        val tableColumns = getTableColumns(archiveRequest.sourceTableName)
        if (tableColumns.isEmpty()) {
            throw RuntimeException("Не удалось получить структуру таблицы ${archiveRequest.sourceTableName}")
        }


// Определяем колонку для фильтрации по дате автоматически
        val dateColumn = getDateColumnForTable(tableColumns)


// Создаем SQL для создания архивной таблицы
        val createArchiveTable = buildCreateArchiveTableSql(archiveRequest, tableColumns)


// Определяем SQL для переноса и удаления данных, используя найденную dateColumn
        val archiveSql = buildArchiveDataSql(archiveRequest, tableColumns, dateColumn)
        val deleteSql = buildDeleteOldDataSql(archiveRequest, dateColumn)


// Выполняем SQL команды
        runSqlInDocker(createArchiveTable)
        val archivedCount = countArchivableRecords(archiveRequest, dateColumn)
        runSqlInDocker(archiveSql)
        runSqlInDocker(deleteSql)


        return "Архивация $archivedCount записей из ${archiveRequest.sourceTableName} в ${archiveRequest.archiveTableName} завершена"
    }

    private fun getDateColumnForTable(columns: Map<String, String>): String {
// Популярные имена колонок с датой/временем
        val preferredNames = listOf(
            "order_date",
            "created_at",
            "created_on",
            "created",
            "createdate",
            "registration_date",
            "updated_at",
            "date",
            "timestamp"
        )
        val lowerKeys = columns.keys.map { it.lowercase() }


// 1) Поиск по имени
        for (name in preferredNames) {
            val idx = lowerKeys.indexOf(name)
            if (idx >= 0) return columns.keys.elementAt(idx)
        }


// 2) Поиск по типу колонки (timestamp/date)
        val byType = columns.entries.firstOrNull { entry ->
            val t = entry.value.lowercase()
            t.contains("timestamp") || t.contains("date") || t.contains("time")
        }
        if (byType != null) return byType.key


        throw IllegalArgumentException("Не удалось автоматически определить колонку с датой в таблице. Укажите колонку явно в ArchiveRequest.")
    }

    private fun runSqlInDocker(sql: String): Int {
        val tempFile = File.createTempFile("sql_", ".sql")
        tempFile.writeText(sql)


        val copyCommand = arrayOf(
            "docker", "cp", tempFile.absolutePath, "$containerName:/tmp/${tempFile.name}"
        )
        ProcessBuilder(*copyCommand).inheritIO().start().waitFor()


        val command = arrayOf(
            "docker", "exec", "-i", containerName,
            "psql", "-U", dbUser, "-d", dbName, "-f", "/tmp/${tempFile.name}"
        )


        val processBuilder = ProcessBuilder(*command)
        processBuilder.environment()["PGPASSWORD"] = dbPassword
        processBuilder.inheritIO()
        val process = processBuilder.start()
        val exitCode = process.waitFor()
        tempFile.delete()


        if (exitCode != 0) throw RuntimeException("SQL выполнение завершено с exit code $exitCode")
        return exitCode
    }

    private fun getTableColumns(tableName: String): Map<String, String> {
        val sql = """
        SELECT column_name, data_type
        FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = '${tableName.lowercase()}'
        ORDER BY ordinal_position;
    """.trimIndent()

        val output = execSqlAndGetOutput(sql)

        return output.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line ->
                val parts = if (line.contains("|")) line.split('|') else line.split("\\s+".toRegex(), limit = 2)
                if (parts.size >= 2) parts[0].trim() to parts[1].trim() else null
            }
            .toMap()
    }


    private fun buildCreateArchiveTableSql(archiveRequest: ArchiveRequest, columns: Map<String, String>): String {
        val archiveColumns = columns.map { (name, type) ->
            when (name.lowercase()) {
                "id" -> "${name}_original $type"
                else -> "$name $type"
            }
        }.toMutableList()


        archiveColumns.add("archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
        archiveColumns.add("archived_by VARCHAR(100) DEFAULT 'system'")


        return """
CREATE TABLE IF NOT EXISTS ${archiveRequest.archiveTableName} (
archive_id BIGSERIAL PRIMARY KEY,
${archiveColumns.joinToString(",\n ")}
);
""".trimIndent()
    }


    private fun buildArchiveDataSql(
        archiveRequest: ArchiveRequest,
        columns: Map<String, String>,
        dateColumn: String
    ): String {
        val columnNames = columns.keys.joinToString(", ")
        val archiveColumnNames = columns.keys.map {
            if (it.lowercase() == "id") "id_original" else it
        }.joinToString(", ")


        return """
INSERT INTO ${archiveRequest.archiveTableName} ($archiveColumnNames, archived_at, archived_by)
SELECT $columnNames, CURRENT_TIMESTAMP, 'system'
FROM ${archiveRequest.sourceTableName}
WHERE "$dateColumn" < NOW() - INTERVAL '${archiveRequest.olderThanDays} days';
""".trimIndent()
    }


    private fun buildDeleteOldDataSql(archiveRequest: ArchiveRequest, dateColumn: String): String {
        return """
DELETE FROM ${archiveRequest.sourceTableName}
WHERE "$dateColumn" < NOW() - INTERVAL '${archiveRequest.olderThanDays} days';
""".trimIndent()
    }


    private fun countArchivableRecords(archiveRequest: ArchiveRequest, dateColumn: String): Int {
        val sql = """
SELECT COUNT(*)
FROM ${archiveRequest.sourceTableName}
WHERE "$dateColumn" < NOW() - INTERVAL '${archiveRequest.olderThanDays} days';
""".trimIndent()


        val output = execSqlAndGetOutput(sql).trim()
        return output.toIntOrNull() ?: 0
    }

    private fun execSqlAndGetOutput(sql: String): String {
        val tempFile = File.createTempFile("sql_query_", ".sql")
        tempFile.writeText(sql)


        val copyCommand = arrayOf(
            "docker", "cp", tempFile.absolutePath, "$containerName:/tmp/${tempFile.name}"
        )
        ProcessBuilder(*copyCommand).inheritIO().start().waitFor()


        val command = arrayOf(
            "docker", "exec", "-i", containerName,
            "psql", "-U", dbUser, "-d", dbName, "-t", "-f", "/tmp/${tempFile.name}"
        )


        val processBuilder = ProcessBuilder(*command)
        processBuilder.environment()["PGPASSWORD"] = dbPassword
        val process = processBuilder.start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        tempFile.delete()


        if (exitCode != 0) throw RuntimeException("Ошибка выполнения SQL: exit code $exitCode")
        return output
    }

    private fun tableExists(tableName: String): Boolean {
        val sql =
            "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = '${tableName.lowercase()}');"
        val output = execSqlAndGetOutput(sql).trim()
// psql возвращает 't' или 'f' либо 'true'/'false'
        return output.startsWith("t") || output.startsWith("true")
    }

    fun listTables(): List<String> {
        val sql = "SELECT tablename FROM pg_tables WHERE schemaname = 'public';"
        val tempFile = File.createTempFile("tables_", ".sql")
        tempFile.writeText(sql)


        val copyCommand = arrayOf(
            "docker", "cp", tempFile.absolutePath, "$containerName:/tmp/${tempFile.name}"
        )
        ProcessBuilder(*copyCommand).inheritIO().start().waitFor()


        val command = arrayOf(
            "docker", "exec", "-i", containerName,
            "psql", "-U", dbUser, "-d", dbName, "-t", "-f", "/tmp/${tempFile.name}"
        )


        val processBuilder = ProcessBuilder(*command)
        processBuilder.environment()["PGPASSWORD"] = dbPassword
        val process = processBuilder.start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        tempFile.delete()


        if (exitCode != 0) throw RuntimeException("Ошибка получения таблиц")


        return output.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

}