package com.intimocoffee.waiter.feature.auth.data.mapper

import com.intimocoffee.waiter.core.database.entity.UserEntity
import com.intimocoffee.waiter.feature.auth.domain.model.User
import com.intimocoffee.waiter.feature.auth.domain.model.UserRole

private fun String.toUserRoleSafe(): UserRole {
    return when (this.uppercase()) {
        "EMPLOYEE" -> UserRole.WAITER // Legacy mapping
        "WAITER" -> UserRole.WAITER
        "BARISTA" -> UserRole.BARISTA
        "COOK" -> UserRole.COOK
        "MANAGER" -> UserRole.MANAGER
        "ADMIN" -> UserRole.ADMIN
        else -> runCatching { UserRole.valueOf(this.uppercase()) }.getOrElse { UserRole.WAITER }
    }
}

fun UserEntity.toDomainModel(): User {
    return User(
        id = id,
        username = username,
        password = "", // Don't expose password in domain model
        fullName = fullName,
        email = email,
        phone = phone,
        role = role.toUserRoleSafe(),
        isActive = isActive,
        hireDate = hireDate,
        salary = salary,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun User.toEntity(): UserEntity {
    return UserEntity(
        id = id,
        username = username,
        password = password, // This should be hashed before storing
        fullName = fullName,
        email = email,
        phone = phone,
        role = role.name,
        isActive = isActive,
        hireDate = hireDate,
        salary = salary,
        createdAt = if (createdAt == 0L) System.currentTimeMillis() else createdAt,
        updatedAt = System.currentTimeMillis()
    )
}