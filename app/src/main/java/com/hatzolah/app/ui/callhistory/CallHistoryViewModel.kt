package com.hatzolah.app.ui.callhistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hatzolah.app.data.database.entity.CallLog
import com.hatzolah.app.data.repository.CallLogRepository
import com.hatzolah.app.util.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class CallHistoryUiState(
    val callLogs: List<CallLog> = emptyList()
)

@HiltViewModel
class CallHistoryViewModel @Inject constructor(
    callLogRepository: CallLogRepository,
    preferencesManager: PreferencesManager
) : ViewModel() {

    private val memberId = preferencesManager.getLoggedInMemberId()

    val uiState: StateFlow<CallHistoryUiState> = callLogRepository
        .getCallLogsByMember(memberId)
        .map { CallHistoryUiState(callLogs = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CallHistoryUiState())
}
