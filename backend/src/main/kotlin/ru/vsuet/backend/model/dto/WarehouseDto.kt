package ru.vsuet.backend.model.dto

import java.time.LocalDateTime

data class WarehouseDto(
    val warehouseId: Long,
    val productId: Long,
    val quantityInStock: Int,
    val lastRestockDate: LocalDateTime?
)
