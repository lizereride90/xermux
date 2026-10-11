package com.xermux.app.util

import android.content.Context
import androidx.core.content.edit

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("xermux", Context.MODE_PRIVATE)

    var xResolution: String
        get() = sp.getString("x_resolution", "1280x720") ?: "1280x720"
        set(v) = sp.edit { putString("x_resolution", v) }

    var xTouchMode: String
        get() = sp.getString("x_touch_mode", "touchpad") ?: "touchpad"
        set(v) = sp.edit { putString("x_touch_mode", v) }

    var termuxShell: String
        get() = sp.getString("termux_shell", "/system/bin/sh") ?: "/system/bin/sh"
        set(v) = sp.edit { putString("termux_shell", v) }

    var bootstrapInstalled: Boolean
        get() = sp.getBoolean("bootstrap_installed", false)
        set(v) = sp.edit { putBoolean("bootstrap_installed", v) }
}
