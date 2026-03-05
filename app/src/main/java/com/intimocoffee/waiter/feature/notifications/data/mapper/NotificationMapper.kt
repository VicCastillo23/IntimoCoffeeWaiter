package com.intimocoffee.waiter.feature.notifications.data.mapper

import com.intimocoffee.waiter.core.database.entity.NotificationEntity
import com.intimocoffee.waiter.feature.notifications.domain.model.Notification
import com.intimocoffee.waiter.feature.notifications.domain.model.NotificationPriority
import com.intimocoffee.waiter.feature.notifications.domain.model.NotificationType
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun Notification.toEntity(): NotificationEntity {
    return NotificationEntity(
        id = if (id == 0L) 0 else id,
        type = type.name,
        title = title,
        message = message,
        relatedOrderId = relatedOrderId,
        relatedOrderNumber = relatedOrderNumber,
        targetDevice = targetDevice,
        targetUserId = targetUserId,
        priority = priority.name,
        isRead = isRead,
        isDelivered = isDelivered,
        createdAt = createdAt.toString(),
        readAt = readAt?.toString(),
        expiresAt = expiresAt?.toString(),
        metadataJson = if (metadata.isNotEmpty()) {
            metadata.entries.joinToString(",") { "${it.key}:${it.value}" }
        } else null
    )
}

fun NotificationEntity.toModel(): Notification {
    return Notification(
        id = id,
        type = NotificationType.valueOf(type),
        title = title,
        message = message,
        relatedOrderId = relatedOrderId,
        relatedOrderNumber = relatedOrderNumber,
        targetDevice = targetDevice,
        targetUserId = targetUserId,
        priority = NotificationPriority.valueOf(priority),
        isRead = isRead,
        isDelivered = isDelivered,
        createdAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()), // Parse from string later
        readAt = null, // Parse from string later
        expiresAt = null, // Parse from string later
        metadata = metadataJson?.split(",")?.mapNotNull {
            val parts = it.split(":")
            if (parts.size == 2) parts[0] to parts[1] else null
        }?.toMap() ?: emptyMap()
    )
}
