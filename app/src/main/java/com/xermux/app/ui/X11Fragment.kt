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

/**
 * Menu 2: Termux X11. Embedded viewer + server controller.
 * OPTIMISATION: SurfaceView created once, hardware-accelerated, server runs
 * in foreground service only while connected; resolution switch reuses service.
 */
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
            log("X server starting $res DISPLAY=:0 …\nRun in Termux menu: pkg i x11-repo && pkg i termux-x11-nightly && termux-x11 :0 &")
        }
        binding.btnStop.setOnClickListener {
            requireContext().startService(
                Intent(requireContext(), XServerService::class.java).apply {
                    action = XServerService.ACTION_STOP
                }
            )
            log("X server stopped.")
        }
        binding.helpText.text = "1) Open Termux menu, install: pkg i x11-repo && pkg i termux-x11-nightly\n" +
            "2) Start server here, then in Termux run: DISPLAY=:0 xfce4-session &\n" +
            "3) Native libXlorie.so (from termux-x11) can be dropped into app/src/main/jniLibs later for zero-setup mode."
    }

    private fun log(s: String) {
        binding.logView.append("\n$s")
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
