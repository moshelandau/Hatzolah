package com.hatzolah.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hatzolah.app.data.database.entity.Member
import com.hatzolah.app.data.repository.MemberRepository
import com.hatzolah.app.util.DevicePhoneUtil
import com.hatzolah.app.util.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Explicit steps of the auth flow. AuthScreen renders different UI for each.
 *
 * Typical path: CHECKING -> NEED_PERMISSION -> DETECTING -> CONFIRM_IDENTITY
 *              -> AUTHENTICATED
 *
 * Failure path: any detection failure or unregistered number routes to
 *               SIGN_IN_FAILED, which shows a "Contact KY-85 for help"
 *               screen. There is no manual phone-entry fallback — the
 *               admin has to add the member and the user tries again.
 */
enum class AuthStep {
    CHECKING,          // initial DB lookup (first-time setup? auto-login?)
    FIRST_TIME_SETUP,  // no members exist yet — create admin
    NEED_PERMISSION,   // READ_PHONE_NUMBERS not granted — show "grant permission" UI
    DETECTING,         // permission granted, reading the SIM line number
    CONFIRM_IDENTITY,  // we have a number + matched member, user confirms/edits unit
    SIGN_IN_FAILED,    // detection failed / unregistered — show help contact
    AUTHENTICATED
}

data class AuthUiState(
    val step: AuthStep = AuthStep.CHECKING,
    val phoneNumber: String = "",
    val name: String = "",
    val unitNumber: String = "",
    val detectedPhoneNumber: String? = null,
    val matchedMember: Member? = null,
    val helpContactName: String = "",
    val helpContactPhone: String = "",
    val helpContactUnit: String = "KY-85",
    val failureReason: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val memberRepository: MemberRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkFirstTimeSetup()
    }

    private fun checkFirstTimeSetup() {
        viewModelScope.launch {
            try {
                val members = memberRepository.getAllMembers().first()
                if (members.isEmpty()) {
                    _uiState.update { it.copy(step = AuthStep.FIRST_TIME_SETUP) }
                } else if (preferencesManager.isLoggedIn()) {
                    _uiState.update { it.copy(step = AuthStep.AUTHENTICATED) }
                } else {
                    // Members exist but nobody is logged in — start the auto-detect flow.
                    // AuthScreen will move us to DETECTING once it checks permission.
                    _uiState.update { it.copy(step = AuthStep.NEED_PERMISSION) }
                }
            } catch (e: Throwable) {
                _uiState.update {
                    it.copy(error = "Unable to load members. Please restart the app.")
                }
            }
        }
    }

    fun onPhoneChanged(phone: String) {
        _uiState.update { it.copy(phoneNumber = phone, error = null) }
    }

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(name = name, error = null) }
    }

    fun onUnitChanged(unit: String) {
        _uiState.update { it.copy(unitNumber = unit.uppercase(), error = null) }
    }

    /** User granted/denied permission in the system dialog. */
    fun onPermissionResult(granted: Boolean, detectedPhone: String?) {
        if (!granted) {
            failToSignIn("Permission was denied. We need to read your SIM phone number to sign you in automatically.")
            return
        }
        // Show the "retrieving" state briefly, then try to match.
        _uiState.update { it.copy(step = AuthStep.DETECTING, isLoading = true, error = null) }
        viewModelScope.launch {
            // Tiny artificial delay so users actually see the "Retrieving your number..."
            // screen instead of it flashing past.
            delay(600)
            resolveDetectedPhone(detectedPhone)
        }
    }

    /** Called from AuthScreen when permission was already granted on launch. */
    fun onDevicePhoneAlreadyAvailable(detectedPhone: String?) {
        _uiState.update { it.copy(step = AuthStep.DETECTING, isLoading = true, error = null) }
        viewModelScope.launch {
            delay(600)
            resolveDetectedPhone(detectedPhone)
        }
    }

    private suspend fun resolveDetectedPhone(detectedPhone: String?) {
        if (detectedPhone.isNullOrBlank()) {
            failToSignIn("We couldn't read your phone number from this SIM. Some carriers don't expose the line number.")
            return
        }

        val normalized = DevicePhoneUtil.normalize(detectedPhone)
        val member = memberRepository.getMemberByPhone(normalized)
        if (member == null) {
            failToSignIn("The number $detectedPhone is not on the Hatzolah member list yet.")
            return
        }

        _uiState.update {
            it.copy(
                step = AuthStep.CONFIRM_IDENTITY,
                isLoading = false,
                detectedPhoneNumber = detectedPhone,
                matchedMember = member,
                unitNumber = member.unitNumber
            )
        }
    }

    /**
     * Central failure path: stores the help contact (KY-85, from the pre-
     * populated roster) and flips the step to SIGN_IN_FAILED so the screen
     * can offer a direct call button.
     */
    private fun failToSignIn(reason: String) {
        viewModelScope.launch {
            val helper = try {
                memberRepository.getMemberByUnit("KY85")
            } catch (_: Throwable) { null }
            _uiState.update {
                it.copy(
                    step = AuthStep.SIGN_IN_FAILED,
                    isLoading = false,
                    failureReason = reason,
                    helpContactName = helper?.name.orEmpty(),
                    helpContactPhone = helper?.phoneNumber.orEmpty(),
                    helpContactUnit = "KY-85"
                )
            }
        }
    }

    /** Restart the flow after a failure — user wants to try auto-detect again. */
    fun retryAutoDetect() {
        _uiState.update { it.copy(step = AuthStep.NEED_PERMISSION, failureReason = "") }
    }

    /**
     * User confirmed their identity on the CONFIRM_IDENTITY screen. If they
     * edited the unit number, update it on the member row before signing in.
     */
    fun confirmAndSignIn() {
        val member = _uiState.value.matchedMember ?: return
        val editedUnit = _uiState.value.unitNumber.trim().uppercase()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                if (editedUnit != member.unitNumber) {
                    memberRepository.updateMember(member.copy(unitNumber = editedUnit))
                }
                memberRepository.verifyMember(member.id)
                preferencesManager.setLoggedInMemberId(member.id)
                preferencesManager.setLoggedIn(true)
                _uiState.update { it.copy(step = AuthStep.AUTHENTICATED, isLoading = false) }
            } catch (e: Throwable) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Could not sign in. Please try again.")
                }
            }
        }
    }

    /** "This isn't me" on the confirm screen → route to the failed screen. */
    fun rejectIdentity() {
        failToSignIn("Please contact KY-85 so the right phone number is on file for you.")
    }

    /**
     * First-time setup: creates the admin account and logs in directly.
     */
    fun setupAdmin() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val name = _uiState.value.name.trim()
            val phone = _uiState.value.phoneNumber.trim()

            if (name.isBlank() || phone.isBlank()) {
                _uiState.update { it.copy(isLoading = false, error = "Name and phone are required") }
                return@launch
            }

            val memberId = memberRepository.addMember(
                Member(
                    name = name,
                    phoneNumber = phone,
                    unitNumber = _uiState.value.unitNumber.trim().uppercase(),
                    isVerified = true,
                    isAdmin = true
                )
            )

            preferencesManager.setLoggedInMemberId(memberId)
            preferencesManager.setLoggedIn(true)

            _uiState.update { it.copy(isLoading = false, step = AuthStep.AUTHENTICATED) }
        }
    }
}
