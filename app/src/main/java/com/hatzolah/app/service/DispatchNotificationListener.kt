package com.hatzolah.app.service

import android.Manifest
import android.app.Notification
import android.app.Person
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.provider.Telephony
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.content.ContextCompat
import com.hatzolah.app.util.DispatchNotificationHelper
import com.hatzolah.app.util.NotificationDebugLog
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

    // Cached display name for the current dispatch number — resolved via the
    // device's contacts. We refresh it whenever the dispatch number changes
    // (rare) and otherwise reuse it across notifications.
    @Volatile private var cachedDispatchNumber: String = ""
    @Volatile private var cachedDispatchContactName: String? = null
    @Volatile private var cachedContactLookupAttempted: Boolean = false

    companion object {
        // Reject any dispatch whose underlying SMS is older than this window.
        // Anything older is almost certainly a re-broadcast of an old unread
        // notification (e.g. Samsung Messages re-posts when the user clears
        // the notification shade) and is unsafe to surface as a fresh call.
        private const val MAX_DISPATCH_AGE_MS = 10 * 60_000L  // 10 minutes
    }

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
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) {
            NotificationDebugLog.log(applicationContext, "SKIP pkg=$pkg reason=group_summary")
            return
        }

        val entryPoint = try {
            EntryPointAccessors.fromApplication(
                applicationContext, ListenerEntryPoint::class.java
            )
        } catch (_: Throwable) {
            NotificationDebugLog.log(applicationContext, "SKIP pkg=$pkg reason=entry_point_failed")
            return
        }

        val preferencesManager = entryPoint.preferencesManager()
        val dispatchNumber = preferencesManager.getDispatchNumber()
        if (dispatchNumber.isBlank()) {
            NotificationDebugLog.log(applicationContext, "SKIP pkg=$pkg reason=dispatch_number_blank")
            return
        }

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
            if (ticker.isBlank()) {
                NotificationDebugLog.log(applicationContext, "SKIP pkg=$pkg reason=no_extras_no_ticker")
                return
            }
            title = ""
            text = ticker
            bigText = ticker
        }

        val normalizedDispatch = normalizePhone(dispatchNumber)
        if (normalizedDispatch.length < 7) {
            NotificationDebugLog.log(applicationContext, "SKIP pkg=$pkg reason=dispatch_too_short=$normalizedDispatch")
            return
        }

        val smsParser = entryPoint.smsParser()
        val notificationHelper = entryPoint.notificationHelper()

        // Notification preview body — may be truncated on some SMS apps (Samsung).
        val notifBody = bigText.ifBlank { text }

        // Match the sender across four independent signals:
        // 1) Title-based digit match — works when the sender is NOT in contacts;
        //    SMS apps put the raw phone number in the notification title.
        // 2) Person-extras match     — works when the dispatch number IS in contacts;
        //    the title becomes the contact name, but some SMS apps still carry
        //    a tel: URI in EXTRA_PEOPLE / EXTRA_PEOPLE_LIST.
        // 3) Contacts lookup         — most reliable contact-aware match: resolve
        //    the dispatch number to its display name via ContactsContract.PhoneLookup
        //    and compare against the notification title. Catches the common
        //    "saved as 'Hatzolah Dispatch' contact" case that breaks the first two.
        // 4) Strict-format match     — last-resort fallback: if the preview itself has
        //    the unique "KJ EMS … CALL TYPE:" header, only the dispatch service sends
        //    that, so we accept even when no sender check matched.
        val normalizedTitle = normalizePhone(title)
        val titleMatches = normalizedTitle.length >= 7 && (
                normalizedTitle == normalizedDispatch ||
                normalizedDispatch.endsWith(normalizedTitle) ||
                normalizedTitle.endsWith(normalizedDispatch))

        val personMatches = !titleMatches && extras != null &&
                personExtrasMatchDispatch(extras, normalizedDispatch)

        val contactNameMatches = !titleMatches && !personMatches &&
                title.isNotBlank() &&
                titleMatchesDispatchContact(title, dispatchNumber)

        val strictFormatMatches = !titleMatches && !personMatches && !contactNameMatches &&
                smsParser.parseDispatchMessage(notifBody, requireCallType = true) != null

        val resolvedContactName = resolveDispatchContactName(dispatchNumber) ?: "(none)"
        val titlePreview = title.take(40).ifBlank { "(empty)" }
        val bodyPreview = notifBody.replace('\n', ' ').take(60).ifBlank { "(empty)" }
        val matchSummary = "title=$titleMatches person=$personMatches contact=$contactNameMatches strict=$strictFormatMatches"

        if (!titleMatches && !personMatches && !contactNameMatches && !strictFormatMatches) {
            NotificationDebugLog.log(
                applicationContext,
                "REJECT pkg=$pkg title='$titlePreview' titleDigits=$normalizedTitle dispatchDigits=$normalizedDispatch resolvedContact='$resolvedContactName' bodyLen=${notifBody.length} body='$bodyPreview' $matchSummary"
            )
            return
        }
        NotificationDebugLog.log(
            applicationContext,
            "ACCEPT pkg=$pkg title='$titlePreview' resolvedContact='$resolvedContactName' bodyLen=${notifBody.length} $matchSummary"
        )

        // Primary: read the full SMS from the inbox (has all fields:
        // address, call type, units, CAD, etc.). Falls back to the
        // notification preview text if READ_SMS isn't granted or the
        // message hasn't landed in the inbox yet.
        val inboxRow = tryReadLatestSmsFrom(normalizedDispatch)

        // Hard guard against stale dispatches: if the inbox row is older
        // than MAX_DISPATCH_AGE_MS, this is almost certainly a re-broadcast
        // of an unread notification (Samsung Messages re-posts when the user
        // clears the shade). Firing the popup for an 11-hour-old call could
        // send a responder to a long-resolved scene — abort.
        if (inboxRow != null && inboxRow.ageMs > MAX_DISPATCH_AGE_MS) {
            NotificationDebugLog.log(
                applicationContext,
                "REJECT pkg=$pkg reason=sms_too_old ageMin=${inboxRow.ageMs / 60_000}"
            )
            return
        }

        val fullSmsBody = inboxRow?.body
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
                NotificationDebugLog.log(applicationContext, "POPUP completed addr='${parsed.address.take(40)}'")
            } catch (t: Throwable) {
                NotificationDebugLog.log(
                    applicationContext,
                    "POPUP EXCEPTION ${t.javaClass.simpleName}: ${t.message?.take(200)}"
                )
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        NotificationDebugLog.log(applicationContext, "LISTENER connected")
    }

    override fun onListenerDisconnected() {
        NotificationDebugLog.log(applicationContext, "LISTENER disconnected")
        super.onListenerDisconnected()
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
     * of the device inbox. Returns the row's body + age-in-ms, or `null` if
     * READ_SMS isn't granted or the message isn't in the inbox yet.
     *
     * This is the primary dispatch-data source because notification previews
     * (especially Samsung) often truncate the message to the first line,
     * losing the CALL TYPE / UNITS / address fields the parser needs.
     *
     * The age is used to discard stale dispatches: when the user clears the
     * notification shade, some SMS apps re-broadcast their unread notification
     * to the listener, which we'd otherwise treat as a fresh dispatch even
     * though the underlying SMS is hours old.
     */
    private data class InboxRow(val body: String, val ageMs: Long)

    private fun tryReadLatestSmsFrom(normalizedPhone: String): InboxRow? {
        try {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return null

            val cursor = contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
                null, null,
                "${Telephony.Sms.DATE} DESC"
            ) ?: return null

            cursor.use {
                var checked = 0
                while (it.moveToNext() && checked < 5) {
                    val address = it.getString(0) ?: continue
                    val body = it.getString(1) ?: continue
                    val date = it.getLong(2)
                    checked++
                    if (normalizePhone(address) == normalizedPhone) {
                        return InboxRow(body, System.currentTimeMillis() - date)
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

    /**
     * Returns true when [notifTitle] matches the display name stored against
     * the dispatch number in the device's contacts. Lets us recognise the
     * dispatch sender when the user has saved the number as a contact (so
     * the SMS app shows the contact name in the notification title instead
     * of the raw phone number).
     */
    private fun titleMatchesDispatchContact(notifTitle: String, dispatchNumber: String): Boolean {
        val contactName = resolveDispatchContactName(dispatchNumber) ?: return false
        val titleNorm = notifTitle.trim().lowercase()
        val contactNorm = contactName.trim().lowercase()
        if (titleNorm.isBlank() || contactNorm.isBlank()) return false
        // Some SMS apps suffix the contact name (e.g. "Hatzolah Dispatch (3)"
        // for unread count), so accept startsWith or substring containment.
        return titleNorm == contactNorm ||
                titleNorm.startsWith(contactNorm) ||
                titleNorm.contains(contactNorm)
    }

    private fun resolveDispatchContactName(dispatchNumber: String): String? {
        if (dispatchNumber.isBlank()) return null
        if (cachedDispatchNumber == dispatchNumber && cachedContactLookupAttempted) {
            return cachedDispatchContactName
        }
        cachedDispatchNumber = dispatchNumber
        cachedContactLookupAttempted = true
        cachedDispatchContactName = null

        val granted = ContextCompat.checkSelfPermission(
            applicationContext, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return null

        try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(dispatchNumber)
            )
            applicationContext.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null
            )?.use { c ->
                if (c.moveToFirst()) {
                    val name = c.getString(0)
                    if (!name.isNullOrBlank()) cachedDispatchContactName = name
                }
            }
        } catch (e: Throwable) {
            Log.w("Hatzolah", "Contact lookup for dispatch number failed", e)
        }
        return cachedDispatchContactName
    }
}
