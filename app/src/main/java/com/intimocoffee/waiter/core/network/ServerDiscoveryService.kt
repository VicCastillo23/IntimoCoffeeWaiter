package com.intimocoffee.waiter.core.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.net.NetworkInterface
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class ServerDiscoveryService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
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
     * Tries NSD/mDNS first (instant), falls back to parallel IP scan.
     */
    suspend fun discoverMainServer(): String? {
        return withContext(Dispatchers.IO) {
            // 1. NSD/mDNS (instant, no IP scanning)
            Log.i(TAG, "🔍 Starting NSD discovery...")
            val nsdUrl = discoverViaNsd()
            if (nsdUrl != null) {
                // Validate to avoid stale mDNS cache
                val nsdIp = nsdUrl.removePrefix("http://").substringBefore(":")
                val validated = testServer(nsdIp)
                if (validated != null) {
                    Log.i(TAG, "✅ NSD server validated: $validated")
                    return@withContext validated
                }
                Log.w(TAG, "⚠️ NSD found $nsdUrl but validation failed (stale cache), falling back to IP scan")
            }

            // 2. Fallback: parallel IP scan on local subnet + adjacent (.0 ↔ .1)
            val localIp = getLocalIpAddress()
            val localSubnet = localIp?.substringBeforeLast(".") ?: "192.168.1"

            val parts = localSubnet.split(".")
            val adjacentSubnet = if (parts.size == 3) {
                when (parts[2].toIntOrNull()) {
                    0 -> "${parts[0]}.${parts[1]}.1"
                    1 -> "${parts[0]}.${parts[1]}.0"
                    else -> null
                }
            } else null

            val subnets = listOfNotNull(localSubnet, adjacentSubnet)
            Log.w(TAG, "⚠️ NSD failed, scanning: ${subnets.joinToString(", ") { "$it.x" }}")

            supervisorScope {
                val deferreds = subnets.flatMap { subnet ->
                    (1..254)
                        .map { "$subnet.$it" }
                        .filter { it != localIp }
                        .map { ip -> async { withTimeoutOrNull(2_000L) { testServer(ip) } } }
                }

                val result = deferreds.awaitAll().firstOrNull { it != null }

                if (result != null) {
                    Log.i(TAG, "✅ Server found via IP scan at: $result")
                } else {
                    Log.w(TAG, "❌ Server not found in any subnet")
                }
                result
            }
        }
    }

    private suspend fun discoverViaNsd(): String? {
        return withTimeoutOrNull(8_000L) {
            suspendCancellableCoroutine { cont ->
                val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
                var discoveryListener: NsdManager.DiscoveryListener? = null

                val resolveListener = object : NsdManager.ResolveListener {
                    override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                        Log.e(TAG, "NSD resolve failed: $errorCode")
                        if (cont.isActive) cont.resume(null)
                    }
                    override fun onServiceResolved(info: NsdServiceInfo) {
                        val url = "http://${info.host.hostAddress}:${info.port}/"
                        Log.i(TAG, "NSD resolved: $url")
                        if (cont.isActive) cont.resume(url)
                    }
                }

                discoveryListener = object : NsdManager.DiscoveryListener {
                    override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                        Log.e(TAG, "NSD start discovery failed: $errorCode")
                        if (cont.isActive) cont.resume(null)
                    }
                    override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
                    override fun onDiscoveryStarted(serviceType: String) {
                        Log.d(TAG, "NSD discovery started")
                    }
                    override fun onDiscoveryStopped(serviceType: String) {}
                    override fun onServiceFound(info: NsdServiceInfo) {
                        Log.d(TAG, "NSD service found: ${info.serviceName}")
                        nsd.stopServiceDiscovery(this)
                        nsd.resolveService(info, resolveListener)
                    }
                    override fun onServiceLost(info: NsdServiceInfo) {}
                }

                nsd.discoverServices("_intimocoffee._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
                cont.invokeOnCancellation {
                    try { nsd.stopServiceDiscovery(discoveryListener) } catch (_: Exception) {}
                }
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