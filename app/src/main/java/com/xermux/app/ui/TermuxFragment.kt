package com.xermux.app.ui

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import androidx.fragment.app.Fragment
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient
import com.xermux.app.XermuxApp
import com.xermux.app.databinding.FragmentTermuxBinding
import com.xermux.app.termux.BootstrapInstaller
import com.xermux.app.termux.TermuxRunner

// the termux tab. creates the session once and re-attaches the view,
// otherwise rotating the phone or switching tabs kills what you were doing.
class TermuxFragment : Fragment(), TerminalSessionClient, TerminalViewClient {

    private var _binding: FragmentTermuxBinding? = null
    private val binding get() = _binding!!

    private var session: TerminalSession? = null
    private var termView: TerminalView? = null

    // extra keys bar is built in code, easier to tweak than xml
    private val extraKeys = listOf(
        "ESC", "TAB", "CTRL", "ALT", "|", "-", "/", "~", ".",
        "\"", "'", "$", "←", "→", "↑", "↓", "HOME", "END", "PGUP", "PGDN", "^C", "^D"
    )
    private val keyButtons = mutableMapOf<String, Button>()
    private var ctrlHeld = false
    private var altHeld = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTermuxBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        termView = binding.terminalView

        // order matters here and I got burned by it twice:
        // 1. setTextSize creates the view's renderer — without it the first
        //    layout crashes in updateSize() on a null renderer.
        // 2. the view client must be set before attachSession, attach calls
        //    back into onEmulatorSet() immediately.
        termView?.setTerminalViewClient(this)
        termView?.setTextSize(14)

        binding.btnNewSession.setOnClickListener { startShell() }
        binding.btnClear.setOnClickListener { session?.reset() }

        buildExtraKeys()
        wireBootstrap()

