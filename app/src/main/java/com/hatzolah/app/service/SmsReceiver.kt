package com.hatzolah.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.hatzolah.app.util.DispatchNotificationHelper
import com.hatzolah.app.util.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Monitors incoming SMS messages and triggers dispatch processing
 * when a message arrives from the configured dispatch number.
 */
@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject lateinit var smsParser: SmsParser
    @Inject lateinit var notificationHelper: DispatchNotificationHelper
    @Inject lateinit var preferencesManager: PreferencesManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val dispatchNumber = preferencesManager.getDispatchNumber()

        if (dispatchNumber.isBlank()) return

        for (smsMessage in messages) {
            val sender = smsMessage.displayOriginatingAddress ?: continue
            val body = smsMessage.displayMessageBody ?: continue

            // Check if SMS is from the dispatch number
            if (normalizePhone(sender) == normalizePhone(dispatchNumber)) {
                handleDispatchMessage(context, body)
            }
        }
    }

    private fun handleDispatchMessage(context: Context, message: String) {
        val parsed = smsParser.parseDispatchMessage(message) ?: return

        CoroutineScope(Dispatchers.Main).launch {
            notificationHelper.showDispatchNotification(
                context = context,
                address = parsed.address,
                callType = parsed.callType
            )
        }
    }

    private fun normalizePhone(phone: String): String {
        return phone.replace(Regex("[^0-9+]"), "").takeLast(10)
    }
}
