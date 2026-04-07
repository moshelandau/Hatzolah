package com.hatzolah.app.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hatzolah.app.data.repository.CallLogRepository
import com.hatzolah.app.util.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.util.Calendar
import javax.inject.Inject

data class AnalyticsUiState(
    val teamTotalCalls: Int = 0,
    val teamTotalMiles: Double = 0.0,
    val teamAvgResponse: Int = 0,
    val personalTotalCalls: Int = 0,
    val personalCallsThisMonth: Int = 0,
    val personalTotalMiles: Double = 0.0,
    val personalTotalMinutes: Int = 0
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val callLogRepository: CallLogRepository,
    preferencesManager: PreferencesManager
) : ViewModel() {

    private val memberId = preferencesManager.getLoggedInMemberId()

    private val calendar = Calendar.getInstance()
    private val startOfMonth: Long
    private val endOfMonth: Long

    init {
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        startOfMonth = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        endOfMonth = calendar.timeInMillis
    }

    val uiState: StateFlow<AnalyticsUiState> = combine(
        callLogRepository.getTotalCallCount(),
        callLogRepository.getTotalTeamMiles(),
        callLogRepository.getAverageResponseTime(),
        callLogRepository.getPersonalCallCount(memberId),
        callLogRepository.getPersonalCallsThisMonth(memberId, startOfMonth, endOfMonth)
    ) { totalCalls, totalMiles, avgResponse, personalCalls, monthCalls ->
        AnalyticsUiState(
            teamTotalCalls = totalCalls,
            teamTotalMiles = totalMiles,
            teamAvgResponse = avgResponse,
            personalTotalCalls = personalCalls,
            personalCallsThisMonth = monthCalls
        )
    }.combine(
        combine(
            callLogRepository.getPersonalTotalMiles(memberId),
            callLogRepository.getPersonalTotalMinutes(memberId)
        ) { miles, minutes -> Pair(miles, minutes) }
    ) { state, (miles, minutes) ->
        state.copy(personalTotalMiles = miles, personalTotalMinutes = minutes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsUiState())
}
