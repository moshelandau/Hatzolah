package com.hatzolah.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import com.hatzolah.app.data.database.entity.CallLog
import com.hatzolah.app.data.repository.CallLogRepository
import com.hatzolah.app.util.DispatchNotificationHelper
import com.hatzolah.app.util.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject lateinit var smsParser: SmsParser
    @Inject lateinit var notificationHelper: DispatchNotificationHelper
    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var callLogRepository: CallLogRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val dispatchNumber = preferencesManager.getDispatchNumber()

        if (dispatchNumber.isBlank()) return
        val normalizedDispatch = normalizePhone(dispatchNumber)

        for (smsMessage in messages) {
            val body = smsMessage.displayMessageBody ?: continue

            // Use raw originatingAddress for reliable matching (not affected by contacts)
            val rawSender = smsMessage.originatingAddress
            val displaySender = smsMessage.displayOriginatingAddress

            val senderMatches = when {
                rawSender != null && normalizePhone(rawSender) == normalizedDispatch -> true
                displaySender != null && normalizePhone(displaySender) == normalizedDispatch -> true
                else -> false
            }

            if (senderMatches) {
                handleDispatchMessage(context, body)
            }
        }
    }

    private fun handleDispatchMessage(context: Context, message: String) {
        val parsed = smsParser.parseDispatchMessage(message) ?: return
        val memberId = preferencesManager.getLoggedInMemberId()

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var callLogId = -1L

                if (memberId != -1L) {
                    callLogId = callLogRepository.addCallLog(
                        CallLog(
                            memberId = memberId,
                            date = System.currentTimeMillis(),
                            dispatchAddress = parsed.address
                        )
                    )
                }

                // Start location tracking for mileage
                if (callLogId > 0) {
                    try {
                        val trackingIntent = Intent(context, LocationTrackingService::class.java).apply {
                            putExtra(LocationTrackingService.EXTRA_CALL_LOG_ID, callLogId)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(trackingIntent)
                        } else {
                            context.startService(trackingIntent)
                        }
                    } catch (_: Exception) {
                        // Location tracking is optional — don't block dispatch
                    }
                }

                notificationHelper.showDispatchNotification(
                    context = context,
                    address = parsed.address,
                    callType = parsed.callType
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun normalizePhone(phone: String): String {
        return phone.replace(Regex("[^0-9]"), "").takeLast(10)
    }
}
