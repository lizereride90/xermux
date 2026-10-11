package com.xermux.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.xermux.app.R
import com.xermux.app.databinding.ActivityMainBinding

/**
 * 2 menus: Termux | Termux X11.
 * OPTIMISATION: fragments are added once and shown/hidden (not replaced)
 * so the terminal session and X11 surface survive menu switches.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val termuxFragment = TermuxFragment()
    private val x11Fragment = X11Fragment()
    private var activeTag = "termux"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                add(R.id.container, termuxFragment, "termux")
                add(R.id.container, x11Fragment, "x11")
                hide(x11Fragment)
            }
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_termux -> show("termux")
                R.id.menu_x11 -> show("x11")
                else -> false
            }
        }
    }

    private fun show(tag: String): Boolean {
        activeTag = tag
        supportFragmentManager.commit {
            if (tag == "termux") {
                show(supportFragmentManager.findFragmentByTag("termux") ?: termuxFragment)
                hide(supportFragmentManager.findFragmentByTag("x11") ?: x11Fragment)
            } else {
                show(supportFragmentManager.findFragmentByTag("x11") ?: x11Fragment)
                hide(supportFragmentManager.findFragmentByTag("termux") ?: termuxFragment)
            }
        }
        return true
    }
}
