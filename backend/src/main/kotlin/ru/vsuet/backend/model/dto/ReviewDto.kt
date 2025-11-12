package ru.vsuet.backend.model.dto

import java.time.LocalDateTime

data class ReviewDto(
    val reviewId: Long,
    val productId: Long,
    val customerId: Long,
    val rating: Int,
    val commentText: String? = null,
    val reviewDate: LocalDateTime,
    val isPublished: Boolean,
)