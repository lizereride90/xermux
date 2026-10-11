package com.xermux.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.xermux.app.XermuxApp
import com.xermux.app.databinding.FragmentX11Binding
import com.xermux.app.x11.XServerService

// the x11 tab. spinner for resolution, start/stop, and a little log
// so you can see what happened without digging in logcat.
class X11Fragment : Fragment() {

    private var _binding: FragmentX11Binding? = null
    private val binding get() = _binding!!

    private val resolutions = listOf("800x600", "1024x768", "1280x720", "1920x1080")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentX11Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val app = requireContext().applicationContext as XermuxApp

        binding.resSpinner.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item, resolutions
        )
        binding.resSpinner.setSelection(
            resolutions.indexOf(app.prefs.xResolution).coerceAtLeast(0)
        )

        binding.btnStart.setOnClickListener {
            val res = binding.resSpinner.selectedItem as String
            app.prefs.xResolution = res
            requireContext().startService(
                Intent(requireContext(), XServerService::class.java).apply {
                    action = XServerService.ACTION_START
                    putExtra("resolution", res)
                }
            )
            say("starting X on :0 ($res)…")
            say("then in the Termux tab run:\n  pkg i x11-repo && pkg i termux-x11-nightly\n  termux-x11 :0 &")
        }

        binding.btnStop.setOnClickListener {
            requireContext().startService(
                Intent(requireContext(), XServerService::class.java).apply {
                    action = XServerService.ACTION_STOP
                }
            )
            say("stopped.")
        }

        binding.helpText.text =
            "quick version:\n" +
                "1. hit Start above\n" +
                "2. in the Termux tab: DISPLAY=:0 xfce4-session &\n" +
                "later I want to bundle libXlorie.so so step 2 just works."
    }

    private fun say(line: String) {
        binding.logView.append("\n$line")
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
