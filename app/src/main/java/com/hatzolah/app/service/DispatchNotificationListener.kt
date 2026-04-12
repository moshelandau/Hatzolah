package com.hatzolah.app.service

import android.app.Notification
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.hatzolah.app.util.DispatchNotificationHelper
import com.hatzolah.app.util.PreferencesManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Listens for incoming SMS notifications and triggers dispatch processing
 * when a message arrives from the configured dispatch number.
 */
class DispatchNotificationListener : NotificationListenerService() {

    private val mainHandler = Handler(Looper.getMainLooper())

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ListenerEntryPoint {
        fun smsParser(): SmsParser
        fun notificationHelper(): DispatchNotificationHelper
        fun preferencesManager(): PreferencesManager
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Clear any stale callbacks to prevent handler leak if onDestroy wasn't called
        try { mainHandler.removeCallbacksAndMessages(null) } catch (_: Throwable) {}

        val pkg = sbn.packageName
        if (!isSmsApp(pkg)) return

        val entryPoint = try {
            EntryPointAccessors.fromApplication(
                applicationContext, ListenerEntryPoint::class.java
            )
        } catch (_: Throwable) { return }

        val preferencesManager = entryPoint.preferencesManager()
        val dispatchNumber = preferencesManager.getDispatchNumber()
        if (dispatchNumber.isBlank()) return

        val extras = sbn.notification.extras
        val title: String
        val text: String
        val bigText: String
        val subText: String
        if (extras != null) {
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
            bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.ifEmpty { text } ?: text
            subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty()
        } else {
            val ticker = sbn.notification.tickerText?.toString().orEmpty()
            if (ticker.isBlank()) return
            title = ""
            text = ticker
            bigText = ticker
            subText = ""
        }

        val allText = "$title $text $bigText $subText"
        val normalizedDispatch = normalizePhone(dispatchNumber)
        if (normalizedDispatch.length < 7) return // avoid false positives on short strings

        val isDispatch = normalizePhone(title) == normalizedDispatch ||
                normalizePhone(allText).contains(normalizedDispatch) ||
                allText.contains(dispatchNumber.takeLast(4)) ||
                allText.contains(dispatchNumber.takeLast(7))

        if (!isDispatch) return

        val smsParser = entryPoint.smsParser()
        val notificationHelper = entryPoint.notificationHelper()

        val messageBody = bigText.ifBlank { text }
        val parsed = smsParser.parseDispatchMessage(messageBody) ?: return

        // Use a Handler to post to main thread instead of creating a new CoroutineScope.
        // NotificationListenerService lives as long as the system wants - creating
        // scopes per-notification leaks memory.
        mainHandler.post {
            try {
                notificationHelper.showDispatchNotification(
                    context = applicationContext,
                    address = parsed.address,
                    callType = parsed.callType,
                    rawMessage = messageBody,
                    units = parsed.units,
                    age = parsed.age,
                    room = parsed.room,
                    cad = parsed.cad
                )
            } catch (_: Throwable) { /* don't crash the listener */ }
        }
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun isSmsApp(packageName: String): Boolean {
        return packageName.contains("messaging") ||
                packageName.contains("sms") ||
                packageName.contains("mms") ||
                packageName.contains("message") ||
                packageName == "com.samsung.android.messaging" ||
                packageName == "com.google.android.apps.messaging" ||
                packageName == "com.android.mms"
    }

    private fun normalizePhone(phone: String): String {
        var digits = phone.replace(Regex("[^0-9]"), "")
        if (digits.length == 11 && digits.startsWith("1")) {
            digits = digits.substring(1)
        }
        return digits.takeLast(10)
    }
}
