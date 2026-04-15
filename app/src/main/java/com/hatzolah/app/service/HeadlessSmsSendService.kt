package com.hatzolah.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Required stub for default SMS app eligibility.
 * Handles respond-via-message intents (e.g. "respond" from incoming call screen).
 * Hatzolah does not send SMS — this is a no-op.
 */
class HeadlessSmsSendService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stopSelf()
        return START_NOT_STICKY
    }
}
