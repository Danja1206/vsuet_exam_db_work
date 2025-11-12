package ru.vsuet.backend.model.dto

import java.math.BigDecimal
import java.time.LocalDateTime

data class ProductDto(
    val productId: Long,
    val productName: String,
    val description: String?,
    val price: BigDecimal,
    val createdDate: LocalDateTime,
    val seller: SellerDto,
    val categoryId: CategoryDto
)
