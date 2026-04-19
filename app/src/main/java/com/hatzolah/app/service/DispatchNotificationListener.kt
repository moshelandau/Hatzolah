package com.hatzolah.app.service

import android.Manifest
import android.app.Notification
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.content.ContextCompat
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
        try { mainHandler.removeCallbacksAndMessages(null) } catch (_: Throwable) {}

        val pkg = sbn.packageName
        if (!isSmsApp(pkg)) return

        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

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
        if (extras != null) {
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
            bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.ifEmpty { text } ?: text
        } else {
            val ticker = sbn.notification.tickerText?.toString().orEmpty()
            if (ticker.isBlank()) return
            title = ""
            text = ticker
            bigText = ticker
        }

        val normalizedDispatch = normalizePhone(dispatchNumber)
        if (normalizedDispatch.length < 7) return

        // --- Dispatch identification ---
        // Step 1: try matching the notification title (the sender) against
        //         the configured dispatch number via digit comparison.
        val normalizedTitle = normalizePhone(title)
        val titleMatchesDispatch = normalizedTitle == normalizedDispatch ||
                (normalizedTitle.length >= 7 && normalizedDispatch.endsWith(normalizedTitle)) ||
                (normalizedTitle.length >= 7 && normalizedTitle.endsWith(normalizedDispatch))

        // Step 2: if the title is a contact name (e.g. "Hatzolah Dispatch")
        //         instead of a raw phone number, normalizedTitle will have
        //         too few digits to match. Fall back to checking the SMS
        //         inbox for a recent message from the dispatch number.
        var inboxBody: String? = null
        if (titleMatchesDispatch) {
            inboxBody = tryReadLatestSmsFrom(normalizedDispatch)
        } else if (normalizedTitle.length < 7) {
            inboxBody = tryReadLatestSmsFrom(normalizedDispatch)
            if (inboxBody == null) return
        } else {
            return
        }

        val smsParser = entryPoint.smsParser()
        val notificationHelper = entryPoint.notificationHelper()

        val notifBody = bigText.ifBlank { text }
        val messageBody = inboxBody ?: notifBody

        val parsed = smsParser.parseDispatchMessage(messageBody)
            ?: smsParser.parseDispatchMessage(notifBody)
            ?: return

        val rawForDisplay = inboxBody ?: notifBody

        mainHandler.post {
            try {
                notificationHelper.showDispatchNotification(
                    context = applicationContext,
                    address = parsed.address,
                    callType = parsed.callType,
                    rawMessage = rawForDisplay,
                    units = parsed.units,
                    age = parsed.age,
                    room = parsed.room,
                    cad = parsed.cad
                )
            } catch (_: Throwable) {}
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

    /**
     * Reads the most recent SMS from the given normalized phone number out
     * of the device inbox. Only considers messages received within the last
     * 2 minutes to avoid re-triggering on old dispatches.
     *
     * Returns the full message body, or `null` if READ_SMS isn't granted,
     * the message isn't in the inbox yet, or there's no recent match.
     */
    private fun tryReadLatestSmsFrom(normalizedPhone: String): String? {
        try {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return null

            val cutoff = System.currentTimeMillis() - 120_000L
            val cursor = contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
                "${Telephony.Sms.DATE} > ?",
                arrayOf(cutoff.toString()),
                "${Telephony.Sms.DATE} DESC"
            ) ?: return null

            cursor.use {
                while (it.moveToNext()) {
                    val address = it.getString(0) ?: continue
                    val body = it.getString(1) ?: continue
                    if (normalizePhone(address) == normalizedPhone) {
                        return body
                    }
                }
            }
        } catch (e: Throwable) {
            Log.w("Hatzolah", "Could not read SMS inbox for dispatch body", e)
        }
        return null
    }

    private fun normalizePhone(phone: String): String {
        var digits = phone.replace(Regex("[^0-9]"), "")
        if (digits.length == 11 && digits.startsWith("1")) {
            digits = digits.substring(1)
        }
        return digits.takeLast(10)
    }
}
