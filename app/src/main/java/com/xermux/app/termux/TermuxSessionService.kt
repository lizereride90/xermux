package com.xermux.app.termux

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

/**
 * Holds the active TerminalSession so rotation / switching to X11 menu
 * does not kill the shell. Bound by TermuxFragment.
 * OPTIMISATION: single session reuse + foreground lifetime only while visible.
 */
class TermuxSessionService : Service() {

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): TermuxSessionService = this@TermuxSessionService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
}
