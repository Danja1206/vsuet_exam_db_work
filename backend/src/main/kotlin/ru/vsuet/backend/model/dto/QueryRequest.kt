package ru.vsuet.backend.model.dto

data class QueryRequest(
    val tableName: String,
    val filters: Map<String, Any> = emptyMap(),
    val columns: List<String> = emptyList(),
    val sortBy: String? = null,
    val format: String? = null,
    val sortDirection: String = "ASC"
)

data class BackupRequest(
    val backupRequest: String? = null,
    val fileName: String? = null,
)

data class ArchiveRequest(
    val sourceTableName: String,
    val archiveTableName: String,
    val olderThanDays: Int
)
