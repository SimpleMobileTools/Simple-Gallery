package com.simplemobiletools.gallery.fragments

import android.support.v4.app.Fragment

abstract class ViewPagerFragment : Fragment() {
    var listener: FragmentListener? = null

    abstract fun fullscreenToggled(isFullscreen: Boolean)

    interface FragmentListener {
        fun fragmentClicked()
    }
}
