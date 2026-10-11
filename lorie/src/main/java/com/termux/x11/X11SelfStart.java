package com.termux.x11;

import android.content.Context;
import android.util.Log;

/**
 * xermux: the normal termux-x11 flow starts the server from the Termux shell
 * (app_process + loader.apk + the termux-x11 script). We ship both halves in
 * one apk, so just boot the server right here in our own process instead.
 * Needs the bootstrap installed (same as ever) or start() says no and the
 * activity keeps showing "not connected".
 */
public class X11SelfStart {
    private static boolean started = false;

    public static synchronized void ensureStarted(Context context) {
        if (started)
            return;
        started = true;
        try {
            if (!CmdEntryPoint.startInProcess(context, new String[]{ ":0" })) {
                Log.w("X11SelfStart", "X server refused to start (bootstrap or xkb data missing?)");
                started = false;
            }
        } catch (Throwable t) {
            Log.e("X11SelfStart", "in-process X start failed", t);
            started = false;
        }
    }
}
