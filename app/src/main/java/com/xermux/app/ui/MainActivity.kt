package com.xermux.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.xermux.app.R
import com.xermux.app.databinding.ActivityMainBinding

// bottom bar with the two tabs. learned the hard way: don't `replace()`
// here, it nukes the terminal every time you switch. add once, hide/show.
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val termuxFragment = TermuxFragment()
    private val x11Fragment = X11Fragment()

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
                R.id.menu_termux -> showTermux()
                R.id.menu_x11 -> showX11()
                else -> false
            }
        }
    }

    private fun showTermux(): Boolean {
        supportFragmentManager.commit {
            show(supportFragmentManager.findFragmentByTag("termux") ?: termuxFragment)
            hide(supportFragmentManager.findFragmentByTag("x11") ?: x11Fragment)
        }
        return true
    }

    private fun showX11(): Boolean {
        supportFragmentManager.commit {
            show(supportFragmentManager.findFragmentByTag("x11") ?: x11Fragment)
            hide(supportFragmentManager.findFragmentByTag("termux") ?: termuxFragment)
        }
        return true
    }
}
