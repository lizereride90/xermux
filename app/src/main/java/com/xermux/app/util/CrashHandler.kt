package com.xermux.app.util

import android.content.Context
import android.content.Intent
import com.xermux.app.ui.CrashActivity
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

// last resort: if anything blows up, show the stack trace inside the app
// instead of the generic "keeps stopping" popup. makes bug reports useful.
class CrashHandler(
    private val app: Context,
    private val old: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    private var alreadyCrashed = false

    override fun uncaughtException(thread: Thread, e: Throwable) {
        if (alreadyCrashed) {
            old?.uncaughtException(thread, e)
            return
        }
        alreadyCrashed = true

        val trace = StringWriter().also { e.printStackTrace(PrintWriter(it)) }.toString()
        try {
            File(app.filesDir, "last_crash.log").writeText(trace)
        } catch (_: Exception) {
        }

        // crash screen lives in its own process so a broken main process
        // can't take it down too
        val i = Intent(app, CrashActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra("trace", trace)
        }
        try {
            app.startActivity(i)
        } catch (_: Exception) {
        }
        exitProcess(10)
    }
}
