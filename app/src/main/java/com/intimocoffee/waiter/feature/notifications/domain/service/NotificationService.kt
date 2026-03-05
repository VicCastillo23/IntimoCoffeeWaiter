package com.intimocoffee.waiter.feature.notifications.domain.service

import com.intimocoffee.waiter.feature.notifications.domain.model.Notification
import com.intimocoffee.waiter.feature.notifications.domain.model.NotificationPriority
import com.intimocoffee.waiter.feature.notifications.domain.model.NotificationType
import com.intimocoffee.waiter.feature.notifications.domain.repository.NotificationRepository
import com.intimocoffee.waiter.feature.orders.domain.model.Order
import com.intimocoffee.waiter.feature.orders.domain.model.OrderItem
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationService @Inject constructor(
    private val notificationRepository: NotificationRepository
) {

    suspend fun notifyNewOrder(order: Order) {
        val kitchenItems = order.items.filter { isKitchenItem(it) }
        val barItems = order.items.filter { isBarItem(it) }

        // Notificar a cocina si hay items de cocina
        if (kitchenItems.isNotEmpty()) {
            val notification = Notification(
                id = 0,
                type = NotificationType.NEW_ORDER_KITCHEN,
                title = "Nueva orden para Cocina",
                message = "Orden #${order.id} - Mesa ${order.tableName} - ${kitchenItems.size} items",
                targetDevice = "KITCHEN",
                priority = NotificationPriority.HIGH,
                createdAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                isRead = false,
                metadata = mapOf(
                    "orderId" to order.id.toString(),
                    "tableId" to order.tableId.toString(),
                    "itemCount" to kitchenItems.size.toString(),
                    "customerName" to (order.customerName ?: "Mesa ${order.tableName}")
                )
            )
            notificationRepository.insertNotification(notification)
        }

        // Notificar a barra si hay items de barra
        if (barItems.isNotEmpty()) {
            val notification = Notification(
                id = 0,
                type = NotificationType.NEW_ORDER_BAR,
                title = "Nueva orden para Barra",
                message = "Orden #${order.id} - Mesa ${order.tableName} - ${barItems.size} items",
                targetDevice = "BAR",
                priority = NotificationPriority.HIGH,
                createdAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                isRead = false,
                metadata = mapOf(
                    "orderId" to order.id.toString(),
                    "tableId" to order.tableId.toString(),
                    "itemCount" to barItems.size.toString(),
                    "customerName" to (order.customerName ?: "Mesa ${order.tableName}")
                )
            )
            notificationRepository.insertNotification(notification)
        }
    }

    suspend fun notifyOrderReady(order: Order) {
        val notification = Notification(
            id = 0,
            type = NotificationType.ORDER_READY,
            title = "Orden lista para servir",
            message = "Orden #${order.id} - Mesa ${order.tableName} lista",
            targetDevice = "WAITER",
            priority = NotificationPriority.HIGH,
            createdAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            isRead = false,
            metadata = mapOf(
                "orderId" to order.id.toString(),
                "tableId" to order.tableId.toString(),
                "customerName" to (order.customerName ?: "Mesa ${order.tableName}")
            )
        )
        notificationRepository.insertNotification(notification)
    }

    suspend fun notifyOrderDelayed(order: Order, delayMinutes: Long) {
        val notification = Notification(
            id = 0,
            type = NotificationType.ORDER_DELAYED,
            title = "Orden demorada",
            message = "Orden #${order.id} - Mesa ${order.tableName} demorada ${delayMinutes}min",
            targetDevice = "ALL",
            priority = NotificationPriority.URGENT,
            createdAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            isRead = false,
            metadata = mapOf(
                "orderId" to order.id.toString(),
                "tableId" to order.tableId.toString(),
                "delayMinutes" to delayMinutes.toString(),
                "customerName" to (order.customerName ?: "Mesa ${order.tableName}")
            )
        )
        notificationRepository.insertNotification(notification)
    }

    suspend fun notifyLowStock(productName: String, currentStock: Int, minStock: Int) {
        val notification = Notification(
            id = 0,
            type = NotificationType.LOW_STOCK,
            title = "Stock bajo",
            message = "$productName: $currentStock unidades (mínimo: $minStock)",
            targetDevice = "ALL",
            priority = NotificationPriority.MEDIUM,
            createdAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            isRead = false,
            metadata = mapOf(
                "productName" to productName,
                "currentStock" to currentStock.toString(),
                "minStock" to minStock.toString()
            )
        )
        notificationRepository.insertNotification(notification)
    }

    suspend fun getNotificationsFlow(): Flow<List<Notification>> {
        return notificationRepository.getNotificationsFlow()
    }

    suspend fun markAsRead(notificationId: Long) {
        notificationRepository.markAsRead(notificationId)
    }

    suspend fun markAllAsRead() {
        notificationRepository.markAllAsRead()
    }

    suspend fun deleteOldNotifications(olderThanHours: Int = 24) {
        notificationRepository.deleteOldNotifications(olderThanHours)
    }

    private fun isKitchenItem(item: OrderItem): Boolean {
        // Por ahora, asumimos que los items con categoryId 3,4,5 son de cocina
        // TODO: Mejorar esta lógica cuando tengamos acceso a ProductCategory
        return item.categoryId in listOf(3L, 4L, 5L)
    }

    private fun isBarItem(item: OrderItem): Boolean {
        // Por ahora, asumimos que los items con categoryId 1,2 son de barra  
        // TODO: Mejorar esta lógica cuando tengamos acceso a ProductCategory
        return item.categoryId in listOf(1L, 2L)
    }
}