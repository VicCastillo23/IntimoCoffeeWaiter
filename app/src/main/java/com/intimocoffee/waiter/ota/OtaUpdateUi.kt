package com.intimocoffee.waiter.ota

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.io.File

private sealed class OtaUiPhase {
    data object Idle : OtaUiPhase()
    data object Checking : OtaUiPhase()
    data object UpToDate : OtaUiPhase()
    data class Available(val update: OtaCheckResult.UpdateAvailable) : OtaUiPhase()
    data class Downloading(val update: OtaCheckResult.UpdateAvailable, val progress: Float) : OtaUiPhase()
    data class Ready(val update: OtaCheckResult.UpdateAvailable, val file: File) : OtaUiPhase()
    data class Error(val message: String) : OtaUiPhase()
}

/**
 * Diálogo reutilizable: buscar / descargar / instalar actualización OTA.
 *
 * @param lanServerUrl base del POS (p. ej. http://192.168.x.x:8080) para manifiesto LAN.
 * @param autoCheck si true, consulta al abrir.
 */
@Composable
fun OtaUpdateDialog(
    onDismiss: () -> Unit,
    lanServerUrl: String? = null,
    autoCheck: Boolean = true,
    title: String = "Actualizar app",
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember(context) { OtaUpdateManager(context.applicationContext) }
    var phase by remember { mutableStateOf<OtaUiPhase>(OtaUiPhase.Idle) }
    var progress by remember { mutableFloatStateOf(0f) }

    fun extraUrls(): List<String> = listOfNotNull(OtaUpdateManager.lanManifestUrl(lanServerUrl))

    fun runCheck() {
        phase = OtaUiPhase.Checking
        scope.launch {
            when (val result = manager.checkForUpdate(extraUrls())) {
                is OtaCheckResult.UpToDate -> phase = OtaUiPhase.UpToDate
                is OtaCheckResult.UpdateAvailable -> phase = OtaUiPhase.Available(result)
                is OtaCheckResult.Error -> phase = OtaUiPhase.Error(result.message)
            }
        }
    }

    fun runDownload(update: OtaCheckResult.UpdateAvailable) {
        phase = OtaUiPhase.Downloading(update, 0f)
        progress = 0f
        scope.launch {
            when (
                val result = manager.downloadApk(update.apkAbsoluteUrl, update.release.sha256) { down, total ->
                    if (total > 0) progress = (down.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                }
            ) {
                is OtaDownloadResult.Ready -> phase = OtaUiPhase.Ready(update, result.apkFile)
                is OtaDownloadResult.Error -> phase = OtaUiPhase.Error(result.message)
            }
        }
    }

    fun runInstall(file: File) {
        if (!manager.canInstallPackages()) {
            context.startActivity(manager.intentToAllowUnknownSources())
            phase = OtaUiPhase.Error(
                "Activa «Permitir de esta fuente» para instalar actualizaciones y vuelve a tocar Instalar."
            )
            return
        }
        try {
            manager.installApk(file)
        } catch (e: Exception) {
            phase = OtaUiPhase.Error(e.message ?: "No se pudo abrir el instalador")
        }
    }

    LaunchedEffect(autoCheck) {
        if (autoCheck) runCheck()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Versión instalada: ${manager.currentVersionLabel()}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                when (val p = phase) {
                    OtaUiPhase.Idle -> Text("Toca «Buscar» para consultar el servidor.")
                    OtaUiPhase.Checking -> Text("Buscando actualización…")
                    OtaUiPhase.UpToDate -> Text("Ya tienes la última versión publicada.")
                    is OtaUiPhase.Available -> {
                        Text(
                            "Nueva versión: ${p.update.release.versionName} (${p.update.release.versionCode})",
                            fontWeight = FontWeight.SemiBold,
                        )
                        p.update.release.releaseNotes?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    is OtaUiPhase.Downloading -> {
                        Text("Descargando…")
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        )
                    }
                    is OtaUiPhase.Ready -> Text("Listo. Toca Instalar (Android pedirá confirmación).")
                    is OtaUiPhase.Error -> Text(p.message, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            when (val p = phase) {
                OtaUiPhase.Idle, OtaUiPhase.UpToDate, is OtaUiPhase.Error -> {
                    Button(onClick = { runCheck() }) { Text("Buscar") }
                }
                OtaUiPhase.Checking, is OtaUiPhase.Downloading -> {
                    Button(onClick = {}, enabled = false) { Text("Espera…") }
                }
                is OtaUiPhase.Available -> {
                    Button(onClick = { runDownload(p.update) }) { Text("Descargar") }
                }
                is OtaUiPhase.Ready -> {
                    Button(onClick = { runInstall(p.file) }) { Text("Instalar") }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        },
    )
}

@Composable
fun OtaUpdateScreen(
    onBack: () -> Unit,
    lanServerUrl: String? = null,
) {
    var showDialog by remember { mutableStateOf(true) }
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
        OutlinedButton(onClick = onBack) { Text("Volver") }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Actualizaciones remotas",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "La app consulta un manifiesto en la nube (y el POS en LAN si está disponible). " +
                "No hace falta cable USB para cada tablet.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { showDialog = true }) { Text("Buscar actualización") }
    }
    if (showDialog) {
        OtaUpdateDialog(
            onDismiss = { showDialog = false },
            lanServerUrl = lanServerUrl,
            autoCheck = true,
        )
    }
}

/** Helper para Activity: abrir ajustes de instalación desconocida. */
fun Activity.openUnknownSourcesSettings(manager: OtaUpdateManager) {
    startActivity(manager.intentToAllowUnknownSources())
}
