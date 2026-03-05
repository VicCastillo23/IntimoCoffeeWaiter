package com.intimocoffee.waiter.core.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
     * Discover the main IntimoCoffeeApp server automatically
     * Returns the base URL if found, null otherwise
     */
    suspend fun discoverMainServer(): String? {
        return withContext(Dispatchers.IO) {
            try {
                val localIp = getLocalIpAddress()
                Log.i(TAG, "🔍 Starting server discovery. Local IP: $localIp")
                
                // PRIORITY 1: Test emulator host IP (MOST IMPORTANT FOR EMULATOR-TO-EMULATOR)
                Log.d(TAG, "Testing emulator host IP: 10.0.2.2:$SERVER_PORT")
                val emulatorHostUrl = testServer("10.0.2.2")
                if (emulatorHostUrl != null) {
                    Log.i(TAG, "✅ Server found at emulator host: $emulatorHostUrl")
                    return@withContext emulatorHostUrl
                }
                
                // PRIORITY 2: Test localhost (same device/emulator)
                Log.d(TAG, "Testing localhost: 127.0.0.1:$SERVER_PORT")
                val localhostUrl = testServer("127.0.0.1")
                if (localhostUrl != null) {
                    Log.i(TAG, "✅ Server found at localhost: $localhostUrl")
                    return@withContext localhostUrl
                }
                
                // PRIORITY 3: Test common Android IPs
                val commonIPs = listOf(
                    "10.0.2.15",    // Android emulator default guest IP
                    "10.0.2.2",     // Android emulator host IP (again, it's that important)
                    "192.168.1.100", // Original IP from config
                    "192.168.1.1",   // Common router IP
                    "192.168.0.1",   // Another common router IP
                    "192.168.1.101",
                    "192.168.0.100"
                )
                
                for (testIp in commonIPs) {
                    Log.d(TAG, "Testing common IP: $testIp:$SERVER_PORT")
                    val serverUrl = testServer(testIp)
                    if (serverUrl != null) {
                        Log.i(TAG, "✅ Server found at common IP: $serverUrl")
                        return@withContext serverUrl
                    }
                }
                
                // PRIORITY 4: If we have local IP, scan its network range
                if (localIp != null && localIp != "127.0.0.1") {
                    val baseIp = localIp.substringBeforeLast(".") + "."
                    Log.d(TAG, "Scanning local network range: ${baseIp}x")
                    
                    // Test a small range around common IPs first
                    val priorityRange = listOf(1, 100, 101, 102, 2, 10, 20, 50, 200, 254)
                    for (i in priorityRange) {
                        val testIp = "$baseIp$i"
                        if (testIp == localIp) continue // Skip self
                        
                        val serverUrl = testServer(testIp)
                        if (serverUrl != null) {
                            Log.i(TAG, "✅ Server found in network scan: $serverUrl")
                            return@withContext serverUrl
                        }
                    }
                }
                
                Log.w(TAG, "❌ Server not found after comprehensive discovery")
                null
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error during server discovery", e)
                null
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