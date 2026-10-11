package com.xermux.app.x11

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

// owns the X server lifecycle. tries the bundled native lib first,
// falls back to companion mode (server started from the termux shell)
// so the app still works before I get the .so bundled.
class XServerService : Service() {

    companion object {
        const val ACTION_START = "com.xermux.app.x11.START"
        const val ACTION_STOP = "com.xermux.app.x11.STOP"
        const val CHANNEL = "x11"

        var running = false
            private set
        var display: String = ":0"
            private set
    }

    private val binder = LocalBinder()
    inner class LocalBinder : Binder() {
        fun getService(): XServerService = this@XServerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, "X11 Server", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startX(intent.getStringExtra("resolution") ?: "1280x720")
            ACTION_STOP -> stopX()
        }
        return START_NOT_STICKY
    }

    private fun startX(resolution: String) {
        if (running) return
        running = true
        display = ":0"

        // only there once someone drops libXlorie.so into jniLibs, ignore until then
        try {
            System.loadLibrary("Xlorie")
            nativeStart(resolution)
        } catch (_: UnsatisfiedLinkError) {
        }

        startForeground(1, note("X server up on $display ($resolution)"))
    }

    private fun stopX() {
        if (!running) return
        try {
            nativeStop()
        } catch (_: UnsatisfiedLinkError) {
        }
        running = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun note(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL)
            .setContentTitle("Xermux X11")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setOngoing(true)
            .build()
    }

    // these exist in termux-x11's CmdEntryPoint, same signatures for later
    private external fun nativeStart(resolution: String)
    private external fun nativeStop()
}
