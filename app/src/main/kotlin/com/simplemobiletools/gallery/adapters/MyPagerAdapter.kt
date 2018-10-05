package com.simplemobiletools.gallery.adapters

import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.PagerAdapter
import android.view.ViewGroup
import com.simplemobiletools.gallery.activities.ViewPagerActivity
import com.simplemobiletools.gallery.fragments.PhotoFragment
import com.simplemobiletools.gallery.fragments.VideoFragment
import com.simplemobiletools.gallery.fragments.ViewPagerFragment
import com.simplemobiletools.gallery.helpers.MEDIUM
import com.simplemobiletools.gallery.models.Medium

class MyPagerAdapter(val activity: ViewPagerActivity, fm: FragmentManager, val media: MutableList<Medium>) : FragmentStatePagerAdapter(fm) {
    private val fragments = HashMap<Int, ViewPagerFragment>()
    override fun getCount() = media.size

    override fun getItem(position: Int): Fragment {
        val medium = media[position]
        val bundle = Bundle()
        bundle.putSerializable(MEDIUM, medium)
        val fragment = if (medium.isVideo()) {
            VideoFragment()
        } else {
            PhotoFragment()
        }

        fragment.arguments = bundle
        fragment.listener = activity
        return fragment
    }

    override fun getItemPosition(item: Any) = PagerAdapter.POSITION_NONE

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val fragment = super.instantiateItem(container, position) as ViewPagerFragment
        fragments[position] = fragment
        return fragment
    }

    override fun destroyItem(container: ViewGroup, position: Int, any: Any) {
        fragments.remove(position)
        super.destroyItem(container, position, any)
    }

    fun getCurrentFragment(position: Int) = fragments[position]

    fun toggleFullscreen(isFullscreen: Boolean) {
        for ((pos, fragment) in fragments) {
            fragment.fullscreenToggled(isFullscreen)
        }
    }

    // try fixing TransactionTooLargeException crash on Android Nougat, tip from https://stackoverflow.com/a/43193425/1967672
    override fun saveState(): Parcelable? {
        val bundle = super.saveState() as Bundle?
        bundle?.putParcelableArray("states", null)
        return bundle
    }
}
