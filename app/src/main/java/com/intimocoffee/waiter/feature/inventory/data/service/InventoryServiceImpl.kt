package com.intimocoffee.waiter.feature.inventory.data.service

import android.util.Log
import com.intimocoffee.waiter.feature.inventory.domain.service.InventoryService
import com.intimocoffee.waiter.feature.inventory.domain.service.StockAvailabilityReport
import com.intimocoffee.waiter.feature.inventory.domain.service.ItemStockStatus
import com.intimocoffee.waiter.feature.inventory.domain.repository.InventoryRepository
import com.intimocoffee.waiter.feature.inventory.domain.model.StockMovement
import com.intimocoffee.waiter.feature.inventory.domain.model.StockMovementType
import com.intimocoffee.waiter.feature.orders.domain.model.Order
import com.intimocoffee.waiter.feature.orders.domain.model.OrderItem
import com.intimocoffee.waiter.feature.orders.domain.model.OrderStatus
import com.intimocoffee.waiter.feature.products.domain.repository.ProductRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventoryServiceImpl @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val productRepository: ProductRepository
) : InventoryService {

    companion object {
        private const val TAG = "InventoryService"
    }

    override suspend fun handleOrderStatusChange(order: Order, newStatus: OrderStatus, userId: Long): Result<Unit> {
        return try {
            Log.d(TAG, "Handling order status change: Order ${order.id} from ${order.status} to $newStatus")
            
            when (newStatus) {
                OrderStatus.DELIVERED, OrderStatus.PAID -> {
                    // Only update stock if this is the first time the order is being completed
                    if (order.status != OrderStatus.DELIVERED && order.status != OrderStatus.PAID) {
                        updateStockForCompletedOrder(order, userId)
                        Log.d(TAG, "Stock updated for completed order ${order.id}")
                    } else {
                        Log.d(TAG, "Order ${order.id} was already completed, skipping stock update")
                    }
                }
                OrderStatus.CANCELLED -> {
                    // Only revert stock if the order was previously completed
                    if (order.status == OrderStatus.DELIVERED || order.status == OrderStatus.PAID) {
                        revertOrderStock(order, userId)
                        Log.d(TAG, "Stock reverted for cancelled order ${order.id}")
                    } else {
                        Log.d(TAG, "Order ${order.id} was not completed, no stock to revert")
                    }
                }
                else -> {
                    Log.d(TAG, "Status change to $newStatus does not require stock update")
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling order status change for order ${order.id}", e)
            Result.failure(e)
        }
    }

    override suspend fun handleOrderItemAdded(orderItem: OrderItem, userId: Long): Result<Unit> {
        return try {
            Log.d(TAG, "Handling order item added: Product ${orderItem.productId}, quantity ${orderItem.quantity}")
            
            // For now, we don't update stock when items are added to active orders
            // Stock is only updated when the order is completed
            // However, we could add stock reservation logic here in the future
            
            Log.d(TAG, "Order item added successfully (no immediate stock impact)")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling order item added: ${orderItem.productId}", e)
            Result.failure(e)
        }
    }

    override suspend fun handleOrderItemChanged(
        oldItem: OrderItem?,
        newItem: OrderItem?,
        userId: Long
    ): Result<Unit> {
        return try {
            Log.d(TAG, "Handling order item change")
            
            // For now, we don't update stock when items are modified in active orders
            // Stock is only updated when the order is completed
            // However, we could add stock reservation logic here in the future
            
            Log.d(TAG, "Order item changed successfully (no immediate stock impact)")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling order item change", e)
            Result.failure(e)
        }
    }

    override suspend fun validateOrderStockAvailability(items: List<OrderItem>): Result<Map<String, Boolean>> {
        return try {
            val requirements = items.groupBy { it.stockProductKey() }
                .mapValues { (_, itemList) -> itemList.sumOf { it.quantity } }

            val availability = inventoryRepository.validateMultipleStockAvailabilityByDatabaseId(requirements)
            Log.d(TAG, "Stock availability validated for ${items.size} items")

            Result.success(availability)
        } catch (e: Exception) {
            Log.e(TAG, "Error validating stock availability", e)
            Result.failure(e)
        }
    }

    override suspend fun revertOrderStock(order: Order, userId: Long): Result<Unit> {
        return try {
            Log.d(TAG, "Reverting stock for order ${order.id}")
            
            order.items.forEach { item ->
                val product = productRepository.getProductByDatabaseId(item.stockProductKey())
                    ?: (if (item.productId != 0L) productRepository.getProductById(item.productId) else null)
                if (product != null) {
                    val currentStock = product.stockQuantity ?: 0
                    val newStock = currentStock + item.quantity // Add back the quantity
                    
                    // Create stock movement for the reversion
                    val movement = StockMovement(
                        productId = product.id,
                        productName = item.productName,
                        movementType = StockMovementType.ADJUSTMENT,
                        quantity = item.quantity, // Positive quantity (adding back)
                        previousStock = currentStock,
                        newStock = newStock,
                        unitCost = null,
                        totalCost = null,
                        reason = "Order cancelled - stock reverted",
                        notes = "Reverted from Order #${order.orderNumber}",
                        createdBy = userId,
                        createdAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    )
                    
                    inventoryRepository.addStockMovement(movement)
                    
                    // Update product stock
                    val updatedProduct = product.copy(stockQuantity = newStock)
                    productRepository.updateProduct(updatedProduct)
                    
                    Log.d(TAG, "Reverted stock for product ${item.productId}: +${item.quantity} (new stock: $newStock)")
                } else {
                    Log.w(TAG, "Product ${item.productId} not found for stock reversion")
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error reverting stock for order ${order.id}", e)
            Result.failure(e)
        }
    }

    override suspend fun handleBatchOrderCompletion(orders: List<Order>, userId: Long): Result<Unit> {
        return try {
            Log.d(TAG, "Handling batch completion of ${orders.size} orders")
            
            val results = orders.map { order ->
                handleOrderStatusChange(order, OrderStatus.DELIVERED, userId)
            }
            
            val failures = results.filter { it.isFailure }
            if (failures.isNotEmpty()) {
                Log.w(TAG, "Some orders failed in batch completion: ${failures.size} failures")
                Result.failure(Exception("${failures.size} orders failed to complete"))
            } else {
                Log.d(TAG, "All ${orders.size} orders completed successfully")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in batch order completion", e)
            Result.failure(e)
        }
    }

    override suspend fun getStockAvailabilityReport(items: List<OrderItem>): StockAvailabilityReport {
        return try {
            val itemAvailability = mutableMapOf<Long, ItemStockStatus>()
            val warnings = mutableListOf<String>()
            var allAvailable = true

            val requirements = items.groupBy { it.stockProductKey() }
                .mapValues { (_, itemList) ->
                    itemList.sumOf { it.quantity } to itemList.first().productName
                }

            requirements.forEach { (productKey, requirementInfo) ->
                val (requiredQuantity, productName) = requirementInfo
                val product = productRepository.getProductByDatabaseId(productKey)
                    ?: productKey.toLongOrNull()?.let { productRepository.getProductById(it) }

                val mapKey = productKey.toLongOrNull() ?: kotlin.math.abs(productKey.hashCode().toLong())

                if (product != null) {
                    val availableStock = product.stockQuantity ?: 0
                    val minStockLevel = product.minStockLevel ?: 5
                    val isAvailable = availableStock >= requiredQuantity
                    val isLowStock = availableStock <= minStockLevel
                    val willBeOutOfStock = (availableStock - requiredQuantity) <= 0

                    itemAvailability[mapKey] = ItemStockStatus(
                        productId = product.id,
                        productName = productName,
                        requestedQuantity = requiredQuantity,
                        availableQuantity = availableStock,
                        isAvailable = isAvailable,
                        isLowStock = isLowStock,
                        willBeOutOfStock = willBeOutOfStock
                    )

                    if (!isAvailable) {
                        allAvailable = false
                        warnings.add("Insufficient stock for $productName (need $requiredQuantity, have $availableStock)")
                    } else if (willBeOutOfStock) {
                        warnings.add("$productName will be out of stock after this order")
                    } else if (isLowStock) {
                        warnings.add("$productName is already low in stock")
                    }
                } else {
                    allAvailable = false
                    warnings.add("Product with ID $productKey not found")
                    itemAvailability[mapKey] = ItemStockStatus(
                        productId = mapKey,
                        productName = productName,
                        requestedQuantity = requiredQuantity,
                        availableQuantity = 0,
                        isAvailable = false,
                        isLowStock = false,
                        willBeOutOfStock = true
                    )
                }
            }
            
            StockAvailabilityReport(
                isAvailable = allAvailable,
                itemAvailability = itemAvailability,
                warnings = warnings
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error generating stock availability report", e)
            StockAvailabilityReport(
                isAvailable = false,
                itemAvailability = emptyMap(),
                warnings = listOf("Error checking stock availability: ${e.message}")
            )
        }
    }
    
    /**
     * Private method to update stock when an order is completed.
     */
    private suspend fun updateStockForCompletedOrder(order: Order, userId: Long) {
        order.items.forEach { item ->
            val product = productRepository.getProductByDatabaseId(item.stockProductKey())
                ?: (if (item.productId != 0L) productRepository.getProductById(item.productId) else null)
            if (product != null) {
                val currentStock = product.stockQuantity ?: 0
                val newStock = maxOf(0, currentStock - item.quantity) // Ensure stock doesn't go negative
                
                // Create stock movement for the sale
                val movement = StockMovement(
                    productId = product.id,
                    productName = item.productName,
                    movementType = StockMovementType.SALE,
                    quantity = -item.quantity, // Negative quantity for sale (outgoing)
                    previousStock = currentStock,
                    newStock = newStock,
                    unitCost = null,
                    totalCost = null,
                    reason = "Sale - Order completed",
                    notes = "From Order #${order.orderNumber}",
                    createdBy = userId,
                    createdAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                )
                
                inventoryRepository.addStockMovement(movement)
                
                // Update product stock
                val updatedProduct = product.copy(stockQuantity = newStock)
                productRepository.updateProduct(updatedProduct)
                
                Log.d(TAG, "Updated stock for product ${item.productId}: -${item.quantity} (new stock: $newStock)")
            } else {
                Log.w(TAG, "Product ${item.productId} not found for stock update")
            }
        }
    }
}