package com.intimocoffee.waiter.feature.auth.domain.repository

import com.intimocoffee.waiter.feature.auth.domain.model.User
import com.intimocoffee.waiter.feature.auth.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    
    fun getAllActiveUsers(): Flow<List<User>>
    
    fun getUsersByRole(role: UserRole): Flow<List<User>>
    
    fun getUsersByRoles(roles: List<UserRole>): Flow<List<User>>
    
    suspend fun getUserById(userId: Long): User?
    
    suspend fun getUserByUsername(username: String): User?
    
    suspend fun authenticateUser(username: String, password: String): User?
    
    suspend fun createUser(user: User): Long
    
    suspend fun updateUser(user: User): Boolean
    
    suspend fun deactivateUser(userId: Long): Boolean
    
    suspend fun deleteUser(userId: Long): Boolean
    
    suspend fun getActiveUsersCount(): Int
    
    suspend fun getUsersCountByRole(role: UserRole): Int
    
    suspend fun changeUserPassword(userId: Long, newPassword: String): Boolean
}