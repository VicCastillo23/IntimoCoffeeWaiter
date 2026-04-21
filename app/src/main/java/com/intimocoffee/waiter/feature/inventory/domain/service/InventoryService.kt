package com.intimocoffee.waiter.feature.inventory.domain.service

import com.intimocoffee.waiter.feature.orders.domain.model.Order
import com.intimocoffee.waiter.feature.orders.domain.model.OrderItem
import com.intimocoffee.waiter.feature.orders.domain.model.OrderStatus

/**
 * Service that handles the integration between sales/orders and inventory management.
 * This service is responsible for automatically updating stock when orders are completed.
 */
interface InventoryService {
    
    /**
     * Updates stock when an order status changes to a final state (DELIVERED, PAID).
     * This automatically creates stock movements and updates product quantities.
     */
    suspend fun handleOrderStatusChange(order: Order, newStatus: OrderStatus, userId: Long): Result<Unit>
    
    /**
     * Updates stock when new items are added to an order.
     * Only affects active orders - completed orders create additional orders instead.
     */
    suspend fun handleOrderItemAdded(orderItem: OrderItem, userId: Long): Result<Unit>
    
    /**
     * Updates stock when an order item is removed or its quantity is changed.
     */
    suspend fun handleOrderItemChanged(
        oldItem: OrderItem?, 
        newItem: OrderItem?, 
        userId: Long
    ): Result<Unit>
    
    /**
     * Validates stock availability before allowing order creation/modification.
     */
    suspend fun validateOrderStockAvailability(items: List<OrderItem>): Result<Map<String, Boolean>>
    
    /**
     * Reverts stock changes when an order is cancelled.
     */
    suspend fun revertOrderStock(order: Order, userId: Long): Result<Unit>
    
    /**
     * Handles batch stock updates for multiple orders (useful for reconciliation).
     */
    suspend fun handleBatchOrderCompletion(orders: List<Order>, userId: Long): Result<Unit>
    
    /**
     * Checks if there's sufficient stock for an order and returns detailed information.
     */
    suspend fun getStockAvailabilityReport(items: List<OrderItem>): StockAvailabilityReport
}

/**
 * Detailed report of stock availability for order items.
 */
data class StockAvailabilityReport(
    val isAvailable: Boolean,
    val itemAvailability: Map<Long, ItemStockStatus>,
    val warnings: List<String> = emptyList()
)

data class ItemStockStatus(
    val productId: Long,
    val productName: String,
    val requestedQuantity: Int,
    val availableQuantity: Int,
    val isAvailable: Boolean,
    val isLowStock: Boolean,
    val willBeOutOfStock: Boolean
)