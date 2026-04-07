package com.hatzolah.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HatzolahApp : Application() {

    companion object {
        const val DISPATCH_CHANNEL_ID = "dispatch_alerts"
        const val TRACKING_CHANNEL_ID = "location_tracking"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val notificationManager = getSystemService(NotificationManager::class.java)

        val dispatchChannel = NotificationChannel(
            DISPATCH_CHANNEL_ID,
            getString(R.string.dispatch_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.dispatch_channel_description)
            enableVibration(true)
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

        notificationManager.createNotificationChannel(dispatchChannel)
        notificationManager.createNotificationChannel(trackingChannel)
    }
}
