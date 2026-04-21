package com.intimocoffee.waiter.feature.orders.domain.model

import kotlinx.datetime.LocalDateTime
import java.math.BigDecimal

data class Order(
    val id: Long = 0,
    val orderNumber: String,
    val tableId: Long? = null,
    val tableName: String? = null,
    val customerName: String? = null,
    val status: OrderStatus,
    val items: List<OrderItem>,
    val subtotal: BigDecimal,
    val tax: BigDecimal,
    val total: BigDecimal,
    val discount: BigDecimal = BigDecimal.ZERO,
    val notes: String? = null,
    val originalOrderId: Long? = null, // Reference to original order if this is an additional order
    val isAdditionalOrder: Boolean = false, // Flag to identify additional orders
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val completedAt: LocalDateTime? = null,
    val createdBy: Long // User ID
)

data class OrderItem(
    val id: Long = 0,
    val orderId: Long,
    val productId: Long,
    val productDatabaseId: String? = null,
    val productName: String,
    val productPrice: BigDecimal,
    val quantity: Int,
    val subtotal: BigDecimal,
    val notes: String? = null,
    val categoryId: Long = 0, // Para dividir cocina/barra
    val itemStatus: OrderItemStatus = OrderItemStatus.PENDING,
    val sentToKitchenAt: LocalDateTime? = null,
    val preparationStartedAt: LocalDateTime? = null,
    val readyAt: LocalDateTime? = null,
    val deliveredAt: LocalDateTime? = null,
    val preparationTimeMinutes: Int? = null, // Tiempo real de preparación
    val createdAt: LocalDateTime
) {
    fun stockProductKey(): String =
        productDatabaseId?.takeIf { it.isNotBlank() } ?: productId.toString()

    val isFood: Boolean
        get() = categoryId in listOf(3L, 4L, 5L) // Categorías de alimentos
    
    val isDrink: Boolean 
        get() = categoryId in listOf(1L, 2L) // Categorías de bebidas
        
    val targetStation: String
        get() = if (isFood) "KITCHEN" else "BAR"
}

enum class OrderItemStatus(val displayName: String, val color: String) {
    PENDING("Pendiente", "#FFA500"),
    SENT_TO_STATION("Enviado", "#FF9800"),
    IN_PREPARATION("En Preparación", "#2196F3"),
    READY("Listo", "#4CAF50"),
    DELIVERED("Entregado", "#8BC34A"),
    CANCELLED("Cancelado", "#F44336");
    
    companion object {
        fun getValidTransitions(currentStatus: OrderItemStatus): List<OrderItemStatus> {
            return when (currentStatus) {
                PENDING -> listOf(SENT_TO_STATION, CANCELLED)
                SENT_TO_STATION -> listOf(IN_PREPARATION, CANCELLED)
                IN_PREPARATION -> listOf(READY, CANCELLED)
                READY -> listOf(DELIVERED, CANCELLED)
                DELIVERED -> emptyList()
                CANCELLED -> emptyList()
            }
        }
    }
}

enum class OrderStatus(val displayName: String, val color: String) {
    PENDING("Pendiente", "#FFA500"),
    SENT_TO_KITCHEN("Enviado a Cocina", "#FF9800"),
    SENT_TO_BAR("Enviado a Barra", "#FF9800"), 
    IN_PREPARATION("En Preparación", "#2196F3"),
    PREPARING("Preparando", "#2196F3"), // Legacy - mantener compatibilidad
    READY("Listo", "#4CAF50"),
    DELIVERED("Entregado", "#8BC34A"),
    PAID("Pagado", "#4CAF50"),
    ARCHIVED("Archivado", "#9E9E9E"),
    CANCELLED("Cancelado", "#F44336");

    companion object {
        fun getValidTransitions(currentStatus: OrderStatus): List<OrderStatus> {
            return when (currentStatus) {
                PENDING -> listOf(SENT_TO_KITCHEN, SENT_TO_BAR, PREPARING, CANCELLED) // Legacy support
                SENT_TO_KITCHEN -> listOf(IN_PREPARATION, CANCELLED)
                SENT_TO_BAR -> listOf(IN_PREPARATION, CANCELLED)
                IN_PREPARATION -> listOf(READY, CANCELLED)
                PREPARING -> listOf(READY, CANCELLED) // Legacy support
                READY -> listOf(DELIVERED, CANCELLED)
                DELIVERED -> listOf(PAID, CANCELLED)
                PAID -> listOf(ARCHIVED)
                ARCHIVED -> emptyList()
                CANCELLED -> emptyList()
            }
        }
        
        /**
         * Returns true if the order status is considered "completed" and cannot accept new items directly.
         * New items to completed orders should create additional orders instead.
         */
        fun isCompleted(status: OrderStatus): Boolean {
            return status == PAID || status == ARCHIVED || status == CANCELLED
        }
    }
}
