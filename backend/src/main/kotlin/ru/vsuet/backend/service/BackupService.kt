package ru.vsuet.backend.service

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import ru.vsuet.backend.component.LiquibaseSchemaInitializer
import ru.vsuet.backend.model.dto.ArchiveRequest
import ru.vsuet.backend.model.dto.BackupRequest
import ru.vsuet.backend.model.dto.BackupFile
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class BackupService(
    @Value("\${spring.datasource.username}") private val dbUser: String,
    @Value("\${spring.datasource.password}") private val dbPassword: String,
    @Value("\${spring.datasource.url}") private val dbUrl: String,
    @Value("\${app.backup.path:/backups/}") private val backupPath: String,
    @Value("\${app.postgres.container-name}") private val containerName: String,
    @Value("\${app.postgres.port:5432}") private val containerPort: Int
) {

    private val dbName: String
        get() {
            val urlWithoutParams = dbUrl.substringBefore("?")
            return urlWithoutParams.substringAfterLast("/")
        }

    init {
        val dir = File(backupPath)
        if (!dir.exists()) dir.mkdirs()
    }

    fun createBackup(backupRequest: BackupRequest): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val backupFile = "$backupPath/backup_$timestamp.sql"

        val command = arrayOf(
            "pg_dump",
            "-h", containerName,
            "-p", containerPort.toString(),
            "-U", dbUser,
            "-F", "c",   // формат custom
            "-f", backupFile,
            dbName
        )

        val pb = ProcessBuilder(*command)
        pb.environment()["PGPASSWORD"] = dbPassword
        pb.inheritIO()

        val process = pb.start()
        val exitCode = process.waitFor()

        if (exitCode != 0) throw RuntimeException("Резервное копирование завершено с exit code $exitCode")

        return backupFile
    }

    fun restoreBackup(backupFileName: String): String {
        val fullPath = if (backupFileName.startsWith("/")) {
            backupFileName
        } else {
            "$backupPath/$backupFileName"
        }

        val command = arrayOf(
            "pg_restore",
            "-h", containerName,
            "-p", containerPort.toString(),
            "-U", dbUser,
            "-d", dbName,
            "-c", fullPath
        )

        val pb = ProcessBuilder(*command)
        pb.environment()["PGPASSWORD"] = dbPassword
        pb.inheritIO()

        val process = pb.start()
        val exitCode = process.waitFor()

        if (exitCode != 0) throw RuntimeException("Восстановление завершено с exit code $exitCode")

        return "База данных восстановлена успешно из: $fullPath"
    }

    fun listBackups(): List<BackupFile> {
        val dir = File(backupPath)
        if (!dir.exists()) return emptyList()

        return dir.listFiles { file -> file.name.startsWith("backup_") && file.name.endsWith(".sql") }
            ?.map { file ->
                BackupFile(
                    name = file.name,
                    size = file.length(),
                    modified = file.lastModified()
                )
            }
            ?.sortedByDescending { it.modified } ?: emptyList()
    }


    fun archiveOldData(archiveRequest: ArchiveRequest): String {
        if (!tableExists(archiveRequest.sourceTableName)) {
            throw IllegalArgumentException("Таблица ${archiveRequest.sourceTableName} не существует")
        }

        val tableColumns = getTableColumns(archiveRequest.sourceTableName)
        if (!tableColumns.containsKey("created_at")) {
            throw IllegalArgumentException("В таблице ${archiveRequest.sourceTableName} нет поля created_at — архивация невозможна")
        }

        val createArchiveTable = buildCreateArchiveTableSql(archiveRequest, tableColumns)
        val archiveSql = buildArchiveDataSql(archiveRequest, tableColumns)
        val deleteSql = buildDeleteOldDataSql(archiveRequest)

        runSqlInDocker(createArchiveTable)

        val archivedCount = countArchivableRecords(archiveRequest)
        if (archivedCount == 0) return "Нет данных старше ${archiveRequest.olderThanDays} дней для архивации"

        runSqlInDocker(archiveSql)
        runSqlInDocker(deleteSql)

        val commonColumns = tableColumns.keys.joinToString(", ") { it }
        val archiveSelectColumns = tableColumns.keys.joinToString(", ") {
            if (it.lowercase() == "id") "id_original" else it
        }

        val viewSql = """
CREATE OR REPLACE VIEW ${archiveRequest.sourceTableName}_with_archive AS
SELECT 
    ${
            tableColumns.keys.joinToString(", ") { col ->
                when (col.lowercase()) {
                    "id" -> "$col AS id_original"
                    else -> "$col"
                }
            }
        },
    NULL::timestamp AS archived_at,
    NULL::varchar AS archived_by
FROM ${archiveRequest.sourceTableName}
UNION ALL
SELECT 
    $archiveSelectColumns,
    archived_at,
    archived_by
FROM ${archiveRequest.archiveTableName};
""".trimIndent()


        runSqlInDocker(viewSql)

        return "Архивация $archivedCount записей из ${archiveRequest.sourceTableName} завершена (по created_at)"
    }


    private fun runSql(sql: String) {
        val tempFile = File.createTempFile("sql_", ".sql")
        tempFile.writeText(sql)

        val command = arrayOf(
            "psql",
            "-U", dbUser,
            "-d", dbName,
            "-f", tempFile.absolutePath
        )

        val pb = ProcessBuilder(*command)
        pb.environment()["PGPASSWORD"] = dbPassword
        pb.inheritIO()
        val process = pb.start()
        val exitCode = process.waitFor()
        tempFile.delete()

        if (exitCode != 0) throw RuntimeException("SQL выполнение завершено с exit code $exitCode")
    }

    private fun getDateColumnForTable(columns: Map<String, String>): String {
        val preferredNames = listOf(
            "created_at",
        )
        val lowerKeys = columns.keys.map { it.lowercase() }


        for (name in preferredNames) {
            val idx = lowerKeys.indexOf(name)
            if (idx >= 0) return columns.keys.elementAt(idx)
        }


        val byType = columns.entries.firstOrNull { entry ->
            val t = entry.value.lowercase()
            t.contains("timestamp") || t.contains("date") || t.contains("time")
        }
        if (byType != null) return byType.key


        throw IllegalArgumentException("Не удалось автоматически определить колонку с датой в таблице. Укажите колонку явно в ArchiveRequest.")
    }

    private fun runSqlInDocker(sql: String) {
        val tempFile = File.createTempFile("sql_", ".sql")
        tempFile.writeText(sql)

        try {
            val execCmd = arrayOf(
                "psql",
                "-h", containerName,
                "-p", containerPort.toString(),
                "-U", dbUser,
                "-d", dbName,
                "-v", "ON_ERROR_STOP=1",
                "-f", tempFile.absolutePath
            )

            val pb = ProcessBuilder(*execCmd)
            pb.environment()["PGPASSWORD"] = dbPassword
            pb.inheritIO()
            val process = pb.start()
            val exitCode = process.waitFor()

            if (exitCode != 0) {
                throw RuntimeException("Ошибка SQL (exit $exitCode)")
            }
        } finally {
            tempFile.delete()
        }
    }

    private fun getTableColumns(tableName: String): Map<String, String> {
        val sql = """
        SELECT column_name, data_type
        FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = '${tableName.lowercase()}'
        ORDER BY ordinal_position;
    """.trimIndent()

        val output = execSqlAndGetOutputInDocker(sql)

        return output.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line ->
                val parts = if (line.contains("|")) line.split('|') else line.split("\\s+".toRegex(), limit = 2)
                if (parts.size >= 2) parts[0].trim() to parts[1].trim() else null
            }
            .toMap()
    }

    private fun execSqlAndGetOutputInDocker(sql: String): String {
        val tempFile = File.createTempFile("sql_query_", ".sql")
        tempFile.writeText(sql)

        try {
            val execCmd = arrayOf(
                "psql",
                "-h", containerName,
                "-p", containerPort.toString(),
                "-U", dbUser,
                "-d", dbName,
                "-t", "-A", // -t: без заголовков, -A: машинно-читаемый формат
                "-f", tempFile.absolutePath,
                "-v", "ON_ERROR_STOP=1"
            )

            val pb = ProcessBuilder(*execCmd)
            pb.environment()["PGPASSWORD"] = dbPassword
            pb.redirectErrorStream(true)

            val process = pb.start()
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode != 0) {
                throw RuntimeException("SQL ошибка (exit $exitCode): $output")
            }

            return output
        } finally {
            tempFile.delete()
        }
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
    ${archiveColumns.joinToString(",\n    ")}
);
""".trimIndent()
    }


    private fun buildArchiveDataSql(archiveRequest: ArchiveRequest, columns: Map<String, String>): String {
        val columnNames = columns.keys.joinToString(", ")
        val archiveColumnNames = columns.keys.map {
            if (it.lowercase() == "id") "id_original" else it
        }.joinToString(", ")

        return """
