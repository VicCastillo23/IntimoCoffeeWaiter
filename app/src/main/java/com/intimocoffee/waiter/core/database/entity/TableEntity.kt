package com.intimocoffee.waiter.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tables")
data class TableEntity(
    @PrimaryKey
    val id: String,
    val number: Int,
    val name: String? = null,
    val capacity: Int = 4,
    val zone: String = "Main",
    val status: String = "FREE", // FREE, OCCUPIED, RESERVED, OUT_OF_SERVICE
    val currentOrderId: String? = null,
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)