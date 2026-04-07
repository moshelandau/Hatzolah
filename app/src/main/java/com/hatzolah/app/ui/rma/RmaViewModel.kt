package com.hatzolah.app.ui.rma

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hatzolah.app.data.repository.CallLogRepository
import com.hatzolah.app.util.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RmaUiState(
    val rmaHotline: String = "",
    val selectedOutcome: String = "",
    val outcomeSaved: Boolean = false,
    val activeCallLogId: Long? = null
)

@HiltViewModel
class RmaViewModel @Inject constructor(
    private val callLogRepository: CallLogRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val memberId = preferencesManager.getLoggedInMemberId()
    private val _uiState = MutableStateFlow(RmaUiState(rmaHotline = preferencesManager.getRmaHotline()))
    val uiState: StateFlow<RmaUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            callLogRepository.getMostRecentCallByMember(memberId).collect { call ->
                _uiState.update { it.copy(activeCallLogId = call?.id) }
            }
        }
    }

    fun logOutcome(outcome: String) {
        _uiState.update { it.copy(selectedOutcome = outcome) }

        viewModelScope.launch {
            val callId = _uiState.value.activeCallLogId ?: return@launch
            callLogRepository.getCallLogById(callId)?.let { call ->
                callLogRepository.updateCallLog(
                    call.copy(rmaApprovalStatus = outcome.lowercase())
                )
                _uiState.update { it.copy(outcomeSaved = true) }
            }
        }
    }
}
