package com.simplemobiletools.gallery.pro.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.duolingo.open.rtlviewpager.RtlViewPager


class CustomViewPager : RtlViewPager {

    private var enableSwipe = true

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
//        return try {
//            super.onInterceptTouchEvent(ev)
//        } catch (ignored: Exception) {
//            false
//        }

        if (enableSwipe) {
            return super.onInterceptTouchEvent(ev)
        }
        return false;
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
//        return try {
//            super.onTouchEvent(ev)
//        } catch (ignored: Exception) {
//            false
//        }

        if (enableSwipe) {
            return super.onTouchEvent(ev)
        }
        return false;
    }

    fun setEnableSwipe() {
        enableSwipe = !enableSwipe
    }
}
