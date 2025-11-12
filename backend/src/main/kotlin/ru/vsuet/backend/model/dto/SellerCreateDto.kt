package ru.vsuet.backend.model.dto

data class SellerCreateDto(
    val companyName: String,
    val contactEmail: String,
    val contactPhone: String?,
    val taxIdentificationNumber: String,
    val isActive: Boolean = true
)
