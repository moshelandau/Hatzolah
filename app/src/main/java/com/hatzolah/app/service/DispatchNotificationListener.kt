package com.hatzolah.app.service

import android.Manifest
import android.app.Notification
import android.app.Person
import android.content.pm.PackageManager
import android.os.Build
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
        if (normalizedDispatch.length < 7) return // avoid false positives on short strings

        val smsParser = entryPoint.smsParser()
        val notificationHelper = entryPoint.notificationHelper()

        // Notification preview body — may be truncated on some SMS apps (Samsung).
        val notifBody = bigText.ifBlank { text }

        // Match the sender across three independent signals:
        // 1) Title-based digit match — works when the sender is NOT in contacts;
        //    SMS apps put the raw phone number in the notification title.
        // 2) Person-extras match     — works when the dispatch number IS in contacts;
        //    the title becomes the contact name, but EXTRA_PEOPLE / EXTRA_PEOPLE_LIST
        //    still carries the underlying tel: URI for the sender.
        // 3) Strict-format match     — last-resort fallback: if the preview itself has
        //    the unique "KJ EMS … CALL TYPE:" header, only the dispatch service sends
        //    that, so we accept even when neither sender check matched.
        val normalizedTitle = normalizePhone(title)
        val titleMatches = normalizedTitle.length >= 7 && (
                normalizedTitle == normalizedDispatch ||
                normalizedDispatch.endsWith(normalizedTitle) ||
                normalizedTitle.endsWith(normalizedDispatch))

        val personMatches = !titleMatches && extras != null &&
                personExtrasMatchDispatch(extras, normalizedDispatch)

        val strictFormatMatches = !titleMatches && !personMatches &&
                smsParser.parseDispatchMessage(notifBody, requireCallType = true) != null

        if (!titleMatches && !personMatches && !strictFormatMatches) return

        // Primary: read the full SMS from the inbox (has all fields:
        // address, call type, units, CAD, etc.). Falls back to the
        // notification preview text if READ_SMS isn't granted or the
        // message hasn't landed in the inbox yet.
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

    /**
     * Many SMS apps populate the notification's people-extras with a `tel:` URI for
     * the sender even when the title shows the contact display name. This lets us
     * recognise the dispatch number when the user has it saved as a contact.
     */
    private fun personExtrasMatchDispatch(
        extras: android.os.Bundle,
        normalizedDispatch: String
    ): Boolean {
        // EXTRA_PEOPLE_LIST (API 28+) — array of Person objects. Isolated below so
        // ART doesn't try to verify the Person reference on minSdk 26/27.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            personListMatches(extras, normalizedDispatch)) return true

        // EXTRA_PEOPLE (legacy) — String[] of URIs.
        try {
            val legacy = extras.getStringArray(Notification.EXTRA_PEOPLE)
            if (legacy != null) {
                for (uri in legacy) {
                    if (uri != null && uriMatches(uri, normalizedDispatch)) return true
                }
            }
        } catch (_: Throwable) { /* ignore */ }
        return false
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.P)
    private fun personListMatches(
        extras: android.os.Bundle,
        normalizedDispatch: String
    ): Boolean {
        return try {
            @Suppress("DEPRECATION")
            val people = extras.getParcelableArrayList<Person>(Notification.EXTRA_PEOPLE_LIST)
                ?: return false
            people.any { p ->
                val uri = p.uri
                uri != null && uriMatches(uri, normalizedDispatch)
            }
        } catch (_: Throwable) { false }
    }

    private fun uriMatches(uri: String, normalizedDispatch: String): Boolean {
        if (!uri.startsWith("tel:", ignoreCase = true)) return false
        val candidate = normalizePhone(uri.substring(4))
        if (candidate.length < 7) return false
        return candidate == normalizedDispatch ||
                normalizedDispatch.endsWith(candidate) ||
                candidate.endsWith(normalizedDispatch)
    }
}
