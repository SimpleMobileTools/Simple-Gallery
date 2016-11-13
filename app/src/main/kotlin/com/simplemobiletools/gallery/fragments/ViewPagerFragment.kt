package com.simplemobiletools.gallery.fragments

import android.support.v4.app.Fragment

abstract class ViewPagerFragment : Fragment() {
    lateinit var listener: FragmentClickListener

    abstract fun itemDragged()

    abstract fun systemUiVisibilityChanged(toFullscreen: Boolean)

    abstract fun updateItem()

    interface FragmentClickListener {
        fun fragmentClicked()
    }
}
