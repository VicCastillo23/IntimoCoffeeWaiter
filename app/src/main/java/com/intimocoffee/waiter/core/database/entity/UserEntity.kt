package com.intimocoffee.waiter.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val username: String,
    val password: String, // In production, this should be hashed
    val fullName: String,
    val email: String?,
    val phone: String?,
    val role: String, // ADMIN, MANAGER, WAITER, BARISTA, COOK
    val isActive: Boolean = true,
    val hireDate: String?,
    val salary: Double?,
    val createdAt: Long,
    val updatedAt: Long
)
