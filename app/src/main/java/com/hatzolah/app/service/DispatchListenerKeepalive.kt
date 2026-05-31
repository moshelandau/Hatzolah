package com.hatzolah.app.service

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.os.IBinder
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationCompat
import com.hatzolah.app.HatzolahApp
import com.hatzolah.app.R
import com.hatzolah.app.ui.MainActivity
import com.hatzolah.app.util.NotificationDebugLog

/**
 * Permanent foreground service whose only job is to keep this app's process
 * alive on aggressive OEMs (especially Samsung). The dispatch notification
 * listener is hosted in our process, so if Samsung puts us to sleep — which
 * it does silently after a few hours of inactivity — the listener stops
 * receiving SMS notifications entirely until the user next opens the app.
 *
 * A foreground service with an ongoing low-importance notification is the
 * standard, sanctioned way for emergency-response apps to opt out of that.
 * We also use every callback as an opportunity to request a rebind of the
 * notification listener, in case Android severed the binding without
 * notifying us.
 */
class DispatchListenerKeepalive : Service() {

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
        requestListenerRebind()
        NotificationDebugLog.log(this, "KEEPALIVE started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Re-request rebind on every start command — cheap, idempotent, and
        // recovers from "listener was killed without onListenerDisconnected".
        requestListenerRebind()
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Some launchers / OEM task killers will try to take us down when the
        // user swipes the app off recents. Resurrect via a sticky restart.
        val restart = Intent(applicationContext, DispatchListenerKeepalive::class.java)
        try { startService(restart) } catch (_: Throwable) {}
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        NotificationDebugLog.log(this, "KEEPALIVE destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun requestListenerRebind() {
        try {
            NotificationListenerService.requestRebind(
                ComponentName(this, DispatchNotificationListener::class.java)
            )
        } catch (_: Throwable) { /* OEM may block; nothing actionable */ }
    }

    private fun buildNotification(): android.app.Notification {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pi = android.app.PendingIntent.getActivity(
            this, 0, tapIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, HatzolahApp.KEEPALIVE_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Hatzolah is listening")
            .setContentText("Watching for dispatch alerts. Do not stop the app.")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setShowWhen(false)
            .setContentIntent(pi)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1002

        fun start(context: android.content.Context) {
            try {
                val intent = Intent(context, DispatchListenerKeepalive::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Throwable) { /* foreground-start restrictions; nothing to do */ }
        }
    }
}
