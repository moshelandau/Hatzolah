package com.hatzolah.app.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.hatzolah.app.HatzolahApp
import com.hatzolah.app.R
import com.hatzolah.app.service.SmsParser
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles creating and showing high-priority lock screen notifications
 * for incoming dispatch calls with a one-tap navigate button.
 */
@Singleton
class DispatchNotificationHelper @Inject constructor(
    private val smsParser: SmsParser
) {
    companion object {
        const val DISPATCH_NOTIFICATION_ID = 1001
    }

    fun showDispatchNotification(context: Context, address: String, callType: String = "") {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Google Maps navigation intent
        val navigationUri = "google.navigation:q=${smsParser.formatForNavigation(address)}"
        val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(navigationUri)).apply {
            setPackage("com.google.android.apps.maps")
        }
        val mapPendingIntent = PendingIntent.getActivity(
            context, 0, mapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the full-screen notification that shows on lock screen
        val title = if (callType.isNotBlank()) "Dispatch: $callType" else "Dispatch Call"

        val notification = NotificationCompat.Builder(context, HatzolahApp.DISPATCH_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(address)
            .setStyle(NotificationCompat.BigTextStyle().bigText(address))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setFullScreenIntent(mapPendingIntent, true)
            .addAction(
                android.R.drawable.ic_menu_directions,
                "Navigate",
                mapPendingIntent
            )
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager.notify(DISPATCH_NOTIFICATION_ID, notification)
    }
}
