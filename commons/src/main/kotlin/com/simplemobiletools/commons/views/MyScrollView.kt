package com.simplemobiletools.commons.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ScrollView

class MyScrollView : ScrollView {
    var isScrollable = true

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        return when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isScrollable) {
                    super.onTouchEvent(ev)
                } else {
                    isScrollable
                }
            }
            else -> super.onTouchEvent(ev)
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return if (!isScrollable) {
            false
        } else {
            super.onInterceptTouchEvent(ev)
        }
    }
}
