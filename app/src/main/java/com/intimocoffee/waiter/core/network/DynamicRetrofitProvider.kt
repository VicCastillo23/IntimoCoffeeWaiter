package com.intimocoffee.waiter.core.network

import android.util.Log
import kotlinx.coroutines.runBlocking
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
        // Fallback base URL when discovery fails. For emulator-to-emulator setups,
        // 10.0.2.2 points to the host, where we forward port 8080 to the main tablet emulator.
        private const val DEFAULT_BASE_URL = "http://10.0.2.2:8080/" // Fallback
    }
    
    private var currentBaseUrl: String? = null
    private var retrofit: Retrofit? = null
    private var apiService: IntimoCoffeeApiService? = null
    
    /**
     * Get the API service, discovering the server if needed
     */
    fun getApiService(): IntimoCoffeeApiService {
        return apiService ?: synchronized(this) {
            apiService ?: createApiService().also { apiService = it }
        }
    }
    
    /**
     * Force rediscovery of the server (useful when connection fails)
     */
    suspend fun rediscoverServer(): IntimoCoffeeApiService {
        Log.i(TAG, "🔄 Forcing server rediscovery...")
        synchronized(this) {
            currentBaseUrl = null
            retrofit = null
            apiService = null
        }
        return getApiService()
    }
    
    /**
     * Get current server URL (for debugging)
     */
    fun getCurrentServerUrl(): String? = currentBaseUrl
    
    private fun createApiService(): IntimoCoffeeApiService {
        try {
            // Try to discover server
            val discoveredUrl = runBlocking { 
                serverDiscoveryService.discoverMainServer()
            }
            
            val baseUrl = if (discoveredUrl != null) {
                Log.i(TAG, "✅ Using discovered server: $discoveredUrl")
                currentBaseUrl = discoveredUrl
                discoveredUrl
            } else {
                Log.w(TAG, "⚠️ Server discovery failed, using default: $DEFAULT_BASE_URL")
                currentBaseUrl = DEFAULT_BASE_URL
                DEFAULT_BASE_URL
            }
            
            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
            
            return retrofit!!.create(IntimoCoffeeApiService::class.java)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating API service", e)
            // Return a service with default URL as last resort
            retrofit = Retrofit.Builder()
                .baseUrl(DEFAULT_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
            
            currentBaseUrl = DEFAULT_BASE_URL
            return retrofit!!.create(IntimoCoffeeApiService::class.java)
        }
    }
}