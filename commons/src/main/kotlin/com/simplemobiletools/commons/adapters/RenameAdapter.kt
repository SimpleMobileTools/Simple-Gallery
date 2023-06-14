package com.simplemobiletools.commons.adapters

import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.interfaces.RenameTab
import java.util.*

class RenameAdapter(val activity: BaseSimpleActivity, val paths: ArrayList<String>) : PagerAdapter() {
    private val tabs = SparseArray<RenameTab>()

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = LayoutInflater.from(activity).inflate(layoutSelection(position), container, false)
        container.addView(view)
        tabs.put(position, view as RenameTab)
        (view as RenameTab).initTab(activity, paths)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, item: Any) {
        tabs.remove(position)
        container.removeView(item as View)
    }

    override fun getCount() = 2

    override fun isViewFromObject(view: View, item: Any) = view == item

    private fun layoutSelection(position: Int): Int = when (position) {
        0 -> R.layout.tab_rename_simple
        1 -> R.layout.tab_rename_pattern
        else -> throw RuntimeException("Only 2 tabs allowed")
    }

    fun dialogConfirmed(useMediaFileExtension: Boolean, position: Int, callback: (success: Boolean) -> Unit) {
        tabs[position].dialogConfirmed(useMediaFileExtension, callback)
    }
}
