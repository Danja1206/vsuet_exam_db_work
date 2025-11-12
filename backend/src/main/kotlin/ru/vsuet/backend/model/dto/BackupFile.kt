package ru.vsuet.backend.model.dto

data class BackupFile(
    val name: String,
    val size: Long,
    val modified: Long
)