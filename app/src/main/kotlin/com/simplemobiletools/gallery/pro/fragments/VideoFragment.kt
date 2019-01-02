package com.simplemobiletools.gallery.pro.fragments

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.gallery.pro.BuildConfig
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.activities.PanoramaVideoActivity
import com.simplemobiletools.gallery.pro.activities.VideoPlayerActivity
import com.simplemobiletools.gallery.pro.extensions.config
import com.simplemobiletools.gallery.pro.extensions.navigationBarWidth
import com.simplemobiletools.gallery.pro.extensions.portrait
import com.simplemobiletools.gallery.pro.extensions.realScreenSize
import com.simplemobiletools.gallery.pro.helpers.IsoTypeReader
import com.simplemobiletools.gallery.pro.helpers.MEDIUM
import com.simplemobiletools.gallery.pro.helpers.PATH
import com.simplemobiletools.gallery.pro.models.Medium
import com.simplemobiletools.gallery.pro.views.MediaSideScroll
import kotlinx.android.synthetic.main.pager_video_item.view.*
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

class VideoFragment : ViewPagerFragment() {
    private val FILE_CHANNEL_CONTAINERS = arrayListOf("moov", "trak", "mdia", "minf", "udta", "stbl")

    private var mIsFullscreen = false
    private var mWasFragmentInit = false
    private var mIsPanorama = false

    private var mStoredShowExtendedDetails = false
    private var mStoredHideExtendedDetails = false
    private var mStoredBottomActions = true
    private var mStoredExtendedDetails = 0

    private lateinit var mBrightnessSideScroll: MediaSideScroll
    private lateinit var mVolumeSideScroll: MediaSideScroll