        if (session == null) startShell() else attach()
    }

    private fun buildExtraKeys() {
        binding.extraKeysRow.removeAllViews()
        keyButtons.clear()
        for (label in extraKeys) {
            val b = Button(requireContext(), null, android.R.attr.buttonStyleSmall).apply {
                text = label
                minWidth = 0
                minimumWidth = 0
                setPadding(20, 0, 20, 0)
                setOnClickListener { pressExtra(label) }
            }
            keyButtons[label] = b
            binding.extraKeysRow.addView(b)
        }
        refreshToggles()
    }

    private fun refreshToggles() {
        keyButtons["CTRL"]?.isActivated = ctrlHeld
        keyButtons["ALT"]?.isActivated = altHeld
        // cheap highlight so you can tell the modifier is armed
        keyButtons["CTRL"]?.alpha = if (ctrlHeld) 1.0f else 0.6f
        keyButtons["ALT"]?.alpha = if (altHeld) 1.0f else 0.6f
    }

    private fun pressExtra(label: String) {
        val s = session ?: return
        when (label) {
            "CTRL" -> {
                ctrlHeld = !ctrlHeld
                refreshToggles()
                return
            }
            "ALT" -> {
                altHeld = !altHeld
                refreshToggles()
                return
            }
            "^C" -> {
                writeBytes(s, byteArrayOf(0x03))
                return
            }
            "^D" -> {
                writeBytes(s, byteArrayOf(0x04))
                return
            }
        }
        var text = when (label) {
            "ESC" -> "\u001B"
            "TAB" -> "\t"
            "←" -> "\u001B[D"
            "→" -> "\u001B[C"
            "↑" -> "\u001B[A"
            "↓" -> "\u001B[B"
            "HOME" -> "\u001B[H"
            "END" -> "\u001B[F"
            "PGUP" -> "\u001B[5~"
            "PGDN" -> "\u001B[6~"
            else -> label
        }
        // ctrl + letter -> control byte (c-a+1 etc), then disarm
        if (ctrlHeld && text.length == 1 && text[0].isLetter()) {
            val c = text[0].lowercaseChar()
            writeBytes(s, byteArrayOf((c - 'a' + 1).toByte()))
            ctrlHeld = false
            refreshToggles()
            return
        }
        if (altHeld) {
            text = "\u001B$text"
            altHeld = false
            refreshToggles()
        }
        writeBytes(s, text.toByteArray(Charsets.UTF_8))
    }

    private fun writeBytes(s: TerminalSession, b: ByteArray) {
        s.write(b, 0, b.size)
    }

    private fun wireBootstrap() {
        val installer = BootstrapInstaller(requireContext())
        if (installer.isInstalled()) {
            binding.bootstrapStatus.text = "full linux ready"
            binding.btnBootstrap.visibility = View.GONE
        } else {
            binding.bootstrapStatus.text = "basic shell — grab the full linux (~100MB)"
            binding.btnBootstrap.setOnClickListener { btn ->
                btn.isEnabled = false
                installer.installAsync(object : BootstrapInstaller.Listener {
                    override fun onStatus(line: String) {
                        activity?.runOnUiThread { _binding?.bootstrapStatus?.text = line }
                    }

                    override fun onDone(ok: Boolean) {
                        activity?.runOnUiThread {
                            btn.isEnabled = true
                            if (ok) {
                                binding.btnBootstrap.visibility = View.GONE
                                startShell()
                            }
                        }
                    }
                })
            }
        }
    }

    private fun startShell() {
        // TerminalSession makes its own Handler inside the constructor, so this
        // MUST run on the main thread (has a Looper). used to do it on bgExecutor
        // and that's the "Can't create handler inside thread" crash.
        val app = requireContext().applicationContext as XermuxApp
        binding.statusText.text = "starting shell…"
        session?.finishIfRunning()
        session = TermuxRunner.createSession(requireContext(), this, app.prefs.termuxShell)
        attach()
    }

    private fun attach() {
        val s = session ?: return
        termView?.attachSession(s)
        termView?.requestFocus()
        binding.statusText.text = "shell attached"
    }

    override fun onDestroyView() {
        // detach so the view doesn't leak, session itself stays alive
        termView?.attachSession(null)
        _binding = null
        super.onDestroyView()
    }

    // --- TerminalSessionClient, mostly no-ops ---

    override fun onTextChanged(session: TerminalSession?) {}
    override fun onTitleChanged(session: TerminalSession?) {}
    override fun onSessionFinished(session: TerminalSession?) {
        activity?.runOnUiThread {
            _binding?.statusText?.text = "shell exited — hit New to start another"
        }
    }
    override fun onCopyTextToClipboard(session: TerminalSession?, text: String?) {}
    override fun onPasteTextFromClipboard(session: TerminalSession?) {}
    override fun onBell(session: TerminalSession?) {}
    override fun onColorsChanged(session: TerminalSession?) {}
    override fun onTerminalCursorStateChange(state: Boolean) {}
    override fun getTerminalCursorStyle(): Int = 0

    // termux lib wants a bunch of log sinks, don't need them in the UI
    override fun logError(tag: String, message: String) {}
    override fun logWarn(tag: String, message: String) {}
    override fun logInfo(tag: String, message: String) {}
    override fun logDebug(tag: String, message: String) {}
    override fun logVerbose(tag: String, message: String) {}
    override fun logStackTraceWithMessage(tag: String, message: String, e: Exception?) {}
    override fun logStackTrace(tag: String, e: Exception?) {}

    // --- TerminalViewClient ---

    override fun onScale(scale: Float): Float = scale.coerceIn(0.5f, 3.0f)

    override fun onSingleTapUp(e: MotionEvent) {
        termView?.requestFocus()
        val imm = requireContext().getSystemService(InputMethodManager::class.java)
        imm.showSoftInput(termView, InputMethodManager.SHOW_IMPLICIT)
    }

    override fun shouldBackButtonBeMappedToEscape(): Boolean = true
    override fun shouldEnforceCharBasedInput(): Boolean = false
    override fun shouldUseCtrlSpaceWorkaround(): Boolean = false
    override fun isTerminalViewSelected(): Boolean = true
    override fun copyModeChanged(copyMode: Boolean) {}
    override fun onKeyDown(keyCode: Int, e: KeyEvent, session: TerminalSession): Boolean = false
    override fun onKeyUp(keyCode: Int, e: KeyEvent): Boolean = false
    override fun onLongPress(event: MotionEvent): Boolean = false
    override fun readControlKey(): Boolean = false
    override fun readAltKey(): Boolean = false
    override fun readShiftKey(): Boolean = false
    override fun readFnKey(): Boolean = false
    override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession): Boolean = false
    override fun onEmulatorSet() {
        termView?.invalidate()
    }
}
