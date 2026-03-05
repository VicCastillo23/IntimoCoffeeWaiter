package com.intimocoffee.waiter.feature.inventory.domain.model

import kotlinx.datetime.LocalDateTime
import java.math.BigDecimal

/**
 * Represents a stock movement (entry, exit, adjustment, etc.)
 */
data class StockMovement(
    val id: Long = 0,
    val productId: Long,
    val productName: String,
    val movementType: StockMovementType,
    val quantity: Int,
    val previousStock: Int,
    val newStock: Int,
    val unitCost: BigDecimal?,
    val totalCost: BigDecimal?,
    val reason: String?,
    val notes: String?,
    val createdBy: Long,
    val createdAt: LocalDateTime
)

/**
 * Types of stock movements
 */
enum class StockMovementType(val displayName: String, val icon: String, val color: String) {
    PURCHASE("Compra", "📦", "#4CAF50"),
    SALE("Venta", "🛒", "#2196F3"),
    ADJUSTMENT("Ajuste", "⚖️", "#FF9800"),
    WASTE("Merma", "🗑️", "#F44336"),
    RETURN("Devolución", "↩️", "#9C27B0"),
    TRANSFER("Transferencia", "🔄", "#607D8B")
}

/**
 * Stock alert information
 */
data class StockAlert(
    val productId: Long,
    val productName: String,
    val currentStock: Int,
    val minStockLevel: Int,
    val alertType: StockAlertType,
    val daysUntilEmpty: Int? = null,
    val suggestedOrder: Int? = null
)

/**
 * Types of stock alerts
 */
enum class StockAlertType(val displayName: String, val color: String) {
    LOW_STOCK("Stock Bajo", "#FF9800"),
    OUT_OF_STOCK("Sin Stock", "#F44336"),
    CRITICAL("Crítico", "#D32F2F"),
    OVERSTOCKED("Sobrestock", "#607D8B")
}

/**
 * Stock adjustment request
 */
data class StockAdjustment(
    val productId: Long,
    val newQuantity: Int,
    val reason: String,
    val notes: String? = null,
    val unitCost: BigDecimal? = null
)

/**
 * Purchase order for restocking
 */
data class PurchaseOrder(
    val id: Long = 0,
    val orderNumber: String,
    val supplierName: String?,
    val status: PurchaseOrderStatus,
    val items: List<PurchaseOrderItem>,
    val totalAmount: BigDecimal,
    val notes: String?,
    val orderedDate: LocalDateTime,
    val expectedDate: LocalDateTime?,
    val receivedDate: LocalDateTime?,
    val createdBy: Long
)

data class PurchaseOrderItem(
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitCost: BigDecimal,
    val totalCost: BigDecimal
)

enum class PurchaseOrderStatus(val displayName: String, val color: String) {
    DRAFT("Borrador", "#757575"),
    ORDERED("Ordenado", "#2196F3"),
    RECEIVED("Recibido", "#4CAF50"),
    CANCELLED("Cancelado", "#F44336")
}

/**
 * Daily stock summary
 */
data class StockSummary(
    val date: kotlinx.datetime.LocalDate,
    val totalProducts: Int,
    val productsInStock: Int,
    val productsLowStock: Int,
    val productsOutOfStock: Int,
    val totalStockValue: BigDecimal,
    val criticalAlerts: Int,
    val todayMovements: Int
)

/**
 * Product stock details with movement history
 */
data class ProductStockDetails(
    val productId: Long,
    val productName: String,
    val currentStock: Int,
    val minStockLevel: Int,
    val maxStockLevel: Int?,
    val unitCost: BigDecimal?,
    val totalValue: BigDecimal,
    val recentMovements: List<StockMovement>,
    val averageDailySales: Double?,
    val daysOfStock: Int?,
    val alerts: List<StockAlert>
)