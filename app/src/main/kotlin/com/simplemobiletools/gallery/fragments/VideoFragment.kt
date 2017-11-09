package com.simplemobiletools.gallery.fragments

import android.content.res.Configuration
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import android.widget.TextView
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.VideoActivity
import com.simplemobiletools.gallery.activities.ViewPagerActivity
import com.simplemobiletools.gallery.extensions.*
import com.simplemobiletools.gallery.helpers.MEDIUM
import com.simplemobiletools.gallery.models.Medium
import kotlinx.android.synthetic.main.pager_video_item.view.*
import java.io.IOException

class VideoFragment : ViewPagerFragment(), SurfaceHolder.Callback, SeekBar.OnSeekBarChangeListener {
    private val CLICK_MAX_DURATION = 150
    private val SLIDE_INFO_FADE_DELAY = 1000L
    private val PROGRESS = "progress"

    private var mMediaPlayer: MediaPlayer? = null
    private var mSurfaceView: SurfaceView? = null
    private var mSurfaceHolder: SurfaceHolder? = null
    private var mCurrTimeView: TextView? = null
    private var mTimerHandler: Handler? = null
    private var mSeekBar: SeekBar? = null

    private var mIsPlaying = false
    private var mIsDragged = false
    private var mIsFullscreen = false
    private var mIsFragmentVisible = false
    private var mPlayOnPrepare = false
    private var mStoredShowExtendedDetails = false
    private var wasEncoded = false
    private var wasInit = false
    private var mStoredExtendedDetails = 0
    private var mCurrTime = 0
    private var mDuration = 0

    private var mTouchDownX = 0f
    private var mTouchDownY = 0f
    private var mTouchDownTime = 0L
    private var mTouchDownVolume = 0
    private var mTouchDownBrightness = -1
    private var mTempBrightness = 0
    private var mLastTouchY = 0f

    private var mSlideInfoText = ""
    private var mSlideInfoFadeHandler = Handler()

