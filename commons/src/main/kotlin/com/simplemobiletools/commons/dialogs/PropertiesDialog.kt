package com.simplemobiletools.commons.dialogs

import android.app.Activity
import android.content.res.Resources
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.exifinterface.media.ExifInterface
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.FileDirItem
import kotlinx.android.synthetic.main.dialog_properties.view.*
import kotlinx.android.synthetic.main.item_property.view.*
import java.io.File
import java.util.*

class PropertiesDialog() {
    private lateinit var mInflater: LayoutInflater
    private lateinit var mPropertyView: ViewGroup
    private lateinit var mResources: Resources
    private lateinit var mActivity: Activity
    private lateinit var mDialogView: View
    private var mCountHiddenItems = false

    /**
     * A File Properties dialog constructor with an optional parameter, usable at 1 file selected
     *
     * @param activity request activity to avoid some Theme.AppCompat issues
     * @param path the file path
     * @param countHiddenItems toggle determining if we will count hidden files themselves and their sizes (reasonable only at directory properties)
     */
    constructor(activity: Activity, path: String, countHiddenItems: Boolean = false) : this() {
        if (!activity.getDoesFilePathExist(path) && !path.startsWith("content://")) {
            activity.toast(String.format(activity.getString(R.string.source_file_doesnt_exist), path))
            return
        }

        mActivity = activity
        mInflater = LayoutInflater.from(activity)
        mResources = activity.resources
        mDialogView = mInflater.inflate(R.layout.dialog_properties, null)
        mCountHiddenItems = countHiddenItems
        mPropertyView = mDialogView.properties_holder!!
        addProperties(path)

        val builder = activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)

        if (!path.startsWith("content://") && path.canModifyEXIF() && activity.isPathOnInternalStorage(path)) {
            if ((isRPlus() && Environment.isExternalStorageManager()) || (!isRPlus() && activity.hasPermission(PERMISSION_WRITE_STORAGE))) {
                builder.setNeutralButton(R.string.remove_exif, null)
            }
        }

