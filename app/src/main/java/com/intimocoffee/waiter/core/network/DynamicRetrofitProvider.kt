package com.intimocoffee.waiter.core.network

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
        // Fallback base URL (emulator only — real devices discover via NSD/IP scan)
        private const val DEFAULT_BASE_URL = "http://10.0.2.2:8080/"
    }
    
    @Volatile private var currentBaseUrl: String = DEFAULT_BASE_URL
    @Volatile private var apiService: IntimoCoffeeApiService = buildService(DEFAULT_BASE_URL)
    
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
        val baseUrl = if (discoveredUrl != null) {
            Log.i(TAG, "✅ Discovered server: $discoveredUrl")
            discoveredUrl
        } else {
            Log.w(TAG, "⚠️ Discovery failed, using default: $DEFAULT_BASE_URL")
            DEFAULT_BASE_URL
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
