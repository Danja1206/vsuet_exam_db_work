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
            isActive = sellerCreateDto.isActive,
            createdAt = LocalDateTime.now()
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
    fun deleteSeller(id: Long): String {
        if (!sellerRepository.existsById(id)) {
            throw RuntimeException("Продавец с id $id не найден")
        }
        val products = productRepository.findBySellerSellerId(id)
        if (products.isNotEmpty()) {
            throw RuntimeException("Нельзя удалить продавца с id $id: существуют связанные продукты")
        }
        sellerRepository.deleteById(id)
        return "Продавец успешно удален"
    }

    fun getAllProducts(): List<ProductDto> {
        return productRepository.findAll().filter { it.isActive }.map { entityToDto(it) }
    }

    fun getAllCategories(): List<CategoryDto> =
        categoryRepository.findAll().filter { it.isActive }.map { entityToDto(it) }

    fun getOrderItems() : List<OrderItemDto> =
        orderItemRepository.findAll().filter { it.isActive }.map { entityToDto(it) }

    fun getReviews() : List<ReviewDto> =
        reviewRepository.findAll().filter { it.isActive }.map { entityToDto(it) }

    fun getWarehouses() : List<WarehouseDto> =
        warehouseRepository.findAll().filter { it.isActive }.map { entityToDto(it) }

    fun getCustomers() : List<CustomerDto> =
        customerRepository.findAll().filter { it.isActive }.map { entityToDto(it) }

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
            category = category,
            createdAt = LocalDateTime.now()
        )

        return entityToDto(productRepository.save(product))
    }

    @Transactional
    fun updateProduct(id: Long, product: UpdateProductDto): ProductDto {
        val existingProduct = productRepository.findById(id)
            .orElseThrow { throw RuntimeException("Продукт с id $id не найден") }

        val seller = sellerRepository.findById(product.sellerId)
            .orElseThrow { throw RuntimeException("Продавец с id ${product.sellerId} не найден") }

        val category = categoryRepository.findById(product.categoryId)
            .orElseThrow { throw RuntimeException("Категория с id ${product.categoryId} не найдена") }

        return entityToDto(productRepository.save(existingProduct.copy(
            seller = seller,
            category = category,
            productName = product.productName,
            description = product.description,
            price = product.price
        )))
    }

    @Transactional
    fun deleteProduct(id: Long): String {
        if (!productRepository.existsById(id)) {
            throw RuntimeException("Продукт с id $id не найден")
        }

        val warehouses = warehouseRepository.findByproductProductId(id)
        if (warehouses.isNotEmpty()) {
            throw RuntimeException("Нельзя удалить продукт с id $id: существуют связанные складские остатки")
        }

        productRepository.deleteById(id)
        return "Товар успешно удален"
    }

    fun getAllOrders(): List<OrderDto> {
        return orderRepository.findAll().map { entityToDto(it) }
    }

    @Transactional
    fun deleteOrder(id: Long): String {
        if (!orderRepository.existsById(id)) {
            throw RuntimeException("Заказ с id $id не найден")
        }

        val orderItems = orderItemRepository.findByOrderOrderId(id)
        if (orderItems.isNotEmpty()) {
            throw RuntimeException("Нельзя удалить заказ с id $id: существуют связанные товары заказа")
        }

        orderRepository.deleteById(id)
        return "Заказ успешно удален"
    }

    @Transactional
    fun deleteOrderItem(id: Long): String {
        if (!orderItemRepository.existsById(id)) {
            throw RuntimeException("Товар заказа с id $id не найден")
        }
        orderItemRepository.deleteById(id)
        return "Товар заказа успешно удален"
    }

    @Transactional
    fun deleteReview(id: Long): String {
        if (!reviewRepository.existsById(id)) {
            throw RuntimeException("Отзыв с id $id не найден")
        }
        reviewRepository.deleteById(id)
        return "Отзыв успешно удален"
    }

    @Transactional
    fun deleteWarehouse(id: Long): String {
        if (!warehouseRepository.existsById(id)) {
            throw RuntimeException("Складские остатки с id $id не найден")
        }
        warehouseRepository.deleteById(id)
        return "Складские остатки успешно удалены"
    }

    @Transactional
    fun deleteCustomer(id: Long): String {
        if (!customerRepository.existsById(id)) {
            throw RuntimeException("Клиент с id $id не найден")
        }

        // Проверка на связанные заказы
        val orders = orderRepository.findByCustomerCustomerId(id)
        if (orders.isNotEmpty()) {
            throw RuntimeException("Нельзя удалить клиента с id $id: существуют связанные заказы")
        }

        // Проверка на связанные отзывы
        val reviews = reviewRepository.findByCustomerCustomerId(id)
        if (reviews.isNotEmpty()) {
            throw RuntimeException("Нельзя удалить клиента с id $id: существуют связанные отзывы")
        }

        customerRepository.deleteById(id)
        return "Клиент успешно удален"
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