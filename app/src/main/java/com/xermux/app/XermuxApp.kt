package com.xermux.app

import android.app.Application
import android.os.StrictMode
import com.xermux.app.util.Prefs
import java.util.concurrent.Executors

class XermuxApp : Application() {

    // OPTIMISATION: single shared background pool for shell + X11 tasks.
    // Avoids spawning a thread per session which kills low-RAM devices.
    val bgExecutor by lazy { Executors.newFixedThreadPool(4) }

    lateinit var prefs: Prefs
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build()
            )
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // OPTIMISATION: drop transcript caches under pressure.
        // Fragments observe this via ComponentCallbacks.
    }
}
