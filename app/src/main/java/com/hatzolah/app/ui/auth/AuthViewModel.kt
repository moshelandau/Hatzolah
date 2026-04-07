package com.hatzolah.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hatzolah.app.service.SmsVerificationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val phoneNumber: String = "",
    val verificationCode: String = "",
    val codeSent: Boolean = false,
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val verificationService: SmsVerificationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onPhoneChanged(phone: String) {
        _uiState.update { it.copy(phoneNumber = phone, error = null) }
    }

    fun onCodeChanged(code: String) {
        _uiState.update { it.copy(verificationCode = code, error = null) }
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

            verificationService.sendVerificationCode(_uiState.value.phoneNumber)
            _uiState.update { it.copy(isLoading = false, codeSent = true) }
        }
    }

    fun verifyCode() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = verificationService.verifyCode(
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
        _uiState.update { AuthUiState() }
    }
}
