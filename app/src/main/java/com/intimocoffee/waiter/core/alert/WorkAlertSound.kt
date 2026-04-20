package com.intimocoffee.waiter.core.alert

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log

/**
 * Alerta fuerte (vibración + tono tipo alarma + pitidos) para que el personal note trabajo nuevo.
 */
object WorkAlertSound {

    private const val TAG = "WorkAlertSound"

    fun play(context: Context) {
        vibrate(context)
        playAlarmRingtoneShort(context)
        playTripleBeepAlarm()
    }

    private fun vibrate(context: Context) {
        try {
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!v.hasVibrator()) return
            }
            val pattern = longArrayOf(0, 380, 110, 380, 110, 480)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            Log.w(TAG, "vibrate: ${e.message}")
        }
    }

    private fun playAlarmRingtoneShort(context: Context) {
        try {
            var uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, uri) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ringtone.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            ringtone.play()
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    ringtone.stop()
                } catch (_: Exception) {
                }
            }, 2400)
        } catch (e: Exception) {
            Log.w(TAG, "ringtone: ${e.message}")
        }
    }

    private fun playTripleBeepAlarm() {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 300)
            val h = Handler(Looper.getMainLooper())
            h.postDelayed({
                try {
                    tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 300)
                } catch (_: Exception) {
                }
            }, 420)
            h.postDelayed({
                try {
                    tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 380)
                } catch (_: Exception) {
                }
            }, 860)
            h.postDelayed({
                try {
                    tg.release()
                } catch (_: Exception) {
                }
            }, 1400)
        } catch (e: Exception) {
            Log.w(TAG, "beep: ${e.message}")
        }
    }
}
