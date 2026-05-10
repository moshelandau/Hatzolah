package com.hatzolah.app.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.hatzolah.app.HatzolahApp
import com.hatzolah.app.service.SmsParser
import com.hatzolah.app.ui.MainActivity
import javax.inject.Inject
import javax.inject.Singleton

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

        // Tapping the notification opens the app
        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val appPendingIntent = PendingIntent.getActivity(
            context, 0, appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Navigate action uses geo: URI which works with any maps app
        val navUri = "geo:0,0?q=${Uri.encode(address)}"
        val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(navUri))
        val mapPendingIntent = PendingIntent.getActivity(
            context, 1, mapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

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
            .setContentIntent(appPendingIntent)
            .setFullScreenIntent(appPendingIntent, true)
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
