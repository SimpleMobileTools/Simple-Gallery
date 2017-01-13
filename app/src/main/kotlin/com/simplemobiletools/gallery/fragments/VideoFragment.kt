package com.simplemobiletools.gallery.fragments

import android.content.res.Configuration
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaPlayer.OnPreparedListener
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import android.widget.TextView
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.extensions.getNavBarHeight
import com.simplemobiletools.gallery.extensions.hasNavBar
import com.simplemobiletools.gallery.helpers.MEDIUM
import com.simplemobiletools.gallery.models.Medium
import kotlinx.android.synthetic.main.pager_video_item.view.*
import java.io.IOException
import java.util.*

class VideoFragment : ViewPagerFragment(), View.OnClickListener, SurfaceHolder.Callback, MediaPlayer.OnCompletionListener,
        MediaPlayer.OnVideoSizeChangedListener, SeekBar.OnSeekBarChangeListener, OnPreparedListener {

    private var mMediaPlayer: MediaPlayer? = null
    private var mSurfaceView: SurfaceView? = null
    private var mSurfaceHolder: SurfaceHolder? = null
    private var mCurrTimeView: TextView? = null
    private var mTimerHandler: Handler? = null
    private var mSeekBar: SeekBar? = null
    private var mTimeHolder: View? = null

    private var mIsPlaying = false
    private var mIsDragged = false
    private var mIsFullscreen = false
    private var mIsFragmentVisible = false
    private var mCurrTime = 0
    private var mDuration = 0

    lateinit var mView: View
    lateinit var mMedium: Medium

    companion object {
        private val TAG = VideoFragment::class.java.simpleName
        private val PROGRESS = "progress"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.pager_video_item, container, false)

        mMedium = arguments.getSerializable(MEDIUM) as Medium
        if (savedInstanceState != null) {
            mCurrTime = savedInstanceState.getInt(PROGRESS)
        }

        mIsFullscreen = activity.window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN == View.SYSTEM_UI_FLAG_FULLSCREEN
        setupPlayer()
        mView.setOnClickListener(this)

        activity.window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            val fullscreen = visibility and View.SYSTEM_UI_FLAG_FULLSCREEN != 0
            mIsFullscreen = fullscreen
            checkFullscreen()
            listener?.systemUiVisibilityChanged(visibility)
        }

        return mView
    }

    private fun setupPlayer() {
        if (activity == null)
            return

        mView.video_play_outline.setOnClickListener(this)

        mSurfaceView = mView.video_surface
        mSurfaceView!!.setOnClickListener(this)
        mSurfaceHolder = mSurfaceView!!.holder
        mSurfaceHolder!!.addCallback(this)

        initTimeHolder()
    }

    override fun setMenuVisibility(menuVisible: Boolean) {
        super.setMenuVisibility(menuVisible)
        mIsFragmentVisible = menuVisible
        if (menuVisible) {
            if (context != null && context.config.autoplayVideos) {
                playVideo()
            }
        } else {
            if (mIsPlaying)
                pauseVideo()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        setVideoSize()
        initTimeHolder()
    }

    private fun initTimeHolder() {
        mTimeHolder = mView.video_time_holder
        val res = resources
        val height = res.getNavBarHeight()
        val left = mTimeHolder!!.paddingLeft
        val top = mTimeHolder!!.paddingTop
        var right = res.getDimension(R.dimen.timer_padding).toInt()
        var bottom = 0

        if (activity.hasNavBar()) {
            if (res.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                bottom += height
            } else {
                right += height
            }
            mTimeHolder!!.setPadding(left, top, right, bottom)
        }

        mCurrTimeView = mView.video_curr_time
        mSeekBar = mView.video_seekbar
        mSeekBar!!.setOnSeekBarChangeListener(this)

        if (mIsFullscreen)
            mTimeHolder!!.visibility = View.INVISIBLE
    }

    private fun setupTimeHolder() {
        mSeekBar!!.max = mDuration
        mView.video_duration.text = getTimeString(mDuration)
        mTimerHandler = Handler()
        setupTimer()
    }

    private fun setupTimer() {
        activity.runOnUiThread(object : Runnable {
            override fun run() {
                if (mMediaPlayer != null && !mIsDragged && mIsPlaying) {
                    mCurrTime = mMediaPlayer!!.currentPosition / 1000
                    mSeekBar!!.progress = mCurrTime
                    mCurrTimeView!!.text = getTimeString(mCurrTime)
                }

                mTimerHandler!!.postDelayed(this, 1000)
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(PROGRESS, mCurrTime)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.video_play_outline -> togglePlayPause()
            else -> {
                mIsFullscreen = !mIsFullscreen
                checkFullscreen()
                listener?.fragmentClicked()
            }
        }
    }

    private fun checkFullscreen() {
        var anim = android.R.anim.fade_in
        if (mIsFullscreen) {
            anim = android.R.anim.fade_out
            mSeekBar!!.setOnSeekBarChangeListener(null)
        } else {
            mSeekBar!!.setOnSeekBarChangeListener(this)
        }

        AnimationUtils.loadAnimation(context, anim).apply {
            duration = 150
            fillAfter = true
            mTimeHolder!!.startAnimation(this)
        }
    }

    private fun togglePlayPause() {
        if (activity == null || !isAdded)
            return

        mIsPlaying = !mIsPlaying
        if (mIsPlaying) {
            playVideo()
        } else {
            pauseVideo()
        }
    }

    private fun playVideo() {
        mIsPlaying = true
        mMediaPlayer?.start()
        mView.video_play_outline.setImageDrawable(null)
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun pauseVideo() {
        mIsPlaying = false
        mMediaPlayer?.pause()
        mView.video_play_outline.setImageDrawable(resources.getDrawable(R.drawable.img_play_outline_big))
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun initMediaPlayer() {
        if (mMediaPlayer != null)
            return

        try {
            mMediaPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.parse(mMedium.path))
                setDisplay(mSurfaceHolder)
                setOnCompletionListener(this@VideoFragment)
                setOnVideoSizeChangedListener(this@VideoFragment)
                setOnPreparedListener(this@VideoFragment)
                setAudioStreamType(AudioManager.STREAM_MUSIC)
                prepareAsync()
            }
        } catch (e: IOException) {
            Log.e(TAG, "init media player failed $e")
        }
    }

    private fun setProgress(seconds: Int) {
        mMediaPlayer!!.seekTo(seconds * 1000)
        mSeekBar!!.progress = seconds
        mCurrTimeView!!.text = getTimeString(seconds)
    }

    private fun addPreviewImage() {
        mMediaPlayer!!.start()
        mMediaPlayer!!.pause()
    }

    override fun onPause() {
        super.onPause()
        pauseVideo()
        mIsFragmentVisible = false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (activity != null && !activity.isChangingConfigurations) {
            cleanup()
        }
    }

    private fun cleanup() {
        pauseVideo()
        mCurrTimeView?.text = getTimeString(0)
        mMediaPlayer?.release()
        mMediaPlayer = null
        mSeekBar?.progress = 0
        mTimerHandler?.removeCallbacksAndMessages(null)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        initMediaPlayer()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        mMediaPlayer?.release()
        mMediaPlayer = null
    }

    override fun onCompletion(mp: MediaPlayer) {
        if (context.config.loopVideos) {
            playVideo()
        } else {
            mSeekBar!!.progress = mSeekBar!!.max
            mCurrTimeView!!.text = getTimeString(mDuration)
            pauseVideo()
        }
    }

    override fun onVideoSizeChanged(mp: MediaPlayer, width: Int, height: Int) {
        setVideoSize()
    }

    private fun setVideoSize() {
        if (activity == null)
            return

        initMediaPlayer()
        val videoProportion = mMediaPlayer!!.videoWidth.toFloat() / mMediaPlayer!!.videoHeight.toFloat()
        val display = activity.windowManager.defaultDisplay
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

    private fun getTimeString(duration: Int): String {
        val sb = StringBuilder(8)
        val hours = duration / (60 * 60)
        val minutes = duration % (60 * 60) / 60
        val seconds = duration % (60 * 60) % 60

        if (duration > 3600) {
            sb.append(String.format(Locale.getDefault(), "%02d", hours)).append(":")
        }

        sb.append(String.format(Locale.getDefault(), "%02d", minutes))
        sb.append(":").append(String.format(Locale.getDefault(), "%02d", seconds))

        return sb.toString()
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (mMediaPlayer != null && fromUser) {
            setProgress(progress)
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        initMediaPlayer()
        mMediaPlayer!!.pause()
        mIsDragged = true
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (!mIsPlaying) {
            togglePlayPause()
        } else {
            mMediaPlayer!!.start()
        }

        mIsDragged = false
    }

    override fun onPrepared(mp: MediaPlayer) {
        mDuration = mp.duration / 1000
        addPreviewImage()
        setupTimeHolder()
        setProgress(mCurrTime)

        if (mIsFragmentVisible && context.config.autoplayVideos)
            playVideo()
    }
}
