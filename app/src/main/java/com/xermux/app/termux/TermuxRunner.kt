package com.xermux.app.termux

import android.content.Context
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import java.io.File

/**
 * Creates TerminalSessions backed by the inbuilt Termux emulator lib.
 * Default: /system/bin/sh (zero download). If bootstrap is installed under
 * files/usr, uses <filesDir>/usr/bin/login shell instead.
 *
 * OPTIMISATION: transcript capped at 2000 lines, single-thread executor,
 * cwd = app private files dir (no storage permission needed).
 */
object TermuxRunner {

    const val TRANSCRIPT_ROWS = 2000

    fun createSession(
        context: Context,
        client: TerminalSessionClient,
        shell: String = "/system/bin/sh"
    ): TerminalSession {
        val filesDir = context.filesDir.absolutePath
        val cwd = File(filesDir).apply { mkdirs() }.absolutePath

        val executable = if (File("$filesDir/usr/bin/bash").canExecute()) {
            "$filesDir/usr/bin/bash -l"
        } else {
            shell
        }
        val parts = executable.split(" ")
        val argv = (parts + emptyArray<String>()).toTypedArray()

        val env = arrayOf(
            "TERM=xterm-256color",
            "HOME=$filesDir/home",
            "PREFIX=$filesDir/usr",
            "TMPDIR=$filesDir/tmp",
            "LANG=C.UTF-8"
        )
        File("$filesDir/home").mkdirs()
        File("$filesDir/tmp").mkdirs()

        return TerminalSession(
            argv[0],
            cwd,
            argv,
            env,
            TerminalEmulator.DEFAULT_TERMINAL_TRANSCRIPT_ROWS,
            client
        )
    }
}
