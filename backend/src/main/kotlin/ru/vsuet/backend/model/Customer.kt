package ru.vsuet.backend.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "customers")
data class Customer(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val customerId: Long = 0,

    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String? = null,
    val registrationDate: LocalDateTime = LocalDateTime.now(),
    val address: String? = null,
    val city: String? = null,
    val postalCode: String? = null,
    val isActive: Boolean = true
)