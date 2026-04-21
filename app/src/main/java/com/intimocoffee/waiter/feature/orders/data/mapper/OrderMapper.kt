package com.intimocoffee.waiter.feature.orders.data.mapper

import android.util.Log
import com.intimocoffee.waiter.core.database.entity.OrderEntity
import com.intimocoffee.waiter.core.database.entity.OrderItemEntity
import com.intimocoffee.waiter.feature.orders.domain.model.Order
import com.intimocoffee.waiter.feature.orders.domain.model.OrderItem
import com.intimocoffee.waiter.feature.orders.domain.model.OrderStatus
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderMapper @Inject constructor(
    private val tableDao: com.intimocoffee.waiter.core.database.dao.TableDao,
    private val productDao: com.intimocoffee.waiter.core.database.dao.ProductDao
) {
    
    /**
     * Converts a UUID string to a Long for domain model compatibility.
     * Uses consistent logic across the app.
     */
    fun uuidToLong(uuid: String): Long {
        return try {
            val hexString = uuid.replace("-", "").take(12)
            Log.d("OrderMapper", "Converting hex string '$hexString' to Long")
            hexString.toLongOrNull(16) ?: kotlin.math.abs(uuid.hashCode().toLong())
        } catch (e: Exception) {
            Log.e("OrderMapper", "Error converting UUID $uuid to Long", e)
            kotlin.math.abs(uuid.hashCode().toLong())
        }
    }

    suspend fun toDomainAsync(orderEntity: OrderEntity, itemEntities: List<OrderItemEntity>): Order {
        Log.d("OrderMapper", "Mapping order ${orderEntity.id} with tableId: ${orderEntity.tableId}")
        
        val tableName = try {
            Log.d("OrderMapper", "Querying table with ID: ${orderEntity.tableId}")
            val table = tableDao.getTableById((orderEntity.tableId?.toLongOrNull() ?: 0L).toString())
            Log.d("OrderMapper", "Found table: $table")
            val name = table?.name ?: "Mesa ${orderEntity.tableId}"
            Log.d("OrderMapper", "Table name resolved to: $name")
            name
        } catch (e: Exception) {
            Log.e("OrderMapper", "Error getting table name for ID ${orderEntity.tableId}", e)
            "Mesa ${orderEntity.tableId}"
        }
        
        Log.d("OrderMapper", "Processing ${itemEntities.size} items for order ${orderEntity.id}")
        val items = try {
            itemEntities.map { itemEntity ->
                toDomainAsync(itemEntity)
            }
        } catch (e: Exception) {
            Log.e("OrderMapper", "Error mapping items for order ${orderEntity.id}", e)
            throw e
        }
        Log.d("OrderMapper", "Processed items: ${items.size} items")
        
        // Use the UUID directly as string for consistency - parse it to Long for domain compatibility
        // Extract numeric part from UUID for backwards compatibility, but ensure uniqueness
        val domainId = uuidToLong(orderEntity.id)
        Log.d("OrderMapper", "Order entity ID ${orderEntity.id} mapped to domain ID: $domainId")
        
        try {
            return Order(
                id = domainId,
                orderNumber = orderEntity.orderNumber.toString(),
                tableId = orderEntity.tableId?.toLongOrNull(),
                tableName = tableName,
                customerName = null, // Not available in current entity (uses customerId)
                status = OrderStatus.valueOf(orderEntity.status),
                items = items,
                subtotal = BigDecimal(orderEntity.subtotal),
                tax = BigDecimal(orderEntity.tax),
                total = BigDecimal(orderEntity.total),
                discount = BigDecimal(orderEntity.discount),
                notes = orderEntity.notes,
                originalOrderId = orderEntity.originalOrderId?.let { uuidToLong(it) },
                isAdditionalOrder = orderEntity.isAdditionalOrder,
                createdAt = Instant.fromEpochMilliseconds(orderEntity.createdAt).toLocalDateTime(TimeZone.currentSystemDefault()),
                updatedAt = Instant.fromEpochMilliseconds(orderEntity.updatedAt).toLocalDateTime(TimeZone.currentSystemDefault()),
                completedAt = null, // Not available in current entity
                createdBy = orderEntity.userId.toLongOrNull() ?: 0L
            )
        } catch (e: Exception) {
            Log.e("OrderMapper", "Error creating Order object for ${orderEntity.id}", e)
            throw e
        }
    }
    
    fun toDomain(orderEntity: OrderEntity, itemEntities: List<OrderItemEntity>): Order {
        // Use same ID mapping as toDomainAsync for consistency
        val domainId = uuidToLong(orderEntity.id)
        return Order(
            id = domainId,
            orderNumber = orderEntity.orderNumber.toString(),
            tableId = orderEntity.tableId?.toLongOrNull(),
            tableName = orderEntity.tableId?.let { "Mesa $it" }, // Simple fallback
            customerName = null, // Not available in current entity (uses customerId)
            status = OrderStatus.valueOf(orderEntity.status),
            items = itemEntities.map { toDomain(it) },
            subtotal = BigDecimal(orderEntity.subtotal),
            tax = BigDecimal(orderEntity.tax),
            total = BigDecimal(orderEntity.total),
            discount = BigDecimal(orderEntity.discount),
            notes = orderEntity.notes,
            originalOrderId = orderEntity.originalOrderId?.let { uuidToLong(it) },
            isAdditionalOrder = orderEntity.isAdditionalOrder,
            createdAt = Instant.fromEpochMilliseconds(orderEntity.createdAt).toLocalDateTime(TimeZone.currentSystemDefault()),
            updatedAt = Instant.fromEpochMilliseconds(orderEntity.updatedAt).toLocalDateTime(TimeZone.currentSystemDefault()),
            completedAt = null, // Not available in current entity
            createdBy = orderEntity.userId.toLongOrNull() ?: 0L
        )
    }

    suspend fun toDomainAsync(itemEntity: OrderItemEntity): OrderItem {
        Log.d("OrderMapper", "Mapping order item ${itemEntity.id} with productId: ${itemEntity.productId}")
        
        val productName = try {
            Log.d("OrderMapper", "Querying product with ID: ${itemEntity.productId}")
            val product = productDao.getProductById(itemEntity.productId)
            Log.d("OrderMapper", "Found product: $product")
            val name = product?.name ?: "Producto ${itemEntity.productId}"
            Log.d("OrderMapper", "Product name resolved to: $name")
            name
        } catch (e: Exception) {
            Log.e("OrderMapper", "Error getting product name for ID ${itemEntity.productId}", e)
            "Producto ${itemEntity.productId}"
        }
        
        val rawPid = itemEntity.productId
        val parsedPid = rawPid.toLongOrNull()
        return OrderItem(
            id = itemEntity.id.toLongOrNull() ?: 0L,
            orderId = itemEntity.orderId.toLongOrNull() ?: 0L,
            productId = parsedPid ?: 0L,
            productDatabaseId = if (parsedPid == null && rawPid.isNotBlank()) rawPid else null,
            productName = productName,
            productPrice = BigDecimal(itemEntity.unitPrice),
            quantity = itemEntity.quantity,
            subtotal = BigDecimal(itemEntity.totalPrice),
            notes = itemEntity.notes,
            categoryId = itemEntity.categoryId ?: 0L,
            createdAt = Instant.fromEpochMilliseconds(itemEntity.createdAt).toLocalDateTime(TimeZone.currentSystemDefault())
        )
    }
    
    fun toDomain(itemEntity: OrderItemEntity): OrderItem {
        val rawPid = itemEntity.productId
        val parsedPid = rawPid.toLongOrNull()
        return OrderItem(
            id = itemEntity.id.toLongOrNull() ?: 0L,
            orderId = itemEntity.orderId.toLongOrNull() ?: 0L,
            productId = parsedPid ?: 0L,
            productDatabaseId = if (parsedPid == null && rawPid.isNotBlank()) rawPid else null,
            productName = "Producto", // Not available in current entity, would need to fetch from product table
            productPrice = BigDecimal(itemEntity.unitPrice),
            quantity = itemEntity.quantity,
            subtotal = BigDecimal(itemEntity.totalPrice),
            notes = itemEntity.notes,
            categoryId = itemEntity.categoryId ?: 0L,
            createdAt = Instant.fromEpochMilliseconds(itemEntity.createdAt).toLocalDateTime(TimeZone.currentSystemDefault())
        )
    }

    fun toEntity(order: Order): OrderEntity {
        return OrderEntity(
            id = if (order.id == 0L) UUID.randomUUID().toString() else "order_${order.id}",
            orderNumber = order.orderNumber.toIntOrNull() ?: 1,
            tableId = order.tableId?.toString(),
            customerId = null, // Would need customer management
            userId = order.createdBy.toString(),
            status = order.status.name,
            subtotal = order.subtotal.toString(),
            tax = order.tax.toString(),
            total = order.total.toString(),
            discount = order.discount.toString(),
            notes = order.notes,
            originalOrderId = order.originalOrderId?.toString(), // Convert back to string
            isAdditionalOrder = order.isAdditionalOrder,
            createdAt = order.createdAt.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
            updatedAt = order.updatedAt.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        )
    }

    fun toEntity(orderItem: OrderItem): OrderItemEntity {
        return OrderItemEntity(
            id = if (orderItem.id == 0L) UUID.randomUUID().toString() else orderItem.id.toString(),
            orderId = if (orderItem.orderId == 0L) UUID.randomUUID().toString() else "order_${orderItem.orderId}",
            productId = orderItem.productDatabaseId?.takeIf { it.isNotBlank() }
                ?: orderItem.productId.toString(),
            categoryId = orderItem.categoryId.takeIf { it != 0L },
            quantity = orderItem.quantity,
            unitPrice = orderItem.productPrice.toString(),
            totalPrice = orderItem.subtotal.toString(),
            notes = orderItem.notes,
            createdAt = orderItem.createdAt.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        )
    }
}
