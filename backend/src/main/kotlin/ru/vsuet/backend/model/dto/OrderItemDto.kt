package ru.vsuet.backend.model.dto

import java.math.BigDecimal

data class OrderItemDto(
    val orderItemId: Long,
    val orderId : Long,
    val productId : Long,
    val quantity : Int,
    val unitPrice: BigDecimal
)
