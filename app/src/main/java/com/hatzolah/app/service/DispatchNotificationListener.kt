package com.hatzolah.app.service

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
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
 * Listens for incoming SMS notifications and triggers dispatch processing
 * when a message arrives from the configured dispatch number.
 * This approach works on Samsung and other devices that restrict SMS permissions.
 */
class DispatchNotificationListener : NotificationListenerService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ListenerEntryPoint {
        fun smsParser(): SmsParser
        fun notificationHelper(): DispatchNotificationHelper
        fun preferencesManager(): PreferencesManager
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Only process SMS/messaging app notifications
        val pkg = sbn.packageName
        if (!isSmsApp(pkg)) return

        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext, ListenerEntryPoint::class.java
        )
        val preferencesManager = entryPoint.preferencesManager()
        val dispatchNumber = preferencesManager.getDispatchNumber()
        if (dispatchNumber.isBlank()) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: text

        // Check if the notification sender matches the dispatch number
        val normalizedDispatch = normalizePhone(dispatchNumber)
        val normalizedTitle = normalizePhone(title)

        if (normalizedTitle == normalizedDispatch || title.contains(dispatchNumber.takeLast(4))) {
            val smsParser = entryPoint.smsParser()
            val notificationHelper = entryPoint.notificationHelper()

            val messageBody = bigText.ifBlank { text }
            val parsed = smsParser.parseDispatchMessage(messageBody) ?: return

            CoroutineScope(Dispatchers.Main).launch {
                notificationHelper.showDispatchNotification(
                    context = applicationContext,
                    address = parsed.address,
                    callType = parsed.callType,
                    rawMessage = messageBody
                )
            }
        }
    }

    private fun isSmsApp(packageName: String): Boolean {
        return packageName.contains("messaging") ||
                packageName.contains("sms") ||
                packageName.contains("mms") ||
                packageName == "com.samsung.android.messaging" ||
                packageName == "com.google.android.apps.messaging" ||
                packageName == "com.android.mms"
    }

    private fun normalizePhone(phone: String): String {
        return phone.replace(Regex("[^0-9]"), "").takeLast(10)
    }
}
