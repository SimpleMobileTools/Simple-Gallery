package com.simplemobiletools.gallery.pro.fragments

import android.provider.MediaStore
import android.view.MotionEvent
import androidx.fragment.app.Fragment
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.gallery.pro.extensions.config
import com.simplemobiletools.gallery.pro.helpers.*
import com.simplemobiletools.gallery.pro.models.Medium
import java.io.File

abstract class ViewPagerFragment : Fragment() {
    var listener: FragmentListener? = null

    private var mTouchDownTime = 0L
    private var mTouchDownX = 0f
    private var mTouchDownY = 0f
    private var mCloseDownThreshold = 100f
    private var mIgnoreCloseDown = false

    abstract fun fullscreenToggled(isFullscreen: Boolean)

    interface FragmentListener {
        fun fragmentClicked()

        fun videoEnded(): Boolean

        fun goToPrevItem()

        fun goToNextItem()

        fun launchViewVideoIntent(path: String)
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
            context!!.getResolution(file.absolutePath)?.formatAsResolution().let { if (it?.isNotEmpty() == true) details.appendln(it) }
        }

        if (detailsFlag and EXT_LAST_MODIFIED != 0) {
            getFileLastModified(file).let { if (it.isNotEmpty()) details.appendln(it) }
        }

        if (detailsFlag and EXT_DATE_TAKEN != 0) {
            path.getExifDateTaken(exif, context!!).let { if (it.isNotEmpty()) details.appendln(it) }
        }

        if (detailsFlag and EXT_CAMERA_MODEL != 0) {
            path.getExifCameraModel(exif).let { if (it.isNotEmpty()) details.appendln(it) }
        }

        if (detailsFlag and EXT_EXIF_PROPERTIES != 0) {
            path.getExifProperties(exif).let { if (it.isNotEmpty()) details.appendln(it) }
        }
        return details.toString().trim()
    }

    private fun getFileLastModified(file: File): String {
        val projection = arrayOf(MediaStore.Images.Media.DATE_MODIFIED)
        val uri = MediaStore.Files.getContentUri("external")
        val selection = "${MediaStore.MediaColumns.DATA} = ?"
        val selectionArgs = arrayOf(file.absolutePath)
        val cursor = context!!.contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            return if (cursor.moveToFirst()) {
                val dateModified = cursor.getLongValue(MediaStore.Images.Media.DATE_MODIFIED) * 1000L
                dateModified.formatDate(context!!)
            } else {
                file.lastModified().formatDate(context!!)
            }
        }
        return ""
    }

    protected fun handleEvent(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mTouchDownTime = System.currentTimeMillis()
                mTouchDownX = event.x
                mTouchDownY = event.y
            }
            MotionEvent.ACTION_POINTER_DOWN -> mIgnoreCloseDown = true
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val diffX = mTouchDownX - event.x
                val diffY = mTouchDownY - event.y

                val downGestureDuration = System.currentTimeMillis() - mTouchDownTime
                if (!mIgnoreCloseDown && Math.abs(diffY) > Math.abs(diffX) && diffY < -mCloseDownThreshold && downGestureDuration < MAX_CLOSE_DOWN_GESTURE_DURATION) {
                    activity?.supportFinishAfterTransition()
                }
                mIgnoreCloseDown = false
            }
        }
    }
}
