package com.intimocoffee.waiter.feature.notifications.data.repository

import com.intimocoffee.waiter.core.database.dao.NotificationDao
import com.intimocoffee.waiter.feature.notifications.data.mapper.toEntity
import com.intimocoffee.waiter.feature.notifications.data.mapper.toModel
import com.intimocoffee.waiter.feature.notifications.domain.model.Notification
import com.intimocoffee.waiter.feature.notifications.domain.model.NotificationType
import com.intimocoffee.waiter.feature.notifications.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val notificationDao: NotificationDao
) : NotificationRepository {

    override suspend fun insertNotification(notification: Notification): Long {
        return notificationDao.insertNotification(notification.toEntity())
    }

    override suspend fun getNotificationById(id: Long): Notification? {
        return notificationDao.getNotificationById(id)?.toModel()
    }

    override suspend fun getAllNotifications(): List<Notification> {
        return notificationDao.getAllNotifications().map { it.toModel() }
    }

    override suspend fun getNotificationsByType(type: NotificationType): List<Notification> {
        return notificationDao.getNotificationsByType(type.name).map { it.toModel() }
    }

    override suspend fun getUnreadNotifications(): List<Notification> {
        return notificationDao.getUnreadNotifications().map { it.toModel() }
    }

    override suspend fun getNotificationsFlow(): Flow<List<Notification>> {
        return notificationDao.getNotificationsFlow().map { entities ->
            entities.map { it.toModel() }
        }
    }

    override suspend fun markAsRead(notificationId: Long) {
        notificationDao.markAsRead(notificationId, System.currentTimeMillis())
    }

    override suspend fun markAllAsRead() {
        notificationDao.markAllAsRead(System.currentTimeMillis())
    }

    override suspend fun deleteNotification(id: Long) {
        notificationDao.deleteNotification(id)
    }

    override suspend fun deleteAllNotifications() {
        notificationDao.deleteAllNotifications()
    }

    override suspend fun deleteOldNotifications(olderThanHours: Int) {
        val cutoffTime = System.currentTimeMillis() - (olderThanHours * 60 * 60 * 1000L)
        notificationDao.deleteOldNotifications(cutoffTime)
    }
}