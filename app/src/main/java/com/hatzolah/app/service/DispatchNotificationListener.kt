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
        // Clear any stale callbacks to prevent handler leak if onDestroy wasn't called
        try { mainHandler.removeCallbacksAndMessages(null) } catch (_: Throwable) {}

        val pkg = sbn.packageName
        if (!isSmsApp(pkg)) return

        // Skip group summary notifications — they contain aggregated text from
        // multiple conversations (e.g. "(844) 599-1212, (845) 481-0055") which
        // the parser cannot extract a real address from.
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

        val normalizedDispatch = normalizePhone(dispatchNumber)
        if (normalizedDispatch.length < 7) return // avoid false positives on short strings

        // Match the sender from the notification title (individual SMS notifications
        // use the sender as the title). Require at least the last 7 digits to match
        // to avoid false positives from group summaries or short digit sequences.
        val normalizedTitle = normalizePhone(title)
        val isDispatch = normalizedTitle == normalizedDispatch ||
                (normalizedTitle.length >= 7 && normalizedDispatch.endsWith(normalizedTitle)) ||
                (normalizedTitle.length >= 7 && normalizedTitle.endsWith(normalizedDispatch))

        if (!isDispatch) return

        val smsParser = entryPoint.smsParser()
        val notificationHelper = entryPoint.notificationHelper()

        // Primary: read the full SMS from the inbox (has all fields:
        // address, call type, units, CAD, etc.). Falls back to the
        // notification preview text if READ_SMS isn't granted or the
        // message hasn't landed in the inbox yet.
        val notifBody = bigText.ifBlank { text }
        val fullSmsBody = tryReadLatestSmsFrom(normalizedDispatch)
        val messageBody = fullSmsBody ?: notifBody

        val parsed = smsParser.parseDispatchMessage(messageBody)
            ?: smsParser.parseDispatchMessage(notifBody)
            ?: return

        val rawForDisplay = fullSmsBody ?: notifBody

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

    /**
     * Reads the most recent SMS from the given normalized phone number out
     * of the device inbox. Returns the full message body, or `null` if
     * READ_SMS isn't granted or the message isn't in the inbox yet.
     *
     * This is the primary dispatch-data source because notification previews
     * (especially Samsung) often truncate the message to the first line,
     * losing the CALL TYPE / UNITS / address fields the parser needs.
     */
    private fun tryReadLatestSmsFrom(normalizedPhone: String): String? {
        try {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return null

            val cursor = contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY),
                null, null,
                "${Telephony.Sms.DATE} DESC"
            ) ?: return null

            cursor.use {
                var checked = 0
                while (it.moveToNext() && checked < 5) {
                    val address = it.getString(0) ?: continue
                    val body = it.getString(1) ?: continue
                    checked++
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
