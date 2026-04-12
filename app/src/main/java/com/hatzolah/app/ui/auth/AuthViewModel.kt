package com.hatzolah.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hatzolah.app.data.database.entity.Member
import com.hatzolah.app.data.repository.MemberRepository
import com.hatzolah.app.util.DevicePhoneUtil
import com.hatzolah.app.util.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val phoneNumber: String = "",
    val name: String = "",
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val isFirstTimeSetup: Boolean = false,
    val detectedPhoneNumber: String? = null,
    val manualEntryMode: Boolean = false,
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
                    _uiState.update { it.copy(isFirstTimeSetup = true) }
                } else if (!preferencesManager.isLoggedIn()) {
                    // Auto-login the first admin member if no one is logged in yet
                    val admin = members.firstOrNull { it.isAdmin }
                    if (admin != null) {
                        preferencesManager.setLoggedInMemberId(admin.id)
                        preferencesManager.setLoggedIn(true)
                        _uiState.update { it.copy(isAuthenticated = true) }
                    }
                }
            } catch (e: Throwable) {
                // Don't flip to first-time setup on a transient DB error - show error instead
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
                    isVerified = true,
                    isAdmin = true
                )
            )

            preferencesManager.setLoggedInMemberId(memberId)
            preferencesManager.setLoggedIn(true)

            _uiState.update { it.copy(isLoading = false, isAuthenticated = true) }
        }
    }

    /**
     * Called by AuthScreen after it has attempted to read the device's phone number.
     * If a number was detected, try to match it to a registered member and log them in.
     * If no number was detected or no match was found, fall back to manual entry.
     */
    fun onDevicePhoneDetected(detectedPhone: String?) {
        viewModelScope.launch {
            if (detectedPhone.isNullOrBlank()) {
                // Carrier didn't expose the number - fall back to manual entry
                _uiState.update {
                    it.copy(
                        manualEntryMode = true,
                        detectedPhoneNumber = null,
                        error = null
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, detectedPhoneNumber = detectedPhone, error = null) }

            val normalized = DevicePhoneUtil.normalize(detectedPhone)
            val member = memberRepository.getMemberByPhone(normalized)
            if (member == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        manualEntryMode = true,
                        error = "This phone ($detectedPhone) is not registered. Contact your admin or enter a different number."
                    )
                }
                return@launch
            }

            memberRepository.verifyMember(member.id)
            preferencesManager.setLoggedInMemberId(member.id)
            preferencesManager.setLoggedIn(true)
            _uiState.update { it.copy(isLoading = false, isAuthenticated = true) }
        }
    }

    /**
     * Manual fallback: user types their phone number directly. No SMS — if the number
     * matches a registered member, log them in immediately.
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

            memberRepository.verifyMember(member.id)
            preferencesManager.setLoggedInMemberId(member.id)
            preferencesManager.setLoggedIn(true)
            _uiState.update { it.copy(isLoading = false, isAuthenticated = true) }
        }
    }

    fun switchToManualEntry() {
        _uiState.update { it.copy(manualEntryMode = true, error = null) }
    }
}
