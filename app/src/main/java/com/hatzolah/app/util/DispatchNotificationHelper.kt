package com.hatzolah.app.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.hatzolah.app.HatzolahApp
import com.hatzolah.app.R
import com.hatzolah.app.data.database.entity.CallLog
import com.hatzolah.app.data.repository.CallLogRepository
import com.hatzolah.app.service.SmsParser
import com.hatzolah.app.ui.DispatchAlertActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue

@Singleton
class DispatchNotificationHelper @Inject constructor(
    private val smsParser: SmsParser,
    private val preferencesManager: PreferencesManager,
    private val callLogRepository: CallLogRepository
) {
    // Fire-and-forget DB write. SupervisorJob prevents parent cancellation.
    private val dbScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    companion object {
        const val DISPATCH_NOTIFICATION_ID = 1001
        private const val PI_REQUEST_ALERT_BASE = 10000
        private const val PI_REQUEST_MAP_BASE = 20000
        private val requestCounter = AtomicInteger(0)
    }

    fun showDispatchNotification(
        context: Context,
        address: String,
        callType: String = "",
        rawMessage: String = "",
        units: String = "",
        age: String = "",
        room: String = ""
    ) {
        // Persist the dispatch so it can be re-shown when user unfolds/unlocks
        preferencesManager.setActiveDispatch(address, callType, rawMessage, units, age)

        // Save to call history - every dispatch creates a call log entry
        dbScope.launch {
            try {
                val memberId = preferencesManager.getLoggedInMemberId()
                callLogRepository.addCallLog(
                    CallLog(
                        memberId = memberId,
                        date = System.currentTimeMillis(),
                        dispatchAddress = address,
                        outcome = callType,
                        medicalNotes = rawMessage,
                        isDocumented = false
                    )
                )
            } catch (_: Throwable) { /* don't crash on DB error */ }
        }

        // Unique request codes per dispatch - bounded integers (Bugs #3, #22)
        val requestId = (requestCounter.incrementAndGet() % 10000).absoluteValue
        val alertRequestCode = PI_REQUEST_ALERT_BASE + requestId
        val mapRequestCode = PI_REQUEST_MAP_BASE + requestId

        // 1. Wake device with timed wake lock (auto-releases) - Bug #1
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wl: PowerManager.WakeLock? = try {
            @Suppress("DEPRECATION")
            pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "hatzolah:dispatch"
            ).apply { acquire(30_000L) }
        } catch (_: Throwable) { null }

        try {
            val alertIntent = DispatchAlertActivity.createIntent(
                context, address, callType, rawMessage, units, age, room
            )
            val alertPendingIntent = PendingIntent.getActivity(
                context, alertRequestCode, alertIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val navigationUri = "google.navigation:q=${smsParser.formatForNavigation(address)}"
            val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(navigationUri))
            val mapPendingIntent = PendingIntent.getActivity(
                context, mapRequestCode, mapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val title = if (callType.isNotBlank()) "DISPATCH: ${callType.uppercase()}" else "DISPATCH CALL"

            val notification = NotificationCompat.Builder(context, HatzolahApp.DISPATCH_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(address)
                .setStyle(NotificationCompat.BigTextStyle().bigText("$address\n$callType\n$rawMessage"))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(false)
                .setOngoing(true)
                .setFullScreenIntent(alertPendingIntent, true)
                .setContentIntent(alertPendingIntent)
                .addAction(android.R.drawable.ic_menu_directions, "NAVIGATE", mapPendingIntent)
                .addAction(android.R.drawable.ic_menu_view, "OPEN ALERT", alertPendingIntent)
                .setVibrate(longArrayOf(0, 800, 300, 800, 300, 800))
                .build()

            notificationManager.notify(DISPATCH_NOTIFICATION_ID, notification)

            // Play alert sound with proper resource management - Bugs #2, #46
            playAlertSound(context)

            // Try direct launch - won't crash if blocked - Bug #13
            try {
                context.startActivity(alertIntent)
            } catch (_: Throwable) { /* background launch blocked; notification handles it */ }
        } finally {
            try { if (wl?.isHeld == true) wl.release() } catch (_: Throwable) { /* already released */ }
        }
    }

    private fun playAlertSound(context: Context) {
        var mp: MediaPlayer? = null
        var audioManager: AudioManager? = null
        var originalVolume = -1
        try {
            audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)

            mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            val afd = context.resources.openRawResourceFd(R.raw.dispatch_alert) ?: return
            try {
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            } finally {
                try { afd.close() } catch (_: Throwable) {}
            }
            mp.prepare()
            val finalAudioManager = audioManager
            val finalOriginalVolume = originalVolume
            mp.setOnCompletionListener { player ->
                try { player.release() } catch (_: Throwable) {}
                try {
                    if (finalOriginalVolume >= 0) {
                        finalAudioManager.setStreamVolume(
                            AudioManager.STREAM_ALARM, finalOriginalVolume, 0
                        )
                    }
                } catch (_: Throwable) {}
            }
            mp.setOnErrorListener { player, _, _ ->
                try { player.release() } catch (_: Throwable) {}
                true
            }
            mp.start()
        } catch (_: Throwable) {
            // Clean up on failure
            try { mp?.release() } catch (_: Throwable) {}
            try {
                if (originalVolume >= 0 && audioManager != null) {
                    audioManager.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume, 0)
                }
            } catch (_: Throwable) {}
        }
    }
}
