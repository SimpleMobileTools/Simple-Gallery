package com.simplemobiletools.gallery.fragments

import android.content.res.Configuration
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.ContentDataSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.FileDataSource
import com.google.android.exoplayer2.video.VideoListener
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.VideoActivity
import com.simplemobiletools.gallery.extensions.*
import com.simplemobiletools.gallery.helpers.MEDIUM
import com.simplemobiletools.gallery.helpers.MediaSideScroll
import com.simplemobiletools.gallery.models.Medium
import kotlinx.android.synthetic.main.pager_video_item.view.*
import java.io.File

class VideoFragment : ViewPagerFragment(), TextureView.SurfaceTextureListener, SeekBar.OnSeekBarChangeListener {
    private val PROGRESS = "progress"
    private val MIN_SKIP_LENGTH = 2000
    private val HIDE_PAUSE_DELAY = 2000L
    private val PLAY_PAUSE_VISIBLE_ALPHA = 0.8f

    private var mTextureView: TextureView? = null
    private var mCurrTimeView: TextView? = null
    private var mSeekBar: SeekBar? = null
    private var mTimeHolder: View? = null
    private var mView: View? = null
    private var mExoPlayer: SimpleExoPlayer? = null
    private var mVideoSize = Point(0, 0)
    private var mTimerHandler = Handler()
    private var mHidePauseHandler = Handler()

    private var mIsPlaying = false
    private var mIsDragged = false
    private var mIsFullscreen = false
    private var mIsFragmentVisible = false
    private var mWasFragmentInit = false
    private var mIsExoPlayerInitialized = false
    private var mCurrTime = 0
    private var mDuration = 0

