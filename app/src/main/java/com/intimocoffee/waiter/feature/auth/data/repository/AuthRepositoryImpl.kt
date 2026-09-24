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
import com.intimocoffee.waiter.feature.accounting.data.AccountingStaffAuthClient
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
    private val accountingStaffAuthClient: AccountingStaffAuthClient,
) : AuthRepository {
    
    companion object {
        private val CURRENT_USER_KEY = stringPreferencesKey("current_user")
        private const val TAG = "AuthRepository"
    }
    
    override suspend fun login(username: String, password: String): Result<User> {
        val trimmedUser = username.trim()
        if (trimmedUser.isEmpty()) {
            return Result.failure(IllegalArgumentException("Indica un usuario"))
        }
        val t0 = System.currentTimeMillis()
        fun elapsed() = System.currentTimeMillis() - t0

        Log.i(TAG, "⏱️ login start user=$trimmedUser")

        // 1) Contabilidad AWS (pos.staff) — no depende de la tablet POS.
        if (accountingStaffAuthClient.isConfigured()) {
            Log.i(TAG, "⏱️ [${elapsed()}ms] trying Contabilidad AWS…")
            val remote = accountingStaffAuthClient.verifyLogin(trimmedUser, password)
            if (remote != null && remote.isActive) {
                Log.i(TAG, "✅ [${elapsed()}ms] login OK vía Contabilidad (${remote.username})")
                // Descubrir POS en segundo plano para pedidos; no bloquea el login.
                try {
                    retrofitProvider.discoverAndRefreshService()
                    Log.i(TAG, "⏱️ [${elapsed()}ms] POS discovery OK → ${retrofitProvider.getCurrentServerUrl()}")
                } catch (e: Exception) {
                    Log.w(
                        TAG,
                        "⚠️ [${elapsed()}ms] Contabilidad OK pero POS aún no visible: ${e.message}",
                    )
                }
                return Result.success(remote)
            }
            Log.w(TAG, "⚠️ [${elapsed()}ms] Contabilidad rechazó o no respondió; probando POS…")
        }

        // 2) Fallback: tablet POS (usuarios Room locales).
        return try {
            val service = retrofitProvider.discoverAndRefreshService()
            Log.i(
                TAG,
                "⏱️ [${elapsed()}ms] discovery done → ${retrofitProvider.getCurrentServerUrl()}",
            )
            if (retrofitProvider.isUsingEmulatorLoopbackOnPhysicalDevice()) {
                return Result.failure(
                    Exception(
                        "No se encontró la tablet POS ni Contabilidad. Revisa Wi‑Fi / usuarios en Contabilidad.",
                    ),
                )
            }
            val apiT0 = System.currentTimeMillis()
            val response = service.login(LoginRequest(username = trimmedUser, password = password))
            Log.i(
                TAG,
                "⏱️ [${elapsed()}ms] POST /api/login http=${response.code()} in ${System.currentTimeMillis() - apiT0}ms",
            )

            if (response.isSuccessful) {
                val body: LoginResponse? = response.body()
                val userDto: UserLoginResponse? = body?.data
                if (body?.success == true && userDto != null && userDto.isActive) {
                    Log.i(TAG, "✅ [${elapsed()}ms] login OK vía POS ${userDto.username}")
                    Result.success(userDto.toDomainModel())
                } else {
                    Log.w(TAG, "❌ [${elapsed()}ms] login rejected by POS")
                    Result.failure(Exception("Usuario o contraseña incorrectos"))
                }
            } else {
                Log.w(TAG, "❌ [${elapsed()}ms] login HTTP ${response.code()}")
                Result.failure(Exception("Usuario o contraseña incorrectos"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ [${elapsed()}ms] login failed", e)
            Result.failure(
                Exception(
                    "No se pudo autenticar (Contabilidad/POS). Misma Wi‑Fi o datos móviles OK.",
                    e,
                ),
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

    override suspend fun revalidateSession(): Boolean {
        val local = getCurrentUser() ?: return false
        // Prefer Contabilidad (mismo id que pos.staff tras login AWS).
        if (accountingStaffAuthClient.isConfigured()) {
            val remote = accountingStaffAuthClient.getStaffById(local.id)
            if (remote != null) {
                saveCurrentUser(remote)
                return true
            }
        }
        return try {
            val service = retrofitProvider.discoverAndRefreshService()
            val response = service.validateSession(local.id)
            val body = response.body()
            val ok = response.isSuccessful && body?.success == true && body.data != null && body.data.isActive
            if (!ok) {
                Log.w(TAG, "Session revalidation failed; clearing DataStore")
                logout()
            } else {
                saveCurrentUser(body!!.data!!.toDomainModel())
            }
            ok
        } catch (e: Exception) {
            Log.e(TAG, "Session revalidation error; clearing DataStore", e)
            logout()
            false
        }
    }
    
    override suspend fun saveCurrentUser(user: User) {
        dataStore.edit { preferences ->
            preferences[CURRENT_USER_KEY] = Json.encodeToString(UserData.fromDomainModel(user))
        }
    }
    
    override suspend fun createDefaultUsers() {
        // Online-only auth: usuarios en Contabilidad (pos.staff) o tablet POS.
    }

    override suspend fun verifyManagerAuthorization(username: String, password: String): User? {
        val result = login(username, password)
        val user = result.getOrNull() ?: return null
        return user.takeIf { it.role.hasManagerAccess() }
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
