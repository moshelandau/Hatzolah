package com.hatzolah.app.util

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Records every SMS-app notification the dispatch listener inspects, with the
 * outcome of each sender-detection signal. Lets users see — via Admin → View
 * Notification Log — whether the listener is actually receiving notifications
 * and, if so, why a given notification was accepted or rejected.
 */
object NotificationDebugLog {

    private const val FILE_NAME = "notif_debug_log.txt"
    private const val MAX_BYTES = 40_000
    private const val TRIM_TO = 20_000

    @Synchronized
    fun log(context: Context, entry: String) {
        try {
            val timestamp = SimpleDateFormat("MM-dd HH:mm:ss", Locale.US).format(Date())
            val line = "[$timestamp] $entry\n"
            val file = File(context.filesDir, FILE_NAME)
            if (file.exists() && file.length() > MAX_BYTES) {
                val tail = file.readText().takeLast(TRIM_TO)
                file.writeText(tail)
            }
            file.appendText(line)
        } catch (_: Throwable) {
            // Never let logging crash the listener.
        }
    }

    fun readLog(context: Context): String {
        val file = File(context.filesDir, FILE_NAME)
        return if (file.exists() && file.length() > 0) file.readText()
        else "No SMS notifications have been seen by the listener yet."
    }

    fun clearLog(context: Context) {
        val file = File(context.filesDir, FILE_NAME)
        if (file.exists()) file.delete()
    }
}
