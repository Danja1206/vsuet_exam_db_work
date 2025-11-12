package ru.vsuet.backend.model.dto

data class CategoryDto(
    val categoryId: Long,
    val categoryName: String,
    val parentCategoryId: Long?
)