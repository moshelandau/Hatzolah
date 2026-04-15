package com.hatzolah.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * Required stub for default SMS app eligibility.
 * Handles SEND/SENDTO intents for sms:/smsto: schemes.
 * Hatzolah does not compose SMS — immediately finishes.
 */
class ComposeSmsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Not an SMS composer — redirect to main app
        finish()
    }
}
