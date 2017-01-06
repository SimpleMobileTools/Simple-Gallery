package com.simplemobiletools.gallery.fragments

import android.support.v4.app.Fragment

abstract class ViewPagerFragment : Fragment() {
    var listener: FragmentListener? = null

    interface FragmentListener {
        fun fragmentClicked()

        fun systemUiVisibilityChanged(visibility: Int)
    }
}
