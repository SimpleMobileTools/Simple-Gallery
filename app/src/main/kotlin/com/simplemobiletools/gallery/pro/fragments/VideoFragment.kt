package com.simplemobiletools.gallery.pro.fragments

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.activities.PanoramaVideoActivity
import com.simplemobiletools.gallery.pro.extensions.*
import com.simplemobiletools.gallery.pro.helpers.MEDIUM
import com.simplemobiletools.gallery.pro.helpers.PATH
import com.simplemobiletools.gallery.pro.models.Medium
import com.simplemobiletools.gallery.pro.views.MediaSideScroll
import kotlinx.android.synthetic.main.pager_video_item.view.*
import java.io.File
import java.io.FileInputStream

class VideoFragment : ViewPagerFragment() {
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

        checkExtendedDetails()
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
        activity!!.openPath(medium.path, false)
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
            context!!.parseFileChannel(medium.path, fis.channel, 0, 0, 0) {
                mIsPanorama = true
            }
        } catch (ignored: Exception) {
        } catch (ignored: OutOfMemoryError) {
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
        val fullscreenOffset = smallMargin + if (mIsFullscreen) 0 else context!!.navigationBarHeight
        val actionsHeight = if (context!!.config.bottomActions && !mIsFullscreen) resources.getDimension(R.dimen.bottom_actions_height) else 0f
        return context!!.realScreenSize.y - height - actionsHeight - fullscreenOffset
    }
}
