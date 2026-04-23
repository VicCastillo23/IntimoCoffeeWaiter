package com.intimocoffee.waiter.feature.auth.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.intimocoffee.waiter.core.database.dao.UserDao
import com.intimocoffee.waiter.core.database.entity.UserEntity
import com.intimocoffee.waiter.core.network.DynamicRetrofitProvider
import com.intimocoffee.waiter.core.network.LoginRequest
import com.intimocoffee.waiter.core.network.LoginResponse
import com.intimocoffee.waiter.core.network.UserLoginResponse
import com.intimocoffee.waiter.feature.auth.data.mapper.toDomainModel
import com.intimocoffee.waiter.feature.auth.domain.model.User
import com.intimocoffee.waiter.feature.auth.domain.model.UserRole
import android.util.Log
import com.intimocoffee.waiter.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val dataStore: DataStore<Preferences>,
    private val retrofitProvider: DynamicRetrofitProvider,
) : AuthRepository {
    
    companion object {
        private val CURRENT_USER_KEY = stringPreferencesKey("current_user")
    }
    
    override suspend fun login(username: String, password: String): Result<User> {
        val trimmedUser = username.trim()
        if (trimmedUser.isEmpty()) {
            return Result.failure(IllegalArgumentException("Indica un usuario"))
        }
        return try {
            val service = retrofitProvider.discoverAndRefreshService()
            if (retrofitProvider.isUsingEmulatorLoopbackOnPhysicalDevice()) {
                return Result.failure(
                    Exception(
                        "No se encontró la tablet POS. En gradle.properties del proyecto mesero: " +
                            "INTIMO_MAIN_SERVER_URL=http://IP_DE_LA_TABLET:8080/ y Sync + recompilar."
                    )
                )
            }
            val response = service.login(LoginRequest(username = trimmedUser, password = password))

            if (response.isSuccessful) {
                val body: LoginResponse? = response.body()
                val userDto: UserLoginResponse? = body?.data
                if (body?.success == true && userDto != null && userDto.isActive) {
                    Result.success(userDto.toDomainModel())
                } else {
                    Result.failure(Exception("Usuario o contraseña incorrectos"))
                }
            } else {
                Result.failure(Exception("Usuario o contraseña incorrectos"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "login failed (¿servidor IntimoCoffeeApp en la misma WiFi?)", e)
            Result.failure(
                Exception(
                    "No se pudo conectar con la tablet POS (misma Wi‑Fi, app cafetería abierta).",
                    e
                )
            )
        }
    }
    
    override suspend fun getCurrentUser(): User? {
        val preferences = dataStore.data.first()
        val userJson = preferences[CURRENT_USER_KEY]
        return userJson?.let { 
            try {
                Json.decodeFromString<UserData>(it).toDomainModel()
            } catch (e: Exception) {
                null
            }
        }
    }
    
    override suspend fun logout() {
        dataStore.edit { preferences ->
            preferences.remove(CURRENT_USER_KEY)
        }
    }
    
    override suspend fun isLoggedIn(): Boolean {
        return getCurrentUser() != null
    }
    
    override suspend fun saveCurrentUser(user: User) {
        dataStore.edit { preferences ->
            preferences[CURRENT_USER_KEY] = Json.encodeToString(UserData.fromDomainModel(user))
        }
    }
    
    override suspend fun createDefaultUsers() {
        // Online-only auth: los usuarios se gestionan en el servidor principal.
        // No creamos usuarios locales por defecto.
    }
}

@kotlinx.serialization.Serializable
private data class UserData(
    val id: Long,
    val username: String,
    val fullName: String,
    val email: String?,
    val phone: String?,
    val role: String,
    val isActive: Boolean,
    val hireDate: String?,
    val salary: Double?,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomainModel(): User = User(
        id = id,
        username = username,
        fullName = fullName,
        email = email,
        phone = phone,
        role = when (role.uppercase()) {
            "EMPLOYEE" -> UserRole.WAITER
            else -> runCatching { UserRole.valueOf(role.uppercase()) }.getOrElse { UserRole.WAITER }
        },
        isActive = isActive,
        hireDate = hireDate,
        salary = salary,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
    
    companion object {
        fun fromDomainModel(user: User): UserData = UserData(
            id = user.id,
            username = user.username,
            fullName = user.fullName,
            email = user.email,
            phone = user.phone,
            role = user.role.name,
            isActive = user.isActive,
            hireDate = user.hireDate,
            salary = user.salary,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt
        )
    }
}

private fun UserLoginResponse.toDomainModel(): User {
    val roleEnum = try {
        UserRole.valueOf(role.uppercase())
    } catch (e: Exception) {
        UserRole.WAITER
    }
    return User(
        id = id,
        username = username,
        fullName = fullName,
        email = null,
        phone = null,
        role = roleEnum,
        isActive = isActive,
        hireDate = null,
        salary = null,
        createdAt = 0L,
        updatedAt = 0L,
    )
}
