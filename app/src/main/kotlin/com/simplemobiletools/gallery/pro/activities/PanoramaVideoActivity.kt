package com.simplemobiletools.gallery.pro.activities

import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.RelativeLayout
import android.widget.SeekBar
import com.google.vr.sdk.widgets.video.VrVideoEventListener
import com.google.vr.sdk.widgets.video.VrVideoView
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.databinding.ActivityPanoramaVideoBinding
import com.simplemobiletools.gallery.pro.databinding.BottomVideoTimeHolderBinding
import com.simplemobiletools.gallery.pro.extensions.config
import com.simplemobiletools.gallery.pro.extensions.hasNavBar
import com.simplemobiletools.gallery.pro.extensions.hideSystemUI
import com.simplemobiletools.gallery.pro.extensions.showSystemUI
import com.simplemobiletools.gallery.pro.helpers.MIN_SKIP_LENGTH
import com.simplemobiletools.gallery.pro.helpers.PATH
import java.io.File

open class PanoramaVideoActivity : SimpleActivity(), SeekBar.OnSeekBarChangeListener {
    private val CARDBOARD_DISPLAY_MODE = 3
    
    private lateinit var panoramaVideoBinding: ActivityPanoramaVideoBinding
    private lateinit var timeHolderBinding: BottomVideoTimeHolderBinding

    private var mIsFullscreen = false
    private var mIsExploreEnabled = true
    private var mIsRendering = false
    private var mIsPlaying = false
    private var mIsDragged = false
    private var mPlayOnReady = false
    private var mDuration = 0
    private var mCurrTime = 0

    private var mTimerHandler = Handler()

    public override fun onCreate(savedInstanceState: Bundle?) {
        useDynamicTheme = false
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        
        panoramaVideoBinding = ActivityPanoramaVideoBinding.inflate(layoutInflater)
        timeHolderBinding = panoramaVideoBinding.bottomVideoTimeHolder
        setContentView(panoramaVideoBinding.root)
        
        supportActionBar?.hide()

        checkNotchSupport()
        checkIntent()
    }

    override fun onResume() {
        super.onResume()
        panoramaVideoBinding.vrVideoView.resumeRendering()
        mIsRendering = true
        if (config.blackBackground) {
            updateStatusbarColor(Color.BLACK)
        }

        window.statusBarColor = resources.getColor(R.color.circle_black_background)
    }

    override fun onPause() {
        super.onPause()
        panoramaVideoBinding.vrVideoView.pauseRendering()
        mIsRendering = false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mIsRendering) {
            panoramaVideoBinding.vrVideoView.shutdown()
        }

