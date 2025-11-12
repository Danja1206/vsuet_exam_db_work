package ru.vsuet.backend.model.dto

import java.time.LocalDateTime

data class SellerDto(
    val sellerId: Long,
    val companyName: String,
    val contactEmail: String,
    val contactPhone: String?,
    val taxIdentificationNumber: String,
    val registrationDate: LocalDateTime,
    val isActive: Boolean
)
