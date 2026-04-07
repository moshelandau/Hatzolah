package com.hatzolah.app.service

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsParser @Inject constructor() {

    data class ParsedDispatch(
        val address: String,
        val rawMessage: String,
        val callType: String = "",
        val units: String = "",
        val caller: String = "",
        val age: String = "",
        val cad: String = ""
    )

    /**
     * Parses Hatzolah dispatch SMS messages.
     *
     * Expected format:
     * KJ EMS D- KY82
     * 15 Dinev Road #303, Monroe 10950
     * CALL TYPE: Dislocation
     * CALLER: 9292830422
     * CAD: 0405202648
     * UNITS: KY85
     * AGE: 2 F
     * http://maps.google.com/maps?daddr=...
     */
    fun parseDispatchMessage(message: String): ParsedDispatch? {
        val trimmed = message.trim()
        if (trimmed.isBlank()) return null

        val lines = trimmed.lines().map { it.trim() }.filter { it.isNotBlank() }

        var address = ""
        var callType = ""
        var units = ""
        var caller = ""
        var age = ""
        var cad = ""

        for (line in lines) {
            val upper = line.uppercase()
            when {
                upper.startsWith("CALL TYPE:") || upper.startsWith("CALLTYPE:") ->
                    callType = line.substringAfter(":").trim()
                upper.startsWith("UNITS:") || upper.startsWith("UNIT:") ->
                    units = line.substringAfter(":").trim()
                upper.startsWith("CALLER:") ->
                    caller = line.substringAfter(":").trim()
                upper.startsWith("CAD:") ->
                    cad = line.substringAfter(":").trim()
                upper.startsWith("AGE:") ->
                    age = line.substringAfter(":").trim()
                upper.startsWith("HTTP") || upper.startsWith("WWW") ->
                    { /* skip URL lines */ }
                // Address line: contains a number followed by street name
                address.isBlank() && line.matches(Regex("^\\d+\\s+[A-Za-z].*")) ->
                    address = line
            }
        }

        // Fallback if no structured address found
        if (address.isBlank()) {
            address = cleanAddress(trimmed)
        }

        return ParsedDispatch(
            address = cleanAddress(address),
            rawMessage = trimmed,
            callType = callType,
            units = units,
            caller = caller,
            age = age,
            cad = cad
        )
    }

    fun formatForNavigation(address: String): String {
        return address.trim().replace("\\s+".toRegex(), "+")
    }

    private fun cleanAddress(raw: String): String {
        return raw.trim()
            .replace("\n", " ")
            .replace("\r", "")
            .replace("\\s+".toRegex(), " ")
    }
}
