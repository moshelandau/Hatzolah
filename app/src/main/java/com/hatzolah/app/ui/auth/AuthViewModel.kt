package com.hatzolah.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hatzolah.app.data.database.entity.Member
import com.hatzolah.app.data.repository.MemberRepository
import com.hatzolah.app.service.SmsVerificationService
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
    val verificationCode: String = "",
    val codeSent: Boolean = false,
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val isFirstTimeSetup: Boolean = false,
    val displayedCode: String? = null,
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val verificationService: SmsVerificationService,
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
            val members = memberRepository.getAllMembers().first()
            if (members.isEmpty()) {
                _uiState.update { it.copy(isFirstTimeSetup = true) }
            }
        }
    }

    fun onPhoneChanged(phone: String) {
        _uiState.update { it.copy(phoneNumber = phone, error = null) }
    }

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(name = name, error = null) }
    }

    fun onCodeChanged(code: String) {
        _uiState.update { it.copy(verificationCode = code, error = null) }
    }

    /**
     * First-time setup: creates the admin account and logs in directly.
     * No SMS verification needed since there are no members yet.
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

    fun sendVerificationCode() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val isRegistered = verificationService.checkMemberRegistration(_uiState.value.phoneNumber)
            if (!isRegistered) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "This phone number is not registered. Contact your admin."
                    )
                }
                return@launch
            }

            val code = verificationService.sendVerificationCode(_uiState.value.phoneNumber)
            // Show the code on screen for testing (SMS may not arrive on test devices)
            _uiState.update { it.copy(isLoading = false, codeSent = true, displayedCode = code) }
        }
    }

    fun verifyCode() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (verificationService.verifyCode(
                _uiState.value.phoneNumber,
                _uiState.value.verificationCode
            )) {
                is SmsVerificationService.VerificationResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, isAuthenticated = true) }
                }
                is SmsVerificationService.VerificationResult.InvalidCode -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = "Invalid verification code")
                    }
                }
                is SmsVerificationService.VerificationResult.NotRegistered -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = "Phone number not registered")
                    }
                }
            }
        }
    }

    fun resetToPhoneEntry() {
        _uiState.update { it.copy(codeSent = false, verificationCode = "", displayedCode = null, error = null) }
    }
}
