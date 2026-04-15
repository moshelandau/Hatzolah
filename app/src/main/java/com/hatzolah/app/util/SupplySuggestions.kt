package com.hatzolah.app.util

/**
 * Maps dispatch call types to suggested supplies from the stock room.
 * Only suggests supplies relevant to the specific call type.
 */
object SupplySuggestions {

    private data class Rule(val keywords: List<String>, val supplies: List<String>)

    private val rules = listOf(
        Rule(
            listOf("cardiac", "chest pain", "arrest", "cpr", "code"),
            listOf("AED", "BP cuff", "Stethoscope", "O2", "BVM", "OPA/NPA", "Pen light")
        ),
        Rule(
            listOf("breathing", "asthma", "respiratory", "sob", "dyspnea"),
            listOf("O2", "Oxygen mask", "Nebulizer mask", "Nasal cannula", "BVM", "Stethoscope", "BP cuff")
        ),
        Rule(
            listOf("diabetic", "sugar", "glucose", "hypoglycemia"),
            listOf("Glucose gel", "BGL monitor", "BP cuff", "Stethoscope")
        ),
        Rule(
            listOf("trauma", "fall", "fracture", "ankle", "leg", "arm", "injury", "broken"),
            listOf("SAM splint", "ACE bandage", "Gauze pads", "Trauma dressing", "Cold pack")
        ),
        Rule(
            listOf("bleed", "laceration", "cut", "hemorrhag"),
            listOf("Gauze pads", "Trauma dressing", "Bandages", "Steri-Strips", "Tape")
        ),
        Rule(
            listOf("burn"),
            listOf("Burn gel", "Gauze pads")
        ),
        Rule(
            listOf("ob ", "labor", "delivery", "pregnant"),
            listOf("OB kit", "Gauze pads", "Trauma dressing", "BVM")
        ),
        Rule(
            listOf("choking"),
            listOf("BVM", "OPA/NPA", "Pen light")
        ),
        Rule(
            listOf("unconscious", "unresponsive", "seizure", "syncope", "faint"),
            listOf("BVM", "OPA/NPA", "Pen light", "BP cuff", "Stethoscope", "Glucose gel", "BGL monitor")
        ),
        Rule(
            listOf("allergic", "anaphylaxis", "anaphyl"),
            listOf("Epi", "Syringes", "BP cuff", "Stethoscope", "Oxygen mask")
        ),
        Rule(
            listOf("mva", "vehicle", "accident", "collision"),
            listOf("Trauma dressing", "SAM splint", "Gauze pads", "ACE bandage")
        ),
        Rule(
            listOf("overdose", "od ", "narcotics", "opioid"),
            listOf("Narcan", "BVM", "O2", "OPA/NPA", "Pen light", "BP cuff")
        ),
        Rule(
            listOf("head injury", "head trauma", "concussion"),
            listOf("Gauze pads", "Trauma dressing", "Pen light", "BP cuff")
        )
    )

    /** Always included — just gloves. */
    private val defaults = listOf("Gloves")

    /**
     * Returns a deduplicated list of suggested supplies for the given call type.
     * Only gloves are always appended.
     */
    fun forCallType(callType: String): List<String> {
        val upper = " ${callType.uppercase()} "
        val matched = linkedSetOf<String>()

        for (rule in rules) {
            if (rule.keywords.any { upper.contains(it.uppercase()) }) {
                matched.addAll(rule.supplies)
            }
        }

        // If nothing matched, add generic assessment supplies
        if (matched.isEmpty()) {
            matched.addAll(listOf("BP cuff", "Stethoscope", "Gauze pads", "Pen light"))
        }

        // Always add defaults at the end
        for (d in defaults) {
            if (d !in matched) matched.add(d)
        }

        return matched.toList()
    }
}
