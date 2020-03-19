package com.simplemobiletools.gallery.pro.activities

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.view.*
import android.widget.SeekBar
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.ContentDataSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.video.VideoListener
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.extensions.*
import com.simplemobiletools.gallery.pro.helpers.*
import kotlinx.android.synthetic.main.activity_video_player.*
import kotlinx.android.synthetic.main.bottom_video_time_holder.*

open class VideoPlayerActivity : SimpleActivity(), SeekBar.OnSeekBarChangeListener, TextureView.SurfaceTextureListener {
    private val PLAY_WHEN_READY_DRAG_DELAY = 100L

    private var mIsFullscreen = false
    private var mIsPlaying = false
    private var mWasVideoStarted = false
    private var mIsDragged = false
    private var mIsOrientationLocked = false
    private var mScreenWidth = 0
    private var mCurrTime = 0
    private var mDuration = 0
    private var mDragThreshold = 0f
    private var mTouchDownX = 0f
    private var mTouchDownY = 0f
    private var mTouchDownTime = 0L
    private var mProgressAtDown = 0L
    private var mCloseDownThreshold = 100f

    private var mUri: Uri? = null
    private var mExoPlayer: SimpleExoPlayer? = null
    private var mVideoSize = Point(0, 0)
    private var mTimerHandler = Handler()
    private var mPlayWhenReadyHandler = Handler()

