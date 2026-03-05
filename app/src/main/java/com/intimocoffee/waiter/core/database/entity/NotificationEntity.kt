package com.intimocoffee.waiter.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notifications",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["relatedOrderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["relatedOrderId"]),
        Index(value = ["targetDevice"]),
        Index(value = ["targetUserId"]),
        Index(value = ["createdAt"]),
        Index(value = ["isRead", "targetDevice"])
    ]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // NotificationType as string
    val title: String,
    val message: String,
    val relatedOrderId: Long? = null,
    val relatedOrderNumber: String? = null,
    val targetDevice: String, // "KITCHEN", "BAR", "WAITER", "ALL"
    val targetUserId: Long? = null,
    val priority: String, // NotificationPriority as string
    val isRead: Boolean = false,
    val isDelivered: Boolean = false,
    val createdAt: String, // LocalDateTime as ISO string
    val readAt: String? = null,
    val expiresAt: String? = null,
    val metadataJson: String? = null // JSON string of metadata map
)