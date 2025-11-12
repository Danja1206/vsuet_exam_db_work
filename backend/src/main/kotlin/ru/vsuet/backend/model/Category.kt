package ru.vsuet.backend.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "categories")
data class Category(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val categoryId: Long = 0,

    val categoryName: String,
    @Column(name = "is_active")

    var isActive: Boolean = true,
    @Column(name = "created_at")

    val createdAt: LocalDateTime? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    var parentCategory: Category? = null
)