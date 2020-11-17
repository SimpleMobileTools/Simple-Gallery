package com.simplemobiletools.gallery.pro.fragments

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.view.*
import android.widget.RelativeLayout
import android.widget.SeekBar
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.ContentDataSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.FileDataSource
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.activities.PanoramaVideoActivity
import com.simplemobiletools.gallery.pro.activities.VideoActivity
import com.simplemobiletools.gallery.pro.databinding.BottomVideoTimeHolderBinding
import com.simplemobiletools.gallery.pro.databinding.PagerVideoItemBinding
import com.simplemobiletools.gallery.pro.extensions.config
import com.simplemobiletools.gallery.pro.extensions.hasNavBar
import com.simplemobiletools.gallery.pro.extensions.parseFileChannel
import com.simplemobiletools.gallery.pro.helpers.*
import com.simplemobiletools.gallery.pro.models.Medium
import java.io.File
import java.io.FileInputStream

class VideoFragment : ViewPagerFragment(), TextureView.SurfaceTextureListener, SeekBar.OnSeekBarChangeListener {
    private val PROGRESS = "progress"

    private var mIsFullscreen = false
    private var mWasFragmentInit = false
    private var mIsPanorama = false
    private var mIsFragmentVisible = false
    private var mIsDragged = false
    private var mWasVideoStarted = false
    private var mWasPlayerInited = false
    private var mWasLastPositionRestored = false
    private var mPlayOnPrepared = false
    private var mIsPlayerPrepared = false
    private var mCurrTime = 0
    private var mDuration = 0
    private var mPositionWhenInit = 0
    private var mPositionAtPause = 0L
    var mIsPlaying = false

    private var mExoPlayer: SimpleExoPlayer? = null
    private var mVideoSize = Point(1, 1)
    private var mTimerHandler = Handler()

    private var mStoredShowExtendedDetails = false
    private var mStoredHideExtendedDetails = false
    private var mStoredBottomActions = true
    private var mStoredExtendedDetails = 0
    private var mStoredRememberLastVideoPosition = false

    private var _videoItemBinding: PagerVideoItemBinding? = null
    private val videoItemBinding get() = _videoItemBinding!!

    private var _videoTimeHolderBinding: BottomVideoTimeHolderBinding? = null
    private val videoTimeHolderBinding get() = _videoTimeHolderBinding!!

    private lateinit var mMedium: Medium
    private lateinit var mConfig: Config

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mMedium = arguments!!.getSerializable(MEDIUM) as Medium
        mConfig = context!!.config
        _videoItemBinding = PagerVideoItemBinding.inflate(inflater, container, false)
        _videoTimeHolderBinding = videoItemBinding.bottomVideoTimeHolder

        videoItemBinding.panoramaOutline.setOnClickListener { openPanorama() }
        videoTimeHolderBinding.videoCurrTime.setOnClickListener { skip(false) }
        videoTimeHolderBinding.videoDuration.setOnClickListener { skip(true) }
        videoItemBinding.videoHolder.setOnClickListener { toggleFullscreen() }
        videoItemBinding.videoPreview.setOnClickListener { toggleFullscreen() }
        videoItemBinding.videoSurfaceFrame.controller.settings.swallowDoubleTaps = true

        videoItemBinding.videoPlayOutline.setOnClickListener {
            if (mConfig.openVideosOnSeparateScreen) {
                launchVideoPlayer()
            } else {
                togglePlayPause()
            }
        }

        videoTimeHolderBinding.videoTogglePlayPause.setOnClickListener {
            togglePlayPause()
        }

        videoTimeHolderBinding.videoSeekbar.setOnSeekBarChangeListener(this)
        // adding an empty click listener just to avoid ripple animation at toggling fullscreen
        videoTimeHolderBinding.videoSeekbar.setOnClickListener { }

        videoItemBinding.videoSurface.surfaceTextureListener = this@VideoFragment

        val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                if (!mConfig.allowInstantChange) {
                    toggleFullscreen()
                    return true
                }

