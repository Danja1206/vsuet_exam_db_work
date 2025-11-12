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

)
