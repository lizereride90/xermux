package com.xermux.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.view.TerminalView
import com.xermux.app.XermuxApp
import com.xermux.app.databinding.FragmentTermuxBinding
import com.xermux.app.termux.TermuxRunner

// the termux tab. creates the session once and re-attaches the view,
// otherwise rotating the phone or switching tabs kills what you were doing.
class TermuxFragment : Fragment(), TerminalSessionClient {

    private var _binding: FragmentTermuxBinding? = null
    private val binding get() = _binding!!

    private var session: TerminalSession? = null
    private var termView: TerminalView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTermuxBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        termView = binding.terminalView

        binding.btnNewSession.setOnClickListener { startShell() }
        binding.btnClear.setOnClickListener { session?.reset() }

        if (session == null) startShell() else attach()
    }

    private fun startShell() {
        val app = requireContext().applicationContext as XermuxApp
        binding.statusText.text = "starting shell…"
        app.bgExecutor.execute {
            session?.finishIfRunning()
            val fresh = TermuxRunner.createSession(
                requireContext(), this, app.prefs.termuxShell
            )
            session = fresh
            activity?.runOnUiThread { attach() }
        }
    }

    private fun attach() {
        val s = session ?: return
        termView?.attachSession(s)
        binding.statusText.text = "shell attached"
    }

    override fun onDestroyView() {
        // detach so the view doesn't leak, session itself stays alive
        termView?.attachSession(null)
        _binding = null
        super.onDestroyView()
    }

    // don't need most of these yet, but the interface wants them
    override fun onTextChanged(session: TerminalSession?) {}
    override fun onTitleChanged(session: TerminalSession?) {}
    override fun onSessionFinished(session: TerminalSession?) {
        activity?.runOnUiThread {
            binding.statusText.text = "shell exited — hit New to start another"
        }
    }
    override fun onCopyTextToClipboard(session: TerminalSession?, text: String?) {}
    override fun onPasteTextFromClipboard(session: TerminalSession?) {}
    override fun onBell(session: TerminalSession?) {}
    override fun onColorsChanged(session: TerminalSession?) {}
    override fun onTerminalCursorStateChange(state: Boolean) {}
    override fun getTerminalCursorStyle(): Int = 0

    // termux lib wants this for its own error logging, just ignore it here
    override fun logError(tag: String, message: String) {}
}
