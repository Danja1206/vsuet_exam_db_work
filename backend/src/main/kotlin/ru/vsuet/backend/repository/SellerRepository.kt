package ru.vsuet.backend.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.vsuet.backend.model.Seller

@Repository
interface SellerRepository: JpaRepository<Seller, Long> {
//    fun findByIsActive(isActive: Boolean): List<Seller>
    fun findByCompanyNameContainingIgnoreCase(name: String): List<Seller>
}