    lateinit var mView: View
    lateinit var medium: Medium

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.pager_video_item, container, false).apply {
            instant_prev_item.setOnClickListener { listener?.goToPrevItem() }
            instant_next_item.setOnClickListener { listener?.goToNextItem() }
            video_holder.setOnClickListener { toggleFullscreen() }
            video_preview.setOnClickListener { toggleFullscreen() }
            panorama_outline.setOnClickListener { openPanorama() }
            video_play_outline.setOnClickListener { launchVideoPlayer() }

            mBrightnessSideScroll = video_brightness_controller
            mVolumeSideScroll = video_volume_controller

            if (context.config.allowDownGesture) {
                video_preview.setOnTouchListener { v, event ->
                    handleEvent(event)
                    false
                }
            }
        }

        storeStateVariables()
        medium = arguments!!.getSerializable(MEDIUM) as Medium
        Glide.with(context!!).load(medium.path).into(mView.video_preview)

        mIsFullscreen = activity!!.window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN == View.SYSTEM_UI_FLAG_FULLSCREEN
        checkIfPanorama()

        if (mIsPanorama) {
            mView.apply {
                panorama_outline.beVisible()
                video_play_outline.beGone()
                mVolumeSideScroll.beGone()
                mBrightnessSideScroll.beGone()
                Glide.with(context!!).load(medium.path).into(video_preview)
            }
        }

        if (!mIsPanorama) {
            mWasFragmentInit = true

            mView.apply {
                mBrightnessSideScroll.initialize(activity!!, slide_info, true, container) { x, y ->
                    video_holder.performClick()
                }

                mVolumeSideScroll.initialize(activity!!, slide_info, false, container) { x, y ->
                    video_holder.performClick()
                }
            }
        }

        updateInstantSwitchWidths()
        return mView
    }

    override fun onResume() {
        super.onResume()
        activity!!.updateTextColors(mView.video_holder)
        val config = context!!.config
        val allowVideoGestures = config.allowVideoGestures
        val allowInstantChange = config.allowInstantChange
        mView.apply {
            video_volume_controller.beVisibleIf(allowVideoGestures && !mIsPanorama)
            video_brightness_controller.beVisibleIf(allowVideoGestures && !mIsPanorama)

            instant_prev_item.beVisibleIf(allowInstantChange)
            instant_next_item.beVisibleIf(allowInstantChange)
        }

        if (config.showExtendedDetails != mStoredShowExtendedDetails || config.extendedDetails != mStoredExtendedDetails) {
            checkExtendedDetails()
        }

        storeStateVariables()
    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        checkExtendedDetails()
        updateInstantSwitchWidths()
    }

    private fun storeStateVariables() {
        context!!.config.apply {
            mStoredShowExtendedDetails = showExtendedDetails
            mStoredHideExtendedDetails = hideExtendedDetails
            mStoredExtendedDetails = extendedDetails
            mStoredBottomActions = bottomActions
        }
    }

    private fun launchVideoPlayer() {
        val newUri = activity!!.getFinalUriFromPath(medium.path, BuildConfig.APPLICATION_ID)
        if (newUri == null) {
            context!!.toast(R.string.unknown_error_occurred)
            return
        }

        val mimeType = activity!!.getUriMimeType(medium.path, newUri)
        Intent(activity, VideoPlayerActivity::class.java).apply {
            setDataAndType(newUri, mimeType)
            context!!.startActivity(this)
        }
    }

    private fun toggleFullscreen() {
        listener?.fragmentClicked()
    }

    private fun checkExtendedDetails() {
        if (context!!.config.showExtendedDetails) {
            mView.video_details.apply {
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
            mView.video_details.beGone()
        }
    }

    private fun checkIfPanorama() {
        try {
            val fis = FileInputStream(File(medium.path))
            parseFileChannel(fis.channel, 0, 0, 0)
        } catch (ignored: Exception) {
        } catch (ignored: OutOfMemoryError) {
        }
    }

    // based on https://github.com/sannies/mp4parser/blob/master/examples/src/main/java/com/google/code/mp4parser/example/PrintStructure.java
    private fun parseFileChannel(fc: FileChannel, level: Int, start: Long, end: Long) {
        try {
            var iteration = 0
            var currEnd = end
            fc.position(start)
            if (currEnd <= 0) {
                currEnd = start + fc.size()
            }

            while (currEnd - fc.position() > 8) {
                // just a check to avoid deadloop at some videos
                if (iteration++ > 50) {
                    return
                }

                val begin = fc.position()
                val byteBuffer = ByteBuffer.allocate(8)
                fc.read(byteBuffer)
                byteBuffer.rewind()
                val size = IsoTypeReader.readUInt32(byteBuffer)
                val type = IsoTypeReader.read4cc(byteBuffer)
                val newEnd = begin + size

                if (type == "uuid") {
                    val fis = FileInputStream(File(medium.path))
                    fis.skip(begin)

                    val sb = StringBuilder()
                    val buffer = ByteArray(1024)
                    while (true) {
                        val n = fis.read(buffer)
                        if (n != -1) {
                            sb.append(String(buffer, 0, n))
                        } else {
                            break
                        }
                    }

                    val xmlString = sb.toString().toLowerCase()
                    mIsPanorama = xmlString.contains("gspherical:projectiontype>equirectangular") ||
                            xmlString.contains("gspherical:projectiontype=\"equirectangular\"")
                    return
                }

                if (FILE_CHANNEL_CONTAINERS.contains(type)) {
                    parseFileChannel(fc, level + 1, begin + 8, newEnd)
                }

                fc.position(newEnd)
            }
        } catch (ignored: Exception) {
        }
    }

    private fun openPanorama() {
        Intent(context, PanoramaVideoActivity::class.java).apply {
            putExtra(PATH, medium.path)
            startActivity(this)
        }
    }

    private fun updateInstantSwitchWidths() {
        val newWidth = resources.getDimension(R.dimen.instant_change_bar_width) + if (activity?.portrait == false) activity!!.navigationBarWidth else 0
        mView.instant_prev_item.layoutParams.width = newWidth.toInt()
        mView.instant_next_item.layoutParams.width = newWidth.toInt()
    }

    override fun fullscreenToggled(isFullscreen: Boolean) {
        mIsFullscreen = isFullscreen
        mView.video_details.apply {
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
        return context!!.realScreenSize.y.toFloat() - height - smallMargin
    }
}
