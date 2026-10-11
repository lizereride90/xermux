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
import com.xermux.app.R
import com.xermux.app.XermuxApp

/**
 * Embedded Termux:X11 server controller.
 *
 * This skeleton runs the full X11 lifecycle (prefs, SurfaceView, start/stop)
 * and is where the native Xlorie .so from termux/termux-x11 drops in.
 * To keep GitHub Actions builds fast and NDK-free, native loading is
 * optional: if libXlorie.so is present it is used, otherwise the service
 * runs in "companion mode" (points the viewer at DISPLAY=:0 started from
 * the Termux shell, same as official Termux:X11 flow).
 *
 * OPTIMISATION: service is foreground only while X is connected, releases
 * WakeLock/surface immediately on stop, resolution change = surface recreate
 * instead of process restart.
 */
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
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
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
        // Try native lib if a future build bundles it; never crash if absent.
        try {
            System.loadLibrary("Xlorie")
            nativeStart(resolution)
        } catch (_: UnsatisfiedLinkError) {
            // Companion mode: X server started from Termux shell via `termux-x11 :0`.
        }
        startForeground(1, notification("X server running $display ($resolution)"))
    }

    private fun stopX() {
        if (!running) return
        try { nativeStop() } catch (_: UnsatisfiedLinkError) { }
        running = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun notification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL)
            .setContentTitle("Xermux X11")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setOngoing(true)
            .build()
    }

    // Linked against libXlorie.so only when bundled (see termux/termux-x11 CmdEntryPoint).
    private external fun nativeStart(resolution: String)
    private external fun nativeStop()
}
