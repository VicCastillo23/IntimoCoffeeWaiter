package com.intimocoffee.waiter.feature.notifications.domain.repository

import com.intimocoffee.waiter.feature.notifications.domain.model.Notification
import com.intimocoffee.waiter.feature.notifications.domain.model.NotificationType
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    suspend fun insertNotification(notification: Notification): Long
    suspend fun getNotificationById(id: Long): Notification?
    suspend fun getAllNotifications(): List<Notification>
    suspend fun getNotificationsByType(type: NotificationType): List<Notification>
    suspend fun getUnreadNotifications(): List<Notification>
    suspend fun getNotificationsFlow(): Flow<List<Notification>>
    suspend fun markAsRead(notificationId: Long)
    suspend fun markAllAsRead()
    suspend fun deleteNotification(id: Long)
    suspend fun deleteAllNotifications()
    suspend fun deleteOldNotifications(olderThanHours: Int = 24)
}