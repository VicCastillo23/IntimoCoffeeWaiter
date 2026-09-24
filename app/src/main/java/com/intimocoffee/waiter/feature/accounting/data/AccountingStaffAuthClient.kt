package com.intimocoffee.waiter.feature.accounting.data

import android.util.Log
import com.intimocoffee.waiter.BuildConfig
import com.intimocoffee.waiter.feature.auth.domain.model.User
import com.intimocoffee.waiter.feature.auth.domain.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Login de staff contra Contabilidad AWS (`pos.staff` vía POST /api/pos/staff/login).
 * Independiente de la tablet POS — útil para entrar con usuarios de Contabilidad.
 */
@Singleton
class AccountingStaffAuthClient @Inject constructor() {

    companion object {
        private const val TAG = "AccountingStaffAuth"
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }

    private val baseUrl: String = BuildConfig.ACCOUNTING_POS_BASE_URL.trimEnd('/')
    private val secret: String = BuildConfig.ACCOUNTING_POS_INGEST_SECRET.trim()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    fun isConfigured(): Boolean = baseUrl.isNotBlank() && secret.isNotBlank()

    suspend fun verifyLogin(username: String, password: String): User? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext null
        val t0 = System.currentTimeMillis()
        try {
            val bodyJson = json.encodeToString(
                StaffLoginRequest.serializer(),
                StaffLoginRequest(username = username.trim(), password = password),
            )
            val request = Request.Builder()
                .url("$baseUrl/api/pos/staff/login")
                .header("Authorization", "Bearer $secret")
                .header("Content-Type", "application/json")
                .post(bodyJson.toRequestBody(JSON_MEDIA))
                .build()
            client.newCall(request).execute().use { response ->
                val bodyText = response.body?.string().orEmpty()
                val ms = System.currentTimeMillis() - t0
                if (!response.isSuccessful) {
                    Log.w(TAG, "login HTTP ${response.code} in ${ms}ms: $bodyText")
                    return@withContext null
                }
                val parsed = json.decodeFromString(StaffLoginResponse.serializer(), bodyText)
                val data = parsed.data ?: return@withContext null
                if (!parsed.success) return@withContext null
                Log.i(TAG, "✅ Contabilidad login OK ${data.username} (${data.role}) in ${ms}ms")
                User(
                    id = data.id,
                    username = data.username,
                    fullName = data.fullName,
                    email = data.email.takeIf { it.isNotBlank() },
                    role = runCatching { UserRole.valueOf(data.role.uppercase()) }
                        .getOrElse { UserRole.WAITER },
                    isActive = data.isActive,
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "login falló en ${System.currentTimeMillis() - t0}ms: ${e.message}")
            null
        }
    }

    suspend fun getStaffById(userId: Long): User? = withContext(Dispatchers.IO) {
        if (!isConfigured() || userId <= 0L) return@withContext null
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/pos/staff/me?userId=$userId")
                .header("Authorization", "Bearer $secret")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val bodyText = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.w(TAG, "me HTTP ${response.code}: $bodyText")
                    return@withContext null
                }
                val parsed = json.decodeFromString(StaffLoginResponse.serializer(), bodyText)
                val data = parsed.data ?: return@withContext null
                if (!parsed.success || !data.isActive) return@withContext null
                User(
                    id = data.id,
                    username = data.username,
                    fullName = data.fullName,
                    email = data.email.takeIf { it.isNotBlank() },
                    role = runCatching { UserRole.valueOf(data.role.uppercase()) }
                        .getOrElse { UserRole.WAITER },
                    isActive = data.isActive,
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "me falló: ${e.message}")
            null
        }
    }
}

@Serializable
private data class StaffLoginRequest(
    val username: String,
    val password: String,
)

@Serializable
private data class StaffLoginResponse(
    val success: Boolean = false,
    val data: StaffUserDto? = null,
    val message: String? = null,
)

@Serializable
private data class StaffUserDto(
    val id: Long,
    val username: String,
    val fullName: String,
    val email: String = "",
    val role: String,
    val isActive: Boolean = true,
)
