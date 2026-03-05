package com.intimocoffee.waiter.feature.inventory.domain.usecase

import android.util.Log
import com.intimocoffee.waiter.feature.inventory.domain.service.InventoryService
import com.intimocoffee.waiter.feature.orders.domain.model.Order
import com.intimocoffee.waiter.feature.orders.domain.model.OrderItem
import com.intimocoffee.waiter.feature.orders.domain.model.OrderStatus
import com.intimocoffee.waiter.feature.orders.domain.repository.OrderRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Demo class to test and showcase the inventory-sales integration functionality.
 * This can be used for testing the integration or as an example of how the system works.
 */
class InventorySalesIntegrationDemo @Inject constructor(
    private val inventoryService: InventoryService,
    private val orderRepository: OrderRepository
) {
    companion object {
        private const val TAG = "InventoryIntegration"
    }

    /**
     * Demonstrates the complete flow of inventory integration with sales.
     */
    suspend fun demonstrateIntegration(): Result<String> {
        return try {
            Log.d(TAG, "Starting inventory-sales integration demonstration...")
            
            // 1. Create a sample order with items
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            
            val sampleOrderItems = listOf(
                OrderItem(
                    id = 1L,
                    orderId = 1L,
                    productId = 1L,
                    productName = "Café Americano",
                    productPrice = BigDecimal("3.50"),
                    quantity = 2,
                    subtotal = BigDecimal("7.00"),
                    notes = null,
                    createdAt = now
                ),
                OrderItem(
                    id = 2L,
                    orderId = 1L,
                    productId = 2L,
                    productName = "Croissant",
                    productPrice = BigDecimal("2.50"),
                    quantity = 1,
                    subtotal = BigDecimal("2.50"),
                    notes = null,
                    createdAt = now
                )
            )
            
            val sampleOrder = Order(
                id = 1L,
                orderNumber = "DEMO-001",
                tableId = 1L,
                tableName = "Mesa 1",
                customerName = "Demo Customer",
                status = OrderStatus.PENDING,
                items = sampleOrderItems,
                subtotal = BigDecimal("9.50"),
                tax = BigDecimal("0.95"),
                total = BigDecimal("10.45"),
                discount = BigDecimal.ZERO,
                notes = "Demo order for integration testing",
                createdAt = now,
                updatedAt = now,
                createdBy = 1L
            )
            
            Log.d(TAG, "Created demo order: ${sampleOrder.orderNumber}")
            
            // 2. Validate stock availability
            val stockReport = inventoryService.getStockAvailabilityReport(sampleOrderItems)
            Log.d(TAG, "Stock availability check: Available=${stockReport.isAvailable}, Warnings=${stockReport.warnings.size}")
            
            stockReport.warnings.forEach { warning ->
                Log.d(TAG, "Stock warning: $warning")
            }
            
            if (!stockReport.isAvailable) {
                return Result.success("Demo completed - Order cannot be created due to insufficient stock")
            }
            
            // 3. Simulate order status changes and see inventory updates
            val userId = 1L
            
            // Simulate order being delivered (this should update stock)
            Log.d(TAG, "Simulating order delivery...")
            val deliveryResult = inventoryService.handleOrderStatusChange(sampleOrder, OrderStatus.DELIVERED, userId)
            
            if (deliveryResult.isSuccess) {
                Log.d(TAG, "Order delivery processed successfully - stock should be updated")
            } else {
                Log.e(TAG, "Order delivery processing failed: ${deliveryResult.exceptionOrNull()?.message}")
            }
            
            // 4. Simulate order cancellation (this should revert stock)
            Log.d(TAG, "Simulating order cancellation...")
            val deliveredOrder = sampleOrder.copy(status = OrderStatus.DELIVERED)
            val cancellationResult = inventoryService.handleOrderStatusChange(deliveredOrder, OrderStatus.CANCELLED, userId)
            
            if (cancellationResult.isSuccess) {
                Log.d(TAG, "Order cancellation processed successfully - stock should be reverted")
            } else {
                Log.e(TAG, "Order cancellation processing failed: ${cancellationResult.exceptionOrNull()?.message}")
            }
            
            // 5. Test adding item to order
            val additionalItem = OrderItem(
                id = 3L,
                orderId = 1L,
                productId = 3L,
                productName = "Muffin",
                productPrice = BigDecimal("3.00"),
                quantity = 1,
                subtotal = BigDecimal("3.00"),
                notes = null,
                createdAt = now
            )
            
            Log.d(TAG, "Simulating item addition to order...")
            val addItemResult = inventoryService.handleOrderItemAdded(additionalItem, userId)
            
            if (addItemResult.isSuccess) {
                Log.d(TAG, "Item addition processed successfully")
            } else {
                Log.e(TAG, "Item addition processing failed: ${addItemResult.exceptionOrNull()?.message}")
            }
            
            Log.d(TAG, "Inventory-sales integration demonstration completed successfully!")
            
            Result.success("Integration demo completed successfully. Check logs for detailed flow.")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during integration demonstration", e)
            Result.failure(e)
        }
    }
    
    /**
     * Tests stock validation for multiple scenarios.
     */
    suspend fun testStockValidationScenarios(): Result<String> {
        return try {
            Log.d(TAG, "Testing stock validation scenarios...")
            
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            
            // Test scenario 1: Normal order with sufficient stock
            val normalOrderItems = listOf(
                OrderItem(
                    id = 1L, orderId = 1L, productId = 1L,
                    productName = "Café Americano", productPrice = BigDecimal("3.50"),
                    quantity = 1, subtotal = BigDecimal("3.50"),
                    notes = null, createdAt = now
                )
            )
            
            val normalStockReport = inventoryService.getStockAvailabilityReport(normalOrderItems)
            Log.d(TAG, "Normal order - Available: ${normalStockReport.isAvailable}")
            
            // Test scenario 2: Large order that might cause stock issues
            val largeOrderItems = listOf(
                OrderItem(
                    id = 2L, orderId = 2L, productId = 1L,
                    productName = "Café Americano", productPrice = BigDecimal("3.50"),
                    quantity = 100, subtotal = BigDecimal("350.00"),
                    notes = null, createdAt = now
                )
            )
            
            val largeStockReport = inventoryService.getStockAvailabilityReport(largeOrderItems)
            Log.d(TAG, "Large order - Available: ${largeStockReport.isAvailable}, Warnings: ${largeStockReport.warnings.size}")
            
            largeStockReport.warnings.forEach { warning ->
                Log.d(TAG, "Large order warning: $warning")
            }
            
            // Test scenario 3: Multiple products order
            val multipleProductItems = listOf(
                OrderItem(
                    id = 3L, orderId = 3L, productId = 1L,
                    productName = "Café Americano", productPrice = BigDecimal("3.50"),
                    quantity = 2, subtotal = BigDecimal("7.00"),
                    notes = null, createdAt = now
                ),
                OrderItem(
                    id = 4L, orderId = 3L, productId = 2L,
                    productName = "Croissant", productPrice = BigDecimal("2.50"),
                    quantity = 5, subtotal = BigDecimal("12.50"),
                    notes = null, createdAt = now
                )
            )
            
            val multipleStockReport = inventoryService.getStockAvailabilityReport(multipleProductItems)
            Log.d(TAG, "Multiple products order - Available: ${multipleStockReport.isAvailable}")
            
            Log.d(TAG, "Stock validation scenarios testing completed!")
            
            Result.success("Stock validation tests completed. Normal order: ${normalStockReport.isAvailable}, Large order: ${largeStockReport.isAvailable}, Multiple products: ${multipleStockReport.isAvailable}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during stock validation testing", e)
            Result.failure(e)
        }
    }
    
    /**
     * Demonstrates batch order processing.
     */
    suspend fun demonstrateBatchProcessing(): Result<String> {
        return try {
            Log.d(TAG, "Demonstrating batch order processing...")
            
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            
            // Create multiple demo orders
            val batchOrders = (1..3).map { orderNum ->
                Order(
                    id = orderNum.toLong(),
                    orderNumber = "BATCH-00$orderNum",
                    tableId = orderNum.toLong(),
                    tableName = "Mesa $orderNum",
                    customerName = "Batch Customer $orderNum",
                    status = OrderStatus.READY,
                    items = listOf(
                        OrderItem(
                            id = orderNum.toLong(),
                            orderId = orderNum.toLong(),
                            productId = 1L,
                            productName = "Café Americano",
                            productPrice = BigDecimal("3.50"),
                            quantity = 1,
                            subtotal = BigDecimal("3.50"),
                            notes = null,
                            createdAt = now
                        )
                    ),
                    subtotal = BigDecimal("3.50"),
                    tax = BigDecimal("0.35"),
                    total = BigDecimal("3.85"),
                    discount = BigDecimal.ZERO,
                    notes = "Batch processing demo",
                    createdAt = now,
                    updatedAt = now,
                    createdBy = 1L
                )
            }
            
            Log.d(TAG, "Processing batch of ${batchOrders.size} orders...")
            
            val batchResult = inventoryService.handleBatchOrderCompletion(batchOrders, 1L)
            
            if (batchResult.isSuccess) {
                Log.d(TAG, "Batch processing completed successfully")
            } else {
                Log.e(TAG, "Batch processing failed: ${batchResult.exceptionOrNull()?.message}")
            }
            
            Result.success("Batch processing demo completed. Result: ${if (batchResult.isSuccess) "Success" else "Failed"}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during batch processing demonstration", e)
            Result.failure(e)
        }
    }
}