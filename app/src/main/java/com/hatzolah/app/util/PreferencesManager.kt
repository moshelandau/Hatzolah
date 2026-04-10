package com.hatzolah.app.util

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("hatzolah_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DISPATCH_NUMBER = "dispatch_number"
        private const val KEY_LOGGED_IN_MEMBER_ID = "logged_in_member_id"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_RMA_HOTLINE = "rma_hotline"
        private const val KEY_VERIFICATION_CODE = "verification_code"

        // Active dispatch state (persists so the alert can reappear on unfold/wake)
        private const val KEY_ACTIVE_DISPATCH = "active_dispatch"
        private const val KEY_DISPATCH_ADDRESS = "active_dispatch_address"
        private const val KEY_DISPATCH_CALL_TYPE = "active_dispatch_call_type"
        private const val KEY_DISPATCH_RAW = "active_dispatch_raw"
        private const val KEY_DISPATCH_UNITS = "active_dispatch_units"
        private const val KEY_DISPATCH_AGE = "active_dispatch_age"
        private const val KEY_DISPATCH_TIMESTAMP = "active_dispatch_timestamp"
    }

    fun getDispatchNumber(): String = prefs.getString(KEY_DISPATCH_NUMBER, "") ?: ""
    fun setDispatchNumber(number: String) {
        val sanitized = number.replace(Regex("[^+\\d]"), "")
        prefs.edit().putString(KEY_DISPATCH_NUMBER, sanitized).apply()
    }

    fun getLoggedInMemberId(): Long = prefs.getLong(KEY_LOGGED_IN_MEMBER_ID, -1)
    fun setLoggedInMemberId(id: Long) = prefs.edit().putLong(KEY_LOGGED_IN_MEMBER_ID, id).apply()

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    fun setLoggedIn(loggedIn: Boolean) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, loggedIn).apply()

    fun getRmaHotline(): String = prefs.getString(KEY_RMA_HOTLINE, "") ?: ""
    fun setRmaHotline(number: String) = prefs.edit().putString(KEY_RMA_HOTLINE, number).apply()

    fun setVerificationCode(code: String) = prefs.edit().putString(KEY_VERIFICATION_CODE, code).apply()
    fun getVerificationCode(): String = prefs.getString(KEY_VERIFICATION_CODE, "") ?: ""

    // Active dispatch state - auto-expires after 30 minutes
    fun setActiveDispatch(address: String, callType: String, rawMessage: String, units: String, age: String) {
        synchronized(prefs) {
            prefs.edit()
                .putBoolean(KEY_ACTIVE_DISPATCH, true)
                .putString(KEY_DISPATCH_ADDRESS, address)
                .putString(KEY_DISPATCH_CALL_TYPE, callType)
                .putString(KEY_DISPATCH_RAW, rawMessage)
                .putString(KEY_DISPATCH_UNITS, units)
                .putString(KEY_DISPATCH_AGE, age)
                .putLong(KEY_DISPATCH_TIMESTAMP, System.currentTimeMillis())
                .apply()
        }
    }

    fun hasActiveDispatch(): Boolean {
        if (!prefs.getBoolean(KEY_ACTIVE_DISPATCH, false)) return false
        val ts = prefs.getLong(KEY_DISPATCH_TIMESTAMP, 0)
        val ageMs = System.currentTimeMillis() - ts
        // Auto-expire after 10 minutes to prevent stale alerts
        if (ageMs > 10 * 60 * 1000) {
            clearActiveDispatch()
            return false
        }
        return true
    }

    data class ActiveDispatch(
        val address: String,
        val callType: String,
        val rawMessage: String,
        val units: String,
        val age: String
    )

    fun getActiveDispatch(): ActiveDispatch? {
        if (!hasActiveDispatch()) return null
        return ActiveDispatch(
            address = prefs.getString(KEY_DISPATCH_ADDRESS, "") ?: "",
            callType = prefs.getString(KEY_DISPATCH_CALL_TYPE, "") ?: "",
            rawMessage = prefs.getString(KEY_DISPATCH_RAW, "") ?: "",
            units = prefs.getString(KEY_DISPATCH_UNITS, "") ?: "",
            age = prefs.getString(KEY_DISPATCH_AGE, "") ?: ""
        )
    }

    fun clearActiveDispatch() {
        prefs.edit()
            .remove(KEY_ACTIVE_DISPATCH)
            .remove(KEY_DISPATCH_ADDRESS)
            .remove(KEY_DISPATCH_CALL_TYPE)
            .remove(KEY_DISPATCH_RAW)
            .remove(KEY_DISPATCH_UNITS)
            .remove(KEY_DISPATCH_AGE)
            .remove(KEY_DISPATCH_TIMESTAMP)
            .apply()
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_LOGGED_IN_MEMBER_ID)
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_VERIFICATION_CODE)
            .apply()
    }
}
