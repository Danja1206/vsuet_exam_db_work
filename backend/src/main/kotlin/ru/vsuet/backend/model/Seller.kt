package ru.vsuet.backend.model

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "sellers")
data class Seller(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val sellerId: Long = 0,

    val companyName: String,
    val contactEmail: String,
    val contactPhone: String? = null,
    val taxIdentificationNumber: String,
    val registrationDate: LocalDateTime = LocalDateTime.now(),
    @Column(name = "is_active")
    var isActive: Boolean = true,
    @Column(name = "created_at")

    val createdAt: LocalDateTime? = null,

)
