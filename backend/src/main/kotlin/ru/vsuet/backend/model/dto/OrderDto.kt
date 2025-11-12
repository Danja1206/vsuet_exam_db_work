package ru.vsuet.backend.model.dto

import java.math.BigDecimal
import java.time.LocalDateTime

data class OrderDto(
    val orderId: Long,
    val orderDate: LocalDateTime,
    val status: String,
    val totalAmount: BigDecimal,
    val shippingAddress: String,
    val customerId: Long
)