    private var mIgnoreCloseDown = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)
        setupOrientation()
        checkNotchSupport()
        initPlayer()
    }

    override fun onResume() {
        super.onResume()
        top_shadow.layoutParams.height = statusBarHeight + actionBarHeight
        supportActionBar?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (config.blackBackground) {
            video_player_holder.background = ColorDrawable(Color.BLACK)
        }

        if (config.maxBrightness) {
            val attributes = window.attributes
            attributes.screenBrightness = 1f
            window.attributes = attributes
        }

        updateTextColors(video_player_holder)
    }

    override fun onPause() {
        super.onPause()
        pauseVideo()

        if (config.rememberLastVideoPosition && mWasVideoStarted) {
            saveVideoProgress()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            pauseVideo()
            video_curr_time.text = 0.getFormattedDuration()
            releaseExoPlayer()
            video_seekbar.progress = 0
            mTimerHandler.removeCallbacksAndMessages(null)
            mPlayWhenReadyHandler.removeCallbacksAndMessages(null)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_video_player, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_change_orientation -> changeOrientation()
            R.id.menu_open_with -> openPath(mUri!!.toString(), true)
            R.id.menu_share -> shareMediumPath(mUri!!.toString())
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setVideoSize()
        initTimeHolder()
        video_surface_frame.onGlobalLayout {
            video_surface_frame.controller.resetState()
        }
    }

    private fun setupOrientation() {
        if (!mIsOrientationLocked) {
            if (config.screenRotation == ROTATE_BY_DEVICE_ROTATION) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
            } else if (config.screenRotation == ROTATE_BY_SYSTEM_SETTING) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    private fun initPlayer() {
        mUri = intent.data ?: return
        supportActionBar?.title = getFilenameFromUri(mUri!!)
        initTimeHolder()

        showSystemUI(true)
        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            val isFullscreen = visibility and View.SYSTEM_UI_FLAG_FULLSCREEN != 0
            fullscreenToggled(isFullscreen)
        }

        video_curr_time.setOnClickListener { doSkip(false) }
        video_duration.setOnClickListener { doSkip(true) }
        video_toggle_play_pause.setOnClickListener { togglePlayPause() }
        video_surface_frame.setOnClickListener { toggleFullscreen() }
        video_surface_frame.controller.settings.swallowDoubleTaps = true

        video_next_file.beVisibleIf(intent.getBooleanExtra(SHOW_NEXT_ITEM, false))
        video_next_file.setOnClickListener { handleNextFile() }

        video_prev_file.beVisibleIf(intent.getBooleanExtra(SHOW_PREV_ITEM, false))
        video_prev_file.setOnClickListener { handlePrevFile() }


        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent?): Boolean {
                if (e != null) {
                    handleDoubleTap(e.rawX)
                }

                return true
            }
        })

        video_surface_frame.setOnTouchListener { view, event ->
            handleEvent(event)
            gestureDetector.onTouchEvent(event)
            false
        }

        initExoPlayer()
        video_surface.surfaceTextureListener = this

        if (config.allowVideoGestures) {
            video_brightness_controller.initialize(this, slide_info, true, video_player_holder, singleTap = { x, y ->
                toggleFullscreen()
            }, doubleTap = { x, y ->
                doSkip(false)
            })

            video_volume_controller.initialize(this, slide_info, false, video_player_holder, singleTap = { x, y ->
                toggleFullscreen()
            }, doubleTap = { x, y ->
                doSkip(true)
            })
        } else {
            video_brightness_controller.beGone()
            video_volume_controller.beGone()
        }

        if (config.hideSystemUI) {
            Handler().postDelayed({
                fullscreenToggled(true)
            }, HIDE_SYSTEM_UI_DELAY)
        }

        mDragThreshold = DRAG_THRESHOLD * resources.displayMetrics.density
    }

    private fun initExoPlayer() {
        val dataSpec = DataSpec(mUri)
        val fileDataSource = ContentDataSource(applicationContext)
        try {
            fileDataSource.open(dataSpec)
        } catch (e: Exception) {
            showErrorToast(e)
        }

        val factory = DataSource.Factory { fileDataSource }
        val audioSource = ExtractorMediaSource(fileDataSource.uri, factory, DefaultExtractorsFactory(), null, null)
        mExoPlayer = ExoPlayerFactory.newSimpleInstance(applicationContext).apply {
            seekParameters = SeekParameters.CLOSEST_SYNC
            audioStreamType = C.STREAM_TYPE_MUSIC
            if (config.loopVideos) {
                repeatMode = Player.REPEAT_MODE_ONE
            }
            prepare(audioSource)
        }
        initExoPlayerListeners()
    }

    private fun initExoPlayerListeners() {
        mExoPlayer!!.addListener(object : Player.EventListener {
            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {}

            override fun onSeekProcessed() {}

            override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {}

            override fun onPlayerError(error: ExoPlaybackException?) {}

            override fun onLoadingChanged(isLoading: Boolean) {}

            override fun onPositionDiscontinuity(reason: Int) {
                // Reset progress views when video loops.
                if (reason == Player.DISCONTINUITY_REASON_PERIOD_TRANSITION) {
                    video_seekbar.progress = 0
                    video_curr_time.text = 0.getFormattedDuration()
                }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {}

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {}

            override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {}

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> videoPrepared()
                    Player.STATE_ENDED -> videoCompleted()
                }
            }
        })

        mExoPlayer!!.addVideoListener(object : VideoListener {
            override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                mVideoSize.x = width
                mVideoSize.y = height
                setVideoSize()
            }

            override fun onRenderedFirstFrame() {}
        })
    }

    private fun videoPrepared() {
        if (!mWasVideoStarted) {
            video_toggle_play_pause.beVisible()
            mDuration = (mExoPlayer!!.duration / 1000).toInt()
            video_seekbar.max = mDuration
            video_duration.text = mDuration.getFormattedDuration()
            setPosition(mCurrTime)

            if (config.rememberLastVideoPosition) {
                setLastVideoSavedPosition()
            }

            if (config.autoplayVideos) {
                resumeVideo()
            } else {
                video_toggle_play_pause.setImageResource(R.drawable.ic_play_outline)
            }
        }
    }

    private fun handleDoubleTap(x: Float) {
        val instantWidth = mScreenWidth / 7
        when {
            x <= instantWidth -> doSkip(false)
            x >= mScreenWidth - instantWidth -> doSkip(true)
            else -> togglePlayPause()
        }
    }

    private fun resumeVideo() {
        video_toggle_play_pause.setImageResource(R.drawable.ic_pause_outline)
        if (mExoPlayer == null) {
            return
        }

        val wasEnded = didVideoEnd()
        if (wasEnded) {
            setPosition(0)
        }

        mWasVideoStarted = true
        mIsPlaying = true
        mExoPlayer?.playWhenReady = true
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun pauseVideo() {
        video_toggle_play_pause.setImageResource(R.drawable.ic_play_outline)
        if (mExoPlayer == null) {
            return
        }

        mIsPlaying = false
        if (!didVideoEnd()) {
            mExoPlayer?.playWhenReady = false
        }

        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun togglePlayPause() {
        mIsPlaying = !mIsPlaying
        if (mIsPlaying) {
            resumeVideo()
        } else {
            pauseVideo()
        }
    }

    private fun setPosition(seconds: Int) {
        mExoPlayer?.seekTo(seconds * 1000L)
        video_seekbar.progress = seconds
        video_curr_time.text = seconds.getFormattedDuration()
    }

    private fun setLastVideoSavedPosition() {
        val pos = config.getLastVideoPosition(mUri.toString())
        if (pos > 0) {
            setPosition(pos)
        }
    }

    private fun videoCompleted() {
        if (mExoPlayer == null) {
            return
        }

        clearLastVideoSavedProgress()
        mCurrTime = (mExoPlayer!!.duration / 1000).toInt()
        video_seekbar.progress = video_seekbar.max
        video_curr_time.text = mDuration.getFormattedDuration()
        pauseVideo()
    }

    private fun didVideoEnd(): Boolean {
        val currentPos = mExoPlayer?.currentPosition ?: 0
        val duration = mExoPlayer?.duration ?: 0
        return currentPos != 0L && currentPos >= duration
    }

    private fun saveVideoProgress() {
        if (!didVideoEnd()) {
            config.saveLastVideoPosition(mUri.toString(), mExoPlayer!!.currentPosition.toInt() / 1000)
        }
    }

    private fun clearLastVideoSavedProgress() {
        config.removeLastVideoPosition(mUri.toString())
    }

    private fun setVideoSize() {
        val videoProportion = mVideoSize.x.toFloat() / mVideoSize.y.toFloat()
        val display = windowManager.defaultDisplay
        val screenWidth: Int
        val screenHeight: Int

        val realMetrics = DisplayMetrics()
        display.getRealMetrics(realMetrics)
        screenWidth = realMetrics.widthPixels
        screenHeight = realMetrics.heightPixels

        val screenProportion = screenWidth.toFloat() / screenHeight.toFloat()

        video_surface.layoutParams.apply {
            if (videoProportion > screenProportion) {
                width = screenWidth
                height = (screenWidth.toFloat() / videoProportion).toInt()
            } else {
                width = (videoProportion * screenHeight.toFloat()).toInt()
                height = screenHeight
            }
            video_surface.layoutParams = this
        }

        val multiplier = if (screenWidth > screenHeight) 0.5 else 0.8
        mScreenWidth = (screenWidth * multiplier).toInt()

        if (config.screenRotation == ROTATE_BY_ASPECT_RATIO) {
            if (mVideoSize.x > mVideoSize.y) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else if (mVideoSize.x < mVideoSize.y) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }

    private fun changeOrientation() {
        mIsOrientationLocked = true
        requestedOrientation = if (resources.configuration.orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    private fun toggleFullscreen() {
        fullscreenToggled(!mIsFullscreen)
    }

    private fun fullscreenToggled(isFullScreen: Boolean) {
        mIsFullscreen = isFullScreen
        if (isFullScreen) {
            hideSystemUI(true)
        } else {
            showSystemUI(true)
        }

        val newAlpha = if (isFullScreen) 0f else 1f
        arrayOf(video_prev_file, video_toggle_play_pause, video_next_file, video_curr_time, video_seekbar, video_duration, top_shadow, video_bottom_gradient).forEach {
            it.animate().alpha(newAlpha).start()
        }
        video_seekbar.setOnSeekBarChangeListener(if (mIsFullscreen) null else this)
        arrayOf(video_prev_file, video_next_file, video_curr_time, video_duration).forEach {
            it.isClickable = !mIsFullscreen
        }
    }

    private fun initTimeHolder() {
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

        video_time_holder.setPadding(0, 0, right, bottom)
        video_seekbar.setOnSeekBarChangeListener(this)
        video_seekbar.max = mDuration
        video_duration.text = mDuration.getFormattedDuration()
        video_curr_time.text = mCurrTime.getFormattedDuration()
        setupTimer()
    }

    private fun setupTimer() {
        runOnUiThread(object : Runnable {
            override fun run() {
                if (mExoPlayer != null && !mIsDragged && mIsPlaying) {
                    mCurrTime = (mExoPlayer!!.currentPosition / 1000).toInt()
                    video_seekbar.progress = mCurrTime
                    video_curr_time.text = mCurrTime.getFormattedDuration()
                }

                mTimerHandler.postDelayed(this, 1000)
            }
        })
    }

    private fun doSkip(forward: Boolean) {
        if (mExoPlayer == null) {
            return
        }

        val curr = mExoPlayer!!.currentPosition
        val newProgress = if (forward) curr + FAST_FORWARD_VIDEO_MS else curr - FAST_FORWARD_VIDEO_MS
        val roundProgress = Math.round(newProgress / 1000f)
        val limitedProgress = Math.max(Math.min(mExoPlayer!!.duration.toInt() / 1000, roundProgress), 0)
        setPosition(limitedProgress)
        if (!mIsPlaying) {
            togglePlayPause()
        }
    }

    private fun handleEvent(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mTouchDownX = event.x
                mTouchDownY = event.y
                mTouchDownTime = System.currentTimeMillis()
                mProgressAtDown = mExoPlayer!!.currentPosition
            }
            MotionEvent.ACTION_POINTER_DOWN -> mIgnoreCloseDown = true
            MotionEvent.ACTION_MOVE -> {
                val diffX = event.x - mTouchDownX
                val diffY = event.y - mTouchDownY

                if (mIsDragged || (Math.abs(diffX) > mDragThreshold && Math.abs(diffX) > Math.abs(diffY)) && video_surface_frame.controller.state.zoom == 1f) {
                    if (!mIsDragged) {
                        arrayOf(video_curr_time, video_seekbar, video_duration).forEach {
                            it.animate().alpha(1f).start()
                        }
                    }
                    mIgnoreCloseDown = true
                    mIsDragged = true
                    var percent = ((diffX / mScreenWidth) * 100).toInt()
                    percent = Math.min(100, Math.max(-100, percent))

                    val skipLength = (mDuration * 1000f) * (percent / 100f)
                    var newProgress = mProgressAtDown + skipLength
                    newProgress = Math.max(Math.min(mExoPlayer!!.duration.toFloat(), newProgress), 0f)
                    val newSeconds = (newProgress / 1000).toInt()
                    setPosition(newSeconds)
                    resetPlayWhenReady()
                }
            }
            MotionEvent.ACTION_UP -> {
                val diffX = mTouchDownX - event.x
                val diffY = mTouchDownY - event.y

                val downGestureDuration = System.currentTimeMillis() - mTouchDownTime
                if (config.allowDownGesture && !mIgnoreCloseDown && Math.abs(diffY) > Math.abs(diffX) && diffY < -mCloseDownThreshold &&
                        downGestureDuration < MAX_CLOSE_DOWN_GESTURE_DURATION &&
                        video_surface_frame.controller.state.zoom == 1f) {
                    supportFinishAfterTransition()
                }

                mIgnoreCloseDown = false
                if (mIsDragged) {
                    if (mIsFullscreen) {
                        arrayOf(video_curr_time, video_seekbar, video_duration).forEach {
                            it.animate().alpha(0f).start()
                        }
                    }

                    if (!mIsPlaying) {
                        togglePlayPause()
                    }
                }
                mIsDragged = false
            }
        }
    }

    private fun handleNextFile() {
        Intent().apply {
            putExtra(GO_TO_NEXT_ITEM, true)
            setResult(Activity.RESULT_OK, this)
        }
        finish()
    }

    private fun handlePrevFile() {
        Intent().apply {
            putExtra(GO_TO_PREV_ITEM, true)
            setResult(Activity.RESULT_OK, this)
        }
        finish()
    }

    private fun resetPlayWhenReady() {
        mExoPlayer?.playWhenReady = false
        mPlayWhenReadyHandler.removeCallbacksAndMessages(null)
        mPlayWhenReadyHandler.postDelayed({
            mExoPlayer?.playWhenReady = true
        }, PLAY_WHEN_READY_DRAG_DELAY)
    }

    private fun releaseExoPlayer() {
        mExoPlayer?.stop()
        ensureBackgroundThread {
            mExoPlayer?.release()
            mExoPlayer = null
        }
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (mExoPlayer != null && fromUser) {
            setPosition(progress)
            resetPlayWhenReady()
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
        mIsDragged = true
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        if (mExoPlayer == null)
            return

        if (mIsPlaying) {
            mExoPlayer!!.playWhenReady = true
        } else {
            togglePlayPause()
        }

        mIsDragged = false
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?) = false

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        ensureBackgroundThread {
            mExoPlayer?.setVideoSurface(Surface(video_surface!!.surfaceTexture))
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {}
}
