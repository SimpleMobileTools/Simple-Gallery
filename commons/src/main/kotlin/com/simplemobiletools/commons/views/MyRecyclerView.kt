package com.simplemobiletools.commons.views

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.interfaces.RecyclerScrollCallback

// drag selection is based on https://github.com/afollestad/drag-select-recyclerview
open class MyRecyclerView : RecyclerView {
    private val AUTO_SCROLL_DELAY = 25L
    private var isZoomEnabled = false
    private var isDragSelectionEnabled = false
    private var zoomListener: MyZoomListener? = null
    private var dragListener: MyDragListener? = null
    private var autoScrollHandler = Handler()

    private var scaleDetector: ScaleGestureDetector

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

    private var currScaleFactor = 1.0f
    private var lastUp = 0L    // allow only pinch zoom, not double tap

    // things related to parallax scrolling (for now only in the music player)
    // cut from https://github.com/ksoichiro/Android-ObservableScrollView
    var recyclerScrollCallback: RecyclerScrollCallback? = null
    private var mPrevFirstVisiblePosition = 0
    private var mPrevScrolledChildrenHeight = 0
    private var mPrevFirstVisibleChildHeight = -1
    private var mScrollY = 0

    // variables used for fetching additional items at scrolling to the bottom/top
    var endlessScrollListener: EndlessScrollListener? = null
    private var totalItemCount = 0
    private var lastMaxItemIndex = 0
    private var linearLayoutManager: LinearLayoutManager? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    init {
        hotspotHeight = context.resources.getDimensionPixelSize(R.dimen.dragselect_hotspot_height)

        if (layoutManager is LinearLayoutManager) {
            linearLayoutManager = layoutManager as LinearLayoutManager
        }

        val gestureListener = object : MyGestureListener {
            override fun getLastUp() = lastUp

            override fun getScaleFactor() = currScaleFactor

            override fun setScaleFactor(value: Float) {
                currScaleFactor = value
            }

            override fun getZoomListener() = zoomListener
        }

        scaleDetector = ScaleGestureDetector(context, GestureListener(gestureListener))
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

    fun resetItemCount() {
        totalItemCount = 0
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (!dragSelectActive) {
            try {
                super.dispatchTouchEvent(ev)
            } catch (ignored: Exception) {
            }
        }

        when (ev.action) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragSelectActive = false
                inTopHotspot = false
                inBottomHotspot = false
                autoScrollHandler.removeCallbacks(autoScrollRunnable)
                currScaleFactor = 1.0f
                lastUp = System.currentTimeMillis()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (dragSelectActive) {
                    val itemPosition = getItemPosition(ev)
                    if (hotspotHeight > -1) {
                        if (ev.y in hotspotTopBoundStart.toFloat()..hotspotTopBoundEnd.toFloat()) {
                            inBottomHotspot = false
                            if (!inTopHotspot) {
                                inTopHotspot = true
                                autoScrollHandler.removeCallbacks(autoScrollRunnable)
                                autoScrollHandler.postDelayed(autoScrollRunnable, AUTO_SCROLL_DELAY)
                            }

                            val simulatedFactor = (hotspotTopBoundEnd - hotspotTopBoundStart).toFloat()
                            val simulatedY = ev.y - hotspotTopBoundStart
                            autoScrollVelocity = (simulatedFactor - simulatedY).toInt() / 2
                        } else if (ev.y in hotspotBottomBoundStart.toFloat()..hotspotBottomBoundEnd.toFloat()) {
                            inTopHotspot = false
                            if (!inBottomHotspot) {
                                inBottomHotspot = true
                                autoScrollHandler.removeCallbacks(autoScrollRunnable)
                                autoScrollHandler.postDelayed(autoScrollRunnable, AUTO_SCROLL_DELAY)
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

                        dragListener?.selectRange(initialSelection, lastDraggedIndex, minReached, maxReached)

                        if (initialSelection == lastDraggedIndex) {
                            minReached = lastDraggedIndex
                            maxReached = lastDraggedIndex
                        }
                    }

                    return true
                }
            }
        }

        return if (isZoomEnabled) {
            scaleDetector.onTouchEvent(ev)
        } else {
            true
        }
    }

    fun setupDragListener(dragListener: MyDragListener?) {
        isDragSelectionEnabled = dragListener != null
        this.dragListener = dragListener
    }

    fun setupZoomListener(zoomListener: MyZoomListener?) {
        isZoomEnabled = zoomListener != null
        this.zoomListener = zoomListener
    }

    fun setDragSelectActive(initialSelection: Int) {
        if (dragSelectActive || !isDragSelectionEnabled)
            return

        lastDraggedIndex = -1
        minReached = -1
        maxReached = -1
        this.initialSelection = initialSelection
        dragSelectActive = true
        dragListener?.selectItem(initialSelection)
    }

    private fun getItemPosition(e: MotionEvent): Int {
        val v = findChildViewUnder(e.x, e.y) ?: return RecyclerView.NO_POSITION

        if (v.tag == null || v.tag !is RecyclerView.ViewHolder) {
            throw IllegalStateException("Make sure your adapter makes a call to super.onBindViewHolder(), and doesn't override itemView tags.")
        }

        val holder = v.tag as RecyclerView.ViewHolder
        return holder.adapterPosition
    }

    override fun onScrollStateChanged(state: Int) {
        super.onScrollStateChanged(state)
        if (endlessScrollListener != null) {
            if (totalItemCount == 0) {
                totalItemCount = adapter?.itemCount ?: return
            }

            if (state == SCROLL_STATE_IDLE) {
                val lastVisiblePosition = linearLayoutManager?.findLastVisibleItemPosition() ?: 0
                if (lastVisiblePosition != lastMaxItemIndex && lastVisiblePosition == totalItemCount - 1) {
                    lastMaxItemIndex = lastVisiblePosition
                    endlessScrollListener!!.updateBottom()
                }

                val firstVisiblePosition = linearLayoutManager?.findFirstVisibleItemPosition() ?: -1
                if (firstVisiblePosition == 0) {
                    endlessScrollListener!!.updateTop()
                }
            }
        }
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        if (recyclerScrollCallback != null) {
            if (childCount > 0) {
                val firstVisiblePosition = getChildAdapterPosition(getChildAt(0))
                val firstVisibleChild = getChildAt(0)
                if (firstVisibleChild != null) {
                    if (mPrevFirstVisiblePosition < firstVisiblePosition) {
                        mPrevScrolledChildrenHeight += mPrevFirstVisibleChildHeight
                    }

                    if (firstVisiblePosition == 0) {
                        mPrevFirstVisibleChildHeight = firstVisibleChild.height
                        mPrevScrolledChildrenHeight = 0
                    }

                    if (mPrevFirstVisibleChildHeight < 0) {
                        mPrevFirstVisibleChildHeight = 0
                    }

                    mScrollY = mPrevScrolledChildrenHeight - firstVisibleChild.top
                    recyclerScrollCallback?.onScrolled(mScrollY)
                }
            }
        }
    }

    class GestureListener(val gestureListener: MyGestureListener) : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        private val ZOOM_IN_THRESHOLD = -0.4f
        private val ZOOM_OUT_THRESHOLD = 0.15f

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            gestureListener.apply {
                if (System.currentTimeMillis() - getLastUp() < 1000)
                    return false

                val diff = getScaleFactor() - detector.scaleFactor
                if (diff < ZOOM_IN_THRESHOLD && getScaleFactor() == 1.0f) {
                    getZoomListener()?.zoomIn()
                    setScaleFactor(detector.scaleFactor)
                } else if (diff > ZOOM_OUT_THRESHOLD && getScaleFactor() == 1.0f) {
                    getZoomListener()?.zoomOut()
                    setScaleFactor(detector.scaleFactor)
                }
            }
            return false
        }
    }

    interface MyZoomListener {
        fun zoomOut()

        fun zoomIn()
    }

    interface MyDragListener {
        fun selectItem(position: Int)

        fun selectRange(initialSelection: Int, lastDraggedIndex: Int, minReached: Int, maxReached: Int)
    }

    interface MyGestureListener {
        fun getLastUp(): Long

        fun getScaleFactor(): Float

        fun setScaleFactor(value: Float)

        fun getZoomListener(): MyZoomListener?
    }

    interface EndlessScrollListener {
        fun updateTop()

        fun updateBottom()
    }
}
