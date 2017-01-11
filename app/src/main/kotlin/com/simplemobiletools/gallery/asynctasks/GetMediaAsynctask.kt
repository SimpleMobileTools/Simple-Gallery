package com.simplemobiletools.gallery.asynctasks

import android.content.Context
import android.database.Cursor
import android.os.AsyncTask
import android.provider.MediaStore
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.gallery.extensions.getLongValue
import com.simplemobiletools.gallery.extensions.getStringValue
import com.simplemobiletools.gallery.helpers.Config
import com.simplemobiletools.gallery.helpers.IMAGES
import com.simplemobiletools.gallery.helpers.VIDEOS
import com.simplemobiletools.gallery.models.Medium
import java.io.File
import java.util.*

class GetMediaAsynctask(val context: Context, val mPath: String, val isPickVideo: Boolean = false, val isPickImage: Boolean = false,
                        val showAll: Boolean, val callback: (media: ArrayList<Medium>) -> Unit) :
        AsyncTask<Void, Void, ArrayList<Medium>>() {
    lateinit var mConfig: Config

    override fun onPreExecute() {
        super.onPreExecute()
        mConfig = Config.newInstance(context)
    }

    override fun doInBackground(vararg params: Void): ArrayList<Medium> {
        val media = ArrayList<Medium>()
        val invalidFiles = ArrayList<File>()
        val showMedia = mConfig.showMedia
        val uri = MediaStore.Files.getContentUri("external")
        val where = "${MediaStore.Images.Media.DATA} LIKE ?"
        val checkPath = if (showAll) "%" else "$mPath%"
        val args = arrayOf(checkPath)
        val columns = arrayOf(MediaStore.Images.Media.DATA, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATE_MODIFIED,
                MediaStore.Images.Media.DATE_TAKEN, MediaStore.Images.Media.SIZE)
        var cursor: Cursor? = null

        try {
            cursor = context.contentResolver.query(uri, columns, where, args, null)
            if (cursor?.moveToFirst() == true) {
                do {
                    val curPath = cursor.getStringValue(MediaStore.Images.Media.DATA) ?: continue
                    val file = File(curPath)
                    val size = cursor.getLongValue(MediaStore.Images.Media.SIZE)

                    if (size == 0L) {
                        invalidFiles.add(file)
                        continue
                    }

                    // exclude images of subdirectories
                    if (!showAll && file.parent != mPath)
                        continue

                    var name = cursor.getStringValue(MediaStore.Images.Media.DISPLAY_NAME) ?: ""
                    val isImage = file.isImageFast() || file.isGif()
                    val isVideo = file.isVideoFast()

                    if (!isImage && !isVideo)
                        continue

                    if (isVideo && (isPickImage || showMedia == IMAGES))
                        continue

                    if (isImage && (isPickVideo || showMedia == VIDEOS))
                        continue

                    if (name.isEmpty())
                        name = curPath.getFilenameFromPath()
                    val dateModified = cursor.getLongValue(MediaStore.Images.Media.DATE_MODIFIED)
                    val dateTaken = cursor.getLongValue(MediaStore.Images.Media.DATE_TAKEN)
                    media.add(Medium(name, curPath, isVideo, dateModified, dateTaken, size))
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
        }

        context.scanFiles(invalidFiles) {}
        Medium.sorting = mConfig.fileSorting
        media.sort()
        return media
    }

    override fun onPostExecute(media: ArrayList<Medium>) {
        super.onPostExecute(media)
        callback.invoke(media)
    }
}