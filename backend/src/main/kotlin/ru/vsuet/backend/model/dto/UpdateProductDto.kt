package ru.vsuet.backend.model.dto

import java.math.BigDecimal

data class UpdateProductDto (
    val categoryId : Long,
    val sellerId : Long,
    val productName: String,
    val description: String,
    val price: BigDecimal,
)