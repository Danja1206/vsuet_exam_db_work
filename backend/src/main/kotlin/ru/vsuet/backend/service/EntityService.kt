package ru.vsuet.backend.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.vsuet.backend.model.*
import ru.vsuet.backend.model.dto.*
import ru.vsuet.backend.repository.*
import java.time.LocalDateTime

@Service
class EntityService(
    private val sellerRepository: SellerRepository,
    private val productRepository: ProductRepository,
    private val orderRepository: OrderRepository,
    private val customerRepository: CustomerRepository,
    private val categoryRepository: CategoryRepository,
    private val orderItemRepository: OrderItemRepository,
    private val reviewRepository: ReviewRepository,
    private val warehouseRepository: WarehouseRepository,
) {

    fun getAllSellers(): List<SellerDto> {
        return sellerRepository.findAll().map { entityToDto(it) }
    }

    @Transactional
    fun createSeller(sellerCreateDto: SellerCreateDto): SellerDto {
        val seller = Seller(
            companyName = sellerCreateDto.companyName,
            contactEmail = sellerCreateDto.contactEmail,
            contactPhone = sellerCreateDto.contactPhone,
            taxIdentificationNumber = sellerCreateDto.taxIdentificationNumber,
            isActive = sellerCreateDto.isActive
        )
        return entityToDto(sellerRepository.save(seller))
    }

    @Transactional
    fun updateSeller(id: Long, sellerCreateDto: SellerCreateDto): SellerDto {
        val existingSeller = sellerRepository.findById(id)
            .orElseThrow { RuntimeException("Продавец с id $id не найден") }

        val updatedSeller = existingSeller.copy(
            companyName = sellerCreateDto.companyName,
            contactEmail = sellerCreateDto.contactEmail,
            contactPhone = sellerCreateDto.contactPhone,
            taxIdentificationNumber = sellerCreateDto.taxIdentificationNumber,
            isActive = sellerCreateDto.isActive
        )

        return entityToDto(sellerRepository.save(updatedSeller))
    }

    @Transactional
    fun deleteSeller(id: Long) {
        if (!sellerRepository.existsById(id)) {
            throw RuntimeException("Продавец с id $id не найден")
        }
        val products = productRepository.findBySellerSellerId(id)
        if (products.isNotEmpty()) {
            throw RuntimeException("Ошибка удаления продавца с id $id причина: существует ассоциация с продуктом")
        }
        sellerRepository.deleteById(id)
    }

    fun getAllProducts(): List<ProductDto> {
        return productRepository.findAll().map { entityToDto(it) }
    }

    fun getAllCategories(): List<CategoryDto> =
        categoryRepository.findAll().map { entityToDto(it) }

    fun getOrderItems() : List<OrderItemDto> =
        orderItemRepository.findAll().map { entityToDto(it) }

    fun getReviews() : List<ReviewDto> =
        reviewRepository.findAll().map { entityToDto(it) }

    fun getWarehouses() : List<WarehouseDto> =
        warehouseRepository.findAll().map { entityToDto(it) }

    fun getCustomers() : List<CustomerDto> =
        customerRepository.findAll().map { entityToDto(it) }

    @Transactional
    fun createProduct(productCreateDto: ProductCreateDto): ProductDto {
        val seller = sellerRepository.findById(productCreateDto.sellerId)
            .orElseThrow { RuntimeException("Продавец с id ${productCreateDto.sellerId} не найден") }

        val category = categoryRepository.findById(productCreateDto.categoryId)
            .orElseThrow { RuntimeException("Категория с id ${productCreateDto.categoryId} не найдена") }

        val product = Product(
            productName = productCreateDto.productName,
            description = productCreateDto.description,
            price = productCreateDto.price,
            seller = seller,
            category = category
        )

        return entityToDto(productRepository.save(product))
    }

    @Transactional
    fun updateProduct(id: Long, product: Product): Product {
        val existingProduct = productRepository.findById(id)
            .orElseThrow { throw RuntimeException("Продукт с id $id не найден") }

        val seller = sellerRepository.findById(product.seller.sellerId)
            .orElseThrow { throw RuntimeException("Продавец с id ${product.seller.sellerId} не найден") }

        val category = categoryRepository.findById(product.category.categoryId)
            .orElseThrow { throw RuntimeException("Категория с id ${product.category.categoryId} не найдена") }

        return productRepository.save(existingProduct.copy(
            seller = seller,
            category = category,
            productName = product.productName,
            description = product.description,
            price = product.price
        ))
    }

    @Transactional
    fun deleteProduct(id: Long) {
        if (!productRepository.existsById(id)) {
            throw RuntimeException("Продукт с id $id не найден")
        }
        productRepository.deleteById(id)
    }

    fun getAllOrders(): List<OrderDto> {
        return orderRepository.findAll().map { entityToDto(it) }
    }

    @Transactional
    fun deleteOrder(id: Long) {
        if (!orderRepository.existsById(id)) {
            throw RuntimeException("Заказ с id $id не найден")
        }
        orderRepository.deleteById(id)
    }

    @Transactional
    fun deleteOrderItem(id: Long) {
        if (!orderItemRepository.existsById(id)) {
            throw RuntimeException("Товар заказа с id $id не найден")
        }
        orderItemRepository.deleteById(id)
    }

    @Transactional
    fun deleteReview(id: Long) {
        if (!reviewRepository.existsById(id)) {
            throw RuntimeException("Отзыв с id $id не найден")
        }
        reviewRepository.deleteById(id)
    }

    @Transactional
    fun deleteWarehouse(id: Long) {
        if (!warehouseRepository.existsById(id)) {
            throw RuntimeException("Складские остатки с id $id не найден")
        }
        warehouseRepository.deleteById(id)
    }

    @Transactional
    fun deleteCustomer(id: Long) {
        if (!customerRepository.existsById(id)) {
            throw RuntimeException("Складские остатки с id $id не найден")
        }
        customerRepository.deleteById(id)
    }

    private fun entityToDto(seller: Seller): SellerDto {
        return SellerDto(
            sellerId = seller.sellerId,
            companyName = seller.companyName,
            contactEmail = seller.contactEmail,
            contactPhone = seller.contactPhone,
            taxIdentificationNumber = seller.taxIdentificationNumber,
            registrationDate = seller.registrationDate,
            isActive = seller.isActive
        )
    }

    private fun entityToDto(product: Product): ProductDto {
        return ProductDto(
            productId = product.productId,
            productName = product.productName,
            description = product.description,
            price = product.price,
            createdDate = product.createdAt!!,
            seller = entityToDto(product.seller),
            categoryId = entityToDto(product.category)
        )
    }

    private fun entityToDto(order: Order): OrderDto {
        return OrderDto(
            orderId = order.orderId,
            orderDate = order.orderDate,
            status = order.status,
            totalAmount = order.totalAmount,
            shippingAddress = order.shippingAddress,
            customerId = order.customer.customerId
        )
    }

    private fun entityToDto(category: Category): CategoryDto {
        return CategoryDto(
            categoryId = category.categoryId,
            categoryName = category.categoryName,
            parentCategoryId = category.parentCategory?.categoryId
        )
    }

    private fun entityToDto(orderItem: OrderItem): OrderItemDto {
        return OrderItemDto(
            orderId = orderItem.orderItemId,
            productId = orderItem.product.productId,
            unitPrice = orderItem.unitPrice,
            quantity = orderItem.quantity,
            orderItemId = orderItem.orderItemId
        )
    }
    private fun entityToDto(review: Review): ReviewDto {
        return ReviewDto(
            reviewId = review.reviewId,
            productId = review.product.productId,
            customerId = review.customer.customerId,
            reviewDate = review.reviewDate,
            commentText = review.commentText,
            rating = review.rating,
            isPublished = review.isPublished,
        )
    }

    private fun entityToDto(warehouse: Warehouse): WarehouseDto {
        return WarehouseDto(
            productId = warehouse.product.productId,
            quantityInStock = warehouse.quantityInStock,
            lastRestockDate = warehouse.lastRestockDate,
            warehouseId = warehouse.warehouseId
        )
    }

    private fun entityToDto(customer: Customer): CustomerDto {
        return CustomerDto(
            customerId = customer.customerId,
            registrationDate = customer.registrationDate,
            address = customer.address,
            isActive = customer.isActive,
            email = customer.email,
            firstName = customer.firstName,
            phoneNumber = customer.phoneNumber,
            postalCode = customer.postalCode,
            city = customer.city,
            lastName = customer.lastName,
        )
    }
}