package com.hatzolah.app.service

import android.telephony.SmsManager
import com.hatzolah.app.data.repository.MemberRepository
import com.hatzolah.app.util.PreferencesManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * SMS-based phone verification system.
 * Sends a one-time code via SMS and validates against the member database.
 */
@Singleton
class SmsVerificationService @Inject constructor(
    private val memberRepository: MemberRepository,
    private val preferencesManager: PreferencesManager
) {

    /**
     * Checks if the phone number belongs to a registered member.
     * Returns null if not found (unregistered numbers are denied access).
     */
    suspend fun checkMemberRegistration(phoneNumber: String): Boolean {
        val normalized = normalizePhone(phoneNumber)
        return memberRepository.getMemberByPhone(normalized) != null
    }

    /**
     * Generates and sends a 6-digit verification code via SMS.
     */
    fun sendVerificationCode(phoneNumber: String): String {
        val code = generateCode()
        preferencesManager.setVerificationCode(code)

        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(
                phoneNumber, null,
                "Your Hatzolah verification code is: $code",
                null, null
            )
        } catch (_: Exception) {
            // SMS sending may fail in emulator; code is stored for verification
        }

        return code
    }

    /**
     * Validates the entered code against the stored verification code.
     */
    suspend fun verifyCode(phoneNumber: String, enteredCode: String): VerificationResult {
        val storedCode = preferencesManager.getVerificationCode()
        if (storedCode.isBlank() || enteredCode != storedCode) {
            return VerificationResult.InvalidCode
        }

        val normalized = normalizePhone(phoneNumber)
        val member = memberRepository.getMemberByPhone(normalized)
            ?: return VerificationResult.NotRegistered

        // Mark member as verified and log them in
        memberRepository.verifyMember(member.id)
        preferencesManager.setLoggedInMemberId(member.id)
        preferencesManager.setLoggedIn(true)
        preferencesManager.setVerificationCode("")

        return VerificationResult.Success(member.id)
    }

    private fun generateCode(): String {
        return Random.nextInt(100000, 999999).toString()
    }

    private fun normalizePhone(phone: String): String {
        return phone.replace(Regex("[^0-9+]"), "").takeLast(10)
    }

    sealed class VerificationResult {
        data class Success(val memberId: Long) : VerificationResult()
        data object InvalidCode : VerificationResult()
        data object NotRegistered : VerificationResult()
    }
}
