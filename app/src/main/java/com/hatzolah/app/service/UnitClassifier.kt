package com.hatzolah.app.service

/**
 * Classifies dispatch unit codes into categories.
 *
 * Unit codes in Hatzolah dispatches can be members (e.g. "KY85"), ambulances
 * (901–913), the night vehicle (918), or Shabbos drivers (90/91/92).
 */
object UnitClassifier {

    enum class Kind {
        MEMBER,        // KY## — maps to a Member in the DB
        AMBULANCE,     // 901–913
        NIGHT_VEHICLE, // 918
        SHABBOS_DRIVER,// 90, 91, 92
        UNKNOWN
    }

    data class Classification(val code: String, val kind: Kind, val label: String)

    /**
     * Splits a dispatch units string like "KY3, KY4, 903, KY85" into a list of
     * trimmed codes. Accepts comma, slash, or whitespace as separators.
     */
    fun splitUnits(units: String): List<String> {
        if (units.isBlank()) return emptyList()
        return units
            .split(Regex("[,\\s/]+"))
            .map { it.trim().uppercase() }
            .filter { it.isNotBlank() }
    }

    /**
     * Classifies a unit code by format. Any code that starts with a letter is
     * treated as a MEMBER candidate (e.g. "K40", "KY85") — the caller is
     * expected to do an authoritative DB lookup and fall back to this
     * classification if no member is found.
     *
     * Pure numeric codes are classified by their number range (ambulance,
     * night vehicle, Shabbos driver).
     */
    fun classify(code: String): Classification {
        val upper = code.trim().uppercase()
        if (upper.isEmpty()) return Classification(upper, Kind.UNKNOWN, upper)

        // If it starts with a letter, it's a member unit (K##, KY##, KM##, etc.)
        if (upper[0].isLetter()) {
            val role = memberRoleFromPrefix(upper)
            val label = if (role.isNotEmpty()) "$upper ($role)" else upper
            return Classification(upper, Kind.MEMBER, label)
        }

        // Pure numeric codes — classify by range
        val numeric = upper.toIntOrNull()
        if (numeric != null) {
            return when {
                numeric in 901..913 -> Classification(upper, Kind.AMBULANCE, "Ambulance $numeric")
                numeric == 918 -> Classification(upper, Kind.NIGHT_VEHICLE, "Night Vehicle")
                numeric in listOf(90, 91, 92) -> Classification(upper, Kind.SHABBOS_DRIVER, "Shabbos Driver $numeric")
                else -> Classification(upper, Kind.UNKNOWN, upper)
            }
        }
        return Classification(upper, Kind.UNKNOWN, upper)
    }

    /** Returns an optional role label based on the member unit prefix. */
    fun memberRoleFromPrefix(upper: String): String {
        val prefix = upper.takeWhile { it.isLetter() }
        return when (prefix) {
            "KM" -> "Medic"
            "KY" -> "EMT"
            "K" -> "EMT"
            else -> ""
        }
    }
}
