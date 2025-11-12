package ru.vsuet.backend.controller

import org.springframework.web.bind.annotation.*
import ru.vsuet.backend.model.Order
import ru.vsuet.backend.model.Product
import ru.vsuet.backend.model.Seller
import ru.vsuet.backend.model.dto.*
import ru.vsuet.backend.service.EntityService

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = ["*"])
class EntityController(
    private val entityService: EntityService
) {
    @GetMapping("/sellers")
    fun getSellers(): List<SellerDto> = entityService.getAllSellers()

    @PostMapping("/sellers")
    fun createSeller(@RequestBody seller: SellerCreateDto): SellerDto = entityService.createSeller(seller)

    @PutMapping("/sellers/{id}")
    fun updateSeller(@PathVariable id: Long, @RequestBody seller: SellerCreateDto): SellerDto =
        entityService.updateSeller(id, seller)

    @DeleteMapping("/sellers/{id}")
    fun deleteSeller(@PathVariable id: Long): Map<String, String> {
        entityService.deleteSeller(id)
        return mapOf("message" to "Продавец успешно удален")
    }

    @GetMapping("/order-items")
    fun getOrderItems(): List<OrderItemDto> = entityService.getOrderItems()

    @GetMapping("/reviews")
    fun getReviews(): List<ReviewDto> = entityService.getReviews()

    @GetMapping("/warehouses")
    fun getWarehouses(): List<WarehouseDto> = entityService.getWarehouses()

    @GetMapping("/customers")
    fun getCustomers(): List<CustomerDto> = entityService.getCustomers()

    @GetMapping("/categories")
    fun getAllCategories(): List<CategoryDto> = entityService.getAllCategories()

    @GetMapping("/products")
    fun getProducts(): List<ProductDto> = entityService.getAllProducts()

    @PostMapping("/products")
    fun createProduct(@RequestBody product: ProductCreateDto): ProductDto = entityService.createProduct(product)

    @PutMapping("/products/{id}")
    fun updateProduct(@PathVariable id: Long, @RequestBody product: Product): Product =
        entityService.updateProduct(id, product)

    @DeleteMapping("/products/{id}")
    fun deleteProduct(@PathVariable id: Long): Map<String, String> {
        entityService.deleteProduct(id)
        return mapOf("message" to "Товар успешно удален")
    }

    @GetMapping("/orders")
    fun getOrders(): List<OrderDto> = entityService.getAllOrders()

    @DeleteMapping("/orders/{id}")
    fun deleteOrder(@PathVariable id: Long): Map<String, String> {
        entityService.deleteOrder(id)
        return mapOf("message" to "Заказ успешно удален")
    }

    @DeleteMapping("/order-item/{id}")
    fun deleteOrderItem(@PathVariable id: Long): Map<String, String> {
        entityService.deleteOrderItem(id)
        return mapOf("message" to "Товар заказа успешно удален")
    }

    @DeleteMapping("/review/{id}")
    fun deleteReview(@PathVariable id: Long): Map<String, String> {
        entityService.deleteReview(id)
        return mapOf("message" to "Отзыв успешно удален")
    }

    @DeleteMapping("/warehouse/{id}")
    fun deleteWarehouse(@PathVariable id: Long): Map<String, String> {
        entityService.deleteWarehouse(id)
        return mapOf("message" to "Складские остатки успешно удалены")
    }

    @DeleteMapping("/customer/{id}")
    fun deleteCustomer(@PathVariable id: Long): Map<String, String> {
        entityService.deleteCustomer(id)
        return mapOf("message" to "Клиент успешно удален")
    }
}