        builder.apply {
            mActivity.setupDialogStuff(mDialogView, this, R.string.properties) { alertDialog ->
                alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                    removeEXIFFromPath(path)
                }
            }
        }
    }

    private fun addProperties(path: String) {
        val fileDirItem = FileDirItem(path, path.getFilenameFromPath(), mActivity.getIsPathDirectory(path))
        addProperty(R.string.name, fileDirItem.name)
        addProperty(R.string.path, fileDirItem.getParentPath())
        addProperty(R.string.size, "…", R.id.properties_size)

        ensureBackgroundThread {
            val fileCount = fileDirItem.getProperFileCount(mActivity, mCountHiddenItems)
            val size = fileDirItem.getProperSize(mActivity, mCountHiddenItems).formatSize()

            val directChildrenCount = if (fileDirItem.isDirectory) {
                fileDirItem.getDirectChildrenCount(mActivity, mCountHiddenItems).toString()
            } else {
                0
            }

            this.mActivity.runOnUiThread {
                (mDialogView.findViewById<LinearLayout>(R.id.properties_size).property_value as TextView).text = size

                if (fileDirItem.isDirectory) {
                    (mDialogView.findViewById<LinearLayout>(R.id.properties_file_count).property_value as TextView).text = fileCount.toString()
                    (mDialogView.findViewById<LinearLayout>(R.id.properties_direct_children_count).property_value as TextView).text =
                        directChildrenCount.toString()
                }
            }

            if (!fileDirItem.isDirectory) {
                val projection = arrayOf(MediaStore.Images.Media.DATE_MODIFIED)
                val uri = MediaStore.Files.getContentUri("external")
                val selection = "${MediaStore.MediaColumns.DATA} = ?"
                val selectionArgs = arrayOf(path)
                val cursor = mActivity.contentResolver.query(uri, projection, selection, selectionArgs, null)
                cursor?.use {
                    if (cursor.moveToFirst()) {
                        val dateModified = cursor.getLongValue(MediaStore.Images.Media.DATE_MODIFIED) * 1000L
                        updateLastModified(mActivity, mDialogView, dateModified)
                    } else {
                        updateLastModified(mActivity, mDialogView, fileDirItem.getLastModified(mActivity))
                    }
                }

                val exif = if (isNougatPlus() && mActivity.isPathOnOTG(fileDirItem.path)) {
                    ExifInterface((mActivity as BaseSimpleActivity).getFileInputStreamSync(fileDirItem.path)!!)
                } else if (isNougatPlus() && fileDirItem.path.startsWith("content://")) {
                    try {
                        ExifInterface(mActivity.contentResolver.openInputStream(Uri.parse(fileDirItem.path))!!)
                    } catch (e: Exception) {
                        return@ensureBackgroundThread
                    }
                } else if (mActivity.isRestrictedSAFOnlyRoot(path)) {
                    try {
                        ExifInterface(mActivity.contentResolver.openInputStream(mActivity.getAndroidSAFUri(path))!!)
                    } catch (e: Exception) {
                        return@ensureBackgroundThread
                    }
                } else {
                    try {
                        ExifInterface(fileDirItem.path)
                    } catch (e: Exception) {
                        return@ensureBackgroundThread
                    }
                }

                val latLon = FloatArray(2)
                if (exif.getLatLong(latLon)) {
                    mActivity.runOnUiThread {
                        addProperty(R.string.gps_coordinates, "${latLon[0]}, ${latLon[1]}")
                    }
                }

                val altitude = exif.getAltitude(0.0)
                if (altitude != 0.0) {
                    mActivity.runOnUiThread {
                        addProperty(R.string.altitude, "${altitude}m")
                    }
                }
            }
        }

        when {
            fileDirItem.isDirectory -> {
                addProperty(R.string.direct_children_count, "…", R.id.properties_direct_children_count)
                addProperty(R.string.files_count, "…", R.id.properties_file_count)
            }
            fileDirItem.path.isImageSlow() -> {
                fileDirItem.getResolution(mActivity)?.let { addProperty(R.string.resolution, it.formatAsResolution()) }
            }
            fileDirItem.path.isAudioSlow() -> {
                fileDirItem.getDuration(mActivity)?.let { addProperty(R.string.duration, it) }
                fileDirItem.getTitle(mActivity)?.let { addProperty(R.string.song_title, it) }
                fileDirItem.getArtist(mActivity)?.let { addProperty(R.string.artist, it) }
                fileDirItem.getAlbum(mActivity)?.let { addProperty(R.string.album, it) }
            }
            fileDirItem.path.isVideoSlow() -> {
                fileDirItem.getDuration(mActivity)?.let { addProperty(R.string.duration, it) }
                fileDirItem.getResolution(mActivity)?.let { addProperty(R.string.resolution, it.formatAsResolution()) }
                fileDirItem.getArtist(mActivity)?.let { addProperty(R.string.artist, it) }
                fileDirItem.getAlbum(mActivity)?.let { addProperty(R.string.album, it) }
            }
        }

        if (fileDirItem.isDirectory) {
            addProperty(R.string.last_modified, fileDirItem.getLastModified(mActivity).formatDate(mActivity))
        } else {
            addProperty(R.string.last_modified, "…", R.id.properties_last_modified)
            try {
                addExifProperties(path, mActivity)
            } catch (e: Exception) {
                mActivity.showErrorToast(e)
                return
            }

            if (mActivity.baseConfig.appId.removeSuffix(".debug") == "com.simplemobiletools.filemanager.pro") {
                addProperty(R.string.md5, "…", R.id.properties_md5)
                ensureBackgroundThread {
                    val md5 = if (mActivity.isRestrictedSAFOnlyRoot(path)) {
                        mActivity.contentResolver.openInputStream(mActivity.getAndroidSAFUri(path))?.md5()
                    } else {
                        File(path).md5()
                    }

                    mActivity.runOnUiThread {
                        if (md5 != null) {
                            (mDialogView.findViewById<LinearLayout>(R.id.properties_md5).property_value as TextView).text = md5
                        } else {
                            mDialogView.findViewById<LinearLayout>(R.id.properties_md5).beGone()
                        }
                    }
                }
            }
        }
    }

    private fun updateLastModified(activity: Activity, view: View, timestamp: Long) {
        activity.runOnUiThread {
            (view.findViewById<LinearLayout>(R.id.properties_last_modified).property_value as TextView).text = timestamp.formatDate(activity)
        }
    }

    /**
     * A File Properties dialog constructor with an optional parameter, usable at multiple items selected
     *
     * @param activity request activity to avoid some Theme.AppCompat issues
     * @param path the file path
     * @param countHiddenItems toggle determining if we will count hidden files themselves and their sizes
     */
    constructor(activity: Activity, paths: List<String>, countHiddenItems: Boolean = false) : this() {
        mActivity = activity
        mInflater = LayoutInflater.from(activity)
        mResources = activity.resources
        mDialogView = mInflater.inflate(R.layout.dialog_properties, null)
        mCountHiddenItems = countHiddenItems
        mPropertyView = mDialogView.properties_holder

        val fileDirItems = ArrayList<FileDirItem>(paths.size)
        paths.forEach {
            val fileDirItem = FileDirItem(it, it.getFilenameFromPath(), activity.getIsPathDirectory(it))
            fileDirItems.add(fileDirItem)
        }

        val isSameParent = isSameParent(fileDirItems)

        addProperty(R.string.items_selected, paths.size.toString())
        if (isSameParent) {
            addProperty(R.string.path, fileDirItems[0].getParentPath())
        }

        addProperty(R.string.size, "…", R.id.properties_size)
        addProperty(R.string.files_count, "…", R.id.properties_file_count)

        ensureBackgroundThread {
            val fileCount = fileDirItems.sumByInt { it.getProperFileCount(activity, countHiddenItems) }
            val size = fileDirItems.sumByLong { it.getProperSize(activity, countHiddenItems) }.formatSize()
            activity.runOnUiThread {
                (mDialogView.findViewById<LinearLayout>(R.id.properties_size).property_value as TextView).text = size
                (mDialogView.findViewById<LinearLayout>(R.id.properties_file_count).property_value as TextView).text = fileCount.toString()
            }
        }

        val builder = activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)

        if (!paths.any { it.startsWith("content://") } && paths.any { it.canModifyEXIF() } && paths.any { activity.isPathOnInternalStorage(it) }) {
            if ((isRPlus() && Environment.isExternalStorageManager()) || (!isRPlus() && activity.hasPermission(PERMISSION_WRITE_STORAGE))) {
                builder.setNeutralButton(R.string.remove_exif, null)
            }
        }

        builder.apply {
            mActivity.setupDialogStuff(mDialogView, this, R.string.properties) { alertDialog ->
                alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                    removeEXIFFromPaths(paths)
                }
            }
        }
    }

    private fun addExifProperties(path: String, activity: Activity) {
        val exif = if (isNougatPlus() && activity.isPathOnOTG(path)) {
            ExifInterface((activity as BaseSimpleActivity).getFileInputStreamSync(path)!!)
        } else if (isNougatPlus() && path.startsWith("content://")) {
            try {
                ExifInterface(activity.contentResolver.openInputStream(Uri.parse(path))!!)
            } catch (e: Exception) {
                return
            }
        } else if (activity.isRestrictedSAFOnlyRoot(path)) {
            try {
                ExifInterface(activity.contentResolver.openInputStream(activity.getAndroidSAFUri(path))!!)
            } catch (e: Exception) {
                return
            }
        } else {
            ExifInterface(path)
        }

        val dateTaken = exif.getExifDateTaken(activity)
        if (dateTaken.isNotEmpty()) {
            addProperty(R.string.date_taken, dateTaken)
        }

        val cameraModel = exif.getExifCameraModel()
        if (cameraModel.isNotEmpty()) {
            addProperty(R.string.camera, cameraModel)
        }

        val exifString = exif.getExifProperties()
        if (exifString.isNotEmpty()) {
            addProperty(R.string.exif, exifString)
        }
    }

    private fun removeEXIFFromPath(path: String) {
        ConfirmationDialog(mActivity, "", R.string.remove_exif_confirmation) {
            try {
                ExifInterface(path).removeValues()
                mActivity.toast(R.string.exif_removed)

                mPropertyView.properties_holder.removeAllViews()
                addProperties(path)
            } catch (e: Exception) {
                mActivity.showErrorToast(e)
            }
        }
    }

    private fun removeEXIFFromPaths(paths: List<String>) {
        ConfirmationDialog(mActivity, "", R.string.remove_exif_confirmation) {
            try {
                paths.filter { mActivity.isPathOnInternalStorage(it) && it.canModifyEXIF() }.forEach {
                    ExifInterface(it).removeValues()
                }
                mActivity.toast(R.string.exif_removed)
            } catch (e: Exception) {
                mActivity.showErrorToast(e)
            }
        }
    }

    private fun isSameParent(fileDirItems: List<FileDirItem>): Boolean {
        var parent = fileDirItems[0].getParentPath()
        for (file in fileDirItems) {
            val curParent = file.getParentPath()
            if (curParent != parent) {
                return false
            }

            parent = curParent
        }
        return true
    }

    private fun addProperty(labelId: Int, value: String?, viewId: Int = 0) {
        if (value == null) {
            return
        }

        mInflater.inflate(R.layout.item_property, mPropertyView, false).apply {
            property_value.setTextColor(mActivity.getProperTextColor())
            property_label.setTextColor(mActivity.getProperTextColor())

            property_label.text = mResources.getString(labelId)
            property_value.text = value
            mPropertyView.properties_holder.addView(this)

            setOnLongClickListener {
                mActivity.copyToClipboard(property_value.value)
                true
            }

            if (labelId == R.string.gps_coordinates) {
                setOnClickListener {
                    mActivity.showLocationOnMap(value)
                }
            }

            if (viewId != 0) {
                id = viewId
            }
        }
    }
}
