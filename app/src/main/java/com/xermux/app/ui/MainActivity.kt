package com.xermux.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.xermux.app.R
import com.xermux.app.databinding.ActivityMainBinding

// bottom bar with the two tabs. learned the hard way: don't `replace()`
// here, it nukes the terminal every time you switch. add once, hide/show.
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var termuxFragment: Fragment
    private lateinit var x11Fragment: Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            termuxFragment = TermuxFragment()
            x11Fragment = X11Fragment()
            supportFragmentManager.commit {
                add(R.id.container, termuxFragment, "termux")
                add(R.id.container, x11Fragment, "x11")
                hide(x11Fragment)
            }
        } else {
            // activity got recreated — reuse the fragments android kept,
            // don't blindly add new ones on top
            val keptTermux = supportFragmentManager.findFragmentByTag("termux")
            val keptX11 = supportFragmentManager.findFragmentByTag("x11")
            if (keptTermux == null || keptX11 == null) {
                termuxFragment = TermuxFragment()
                x11Fragment = X11Fragment()
                supportFragmentManager.commit {
                    add(R.id.container, termuxFragment, "termux")
                    add(R.id.container, x11Fragment, "x11")
                    hide(x11Fragment)
                }
            } else {
                termuxFragment = keptTermux
                x11Fragment = keptX11
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
            show(termuxFragment)
            hide(x11Fragment)
        }
        return true
    }

    private fun showX11(): Boolean {
        supportFragmentManager.commit {
            show(x11Fragment)
            hide(termuxFragment)
        }
        return true
    }
}
