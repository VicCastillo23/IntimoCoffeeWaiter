package com.intimocoffee.waiter.core.network

import com.intimocoffee.waiter.feature.orders.domain.model.Order
import com.intimocoffee.waiter.feature.orders.domain.model.OrderItem
import com.intimocoffee.waiter.feature.orders.domain.model.OrderItemStatus
import com.intimocoffee.waiter.feature.orders.domain.model.OrderStatus
import com.intimocoffee.waiter.feature.products.domain.model.Product
import com.intimocoffee.waiter.feature.tables.domain.model.Table
import com.intimocoffee.waiter.feature.tables.domain.model.TableStatus
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal

/**
 * Mappers to convert API Response DTOs to Domain Models
 */
object ApiMappers {
    
    fun mapToProduct(response: ProductResponse): Product {
        val taxRate = response.taxRatePercent
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { BigDecimal(it) }.getOrNull() }
        val rawId = response.databaseId?.trim()?.takeIf { it.isNotEmpty() } ?: response.id.toString()
        val parsedId = response.id.takeIf { it != 0L } ?: rawId.toLongOrNull()
        return Product(
            id = parsedId ?: 0L,
            rawId = rawId,
            name = response.name,
            description = response.description,
            price = BigDecimal(response.price),
            categoryId = response.categoryId,
            categoryName = response.categoryName,
            imageUrl = response.imageUrl,
            isActive = response.isActive,
            stockQuantity = response.stockQuantity,
            minStockLevel = response.minStockLevel,
            barcode = null, // Not available in API response
            taxRatePercent = taxRate
        )
    }
    
    fun mapToTable(response: TableResponse): Table {
        return Table(
            id = response.id,
            number = response.number,
            name = response.name,
            capacity = response.capacity,
            zone = response.zone,
            status = mapToTableStatus(response.status),
            isActive = response.isActive,
            currentOrderId = null, // May need to be added to API response
            positionX = 0f, // Default values - may need to be added to API
            positionY = 0f
        )
    }
    
    fun mapToOrder(response: OrderResponse): Order {
        return Order(
            id = response.id,
            orderNumber = response.orderNumber,
            tableId = response.tableId,
            tableName = response.tableName,
            customerName = response.customerName,
            status = mapToOrderStatus(response.status),
            items = response.items.map { mapToOrderItem(it).copy(orderId = response.id) },
            subtotal = BigDecimal(response.subtotal),
            tax = BigDecimal(response.tax),
            total = BigDecimal(response.total),
            discount = BigDecimal.ZERO, // Default - may need to be added to API
            notes = response.notes,
            originalOrderId = null, // May need to be added to API response
            isAdditionalOrder = false, // Default value
            createdAt = parseDateTime(response.createdAt),
            updatedAt = parseDateTime(response.updatedAt),
            completedAt = null, // May need to be added to API response
            createdBy = response.createdBy
        )
    }
    
    fun mapToOrderItem(response: OrderItemResponse): OrderItem {
        return OrderItem(
            id = response.id,
            orderId = 0L, // Will be set by order mapping
            productId = response.productId,
            productName = response.productName,
            productPrice = BigDecimal(response.unitPrice),
            quantity = response.quantity,
            subtotal = BigDecimal(response.subtotal),
            notes = response.notes,
            categoryId = response.categoryId,
            itemStatus = mapToOrderItemStatus(response.itemStatus),
            sentToKitchenAt = null, // These timestamps may need to be added to API
            preparationStartedAt = null,
            readyAt = null,
            deliveredAt = null,
            preparationTimeMinutes = null,
            createdAt = LocalDateTime.parse("2025-01-01T00:00:00") // Default - needs timestamp in API
        )
    }
    
    private fun mapToTableStatus(status: String): TableStatus {
        return when (status.uppercase()) {
            "FREE" -> TableStatus.FREE
            "OCCUPIED" -> TableStatus.OCCUPIED
            "RESERVED" -> TableStatus.RESERVED
            "OUT_OF_SERVICE" -> TableStatus.OUT_OF_SERVICE
            else -> TableStatus.FREE
        }
    }
    
    private fun mapToOrderStatus(status: String): OrderStatus {
        return try {
            OrderStatus.valueOf(status.uppercase())
        } catch (e: Exception) {
            OrderStatus.PENDING // Default fallback
        }
    }
    
    private fun mapToOrderItemStatus(status: String): OrderItemStatus {
        return try {
            OrderItemStatus.valueOf(status.uppercase())
        } catch (e: Exception) {
            OrderItemStatus.PENDING // Default fallback
        }
    }
    
    private fun parseDateTime(dateTimeString: String): LocalDateTime {
        return try {
            // Try parsing ISO format first
            LocalDateTime.parse(dateTimeString)
        } catch (e: Exception) {
            try {
                // Fallback to other common formats
                if (dateTimeString.contains("T")) {
                    LocalDateTime.parse(dateTimeString.substringBefore(".") + "")
                } else {
                    // Use current time as fallback
                    kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
                }
            } catch (e2: Exception) {
                // Ultimate fallback
                kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
            }
        }
    }
    
    // Extension functions for easier conversion
    fun List<ProductResponse>.toProductDomainModels(): List<Product> {
        return this.map { mapToProduct(it) }
    }
    
    fun List<TableResponse>.toTableDomainModels(): List<Table> {
        return this.map { mapToTable(it) }
    }
    
    fun List<OrderResponse>.toOrderDomainModels(): List<Order> {
        return this.map { mapToOrder(it) }
    }
}