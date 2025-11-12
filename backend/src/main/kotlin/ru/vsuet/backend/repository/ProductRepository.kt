package ru.vsuet.backend.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.vsuet.backend.model.Product
import java.math.BigDecimal

interface ProductRepository: JpaRepository<Product, Long> {

    fun findBySellerSellerId(sellerId: Long): List<Product>
    fun findByPriceBetween(minPrice: BigDecimal, maxPrice: BigDecimal): List<Product>

    @Query("SELECT p FROM Product p WHERE p.productName LIKE %:name%")
    fun searchByName(@Param("name") name: String): List<Product>
}