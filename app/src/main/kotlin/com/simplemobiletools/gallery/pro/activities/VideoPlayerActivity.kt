package com.simplemobiletools.gallery.pro.activities

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
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
import com.simplemobiletools.commons.helpers.PERMISSION_WRITE_STORAGE
import com.simplemobiletools.commons.helpers.isPiePlus
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.extensions.*
import kotlinx.android.synthetic.main.activity_video_player.*
import kotlinx.android.synthetic.main.bottom_video_time_holder.*

open class VideoPlayerActivity : SimpleActivity(), SeekBar.OnSeekBarChangeListener, TextureView.SurfaceTextureListener {
    private var mIsFullscreen = false
    private var mCurrTime = 0
    private var mDuration = 0
    private var mVideoSize = Point(0, 0)

    private var mUri: Uri? = null
    private var mExoPlayer: SimpleExoPlayer? = null

    private var mTouchDownX = 0f
    private var mTouchDownY = 0f
    private var mCloseDownThreshold = 100f
    private var mIgnoreCloseDown = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        handlePermission(PERMISSION_WRITE_STORAGE) {
            if (it) {
                initPlayer()
            } else {
                toast(R.string.no_storage_permissions)
                finish()
            }
        }
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
        updateTextColors(video_player_holder)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_video_player, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_change_orientation -> changeOrientation()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun initPlayer() {
        mUri = intent.data ?: return
        supportActionBar?.title = getFilenameFromUri(mUri!!)
        initTimeHolder()

        if (isPiePlus()) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        }

        showSystemUI(true)
        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            val isFullscreen = visibility and View.SYSTEM_UI_FLAG_FULLSCREEN != 0
            fullscreenToggled(isFullscreen)
        }

        // adding an empty click listener just to avoid ripple animation at toggling fullscreen
        video_seekbar.setOnClickListener { }
        video_curr_time.setOnClickListener { skip(false) }
        video_duration.setOnClickListener { skip(true) }
        video_player_holder.setOnClickListener {
            fullscreenToggled(!mIsFullscreen)
        }

        if (config.allowDownGesture) {
            video_player_holder.setOnTouchListener { v, event ->
                handleEvent(event)
                false
            }
        }

        initExoPlayer()
        video_surface.surfaceTextureListener = this
        video_surface.setOnClickListener {
            fullscreenToggled(!mIsFullscreen)
        }
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
        mExoPlayer = ExoPlayerFactory.newSimpleInstance(applicationContext)
        mExoPlayer!!.seekParameters = SeekParameters.CLOSEST_SYNC
        mExoPlayer!!.audioStreamType = C.STREAM_TYPE_MUSIC
        mExoPlayer!!.prepare(audioSource)
        initExoPlayerListeners()
    }

    private fun initExoPlayerListeners() {
        mExoPlayer!!.addListener(object : Player.EventListener {
            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {}

            override fun onSeekProcessed() {}

            override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {}

            override fun onPlayerError(error: ExoPlaybackException?) {}

            override fun onLoadingChanged(isLoading: Boolean) {}

            override fun onPositionDiscontinuity(reason: Int) {}

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
        mDuration = (mExoPlayer!!.duration / 1000).toInt()
        video_duration.text = mDuration.getFormattedDuration()
        setPosition(mCurrTime)
    }

    private fun setPosition(seconds: Int) {
        mExoPlayer?.seekTo(seconds * 1000L)
        video_seekbar.progress = seconds
        video_curr_time.text = seconds.getFormattedDuration()
    }

    private fun videoCompleted() {

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
    }

    private fun changeOrientation() {
        requestedOrientation = if (resources.configuration.orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    private fun fullscreenToggled(isFullScreen: Boolean) {
        mIsFullscreen = isFullScreen
        if (isFullScreen) {
            hideSystemUI(true)
        } else {
            showSystemUI(true)
        }

        val newAlpha = if (isFullScreen) 0f else 1f
        top_shadow.animate().alpha(newAlpha).start()
        video_time_holder.animate().alpha(newAlpha).start()
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
        video_seekbar!!.max = mDuration
        video_time_holder.beInvisibleIf(mIsFullscreen)
        video_duration.text = mDuration.getFormattedDuration()
        video_curr_time.text = mCurrTime.getFormattedDuration()
    }

    private fun hasNavBar(): Boolean {
        val display = windowManager.defaultDisplay

        val realDisplayMetrics = DisplayMetrics()
        display.getRealMetrics(realDisplayMetrics)

        val displayMetrics = DisplayMetrics()
        display.getMetrics(displayMetrics)

        return (realDisplayMetrics.widthPixels - displayMetrics.widthPixels > 0) || (realDisplayMetrics.heightPixels - displayMetrics.heightPixels > 0)
    }

    private fun skip(forward: Boolean) {

    }

    private fun handleEvent(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mTouchDownX = event.x
                mTouchDownY = event.y
            }
            MotionEvent.ACTION_POINTER_DOWN -> mIgnoreCloseDown = true
            MotionEvent.ACTION_UP -> {
                val diffX = mTouchDownX - event.x
                val diffY = mTouchDownY - event.y

                if (!mIgnoreCloseDown && Math.abs(diffY) > Math.abs(diffX) && diffY < -mCloseDownThreshold) {
                    supportFinishAfterTransition()
                }
                mIgnoreCloseDown = false
            }
        }
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?) = false

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        Thread {
            mExoPlayer?.setVideoSurface(Surface(video_surface!!.surfaceTexture))
        }.start()
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
    }
}
