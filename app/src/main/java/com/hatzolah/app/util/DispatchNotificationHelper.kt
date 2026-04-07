package com.hatzolah.app.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.hatzolah.app.HatzolahApp
import com.hatzolah.app.R
import com.hatzolah.app.service.SmsParser
import com.hatzolah.app.ui.DispatchAlertActivity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DispatchNotificationHelper @Inject constructor(
    private val smsParser: SmsParser
) {
    companion object {
        const val DISPATCH_NOTIFICATION_ID = 1001
    }

    fun showDispatchNotification(context: Context, address: String, callType: String = "", rawMessage: String = "", units: String = "", age: String = "") {
        val alertIntent = DispatchAlertActivity.createIntent(context, address, callType, rawMessage, units, age)

        // Wake the device
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wl = pm.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "hatzolah:dispatch"
        )
        wl.acquire(10000) // 10 seconds

        // Play alert sound LOUD - even on vibrate/silent mode
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            // Save current state
            val originalVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_ALARM)
            val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM)
            // Force alarm volume to max
            audioManager.setStreamVolume(
                android.media.AudioManager.STREAM_ALARM,
                maxVolume,
                0
            )

            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            val afd = context.resources.openRawResourceFd(R.raw.dispatch_alert)
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mp.prepare()
            mp.setOnCompletionListener { player ->
                player.release()
                // Restore original volume
                audioManager.setStreamVolume(android.media.AudioManager.STREAM_ALARM, originalVolume, 0)
            }
            mp.start()
        } catch (_: Exception) {}

        // Build notification with full-screen intent
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val alertPendingIntent = PendingIntent.getActivity(
            context, 1, alertIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val navigationUri = "google.navigation:q=${smsParser.formatForNavigation(address)}"
        val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(navigationUri)).apply {
            setPackage("com.google.android.apps.maps")
        }
        val mapPendingIntent = PendingIntent.getActivity(
            context, 0, mapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (callType.isNotBlank()) "DISPATCH: $callType" else "DISPATCH CALL"

        val notification = NotificationCompat.Builder(context, HatzolahApp.DISPATCH_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(address)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$address\n\n$rawMessage"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(true)
            .setFullScreenIntent(alertPendingIntent, true)
            .setContentIntent(alertPendingIntent)
            .addAction(android.R.drawable.ic_menu_directions, "NAVIGATE", mapPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "OPEN ALERT", alertPendingIntent)
            .setVibrate(longArrayOf(0, 800, 300, 800, 300, 800))
            .setSilent(false)
            .build()

        // Show notification FIRST (this triggers full-screen intent on lock screen)
        notificationManager.notify(DISPATCH_NOTIFICATION_ID, notification)

        // Also try direct activity launch (works when screen is on)
        try {
            context.startActivity(alertIntent)
        } catch (_: Exception) {
            // Background activity start blocked - the full-screen intent will handle it
        }
    }
}
