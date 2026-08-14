package com.intimocoffee.waiter.core.alert

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import java.util.concurrent.atomic.AtomicReference

/**
 * Alerta muy audible (alarma a máximo volumen + vibración fuerte)
 * para que el mesero la note aunque el teléfono esté en el bolsillo.
 */
object WorkAlertSound {

    private const val TAG = "WorkAlertSound"
    private const val ALERT_MS = 5_500L

    private val activePlayer = AtomicReference<MediaPlayer?>(null)
    private val mainHandler = Handler(Looper.getMainLooper())

    fun play(context: Context) {
        val appCtx = context.applicationContext
        vibrateStrong(appCtx)
        boostAndPlay(appCtx)
    }

    private fun boostAndPlay(context: Context) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val prevAlarm = am?.getStreamVolume(AudioManager.STREAM_ALARM)
        val prevMusic = am?.getStreamVolume(AudioManager.STREAM_MUSIC)
        val prevNotif = am?.getStreamVolume(AudioManager.STREAM_NOTIFICATION)

        try {
            am?.let { audio ->
                audio.setStreamVolume(
                    AudioManager.STREAM_ALARM,
                    audio.getStreamMaxVolume(AudioManager.STREAM_ALARM),
                    0,
                )
                // Por si el tono cae a otro stream en algunos equipos.
                audio.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                    0,
                )
                audio.setStreamVolume(
                    AudioManager.STREAM_NOTIFICATION,
                    audio.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION),
                    0,
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "boost volume: ${e.message}")
        }

        playLoopingAlarm(context)
        playTripleBeepAlarm()

        mainHandler.postDelayed({
            stopPlayer()
            try {
                am?.let { audio ->
                    prevAlarm?.let { audio.setStreamVolume(AudioManager.STREAM_ALARM, it, 0) }
                    prevMusic?.let { audio.setStreamVolume(AudioManager.STREAM_MUSIC, it, 0) }
                    prevNotif?.let { audio.setStreamVolume(AudioManager.STREAM_NOTIFICATION, it, 0) }
                }
            } catch (e: Exception) {
                Log.w(TAG, "restore volume: ${e.message}")
            }
        }, ALERT_MS)
    }

    private fun playLoopingAlarm(context: Context) {
        try {
            stopPlayer()
            var uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            if (uri == null) return

            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                        .build(),
                )
                setDataSource(context, uri)
                isLooping = true
                setVolume(1f, 1f)
                prepare()
                start()
            }
            activePlayer.set(player)
        } catch (e: Exception) {
            Log.w(TAG, "mediaPlayer: ${e.message}")
            // Fallback: ringtone corto
            playAlarmRingtoneFallback(context)
        }
    }

    private fun playAlarmRingtoneFallback(context: Context) {
        try {
            var uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, uri) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone.isLooping = true
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ringtone.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                    .build()
            }
            ringtone.play()
            mainHandler.postDelayed({
                try {
                    ringtone.stop()
                } catch (_: Exception) {
                }
            }, ALERT_MS)
        } catch (e: Exception) {
            Log.w(TAG, "ringtone fallback: ${e.message}")
        }
    }

    private fun stopPlayer() {
        try {
            activePlayer.getAndSet(null)?.let { mp ->
                if (mp.isPlaying) mp.stop()
                mp.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "stopPlayer: ${e.message}")
        }
    }

    private fun vibrateStrong(context: Context) {
        try {
            val v: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (v == null || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !v.hasVibrator())) return

            // Patrón largo y agresivo para bolsillo.
            val pattern = longArrayOf(
                0,
                450, 120, 450, 120, 450, 120,
                650, 150, 650, 150,
                450, 120, 450,
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amplitudes = intArrayOf(
                    0,
                    255, 0, 255, 0, 255, 0,
                    255, 0, 255, 0,
                    255, 0, 255,
                )
                v.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            Log.w(TAG, "vibrate: ${e.message}")
        }
    }

    private fun playTripleBeepAlarm() {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            val delays = longArrayOf(0, 500, 1000, 1600, 2200, 2800)
            delays.forEach { delay ->
                mainHandler.postDelayed({
                    try {
                        tg.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 350)
                    } catch (_: Exception) {
                    }
                }, delay)
            }
            mainHandler.postDelayed({
                try {
                    tg.release()
                } catch (_: Exception) {
                }
            }, 3600)
        } catch (e: Exception) {
            Log.w(TAG, "beep: ${e.message}")
        }
    }
}
