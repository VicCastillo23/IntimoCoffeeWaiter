package com.intimocoffee.waiter.core.network

import android.util.Log
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.net.NetworkInterface
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerDiscoveryService @Inject constructor() {
    
    companion object {
        private const val TAG = "ServerDiscoveryService"
        private const val SERVER_PORT = 8080
        private const val DISCOVERY_TIMEOUT = 2 // seconds
    }
    
    private val quickHttpClient = OkHttpClient.Builder()
        .connectTimeout(DISCOVERY_TIMEOUT.toLong(), TimeUnit.SECONDS)
        .readTimeout(DISCOVERY_TIMEOUT.toLong(), TimeUnit.SECONDS)
        .writeTimeout(DISCOVERY_TIMEOUT.toLong(), TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC // Less verbose for discovery
        })
        .build()
    
    /**
     * Discover the main IntimoCoffeeApp server automatically.
     * Scans the full local network range IN PARALLEL so the total time is bounded
     * by a single timeout (~2 s) regardless of how many IPs don't respond.
     */
    suspend fun discoverMainServer(): String? {
        return withContext(Dispatchers.IO) {
            val localIp = getLocalIpAddress()
            val localIpRange = localIp?.substringBeforeLast(".") ?: "192.168.1"
            Log.i(TAG, "🔍 Starting parallel server discovery. Local IP: $localIp, range: $localIpRange.x")

            supervisorScope {
                val deferreds = (1..254)
                    .map { "$localIpRange.$it" }
                    .filter { it != localIp }
                    .map { ip -> async { testServer(ip) } }

                val result = deferreds.awaitAll().firstOrNull { it != null }

                if (result != null) {
                    Log.i(TAG, "✅ Server found at: $result")
                } else {
                    Log.w(TAG, "❌ Server not found in $localIpRange.1-254")
                }
                result
            }
        }
    }
    
    /**
     * Test if a server is running at the given IP using the /discover endpoint
     * exposed by IntimoCoffeeApp's HttpServer and validate it's the main server.
     */
    private suspend fun testServer(ip: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = "http://$ip:$SERVER_PORT"
                val request = Request.Builder()
                    .url("$baseUrl/discover")
                    .get()
                    .build()

                quickHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.d(TAG, "❌ Server at $baseUrl responded with error on /discover: ${response.code}")
                        return@withContext null
                    }

                    val bodyString = response.body?.string()
                    if (bodyString.isNullOrBlank()) {
                        Log.d(TAG, "❌ Empty body from $baseUrl/discover")
                        return@withContext null
                    }

                    return@withContext try {
                        val json = org.json.JSONObject(bodyString)
                        val serviceType = json.optString("serviceType")
                        val announcedPort = json.optInt("port", SERVER_PORT)
                        val announcedIp = json.optString("ipAddress", ip)

                        if (serviceType == "INTIMO_COFFEE_MAIN") {
                            // En escenario de emuladores con port forwarding, si contactamos a 10.0.2.2 o 127.0.0.1
                            // debemos seguir usando ese host y NO la IP interna anunciada (10.0.2.x del emulador servidor).
                            val originalHost = baseUrl.removePrefix("http://").substringBefore(":")
                            val useOriginalHost = originalHost == "10.0.2.2" || originalHost == "127.0.0.1"

                            val resolvedHost = if (useOriginalHost) {
                                originalHost
                            } else if (announcedIp.isNotBlank() && announcedIp != "unknown") {
                                announcedIp
                            } else {
                                ip
                            }

                            val resolvedBaseUrl = "http://$resolvedHost:$announcedPort/"
                            Log.d(TAG, "✅ Valid INTIMO_COFFEE_MAIN discovered at $resolvedBaseUrl (announced from $baseUrl)")
                            resolvedBaseUrl
                        } else {
                            Log.d(TAG, "❌ Service type mismatch at $baseUrl: $serviceType")
                            null
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "❌ Failed to parse /discover response from $baseUrl: ${e.message}")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "❌ No server at $ip (error calling /discover): ${e.message}")
                null
            }
        }
    }
    
    /**
     * Get local IP address of this device
     */
    private fun getLocalIpAddress(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces().asSequence()
                .flatMap { it.inetAddresses.asSequence() }
                .find { !it.isLoopbackAddress && it is java.net.Inet4Address }
                ?.hostAddress
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local IP address", e)
            null
        }
    }
}