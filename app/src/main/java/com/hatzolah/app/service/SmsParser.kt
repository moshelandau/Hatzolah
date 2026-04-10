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
        val cad: String = "",
        val room: String = ""
    )

    /**
     * Parses Hatzolah dispatch SMS messages.
     * Handles both multi-line and single-line (Samsung notification) formats.
     *
     * Format:
     * KJ EMS D- KY14
     * Icon Mall, 97 Acres Rd #, Monroe 10950
     * CALL TYPE: possible ankle
     * CALLER: 3476095253
     * CAD: 0405202635
     * UNITS: KY85
     * http://maps.google.com/maps?daddr=...
     */
    fun parseDispatchMessage(message: String): ParsedDispatch? {
        val trimmed = message.trim()
        if (trimmed.isBlank()) return null

        // Extract fields using regex - works whether newlines exist or not
        val callType = Regex("CALL\\s*TYPE:\\s*([^\\n]+?)(?=\\s*(?:CALLER|CAD|UNITS|AGE|http|$))", RegexOption.IGNORE_CASE)
            .find(trimmed)?.groupValues?.get(1)?.trim() ?: ""

        val units = Regex("UNITS?:\\s*([^\\n]+?)(?=\\s*(?:CALLER|CAD|CALL|AGE|http|$))", RegexOption.IGNORE_CASE)
            .find(trimmed)?.groupValues?.get(1)?.trim() ?: ""

        val caller = Regex("CALLER:\\s*(\\d+)", RegexOption.IGNORE_CASE)
            .find(trimmed)?.groupValues?.get(1)?.trim() ?: ""

        val cad = Regex("CAD:\\s*(\\d+)", RegexOption.IGNORE_CASE)
            .find(trimmed)?.groupValues?.get(1)?.trim() ?: ""

        val age = Regex("AGE:\\s*([^\\n]+?)(?=\\s*(?:CALLER|CAD|CALL|UNITS|ROOM|http|$))", RegexOption.IGNORE_CASE)
            .find(trimmed)?.groupValues?.get(1)?.trim() ?: ""

        val room = Regex("ROOM:\\s*([^\\n]+?)(?=\\s*(?:CALLER|CAD|CALL|UNITS|AGE|http|$))", RegexOption.IGNORE_CASE)
            .find(trimmed)?.groupValues?.get(1)?.trim() ?: ""

        // Extract address: it's after the header line (KJ EMS...) and before CALL TYPE
        var address = ""

        // Remove the URL from the message for cleaner parsing
        val withoutUrl = trimmed.replace(Regex("https?://\\S+"), "").trim()

        // Try to find address between header and CALL TYPE
        val callTypePos = withoutUrl.uppercase().indexOf("CALL TYPE")
        if (callTypePos > 0) {
            val beforeCallType = withoutUrl.substring(0, callTypePos).trim()
            // Address is the part that contains a street number
            // Header is like "KJ EMS D- KY14" (no street number)
            val lines = beforeCallType.split("\n").map { it.trim() }.filter { it.isNotBlank() }
            if (lines.size >= 2) {
                // Multi-line: skip header, take the rest as address
                address = lines.drop(1).joinToString(", ")
            } else if (lines.size == 1) {
                // Single line: try to find where address starts (after header pattern)
                val headerMatch = Regex("^[A-Z0-9]{2,}\\s+EMS\\s+\\S+\\s+\\S+\\s+").find(lines[0])
                if (headerMatch != null) {
                    address = lines[0].substring(headerMatch.range.last + 1).trim()
                } else {
                    // Try: everything after first comma-separated part that looks like a header
                    val addrMatch = Regex("\\d+\\s+[A-Za-z]").find(lines[0])
                    if (addrMatch != null) {
                        address = lines[0].substring(addrMatch.range.first).trim()
                    }
                }
            }
        }

        // Fallback: find any street address pattern in the message
        if (address.isBlank()) {
            Regex("(\\d+\\s+[A-Za-z][A-Za-z .']+(?:,\\s*[A-Za-z ]+)?\\s*\\d{5})", RegexOption.IGNORE_CASE)
                .find(withoutUrl)?.let { address = it.groupValues[1] }
        }
        if (address.isBlank()) {
            Regex("(\\d+\\s+[A-Za-z][A-Za-z .,'#]{1,60}(?:Rd|Road|St|Street|Ave|Avenue|Dr|Drive|Blvd|Ln|Lane|Mall|Way|Ct|Pl)[A-Za-z0-9,. #]{0,30})", RegexOption.IGNORE_CASE)
                .find(withoutUrl)?.let { address = it.groupValues[1] }
        }

        // Clean up address - remove any trailing field labels that got caught
        address = address.replace(Regex("\\s*(CALL TYPE|CALLER|CAD|UNITS|AGE|ROOM).*", RegexOption.IGNORE_CASE), "").trim()
        address = address.trimEnd(',', ' ')

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
            cad = cad,
            room = room
        )
    }

    /**
     * Formats an address for use in Google Maps navigation URLs.
     * Strips unit/apt/room numbers (which confuse Google Maps routing) and URI-encodes
     * the result. E.g. "3 Hamaspik Way #011, Monroe 10950" -> "3%20Hamaspik%20Way%2C%20Monroe%2010950".
     *
     * Compare with [cleanAddress], which is for display only -- it just normalizes
     * whitespace without removing unit numbers or encoding.
     */
    fun formatForNavigation(address: String): String {
        var clean = address.trim()
        // Remove unit/apt/room patterns that confuse Google Maps
        clean = clean.replace(Regex("\\s*#\\d+[A-Za-z]?"), "")          // #011, #4B
        clean = clean.replace(Regex("(?i)\\s*(apt|unit|suite|ste|rm|room)\\.?\\s*[A-Za-z0-9-]+"), "")
        clean = clean.replace(Regex(",\\s*,"), ",")                      // double commas
        clean = clean.replace(Regex("^\\s*,|,\\s*$"), "")               // leading/trailing commas
        clean = clean.trim().trimEnd(',', ' ')
        return android.net.Uri.encode(clean)
    }

    private fun cleanAddress(raw: String): String {
        return raw.trim()
            .replace("\n", " ")
            .replace("\r", "")
            .replace("\\s+".toRegex(), " ")
    }
}
