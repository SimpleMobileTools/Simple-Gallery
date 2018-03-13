package com.simplemobiletools.gallery.fragments

import android.content.res.Configuration
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.annotation.RequiresApi
import android.util.DisplayMetrics
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import android.widget.TextView
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.isJellyBean1Plus
import com.simplemobiletools.gallery.BuildConfig
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.VideoActivity
import com.simplemobiletools.gallery.extensions.*
import com.simplemobiletools.gallery.helpers.MEDIUM
import com.simplemobiletools.gallery.helpers.MediaSideScroll
import com.simplemobiletools.gallery.models.Medium
import kotlinx.android.synthetic.main.pager_video_item.view.*
import java.io.File
import java.io.IOException

class VideoFragment : ViewPagerFragment(), SurfaceHolder.Callback, SeekBar.OnSeekBarChangeListener {
    private val PROGRESS = "progress"
    private val MIN_SKIP_LENGTH = 2000

    private var mMediaPlayer: MediaPlayer? = null
    private var mSurfaceView: SurfaceView? = null
    private var mSurfaceHolder: SurfaceHolder? = null
    private var mCurrTimeView: TextView? = null
    private var mTimerHandler: Handler? = null
    private var mSeekBar: SeekBar? = null
    private var mTimeHolder: View? = null
    private var mView: View? = null

    private var mIsPlaying = false
    private var mIsDragged = false
    private var mIsFullscreen = false
    private var mIsFragmentVisible = false
    private var mPlayOnPrepare = false
    private var wasEncoded = false
    private var wasInit = false
    private var isPrepared = false
    private var mCurrTime = 0
    private var mDuration = 0
    private var mEncodedPath = ""

    private var mStoredShowExtendedDetails = false
    private var mStoredHideExtendedDetails = false
    private var mStoredExtendedDetails = 0

    private lateinit var brightnessSideScroll: MediaSideScroll
    private lateinit var volumeSideScroll: MediaSideScroll

