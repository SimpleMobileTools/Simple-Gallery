package com.simplemobiletools.gallery.pro.activities

import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.RelativeLayout
import android.widget.SeekBar
import com.google.vr.sdk.widgets.video.VrVideoEventListener
import com.google.vr.sdk.widgets.video.VrVideoView
import com.simplemobiletools.commons.extensions.getFormattedDuration
import com.simplemobiletools.commons.extensions.onGlobalLayout
import com.simplemobiletools.commons.extensions.showErrorToast
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.helpers.PERMISSION_WRITE_STORAGE
import com.simplemobiletools.commons.helpers.isPiePlus
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.extensions.*
import com.simplemobiletools.gallery.pro.helpers.HIDE_PLAY_PAUSE_DELAY
import com.simplemobiletools.gallery.pro.helpers.MIN_SKIP_LENGTH
import com.simplemobiletools.gallery.pro.helpers.PATH
import com.simplemobiletools.gallery.pro.helpers.PLAY_PAUSE_VISIBLE_ALPHA
import kotlinx.android.synthetic.main.activity_panorama_video.*
import kotlinx.android.synthetic.main.bottom_video_time_holder.*
import java.io.File

open class PanoramaVideoActivity : SimpleActivity(), SeekBar.OnSeekBarChangeListener {
    private val CARDBOARD_DISPLAY_MODE = 3

    private var mIsFullscreen = false
    private var mIsExploreEnabled = true
    private var mIsRendering = false
    private var mIsPlaying = false
    private var mIsDragged = false
    private var mPlayOnReady = false
    private var mDuration = 0
    private var mCurrTime = 0

    private var mHidePlayPauseHandler = Handler()
    private var mTimerHandler = Handler()

    public override fun onCreate(savedInstanceState: Bundle?) {
        useDynamicTheme = false
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_panorama_video)
        supportActionBar?.hide()

