package com.intimocoffee.waiter.ota

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.intimocoffee.waiter.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Comprueba [BuildConfig.OTA_MANIFEST_URL] y URLs LAN opcionales (p. ej. POS `/ota/version.json`).
 */
class OtaUpdateManager(
    private val context: Context,
    private val packageName: String = context.packageName,
    private val currentVersionCode: Int = BuildConfig.VERSION_CODE,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun checkForUpdate(
        extraManifestUrls: List<String> = emptyList(),
    ): OtaCheckResult = withContext(Dispatchers.IO) {
        val urls = linkedSetOf<String>()
        // Nube primero (AWS / contabilidad); LAN solo como respaldo.
        // Preferir HTTP: Android 6 no confía la raíz ISRG Root YR de Let's Encrypt.
        BuildConfig.OTA_MANIFEST_URL.trim().takeIf { it.isNotEmpty() }?.let {
            urls.addAll(manifestCandidates(it))
        }
        extraManifestUrls.map { it.trim() }.filter { it.isNotEmpty() }.forEach {
            urls.addAll(manifestCandidates(it))
        }
        if (urls.isEmpty()) {
            return@withContext OtaCheckResult.Error("No hay URL de actualizaciones configurada.")
        }

        var lastError: String? = null
        var best: OtaCheckResult.UpdateAvailable? = null
        var sawPackage = false
        for (manifestUrl in urls) {
            try {
                val body = httpGetText(manifestUrl)
                val manifest = json.decodeFromString(OtaManifest.serializer(), body)
                val release = manifest.apps[packageName]
                if (release == null) {
                    Log.w(TAG, "Sin entrada $packageName en $manifestUrl")
                    continue
                }
                sawPackage = true
                if (release.versionCode <= currentVersionCode) {
                    continue
                }
                val apkUrl = resolveUrl(manifestUrl, release.apkUrl)
                val candidate = OtaCheckResult.UpdateAvailable(release, apkUrl, manifestUrl)
                if (best == null || candidate.release.versionCode > best.release.versionCode) {
                    best = candidate
                }
            } catch (e: Exception) {
                lastError = e.message ?: e.javaClass.simpleName
                Log.w(TAG, "OTA check falló en $manifestUrl: $lastError")
            }
        }
        best?.let { return@withContext it }
        if (sawPackage) return@withContext OtaCheckResult.UpToDate
        OtaCheckResult.Error(lastError ?: "No se pudo consultar actualizaciones.")
    }

    suspend fun downloadApk(
        apkUrl: String,
        expectedSha256: String? = null,
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> },
    ): OtaDownloadResult = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.cacheDir, "ota").apply { mkdirs() }
            val out = File(dir, "update.apk")
            if (out.exists()) out.delete()

            val downloadUrl = preferCleartextOta(apkUrl)
            val conn = (URL(downloadUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 120_000
                // No seguir a HTTPS: Android 6 falla con CertPathValidatorException.
                instanceFollowRedirects = false
                requestMethod = "GET"
            }
            conn.connect()
            if (conn.responseCode !in 200..299) {
                return@withContext OtaDownloadResult.Error("Descarga HTTP ${conn.responseCode}")
            }
            val total = conn.contentLengthLong.coerceAtLeast(-1L)
            conn.inputStream.use { input ->
                out.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    var downloaded = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        output.write(buf, 0, n)
                        downloaded += n
                        onProgress(downloaded, total)
                    }
                }
            }
            if (out.length() < 1_000L) {
                out.delete()
                return@withContext OtaDownloadResult.Error("APK vacío o incompleto.")
            }
            val expected = expectedSha256?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
            if (expected != null) {
                val actual = sha256Hex(out)
                if (actual != expected) {
                    out.delete()
                    return@withContext OtaDownloadResult.Error("Checksum SHA-256 no coincide.")
                }
            }
            OtaDownloadResult.Ready(out)
        } catch (e: Exception) {
            Log.e(TAG, "OTA download falló", e)
            OtaDownloadResult.Error(e.message ?: "Error al descargar")
        }
    }

    fun canInstallPackages(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun intentToAllowUnknownSources(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            )
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
    }

    fun installApk(apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    fun currentVersionLabel(): String =
        "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

    companion object {
        private const val TAG = "OtaUpdate"

        fun lanManifestUrl(serverBaseUrl: String?): String? {
            var base = serverBaseUrl?.trim()?.trimEnd('/') ?: return null
            if (base.isEmpty()) return null
            // El POS a veces reporta "192.168.x.x:8080" sin esquema.
            if (!base.contains("://")) {
                base = "http://$base"
            }
            return "$base/ota/version.json"
        }

        /** Tablets viejos no validan el cert LE actual → OTA por HTTP en este host. */
        fun preferCleartextOta(url: String): String {
            val u = url.trim()
            val https = "https://contabilidad.cafeintimo.mx/"
            return if (u.startsWith(https, ignoreCase = true)) {
                "http://" + u.substring(8)
            } else {
                u
            }
        }

        private fun manifestCandidates(url: String): List<String> {
            val clear = preferCleartextOta(url)
            return linkedSetOf(clear, url.trim()).filter { it.isNotEmpty() }
        }

        private fun resolveUrl(manifestUrl: String, apkUrl: String): String {
            val raw = apkUrl.trim()
            val absolute = if (
                raw.startsWith("http://", ignoreCase = true) ||
                raw.startsWith("https://", ignoreCase = true)
            ) {
                raw
            } else {
                URL(URL(manifestUrl), raw).toString()
            }
            return preferCleartextOta(absolute)
        }

        private fun httpGetText(url: String): String {
            val conn = (URL(preferCleartextOta(url)).openConnection() as HttpURLConnection).apply {
                connectTimeout = 12_000
                readTimeout = 20_000
                instanceFollowRedirects = false
                requestMethod = "GET"
            }
            conn.connect()
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code al leer manifiesto")
            }
            return text
        }

        private fun sha256Hex(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buf = ByteArray(64 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    digest.update(buf, 0, n)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
}