INSERT INTO ${archiveRequest.archiveTableName} ($archiveColumnNames, archived_at, archived_by)
SELECT $columnNames, CURRENT_TIMESTAMP, 'system'
FROM ${archiveRequest.sourceTableName}
WHERE created_at < NOW() - INTERVAL '${archiveRequest.olderThanDays} days';
""".trimIndent()
    }


    private fun buildDeleteOldDataSql(archiveRequest: ArchiveRequest): String {
        return """
DELETE FROM ${archiveRequest.sourceTableName}
WHERE created_at < NOW() - INTERVAL '${archiveRequest.olderThanDays} days';
""".trimIndent()
    }


    private fun countArchivableRecords(archiveRequest: ArchiveRequest): Int {
        val sql = """
SELECT COUNT(*)
FROM ${archiveRequest.sourceTableName}
WHERE created_at < NOW() - INTERVAL '${archiveRequest.olderThanDays} days';
""".trimIndent()


        val output = execSqlAndGetOutputInDocker(sql).trim()
        return output.toIntOrNull() ?: 0
    }

    private fun execSqlAndGetOutput(sql: String): String {
        val tempFile = File.createTempFile("sql_query_", ".sql")
        tempFile.writeText(sql)

        val command = arrayOf(
            "psql",
            "-U", dbUser,
            "-d", dbName,
            "-t",
            "-f", tempFile.absolutePath
        )

        val pb = ProcessBuilder(*command)
        pb.environment()["PGPASSWORD"] = dbPassword
        val process = pb.start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        tempFile.delete()

        if (exitCode != 0) throw RuntimeException("Ошибка выполнения SQL: exit code $exitCode")
        return output
    }

    private fun tableExists(tableName: String): Boolean {
        val sql =
            "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = '${tableName.lowercase()}');"
        val output = execSqlAndGetOutputInDocker(sql).trim()
// psql возвращает 't' или 'f' либо 'true'/'false'
        return output.startsWith("t") || output.startsWith("true")
    }

    fun listTables(): List<String> {
        val sql = "SELECT tablename FROM pg_tables WHERE schemaname = 'public';"
        val output = execSqlAndGetOutputInDocker(sql)

        return output.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    companion object {
        private val logger: Logger = LogManager.getLogger(BackupService::class.java)
    }

}