        if (isPiePlus()) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        }

        setupButtons()

        cardboard.setOnClickListener {
            vr_video_view.displayMode = CARDBOARD_DISPLAY_MODE
        }

        explore.setOnClickListener {
            mIsExploreEnabled = !mIsExploreEnabled
            vr_video_view.setPureTouchTracking(mIsExploreEnabled)
            explore.setImageResource(if (mIsExploreEnabled) R.drawable.ic_explore else R.drawable.ic_explore_off)
        }

        handlePermission(PERMISSION_WRITE_STORAGE) {
            if (it) {
                checkIntent()
            } else {
                toast(R.string.no_storage_permissions)
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vr_video_view.resumeRendering()
        mIsRendering = true
        if (config.blackBackground) {
            updateStatusbarColor(Color.BLACK)
        }

        window.statusBarColor = resources.getColor(R.color.circle_black_background)
    }

    override fun onPause() {
        super.onPause()
        vr_video_view.pauseRendering()
        mIsRendering = false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mIsRendering) {
            vr_video_view.shutdown()
        }

        if (!isChangingConfigurations) {
            mHidePlayPauseHandler.removeCallbacksAndMessages(null)
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

        intent.removeExtra(PATH)

        video_curr_time.setOnClickListener { skip(false) }
        video_duration.setOnClickListener { skip(true) }

        try {
            val options = VrVideoView.Options()
            options.inputType = VrVideoView.Options.TYPE_MONO

            vr_video_view.apply {
                loadVideo(Uri.fromFile(File(path)), options)
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

                        if (mPlayOnReady) {
                            mPlayOnReady = false
                            playVideo()
                        }
                    }

                    override fun onCompletion() {
                        videoCompleted()
                    }
                })
            }

            video_play_outline.setOnClickListener {
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

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        setupButtons()
    }

    private fun setupDuration(duration: Long) {
        mDuration = (duration / 1000).toInt()
        video_seekbar.max = mDuration
        video_duration.text = mDuration.getFormattedDuration()
        setVideoProgress(0)
    }

    private fun setupTimer() {
        runOnUiThread(object : Runnable {
            override fun run() {
                if (mIsPlaying && !mIsDragged) {
                    mCurrTime = (vr_video_view!!.currentPosition / 1000).toInt()
                    video_seekbar.progress = mCurrTime
                    video_curr_time.text = mCurrTime.getFormattedDuration()
                }

                mTimerHandler.postDelayed(this, 1000)
            }
        })
    }

    private fun togglePlayPause() {
        mIsPlaying = !mIsPlaying
        video_play_outline.alpha = PLAY_PAUSE_VISIBLE_ALPHA
        mHidePlayPauseHandler.removeCallbacksAndMessages(null)
        if (mIsPlaying) {
            playVideo()
        } else {
            pauseVideo()
        }
        schedulePlayPauseFadeOut()
    }

    private fun playVideo() {
        video_play_outline.setImageResource(R.drawable.ic_pause)
        if (mCurrTime == mDuration) {
            setVideoProgress(0)
            mPlayOnReady = true
            return
        }

        vr_video_view.playVideo()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun pauseVideo() {
        vr_video_view.pauseVideo()
        video_play_outline.setImageResource(R.drawable.ic_play)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun setVideoProgress(seconds: Int) {
        vr_video_view.seekTo(seconds * 1000L)
        video_seekbar.progress = seconds
        mCurrTime = seconds
        video_curr_time.text = seconds.getFormattedDuration()
    }

    private fun videoCompleted() {
        mIsPlaying = false
        mCurrTime = (vr_video_view.duration / 1000).toInt()
        video_seekbar.progress = video_seekbar.max
        video_curr_time.text = mDuration.getFormattedDuration()
        pauseVideo()
        video_play_outline.alpha = PLAY_PAUSE_VISIBLE_ALPHA
    }

    private fun schedulePlayPauseFadeOut() {
        mHidePlayPauseHandler.removeCallbacksAndMessages(null)
        mHidePlayPauseHandler.postDelayed({
            video_play_outline.animate().alpha(0f).start()
        }, HIDE_PLAY_PAUSE_DELAY)
    }

    private fun setupButtons() {
        val navBarHeight = navigationBarHeight
        video_time_holder.apply {
            (layoutParams as RelativeLayout.LayoutParams).bottomMargin = navBarHeight
            setPadding(paddingLeft, paddingTop, navigationBarWidth, paddingBottom)
        }

        video_time_holder.onGlobalLayout {
            (explore.layoutParams as RelativeLayout.LayoutParams).bottomMargin = navBarHeight + video_time_holder.height

            (cardboard.layoutParams as RelativeLayout.LayoutParams).apply {
                bottomMargin = navBarHeight + video_time_holder.height
                rightMargin = navigationBarWidth
            }
            vr_view_gradient_background.layoutParams.height = navBarHeight + video_time_holder.height + explore.height
            explore.requestLayout()
        }
    }

    private fun toggleButtonVisibility() {
        val newAlpha = if (mIsFullscreen) 0f else 1f
        arrayOf(cardboard, explore, vr_view_gradient_background).forEach {
            it.animate().alpha(newAlpha)
            it.isClickable = !mIsFullscreen
        }

        var anim = android.R.anim.fade_in
        if (mIsFullscreen) {
            anim = android.R.anim.fade_out
            video_seekbar.setOnSeekBarChangeListener(null)
        } else {
            video_seekbar.setOnSeekBarChangeListener(this)
        }

        AnimationUtils.loadAnimation(this, anim).apply {
            duration = 150
            fillAfter = true
            video_time_holder.startAnimation(this)
        }
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

        val curr = vr_video_view.currentPosition
        val twoPercents = Math.max((vr_video_view.duration / 50).toInt(), MIN_SKIP_LENGTH)
        val newProgress = if (forward) curr + twoPercents else curr - twoPercents
        val roundProgress = Math.round(newProgress / 1000f)
        val limitedProgress = Math.max(Math.min(vr_video_view.duration.toInt(), roundProgress), 0)
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
        vr_video_view.pauseVideo()
        mIsDragged = true
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        mIsPlaying = true
        playVideo()
        mIsDragged = false
        schedulePlayPauseFadeOut()
    }
}
