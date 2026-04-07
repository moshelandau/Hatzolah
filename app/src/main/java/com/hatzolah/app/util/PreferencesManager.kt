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
    }

    fun getDispatchNumber(): String = prefs.getString(KEY_DISPATCH_NUMBER, "") ?: ""
    fun setDispatchNumber(number: String) = prefs.edit().putString(KEY_DISPATCH_NUMBER, number).apply()

    fun getLoggedInMemberId(): Long = prefs.getLong(KEY_LOGGED_IN_MEMBER_ID, -1)
    fun setLoggedInMemberId(id: Long) = prefs.edit().putLong(KEY_LOGGED_IN_MEMBER_ID, id).apply()

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    fun setLoggedIn(loggedIn: Boolean) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, loggedIn).apply()

    fun getRmaHotline(): String = prefs.getString(KEY_RMA_HOTLINE, "") ?: ""
    fun setRmaHotline(number: String) = prefs.edit().putString(KEY_RMA_HOTLINE, number).apply()

    fun setVerificationCode(code: String) = prefs.edit().putString(KEY_VERIFICATION_CODE, code).apply()
    fun getVerificationCode(): String = prefs.getString(KEY_VERIFICATION_CODE, "") ?: ""

    fun clearSession() {
        prefs.edit()
            .remove(KEY_LOGGED_IN_MEMBER_ID)
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_VERIFICATION_CODE)
            .apply()
    }
}
