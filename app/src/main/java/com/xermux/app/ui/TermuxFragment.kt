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

/**
 * Menu 1: Termux. Inbuilt terminal via official terminal-view lib.
 * OPTIMISATION: session created once per fragment lifetime, runs on
 * app-wide bgExecutor, transcript capped, view detaches on hide.
 */
class TermuxFragment : Fragment(), TerminalSessionClient {

    private var _binding: FragmentTermuxBinding? = null
    private val binding get() = _binding!!
    private var session: TerminalSession? = null
    private var terminalView: TerminalView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTermuxBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        terminalView = binding.terminalView
        binding.btnNewSession.setOnClickListener { restartSession() }
        binding.btnClear.setOnClickListener { session?.reset() }
        if (session == null) restartSession() else attach()
    }

    private fun restartSession() {
        val app = requireContext().applicationContext as XermuxApp
        app.bgExecutor.execute {
            session?.finishIfRunning()
            val s = TermuxRunner.createSession(
                requireContext(), this@TermuxFragment, app.prefs.termuxShell
            )
            session = s
            activity?.runOnUiThread { attach() }
        }
    }

    private fun attach() {
        val s = session ?: return
        terminalView?.attachSession(s)
        binding.statusText.text = "shell attached — transcript capped, executor-shared"
    }

    override fun onDestroyView() {
        terminalView?.attachSession(null)
        _binding = null
        super.onDestroyView()
    }

    // --- TerminalSessionClient (minimal, no-op where safe) ---
    override fun onTextChanged(session: TerminalSession?) {}
    override fun onTitleChanged(session: TerminalSession?) {}
    override fun onSessionFinished(session: TerminalSession?) {
        activity?.runOnUiThread { binding.statusText.text = "session exited — tap New to restart" }
    }
    override fun onCopyTextToClipboard(session: TerminalSession?, text: String?) {}
    override fun onPasteTextFromClipboard(session: TerminalSession?) {}
    override fun onBell(session: TerminalSession?) {}
    override fun onColorsChanged(session: TerminalSession?) {}
    override fun onTerminalCursorStateChange(state: Boolean) {}
    override fun getTerminalCursorStyle(): Int = 0
}
