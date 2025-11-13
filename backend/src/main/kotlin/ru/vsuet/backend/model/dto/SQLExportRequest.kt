package ru.vsuet.backend.model.dto

data class SQLExportRequest(
    val format: String,
    val query: String,
)
