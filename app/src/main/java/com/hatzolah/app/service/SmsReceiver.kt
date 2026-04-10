package com.hatzolah.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import com.hatzolah.app.util.DispatchNotificationHelper
import com.hatzolah.app.util.PreferencesManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Fallback SMS receiver - only works if app is the default SMS handler
 * or on older Android versions. The primary dispatch mechanism is
 * DispatchNotificationListener which reads SMS notifications.
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

        val entryPoint = try {
            EntryPointAccessors.fromApplication(
                context.applicationContext, SmsReceiverEntryPoint::class.java
            )
        } catch (_: Throwable) { return }

        val smsParser = entryPoint.smsParser()
        val preferencesManager = entryPoint.preferencesManager()
        val notificationHelper = entryPoint.notificationHelper()

        val dispatchNumber = preferencesManager.getDispatchNumber()
        if (dispatchNumber.isBlank()) return

        val messages = try {
            Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        } catch (_: Throwable) { return }

        val dispatchTail = dispatchNumber.takeLast(10).filter { it.isDigit() }
        if (dispatchTail.length < 7) return

        for (smsMessage in messages) {
            val sender = smsMessage.displayOriginatingAddress ?: continue
            val body = smsMessage.displayMessageBody ?: continue

            if (normalizePhone(sender) == normalizePhone(dispatchNumber)) {
                val parsed = smsParser.parseDispatchMessage(body) ?: continue
                // Post back to main thread via handler instead of new scope
                Handler(Looper.getMainLooper()).post {
                    try {
                        notificationHelper.showDispatchNotification(
                            context = context.applicationContext,
                            address = parsed.address,
                            callType = parsed.callType,
                            rawMessage = body,
                            units = parsed.units,
                            age = parsed.age
                        )
                    } catch (_: Throwable) { /* don't crash receiver */ }
                }
            }
        }
    }

    private fun normalizePhone(phone: String): String {
        return phone.replace(Regex("[^0-9+]"), "").takeLast(10)
    }
}
