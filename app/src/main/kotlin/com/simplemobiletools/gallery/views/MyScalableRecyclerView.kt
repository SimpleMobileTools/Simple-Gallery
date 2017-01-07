package com.simplemobiletools.gallery.views

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector

class MyScalableRecyclerView : RecyclerView {
    var mScaleDetector: ScaleGestureDetector

    companion object {
        var mListener: ZoomListener? = null
        var mCurrScaleFactor = 1.0f
        var mLastUp = 0L    // allow only pinch zoom, not double tap
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    init {
        mScaleDetector = ScaleGestureDetector(context, GestureListener())
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        super.dispatchTouchEvent(ev)
        if (ev.action == MotionEvent.ACTION_UP) {
            mCurrScaleFactor = 1.0f
            mLastUp = System.currentTimeMillis()
        }

        return mScaleDetector.onTouchEvent(ev)
    }

    class GestureListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        val ZOOM_IN_THRESHOLD = -0.6f
        val ZOOM_OUT_THRESHOLD = 0.25f

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (System.currentTimeMillis() - mLastUp < 1000)
                return false

            val diff = mCurrScaleFactor - detector.scaleFactor
            if (diff < ZOOM_IN_THRESHOLD && mCurrScaleFactor == 1.0f) {
                mListener?.zoomIn()
                mCurrScaleFactor = detector.scaleFactor
            } else if (diff > ZOOM_OUT_THRESHOLD && mCurrScaleFactor == 1.0f) {
                mListener?.zoomOut()
                mCurrScaleFactor = detector.scaleFactor
            }
            return false
        }
    }

    interface ZoomListener {
        fun zoomOut()

        fun zoomIn()
    }
}
