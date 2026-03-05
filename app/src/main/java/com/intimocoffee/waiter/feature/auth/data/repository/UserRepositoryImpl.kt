package com.intimocoffee.waiter.feature.auth.data.repository

import com.intimocoffee.waiter.core.database.dao.UserDao
import com.intimocoffee.waiter.feature.auth.data.mapper.toDomainModel
import com.intimocoffee.waiter.feature.auth.data.mapper.toEntity
import com.intimocoffee.waiter.feature.auth.domain.model.User
import com.intimocoffee.waiter.feature.auth.domain.model.UserRole
import com.intimocoffee.waiter.feature.auth.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao
) : UserRepository {
    
    override fun getAllActiveUsers(): Flow<List<User>> {
        return userDao.getAllActiveUsers().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override fun getUsersByRole(role: UserRole): Flow<List<User>> {
        return userDao.getUsersByRole(role.name).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override fun getUsersByRoles(roles: List<UserRole>): Flow<List<User>> {
        return userDao.getUsersByRoles(roles.map { it.name }).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override suspend fun getUserById(userId: Long): User? {
        return userDao.getUserById(userId)?.toDomainModel()
    }
    
    override suspend fun getUserByUsername(username: String): User? {
        return userDao.getUserByUsername(username)?.toDomainModel()
    }
    
    override suspend fun authenticateUser(username: String, password: String): User? {
        return userDao.authenticateUser(username, password)?.toDomainModel()
    }
    
    override suspend fun createUser(user: User): Long {
        return try {
            val hashedPassword = hashPassword(user.password) // TODO: Implement proper password hashing
            val userWithHashedPassword = user.copy(password = hashedPassword)
            userDao.insertUser(userWithHashedPassword.toEntity())
        } catch (e: Exception) {
            -1L
        }
    }
    
    override suspend fun updateUser(user: User): Boolean {
        return try {
            val existingUser = userDao.getUserById(user.id)
            if (existingUser != null) {
                val updatedEntity = if (user.password.isNotEmpty()) {
                    // Update password if provided
                    val hashedPassword = hashPassword(user.password)
                    user.copy(password = hashedPassword).toEntity()
                } else {
                    // Keep existing password
                    user.copy(password = existingUser.password).toEntity()
                }
                userDao.updateUser(updatedEntity)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun deactivateUser(userId: Long): Boolean {
        return try {
            userDao.deactivateUser(userId, System.currentTimeMillis())
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun deleteUser(userId: Long): Boolean {
        return try {
            val user = userDao.getUserById(userId)
            if (user != null) {
                userDao.deleteUser(user)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun getActiveUsersCount(): Int {
        return userDao.getActiveUsersCount()
    }
    
    override suspend fun getUsersCountByRole(role: UserRole): Int {
        return userDao.getUsersCountByRole(role.name)
    }
    
    override suspend fun changeUserPassword(userId: Long, newPassword: String): Boolean {
        return try {
            val user = userDao.getUserById(userId)
            if (user != null) {
                val hashedPassword = hashPassword(newPassword)
                val updatedUser = user.copy(
                    password = hashedPassword,
                    updatedAt = System.currentTimeMillis()
                )
                userDao.updateUser(updatedUser)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    private fun hashPassword(password: String): String {
        // TODO: Implement proper password hashing (e.g., BCrypt)
        // For now, just return the password (NOT SECURE - for demo only)
        return password
    }
}