package com.simplemobiletools.gallery.asynctasks

import android.content.Context
import android.os.AsyncTask
import android.provider.MediaStore
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.extensions.getParents
import com.simplemobiletools.gallery.helpers.IMAGES
import com.simplemobiletools.gallery.helpers.IMAGES_AND_VIDEOS
import com.simplemobiletools.gallery.helpers.VIDEOS
import com.simplemobiletools.gallery.models.Medium
import java.io.File
import java.util.*

class GetMediaAsynctask(val context: Context, val mPath: String, val isPickVideo: Boolean = false, val isPickImage: Boolean = false,
                        val showAll: Boolean, val callback: (media: ArrayList<Medium>) -> Unit) :
        AsyncTask<Void, Void, Unit>() {
    var config = context.config
    var showMedia = IMAGES_AND_VIDEOS
    var fileSorting = 0
    var shouldStop = false
    var media = ArrayList<Medium>()
    val showHidden = config.shouldShowHidden

    override fun onPreExecute() {
        super.onPreExecute()
        showMedia = config.showMedia
        fileSorting = config.getFileSorting(mPath)
    }

    override fun doInBackground(vararg params: Void): Unit {
        if (showAll) {
            val parents = context.getParents()
            for (parent in parents) {
                getFilesFrom(parent)
            }
        } else {
            getFilesFrom(mPath)
        }

        Medium.sorting = fileSorting
        media.sort()
        return Unit
    }

    private fun getFilesFrom(curPath: String) {
        val projection = arrayOf(MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.DATE_MODIFIED,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.SIZE)
        val uri = MediaStore.Files.getContentUri("external")
        val selection = "${MediaStore.Images.Media.DATA} LIKE ? AND ${MediaStore.Images.Media.DATA} NOT LIKE ?"
        val selectionArgs = arrayOf("$curPath/%", "$curPath/%/%")

        val cur = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
        if (cur.moveToFirst()) {
            var filename: String
            var path: String
            var dateTaken: Long
            var dateModified: Long
            var size: Long

            do {
                if (shouldStop)
                    cancel(true)

                path = cur.getStringValue(MediaStore.Images.Media.DATA)
                size = cur.getLongValue(MediaStore.Images.Media.SIZE)
                if (size == 0L) {
                    size = File(path).length()
                }

                if (size <= 0L) {
                    continue
                }

                filename = cur.getStringValue(MediaStore.Images.Media.DISPLAY_NAME) ?: ""
                val isImage = filename.isImageFast() || filename.isGif()
                val isVideo = if (isImage) false else filename.isVideoFast()

                if (!isImage && !isVideo)
                    continue

                if (isVideo && (isPickImage || showMedia == IMAGES))
                    continue

                if (isImage && (isPickVideo || showMedia == VIDEOS))
                    continue

                if (!showHidden && filename.startsWith('.'))
                    continue

                dateTaken = cur.getLongValue(MediaStore.Images.Media.DATE_TAKEN)
                dateModified = cur.getIntValue(MediaStore.Images.Media.DATE_MODIFIED) * 1000L

                val medium = Medium(filename, path, isVideo, dateModified, dateTaken, size)
                media.add(medium)
            } while (cur.moveToNext())
        }
        cur.close()
    }

    override fun onPostExecute(result: Unit?) {
        super.onPostExecute(result)
        callback.invoke(media)
    }
}
