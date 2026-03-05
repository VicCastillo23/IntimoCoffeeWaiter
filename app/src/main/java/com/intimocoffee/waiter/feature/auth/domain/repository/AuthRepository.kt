package com.intimocoffee.waiter.feature.auth.domain.repository

import com.intimocoffee.waiter.feature.auth.domain.model.User

interface AuthRepository {
    suspend fun login(username: String, password: String): User?
    suspend fun getCurrentUser(): User?
    suspend fun logout()
    suspend fun isLoggedIn(): Boolean
    suspend fun saveCurrentUser(user: User)
    suspend fun createDefaultUsers()
}