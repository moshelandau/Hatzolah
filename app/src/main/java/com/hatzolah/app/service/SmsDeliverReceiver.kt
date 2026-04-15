package com.hatzolah.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

/**
 * Required for default SMS app eligibility.
 * Receives SMS_DELIVER intents when this app is the default SMS handler.
 * Delegates to the existing SmsReceiver logic.
 */
class SmsDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return
        // Reuse the same SmsReceiver logic
        SmsReceiver().onReceive(context, intent.apply {
            action = Telephony.Sms.Intents.SMS_RECEIVED_ACTION
        })
    }
}
