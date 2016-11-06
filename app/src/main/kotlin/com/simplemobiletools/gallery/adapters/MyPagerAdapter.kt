package com.simplemobiletools.gallery.adapters

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.util.SparseArray
import com.simplemobiletools.gallery.Constants
import com.simplemobiletools.gallery.activities.ViewPagerActivity
import com.simplemobiletools.gallery.fragments.PhotoFragment
import com.simplemobiletools.gallery.fragments.VideoFragment
import com.simplemobiletools.gallery.fragments.ViewPagerFragment
import com.simplemobiletools.gallery.models.Medium

class MyPagerAdapter(val activity: ViewPagerActivity, fm: FragmentManager, val media: MutableList<Medium>) : FragmentStatePagerAdapter(fm) {
    private val mFragments: SparseArray<ViewPagerFragment>

    init {
        mFragments = SparseArray<ViewPagerFragment>()
    }

    override fun getCount(): Int {
        return media.size
    }

    override fun getItem(position: Int): Fragment {
        val medium = media[position]
        val bundle = Bundle()
        bundle.putSerializable(Constants.MEDIUM, medium)
        val fragment: ViewPagerFragment

        if (medium.isVideo) {
            fragment = VideoFragment()
        } else {
            fragment = PhotoFragment()
        }

        mFragments.put(position, fragment)
        fragment.arguments = bundle
        fragment.setListener(activity)
        return fragment
    }

    fun itemDragged(pos: Int) {
        mFragments[pos]?.itemDragged()
    }

    fun updateUiVisibility(isFullscreen: Boolean, pos: Int) {
        for (i in -1..1) {
            val fragment = mFragments[pos + i]
            fragment?.systemUiVisibilityChanged(isFullscreen)
        }
    }

    fun updateItems(pos: Int) {
        for (i in -1..1) {
            val fragment = mFragments[pos + i]
            fragment?.updateItem()
        }
    }

    fun updateItems(newPaths: List<Medium>) {
        media.clear()
        media.addAll(newPaths)
        notifyDataSetChanged()
    }
}
