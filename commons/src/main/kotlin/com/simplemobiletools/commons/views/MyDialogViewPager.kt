package com.simplemobiletools.commons.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.viewpager.widget.ViewPager

class MyDialogViewPager : ViewPager {
    var allowSwiping = true

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    // disable manual swiping of viewpager at the dialog by swiping over the pattern
    override fun onInterceptTouchEvent(ev: MotionEvent) = false

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!allowSwiping)
            return false

        try {
            return super.onTouchEvent(ev)
        } catch (ignored: Exception) {
        }

        return false
    }

    // https://stackoverflow.com/a/20784791/1967672
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var height = 0
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.measure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            val h = child.measuredHeight
            if (h > height) height = h
        }

        val newHeightMeasureSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        super.onMeasure(widthMeasureSpec, newHeightMeasureSpec)
    }
}
