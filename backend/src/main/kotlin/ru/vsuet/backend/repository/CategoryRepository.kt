package ru.vsuet.backend.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.vsuet.backend.model.Category

@Repository
interface CategoryRepository : JpaRepository<Category, Long>