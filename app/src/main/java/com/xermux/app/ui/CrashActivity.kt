package com.xermux.app.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.xermux.app.R
import java.io.File

// plain framework Activity on purpose — if our own theme is what crashed,
// this screen must not depend on it.
class CrashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crash)

        var trace = intent.getStringExtra("trace")
        if (trace.isNullOrEmpty()) {
            trace = try {
                File(filesDir, "last_crash.log").readText()
            } catch (_: Exception) {
                "no trace saved, weird."
            }
        }

        findViewById<TextView>(R.id.crash_text).text = trace
        findViewById<Button>(R.id.btn_copy).setOnClickListener {
            val cm = getSystemService(ClipboardManager::class.java)
            cm.setPrimaryClip(ClipData.newPlainText("xermux crash", trace))
            Toast.makeText(this, "copied — paste it in the xermux issue/chat", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btn_close).setOnClickListener { finishAffinity() }
    }
}
