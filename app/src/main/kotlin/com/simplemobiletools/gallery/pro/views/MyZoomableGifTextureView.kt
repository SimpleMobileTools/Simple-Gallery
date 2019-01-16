package com.simplemobiletools.gallery.pro.views

import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ImageView
import com.simplemobiletools.commons.extensions.beVisible
import com.simplemobiletools.gallery.pro.helpers.*
import pl.droidsonroids.gif.GifTextureView

// allow horizontal swipes through the layout, else it can cause glitches at zoomed in images
class MyZoomableGifTextureView(context: Context, attrs: AttributeSet) : GifTextureView(context, attrs) {
    private var mSaveScale = 1f
    private var mRight = 0f
    private var mBottom = 0f
    private var mLastTouchX = 0f
    private var mLastTouchY = 0f
    private var mTouchDownTime = 0L
    private var mTouchDownX = 0f
    private var mTouchDownY = 0f
    private var mGifWidth = 0f
    private var mGifHeight = 0f
    private var mScreenWidth = 0f
    private var mScreenHeight = 0f
    private var mLastFocusX = 0f
    private var mLastFocusY = 0f
    private var mCurrZoomMode = ZOOM_MODE_NONE

    private var mScaleDetector: ScaleGestureDetector? = null
    private var mMatrices = FloatArray(9)
    private val mMatrix = Matrix()
    private var mOrigRect = RectF()
    private var mWantedRect = RectF()
    private var mCurrentViewport = RectF()

    init {
        mScaleDetector = ScaleGestureDetector(context, ScaleListener())
    }

    fun setupSizes(gifWidth: Int, gifHeight: Int, screenWidth: Int, screenHeight: Int) {
        // if we don't know the gifs' resolution, just display it and disable zooming
        if (gifWidth == 0 || gifHeight == 0) {
            scaleType = ImageView.ScaleType.FIT_CENTER
            mScaleDetector = null
            beVisible()
            return
        }

        mGifWidth = gifWidth.toFloat()
        mGifHeight = gifHeight.toFloat()
        mScreenWidth = screenWidth.toFloat()
        mScreenHeight = screenHeight.toFloat()

        // we basically want scaleType fitCenter, but we have to use matrices to reach that
        mOrigRect = RectF(0f, 0f, mGifWidth, mGifHeight)
        mWantedRect = RectF(0f, 0f, mScreenWidth, mScreenHeight)
        mMatrix.setRectToRect(mOrigRect, mWantedRect, Matrix.ScaleToFit.CENTER)
        mMatrix.getValues(mMatrices)

        val left = mMatrices[Matrix.MTRANS_X]
        val top = mMatrices[Matrix.MTRANS_Y]
        val right = mScreenWidth - left
        val bottom = mScreenHeight - top
        mCurrentViewport.set(left, top, right, bottom)

        setTransform(mMatrix)
        invalidate()
        beVisible()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
//        mScaleDetector?.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mTouchDownTime = System.currentTimeMillis()
                mCurrZoomMode = ZOOM_MODE_DRAG
                mLastTouchX = event.x
                mLastTouchY = event.y

                mTouchDownX = event.x
                mTouchDownY = event.y
            }
            MotionEvent.ACTION_UP -> {
                mCurrZoomMode = ZOOM_MODE_NONE
                if (System.currentTimeMillis() - mTouchDownTime < CLICK_MAX_DURATION) {
                    performClick()
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                mLastTouchX = event.x
                mLastTouchY = event.y
                mCurrZoomMode = ZOOM_MODE_ZOOM
            }
            MotionEvent.ACTION_MOVE -> {
                if (mCurrZoomMode == ZOOM_MODE_ZOOM || mCurrZoomMode == ZOOM_MODE_DRAG && mSaveScale > MIN_VIDEO_ZOOM_SCALE) {
                    var diffX = event.x - mLastTouchX
                    var diffY = event.y - mLastTouchY

                    // horizontal bounds
                    if (mCurrentViewport.left + diffX < 0) {
                        diffX = -mCurrentViewport.left
                    } else if (mCurrentViewport.right + diffX > mScreenWidth) {
                        diffX = mScreenWidth - mCurrentViewport.right
                    }

                    // vertical bounds
                    if (mCurrentViewport.top + diffY < 0) {
                        diffY = -mCurrentViewport.top
                    } else if (mCurrentViewport.bottom + diffY > mScreenHeight) {
                        diffY = mScreenHeight - mCurrentViewport.bottom
                    }

                    mCurrentViewport.left += diffX
                    mCurrentViewport.right += diffX
                    mCurrentViewport.top += diffY
                    mCurrentViewport.bottom += diffY

                    mMatrix.postTranslate(diffX, diffY)

                    mCurrentViewport.left = Math.max(mCurrentViewport.left, 0f)
                    mCurrentViewport.right = Math.min(mCurrentViewport.right, mScreenWidth)
                    mCurrentViewport.top = Math.max(mCurrentViewport.top, 0f)
                    mCurrentViewport.bottom = Math.min(mCurrentViewport.bottom, mScreenHeight)

                    mLastTouchX = event.x
                    mLastTouchY = event.y
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                mCurrZoomMode = ZOOM_MODE_NONE
            }
        }

        setTransform(mMatrix)
        invalidate()
        return true
    }

    // taken from https://github.com/Manuiq/ZoomableTextureView
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            mCurrZoomMode = ZOOM_MODE_ZOOM
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (width <= 0 || height <= 0) {
                return true
            }

            mLastFocusX = detector.focusX
            mLastFocusY = detector.focusY
            var scaleFactor = detector.scaleFactor
            val origScale = mSaveScale
            mSaveScale *= scaleFactor

            if (mSaveScale > MAX_VIDEO_ZOOM_SCALE) {
                mSaveScale = MAX_VIDEO_ZOOM_SCALE
                scaleFactor = MAX_VIDEO_ZOOM_SCALE / origScale
            } else if (mSaveScale < MIN_VIDEO_ZOOM_SCALE) {
                mSaveScale = MIN_VIDEO_ZOOM_SCALE
                scaleFactor = MIN_VIDEO_ZOOM_SCALE / origScale
            }

            mRight = width * mSaveScale - width
            mBottom = height * mSaveScale - height
            mMatrix.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
            if (scaleFactor < 1) {
                mMatrix.getValues(mMatrices)
                val y = mMatrices[Matrix.MTRANS_Y]
                if (scaleFactor < 1) {
                    if (y < -mBottom) {
                        mMatrix.postTranslate(0f, -(y + mBottom))
                    } else if (y > 0) {
                        mMatrix.postTranslate(0f, -y)
                    }
                }
            }
            return true
        }
    }
}
