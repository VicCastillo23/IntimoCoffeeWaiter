package com.intimocoffee.waiter.feature.notifications.domain.model

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class Notification(
    val id: Long = 0,
    val type: NotificationType,
    val title: String,
    val message: String,
    val relatedOrderId: Long? = null,
    val relatedOrderNumber: String? = null,
    val targetDevice: String, // "KITCHEN", "BAR", "WAITER", "ALL"
    val targetUserId: Long? = null, // Específico para un usuario
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val isRead: Boolean = false,
    val isDelivered: Boolean = false,
    val createdAt: LocalDateTime,
    val readAt: LocalDateTime? = null,
    val expiresAt: LocalDateTime? = null,
    val metadata: Map<String, String> = emptyMap() // Para datos adicionales
) {
    val isExpired: Boolean
        get() = expiresAt?.let { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) > it } ?: false
        
    val displayTime: String
        get() = createdAt.toString() // TODO: Format appropriately
}

enum class NotificationType(val displayName: String, val icon: String) {
    NEW_ORDER("Nueva Orden", "🆕"),
    NEW_ORDER_KITCHEN("Nueva Orden - Cocina", "🍳"),
    NEW_ORDER_BAR("Nueva Orden - Barra", "🍹"),
    ORDER_READY("Orden Lista", "✅"),
    ORDER_DELAYED("Orden Retrasada", "⏰"),
    KITCHEN_ALERT("Alerta Cocina", "🍳"),
    BAR_ALERT("Alerta Barra", "🍹"),
    WAITER_ALERT("Alerta Mesero", "👨‍🍽️"),
    SYSTEM_ALERT("Alerta Sistema", "⚠️"),
    LOW_STOCK("Stock Bajo", "📦"),
    INVENTORY_LOW("Stock Bajo", "📦"),
    CUSTOMER_WAITING("Cliente Esperando", "⏳")
}

enum class NotificationPriority(val level: Int, val displayName: String, val color: String) {
    LOW(1, "Baja", "#4CAF50"),
    NORMAL(2, "Normal", "#2196F3"),
    MEDIUM(2, "Media", "#2196F3"),
    HIGH(3, "Alta", "#FF9800"),
    URGENT(4, "Urgente", "#F44336")
}

data class NotificationAction(
    val id: String,
    val label: String,
    val action: String, // "MARK_READY", "START_PREPARATION", etc.
    val parameters: Map<String, String> = emptyMap()
)

// Extension para crear notificaciones comunes
object NotificationFactory {
    fun createNewOrderNotification(
        orderId: Long,
        orderNumber: String,
        targetDevice: String,
        itemCount: Int
    ): Notification {
        return Notification(
            type = NotificationType.NEW_ORDER,
            title = "Nueva Orden - $orderNumber",
            message = "$itemCount items para preparar",
            relatedOrderId = orderId,
            relatedOrderNumber = orderNumber,
            targetDevice = targetDevice,
            priority = NotificationPriority.HIGH,
            createdAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            metadata = mapOf(
                "itemCount" to itemCount.toString(),
                "station" to targetDevice
            )
        )
    }
    
    fun createOrderReadyNotification(
        orderId: Long,
        orderNumber: String,
        tableName: String?,
        preparationTime: Int?
    ): Notification {
        return Notification(
            type = NotificationType.ORDER_READY,
            title = "Orden Lista - $orderNumber",
            message = "Lista para entregar${tableName?.let { " a $it" } ?: ""}",
            relatedOrderId = orderId,
            relatedOrderNumber = orderNumber,
            targetDevice = "WAITER",
            priority = NotificationPriority.HIGH,
            createdAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            metadata = buildMap {
                put("orderNumber", orderNumber)
                tableName?.let { put("tableName", it) }
                preparationTime?.let { put("preparationTime", it.toString()) }
            }
        )
    }
}