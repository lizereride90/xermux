package com.xermux.app

import android.app.Application
import android.os.StrictMode
import com.xermux.app.util.Prefs
import java.util.concurrent.Executors

class XermuxApp : Application() {

    // one pool for everything (shell startups, x11 bits). used to
    // spin up threads everywhere and it showed on cheap devices.
    val bgExecutor by lazy { Executors.newFixedThreadPool(4) }

    lateinit var prefs: Prefs
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)

        // catch disk/network on main thread while I'm still developing
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build()
            )
        }
    }
}