                val viewWidth = videoItemBinding.root.width
                val instantWidth = viewWidth / 7
                val clickedX = e?.rawX ?: 0f
                when {
                    clickedX <= instantWidth -> listener?.goToPrevItem()
                    clickedX >= viewWidth - instantWidth -> listener?.goToNextItem()
                    else -> toggleFullscreen()
                }
                return true
            }

            override fun onDoubleTap(e: MotionEvent?): Boolean {
                if (e != null) {
                    handleDoubleTap(e.rawX)
                }

                return true
            }
        })

        videoItemBinding.videoPreview.setOnTouchListener { view, event ->
            handleEvent(event)
            false
        }

        videoItemBinding.videoSurfaceFrame.setOnTouchListener { view, event ->
            if (videoItemBinding.videoSurfaceFrame.controller.state.zoom == 1f) {
                handleEvent(event)
            }

            gestureDetector.onTouchEvent(event)
            false
        }

        if (!arguments!!.getBoolean(SHOULD_INIT_FRAGMENT, true)) {
            return videoItemBinding.root
        }

        storeStateVariables()
        Glide.with(context!!).load(mMedium.path).into(videoItemBinding.videoPreview)

        // setMenuVisibility is not called at VideoActivity (third party intent)
        if (!mIsFragmentVisible && activity is VideoActivity) {
            mIsFragmentVisible = true
        }

        mIsFullscreen = activity!!.window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN == View.SYSTEM_UI_FLAG_FULLSCREEN
        initTimeHolder()
        checkIfPanorama()

        ensureBackgroundThread {
            activity?.getVideoResolution(mMedium.path)?.apply {
                mVideoSize.x = x
                mVideoSize.y = y
            }
        }

        if (mIsPanorama) {
            videoItemBinding.panoramaOutline.beVisible()
            videoItemBinding.videoPlayOutline.beGone()
            videoItemBinding.videoVolumeController.beGone()
            videoItemBinding.videoBrightnessController.beGone()
            Glide.with(context!!).load(mMedium.path).into(videoItemBinding.videoPreview)
        }

        if (!mIsPanorama) {
            if (savedInstanceState != null) {
                mCurrTime = savedInstanceState.getInt(PROGRESS)
            }

            mWasFragmentInit = true
            setVideoSize()

            videoItemBinding.videoBrightnessController.initialize(activity!!, videoItemBinding.slideInfo,
                    true, container, singleTap = { x, y ->
                if (mConfig.allowInstantChange) {
                    listener?.goToPrevItem()
                } else {
                    toggleFullscreen()
                }
            }, doubleTap = { x, y ->
                doSkip(false)
            })

            videoItemBinding.videoVolumeController.initialize(activity!!, videoItemBinding.slideInfo,
                    false, container, singleTap = { x, y ->
                if (mConfig.allowInstantChange) {
                    listener?.goToNextItem()
                } else {
                    toggleFullscreen()
                }
            }, doubleTap = { x, y ->
                doSkip(true)
            })

            videoItemBinding.videoSurface.onGlobalLayout {
                if (mIsFragmentVisible && mConfig.autoplayVideos && !mConfig.openVideosOnSeparateScreen) {
                    playVideo()
                }
            }
        }

        setupVideoDuration()
        if (mStoredRememberLastVideoPosition) {
            restoreLastVideoSavedPosition()
        }

        return videoItemBinding.root
    }

    override fun onResume() {
        super.onResume()
        mConfig = context!!.config      // make sure we get a new config, in case the user changed something in the app settings
        activity!!.updateTextColors(videoItemBinding.videoHolder)
        val allowVideoGestures = mConfig.allowVideoGestures
        videoItemBinding.videoSurface.beGoneIf(mConfig.openVideosOnSeparateScreen || mIsPanorama)
        videoItemBinding.videoSurfaceFrame.beGoneIf(videoItemBinding.videoSurface.isGone())

        videoItemBinding.videoVolumeController.beVisibleIf(allowVideoGestures && !mIsPanorama)
        videoItemBinding.videoBrightnessController.beVisibleIf(allowVideoGestures && !mIsPanorama)

        checkExtendedDetails()
        initTimeHolder()
        storeStateVariables()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _videoItemBinding = null
        _videoTimeHolderBinding = null
    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
        pauseVideo()
        if (mStoredRememberLastVideoPosition && mIsFragmentVisible && mWasVideoStarted) {
            saveVideoProgress()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (activity?.isChangingConfigurations == false) {
            cleanup()
        }
    }

    override fun setMenuVisibility(menuVisible: Boolean) {
        super.setMenuVisibility(menuVisible)
        if (mIsFragmentVisible && !menuVisible) {
            pauseVideo()
        }

        mIsFragmentVisible = menuVisible
        if (mWasFragmentInit && menuVisible && mConfig.autoplayVideos && !mConfig.openVideosOnSeparateScreen) {
            playVideo()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setVideoSize()
        initTimeHolder()
        checkExtendedDetails()
        videoItemBinding.videoSurfaceFrame.onGlobalLayout {
            videoItemBinding.videoSurfaceFrame.controller.resetState()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(PROGRESS, mCurrTime)
    }

    private fun storeStateVariables() {
        mConfig.apply {
            mStoredShowExtendedDetails = showExtendedDetails
            mStoredHideExtendedDetails = hideExtendedDetails
            mStoredExtendedDetails = extendedDetails
            mStoredBottomActions = bottomActions
            mStoredRememberLastVideoPosition = rememberLastVideoPosition
        }
    }

    private fun saveVideoProgress() {
        if (!videoEnded()) {
            if (mExoPlayer != null) {
                mConfig.saveLastVideoPosition(mMedium.path, mExoPlayer!!.currentPosition.toInt() / 1000)
            } else {
                mConfig.saveLastVideoPosition(mMedium.path, mPositionAtPause.toInt() / 1000)
            }
        }
    }

    private fun restoreLastVideoSavedPosition() {
        val pos = mConfig.getLastVideoPosition(mMedium.path)
        if (pos > 0) {
            mPositionAtPause = pos * 1000L
            setPosition(pos)
        }
    }

    private fun setupTimeHolder() {
        videoTimeHolderBinding.videoSeekbar.max = mDuration
        videoTimeHolderBinding.videoDuration.text = mDuration.getFormattedDuration()
        setupTimer()
    }

    private fun setupTimer() {
        activity?.runOnUiThread(object : Runnable {
            override fun run() {
                if (mExoPlayer != null && !mIsDragged && mIsPlaying) {
                    mCurrTime = (mExoPlayer!!.currentPosition / 1000).toInt()
                    videoTimeHolderBinding.videoSeekbar.progress = mCurrTime
                    videoTimeHolderBinding.videoCurrTime.text = mCurrTime.getFormattedDuration()
                }

                mTimerHandler.postDelayed(this, 1000)
            }
        })
    }

    private fun initExoPlayer() {
        if (activity == null || mConfig.openVideosOnSeparateScreen || mIsPanorama || mExoPlayer != null) {
            return
        }

        mExoPlayer = ExoPlayerFactory.newSimpleInstance(context)
        mExoPlayer!!.seekParameters = SeekParameters.CLOSEST_SYNC
        if (mConfig.loopVideos && listener?.isSlideShowActive() == false) {
            mExoPlayer?.repeatMode = Player.REPEAT_MODE_ONE
        }

        val isContentUri = mMedium.path.startsWith("content://")
        val uri = if (isContentUri) Uri.parse(mMedium.path) else Uri.fromFile(File(mMedium.path))
        val dataSpec = DataSpec(uri)
        val fileDataSource = if (isContentUri) ContentDataSource(context) else FileDataSource()
        try {
            fileDataSource.open(dataSpec)
        } catch (e: Exception) {
            activity?.showErrorToast(e)
            return
        }

        val factory = DataSource.Factory { fileDataSource }
        val audioSource = ExtractorMediaSource(fileDataSource.uri, factory, DefaultExtractorsFactory(), null, null)
        mPlayOnPrepared = true
        mExoPlayer!!.audioStreamType = C.STREAM_TYPE_MUSIC
        mExoPlayer!!.prepare(audioSource)

        if (videoItemBinding.videoSurface.surfaceTexture != null) {
            mExoPlayer!!.setVideoSurface(Surface(videoItemBinding.videoSurface.surfaceTexture))
        }

        mExoPlayer!!.addListener(object : Player.EventListener {
            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {}

            override fun onSeekProcessed() {}

            override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {}

            override fun onPlayerError(error: ExoPlaybackException?) {}

            override fun onLoadingChanged(isLoading: Boolean) {}

            override fun onPositionDiscontinuity(reason: Int) {
                // Reset progress views when video loops.
                if (reason == Player.DISCONTINUITY_REASON_PERIOD_TRANSITION) {
                    videoTimeHolderBinding.videoSeekbar.progress = 0
                    videoTimeHolderBinding.videoCurrTime.text = 0.getFormattedDuration()
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

        mExoPlayer!!.addVideoListener(object : SimpleExoPlayer.VideoListener {
            override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                mVideoSize.x = width
                mVideoSize.y = (height / pixelWidthHeightRatio).toInt()
                setVideoSize()
            }

            override fun onRenderedFirstFrame() {}
        })
    }

    private fun launchVideoPlayer() {
        listener?.launchViewVideoIntent(mMedium.path)
    }

    private fun toggleFullscreen() {
        listener?.fragmentClicked()
    }

    private fun handleDoubleTap(x: Float) {
        val viewWidth = videoItemBinding.root.width
        val instantWidth = viewWidth / 7
        when {
            x <= instantWidth -> doSkip(false)
            x >= viewWidth - instantWidth -> doSkip(true)
            else -> togglePlayPause()
        }
    }

    private fun checkExtendedDetails() {
        if (mConfig.showExtendedDetails) {
            videoItemBinding.videoDetails.apply {
                beInvisible()   // make it invisible so we can measure it, but not show yet
                text = getMediumExtendedDetails(mMedium)
                onGlobalLayout {
                    if (isAdded) {
                        val realY = getExtendedDetailsY(height)
                        if (realY > 0) {
                            y = realY
                            beVisibleIf(text.isNotEmpty())
                            alpha = if (!mConfig.hideExtendedDetails || !mIsFullscreen) 1f else 0f
                        }
                    }
                }
            }
        } else {
            videoItemBinding.videoDetails.beGone()
        }
    }

    private fun initTimeHolder() {
        var right = 0
        var bottom = context!!.navigationBarHeight
        if (mConfig.bottomActions) {
            bottom += resources.getDimension(R.dimen.bottom_actions_height).toInt()
        }

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && activity?.hasNavBar() == true) {
            right += activity!!.navigationBarWidth
        }

        (videoTimeHolderBinding.root.layoutParams as RelativeLayout.LayoutParams).apply {
            bottomMargin = bottom
            rightMargin = right
        }
        videoTimeHolderBinding.root.beInvisibleIf(mIsFullscreen)
    }

    private fun checkIfPanorama() {
        try {
            val fis = FileInputStream(File(mMedium.path))
            fis.use {
                context!!.parseFileChannel(mMedium.path, it.channel, 0, 0, 0) {
                    mIsPanorama = true
                }
            }
        } catch (ignored: Exception) {
        } catch (ignored: OutOfMemoryError) {
        }
    }

    private fun openPanorama() {
        Intent(context, PanoramaVideoActivity::class.java).apply {
            putExtra(PATH, mMedium.path)
            startActivity(this)
        }
    }

    override fun fullscreenToggled(isFullscreen: Boolean) {
        mIsFullscreen = isFullscreen
        val newAlpha = if (isFullscreen) 0f else 1f
        if (!mIsFullscreen) {
            videoTimeHolderBinding.root.beVisible()
        }

        videoTimeHolderBinding.videoSeekbar.setOnSeekBarChangeListener(if (mIsFullscreen) null else this)
        arrayOf(videoTimeHolderBinding.videoCurrTime, videoTimeHolderBinding.videoDuration,
                videoTimeHolderBinding.videoTogglePlayPause).forEach {
            it.isClickable = !mIsFullscreen
        }

        videoTimeHolderBinding.root.animate().alpha(newAlpha).start()
        videoItemBinding.videoDetails.apply {
            if (mStoredShowExtendedDetails && isVisible() && context != null && resources != null) {
                animate().y(getExtendedDetailsY(height))

                if (mStoredHideExtendedDetails) {
                    animate().alpha(newAlpha).start()
                }
            }
        }
    }

    private fun getExtendedDetailsY(height: Int): Float {
        val smallMargin = context?.resources?.getDimension(R.dimen.small_margin) ?: return 0f
        val fullscreenOffset = smallMargin + if (mIsFullscreen) 0 else context!!.navigationBarHeight
        var actionsHeight = 0f
        if (!mIsFullscreen) {
            actionsHeight += resources.getDimension(R.dimen.video_player_play_pause_size)
            if (mConfig.bottomActions) {
                actionsHeight += resources.getDimension(R.dimen.bottom_actions_height)
            }
        }
        return context!!.realScreenSize.y - height - actionsHeight - fullscreenOffset
    }

    private fun skip(forward: Boolean) {
        if (mIsPanorama) {
            return
        } else if (mExoPlayer == null) {
            playVideo()
            return
        }

        mPositionAtPause = 0L
        doSkip(forward)
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

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (fromUser) {
            if (mExoPlayer != null) {
                if (!mWasPlayerInited) {
                    mPositionWhenInit = progress
                }
                setPosition(progress)
            }

            if (mExoPlayer == null) {
                mPositionAtPause = progress * 1000L
                playVideo()
            }
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        if (mExoPlayer == null) {
            return
        }

        mExoPlayer!!.playWhenReady = false
        mIsDragged = true
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (mIsPanorama) {
            openPanorama()
            return
        }

        if (mExoPlayer == null) {
            return
        }

        if (mIsPlaying) {
            mExoPlayer!!.playWhenReady = true
        } else {
            playVideo()
        }

        mIsDragged = false
    }

    private fun togglePlayPause() {
        if (activity == null || !isAdded) {
            return
        }

        if (mIsPlaying) {
            pauseVideo()
        } else {
            playVideo()
        }
    }

    fun playVideo() {
        if (mExoPlayer == null) {
            initExoPlayer()
            return
        }

        if (videoItemBinding.videoPreview.isVisible()) {
            videoItemBinding.videoPreview.beGone()
            initExoPlayer()
        }

        val wasEnded = videoEnded()
        if (wasEnded) {
            setPosition(0)
        }

        if (mStoredRememberLastVideoPosition && !mWasLastPositionRestored) {
            mWasLastPositionRestored = true
            restoreLastVideoSavedPosition()
        }

        if (!wasEnded || !mConfig.loopVideos) {
            videoTimeHolderBinding.videoTogglePlayPause.setImageResource(R.drawable.ic_pause_outline_vector)
        }

        if (!mWasVideoStarted) {
            videoItemBinding.videoPlayOutline.beGone()
            videoTimeHolderBinding.videoTogglePlayPause.beVisible()
        }

        mWasVideoStarted = true
        if (mIsPlayerPrepared) {
            mIsPlaying = true
        }
        mExoPlayer?.playWhenReady = true
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun pauseVideo() {
        if (mExoPlayer == null) {
            return
        }

        mIsPlaying = false
        if (!videoEnded()) {
            mExoPlayer?.playWhenReady = false
        }

        videoTimeHolderBinding.videoTogglePlayPause.setImageResource(R.drawable.ic_play_outline_vector)
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        mPositionAtPause = mExoPlayer?.currentPosition ?: 0L
        releaseExoPlayer()
    }

    private fun videoEnded(): Boolean {
        val currentPos = mExoPlayer?.currentPosition ?: 0
        val duration = mExoPlayer?.duration ?: 0
        return currentPos != 0L && currentPos >= duration
    }

    private fun setPosition(seconds: Int) {
        mExoPlayer?.seekTo(seconds * 1000L)
        videoTimeHolderBinding.videoSeekbar.progress = seconds
        videoTimeHolderBinding.videoCurrTime.text = seconds.getFormattedDuration()

        if (!mIsPlaying) {
            mPositionAtPause = mExoPlayer?.currentPosition ?: 0L
        }
    }

    private fun setupVideoDuration() {
        ensureBackgroundThread {
            mDuration = context?.getDuration(mMedium.path) ?: 0

            activity?.runOnUiThread {
                setupTimeHolder()
                setPosition(0)
            }
        }
    }

    private fun videoPrepared() {
        if (mDuration == 0) {
            mDuration = (mExoPlayer!!.duration / 1000).toInt()
            setupTimeHolder()
            setPosition(mCurrTime)

            if (mIsFragmentVisible && (mConfig.autoplayVideos)) {
                playVideo()
            }
        }

        if (mPositionWhenInit != 0 && !mWasPlayerInited) {
            setPosition(mPositionWhenInit)
            mPositionWhenInit = 0
        }

        mIsPlayerPrepared = true
        if (mPlayOnPrepared && !mIsPlaying) {
            if (mPositionAtPause != 0L) {
                mExoPlayer?.seekTo(mPositionAtPause)
                mPositionAtPause = 0L
            }
            playVideo()
        }
        mWasPlayerInited = true
        mPlayOnPrepared = false
    }

    private fun videoCompleted() {
        if (!isAdded || mExoPlayer == null) {
            return
        }

        mCurrTime = (mExoPlayer!!.duration / 1000).toInt()
        if (listener?.videoEnded() == false && mConfig.loopVideos) {
            playVideo()
        } else {
            videoTimeHolderBinding.videoSeekbar.progress = videoTimeHolderBinding.videoSeekbar.max
            videoTimeHolderBinding.videoCurrTime.text = mDuration.getFormattedDuration()
            pauseVideo()
        }
    }

    private fun cleanup() {
        pauseVideo()
        releaseExoPlayer()

        if (mWasFragmentInit) {
            videoTimeHolderBinding.videoCurrTime.text = 0.getFormattedDuration()
            videoTimeHolderBinding.videoSeekbar.progress = 0
            mTimerHandler.removeCallbacksAndMessages(null)
        }
    }

    private fun releaseExoPlayer() {
        mIsPlayerPrepared = false
        mExoPlayer?.stop()
        ensureBackgroundThread {
            mExoPlayer?.release()
            mExoPlayer = null
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {}

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {}

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?) = false

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        ensureBackgroundThread {
            mExoPlayer?.setVideoSurface(Surface(videoItemBinding.videoSurface.surfaceTexture))
        }
    }

    private fun setVideoSize() {
        if (activity == null || mConfig.openVideosOnSeparateScreen) {
            return
        }

        val videoProportion = mVideoSize.x.toFloat() / mVideoSize.y.toFloat()
        val display = activity!!.windowManager.defaultDisplay
        val screenWidth: Int
        val screenHeight: Int

        val realMetrics = DisplayMetrics()
        display.getRealMetrics(realMetrics)
        screenWidth = realMetrics.widthPixels
        screenHeight = realMetrics.heightPixels

        val screenProportion = screenWidth.toFloat() / screenHeight.toFloat()

        videoItemBinding.videoSurface.layoutParams.apply {
            if (videoProportion > screenProportion) {
                width = screenWidth
                height = (screenWidth.toFloat() / videoProportion).toInt()
            } else {
                width = (videoProportion * screenHeight.toFloat()).toInt()
                height = screenHeight
            }
            videoItemBinding.videoSurface.layoutParams = this
        }
    }
}
