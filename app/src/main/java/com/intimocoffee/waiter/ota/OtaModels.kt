package com.intimocoffee.waiter.ota

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OtaManifest(
    val generatedAt: String? = null,
    val apps: Map<String, OtaAppRelease> = emptyMap(),
)

@Serializable
data class OtaAppRelease(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val releaseNotes: String? = null,
    val sha256: String? = null,
)

sealed class OtaCheckResult {
    data object UpToDate : OtaCheckResult()
    data class UpdateAvailable(
        val release: OtaAppRelease,
        val apkAbsoluteUrl: String,
        val manifestUrl: String,
    ) : OtaCheckResult()
    data class Error(val message: String) : OtaCheckResult()
}

sealed class OtaDownloadResult {
    data class Ready(val apkFile: java.io.File) : OtaDownloadResult()
    data class Error(val message: String) : OtaDownloadResult()
}
