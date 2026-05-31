package com.hatzolah.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.net.Uri
import com.hatzolah.app.service.DispatchListenerKeepalive
import com.hatzolah.app.util.CrashLogger
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HatzolahApp : Application() {

    companion object {
        const val DISPATCH_CHANNEL_ID = "dispatch_alerts"
        const val TRACKING_CHANNEL_ID = "location_tracking"
        const val KEEPALIVE_CHANNEL_ID = "dispatch_keepalive"
    }

    override fun onCreate() {
        super.onCreate()
        CrashLogger(this).install()
        createNotificationChannels()
        setDefaultSettings()
        // Start the listener keepalive immediately so the foreground service
        // is up before Android has a chance to put the process to sleep.
        DispatchListenerKeepalive.start(this)
    }

    private fun setDefaultSettings() {
        val prefs = getSharedPreferences("hatzolah_prefs", MODE_PRIVATE)
        if (!prefs.contains("defaults_configured")) {
            prefs.edit()
                .putString("dispatch_number", "8445991212")
                .putString("rma_hotline", "8453675077")
                .putBoolean("defaults_configured", true)
                .apply()
        }
    }

    private fun createNotificationChannels() {
        val notificationManager = getSystemService(NotificationManager::class.java)

        // Delete old channel in case it exists with different settings
        notificationManager.deleteNotificationChannel(DISPATCH_CHANNEL_ID)

        val soundUri = Uri.parse("android.resource://${packageName}/${R.raw.dispatch_alert}")
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val dispatchChannel = NotificationChannel(
            DISPATCH_CHANNEL_ID,
            getString(R.string.dispatch_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.dispatch_channel_description)
            setSound(soundUri, audioAttributes)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 200, 500)
            setShowBadge(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }

        val trackingChannel = NotificationChannel(
            TRACKING_CHANNEL_ID,
            getString(R.string.tracking_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.tracking_channel_description)
        }

        // Silent low-importance channel for the keepalive foreground service.
        // Has to exist so the user can see (and can't accidentally disable
        // away) the persistent "Hatzolah is listening" notification.
        val keepaliveChannel = NotificationChannel(
            KEEPALIVE_CHANNEL_ID,
            "Dispatch Listener",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "Keeps Hatzolah watching for dispatch SMS. Do not disable."
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }

        notificationManager.createNotificationChannel(dispatchChannel)
        notificationManager.createNotificationChannel(trackingChannel)
        notificationManager.createNotificationChannel(keepaliveChannel)
    }
}
