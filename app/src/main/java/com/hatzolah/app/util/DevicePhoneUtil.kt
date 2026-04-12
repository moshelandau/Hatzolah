package com.hatzolah.app.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

/**
 * Reads the device's own phone number from the SIM / telephony settings.
 *
 * Note: This is carrier-dependent. Many carriers do not populate the line1Number
 * field on the SIM, so this may return null even with permission granted. Callers
 * must handle the null case and fall back to manual entry.
 */
object DevicePhoneUtil {

    /**
     * Returns the permission needed to read the device's phone number on this API level.
     * API 26+: READ_PHONE_NUMBERS (less invasive)
     * Below 26: READ_PHONE_STATE
     */
    fun requiredPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Manifest.permission.READ_PHONE_NUMBERS
        } else {
            Manifest.permission.READ_PHONE_STATE
        }
    }

    fun hasPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, requiredPermission()
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Attempts to read the device's phone number.
     * Returns null if permission is not granted, SIM is absent, or the carrier
     * does not expose the line number.
     */
    @SuppressLint("MissingPermission", "HardwareIds")
    fun getDevicePhoneNumber(context: Context): String? {
        if (!hasPermission(context)) return null
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+: prefer SubscriptionManager.getPhoneNumber()
                val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
                val activeSub = subscriptionManager?.activeSubscriptionInfoList?.firstOrNull()
                if (activeSub != null) {
                    val num = subscriptionManager.getPhoneNumber(activeSub.subscriptionId)
                    if (!num.isNullOrBlank()) return num
                }
                // Fall through to TelephonyManager as a last resort
                telephonyLineNumber(context)
            } else {
                telephonyLineNumber(context)
            }
        } catch (_: SecurityException) {
            null
        } catch (_: Throwable) {
            null
        }
    }

    @SuppressLint("MissingPermission", "HardwareIds")
    private fun telephonyLineNumber(context: Context): String? {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        @Suppress("DEPRECATION")
        val number = telephonyManager?.line1Number
        return number?.takeIf { it.isNotBlank() }
    }

    /** Normalize phone number by stripping to last 10 digits. Matches SmsVerificationService. */
    fun normalize(phone: String): String {
        return phone.replace(Regex("[^0-9+]"), "").takeLast(10)
    }
}
