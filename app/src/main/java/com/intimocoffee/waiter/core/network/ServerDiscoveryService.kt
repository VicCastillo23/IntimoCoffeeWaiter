package com.intimocoffee.waiter.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.net.NetworkInterface
import java.util.concurrent.TimeUnit
import com.intimocoffee.waiter.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@OptIn(ExperimentalCoroutinesApi::class)
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
            // 0. URL fija (gradle.properties → BuildConfig) — útil cuando NSD/escaneo fallan en la red
            val configured = BuildConfig.MAIN_SERVER_BASE_URL.trim()
            if (configured.isNotEmpty()) {
                Log.i(TAG, "🔧 Trying MAIN_SERVER_BASE_URL from BuildConfig: $configured")
                val validated = testDiscoverAtBaseUrl(configured)
                if (validated != null) {
                    Log.i(TAG, "✅ Using configured main server: $validated")
                    return@withContext validated
                }
                Log.w(
                    TAG,
                    "⚠️ MAIN_SERVER_BASE_URL no responde en /discover; probando NSD y escaneo…"
                )
            }

            // 1. NSD/mDNS (instant, no IP scanning)
            Log.i(TAG, "🔍 Starting NSD discovery...")
            val nsdUrl = discoverViaNsd()
            if (nsdUrl != null) {
                // Validate to avoid stale mDNS cache (conserva host:puerto del NSD)
                val validated = testDiscoverAtBaseUrl(nsdUrl)
                if (validated != null) {
                    Log.i(TAG, "✅ NSD server validated: $validated")
                    return@withContext validated
                }
                Log.w(TAG, "⚠️ NSD found $nsdUrl but validation failed (stale cache), falling back to IP scan")
            }

            // 2. Fallback: limited-concurrency scan + common LAN subnets (VPN-safe)
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

            val subnets = buildList {
                add(localSubnet)
                adjacentSubnet?.let { add(it) }
                add("192.168.1")
                add("192.168.172") // Android emulator / ADB reverse common bridge range
                add("192.168.0")
                add("192.168.50")
                add("192.168.86")
                add("10.0.0")
                add("172.20.10") // hotspot iPhone
            }.distinct()
            Log.w(TAG, "⚠️ NSD failed, scanning: ${subnets.joinToString(", ") { "$it.x" }} (localIp=$localIp)")

            val result = scanSubnetsForServer(subnets, localIp)
            if (result != null) {
                Log.i(TAG, "✅ Server found via IP scan at: $result")
            } else {
                Log.w(TAG, "❌ Server not found in any subnet")
            }
            result
        }
    }

    private suspend fun discoverViaNsd(): String? {
        val multicastLock = acquireMulticastLock()
        return try {
            withTimeoutOrNull(8_000L) {
            suspendCancellableCoroutine { cont ->
                val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
                var discoveryListener: NsdManager.DiscoveryListener? = null

                val resolveListener = object : NsdManager.ResolveListener {
                    override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                        Log.e(TAG, "NSD resolve failed: $errorCode")
                        if (cont.isActive) cont.resume(null)
                    }
                    override fun onServiceResolved(info: NsdServiceInfo) {
                        val host = info.host.hostAddress ?: run {
                            if (cont.isActive) cont.resume(null)
                            return
                        }
                        val hostInUrl = if (host.contains(':') && !host.startsWith('[')) "[$host]" else host
                        val url = "http://$hostInUrl:${info.port}/"
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
        } finally {
            releaseMulticastLock(multicastLock)
        }
    }

    /**
     * Android often drops mDNS unless the Wi‑Fi multicast lock is held during discovery.
     */
    @Suppress("DEPRECATION")
    private fun acquireMulticastLock(): WifiManager.MulticastLock? {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return null
        return try {
            wifi.createMulticastLock("IntimoWaiter-NSD").apply {
                setReferenceCounted(false)
                acquire()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not acquire multicast lock (NSD may be unreliable)", e)
            null
        }
    }

    private fun releaseMulticastLock(lock: WifiManager.MulticastLock?) {
        try {
            if (lock?.isHeld == true) lock.release()
        } catch (_: Exception) {}
    }

    private suspend fun scanSubnetsForServer(subnets: List<String>, localIp: String?): String? {
        val ips = subnets.asSequence()
            .flatMap { subnet -> (1..254).asSequence().map { "$subnet.$it" } }
            .filter { it != localIp }
            .distinct()
            .toList()
        if (ips.isEmpty()) return null
        return withTimeoutOrNull(90_000) {
            ips.asFlow()
                .flatMapMerge(concurrency = 32) { ip ->
                    flow {
                        val r = withTimeoutOrNull(2_500L) { testServer(ip) }
                        if (r != null) emit(r)
                    }
                }
                .firstOrNull()
        }
    }
    
    /** Valida `http://host:port/discover` (INTIMO_COFFEE_MAIN). */
    private suspend fun testDiscoverAtBaseUrl(baseUrl: String): String? {
        val normalized = baseUrl.trim().ifEmpty { return null }
        val uri = Uri.parse(normalized)
        val host = uri.host ?: return null
        val port = if (uri.port != -1) uri.port else SERVER_PORT
        return testServer(host, port)
    }

    /**
     * Test if a server is running at the given IP using the /discover endpoint
     * exposed by IntimoCoffeeApp's HttpServer and validate it's the main server.
     */
    private suspend fun testServer(ip: String, port: Int = SERVER_PORT): String? {
        return withContext(Dispatchers.IO) {
            try {
                val hostForUrl = if (ip.contains(':') && !ip.startsWith("[")) "[$ip]" else ip
                val probeUrl = "http://$hostForUrl:$port"
                val request = Request.Builder()
                    .url("$probeUrl/discover")
                    .get()
                    .build()

                quickHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.d(TAG, "❌ Server at $probeUrl responded with error on /discover: ${response.code}")
                        return@withContext null
                    }

                    val bodyString = response.body?.string()
                    if (bodyString.isNullOrBlank()) {
                        Log.d(TAG, "❌ Empty body from $probeUrl/discover")
                        return@withContext null
                    }

                    return@withContext try {
                        val json = org.json.JSONObject(bodyString)
                        val serviceType = json.optString("serviceType")

                        if (serviceType == "INTIMO_COFFEE_MAIN") {
                            // Siempre el host:puerto con el que respondió /discover (JSON ipAddress puede estar mal si hay VPN).
                            val resolvedBaseUrl = "http://$hostForUrl:$port/"
                            Log.d(TAG, "✅ Valid INTIMO_COFFEE_MAIN at $resolvedBaseUrl")
                            resolvedBaseUrl
                        } else {
                            Log.d(TAG, "❌ Service type mismatch at $probeUrl: $serviceType")
                            null
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "❌ Failed to parse /discover response from $probeUrl: ${e.message}")
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
     * Prefer the IPv4 of the active (e.g. Wi‑Fi) network so subnet scans match the server's LAN.
     */
    @Suppress("DEPRECATION")
    private fun getLocalIpAddress(): String? {
        val fromActive = try {
            val cm = context.applicationContext
                .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork
            val lp = if (network != null) cm.getLinkProperties(network) else null
            lp?.linkAddresses?.asSequence()
                ?.mapNotNull { la ->
                    val addr = la.address ?: return@mapNotNull null
                    if (addr.isLoopbackAddress || addr !is java.net.Inet4Address) return@mapNotNull null
                    addr.hostAddress?.takeIf { it.isNotBlank() }
                }
                ?.firstOrNull()
        } catch (e: Exception) {
            Log.w(TAG, "activeNetwork IPv4 lookup failed", e)
            null
        }
        if (fromActive != null) return fromActive

        return try {
            NetworkInterface.getNetworkInterfaces().asSequence()
                .flatMap { it.inetAddresses.asSequence() }
                .mapNotNull { addr ->
                    if (addr.isLoopbackAddress || addr !is java.net.Inet4Address) return@mapNotNull null
                    addr.hostAddress?.takeIf { it.isNotBlank() }
                }
                .sortedWith(
                    compareBy(
                        { host ->
                            when {
                                host.startsWith("192.168.") -> 0
                                host.startsWith("10.") -> 1
                                host.startsWith("172.") -> 2
                                else -> 3
                            }
                        },
                        { it }
                    )
                )
                .firstOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local IP address", e)
            null
        }
    }
}