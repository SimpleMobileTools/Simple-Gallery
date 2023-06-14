package com.simplemobiletools.commons.extensions

import androidx.viewpager.widget.ViewPager

fun ViewPager.onPageChangeListener(pageChangedAction: (newPosition: Int) -> Unit) =
        addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                pageChangedAction(position)
            }
        })
