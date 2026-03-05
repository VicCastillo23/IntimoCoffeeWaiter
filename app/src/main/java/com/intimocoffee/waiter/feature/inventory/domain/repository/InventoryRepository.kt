package com.intimocoffee.waiter.feature.inventory.domain.repository

import com.intimocoffee.waiter.feature.inventory.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

interface InventoryRepository {
    
    // Stock Summary
    suspend fun getStockSummary(date: LocalDate): StockSummary
    suspend fun getTodayStockSummary(): StockSummary
    fun getStockSummaryFlow(date: LocalDate): Flow<StockSummary>
    fun getTodayStockSummaryFlow(): Flow<StockSummary>
    
    // Stock Alerts
    fun getStockAlerts(): Flow<List<StockAlert>>
    suspend fun getStockAlertsForProduct(productId: Long): List<StockAlert>
    
    // Stock Movements
    fun getAllStockMovements(): Flow<List<StockMovement>>
    fun getStockMovementsByProduct(productId: Long): Flow<List<StockMovement>>
    fun getStockMovementsByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<StockMovement>>
    suspend fun addStockMovement(movement: StockMovement): Boolean
    
    // Stock Adjustments
    suspend fun adjustStock(adjustment: StockAdjustment, userId: Long): Boolean
    suspend fun adjustMultipleStocks(adjustments: List<StockAdjustment>, userId: Long): Boolean
    
    // Product Stock Details
    suspend fun getProductStockDetails(productId: Long): ProductStockDetails?
    fun getAllProductsStock(): Flow<List<ProductStockDetails>>
    
    // Purchase Orders
    fun getAllPurchaseOrders(): Flow<List<PurchaseOrder>>
    suspend fun createPurchaseOrder(purchaseOrder: PurchaseOrder): Long
    suspend fun updatePurchaseOrderStatus(orderId: Long, status: PurchaseOrderStatus): Boolean
    suspend fun receivePurchaseOrder(orderId: Long, receivedItems: List<PurchaseOrderItem>): Boolean
    
    // Automatic Stock Updates (when products are sold)
    suspend fun updateStockFromSale(productId: Long, quantity: Int, userId: Long): Boolean
    suspend fun updateStockFromSales(sales: Map<Long, Int>, userId: Long): Boolean
    
    // Stock Validation
    suspend fun validateStockAvailability(productId: Long, requiredQuantity: Int): Boolean
    suspend fun validateMultipleStockAvailability(requirements: Map<Long, Int>): Map<Long, Boolean>
    
    // Low Stock Notifications
    suspend fun generateLowStockAlerts(): List<StockAlert>
    suspend fun markAlertAsViewed(productId: Long): Boolean
}