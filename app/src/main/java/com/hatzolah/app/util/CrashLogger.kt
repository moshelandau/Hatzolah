package com.hatzolah.app.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Global crash handler that saves crash logs to a file.
 * Check: /data/data/com.hatzolah.app/files/crash_log.txt
 * Or via: Admin > Settings > View Crash Log
 */
class CrashLogger(private val context: Context) : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    fun install() {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)
            val stackTrace = sw.toString()

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            val entry = """
                |=== CRASH at $timestamp ===
                |Thread: ${thread.name}
                |Exception: ${throwable.javaClass.name}: ${throwable.message}
                |
                |$stackTrace
                |
            """.trimMargin()

            Log.e("CrashLogger", entry)

            val file = File(context.filesDir, "crash_log.txt")
            // Keep last 50KB of crash logs
            if (file.exists() && file.length() > 50_000) {
                val existing = file.readText()
                file.writeText(existing.takeLast(25_000))
            }
            file.appendText(entry)
        } catch (_: Throwable) {
            // Don't crash in the crash handler
        }

        // Pass to default handler (shows system crash dialog)
        defaultHandler?.uncaughtException(thread, throwable)
    }

    companion object {
        fun readLog(context: Context): String {
            val file = File(context.filesDir, "crash_log.txt")
            return if (file.exists()) file.readText() else "No crashes recorded."
        }

        fun clearLog(context: Context) {
            val file = File(context.filesDir, "crash_log.txt")
            if (file.exists()) file.delete()
        }
    }
}