    private var mStoredShowExtendedDetails = false
    private var mStoredHideExtendedDetails = false
    private var mStoredBottomActions = true
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
        mWasFragmentInit = true

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
            Glide.with(context!!).load(medium.path).into(video_preview)
        }

        mExoPlayer = ExoPlayerFactory.newSimpleInstance(context, DefaultTrackSelector())
        mExoPlayer!!.seekParameters = SeekParameters.CLOSEST_SYNC
        initExoPlayerListeners()

        medium.path.getVideoResolution()?.apply {
            mVideoSize.x = x
            mVideoSize.y = y
            setVideoSize()
        }

        setupVideoDuration()

        mView!!.video_surface.onGlobalLayout {
            if (mIsFragmentVisible && context?.config?.autoplayVideos == true) {
                playVideo()
            }
        }

        return mView
    }

    override fun onResume() {
        super.onResume()
        activity!!.updateTextColors(mView!!.video_holder)
        val config = context!!.config
        val allowVideoGestures = config.allowVideoGestures
        val allowInstantChange = config.allowInstantChange
        mView!!.apply {
            video_volume_controller.beVisibleIf(allowVideoGestures)
            video_brightness_controller.beVisibleIf(allowVideoGestures)

            instant_prev_item.beVisibleIf(allowInstantChange)
            instant_next_item.beVisibleIf(allowInstantChange)
        }

        if (config.showExtendedDetails != mStoredShowExtendedDetails || config.extendedDetails != mStoredExtendedDetails) {
            checkExtendedDetails()
        }

        if (config.bottomActions != mStoredBottomActions) {
            initTimeHolder()
        }

        mView!!.video_time_holder.setBackgroundResource(if (config.bottomActions) 0 else R.drawable.gradient_background)
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

    override fun setMenuVisibility(menuVisible: Boolean) {
        super.setMenuVisibility(menuVisible)
        if (mIsFragmentVisible && !menuVisible) {
            pauseVideo()
        }

        mIsFragmentVisible = menuVisible
        if (mWasFragmentInit && menuVisible && context?.config?.autoplayVideos == true) {
            playVideo()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setVideoSize()
        initTimeHolder()
        checkExtendedDetails()
    }

    private fun storeStateVariables() {
        context!!.config.apply {
            mStoredShowExtendedDetails = showExtendedDetails
            mStoredHideExtendedDetails = hideExtendedDetails
            mStoredExtendedDetails = extendedDetails
            mStoredBottomActions = bottomActions
        }
    }

    private fun setupPlayer() {
        if (activity == null)
            return

        mView!!.video_play_outline.setOnClickListener { togglePlayPause() }

        mTextureView = mView!!.video_surface
        mTextureView!!.setOnClickListener { toggleFullscreen() }
        mTextureView!!.surfaceTextureListener = this
        mView!!.video_holder.setOnClickListener { toggleFullscreen() }

        initTimeHolder()
        checkExtendedDetails()
    }

    private fun initExoPlayer() {
        val isContentUri = medium.path.startsWith("content://")
        val uri = if (isContentUri) Uri.parse(medium.path) else Uri.fromFile(File(medium.path))
        val dataSpec = DataSpec(uri)
        val fileDataSource = if (isContentUri) ContentDataSource(context) else FileDataSource()
        try {
            fileDataSource.open(dataSpec)
        } catch (e: Exception) {
            activity?.showErrorToast(e)
        }

        val factory = DataSource.Factory { fileDataSource }
        val audioSource = ExtractorMediaSource(fileDataSource.uri, factory, DefaultExtractorsFactory(), null, null)
        mExoPlayer!!.audioStreamType = AudioManager.STREAM_MUSIC
        mExoPlayer!!.prepare(audioSource)
    }

    private fun initExoPlayerListeners() {
        mExoPlayer!!.addListener(object : Player.EventListener {
            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {}

            override fun onSeekProcessed() {}

            override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {}

            override fun onPlayerError(error: ExoPlaybackException?) {
                mIsExoPlayerInitialized = false
            }

            override fun onLoadingChanged(isLoading: Boolean) {}

            override fun onPositionDiscontinuity(reason: Int) {}

            override fun onRepeatModeChanged(repeatMode: Int) {}

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {}

            override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {}

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                mIsExoPlayerInitialized = playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED
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
        }

        if (context!!.config.bottomActions) {
            bottom += resources.getDimension(R.dimen.bottom_actions_height).toInt()
        }

        mTimeHolder!!.setPadding(left, top, right, bottom)

        mCurrTimeView = mView!!.video_curr_time
        mSeekBar = mView!!.video_seekbar
        mSeekBar!!.setOnSeekBarChangeListener(this)

        if (mIsFullscreen) {
            mTimeHolder!!.beInvisible()
        }
    }

    private fun hasNavBar(): Boolean {
        val display = context!!.windowManager.defaultDisplay

        val realDisplayMetrics = DisplayMetrics()
        display.getRealMetrics(realDisplayMetrics)

        val realHeight = realDisplayMetrics.heightPixels
        val realWidth = realDisplayMetrics.widthPixels

        val displayMetrics = DisplayMetrics()
        display.getMetrics(displayMetrics)

        val displayHeight = displayMetrics.heightPixels
        val displayWidth = displayMetrics.widthPixels

        return realWidth - displayWidth > 0 || realHeight - displayHeight > 0
    }

    private fun setupTimeHolder() {
        mSeekBar!!.max = mDuration
        mView!!.video_duration.text = mDuration.getFormattedDuration()
        setupTimer()
    }

    private fun setupTimer() {
        activity!!.runOnUiThread(object : Runnable {
            override fun run() {
                if (mExoPlayer != null && !mIsDragged && mIsPlaying) {
                    mCurrTime = (mExoPlayer!!.currentPosition / 1000).toInt()
                    mSeekBar!!.progress = mCurrTime
                    mCurrTimeView!!.text = mCurrTime.getFormattedDuration()
                }

                mTimerHandler.postDelayed(this, 1000)
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

        mIsPlaying = !mIsPlaying
        mHidePauseHandler.removeCallbacksAndMessages(null)
        if (mIsPlaying) {
            playVideo()
        } else {
            pauseVideo()
        }
    }

    fun playVideo() {
        if (mExoPlayer == null) {
            return
        }

        if (mView!!.video_preview.isVisible()) {
            mView!!.video_preview.beGone()
            initExoPlayer()
        }

        if (videoEnded()) {
            setProgress(0)
        }

        mIsPlaying = true
        mExoPlayer?.playWhenReady = true
        mView!!.video_play_outline.setImageResource(R.drawable.ic_pause)
        activity!!.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        schedulePlayPauseFadeOut()
    }

    private fun pauseVideo() {
        if (mExoPlayer == null) {
            return
        }

        mIsPlaying = false
        if (!videoEnded()) {
            mExoPlayer?.playWhenReady = false
        }

        mView?.video_play_outline?.setImageResource(R.drawable.ic_play)
        mView?.video_play_outline?.alpha = PLAY_PAUSE_VISIBLE_ALPHA
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        schedulePlayPauseFadeOut()
    }

    private fun schedulePlayPauseFadeOut() {
        mHidePauseHandler.removeCallbacksAndMessages(null)
        mHidePauseHandler.postDelayed({
            mView!!.video_play_outline.animate().alpha(0f).start()
        }, HIDE_PAUSE_DELAY)
    }

    private fun videoEnded() = mExoPlayer?.currentPosition ?: 0 >= mExoPlayer?.duration ?: 0

    private fun setProgress(seconds: Int) {
        mExoPlayer!!.seekTo(seconds * 1000L)
        mSeekBar!!.progress = seconds
        mCurrTimeView!!.text = seconds.getFormattedDuration()
    }

    private fun setupVideoDuration() {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(medium.path)
            mDuration = Math.round(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toInt() / 1000f)
        } catch (ignored: Exception) {
        }

        setupTimeHolder()
        setProgress(0)
    }

    private fun videoPrepared() {
        if (mDuration == 0) {
            mDuration = (mExoPlayer!!.duration / 1000).toInt()
            setupTimeHolder()
            setProgress(mCurrTime)

            if (mIsFragmentVisible && (context!!.config.autoplayVideos)) {
                playVideo()
            }
        }
    }

    private fun videoCompleted() {
        if (!isAdded || mExoPlayer == null) {
            return
        }

        mCurrTime = (mExoPlayer!!.duration / 1000).toInt()
        if (listener?.videoEnded() == false && context!!.config.loopVideos) {
            playVideo()
        } else {
            mSeekBar!!.progress = mSeekBar!!.max
            mCurrTimeView!!.text = mDuration.getFormattedDuration()
            pauseVideo()
        }
    }

    private fun cleanup() {
        pauseVideo()
        mCurrTimeView?.text = 0.getFormattedDuration()
        releaseExoPlayer()
        mSeekBar?.progress = 0
        mTimerHandler.removeCallbacksAndMessages(null)
        mHidePauseHandler.removeCallbacksAndMessages(null)
        mTextureView = null
    }

    private fun releaseExoPlayer() {
        mExoPlayer?.stop()
        Thread {
            mExoPlayer?.release()
            mExoPlayer = null
        }.start()
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {}

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {}

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?) = false

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        Thread {
            mExoPlayer?.setVideoSurface(Surface(mTextureView!!.surfaceTexture))
        }.start()
    }

    private fun setVideoSize() {
        if (activity == null || mTextureView == null)
            return

        val videoProportion = mVideoSize.x.toFloat() / mVideoSize.y.toFloat()
        val display = activity!!.windowManager.defaultDisplay
        val screenWidth: Int
        val screenHeight: Int

        val realMetrics = DisplayMetrics()
        display.getRealMetrics(realMetrics)
        screenWidth = realMetrics.widthPixels
        screenHeight = realMetrics.heightPixels

        val screenProportion = screenWidth.toFloat() / screenHeight.toFloat()

        mTextureView!!.layoutParams.apply {
            if (videoProportion > screenProportion) {
                width = screenWidth
                height = (screenWidth.toFloat() / videoProportion).toInt()
            } else {
                width = (videoProportion * screenHeight.toFloat()).toInt()
                height = screenHeight
            }
            mTextureView!!.layoutParams = this
        }
    }

    private fun checkExtendedDetails() {
        if (context!!.config.showExtendedDetails) {
            mView!!.video_details.apply {
                beInvisible()   // make it invisible so we can measure it, but not show yet
                text = getMediumExtendedDetails(medium)
                onGlobalLayout {
                    if (isAdded) {
                        val realY = getExtendedDetailsY(height)
                        if (realY > 0) {
                            y = realY
                            beVisibleIf(text.isNotEmpty())
                            alpha = if (!context!!.config.hideExtendedDetails || !mIsFullscreen) 1f else 0f
                        }
                    }
                }
            }
        } else {
            mView!!.video_details.beGone()
        }
    }

    private fun skip(forward: Boolean) {
        if (mExoPlayer == null) {
            return
        }

        val curr = mExoPlayer!!.currentPosition
        val twoPercents = Math.max((mExoPlayer!!.duration / 50).toInt(), MIN_SKIP_LENGTH)
        val newProgress = if (forward) curr + twoPercents else curr - twoPercents
        val roundProgress = Math.round(newProgress / 1000f)
        val limitedProgress = Math.max(Math.min(mExoPlayer!!.duration.toInt(), roundProgress), 0)
        setProgress(limitedProgress)
        if (!mIsPlaying) {
            togglePlayPause()
        }
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (mExoPlayer != null && fromUser) {
            setProgress(progress)
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        if (mExoPlayer == null)
            return

        mExoPlayer!!.playWhenReady = false
        mIsDragged = true
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (mExoPlayer == null)
            return

        if (!mIsPlaying) {
            togglePlayPause()
        } else {
            mExoPlayer!!.playWhenReady = true
        }

        mIsDragged = false
    }

    override fun fullscreenToggled(isFullscreen: Boolean) {
        mIsFullscreen = isFullscreen
        checkFullscreen()
        mView!!.video_details.apply {
            if (mStoredShowExtendedDetails && isVisible()) {
                animate().y(getExtendedDetailsY(height))

                if (mStoredHideExtendedDetails) {
                    animate().alpha(if (isFullscreen) 0f else 1f).start()
                }
            }
        }
    }

    private fun getExtendedDetailsY(height: Int): Float {
        val smallMargin = resources.getDimension(R.dimen.small_margin)
        val fullscreenOffset = smallMargin + if (mIsFullscreen) 0 else mTimeHolder!!.height
        return context!!.realScreenSize.y.toFloat() - height - fullscreenOffset
    }
}
