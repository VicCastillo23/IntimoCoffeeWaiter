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
    
    @Volatile private var currentBaseUrl: String =
        if (isLikelyEmulator()) EMULATOR_LOOPBACK_BASE_URL else ""
    @Volatile private var apiService: IntimoCoffeeApiService? =
        if (isLikelyEmulator()) buildService(EMULATOR_LOOPBACK_BASE_URL) else null
    
    /**
     * Returns the cached API service. On physical devices this is null until
     * [discoverAndRefreshService] finds a server (never falls back to 10.0.2.2).
     */
    fun getApiService(): IntimoCoffeeApiService =
        apiService ?: throw IllegalStateException(
            "Servidor POS no disponible. Configura INTIMO_MAIN_SERVER_URL o espera el descubrimiento."
        )
    
    /**
     * Discovers the server and updates the cached service. Must be called before login.
     * Si ya hay URL en caché y /discover responde, la reutiliza (evita NSD/escaneo en cada login).
     */
    suspend fun discoverAndRefreshService(): IntimoCoffeeApiService {
        val t0 = System.currentTimeMillis()
        fun elapsed() = System.currentTimeMillis() - t0

        val cached = currentBaseUrl.trim()
        if (cached.isNotEmpty() && !cached.contains("10.0.2.2")) {
            Log.i(TAG, "♻️ [${elapsed()}ms] Probando servidor en caché: $cached")
            val stillAlive = serverDiscoveryService.validateKnownServer(cached)
            if (stillAlive != null) {
                synchronized(this) {
                    currentBaseUrl = stillAlive
                    if (apiService == null) apiService = buildService(stillAlive)
                }
                Log.i(TAG, "✅ [${elapsed()}ms] Caché OK — sin rediscovery completo")
                return apiService!!
            }
            Log.w(TAG, "⚠️ [${elapsed()}ms] Caché muerta; discovery completo…")
        }

        Log.i(TAG, "🔍 [${elapsed()}ms] Discovering server (full)...")
        val discoveredUrl = serverDiscoveryService.discoverMainServer()
        val baseUrl = when {
            discoveredUrl != null -> {
                Log.i(TAG, "✅ [${elapsed()}ms] Discovered server: $discoveredUrl")
                discoveredUrl
            }
            isLikelyEmulator() -> {
                Log.w(TAG, "⚠️ [${elapsed()}ms] Discovery failed, using emulator host: $EMULATOR_LOOPBACK_BASE_URL")
                EMULATOR_LOOPBACK_BASE_URL
            }
            else -> {
                Log.e(
                    TAG,
                    "❌ [${elapsed()}ms] Discovery failed on dispositivo físico. Añade en gradle.properties la IP de la tablet: " +
                        "INTIMO_MAIN_SERVER_URL=http://192.168.x.x:8080/ y Sync + rebuild.",
                )
                throw IllegalStateException(
                    "No se encontró la tablet POS. En gradle.properties del proyecto mesero: " +
                        "INTIMO_MAIN_SERVER_URL=http://IP_DE_LA_TABLET:8080/ y Sync + recompilar.",
                )
            }
        }
        synchronized(this) {
            currentBaseUrl = baseUrl
            apiService = buildService(baseUrl)
        }
        Log.i(TAG, "✅ [${elapsed()}ms] Retrofit listo → $baseUrl")
        return apiService!!
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
    fun getCurrentServerUrl(): String = currentBaseUrl.ifBlank { "(sin servidor)" }

    /**
     * True if somehow still pointing at emulator loopback on a real device (should not happen after WTR-008).
     */
    fun isUsingEmulatorLoopbackOnPhysicalDevice(): Boolean {
        return !isLikelyEmulator() && currentBaseUrl.contains("10.0.2.2")
    }

    private fun buildService(baseUrl: String): IntimoCoffeeApiService =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(IntimoCoffeeApiService::class.java)
}
