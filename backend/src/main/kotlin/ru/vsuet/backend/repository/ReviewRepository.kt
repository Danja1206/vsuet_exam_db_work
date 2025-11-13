package ru.vsuet.backend.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.vsuet.backend.model.Review

interface ReviewRepository : JpaRepository<Review, Long> {
    fun findByCustomerCustomerId(customerId: Long): List<Review>
}