package com.simplemobiletools.gallery.fragments

import android.view.MotionEvent
import androidx.fragment.app.Fragment
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.OTG_PATH
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.helpers.*
import com.simplemobiletools.gallery.models.Medium
import java.io.File

abstract class ViewPagerFragment : Fragment() {
    var listener: FragmentListener? = null

    protected var mTouchDownX = 0f
    protected var mTouchDownY = 0f
    protected var mCloseDownThreshold = 100f
    protected var mIgnoreCloseDown = false

    abstract fun fullscreenToggled(isFullscreen: Boolean)

    interface FragmentListener {
        fun fragmentClicked()

        fun videoEnded(): Boolean

        fun goToPrevItem()

        fun goToNextItem()
    }

    fun getMediumExtendedDetails(medium: Medium): String {
        val file = File(medium.path)
        if (!file.exists()) {
            return ""
        }

        val path = "${file.parent.trimEnd('/')}/"
        val exif = android.media.ExifInterface(medium.path)
        val details = StringBuilder()
        val detailsFlag = context!!.config.extendedDetails
        if (detailsFlag and EXT_NAME != 0) {
            medium.name.let { if (it.isNotEmpty()) details.appendln(it) }
        }

        if (detailsFlag and EXT_PATH != 0) {
            path.let { if (it.isNotEmpty()) details.appendln(it) }
        }

        if (detailsFlag and EXT_SIZE != 0) {
            file.length().formatSize().let { if (it.isNotEmpty()) details.appendln(it) }
        }

        if (detailsFlag and EXT_RESOLUTION != 0) {
            file.absolutePath.getResolution()?.formatAsResolution().let { if (it?.isNotEmpty() == true) details.appendln(it) }
        }

        if (detailsFlag and EXT_LAST_MODIFIED != 0) {
            file.lastModified().formatDate().let { if (it.isNotEmpty()) details.appendln(it) }
        }

        if (detailsFlag and EXT_DATE_TAKEN != 0) {
            path.getExifDateTaken(exif).let { if (it.isNotEmpty()) details.appendln(it) }
        }

        if (detailsFlag and EXT_CAMERA_MODEL != 0) {
            path.getExifCameraModel(exif).let { if (it.isNotEmpty()) details.appendln(it) }
        }

        if (detailsFlag and EXT_EXIF_PROPERTIES != 0) {
            path.getExifProperties(exif).let { if (it.isNotEmpty()) details.appendln(it) }
        }
        return details.toString().trim()
    }

    fun getPathToLoad(medium: Medium) = if (medium.path.startsWith(OTG_PATH)) medium.path.getOTGPublicPath(context!!) else medium.path

    protected fun handleEvent(event: MotionEvent) {
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
                    activity?.supportFinishAfterTransition()
                }
                mIgnoreCloseDown = false
            }
        }
    }
}
