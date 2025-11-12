package ru.vsuet.backend.model.dto

data class QueryResultResponse(
    val success: Boolean = true,
    val data: MutableList<Map<String, Any?>>? = null,
    val error: String? = null,
    val timestamp: String = java.time.LocalDateTime.now().toString()
)