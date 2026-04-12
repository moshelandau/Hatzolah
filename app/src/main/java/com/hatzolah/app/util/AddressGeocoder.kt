package com.hatzolah.app.util

import android.content.Context
import android.location.Geocoder
import android.os.Build
import java.util.Locale

/**
 * Thin wrapper around Android's Geocoder. Uses the async API on Android 13+,
 * falls back to the synchronous (deprecated) API on older versions.
 *
 * Call [geocode] from a background dispatcher — the older API is blocking.
 */
object AddressGeocoder {

    data class LatLng(val latitude: Double, val longitude: Double)

    /**
     * Geocodes [address] to a LatLng. Returns null on failure (service unavailable,
     * no results, IO error). Caller must be on a background thread.
     */
    @Suppress("DEPRECATION")
    fun geocode(context: Context, address: String): LatLng? {
        if (address.isBlank()) return null
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.getDefault())
        // Strip unit numbers which confuse the geocoder
        val clean = address
            .replace(Regex("\\s*#\\d+[A-Za-z]?"), "")
            .replace(Regex("(?i)\\s*(apt|unit|suite|ste|rm|room)\\.?\\s*[A-Za-z0-9-]+"), "")
            .replace(Regex(",\\s*,"), ",")
            .trim()
            .trimEnd(',', ' ')
        return try {
            val results = geocoder.getFromLocationName(clean, 1)
            val first = results?.firstOrNull() ?: return null
            LatLng(first.latitude, first.longitude)
        } catch (_: Throwable) {
            null
        }
    }
}
