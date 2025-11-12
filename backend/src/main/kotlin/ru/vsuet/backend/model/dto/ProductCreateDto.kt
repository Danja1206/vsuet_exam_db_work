package ru.vsuet.backend.model.dto

import java.math.BigDecimal

data class ProductCreateDto(
    val productName: String,
    val description: String?,
    val price: BigDecimal,
    val sellerId: Long,
    val categoryId: Long
)