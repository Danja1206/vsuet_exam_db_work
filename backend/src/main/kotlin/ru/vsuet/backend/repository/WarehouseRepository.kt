package ru.vsuet.backend.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.vsuet.backend.model.Product
import ru.vsuet.backend.model.Warehouse

interface WarehouseRepository : JpaRepository<Warehouse, Long> {
    fun findByproductProductId(id: Long): List<Warehouse>
}