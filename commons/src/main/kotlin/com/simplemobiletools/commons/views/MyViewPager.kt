package com.simplemobiletools.commons.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.duolingo.open.rtlviewpager.RtlViewPager

class MyViewPager : RtlViewPager {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return try {
            super.onInterceptTouchEvent(ev)
        } catch (ignored: Exception) {
            false
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        return try {
            super.onTouchEvent(ev)
        } catch (ignored: Exception) {
            false
        }
    }
}
