package com.simplemobiletools.commons.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.LinearLayout
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.extensions.isRTLLayout
import com.simplemobiletools.commons.extensions.onGlobalLayout
import com.simplemobiletools.commons.interfaces.LineColorPickerListener
import java.util.*

class LineColorPicker(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    private var colorsCount = 0
    private var pickerWidth = 0
    private var stripeWidth = 0
    private var unselectedMargin = 0
    private var lastColorIndex = -1
    private var wasInit = false
    private var colors = ArrayList<Int>()

    var listener: LineColorPickerListener? = null

    init {
        unselectedMargin = context.resources.getDimension(R.dimen.line_color_picker_margin).toInt()
        onGlobalLayout {
            if (pickerWidth == 0) {
                pickerWidth = width

                if (colorsCount != 0)
                    stripeWidth = width / colorsCount
            }

            if (!wasInit) {
                wasInit = true
                initColorPicker()
                updateItemMargin(lastColorIndex, false)
            }
        }
        orientation = LinearLayout.HORIZONTAL

        setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {
                    if (pickerWidth != 0 && stripeWidth != 0) {
                        touchAt(motionEvent.x.toInt())
                    }
                }
            }
            true
        }
    }

    fun updateColors(colors: ArrayList<Int>, selectColorIndex: Int = -1) {
        this.colors = colors
        colorsCount = colors.size
        if (pickerWidth != 0) {
            stripeWidth = pickerWidth / colorsCount
        }

        if (selectColorIndex != -1) {
            lastColorIndex = selectColorIndex
        }

        initColorPicker()
        updateItemMargin(lastColorIndex, false)
    }

    // do not remove ": Int", it causes "NoSuchMethodError" for some reason
    fun getCurrentColor(): Int = colors[lastColorIndex]

    private fun initColorPicker() {
        removeAllViews()
        val inflater = LayoutInflater.from(context)
        colors.forEach {
            inflater.inflate(R.layout.empty_image_view, this, false).apply {
                setBackgroundColor(it)
                addView(this)
            }
        }
    }

    private fun touchAt(touchX: Int) {
        var colorIndex = touchX / stripeWidth
        if (context.isRTLLayout) {
            colorIndex = colors.size - colorIndex - 1
        }
        val index = Math.max(0, Math.min(colorIndex, colorsCount - 1))
        if (lastColorIndex != index) {
            updateItemMargin(lastColorIndex, true)
            lastColorIndex = index
            updateItemMargin(index, false)
            listener?.colorChanged(index, colors[index])
        }
    }

    private fun updateItemMargin(index: Int, addMargin: Boolean) {
        getChildAt(index)?.apply {
            (layoutParams as LinearLayout.LayoutParams).apply {
                topMargin = if (addMargin) unselectedMargin else 0
                bottomMargin = if (addMargin) unselectedMargin else 0
            }
            requestLayout()
        }
    }
}
