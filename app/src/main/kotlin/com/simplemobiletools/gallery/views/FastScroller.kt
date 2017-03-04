package com.simplemobiletools.gallery.views

import android.content.Context
import android.graphics.PorterDuff
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.extensions.config
import kotlinx.android.synthetic.main.fastscroller.view.*

// based on https://blog.stylingandroid.com/recyclerview-fastscroll-part-1
class FastScroller : LinearLayout {
    private val handle: View
    private var currHeight = 0

    private val HANDLE_HIDE_DELAY = 1000L
    private var recyclerView: RecyclerView? = null
    private var swipeRefreshLayout: SwipeRefreshLayout? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    fun setViews(recyclerView: RecyclerView, swipeRefreshLayout: SwipeRefreshLayout) {
        this.recyclerView = recyclerView
        this.swipeRefreshLayout = swipeRefreshLayout
        handle.background.setColorFilter(context.config.primaryColor, PorterDuff.Mode.SRC_IN)

        recyclerView.setOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                updateHandlePosition()
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    showHandle()
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    hideHandle()
                }
            }
        })
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        currHeight = h
        updateHandlePosition()
    }

    private fun updateHandlePosition() {
        if (handle.isSelected || recyclerView == null)
            return

        val verticalScrollOffset = recyclerView!!.computeVerticalScrollOffset()
        val verticalScrollRange = recyclerView!!.computeVerticalScrollRange()
        val proportion = verticalScrollOffset.toFloat() / (verticalScrollRange.toFloat() - currHeight)
        setPosition(currHeight * proportion)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                showHandle()
                handle.isSelected = true
                swipeRefreshLayout?.isEnabled = false
                true
            }
            MotionEvent.ACTION_MOVE -> {
                setPosition(event.y)
                setRecyclerViewPosition(event.y)
                true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                hideHandle()
                handle.isSelected = false
                swipeRefreshLayout?.isEnabled = true
                true
            }
            else -> super.onTouchEvent(event)
        }
    }

    private fun setRecyclerViewPosition(y: Float) {
        if (recyclerView != null) {
            val itemCount = recyclerView!!.adapter.itemCount
            val proportion = y / currHeight
            val targetPos = getValueInRange(0f, (itemCount - 1).toFloat(), proportion * itemCount).toInt()
            (recyclerView!!.layoutManager as GridLayoutManager).scrollToPositionWithOffset(targetPos, 0)
        }
    }

    init {
        orientation = LinearLayout.HORIZONTAL
        clipChildren = false
        val inflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.fastscroller, this)
        handle = fastscroller_handle
    }

    private fun showHandle() {
        handle.animate().alpha(1f).startDelay = 0L
    }

    private fun hideHandle() {
        handle.animate().alpha(0f).startDelay = HANDLE_HIDE_DELAY
    }

    private fun setPosition(y: Float) {
        val position = y / currHeight
        val handleHeight = handle.height
        handle.y = getValueInRange(0f, (currHeight - handleHeight).toFloat(), (currHeight - handleHeight) * position)
    }

    private fun getValueInRange(min: Float, max: Float, value: Float) = Math.min(Math.max(min, value), max)
}
