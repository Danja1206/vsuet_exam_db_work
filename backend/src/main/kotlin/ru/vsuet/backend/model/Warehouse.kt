package ru.vsuet.backend.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "warehouse")
data class Warehouse(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val warehouseId: Long,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    val product: Product,

    val quantityInStock: Int,
    val lastRestockDate: LocalDateTime? = null,
    @Column(name = "is_active")

    var isActive: Boolean = true,
    @Column(name = "created_at")

    val createdAt: LocalDateTime? = null,


)
