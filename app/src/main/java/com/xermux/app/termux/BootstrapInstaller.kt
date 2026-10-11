package com.xermux.app.termux

import android.content.Context
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

// fetches the termux bootstrap and unpacks it into files/usr so the
// terminal gets real pkg/bash instead of just /system/bin/sh.
// still experimental: java's zip reader can't see symlinks, so exotic
// packages might misbehave until I do this properly. worst case the
// button just fails and you're back on the basic shell.
class BootstrapInstaller(private val context: Context) {

    interface Listener {
        fun onStatus(line: String)
        fun onDone(ok: Boolean)
    }

    fun isInstalled(): Boolean =
        File(context.filesDir, "usr/bin/bash").canExecute()

    private fun arch(): String {
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        return when (abi) {
            "arm64-v8a" -> "aarch64"
            "armeabi-v7a", "armeabi" -> "arm"
            "x86_64" -> "x86_64"
            else -> "i686"
        }
    }

    fun installAsync(listener: Listener) {
        Thread {
            try {
                val a = arch()
                // same flavour termux-app itself embeds
                val ver = "2026.02.12-r1%2Bapt.android-7"
                val url =
                    URL("https://github.com/termux/termux-packages/releases/download/bootstrap-$ver/bootstrap-$a.zip")
                listener.onStatus("downloading full linux for $a (~100MB)…")
                val tmp = File(context.cacheDir, "bootstrap-$a.zip")
                download(url, tmp, listener)
                listener.onStatus("unpacking…")
                unpack(tmp, context.filesDir)
                tmp.delete()
                makeExecutables(File(context.filesDir, "usr/bin"))
                listener.onStatus("done — fresh shell coming up")
                listener.onDone(true)
            } catch (e: Exception) {
                listener.onStatus("didn't work: ${e.message}")
                listener.onDone(false)
            }
        }.start()
    }

    private fun download(url: URL, out: File, listener: Listener) {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 30_000
            readTimeout = 30_000
        }
        conn.connect()
        if (conn.responseCode !in 200..299) throw RuntimeException("http ${conn.responseCode}")
        val total = conn.contentLengthLong
        conn.inputStream.use { input ->
            FileOutputStream(out).use { output ->
                val buf = ByteArray(64 * 1024)
                var done = 0L
                var lastShown = -1
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    output.write(buf, 0, n)
                    done += n
                    if (total > 0) {
                        val pct = (done * 100 / total).toInt()
                        if (pct != lastShown && pct % 10 == 0) {
                            lastShown = pct
                            listener.onStatus("downloading… $pct%")
                        }
                    }
                }
            }
        }
    }

    private fun unpack(zip: File, root: File) {
        ZipInputStream(zip.inputStream().buffered()).use { zin ->
            var entry = zin.nextEntry
            while (entry != null) {
                val target = File(root, entry.name)
                if (!target.canonicalPath.startsWith(root.canonicalPath)) {
                    throw RuntimeException("sketchy zip path: ${entry.name}")
                }
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    FileOutputStream(target).use { out -> zin.copyTo(out) }
                }
                zin.closeEntry()
                entry = zin.nextEntry
            }
        }
    }

    private fun makeExecutables(dir: File) {
        if (!dir.isDirectory) return
        dir.walkTopDown().forEach { f ->
            if (f.isFile) f.setExecutable(true)
        }
    }
}
