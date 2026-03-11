package com.intimocoffee.waiter.feature.orders.data.repository

import android.util.Log
import com.intimocoffee.waiter.core.database.dao.OrderDao
import com.intimocoffee.waiter.core.database.entity.OrderItemEntity
import com.intimocoffee.waiter.core.network.ApiMappers.toOrderDomainModels
import com.intimocoffee.waiter.core.network.RemoteOrderService
import com.intimocoffee.waiter.feature.inventory.domain.service.InventoryService
import com.intimocoffee.waiter.feature.notifications.domain.service.NotificationService
import com.intimocoffee.waiter.feature.orders.data.mapper.OrderMapper
import com.intimocoffee.waiter.feature.orders.domain.model.Order
import com.intimocoffee.waiter.feature.orders.domain.model.OrderItem
import com.intimocoffee.waiter.feature.orders.domain.model.OrderStatus
import com.intimocoffee.waiter.feature.orders.domain.repository.OrderRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepositoryImpl @Inject constructor(
    private val orderDao: OrderDao,
    private val orderMapper: OrderMapper,
    private val inventoryService: InventoryService,
    private val notificationService: NotificationService,
    private val remoteOrderService: RemoteOrderService
) : OrderRepository {

    override suspend fun createOrder(order: Order, items: List<OrderItem>): Long {
        return try {
            // Validate stock availability first
            val stockAvailabilityResult = inventoryService.validateOrderStockAvailability(items)
            if (stockAvailabilityResult.isFailure) {
                Log.e("OrderRepository", "Stock validation failed during order creation: ${stockAvailabilityResult.exceptionOrNull()?.message}")
                return 0L
            }
            
            val stockAvailability = stockAvailabilityResult.getOrNull() ?: emptyMap()
            val unavailableProducts = items.filter { stockAvailability[it.productId] == false }
            if (unavailableProducts.isNotEmpty()) {
                Log.w("OrderRepository", "Cannot create order: Insufficient stock for products: ${unavailableProducts.map { it.productName }}")
                return 0L
            }
            
            val orderNumber = orderDao.getNextOrderNumber()
            val orderEntity = orderMapper.toEntity(order.copy(orderNumber = orderNumber.toString()))
            
            // Primero insertar la orden
            orderDao.insertOrder(orderEntity)
            
            // Obtener el ID generado
            val insertedOrder = orderDao.getOrderById(orderEntity.id) ?: throw Exception("Error al crear orden")
            val domainOrderId = orderMapper.uuidToLong(insertedOrder.id)
            
            // Ahora crear los items con el ID real de la orden (String UUID)
            val itemEntities = items.map { item ->
                orderMapper.toEntity(item.copy(orderId = domainOrderId)).copy(orderId = insertedOrder.id)
            }
            Log.d("OrderRepository", "Inserting ${itemEntities.size} items for order ${insertedOrder.id}")
            orderDao.insertOrderItems(itemEntities)
            
            // Send notification for new order and auto-route based on content
            val completeOrder = getOrderById(domainOrderId)
            if (completeOrder != null) {
                try {
                    notificationService.notifyNewOrder(completeOrder)
                    Log.d("OrderRepository", "Notification sent for new order $domainOrderId")
                    
                    // Auto-route order based on content
                    autoRouteOrderBasedOnContent(completeOrder, domainOrderId)
                    
                } catch (e: Exception) {
                    Log.w("OrderRepository", "Failed to send notification for new order $domainOrderId: ${e.message}")
                }
            }
            
            Log.d("OrderRepository", "Order created successfully with stock validation")
            domainOrderId
        } catch (e: Exception) {
            Log.e("OrderRepository", "Error creating order: ${e.message}", e)
            0L
        }
    }

    override suspend fun getOrderById(id: Long): Order? {
        return try {
            // Find order by consistent UUID conversion
            val allOrders = orderDao.getAllOrders().first()
            val orderEntity = allOrders.find { orderMapper.uuidToLong(it.id) == id } ?: return null
            val items = orderDao.getOrderItems(orderEntity.id)
            orderMapper.toDomainAsync(orderEntity, items)
        } catch (e: Exception) {
            Log.e("OrderRepository", "Error getting order by ID $id", e)
            null
        }
    }

    override suspend fun updateOrderStatus(orderId: Long, status: OrderStatus): Boolean {
        return try {
            Log.d("OrderRepository", "Updating order $orderId to status $status")
            
            // Get the complete order first (for inventory service)
            val order = getOrderById(orderId)
            if (order == null) {
                Log.e("OrderRepository", "Order $orderId not found")
                return false
            }
            
            // Find order entity by consistent UUID conversion
            val allOrders = orderDao.getAllOrders().first()
            val orderEntity = allOrders.find { orderMapper.uuidToLong(it.id) == orderId }
            if (orderEntity == null) {
                Log.e("OrderRepository", "Order entity $orderId not found")
                return false
            }
            Log.d("OrderRepository", "Found order: $orderEntity")
            
            val updatedOrder = orderEntity.copy(
                status = status.name,
                updatedAt = System.currentTimeMillis()
            )
            Log.d("OrderRepository", "Updating to: $updatedOrder")
            
            // Update the order status in database first
            orderDao.updateOrder(updatedOrder)
            Log.d("OrderRepository", "Order status updated successfully")
            
            // Handle inventory updates
            val userId = order.createdBy
            val inventoryResult = inventoryService.handleOrderStatusChange(order, status, userId)
            if (inventoryResult.isFailure) {
                Log.w("OrderRepository", "Inventory update failed for order $orderId: ${inventoryResult.exceptionOrNull()?.message}")
                // We don't fail the order status update if inventory fails
                // but we log the warning for manual intervention if needed
            } else {
                Log.d("OrderRepository", "Inventory updated successfully for order $orderId")
            }
            
            // Send notifications based on status change
            try {
                when (status) {
                    OrderStatus.READY -> {
                        notificationService.notifyOrderReady(order)
                        Log.d("OrderRepository", "Order ready notification sent for order $orderId")
                    }
                    else -> {
                        // Other status changes don't need notifications yet
                        Log.d("OrderRepository", "No notification needed for status $status")
                    }
                }
            } catch (e: Exception) {
                Log.w("OrderRepository", "Failed to send status change notification for order $orderId: ${e.message}")
            }
            
            true
        } catch (e: Exception) {
            Log.e("OrderRepository", "Error updating order status", e)
            false
        }
    }

    override suspend fun updateOrder(order: Order): Boolean {
        return try {
            orderDao.updateOrder(orderMapper.toEntity(order))
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun deleteOrder(id: Long): Boolean {
        return try {
            val orderEntity = orderDao.getOrderById(id.toString()) ?: return false
            // First delete all order items
            orderDao.deleteOrderItems(id.toString())
            // Then delete the order
            orderDao.deleteOrder(orderEntity)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun getAllOrders(): Flow<List<Order>> {
        // Online-only: read orders from the main server instead of local DB
        return flow {
            try {
                Log.d("OrderRepository", "getAllOrders: Fetching active orders from remote server (online-only)...")
                val result = remoteOrderService.getActiveOrdersFromServer()

                if (result.isSuccess) {
                    val apiOrders = result.getOrNull() ?: emptyList()
                    Log.d(
                        "OrderRepository",
                        "getAllOrders: Received ${apiOrders.size} active orders from server"
                    )
                    val domainOrders = apiOrders.toOrderDomainModels()
                    emit(domainOrders)
                } else {
                    Log.e(
                        "OrderRepository",
                        "getAllOrders: Failed to fetch orders from server (no local fallback)",
                        result.exceptionOrNull()
                    )
                    emit(emptyList())
                }
            } catch (e: CancellationException) {
                throw e // no atrapar cancelaciones de coroutine/flow
            } catch (e: Exception) {
                Log.e(
                    "OrderRepository",
                    "getAllOrders: Exception fetching orders from server (no local fallback)",
                    e
                )
                emit(emptyList())
            }
        }
    }

    override fun getOrdersByStatus(status: OrderStatus): Flow<List<Order>> {
        // Since we don't have a specific query, filter from all orders
        return getAllOrders().map { orders ->
            orders.filter { it.status == status }
        }
    }

    override fun getOrdersByTable(tableId: Long): Flow<List<Order>> {
        // Since we don't have a specific query, filter from all orders
        return getAllOrders().map { orders ->
            orders.filter { it.tableId == tableId }
        }
    }

    override fun getOrdersByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Order>> {
        // Simple filter approach since we don't have a direct query for date range
        return getAllOrders().map { orders ->
            orders.filter { order ->
                val orderDate = order.createdAt.date
                orderDate >= startDate && orderDate <= endDate
            }
        }
    }

    override fun getTodayOrders(): Flow<List<Order>> {
        return flow {
            orderDao.getTodayOrders().collect { orderEntities ->
                val orders = mutableListOf<Order>()
                for (orderEntity in orderEntities) {
                    try {
                        val items = orderDao.getOrderItems(orderEntity.id)
                        val order = orderMapper.toDomainAsync(orderEntity, items)
                        orders.add(order)
                    } catch (e: Exception) {
                        // Log error but continue with other orders
                    }
                }
                emit(orders)
            }
        }
    }

    override fun getActiveOrders(): Flow<List<Order>> {
        return flow {
            try {
                Log.d("OrderRepository", "Fetching active orders from remote server...")
                val result = remoteOrderService.getActiveOrdersFromServer()
                
                if (result.isSuccess) {
                    val orders = result.getOrNull()?.toOrderDomainModels() ?: emptyList()
                    Log.d("OrderRepository", "Successfully fetched ${orders.size} active orders from server")
                    emit(orders)
                } else {
                    Log.w("OrderRepository", "Failed to fetch active orders from server, returning empty list")
                    emit(emptyList())
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("OrderRepository", "Exception fetching active orders from server, returning empty list", e)
                emit(emptyList())
            }
        }
    }

    override suspend fun getOrderItems(orderId: Long): List<OrderItem> {
        return try {
            orderDao.getOrderItems(orderId.toString()).map { itemEntity ->
                orderMapper.toDomain(itemEntity)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun addItemToOrder(orderId: Long, item: OrderItem): Boolean {
        return try {
            Log.d("OrderRepository", "Adding item to order $orderId: ${item.productName} (qty: ${item.quantity})")
            
            // Validate stock availability first
            val stockAvailabilityResult = inventoryService.validateOrderStockAvailability(listOf(item))
            if (stockAvailabilityResult.isFailure) {
                Log.e("OrderRepository", "Stock validation failed: ${stockAvailabilityResult.exceptionOrNull()?.message}")
                return false
            }
            
            val stockAvailability = stockAvailabilityResult.getOrNull() ?: emptyMap()
            if (stockAvailability[item.productId] == false) {
                Log.w("OrderRepository", "Insufficient stock for product ${item.productId} (${item.productName}): requested ${item.quantity}")
                return false
            }
            
            // Find the order first
            val allOrdersFlow = orderDao.getAllOrders()
            val allOrders = allOrdersFlow.first()
            Log.d("OrderRepository", "Found ${allOrders.size} orders in database for adding item")
            
            val orderEntity = allOrders.find { orderMapper.uuidToLong(it.id) == orderId }
            if (orderEntity == null) {
                Log.e("OrderRepository", "Order $orderId not found among ${allOrders.size} orders")
                return false
            }
            
            Log.d("OrderRepository", "Found order entity: ${orderEntity.id} for domain ID: $orderId, status: ${orderEntity.status}")
            
            // Check if the order is completed
            val orderStatus = OrderStatus.valueOf(orderEntity.status)
            if (OrderStatus.isCompleted(orderStatus)) {
                Log.d("OrderRepository", "Order $orderId is completed (status: $orderStatus). Creating additional order instead.")
                
                // Create additional order with the new item
                val additionalOrderId = createAdditionalOrder(orderId, listOf(item))
                if (additionalOrderId > 0) {
                    Log.d("OrderRepository", "Successfully created additional order $additionalOrderId for completed order $orderId")
                    return true
                } else {
                    Log.e("OrderRepository", "Failed to create additional order for completed order $orderId")
                    return false
                }
            }
            
            Log.d("OrderRepository", "Order $orderId is active (status: $orderStatus). Adding item directly.")
            
            // Create the item entity with the correct UUID
            val itemEntity = OrderItemEntity(
                id = java.util.UUID.randomUUID().toString(),
                orderId = orderEntity.id, // Use the real UUID, not the domain ID
                productId = item.productId.toString(),
                quantity = item.quantity,
                unitPrice = item.productPrice.toString(),
                totalPrice = item.subtotal.toString(),
                notes = item.notes,
                createdAt = System.currentTimeMillis()
            )
            
            Log.d("OrderRepository", "Item entity to insert: $itemEntity")
            orderDao.insertOrderItem(itemEntity)
            Log.d("OrderRepository", "Successfully added item to order $orderId")
            
            // Handle inventory service for added item
            val order = getOrderById(orderId)
            val userId = order?.createdBy ?: 1L // Fallback to user ID 1 if not found
            val inventoryResult = inventoryService.handleOrderItemAdded(item, userId)
            if (inventoryResult.isFailure) {
                Log.w("OrderRepository", "Inventory service failed for added item: ${inventoryResult.exceptionOrNull()?.message}")
            }
            
            true
        } catch (e: Exception) {
            Log.e("OrderRepository", "Error adding item to order $orderId: ${e.message}", e)
            false
        }
    }

    override suspend fun updateOrderItem(item: OrderItem): Boolean {
        return try {
            orderDao.updateOrderItem(orderMapper.toEntity(item))
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun removeItemFromOrder(itemId: Long): Boolean {
        return try {
            // We need to find the item first since delete expects the entity
            // For simplicity, we'll implement this as a stub for now
            // TODO: Implement proper item removal
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getOrdersCount(): Long {
        return try {
            orderDao.getAllOrders().map { it.size.toLong() }.first()
        } catch (e: Exception) {
            0L
        }
    }

    override suspend fun getTodayOrdersCount(): Long {
        return try {
            orderDao.getTodayOrders().map { it.size.toLong() }.first()
        } catch (e: Exception) {
            0L
        }
    }

    override suspend fun getOrdersCountByStatus(status: OrderStatus): Long {
        return try {
            getOrdersByStatus(status).map { it.size.toLong() }.first()
        } catch (e: Exception) {
            0L
        }
    }

    override suspend fun generateOrderNumber(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val date = dateFormat.format(Date())
        val orderNumber = orderDao.getNextOrderNumber()
        return "ORD-$date-${orderNumber.toString().padStart(3, '0')}"
    }
    
    override suspend fun createAdditionalOrder(originalOrderId: Long, items: List<OrderItem>): Long {
        return try {
            Log.d("OrderRepository", "Creating additional order for original order ID: $originalOrderId")
            
            // Find the original order to copy its basic info
            val originalOrder = getOrderById(originalOrderId)
            if (originalOrder == null) {
                Log.e("OrderRepository", "Original order $originalOrderId not found for additional order")
                return 0L
            }
            
            Log.d("OrderRepository", "Original order found: table ${originalOrder.tableId}, status ${originalOrder.status}")
            
            // Generate a new order number
            val orderNumber = orderDao.getNextOrderNumber()
            val orderNumberStr = "${orderNumber}A" // Add 'A' suffix to indicate it's additional
            
            // Create the additional order with reference to original
            val additionalOrder = Order(
                id = 0L, // Let the system generate new ID
                orderNumber = orderNumberStr,
                tableId = originalOrder.tableId,
                tableName = originalOrder.tableName,
                customerName = originalOrder.customerName,
                status = OrderStatus.PENDING, // Always start as PENDING
                items = emptyList(), // Items will be added separately
                subtotal = items.sumOf { it.subtotal },
                tax = BigDecimal.ZERO, // Can be calculated later
                total = items.sumOf { it.subtotal },
                discount = BigDecimal.ZERO,
                notes = "Orden adicional para #${originalOrder.orderNumber}",
                originalOrderId = originalOrderId,
                isAdditionalOrder = true,
                createdAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                updatedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                createdBy = originalOrder.createdBy
            )
            
            Log.d("OrderRepository", "Creating additional order: $additionalOrder")
            
            // Convert to entity and create the order
            val orderEntity = orderMapper.toEntity(additionalOrder)
            orderDao.insertOrder(orderEntity)
            
            // Get the inserted order to get the real UUID
            val insertedOrder = orderDao.getOrderById(orderEntity.id) ?: throw Exception("Error creating additional order")
            val domainOrderId = orderMapper.uuidToLong(insertedOrder.id)
            
            // Create items with the correct order ID
            val itemEntities = items.map { item ->
                orderMapper.toEntity(item.copy(orderId = domainOrderId)).copy(orderId = insertedOrder.id)
            }
            
            Log.d("OrderRepository", "Inserting ${itemEntities.size} items for additional order ${insertedOrder.id}")
            orderDao.insertOrderItems(itemEntities)
            
            Log.d("OrderRepository", "Successfully created additional order with ID: $domainOrderId")
            domainOrderId
        } catch (e: Exception) {
            Log.e("OrderRepository", "Error creating additional order for $originalOrderId: ${e.message}", e)
            0L
        }
    }
    
    /**
     * Automatically routes order to kitchen or bar based on item content
     * The UI will show individual item statuses based on their categories
     */
    private suspend fun autoRouteOrderBasedOnContent(order: Order, orderId: Long) {
        try {
            val kitchenItems = order.items.filter { isKitchenItem(it) }
            val barItems = order.items.filter { isBarItem(it) }
            
            Log.d("OrderRepository", "Order $orderId analysis: Kitchen items=${kitchenItems.size}, Bar items=${barItems.size}")
            
            // Log individual item routing for clarity
            kitchenItems.forEach { item ->
                Log.d("OrderRepository", "Item ${item.productName} will be shown as 'Enviado a Cocina' in UI")
            }
            
            barItems.forEach { item ->
                Log.d("OrderRepository", "Item ${item.productName} will be shown as 'Enviado a Barra' in UI")
            }
            
            // Update overall order status based on content
            when {
                kitchenItems.isNotEmpty() && barItems.isNotEmpty() -> {
                    // Mixed order - set to IN_PREPARATION since items go to different stations
                    Log.d("OrderRepository", "Mixed order detected - setting to IN_PREPARATION")
                    updateOrderStatus(orderId, OrderStatus.IN_PREPARATION)
                }
                kitchenItems.isNotEmpty() -> {
                    // Only kitchen items
                    Log.d("OrderRepository", "Kitchen-only order detected - sending to kitchen")
                    updateOrderStatus(orderId, OrderStatus.SENT_TO_KITCHEN)
                }
                barItems.isNotEmpty() -> {
                    // Only bar items
                    Log.d("OrderRepository", "Bar-only order detected - sending to bar")
                    updateOrderStatus(orderId, OrderStatus.SENT_TO_BAR)
                }
                else -> {
                    // No items or unclassified items - keep as PENDING
                    Log.d("OrderRepository", "Order has no classifiable items - keeping as PENDING")
                }
            }
            
        } catch (e: Exception) {
            Log.e("OrderRepository", "Error auto-routing order $orderId: ${e.message}", e)
        }
    }
    
    /**
     * Determines if an order item belongs to kitchen (food items)
     */
    private fun isKitchenItem(item: OrderItem): Boolean {
        // Categories 3, 4, 5 are typically food items (FOOD, SNACKS, etc.)
        return item.categoryId in listOf(3L, 4L, 5L)
    }
    
    /**
     * Determines if an order item belongs to bar (drinks)
     */
    private fun isBarItem(item: OrderItem): Boolean {
        // Categories 1, 2 are typically drink items (BEVERAGES, HOT_DRINKS, COLD_DRINKS)
        return item.categoryId in listOf(1L, 2L)
    }
    
}
