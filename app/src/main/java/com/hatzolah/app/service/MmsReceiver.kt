package com.hatzolah.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Required stub for default SMS app eligibility.
 * MMS messages are not used by Hatzolah dispatch — this is a no-op.
 */
class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // No-op: MMS not used for dispatch
    }
}
