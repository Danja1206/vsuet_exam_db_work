package ru.vsuet.backend.model.dto

import java.time.LocalDateTime

data class CustomerDto(
    val customerId: Long,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String?,
    val registrationDate: LocalDateTime = LocalDateTime.now(),
    val address: String?,
    val city: String?,
    val postalCode: String?,
    val isActive: Boolean
)
