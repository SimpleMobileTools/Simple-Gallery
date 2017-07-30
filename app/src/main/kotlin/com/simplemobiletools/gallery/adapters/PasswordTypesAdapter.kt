package com.simplemobiletools.gallery.adapters

import android.content.Context
import android.support.v4.view.PagerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.views.PatternTab

class PasswordTypesAdapter(val context: Context, val requiredHash: String, val hashListener: PatternTab.HashListener) : PagerAdapter() {

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = LayoutInflater.from(context).inflate(layoutSelection(position), container, false)
        container.addView(view)
        if (position == 0)
            (view as PatternTab).initTab(requiredHash, hashListener)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, item: Any) {
        container.removeView(item as View)
    }

    override fun getCount() = 2
    override fun isViewFromObject(view: View, item: Any) = view == item

    private fun layoutSelection(position: Int): Int = when (position) {
        0 -> R.layout.tab_pattern
        1 -> R.layout.tab_pin
        else -> throw RuntimeException("Only 2 tabs allowed")
    }
}
