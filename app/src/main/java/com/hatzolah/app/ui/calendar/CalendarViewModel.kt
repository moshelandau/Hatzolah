package com.hatzolah.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hatzolah.app.data.database.entity.CallLog
import com.hatzolah.app.data.repository.CallLogRepository
import com.hatzolah.app.util.CmeSchedule
import com.hatzolah.app.util.HebrewDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

/**
 * Represents one cell in the calendar grid. Cells for days outside the
 * currently displayed month are marked with [inCurrentMonth] = false and
 * rendered in a muted style.
 */
data class CalendarDayCell(
    val date: LocalDate,
    val inCurrentMonth: Boolean,
    val callCount: Int,
    val hasCme: Boolean,
    val hebrewDay: String
)

data class CalendarUiState(
    val yearMonth: YearMonth = YearMonth.now(),
    val cells: List<CalendarDayCell> = emptyList(),
    val gregorianTitle: String = "",
    val hebrewTitle: String = "",
    val callsByDate: Map<LocalDate, List<CallLog>> = emptyMap()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val callLogRepository: CallLogRepository
) : ViewModel() {

    private val zone = ZoneId.systemDefault()
    private val _month = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<CalendarUiState> = _month
        .flatMapLatest { ym ->
            val (startMs, endMs) = monthRangeMs(ym)
            callLogRepository.getCallLogsByDateRange(startMs, endMs).map { calls ->
                val byDate: Map<LocalDate, List<CallLog>> = calls.groupBy { call ->
                    Instant.ofEpochMilli(call.date).atZone(zone).toLocalDate()
                }
                CalendarUiState(
                    yearMonth = ym,
                    cells = buildCells(ym, byDate),
                    gregorianTitle = "${ym.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${ym.year}",
                    hebrewTitle = hebrewMonthTitle(ym),
                    callsByDate = byDate
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CalendarUiState())

    fun previousMonth() {
        _month.value = _month.value.minusMonths(1)
    }

    fun nextMonth() {
        _month.value = _month.value.plusMonths(1)
    }

    fun jumpToToday() {
        _month.value = YearMonth.now()
    }

    private fun monthRangeMs(ym: YearMonth): Pair<Long, Long> {
        val start = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = ym.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to end
    }

    /**
     * Builds a 6x7 grid of cells for [ym], padded with leading days from the
     * previous month and trailing days from the next month so the grid is
     * always rectangular.
     */
    private fun buildCells(ym: YearMonth, callsByDate: Map<LocalDate, List<CallLog>>): List<CalendarDayCell> {
        val firstOfMonth = ym.atDay(1)
        // java DayOfWeek: Monday=1..Sunday=7. We want Sunday-first, so offset by (dayOfWeek%7).
        val leadingBlanks = firstOfMonth.dayOfWeek.value % 7
        val gridStart = firstOfMonth.minusDays(leadingBlanks.toLong())
        return (0 until 42).map { offset ->
            val date = gridStart.plusDays(offset.toLong())
            val inMonth = YearMonth.from(date) == ym
            val calls = callsByDate[date].orEmpty()
            CalendarDayCell(
                date = date,
                inCurrentMonth = inMonth,
                callCount = calls.size,
                hasCme = CmeSchedule.hasEventOn(date),
                hebrewDay = HebrewDate.gematria(HebrewDate.fromGregorian(date).day)
            )
        }
    }

    /**
     * Returns a Hebrew title like "ניסן–אייר תשפ״ו" covering whichever Hebrew
     * months the displayed Gregorian month spans.
     */
    private fun hebrewMonthTitle(ym: YearMonth): String {
        val first = HebrewDate.fromGregorian(ym.atDay(1))
        val last = HebrewDate.fromGregorian(ym.atEndOfMonth())
        return if (first.month == last.month && first.year == last.year) {
            "${first.monthName} ${first.yearLetters}"
        } else {
            "${first.monthName} – ${last.monthName} ${last.yearLetters}"
        }
    }
}
