package com.hatzolah.app.service

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parses incoming dispatch SMS messages to extract address and call details.
 * Hatzolah dispatch messages typically contain the address of the emergency.
 */
@Singleton
class SmsParser @Inject constructor() {

    data class ParsedDispatch(
        val address: String,
        val rawMessage: String,
        val callType: String = ""
    )

    /**
     * Extracts the dispatch address from an incoming SMS message.
     * Handles common Hatzolah dispatch message formats.
     */
    fun parseDispatchMessage(message: String): ParsedDispatch? {
        val trimmed = message.trim()
        if (trimmed.isBlank()) return null

        // Try to extract address from common dispatch formats
        // Format: "CALL: <type> at <address>"
        val callAtPattern = Regex("(?i)call:?\\s*(.+?)\\s+at\\s+(.+)", RegexOption.DOT_MATCHES_ALL)
        callAtPattern.find(trimmed)?.let { match ->
            return ParsedDispatch(
                address = cleanAddress(match.groupValues[2]),
                rawMessage = trimmed,
                callType = match.groupValues[1].trim()
            )
        }

        // Format: "DISPATCH <address>"
        val dispatchPattern = Regex("(?i)dispatch:?\\s+(.+)", RegexOption.DOT_MATCHES_ALL)
        dispatchPattern.find(trimmed)?.let { match ->
            return ParsedDispatch(
                address = cleanAddress(match.groupValues[1]),
                rawMessage = trimmed
            )
        }

        // Format: Address on its own line (common simple format)
        // If message contains a street number followed by a street name
        val addressPattern = Regex("(\\d+\\s+[A-Za-z].+)")
        addressPattern.find(trimmed)?.let { match ->
            return ParsedDispatch(
                address = cleanAddress(match.groupValues[1]),
                rawMessage = trimmed
            )
        }

        // Fallback: use the entire message as the address
        return ParsedDispatch(
            address = cleanAddress(trimmed),
            rawMessage = trimmed
        )
    }

    /**
     * Formats an address for use in Google Maps navigation.
     */
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
