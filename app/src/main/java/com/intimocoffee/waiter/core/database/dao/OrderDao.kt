package com.intimocoffee.waiter.core.database.dao

import androidx.room.*
import com.intimocoffee.waiter.core.database.entity.OrderEntity
import com.intimocoffee.waiter.core.database.entity.OrderItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>
    
    @Query("SELECT * FROM orders WHERE status IN ('PENDING', 'PREPARING', 'READY') ORDER BY createdAt DESC")
    fun getActiveOrders(): Flow<List<OrderEntity>>
    
    @Query("SELECT * FROM orders WHERE id = :orderId")
    suspend fun getOrderById(orderId: String): OrderEntity?
    
    @Query("SELECT * FROM orders WHERE tableId = :tableId AND status != 'PAID' AND status != 'CANCELLED' ORDER BY createdAt DESC LIMIT 1")
    suspend fun getCurrentOrderForTable(tableId: String): OrderEntity?
    
    @Query("SELECT * FROM orders WHERE DATE(createdAt/1000, 'unixepoch') = DATE('now') ORDER BY createdAt DESC")
    fun getTodayOrders(): Flow<List<OrderEntity>>
    
    @Query("SELECT COALESCE(MAX(orderNumber), 0) + 1 FROM orders")
    suspend fun getNextOrderNumber(): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)
    
    @Update
    suspend fun updateOrder(order: OrderEntity)
    
    @Delete
    suspend fun deleteOrder(order: OrderEntity)
    
    // Order Items
    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getOrderItems(orderId: String): List<OrderItemEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItem(orderItem: OrderItemEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(orderItems: List<OrderItemEntity>)
    
    @Update
    suspend fun updateOrderItem(orderItem: OrderItemEntity)
    
    @Delete
    suspend fun deleteOrderItem(orderItem: OrderItemEntity)
    
    @Query("DELETE FROM order_items WHERE orderId = :orderId")
    suspend fun deleteOrderItems(orderId: String)
    
    @Query("DELETE FROM orders")
    suspend fun deleteAllOrders()
    
    @Query("DELETE FROM order_items")
    suspend fun deleteAllOrderItems()
    
    @Transaction
    suspend fun insertOrderWithItems(order: OrderEntity, items: List<OrderItemEntity>) {
        insertOrder(order)
        insertOrderItems(items)
    }
}