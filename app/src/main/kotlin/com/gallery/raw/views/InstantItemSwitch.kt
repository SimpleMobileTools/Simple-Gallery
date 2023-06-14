package com.gallery.raw.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.gallery.raw.helpers.CLICK_MAX_DISTANCE
import com.gallery.raw.helpers.CLICK_MAX_DURATION
import com.gallery.raw.helpers.DRAG_THRESHOLD

// handle only one finger clicks, pass other events to the parent view and ignore it when received again
class InstantItemSwitch(context: Context, attrs: AttributeSet) : RelativeLayout(context, attrs) {
    private var mTouchDownTime = 0L
    private var mTouchDownX = 0f
    private var mTouchDownY = 0f
    private var passTouches = false
    private var dragThreshold = DRAG_THRESHOLD * context.resources.displayMetrics.density

    var parentView: ViewGroup? = null

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (passTouches) {
            if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
                passTouches = false
            }
            return false
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (passTouches) {
            return false
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mTouchDownX = event.rawX
                mTouchDownY = event.rawY
                mTouchDownTime = System.currentTimeMillis()
            }
            MotionEvent.ACTION_UP -> {
                val diffX = mTouchDownX - event.rawX
                val diffY = mTouchDownY - event.rawY
                if (Math.abs(diffX) < CLICK_MAX_DISTANCE && Math.abs(diffY) < CLICK_MAX_DISTANCE && System.currentTimeMillis() - mTouchDownTime < CLICK_MAX_DURATION) {
                    performClick()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (passTouches) {
                    return false
                }

                val diffX = mTouchDownX - event.rawX
                val diffY = mTouchDownY - event.rawY
                if (Math.abs(diffX) > dragThreshold || Math.abs(diffY) > dragThreshold) {
                    if (!passTouches) {
                        event.action = MotionEvent.ACTION_DOWN
                        event.setLocation(event.rawX, event.rawY)
                        parentView?.dispatchTouchEvent(event)
                    }
                    passTouches = true
                    parentView?.dispatchTouchEvent(event)
                    return false
                }
            }
        }
        return true
    }
}