        if (!isChangingConfigurations) {
            mTimerHandler.removeCallbacksAndMessages(null)
        }
    }

    private fun checkIntent() {
        val path = intent.getStringExtra(PATH)
        if (path == null) {
            toast(R.string.invalid_image_path)
            finish()
            return
        }

        setupButtons()
        intent.removeExtra(PATH)

        timeHolderBinding.videoCurrTime.setOnClickListener { skip(false) }
        timeHolderBinding.videoDuration.setOnClickListener { skip(true) }

        try {
            val options = VrVideoView.Options()
            options.inputType = VrVideoView.Options.TYPE_MONO
            val uri = if (path.startsWith("content://")) {
                Uri.parse(path)
            } else {
                Uri.fromFile(File(path))
            }

            panoramaVideoBinding.vrVideoView.apply {
                loadVideo(uri, options)
                pauseVideo()

                setFlingingEnabled(true)
                setPureTouchTracking(true)

                // add custom buttons so we can position them and toggle visibility as desired
                setFullscreenButtonEnabled(false)
                setInfoButtonEnabled(false)
                setTransitionViewEnabled(false)
                setStereoModeButtonEnabled(false)

                setOnClickListener {
                    handleClick()
                }

                setEventListener(object : VrVideoEventListener() {
                    override fun onClick() {
                        handleClick()
                    }

                    override fun onLoadSuccess() {
                        if (mDuration == 0) {
                            setupDuration(duration)
                            setupTimer()
                        }

                        if (mPlayOnReady || config.autoplayVideos) {
                            mPlayOnReady = false
                            mIsPlaying = true
                            resumeVideo()
                        } else {
                            timeHolderBinding.videoTogglePlayPause.setImageResource(R.drawable.ic_play_outline_vector)
                        }
                        timeHolderBinding.videoTogglePlayPause.beVisible()
                    }

                    override fun onCompletion() {
                        videoCompleted()
                    }
                })
            }

            timeHolderBinding.videoTogglePlayPause.setOnClickListener {
                togglePlayPause()
            }
        } catch (e: Exception) {
            showErrorToast(e)
        }

        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            mIsFullscreen = visibility and View.SYSTEM_UI_FLAG_FULLSCREEN != 0
            toggleButtonVisibility()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setupButtons()
    }

    private fun setupDuration(duration: Long) {
        mDuration = (duration / 1000).toInt()
        timeHolderBinding.videoSeekbar.max = mDuration
        timeHolderBinding.videoDuration.text = mDuration.getFormattedDuration()
        setVideoProgress(0)
    }

    private fun setupTimer() {
        runOnUiThread(object : Runnable {
            override fun run() {
                if (mIsPlaying && !mIsDragged) {
                    mCurrTime = (panoramaVideoBinding.vrVideoView.currentPosition / 1000).toInt()
                    timeHolderBinding.videoSeekbar.progress = mCurrTime
                    timeHolderBinding.videoCurrTime.text = mCurrTime.getFormattedDuration()
                }

                mTimerHandler.postDelayed(this, 1000)
            }
        })
    }

    private fun togglePlayPause() {
        mIsPlaying = !mIsPlaying
        if (mIsPlaying) {
            resumeVideo()
        } else {
            pauseVideo()
        }
    }

    private fun resumeVideo() {
        timeHolderBinding.videoTogglePlayPause.setImageResource(R.drawable.ic_pause_outline_vector)
        if (mCurrTime == mDuration) {
            setVideoProgress(0)
            mPlayOnReady = true
            return
        }

        panoramaVideoBinding.vrVideoView.playVideo()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun pauseVideo() {
        panoramaVideoBinding.vrVideoView.pauseVideo()
        timeHolderBinding.videoTogglePlayPause.setImageResource(R.drawable.ic_play_outline_vector)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun setVideoProgress(seconds: Int) {
        panoramaVideoBinding.vrVideoView.seekTo(seconds * 1000L)
        timeHolderBinding.videoSeekbar.progress = seconds
        mCurrTime = seconds
        timeHolderBinding.videoCurrTime.text = seconds.getFormattedDuration()
    }

    private fun videoCompleted() {
        mIsPlaying = false
        mCurrTime = (panoramaVideoBinding.vrVideoView.duration / 1000).toInt()
        timeHolderBinding.videoSeekbar.progress = timeHolderBinding.videoSeekbar.max
        timeHolderBinding.videoCurrTime.text = mDuration.getFormattedDuration()
        pauseVideo()
    }

    private fun setupButtons() {
        var right = 0
        var bottom = 0

        if (hasNavBar()) {
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                bottom += navigationBarHeight
            } else {
                right += navigationBarWidth
                bottom += navigationBarHeight
            }
        }

        timeHolderBinding.root.setPadding(0, 0, right, bottom)
        timeHolderBinding.root.background = resources.getDrawable(R.drawable.gradient_background)
        timeHolderBinding.root.onGlobalLayout {
            val newBottomMargin = timeHolderBinding.root.height -
                    resources.getDimension(R.dimen.video_player_play_pause_size).toInt() -
                    resources.getDimension(R.dimen.activity_margin).toInt()
            (panoramaVideoBinding.explore.layoutParams as RelativeLayout.LayoutParams).bottomMargin = newBottomMargin

            (panoramaVideoBinding.cardboard.layoutParams as RelativeLayout.LayoutParams).apply {
                bottomMargin = newBottomMargin
                rightMargin = navigationBarWidth
            }
            panoramaVideoBinding.explore.requestLayout()
        }
        timeHolderBinding.videoTogglePlayPause.setImageResource(R.drawable.ic_play_outline_vector)

        panoramaVideoBinding.cardboard.setOnClickListener {
            panoramaVideoBinding.vrVideoView.displayMode = CARDBOARD_DISPLAY_MODE
        }

        panoramaVideoBinding.explore.setOnClickListener {
            mIsExploreEnabled = !mIsExploreEnabled
            panoramaVideoBinding.vrVideoView.setPureTouchTracking(mIsExploreEnabled)
            panoramaVideoBinding.explore.setImageResource(if (mIsExploreEnabled) R.drawable.ic_explore_vector
            else R.drawable.ic_explore_off_vector)
        }
    }

    private fun toggleButtonVisibility() {
        val newAlpha = if (mIsFullscreen) 0f else 1f
        arrayOf(panoramaVideoBinding.cardboard, panoramaVideoBinding.explore).forEach {
            it.animate().alpha(newAlpha)
        }

        arrayOf(panoramaVideoBinding.cardboard, panoramaVideoBinding.explore,
                timeHolderBinding.videoTogglePlayPause, timeHolderBinding.videoCurrTime,
                timeHolderBinding.videoDuration).forEach {
            it.isClickable = !mIsFullscreen
        }

        timeHolderBinding.videoSeekbar.setOnSeekBarChangeListener(if (mIsFullscreen) null else this)
        timeHolderBinding.root.animate().alpha(newAlpha).start()
    }

    private fun handleClick() {
        mIsFullscreen = !mIsFullscreen
        toggleButtonVisibility()
        if (mIsFullscreen) {
            hideSystemUI(false)
        } else {
            showSystemUI(false)
        }
    }

    private fun skip(forward: Boolean) {
        if (forward && mCurrTime == mDuration) {
            return
        }

        val curr = panoramaVideoBinding.vrVideoView.currentPosition
        val twoPercents = Math.max((panoramaVideoBinding.vrVideoView.duration / 50).toInt(), MIN_SKIP_LENGTH)
        val newProgress = if (forward) curr + twoPercents else curr - twoPercents
        val roundProgress = Math.round(newProgress / 1000f)
        val limitedProgress = Math.max(Math.min(panoramaVideoBinding.vrVideoView.duration.toInt(), roundProgress), 0)
        setVideoProgress(limitedProgress)
        if (!mIsPlaying) {
            togglePlayPause()
        }
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (fromUser) {
            setVideoProgress(progress)
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
        panoramaVideoBinding.vrVideoView.pauseVideo()
        mIsDragged = true
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        mIsPlaying = true
        resumeVideo()
        mIsDragged = false
    }
}
