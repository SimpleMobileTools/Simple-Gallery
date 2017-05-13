package com.simplemobiletools.gallery.views

import android.content.Context
import android.os.Handler
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.simplemobiletools.gallery.R

// drag selection is based on https://github.com/afollestad/drag-select-recyclerview
class MyScalableRecyclerView : RecyclerView {
    private val AUTO_SCROLL_DELAY = 25L

    private var mScaleDetector: ScaleGestureDetector

    private var dragSelectActive = false
    private var lastDraggedIndex = -1
    private var minReached = 0
    private var maxReached = 0
    private var initialSelection = 0

    private var hotspotHeight = 0
    private var hotspotOffsetTop = 0
    private var hotspotOffsetBottom = 0

    private var hotspotTopBoundStart = 0
    private var hotspotTopBoundEnd = 0
    private var hotspotBottomBoundStart = 0
    private var hotspotBottomBoundEnd = 0
    private var autoScrollVelocity = 0

    private var inTopHotspot = false
    private var inBottomHotspot = false

    companion object {
        var mListener: MyScalableRecyclerViewListener? = null
        var mCurrScaleFactor = 1.0f
        var mLastUp = 0L    // allow only pinch zoom, not double tap
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    init {
        hotspotHeight = context.resources.getDimensionPixelSize(R.dimen.dragselect_hotspot_height)
        mScaleDetector = ScaleGestureDetector(context, GestureListener())
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        super.onMeasure(widthSpec, heightSpec)
        if (hotspotHeight > -1) {
            hotspotTopBoundStart = hotspotOffsetTop
            hotspotTopBoundEnd = hotspotOffsetTop + hotspotHeight
            hotspotBottomBoundStart = measuredHeight - hotspotHeight - hotspotOffsetBottom
            hotspotBottomBoundEnd = measuredHeight - hotspotOffsetBottom
        }
    }

    private var autoScrollHandler = Handler()
    private val autoScrollRunnable = object : Runnable {
        override fun run() {
            if (inTopHotspot) {
                scrollBy(0, -autoScrollVelocity)
                autoScrollHandler.postDelayed(this, AUTO_SCROLL_DELAY)
            } else if (inBottomHotspot) {
                scrollBy(0, autoScrollVelocity)
                autoScrollHandler.postDelayed(this, AUTO_SCROLL_DELAY)
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (!dragSelectActive)
            super.dispatchTouchEvent(ev)

        when (ev.action) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragSelectActive = false
                mCurrScaleFactor = 1.0f
                mLastUp = System.currentTimeMillis()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (dragSelectActive) {
                    val itemPosition = getItemPosition(ev)
                    if (hotspotHeight > -1) {
                        if (ev.y in hotspotTopBoundStart..hotspotTopBoundEnd) {
                            inBottomHotspot = false
                            if (!inTopHotspot) {
                                inTopHotspot = true
                                autoScrollHandler.removeCallbacks(autoScrollRunnable)
                                autoScrollHandler.postDelayed(autoScrollRunnable, AUTO_SCROLL_DELAY.toLong())
                            }

                            val simulatedFactor = (hotspotTopBoundEnd - hotspotTopBoundStart).toFloat()
                            val simulatedY = ev.y - hotspotTopBoundStart
                            autoScrollVelocity = (simulatedFactor - simulatedY).toInt() / 2
                        } else if (ev.y in hotspotBottomBoundStart..hotspotBottomBoundEnd) {
                            inTopHotspot = false
                            if (!inBottomHotspot) {
                                inBottomHotspot = true
                                autoScrollHandler.removeCallbacks(autoScrollRunnable)
                                autoScrollHandler.postDelayed(autoScrollRunnable, AUTO_SCROLL_DELAY.toLong())
                            }

                            val simulatedY = ev.y + hotspotBottomBoundEnd
                            val simulatedFactor = (hotspotBottomBoundStart + hotspotBottomBoundEnd).toFloat()
                            autoScrollVelocity = (simulatedY - simulatedFactor).toInt() / 2
                        } else if (inTopHotspot || inBottomHotspot) {
                            autoScrollHandler.removeCallbacks(autoScrollRunnable)
                            inTopHotspot = false
                            inBottomHotspot = false
                        }
                    }

                    if (itemPosition != RecyclerView.NO_POSITION && lastDraggedIndex != itemPosition) {
                        lastDraggedIndex = itemPosition
                        if (minReached == -1) {
                            minReached = lastDraggedIndex
                        }

                        if (maxReached == -1) {
                            maxReached = lastDraggedIndex
                        }

                        if (lastDraggedIndex > maxReached) {
                            maxReached = lastDraggedIndex
                        }

                        if (lastDraggedIndex < minReached) {
                            minReached = lastDraggedIndex
                        }

                        mListener?.selectRange(initialSelection, lastDraggedIndex, minReached, maxReached)

                        if (initialSelection == lastDraggedIndex) {
                            minReached = lastDraggedIndex
                            maxReached = lastDraggedIndex
                        }
                    }

                    return true
                }
            }
        }
        return mScaleDetector.onTouchEvent(ev)
    }

    fun setDragSelectActive(initialSelection: Int) {
        if (dragSelectActive)
            return

        lastDraggedIndex = -1
        minReached = -1
        maxReached = -1
        this.initialSelection = initialSelection
        dragSelectActive = true
        mListener?.selectItem(initialSelection)
    }

    private fun getItemPosition(e: MotionEvent): Int {
        val v = findChildViewUnder(e.x, e.y) ?: return RecyclerView.NO_POSITION

        if (v.tag == null || v.tag !is RecyclerView.ViewHolder) {
            throw IllegalStateException("Make sure your adapter makes a call to super.onBindViewHolder(), and doesn't override itemView tags.")
        }

        val holder = v.tag as RecyclerView.ViewHolder
        return holder.adapterPosition
    }


    class GestureListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        private val ZOOM_IN_THRESHOLD = -0.4f
        private val ZOOM_OUT_THRESHOLD = 0.15f

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

    interface MyScalableRecyclerViewListener {
        fun zoomOut()

        fun zoomIn()

        fun selectItem(position: Int)

        fun selectRange(initialSelection: Int, lastDraggedIndex: Int, minReached: Int, maxReached: Int)
    }
}
