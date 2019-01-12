package com.simplemobiletools.gallery.pro.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.simplemobiletools.gallery.pro.extensions.config
import com.simplemobiletools.gallery.pro.models.MyPath
import com.simplemobiletools.gallery.pro.models.PaintOptions
import java.util.*

class EditorDrawCanvas(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private var mCurX = 0f
    private var mCurY = 0f
    private var mStartX = 0f
    private var mStartY = 0f
    private var mColor = 0
    private var mWasMultitouch = false

    private var mPaths = LinkedHashMap<MyPath, PaintOptions>()
    private var mPaint = Paint()
    private var mPath = MyPath()
    private var mPaintOptions = PaintOptions()

    private var backgroundBitmap: Bitmap? = null

    init {
        mColor = context.config.primaryColor
        mPaint.apply {
            color = mColor
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 40f
            isAntiAlias = true
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()

        if (backgroundBitmap != null) {
            val left = (width - backgroundBitmap!!.width) / 2
            val top = (height - backgroundBitmap!!.height) / 2
            canvas.drawBitmap(backgroundBitmap!!, left.toFloat(), top.toFloat(), null)
        }

        for ((key, value) in mPaths) {
            changePaint(value)
            canvas.drawPath(key, mPaint)
        }

        changePaint(mPaintOptions)
        canvas.drawPath(mPath, mPaint)

        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                mWasMultitouch = false
                mStartX = x
                mStartY = y
                actionDown(x, y)
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 1 && !mWasMultitouch) {
                    actionMove(x, y)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> actionUp()
            MotionEvent.ACTION_POINTER_DOWN -> mWasMultitouch = true
        }

        invalidate()
        return true
    }

    private fun actionDown(x: Float, y: Float) {
        mPath.reset()
        mPath.moveTo(x, y)
        mCurX = x
        mCurY = y
    }

    private fun actionMove(x: Float, y: Float) {
        mPath.quadTo(mCurX, mCurY, (x + mCurX) / 2, (y + mCurY) / 2)
        mCurX = x
        mCurY = y
    }

    private fun actionUp() {
        if (!mWasMultitouch) {
            mPath.lineTo(mCurX, mCurY)

            // draw a dot on click
            if (mStartX == mCurX && mStartY == mCurY) {
                mPath.lineTo(mCurX, mCurY + 2)
                mPath.lineTo(mCurX + 1, mCurY + 2)
                mPath.lineTo(mCurX + 1, mCurY)
            }
        }

        mPaths[mPath] = mPaintOptions
        mPath = MyPath()
        mPaintOptions = PaintOptions(mPaintOptions.color, mPaintOptions.strokeWidth, mPaintOptions.isEraser)
    }

    private fun changePaint(paintOptions: PaintOptions) {
        mPaint.color = paintOptions.color
        mPaint.strokeWidth = paintOptions.strokeWidth
    }

    fun updateColor(newColor: Int) {
        mPaintOptions.color = newColor
    }

    fun updateBackgroundBitmap(bitmap: Bitmap) {
        backgroundBitmap = bitmap
        invalidate()
    }
}
