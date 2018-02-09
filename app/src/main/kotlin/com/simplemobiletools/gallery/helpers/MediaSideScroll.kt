package com.simplemobiletools.gallery.helpers

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.provider.Settings
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.ViewPagerActivity
import com.simplemobiletools.gallery.extensions.audioManager

class MediaSideScroll(context: Context, attrs: AttributeSet) : RelativeLayout(context, attrs) {
    private val SLIDE_INFO_FADE_DELAY = 1000L
    private var mTouchDownX = 0f
    private var mTouchDownY = 0f
    private var mTouchDownTime = 0L
    private var mTouchDownValue = -1
    private var mTempBrightness = 0
    private var mLastTouchY = 0f
    private var mIsBrightnessScroll = false

    private var mSlideInfoText = ""
    private var mSlideInfoFadeHandler = Handler()
    private var mParentView: ViewGroup? = null

    private lateinit var activity: Activity
    private lateinit var slideInfoView: TextView
    private lateinit var callback: () -> Unit

    fun initialize(activity: Activity, slideInfoView: TextView, isBrightness: Boolean, parentView: ViewGroup?, callback: () -> Unit) {
        this.activity = activity
        this.slideInfoView = slideInfoView
        this.callback = callback
        mParentView = parentView
        mIsBrightnessScroll = isBrightness
        mSlideInfoText = activity.getString(if (isBrightness) R.string.brightness else R.string.volume)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mTouchDownX = event.x
                mTouchDownY = event.y
                mLastTouchY = event.y
                mTouchDownTime = System.currentTimeMillis()
                if (mIsBrightnessScroll) {
                    if (mTouchDownValue == -1) {
                        mTouchDownValue = getCurrentBrightness()
                    }
                } else {
                    mTouchDownValue = getCurrentVolume()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val diffX = mTouchDownX - event.x
                val diffY = mTouchDownY - event.y

                if (Math.abs(diffY) > DRAG_THRESHOLD && Math.abs(diffY) > Math.abs(diffX)) {
                    var percent = ((diffY / ViewPagerActivity.screenHeight) * 100).toInt() * 3
                    percent = Math.min(100, Math.max(-100, percent))

                    if ((percent == 100 && event.y > mLastTouchY) || (percent == -100 && event.y < mLastTouchY)) {
                        mTouchDownY = event.y
                        mTouchDownValue = if (mIsBrightnessScroll) mTempBrightness else getCurrentVolume()
                    }

                    percentChanged(percent)
                }
                mLastTouchY = event.y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (System.currentTimeMillis() - mTouchDownTime < CLICK_MAX_DURATION) {
                    callback()
                }

                if (mIsBrightnessScroll) {
                    mTouchDownValue = mTempBrightness
                }
            }
        }
        return true
    }

    private fun getCurrentVolume() = activity.audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

    private fun getCurrentBrightness(): Int {
        return try {
            Settings.System.getInt(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Settings.SettingNotFoundException) {
            70
        }
    }

    private fun percentChanged(percent: Int) {
        if (mIsBrightnessScroll) {
            brightnessPercentChanged(percent)
        } else {
            volumePercentChanged(percent)
        }
    }

    private fun volumePercentChanged(percent: Int) {
        val stream = AudioManager.STREAM_MUSIC
        val maxVolume = activity.audioManager.getStreamMaxVolume(stream)
        val percentPerPoint = 100 / maxVolume
        val addPoints = percent / percentPerPoint
        val newVolume = Math.min(maxVolume, Math.max(0, mTouchDownValue + addPoints))
        activity.audioManager.setStreamVolume(stream, newVolume, 0)

        val absolutePercent = ((newVolume / maxVolume.toFloat()) * 100).toInt()
        showValue(absolutePercent)

        mSlideInfoFadeHandler.removeCallbacksAndMessages(null)
        mSlideInfoFadeHandler.postDelayed({
            slideInfoView.animate().alpha(0f)
        }, SLIDE_INFO_FADE_DELAY)
    }

    private fun brightnessPercentChanged(percent: Int) {
        val maxBrightness = 255f
        var newBrightness = (mTouchDownValue + 2.55 * percent).toFloat()
        newBrightness = Math.min(maxBrightness, Math.max(0f, newBrightness))
        mTempBrightness = newBrightness.toInt()

        val absolutePercent = ((newBrightness / maxBrightness) * 100).toInt()
        showValue(absolutePercent)

        val attributes = activity.window.attributes
        attributes.screenBrightness = absolutePercent / 100f
        activity.window.attributes = attributes

        mSlideInfoFadeHandler.removeCallbacksAndMessages(null)
        mSlideInfoFadeHandler.postDelayed({
            slideInfoView.animate().alpha(0f)
        }, SLIDE_INFO_FADE_DELAY)
    }

    private fun showValue(percent: Int) {
        slideInfoView.apply {
            text = "$mSlideInfoText:\n$percent%"
            alpha = 1f
        }
    }
}