    lateinit var medium: Medium

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.pager_video_item, container, false).apply {
            instant_prev_item.setOnClickListener { listener?.goToPrevItem() }
            instant_next_item.setOnClickListener { listener?.goToNextItem() }
            mTimeHolder = video_time_holder
        }

        storeStateVariables()
        medium = arguments!!.getSerializable(MEDIUM) as Medium

        // setMenuVisibility is not called at VideoActivity (third party intent)
        if (!mIsFragmentVisible && activity is VideoActivity) {
            mIsFragmentVisible = true
        }

        mIsFullscreen = activity!!.window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN == View.SYSTEM_UI_FLAG_FULLSCREEN
        setupPlayer()
        if (savedInstanceState != null) {
            mCurrTime = savedInstanceState.getInt(PROGRESS)
        }

        checkFullscreen()
        wasInit = true

        mView!!.apply {
            brightnessSideScroll = video_brightness_controller
            brightnessSideScroll.initialize(activity!!, slide_info, true, container) { x, y ->
                video_holder.performClick()
            }

            volumeSideScroll = video_volume_controller
            volumeSideScroll.initialize(activity!!, slide_info, false, container) { x, y ->
                video_holder.performClick()
            }

            video_curr_time.setOnClickListener { skip(false) }
            video_duration.setOnClickListener { skip(true) }
        }

        return mView
    }

    override fun onResume() {
        super.onResume()
        activity!!.updateTextColors(mView!!.video_holder)
        val allowVideoGestures = context!!.config.allowVideoGestures
        val allowInstantChange = context!!.config.allowInstantChange
        mView!!.apply {
            video_volume_controller.beVisibleIf(allowVideoGestures)
            video_brightness_controller.beVisibleIf(allowVideoGestures)

            instant_prev_item.beVisibleIf(allowInstantChange)
            instant_next_item.beVisibleIf(allowInstantChange)
        }

        if (context!!.config.showExtendedDetails != mStoredShowExtendedDetails || context!!.config.extendedDetails != mStoredExtendedDetails) {
            checkExtendedDetails()
        }
        storeStateVariables()
    }

    override fun onPause() {
        super.onPause()
        pauseVideo()
        storeStateVariables()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (activity?.isChangingConfigurations == false) {
            cleanup()
        }
    }

    private fun storeStateVariables() {
        context!!.config.apply {
            mStoredShowExtendedDetails = showExtendedDetails
            mStoredHideExtendedDetails = hideExtendedDetails
            mStoredExtendedDetails = extendedDetails
        }
    }

    private fun setupPlayer() {
        if (activity == null)
            return

        mView!!.video_play_outline.setOnClickListener { togglePlayPause() }

        mSurfaceView = mView!!.video_surface
        mSurfaceHolder = mSurfaceView!!.holder
        mSurfaceHolder!!.addCallback(this)
        mSurfaceView!!.setOnClickListener { toggleFullscreen() }
        mView!!.video_holder.setOnClickListener { toggleFullscreen() }

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

    private fun initTimeHolder() {
        val res = resources
        val left = 0
        val top = 0
        var right = 0
        var bottom = 0

        if (hasNavBar()) {
            if (res.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                bottom += context!!.navigationBarHeight
            } else {
                right += context!!.navigationBarWidth
                bottom += context!!.navigationBarHeight
            }
            mTimeHolder!!.setPadding(left, top, right, bottom)
        }

        mCurrTimeView = mView!!.video_curr_time
        mSeekBar = mView!!.video_seekbar
        mSeekBar!!.setOnSeekBarChangeListener(this)

        if (mIsFullscreen) {
            mTimeHolder!!.beInvisible()
        }
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
        mView!!.video_duration.text = mDuration.getFormattedDuration()
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
        if (activity == null) {
            return
        }

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
            mTimeHolder?.startAnimation(this)
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
        if (mMediaPlayer != null && isPrepared) {
            mIsPlaying = true
            mMediaPlayer?.start()
        } else {
            mPlayOnPrepare = true
        }
        mView!!.video_play_outline.setImageDrawable(null)
        activity!!.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun pauseVideo() {
        mIsPlaying = false
        mMediaPlayer?.pause()
        mView?.video_play_outline?.setImageDrawable(resources.getDrawable(R.drawable.img_play_outline_big))
        activity!!.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun initMediaPlayer() {
        if (mMediaPlayer != null || !mIsFragmentVisible) {
            return
        }

        val mediumPath = if (wasEncoded) mEncodedPath else getPathToLoad(medium)

        // this workaround is needed for example if the filename contains a colon
        val fileUri = if (mediumPath.startsWith("/")) context!!.getFilePublicUri(File(mediumPath), BuildConfig.APPLICATION_ID) else Uri.parse(mediumPath)
        try {
            mMediaPlayer = MediaPlayer().apply {
                setDataSource(context, fileUri)
                setDisplay(mSurfaceHolder)
                setOnCompletionListener { videoCompleted() }
                setOnVideoSizeChangedListener { mediaPlayer, width, height -> setVideoSize() }
                setOnPreparedListener { videoPrepared(it) }
                setAudioStreamType(AudioManager.STREAM_MUSIC)
                prepare()
            }
        } catch (e: IOException) {
            mEncodedPath = Uri.encode(getPathToLoad(medium))
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
        mSurfaceView = null
        mSurfaceHolder?.removeCallback(this)
        mSurfaceHolder = null
    }

    private fun releaseMediaPlayer() {
        mMediaPlayer?.setSurface(null)
        mMediaPlayer?.release()
        mMediaPlayer = null
    }

    private fun videoPrepared(mediaPlayer: MediaPlayer) {
        isPrepared = true
        mDuration = mediaPlayer.duration / 1000
        addPreviewImage()
        setupTimeHolder()
        setProgress(mCurrTime)

        if (mIsFragmentVisible && (context!!.config.autoplayVideos || mPlayOnPrepare)) {
            playVideo()
        }
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
        if (mIsFragmentVisible) {
            initMediaPlayer()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (width != 0 && height != 0 && mSurfaceView != null) {
            setVideoSize()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        releaseMediaPlayer()
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
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

        if (isJellyBean1Plus()) {
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
            mView!!.video_details.apply {
                text = getMediumExtendedDetails(medium)
                setTextColor(context.config.textColor)
                beVisibleIf(text.isNotEmpty())
                alpha = if (!context!!.config.hideExtendedDetails || !mIsFullscreen) 1f else 0f
                onGlobalLayout {
                    if (height != 0 && isAdded) {
                        y = getExtendedDetailsY(height)
                    }
                }
            }
        } else {
            mView!!.video_details.beGone()
        }
    }

    private fun skip(forward: Boolean) {
        if (mMediaPlayer == null) {
            return
        }

        val curr = mMediaPlayer!!.currentPosition
        val twoPercents = Math.max(mMediaPlayer!!.duration / 50, MIN_SKIP_LENGTH)
        val newProgress = if (forward) curr + twoPercents else curr - twoPercents
        val roundProgress = Math.round(newProgress / 1000f)
        val limitedProgress = Math.max(Math.min(mMediaPlayer!!.duration / 1000, roundProgress), 0)
        setProgress(limitedProgress)
        if (!mIsPlaying) {
            togglePlayPause()
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
        mView!!.video_details.apply {
            if (mStoredShowExtendedDetails) {
                animate().y(getExtendedDetailsY(height))

                if (mStoredHideExtendedDetails) {
                    animate().alpha(if (isFullscreen) 0f else 1f).start()
                }
            }
        }
    }

    private fun getExtendedDetailsY(height: Int): Float {
        val smallMargin = resources.getDimension(R.dimen.small_margin)
        val timeHolderHeight = mTimeHolder!!.height - context!!.navigationBarHeight.toFloat()
        val fullscreenOffset = context!!.navigationBarHeight.toFloat() - smallMargin
        return context!!.usableScreenSize.y - height + if (mIsFullscreen) fullscreenOffset else -(timeHolderHeight + if (context!!.navigationBarHeight == 0) smallMargin else 0f)
    }
}
