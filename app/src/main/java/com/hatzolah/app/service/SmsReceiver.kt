package com.hatzolah.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.hatzolah.app.util.DispatchNotificationHelper
import com.hatzolah.app.util.PreferencesManager
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Monitors incoming SMS messages and triggers dispatch processing
 * when a message arrives from the configured dispatch number.
 */
class SmsReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SmsReceiverEntryPoint {
        fun smsParser(): SmsParser
        fun notificationHelper(): DispatchNotificationHelper
        fun preferencesManager(): PreferencesManager
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext, SmsReceiverEntryPoint::class.java
        )
        val smsParser = entryPoint.smsParser()
        val preferencesManager = entryPoint.preferencesManager()
        val notificationHelper = entryPoint.notificationHelper()

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val dispatchNumber = preferencesManager.getDispatchNumber()

        if (dispatchNumber.isBlank()) return

        for (smsMessage in messages) {
            val sender = smsMessage.displayOriginatingAddress ?: continue
            val body = smsMessage.displayMessageBody ?: continue

            if (normalizePhone(sender) == normalizePhone(dispatchNumber)) {
                handleDispatchMessage(context, smsParser, notificationHelper, body)
            }
        }
    }

    private fun handleDispatchMessage(
        context: Context,
        smsParser: SmsParser,
        notificationHelper: DispatchNotificationHelper,
        message: String
    ) {
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
