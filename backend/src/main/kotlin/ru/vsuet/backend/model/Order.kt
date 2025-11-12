package ru.vsuet.backend.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "orders")
data class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val orderId: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    val customer: Customer,

    val orderDate: LocalDateTime = LocalDateTime.now(),
    val status: String = "Оформлен",
    val totalAmount: BigDecimal,
    val shippingAddress: String,
    @Column(name = "is_active")

    var isActive: Boolean = true,
    @Column(name = "created_at")

    val createdAt: LocalDateTime? = null,



    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    val items: MutableList<OrderItem> = mutableListOf()
)