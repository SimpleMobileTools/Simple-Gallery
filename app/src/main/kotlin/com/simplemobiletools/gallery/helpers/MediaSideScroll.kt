package com.simplemobiletools.gallery.helpers

import android.app.Activity
import android.media.AudioManager
import android.os.Handler
import android.provider.Settings
import android.view.MotionEvent
import android.widget.TextView
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.ViewPagerActivity
import com.simplemobiletools.gallery.extensions.audioManager

class MediaSideScroll(val activity: Activity, val slideInfoView: TextView, val callback: () -> Unit) {
    private val SLIDE_INFO_FADE_DELAY = 1000L
    private var mTouchDownX = 0f
    private var mTouchDownY = 0f
    private var mTouchDownTime = 0L
    private var mTouchDownVolume = 0
    private var mTouchDownBrightness = -1
    private var mTempBrightness = 0
    private var mLastTouchY = 0f

    private var mSlideInfoText = ""
    private var mSlideInfoFadeHandler = Handler()

    fun handleVolumeTouched(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mTouchDownX = event.x
                mTouchDownY = event.y
                mLastTouchY = event.y
                mTouchDownTime = System.currentTimeMillis()
                mTouchDownVolume = getCurrentVolume()
                mSlideInfoText = "${activity.getString(R.string.volume)}:\n"
            }
            MotionEvent.ACTION_MOVE -> {
                val diffX = mTouchDownX - event.x
                val diffY = mTouchDownY - event.y

                if (Math.abs(diffY) > DRAG_THRESHOLD && Math.abs(diffY) > Math.abs(diffX)) {
                    var percent = ((diffY / ViewPagerActivity.screenHeight) * 100).toInt() * 3
                    percent = Math.min(100, Math.max(-100, percent))

                    if ((percent == 100 && event.y > mLastTouchY) || (percent == -100 && event.y < mLastTouchY)) {
                        mTouchDownY = event.y
                        mTouchDownVolume = getCurrentVolume()
                    }

                    volumePercentChanged(percent)
                }
                mLastTouchY = event.y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val diffX = Math.abs(event.x - mTouchDownX)
                val diffY = Math.abs(event.y - mTouchDownY)
                if (System.currentTimeMillis() - mTouchDownTime < CLICK_MAX_DURATION && diffX < 20 && diffY < 20) {
                    callback()
                }
            }
        }
    }

    fun handleBrightnessTouched(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mTouchDownX = event.x
                mTouchDownY = event.y
                mLastTouchY = event.y
                mTouchDownTime = System.currentTimeMillis()
                mSlideInfoText = "${activity.getString(R.string.brightness)}:\n"
                if (mTouchDownBrightness == -1) {
                    mTouchDownBrightness = getCurrentBrightness()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val diffX = mTouchDownX - event.x
                val diffY = mTouchDownY - event.y

                if (Math.abs(diffY) > 20 && Math.abs(diffY) > Math.abs(diffX)) {
                    var percent = ((diffY / ViewPagerActivity.screenHeight) * 100).toInt() * 3
                    percent = Math.min(100, Math.max(-100, percent))

                    if ((percent == 100 && event.y > mLastTouchY) || (percent == -100 && event.y < mLastTouchY)) {
                        mTouchDownY = event.y
                        mTouchDownBrightness = mTempBrightness
                    }

                    brightnessPercentChanged(percent)
                }
                mLastTouchY = event.y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val diffX = Math.abs(event.x - mTouchDownX)
                val diffY = Math.abs(event.y - mTouchDownY)
                if (System.currentTimeMillis() - mTouchDownTime < CLICK_MAX_DURATION && diffX < 20 && diffY < 20) {
                    callback()
                }
                mTouchDownBrightness = mTempBrightness
            }
        }
    }

    private fun getCurrentVolume() = activity.audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

    private fun getCurrentBrightness(): Int {
        return try {
            Settings.System.getInt(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Settings.SettingNotFoundException) {
            70
        }
    }

    private fun volumePercentChanged(percent: Int) {
        val stream = AudioManager.STREAM_MUSIC
        val maxVolume = activity.audioManager.getStreamMaxVolume(stream)
        val percentPerPoint = 100 / maxVolume
        val addPoints = percent / percentPerPoint
        val newVolume = Math.min(maxVolume, Math.max(0, mTouchDownVolume + addPoints))
        activity.audioManager.setStreamVolume(stream, newVolume, 0)

        val absolutePercent = ((newVolume / maxVolume.toFloat()) * 100).toInt()
        slideInfoView.apply {
            text = "$mSlideInfoText$absolutePercent%"
            alpha = 1f
        }

        mSlideInfoFadeHandler.removeCallbacksAndMessages(null)
        mSlideInfoFadeHandler.postDelayed({
            slideInfoView.animate().alpha(0f)
        }, SLIDE_INFO_FADE_DELAY)
    }

    private fun brightnessPercentChanged(percent: Int) {
        val maxBrightness = 255f
        var newBrightness = (mTouchDownBrightness + 2.55 * percent).toFloat()
        newBrightness = Math.min(maxBrightness, Math.max(0f, newBrightness))
        mTempBrightness = newBrightness.toInt()

        val absolutePercent = ((newBrightness / maxBrightness) * 100).toInt()
        slideInfoView.apply {
            text = "$mSlideInfoText$absolutePercent%"
            alpha = 1f
        }

        val attributes = activity.window.attributes
        attributes.screenBrightness = absolutePercent / 100f
        activity.window.attributes = attributes

        mSlideInfoFadeHandler.removeCallbacksAndMessages(null)
        mSlideInfoFadeHandler.postDelayed({
            slideInfoView.animate().alpha(0f)
        }, SLIDE_INFO_FADE_DELAY)
    }
}
