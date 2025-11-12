package ru.vsuet.backend.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "order_items")
data class OrderItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val orderItemId: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    val order: Order,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    val product: Product,

    val quantity: Int,
    @Column(name = "is_active")

    var isActive: Boolean = true,
    @Column(name = "created_at")

    val createdAt: LocalDateTime? = null,

    val unitPrice: BigDecimal

)