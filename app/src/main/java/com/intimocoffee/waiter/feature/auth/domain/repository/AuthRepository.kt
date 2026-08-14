package com.intimocoffee.waiter.feature.auth.domain.repository

import com.intimocoffee.waiter.feature.auth.domain.model.User

interface AuthRepository {
    /** [Result] distingue fallo de red/configuración de credenciales incorrectas. */
    suspend fun login(username: String, password: String): Result<User>
    suspend fun getCurrentUser(): User?
    suspend fun logout()
    suspend fun isLoggedIn(): Boolean
    /**
     * Revalidates the DataStore session against POS `/api/login/me`.
     * On failure clears the local session and returns false.
     */
    suspend fun revalidateSession(): Boolean
    suspend fun saveCurrentUser(user: User)
    suspend fun createDefaultUsers()
}