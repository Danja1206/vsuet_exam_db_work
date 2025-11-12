package ru.vsuet.backend.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.vsuet.backend.model.OrderItem

interface OrderItemRepository : JpaRepository<OrderItem, Long> {
}