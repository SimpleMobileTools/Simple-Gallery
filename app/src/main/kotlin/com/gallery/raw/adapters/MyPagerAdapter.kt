package com.gallery.raw.adapters

import android.os.Bundle
import android.os.Parcelable
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.PagerAdapter
import com.gallery.raw.activities.ViewPagerActivity
import com.gallery.raw.fragments.PhotoFragment
import com.gallery.raw.fragments.VideoFragment
import com.gallery.raw.fragments.ViewPagerFragment
import com.gallery.raw.helpers.MEDIUM
import com.gallery.raw.helpers.SHOULD_INIT_FRAGMENT
import com.gallery.raw.models.Medium

class MyPagerAdapter(val activity: ViewPagerActivity, fm: FragmentManager, val media: MutableList<Medium>) : FragmentStatePagerAdapter(fm) {
    private val fragments = HashMap<Int, ViewPagerFragment>()
    var shouldInitFragment = true

    override fun getCount() = media.size

    override fun getItem(position: Int): Fragment {
        val medium = media[position]
        val bundle = Bundle()
        bundle.putSerializable(MEDIUM, medium)
        bundle.putBoolean(SHOULD_INIT_FRAGMENT, shouldInitFragment)
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
