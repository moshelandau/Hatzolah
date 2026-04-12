package com.hatzolah.app.util

import java.time.LocalDate

/**
 * KY BLS CME schedule. Each entry is a Sunday CME topic for the listed year.
 * Shown on the Calendar screen as colored dots and in the day-detail popup.
 */
object CmeSchedule {

    data class Event(val date: LocalDate, val title: String)

    val events: List<Event> = listOf(
        Event(LocalDate.of(2026, 1, 4), "DDC Prep / Operations"),
        Event(LocalDate.of(2026, 2, 1), "Neuro / Abdominal"),
        Event(LocalDate.of(2026, 3, 1), "Pharmacology / Toxicology"),
        Event(LocalDate.of(2026, 4, 12), "OB"),
        Event(LocalDate.of(2026, 5, 3), "CPR / BCLS"),
        Event(LocalDate.of(2026, 6, 7), "Cardiac"),
        Event(LocalDate.of(2026, 7, 5), "Shock & Resuscitation"),
        Event(LocalDate.of(2026, 8, 2), "Elective"),
        Event(LocalDate.of(2026, 8, 30), "Respiratory"),
        Event(LocalDate.of(2026, 10, 11), "Trauma"),
        Event(LocalDate.of(2026, 11, 1), "BLS Skills A"),
        Event(LocalDate.of(2026, 12, 13), "BLS Skills B")
    )

    private val byDate: Map<LocalDate, List<Event>> = events.groupBy { it.date }

    fun eventsOn(date: LocalDate): List<Event> = byDate[date].orEmpty()

    fun hasEventOn(date: LocalDate): Boolean = byDate.containsKey(date)
}
