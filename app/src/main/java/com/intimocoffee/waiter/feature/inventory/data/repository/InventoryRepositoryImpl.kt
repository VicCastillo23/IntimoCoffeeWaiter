package com.intimocoffee.waiter.feature.inventory.data.repository

import com.intimocoffee.waiter.feature.inventory.domain.repository.InventoryRepository
import com.intimocoffee.waiter.feature.inventory.domain.model.*
import com.intimocoffee.waiter.feature.products.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import javax.inject.Inject

class InventoryRepositoryImpl @Inject constructor(
    private val productRepository: ProductRepository
) : InventoryRepository {

    // In-memory storage for demo (in real app, this would be in database)
    private val stockMovements = mutableListOf<StockMovement>()
    private val purchaseOrders = mutableListOf<PurchaseOrder>()

    private fun getTodayDate(): LocalDate {
        return Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }
    
    override suspend fun getStockSummary(date: LocalDate): StockSummary {
        val allProducts = productRepository.getAllActiveProducts().first()
        
        // Filtrar solo productos de alimentación (categorías 3, 4, 5)
        // Categorías 1 y 2 son bebidas, 3, 4, 5 son alimentos
        val products = allProducts.filter { product ->
            product.categoryId in listOf(3L, 4L, 5L)
        }
        
        val totalProducts = products.size
        val productsInStock = products.count { (it.stockQuantity ?: 0) > 0 }
        val productsLowStock = products.count { it.isLowStock }
        val productsOutOfStock = products.count { (it.stockQuantity ?: 0) == 0 }
        
        val totalStockValue = products.fold(BigDecimal.ZERO) { acc, product ->
            val stock = product.stockQuantity ?: 0
            // For demo, assume unit cost is 60% of selling price
            val unitCost = product.price.multiply(BigDecimal("0.6"))
            acc.add(unitCost.multiply(BigDecimal(stock)))
        }
        
        val todayMovements = stockMovements.count { 
            it.createdAt.date == date 
        }
        
        val criticalAlerts = products.count { 
            (it.stockQuantity ?: 0) == 0 || 
            ((it.stockQuantity ?: 0) <= ((it.minStockLevel ?: 5) / 2))
        }

        return StockSummary(
            date = date,
            totalProducts = totalProducts,
            productsInStock = productsInStock,
            productsLowStock = productsLowStock,
            productsOutOfStock = productsOutOfStock,
            totalStockValue = totalStockValue,
            criticalAlerts = criticalAlerts,
            todayMovements = todayMovements
        )
    }
    
    override suspend fun getTodayStockSummary(): StockSummary {
        return getStockSummary(getTodayDate())
    }

    override fun getStockSummaryFlow(date: LocalDate): Flow<StockSummary> = flow {
        emit(getStockSummary(date))
    }
    
    override fun getTodayStockSummaryFlow(): Flow<StockSummary> = flow {
        emit(getTodayStockSummary())
    }

    override fun getStockAlerts(): Flow<List<StockAlert>> = flow {
        val alerts = generateLowStockAlerts()
        emit(alerts)
    }

    override suspend fun getStockAlertsForProduct(productId: Long): List<StockAlert> {
        val product = productRepository.getProductById(productId)
        if (product == null) return emptyList()
        
        val alerts = mutableListOf<StockAlert>()
        val currentStock = product.stockQuantity ?: 0
        val minStock = product.minStockLevel ?: 5
        
        when {
            currentStock == 0 -> {
                alerts.add(StockAlert(
                    productId = product.id,
                    productName = product.name,
                    currentStock = currentStock,
                    minStockLevel = minStock,
                    alertType = StockAlertType.OUT_OF_STOCK,
                    suggestedOrder = minStock * 3
                ))
            }
            currentStock <= minStock / 2 -> {
                alerts.add(StockAlert(
                    productId = product.id,
                    productName = product.name,
                    currentStock = currentStock,
                    minStockLevel = minStock,
                    alertType = StockAlertType.CRITICAL,
                    daysUntilEmpty = 1,
                    suggestedOrder = minStock * 2
                ))
            }
            currentStock <= minStock -> {
                alerts.add(StockAlert(
                    productId = product.id,
                    productName = product.name,
                    currentStock = currentStock,
                    minStockLevel = minStock,
                    alertType = StockAlertType.LOW_STOCK,
                    daysUntilEmpty = 3,
                    suggestedOrder = minStock * 2
                ))
            }
        }
        
        return alerts
    }

    override fun getAllStockMovements(): Flow<List<StockMovement>> = flow {
        emit(stockMovements.sortedByDescending { it.createdAt })
    }

    override fun getStockMovementsByProduct(productId: Long): Flow<List<StockMovement>> = flow {
        val productMovements = stockMovements
            .filter { it.productId == productId }
            .sortedByDescending { it.createdAt }
        emit(productMovements)
    }

    override fun getStockMovementsByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<StockMovement>> = flow {
        val filteredMovements = stockMovements
            .filter { it.createdAt.date >= startDate && it.createdAt.date <= endDate }
            .sortedByDescending { it.createdAt }
        emit(filteredMovements)
    }

    override suspend fun addStockMovement(movement: StockMovement): Boolean {
        return try {
            val newMovement = movement.copy(
                id = (stockMovements.maxOfOrNull { it.id } ?: 0) + 1,
                createdAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            )
            stockMovements.add(newMovement)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun adjustStock(adjustment: StockAdjustment, userId: Long): Boolean {
        return try {
            val product = productRepository.getProductById(adjustment.productId) ?: return false
            val currentStock = product.stockQuantity ?: 0
            
            // Create stock movement record
            val movement = StockMovement(
                productId = adjustment.productId,
                productName = product.name,
                movementType = StockMovementType.ADJUSTMENT,
                quantity = adjustment.newQuantity - currentStock,
                previousStock = currentStock,
                newStock = adjustment.newQuantity,
                unitCost = adjustment.unitCost,
                totalCost = adjustment.unitCost?.multiply(BigDecimal(kotlin.math.abs(adjustment.newQuantity - currentStock))),
                reason = adjustment.reason,
                notes = adjustment.notes,
                createdBy = userId,
                createdAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            )
            
            // Add the movement record
            addStockMovement(movement)
            
            // ACTUALLY UPDATE THE PRODUCT STOCK
            val updatedProduct = product.copy(stockQuantity = adjustment.newQuantity)
            val updateSuccess = productRepository.updateProduct(updatedProduct)
            
            if (updateSuccess) {
                android.util.Log.d("InventoryRepository", "Successfully updated stock for product ${product.name} from $currentStock to ${adjustment.newQuantity}")
                true
            } else {
                android.util.Log.e("InventoryRepository", "Failed to update product stock for ${product.name}")
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("InventoryRepository", "Error adjusting stock: ${e.message}", e)
            false
        }
    }

    override suspend fun adjustMultipleStocks(adjustments: List<StockAdjustment>, userId: Long): Boolean {
        return adjustments.all { adjustStock(it, userId) }
    }

    override suspend fun getProductStockDetails(productId: Long): ProductStockDetails? {
        val product = productRepository.getProductById(productId) ?: return null
        val movements = stockMovements.filter { it.productId == productId }.takeLast(10)
        val alerts = getStockAlertsForProduct(productId)
        
        val currentStock = product.stockQuantity ?: 0
        val minStock = product.minStockLevel ?: 5
        val unitCost = product.price.multiply(BigDecimal("0.6")) // Demo: assume 60% of price is cost
        
        return ProductStockDetails(
            productId = product.id,
            productName = product.name,
            currentStock = currentStock,
            minStockLevel = minStock,
            maxStockLevel = minStock * 3, // Demo: max is 3x min
            unitCost = unitCost,
            totalValue = unitCost.multiply(BigDecimal(currentStock)),
            recentMovements = movements,
            averageDailySales = 2.0, // Demo value
            daysOfStock = if (currentStock > 0) currentStock / 2 else null, // Demo calculation
            alerts = alerts
        )
    }

    override fun getAllProductsStock(): Flow<List<ProductStockDetails>> = flow {
        val products = productRepository.getAllActiveProducts().first()
        
        // Filtrar solo productos de alimentación (categorías 3, 4, 5)
        // Categorías 1 y 2 son bebidas, 3, 4, 5 son alimentos
        val foodProducts = products.filter { product ->
            product.categoryId in listOf(3L, 4L, 5L)
        }
        
        val stockDetails = foodProducts.mapNotNull { product ->
            getProductStockDetails(product.id)
        }
        emit(stockDetails)
    }

    override fun getAllPurchaseOrders(): Flow<List<PurchaseOrder>> = flow {
        emit(purchaseOrders.sortedByDescending { it.orderedDate })
    }

    override suspend fun createPurchaseOrder(purchaseOrder: PurchaseOrder): Long {
        val newId = (purchaseOrders.maxOfOrNull { it.id } ?: 0) + 1
        val newOrder = purchaseOrder.copy(id = newId)
        purchaseOrders.add(newOrder)
        return newId
    }

    override suspend fun updatePurchaseOrderStatus(orderId: Long, status: PurchaseOrderStatus): Boolean {
        val orderIndex = purchaseOrders.indexOfFirst { it.id == orderId }
        return if (orderIndex >= 0) {
            val updatedOrder = purchaseOrders[orderIndex].copy(status = status)
            purchaseOrders[orderIndex] = updatedOrder
            true
        } else {
            false
        }
    }

    override suspend fun receivePurchaseOrder(orderId: Long, receivedItems: List<PurchaseOrderItem>): Boolean {
        val order = purchaseOrders.find { it.id == orderId } ?: return false
        
        // Create stock movements for received items
        receivedItems.forEach { item ->
            val product = productRepository.getProductById(item.productId)
            if (product != null) {
                val movement = StockMovement(
                    productId = item.productId,
                    productName = item.productName,
                    movementType = StockMovementType.PURCHASE,
                    quantity = item.quantity,
                    previousStock = product.stockQuantity ?: 0,
                    newStock = (product.stockQuantity ?: 0) + item.quantity,
                    unitCost = item.unitCost,
                    totalCost = item.totalCost,
                    reason = "Purchase Order #${order.orderNumber}",
                    notes = null,
                    createdBy = 1L, // Demo user ID
                    createdAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                )
                addStockMovement(movement)
            }
        }
        
        return updatePurchaseOrderStatus(orderId, PurchaseOrderStatus.RECEIVED)
    }

    override suspend fun updateStockFromSale(productId: Long, quantity: Int, userId: Long): Boolean {
        val product = productRepository.getProductById(productId) ?: return false
        val currentStock = product.stockQuantity ?: 0
        
        val movement = StockMovement(
            productId = productId,
            productName = product.name,
            movementType = StockMovementType.SALE,
            quantity = -quantity, // Negative because it's a sale (outgoing)
            previousStock = currentStock,
            newStock = currentStock - quantity,
            unitCost = null,
            totalCost = null,
            reason = "Sold via POS",
            notes = null,
            createdBy = userId,
            createdAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        )
        
        return addStockMovement(movement)
    }

    override suspend fun updateStockFromSales(sales: Map<Long, Int>, userId: Long): Boolean {
        return sales.all { (productId, quantity) ->
            updateStockFromSale(productId, quantity, userId)
        }
    }

    override suspend fun validateStockAvailability(productId: Long, requiredQuantity: Int): Boolean {
        val product = productRepository.getProductById(productId) ?: return false
        return (product.stockQuantity ?: 0) >= requiredQuantity
    }

    override suspend fun validateMultipleStockAvailability(requirements: Map<Long, Int>): Map<Long, Boolean> {
        return requirements.mapValues { (productId, quantity) ->
            validateStockAvailability(productId, quantity)
        }
    }

    override suspend fun validateStockAvailabilityByDatabaseId(productKey: String, requiredQuantity: Int): Boolean {
        val product = productRepository.getProductByDatabaseId(productKey) ?: return false
        return (product.stockQuantity ?: 0) >= requiredQuantity
    }

    override suspend fun validateMultipleStockAvailabilityByDatabaseId(requirements: Map<String, Int>): Map<String, Boolean> {
        return requirements.mapValues { (key, quantity) ->
            validateStockAvailabilityByDatabaseId(key, quantity)
        }
    }

    override suspend fun generateLowStockAlerts(): List<StockAlert> {
        val allProducts = productRepository.getAllActiveProducts().first()
        
        // Filtrar solo productos de alimentación (categorías 3, 4, 5)
        // Categorías 1 y 2 son bebidas, 3, 4, 5 son alimentos
        val products = allProducts.filter { product ->
            product.categoryId in listOf(3L, 4L, 5L)
        }
        
        val alerts = mutableListOf<StockAlert>()
        
        products.forEach { product ->
            val productAlerts = getStockAlertsForProduct(product.id)
            alerts.addAll(productAlerts)
        }
        
        return alerts.sortedBy { alert ->
            when (alert.alertType) {
                StockAlertType.OUT_OF_STOCK -> 0
                StockAlertType.CRITICAL -> 1
                StockAlertType.LOW_STOCK -> 2
                StockAlertType.OVERSTOCKED -> 3
            }
        }
    }

    override suspend fun markAlertAsViewed(productId: Long): Boolean {
        // In a real implementation, this would mark the alert as viewed in the database
        return true
    }
}