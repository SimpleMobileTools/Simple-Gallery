package com.simplemobiletools.gallery.fragments

import android.support.v4.app.Fragment

abstract class ViewPagerFragment : Fragment() {
    var listener: FragmentClickListener? = null

    abstract fun itemDragged()

    abstract fun systemUiVisibilityChanged(toFullscreen: Boolean)

    abstract fun updateItem()

    interface FragmentClickListener {
        fun fragmentClicked()
    }
}