    lateinit var mView: View
    lateinit var medium: Medium
    lateinit var mTimeHolder: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.pager_video_item, container, false)
        mTimeHolder = mView.video_time_holder
        medium = arguments!!.getSerializable(MEDIUM) as Medium

        // setMenuVisibility is not called at VideoActivity (third party intent)
        if (!mIsFragmentVisible && activity is VideoActivity) {
            mIsFragmentVisible = true
        }

        setupPlayer()
        if (savedInstanceState != null) {
            mCurrTime = savedInstanceState.getInt(PROGRESS)
        }

        mIsFullscreen = activity!!.window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN == View.SYSTEM_UI_FLAG_FULLSCREEN
        checkFullscreen()
        wasInit = true

        return mView
    }

    override fun onResume() {
        super.onResume()
        activity!!.updateTextColors(mView.video_holder)
        mView.video_volume_controller.beVisibleIf(context!!.config.allowVideoGestures)
        mView.video_brightness_controller.beVisibleIf(context!!.config.allowVideoGestures)

        if (context!!.config.showExtendedDetails != mStoredShowExtendedDetails || context!!.config.extendedDetails != mStoredExtendedDetails) {
            checkExtendedDetails()
        }
    }

    override fun onPause() {
        super.onPause()
        pauseVideo()
        mStoredShowExtendedDetails = context!!.config.showExtendedDetails
        mStoredExtendedDetails = context!!.config.extendedDetails
    }

    override fun onDestroy() {
        super.onDestroy()
        if (activity?.isChangingConfigurations == false) {
            cleanup()
        }
    }

    private fun setupPlayer() {
        if (activity == null)
            return

        mView.video_play_outline.setOnClickListener { togglePlayPause() }

        mSurfaceView = mView.video_surface
        mSurfaceHolder = mSurfaceView!!.holder
        mSurfaceHolder!!.addCallback(this)
        mSurfaceView!!.setOnClickListener({ toggleFullscreen() })
        mView.video_holder.setOnClickListener { toggleFullscreen() }
        mView.video_volume_controller.setOnTouchListener { v, event ->
            handleVolumeTouched(event)
            true
        }

        mView.video_brightness_controller.setOnTouchListener { v, event ->
            handleBrightnessTouched(event)
            true
        }

        initTimeHolder()
        checkExtendedDetails()
        initMediaPlayer()
    }

    override fun setMenuVisibility(menuVisible: Boolean) {
        super.setMenuVisibility(menuVisible)
        if (mIsFragmentVisible && !menuVisible) {
            pauseVideo()
            releaseMediaPlayer()
        }
        mIsFragmentVisible = menuVisible
        if (menuVisible && wasInit) {
            initMediaPlayer()
            if (context?.config?.autoplayVideos == true) {
                playVideo()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setVideoSize()
        initTimeHolder()
        checkExtendedDetails()
    }

    private fun toggleFullscreen() {
        listener?.fragmentClicked()
    }

    private fun handleVolumeTouched(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mTouchDownX = event.x
                mTouchDownY = event.y
                mLastTouchY = event.y
                mTouchDownTime = System.currentTimeMillis()
                mTouchDownVolume = getCurrentVolume()
                mSlideInfoText = "${getString(R.string.volume)}:\n"
            }
            MotionEvent.ACTION_MOVE -> {
                val diffX = mTouchDownX - event.x
                val diffY = mTouchDownY - event.y

                if (Math.abs(diffY) > 20 && Math.abs(diffY) > Math.abs(diffX)) {
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
                    mView.video_holder.performClick()
                }
            }
        }
    }

    private fun handleBrightnessTouched(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mTouchDownX = event.x
                mTouchDownY = event.y
                mLastTouchY = event.y
                mTouchDownTime = System.currentTimeMillis()
                mSlideInfoText = "${getString(R.string.brightness)}:\n"
                if (mTouchDownBrightness == -1)
                    mTouchDownBrightness = getCurrentBrightness()
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
                    mView.video_holder.performClick()
                }
                mTouchDownBrightness = mTempBrightness
            }
        }
        mView.video_holder
    }

    private fun getCurrentVolume() = context!!.audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

    private fun getCurrentBrightness() = Settings.System.getInt(activity!!.contentResolver, Settings.System.SCREEN_BRIGHTNESS)

    private fun volumePercentChanged(percent: Int) {
        val stream = AudioManager.STREAM_MUSIC
        val maxVolume = context!!.audioManager.getStreamMaxVolume(stream)
        val percentPerPoint = 100 / maxVolume
        val addPoints = percent / percentPerPoint
        val newVolume = Math.min(maxVolume, Math.max(0, mTouchDownVolume + addPoints))
        context!!.audioManager.setStreamVolume(stream, newVolume, 0)

        val absolutePercent = ((newVolume / maxVolume.toFloat()) * 100).toInt()
        mView.slide_info.apply {
            text = "$mSlideInfoText$absolutePercent%"
            alpha = 1f
        }

        mSlideInfoFadeHandler.removeCallbacksAndMessages(null)
        mSlideInfoFadeHandler.postDelayed({
            mView.slide_info.animate().alpha(0f)
        }, SLIDE_INFO_FADE_DELAY)
    }

    private fun brightnessPercentChanged(percent: Int) {
        val maxBrightness = 255f
        var newBrightness = (mTouchDownBrightness + 2.55 * percent).toFloat()
        newBrightness = Math.min(maxBrightness, Math.max(0f, newBrightness))
        mTempBrightness = newBrightness.toInt()

        val absolutePercent = ((newBrightness / maxBrightness) * 100).toInt()
        mView.slide_info.apply {
            text = "$mSlideInfoText$absolutePercent%"
            alpha = 1f
        }

        val attributes = activity!!.window.attributes
        attributes.screenBrightness = absolutePercent / 100f
        activity!!.window.attributes = attributes

        mSlideInfoFadeHandler.removeCallbacksAndMessages(null)
        mSlideInfoFadeHandler.postDelayed({
            mView.slide_info.animate().alpha(0f)
        }, SLIDE_INFO_FADE_DELAY)
    }

    private fun initTimeHolder() {
        val res = resources
        val height = context!!.navigationBarHeight
        val left = mTimeHolder.paddingLeft
        val top = mTimeHolder.paddingTop
        var right = res.getDimension(R.dimen.timer_padding).toInt()
        var bottom = 0

        if (hasNavBar()) {
            if (res.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                bottom += height
            } else {
                right += height
                bottom += context!!.navigationBarHeight
            }
            mTimeHolder.setPadding(left, top, right, bottom)
        }

        mCurrTimeView = mView.video_curr_time
        mSeekBar = mView.video_seekbar
        mSeekBar!!.setOnSeekBarChangeListener(this)

        if (mIsFullscreen)
            mTimeHolder.beInvisible()
    }

    private fun hasNavBar(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val display = context!!.windowManager.defaultDisplay

            val realDisplayMetrics = DisplayMetrics()
            display.getRealMetrics(realDisplayMetrics)

            val realHeight = realDisplayMetrics.heightPixels
            val realWidth = realDisplayMetrics.widthPixels

            val displayMetrics = DisplayMetrics()
            display.getMetrics(displayMetrics)

            val displayHeight = displayMetrics.heightPixels
            val displayWidth = displayMetrics.widthPixels

            realWidth - displayWidth > 0 || realHeight - displayHeight > 0
        } else {
            val hasMenuKey = ViewConfiguration.get(context).hasPermanentMenuKey()
            val hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK)
            !hasMenuKey && !hasBackKey
        }
    }

    private fun setupTimeHolder() {
        mSeekBar!!.max = mDuration
        mView.video_duration.text = mDuration.getFormattedDuration()
        mTimerHandler = Handler()
        setupTimer()
    }

    private fun setupTimer() {
        activity!!.runOnUiThread(object : Runnable {
            override fun run() {
                if (mMediaPlayer != null && !mIsDragged && mIsPlaying) {
                    mCurrTime = mMediaPlayer!!.currentPosition / 1000
                    mSeekBar!!.progress = mCurrTime
                    mCurrTimeView!!.text = mCurrTime.getFormattedDuration()
                }

                mTimerHandler!!.postDelayed(this, 1000)
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(PROGRESS, mCurrTime)
    }

    private fun checkFullscreen() {
        if (activity == null)
            return

        var anim = android.R.anim.fade_in
        if (mIsFullscreen) {
            anim = android.R.anim.fade_out
            mSeekBar!!.setOnSeekBarChangeListener(null)
        } else {
            mSeekBar!!.setOnSeekBarChangeListener(this)
        }

        AnimationUtils.loadAnimation(activity, anim).apply {
            duration = 150
            fillAfter = true
            mTimeHolder.startAnimation(this)
        }
    }

    private fun togglePlayPause() {
        if (activity == null || !isAdded)
            return

        initMediaPlayer()

        mIsPlaying = !mIsPlaying
        if (mIsPlaying) {
            playVideo()
        } else {
            pauseVideo()
        }
    }

    fun playVideo() {
        if (mMediaPlayer != null) {
            mIsPlaying = true
            mMediaPlayer?.start()
        } else {
            mPlayOnPrepare = true
        }
        mView.video_play_outline.setImageDrawable(null)
        activity!!.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun pauseVideo() {
        mIsPlaying = false
        mMediaPlayer?.pause()
        mView.video_play_outline.setImageDrawable(resources.getDrawable(R.drawable.img_play_outline_big))
        activity!!.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun initMediaPlayer() {
        if (mMediaPlayer != null || !mIsFragmentVisible) {
            return
        }

        try {
            mMediaPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.parse(medium.path))
                setDisplay(mSurfaceHolder)
                setOnCompletionListener { videoCompleted() }
                setOnVideoSizeChangedListener({ mediaPlayer, width, height -> setVideoSize() })
                setOnPreparedListener { videoPrepared(it) }
                setAudioStreamType(AudioManager.STREAM_MUSIC)
                prepare()
            }
        } catch (e: IOException) {
            medium.path = Uri.encode(medium.path)
            if (wasEncoded) {
                releaseMediaPlayer()
            } else {
                wasEncoded = true
                mMediaPlayer = null
                initMediaPlayer()
            }
        } catch (e: Exception) {
            releaseMediaPlayer()
        }
    }

    private fun setProgress(seconds: Int) {
        mMediaPlayer!!.seekTo(seconds * 1000)
        mSeekBar!!.progress = seconds
        mCurrTimeView!!.text = seconds.getFormattedDuration()
    }

    private fun addPreviewImage() {
        mMediaPlayer!!.start()
        mMediaPlayer!!.pause()
    }

    private fun cleanup() {
        pauseVideo()
        mCurrTimeView?.text = 0.getFormattedDuration()
        releaseMediaPlayer()
        mSeekBar?.progress = 0
        mTimerHandler?.removeCallbacksAndMessages(null)
    }

    private fun releaseMediaPlayer() {
        mMediaPlayer?.release()
        mMediaPlayer = null
    }

    private fun videoPrepared(mediaPlayer: MediaPlayer) {
        mDuration = mediaPlayer.duration / 1000
        addPreviewImage()
        setupTimeHolder()
        setProgress(mCurrTime)

        if (mIsFragmentVisible && (context!!.config.autoplayVideos || mPlayOnPrepare))
            playVideo()
    }

    private fun videoCompleted() {
        if (!isAdded) {
            return
        }

        if (listener?.videoEnded() == false && context!!.config.loopVideos) {
            playVideo()
        } else {
            mSeekBar!!.progress = mSeekBar!!.max
            mCurrTimeView!!.text = mDuration.getFormattedDuration()
            pauseVideo()
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        mSurfaceHolder = holder
        if (mIsFragmentVisible)
            initMediaPlayer()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (width != 0 && height != 0 && mSurfaceView != null)
            setVideoSize()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        releaseMediaPlayer()
    }

    private fun setVideoSize() {
        if (mSurfaceHolder == null)
            mSurfaceHolder = mSurfaceView!!.holder

        if (activity == null || mSurfaceHolder == null || !mSurfaceHolder!!.surface.isValid)
            return

        initMediaPlayer()
        if (mMediaPlayer == null) {
            return
        }

        val videoProportion = mMediaPlayer!!.videoWidth.toFloat() / mMediaPlayer!!.videoHeight.toFloat()
        val display = activity!!.windowManager.defaultDisplay
        val screenWidth: Int
        val screenHeight: Int

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val realMetrics = DisplayMetrics()
            display.getRealMetrics(realMetrics)
            screenWidth = realMetrics.widthPixels
            screenHeight = realMetrics.heightPixels
        } else {
            screenWidth = display.width
            screenHeight = display.height
        }

        val screenProportion = screenWidth.toFloat() / screenHeight.toFloat()

        mSurfaceView!!.layoutParams.apply {
            if (videoProportion > screenProportion) {
                width = screenWidth
                height = (screenWidth.toFloat() / videoProportion).toInt()
            } else {
                width = (videoProportion * screenHeight.toFloat()).toInt()
                height = screenHeight
            }
            mSurfaceView!!.layoutParams = this
        }
    }

    private fun checkExtendedDetails() {
        if (context!!.config.showExtendedDetails) {
            mView.video_details.apply {
                text = getMediumExtendedDetails(medium)
                setTextColor(context.config.textColor)
                beVisibleIf(text.isNotEmpty())
                onGlobalLayout {
                    if (height != 0) {
                        val smallMargin = resources.getDimension(R.dimen.small_margin)
                        val timeHolderHeight = mTimeHolder.height - context.navigationBarHeight
                        y = context.usableScreenSize.y - height - timeHolderHeight - if (context.navigationBarHeight == 0) smallMargin else 0f
                    }
                }
            }
        } else {
            mView.video_details.beGone()
        }
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (mMediaPlayer != null && fromUser) {
            setProgress(progress)
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        initMediaPlayer()
        if (mMediaPlayer == null)
            return

        mMediaPlayer!!.pause()
        mIsDragged = true
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (!mIsPlaying) {
            togglePlayPause()
        } else {
            mMediaPlayer?.start()
        }

        mIsDragged = false
    }

    override fun fullscreenToggled(isFullscreen: Boolean) {
        mIsFullscreen = isFullscreen
        checkFullscreen()
        mView.video_details.apply {
            if (visibility == View.VISIBLE) {
                val smallMargin = resources.getDimension(R.dimen.small_margin)
                val timeHolderHeight = mTimeHolder.height - context.navigationBarHeight.toFloat()
                val fullscreenOffset = context.navigationBarHeight.toFloat() - smallMargin
                val newY = context.usableScreenSize.y - height + if (mIsFullscreen) fullscreenOffset else -(timeHolderHeight + if (context.navigationBarHeight == 0) smallMargin else 0f)
                animate().y(newY)
            }
        }
    }
}
