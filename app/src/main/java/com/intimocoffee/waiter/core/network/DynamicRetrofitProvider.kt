package com.intimocoffee.waiter.core.network

import android.os.Build
import android.util.Log
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicRetrofitProvider @Inject constructor(
    private val serverDiscoveryService: ServerDiscoveryService,
    private val okHttpClient: OkHttpClient,
    private val json: Json
) {
    
    companion object {
        private const val TAG = "DynamicRetrofitProvider"
        /** Solo emulador Android; en teléfono real no sirve — hay que usar NSD, escaneo o INTIMO_MAIN_SERVER_URL. */
        private const val EMULATOR_LOOPBACK_BASE_URL = "http://10.0.2.2:8080/"
    }

    private fun isLikelyEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("generic")
            || Build.FINGERPRINT.startsWith("unknown")
            || Build.MODEL.contains("Emulator")
            || Build.MODEL.contains("Android SDK built for x86")
            || Build.MANUFACTURER.contains("Genymotion")
    }
    
    @Volatile private var currentBaseUrl: String = EMULATOR_LOOPBACK_BASE_URL
    @Volatile private var apiService: IntimoCoffeeApiService =
        buildService(EMULATOR_LOOPBACK_BASE_URL)
    
    /**
     * Returns the cached API service (uses DEFAULT_BASE_URL until discoverAndRefreshService() runs).
     */
    fun getApiService(): IntimoCoffeeApiService = apiService
    
    /**
     * Discovers the server and updates the cached service. Must be called before login.
     */
    suspend fun discoverAndRefreshService(): IntimoCoffeeApiService {
        Log.i(TAG, "🔍 Discovering server...")
        val discoveredUrl = serverDiscoveryService.discoverMainServer()
        val baseUrl = when {
            discoveredUrl != null -> {
                Log.i(TAG, "✅ Discovered server: $discoveredUrl")
                discoveredUrl
            }
            isLikelyEmulator() -> {
                Log.w(TAG, "⚠️ Discovery failed, using emulator host: $EMULATOR_LOOPBACK_BASE_URL")
                EMULATOR_LOOPBACK_BASE_URL
            }
            else -> {
                Log.e(
                    TAG,
                    "❌ Discovery failed on dispositivo físico. Añade en gradle.properties la IP de la tablet: " +
                        "INTIMO_MAIN_SERVER_URL=http://192.168.x.x:8080/ y Sync + rebuild."
                )
                EMULATOR_LOOPBACK_BASE_URL
            }
        }
        synchronized(this) {
            currentBaseUrl = baseUrl
            apiService = buildService(baseUrl)
        }
        return apiService
    }
    
    /**
     * Force rediscovery of the server (useful when a connection fails mid-session).
     */
    suspend fun rediscoverServer(): IntimoCoffeeApiService {
        Log.i(TAG, "🔄 Forcing server rediscovery...")
        return discoverAndRefreshService()
    }
    
    /**
     * Get current server URL (for debugging / header display).
     */
    fun getCurrentServerUrl(): String = currentBaseUrl
    
    private fun buildService(baseUrl: String): IntimoCoffeeApiService =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(IntimoCoffeeApiService::class.java)
}
