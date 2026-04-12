package com.hatzolah.app.util

import android.icu.util.HebrewCalendar
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

/**
 * Converts Gregorian dates to Hebrew (Yiddish-style) display strings using
 * Android's built-in ICU HebrewCalendar (API 24+; app min SDK is 26).
 *
 * This works offline and doesn't require the Hebcal API. Day numbers are
 * rendered as Hebrew gematria letters (א, ב, יד, טו, …), with the traditional
 * avoidance of יה (15) and יו (16) that contain parts of the Divine name.
 */
object HebrewDate {

    /** Weekday column headers (Sun–Sat) shown as Hebrew א, ב, ג, ד, ה, ו, ש. */
    val weekdayLetters: List<String> = listOf("א", "ב", "ג", "ד", "ה", "ו", "ש")

    /** Full Yiddish day-of-week names, indexed by java Calendar DAY_OF_WEEK - 1 (Sun=0). */
    val yiddishDayNames: List<String> = listOf(
        "זונטיק",    // Sunday
        "מאָנטיק",   // Monday
        "דינסטיק",   // Tuesday
        "מיטוואָך",  // Wednesday
        "דאָנערשטיק",// Thursday
        "פֿרײַטיק",   // Friday
        "שבת"        // Saturday
    )

    data class HebrewYmd(
        val year: Int,
        val month: Int, // 0..12 (ICU HebrewCalendar)
        val day: Int
    ) {
        val monthName: String get() = hebrewMonthName(year, month)
        val dayLetters: String get() = gematria(day)
        val yearLetters: String get() = hebrewYearShort(year)

        /** Display like "י״ג ניסן תשפ״ו". */
        fun formatted(): String = "$dayLetters $monthName $yearLetters"
    }

    fun fromGregorian(date: LocalDate): HebrewYmd {
        val cal = HebrewCalendar()
        val utc = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant())
        cal.time = utc
        return HebrewYmd(
            year = cal.get(HebrewCalendar.YEAR),
            month = cal.get(HebrewCalendar.MONTH),
            day = cal.get(HebrewCalendar.DAY_OF_MONTH)
        )
    }

    /** Returns the Hebrew month name, taking leap year Adar splits into account. */
    private fun hebrewMonthName(year: Int, month: Int): String {
        // ICU months: 0=TISHRI, 1=HESHVAN, 2=KISLEV, 3=TEVET, 4=SHEVAT,
        //             5=ADAR_1 (leap only), 6=ADAR, 7=NISAN, 8=IYAR, 9=SIVAN,
        //             10=TAMUZ, 11=AV, 12=ELUL
        val isLeap = isHebrewLeapYear(year)
        return when (month) {
            0 -> "תשרי"
            1 -> "חשון"
            2 -> "כסלו"
            3 -> "טבת"
            4 -> "שבט"
            5 -> if (isLeap) "אדר א" else "אדר"
            6 -> if (isLeap) "אדר ב" else "אדר"
            7 -> "ניסן"
            8 -> "אייר"
            9 -> "סיון"
            10 -> "תמוז"
            11 -> "אב"
            12 -> "אלול"
            else -> ""
        }
    }

    private fun isHebrewLeapYear(year: Int): Boolean {
        // Hebrew 19-year metonic cycle: leap years are 3, 6, 8, 11, 14, 17, 19.
        val cyclePos = year % 19
        return cyclePos in setOf(0, 3, 6, 8, 11, 14, 17)
    }

    /** Converts a day number 1..30 to Hebrew gematria letters. */
    fun gematria(n: Int): String {
        if (n <= 0) return ""
        // Traditional substitutions to avoid writing parts of the Divine name.
        if (n == 15) return "טו"
        if (n == 16) return "טז"
        val tens = n / 10
        val ones = n % 10
        val sb = StringBuilder()
        when (tens) {
            1 -> sb.append('י')
            2 -> sb.append('כ')
            3 -> sb.append('ל')
        }
        when (ones) {
            1 -> sb.append('א')
            2 -> sb.append('ב')
            3 -> sb.append('ג')
            4 -> sb.append('ד')
            5 -> sb.append('ה')
            6 -> sb.append('ו')
            7 -> sb.append('ז')
            8 -> sb.append('ח')
            9 -> sb.append('ט')
        }
        return sb.toString()
    }

    /** Short Hebrew year like "תשפ״ו" from 5786. Strips the thousands digit. */
    fun hebrewYearShort(year: Int): String {
        // Drop the leading 5 (5000s) — traditional Hebrew abbreviation.
        val yearInCentury = year % 1000
        // Convert 786 -> תשפ״ו (ת=400, ש=300, פ=80, ו=6)
        val letters = buildString {
            var remaining = yearInCentury
            val pairs = listOf(
                400 to 'ת', 300 to 'ש', 200 to 'ר', 100 to 'ק',
                90 to 'צ', 80 to 'פ', 70 to 'ע', 60 to 'ס', 50 to 'נ',
                40 to 'מ', 30 to 'ל', 20 to 'כ', 10 to 'י',
                9 to 'ט', 8 to 'ח', 7 to 'ז', 6 to 'ו', 5 to 'ה',
                4 to 'ד', 3 to 'ג', 2 to 'ב', 1 to 'א'
            )
            // Handle 15 and 16 specially inside the year too.
            for ((value, letter) in pairs) {
                if (value == 10 && remaining in listOf(15, 16)) {
                    if (remaining == 15) { append("טו"); remaining = 0 }
                    else { append("טז"); remaining = 0 }
                    break
                }
                while (remaining >= value) {
                    append(letter)
                    remaining -= value
                }
            }
        }
        // Insert gershayim (״) before the last letter, per tradition.
        return if (letters.length >= 2) {
            letters.dropLast(1) + "״" + letters.last()
        } else letters
    }
}
