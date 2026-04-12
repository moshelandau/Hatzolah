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
 * Fallback path: CHECKING -> NEED_PERMISSION -> MANUAL_ENTRY -> AUTHENTICATED
 */
enum class AuthStep {
    CHECKING,          // initial DB lookup (first-time setup? auto-login?)
    FIRST_TIME_SETUP,  // no members exist yet — create admin
    NEED_PERMISSION,   // READ_PHONE_NUMBERS not granted — show "grant permission" UI
    DETECTING,         // permission granted, reading the SIM line number
    CONFIRM_IDENTITY,  // we have a number + matched member, user confirms/edits unit
    MANUAL_ENTRY,      // detection failed / unregistered / user picked manual
    AUTHENTICATED
}

data class AuthUiState(
    val step: AuthStep = AuthStep.CHECKING,
    val phoneNumber: String = "",
    val name: String = "",
    val unitNumber: String = "",
    val detectedPhoneNumber: String? = null,
    val matchedMember: Member? = null,
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
            _uiState.update {
                it.copy(
                    step = AuthStep.MANUAL_ENTRY,
                    error = "Permission denied. Please enter your phone number manually."
                )
            }
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
            _uiState.update {
                it.copy(
                    step = AuthStep.MANUAL_ENTRY,
                    isLoading = false,
                    detectedPhoneNumber = null,
                    error = "We couldn't read your phone number from this SIM. Please enter it manually."
                )
            }
            return
        }

        val normalized = DevicePhoneUtil.normalize(detectedPhone)
        val member = memberRepository.getMemberByPhone(normalized)
        if (member == null) {
            _uiState.update {
                it.copy(
                    step = AuthStep.MANUAL_ENTRY,
                    isLoading = false,
                    detectedPhoneNumber = detectedPhone,
                    phoneNumber = normalized,
                    error = "The phone $detectedPhone is not registered. Ask your admin to add you, or enter a different number."
                )
            }
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

    /** "This isn't me" / manual fallback from the confirm screen. */
    fun switchToManualEntry() {
        _uiState.update {
            it.copy(step = AuthStep.MANUAL_ENTRY, matchedMember = null, error = null)
        }
    }

    /**
     * Manual fallback: user types their phone number directly. No SMS — if the
     * number matches a registered member, log them in immediately.
     */
    fun loginWithManualPhone() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val normalized = DevicePhoneUtil.normalize(_uiState.value.phoneNumber)
            if (normalized.length < 10) {
                _uiState.update { it.copy(isLoading = false, error = "Enter a valid phone number") }
                return@launch
            }

            val member = memberRepository.getMemberByPhone(normalized)
            if (member == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "This phone number is not registered. Contact your admin."
                    )
                }
                return@launch
            }

            // For manual entry, show confirmation step too if the member has a
            // unit — consistent UX with the auto-detect path.
            _uiState.update {
                it.copy(
                    step = AuthStep.CONFIRM_IDENTITY,
                    isLoading = false,
                    detectedPhoneNumber = normalized,
                    matchedMember = member,
                    unitNumber = member.unitNumber
                )
            }
        }
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
