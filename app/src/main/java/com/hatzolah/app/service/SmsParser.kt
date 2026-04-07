package com.hatzolah.app.service

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsParser @Inject constructor() {

    data class ParsedDispatch(
        val address: String,
        val rawMessage: String,
        val callType: String = ""
    )

    /**
     * Parses dispatch SMS messages in various formats.
     * Hatzolah dispatch formats vary - this handles the most common ones.
     */
    fun parseDispatchMessage(message: String): ParsedDispatch? {
        val trimmed = message.trim()
        if (trimmed.isBlank()) return null

        // Try structured formats first, then fall back to raw parsing

        // Format: "CALL: <type> at <address>"
        Regex("(?i)call:?\\s*(.+?)\\s+at\\s+(.+)", RegexOption.DOT_MATCHES_ALL)
            .find(trimmed)?.let { match ->
                return ParsedDispatch(
                    address = cleanAddress(match.groupValues[2]),
                    rawMessage = trimmed,
                    callType = match.groupValues[1].trim()
                )
            }

        // Format: "<type> - <address>" or "<type>: <address>"
        Regex("^([A-Za-z /]+?)\\s*[-:]\\s*(\\d+.+)", RegexOption.DOT_MATCHES_ALL)
            .find(trimmed)?.let { match ->
                return ParsedDispatch(
                    address = cleanAddress(match.groupValues[2]),
                    rawMessage = trimmed,
                    callType = match.groupValues[1].trim()
                )
            }

        // Format: "DISPATCH <address>" or "DISP <address>"
        Regex("(?i)(?:dispatch|disp):?\\s+(.+)", RegexOption.DOT_MATCHES_ALL)
            .find(trimmed)?.let { match ->
                return ParsedDispatch(
                    address = cleanAddress(match.groupValues[1]),
                    rawMessage = trimmed
                )
            }

        // Format: Multi-line - first line is type, rest is address
        val lines = trimmed.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.size >= 2) {
            val firstLine = lines[0]
            val rest = lines.drop(1).joinToString(" ")
            // If first line has no numbers (likely call type) and rest has numbers (likely address)
            if (!firstLine.contains(Regex("\\d{2,}")) && rest.contains(Regex("\\d"))) {
                return ParsedDispatch(
                    address = cleanAddress(rest),
                    rawMessage = trimmed,
                    callType = firstLine.replace(Regex("[:\\-]+$"), "").trim()
                )
            }
        }

        // Format: Contains a street address pattern anywhere
        Regex("(\\d+\\s+[A-Za-z][A-Za-z .']+(?:St|Street|Ave|Avenue|Rd|Road|Dr|Drive|Blvd|Ln|Lane|Ct|Court|Pl|Place|Way|Pkwy|Hwy)[A-Za-z0-9,. #]*)", RegexOption.IGNORE_CASE)
            .find(trimmed)?.let { match ->
                val address = match.groupValues[1]
                val callType = trimmed.substringBefore(address).trim().trimEnd('-', ':', ' ')
                return ParsedDispatch(
                    address = cleanAddress(address),
                    rawMessage = trimmed,
                    callType = callType
                )
            }

        // Fallback: use entire message as both address and content
        return ParsedDispatch(
            address = cleanAddress(trimmed),
            rawMessage = trimmed
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
