package com.xermux.app.termux

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

// sticking around so the shell has an owner when you flip to the X11 tab.
// right now the fragment owns the actual session, this just gives it a
// service to bind to later when I move session handling out of the UI.
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
