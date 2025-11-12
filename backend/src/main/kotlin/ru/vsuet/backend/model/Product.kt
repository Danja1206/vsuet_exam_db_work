package ru.vsuet.backend.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "products")
data class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val productId: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    val seller: Seller,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    val category: Category,

    val productName: String,
    val description: String? = null,
    val price: BigDecimal,

    @Column(name = "is_active")
    var isActive: Boolean = true,
    @Column(name = "created_at")

    val createdAt: LocalDateTime? = null,
)