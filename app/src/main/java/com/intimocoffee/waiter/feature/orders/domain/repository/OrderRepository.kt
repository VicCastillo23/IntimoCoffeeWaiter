package com.intimocoffee.waiter.feature.orders.domain.repository

import com.intimocoffee.waiter.feature.orders.domain.model.Order
import com.intimocoffee.waiter.feature.orders.domain.model.OrderItem
import com.intimocoffee.waiter.feature.orders.domain.model.OrderStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface OrderRepository {
    
    // Orders CRUD
    suspend fun createOrder(order: Order, items: List<OrderItem>): Long
    suspend fun getOrderById(id: Long): Order?
    suspend fun updateOrderStatus(orderId: Long, status: OrderStatus): Boolean
    suspend fun updateOrder(order: Order): Boolean
    suspend fun deleteOrder(id: Long): Boolean
    
    // Orders queries
    fun getAllOrders(): Flow<List<Order>>
    fun getOrdersByStatus(status: OrderStatus): Flow<List<Order>>
    fun getOrdersByTable(tableId: Long): Flow<List<Order>>
    fun getOrdersByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Order>>
    fun getTodayOrders(): Flow<List<Order>>
    fun getActiveOrders(): Flow<List<Order>> // PENDING, PREPARING, READY
    
    // Order Items
    suspend fun getOrderItems(orderId: Long): List<OrderItem>
    suspend fun addItemToOrder(orderId: Long, item: OrderItem): Boolean
    suspend fun updateOrderItem(item: OrderItem): Boolean
    suspend fun removeItemFromOrder(itemId: Long): Boolean
    
    // Statistics
    suspend fun getOrdersCount(): Long
    suspend fun getTodayOrdersCount(): Long
    suspend fun getOrdersCountByStatus(status: OrderStatus): Long
    
    // Order numbers
    suspend fun generateOrderNumber(): String
    
    /**
     * Creates an additional order for a completed order.
     * This is used when items are added to DELIVERED or CANCELLED orders.
     */
    suspend fun createAdditionalOrder(originalOrderId: Long, items: List<OrderItem>): Long
}