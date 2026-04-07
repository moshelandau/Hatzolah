package com.hatzolah.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hatzolah.app.data.repository.CallLogRepository
import com.hatzolah.app.util.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class DashboardUiState(
    val recentCallAddress: String = "",
    val totalCalls: Int = 0,
    val totalMiles: Double = 0.0,
    val avgResponseTime: Int = 0,
    val personalCalls: Int = 0,
    val personalMiles: Double = 0.0,
    val personalMinutes: Int = 0
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val callLogRepository: CallLogRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val memberId = preferencesManager.getLoggedInMemberId()

    val uiState: StateFlow<DashboardUiState> = combine(
        callLogRepository.getMostRecentCall(),
        callLogRepository.getTotalCallCount(),
        callLogRepository.getTotalTeamMiles(),
        callLogRepository.getAverageResponseTime(),
        callLogRepository.getPersonalCallCount(memberId),
    ) { recentCall, totalCalls, totalMiles, avgTime, personalCalls ->
        DashboardUiState(
            recentCallAddress = recentCall?.dispatchAddress ?: "",
            totalCalls = totalCalls,
            totalMiles = totalMiles,
            avgResponseTime = avgTime,
            personalCalls = personalCalls
        )
    }.combine(
        combine(
            callLogRepository.getPersonalTotalMiles(memberId),
            callLogRepository.getPersonalTotalMinutes(memberId)
        ) { miles, minutes -> Pair(miles, minutes) }
    ) { state, (miles, minutes) ->
        state.copy(personalMiles = miles, personalMinutes = minutes)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DashboardUiState()
    )
}
