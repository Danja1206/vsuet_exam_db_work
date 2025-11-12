package ru.vsuet.backend.model

import jakarta.persistence.*
import java.time.LocalDateTime


@Entity
@Table(name = "reviews")
data class Review(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val reviewId: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    val product: Product,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    val customer: Customer,

    val rating: Int,
    val commentText: String? = null,
    val reviewDate: LocalDateTime = LocalDateTime.now(),
    val isPublished: Boolean = true,
    @Column(name = "is_active")

    var isActive: Boolean = true,
    @Column(name = "created_at")

    val createdAt: LocalDateTime? = null,



) {
    init {
        require(rating in 1..5) { "Rating must be between 1 and 5" }
    }
}