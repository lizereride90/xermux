package com.xermux.app.termux

import android.content.Context
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import java.io.File

// builds the shell session. keeps it simple:
// no bootstrap yet -> plain /system/bin/sh so the app works out of the box.
// bootstrap present (files/usr/bin/bash) -> use that with -l.
object TermuxRunner {

    fun createSession(
        context: Context,
        client: TerminalSessionClient,
        shell: String = "/system/bin/sh"
    ): TerminalSession {
        val filesDir = context.filesDir.absolutePath
        val cwd = File(filesDir).apply { mkdirs() }.absolutePath

        val bash = File("$filesDir/usr/bin/bash")
        val cmd = if (bash.canExecute()) "${bash.absolutePath} -l" else shell
        val argv = cmd.split(" ").toTypedArray()

        // private dirs only, no storage permission dance
        File("$filesDir/home").mkdirs()
        File("$filesDir/tmp").mkdirs()

        val env = arrayOf(
            "TERM=xterm-256color",
            "HOME=$filesDir/home",
            "PREFIX=$filesDir/usr",
            "TMPDIR=$filesDir/tmp",
            "LANG=C.UTF-8"
        )

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
