package com.simplemobiletools.gallery.asynctasks

import android.content.Context
import android.os.AsyncTask
import android.provider.MediaStore
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.SORT_BY_DATE_MODIFIED
import com.simplemobiletools.commons.helpers.SORT_BY_NAME
import com.simplemobiletools.commons.helpers.SORT_BY_SIZE
import com.simplemobiletools.commons.helpers.SORT_DESCENDING
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.helpers.IMAGES
import com.simplemobiletools.gallery.helpers.VIDEOS
import com.simplemobiletools.gallery.models.Medium
import java.io.File
import java.util.*

class GetMediaAsynctask(val context: Context, val mPath: String, val isPickVideo: Boolean = false, val isPickImage: Boolean = false,
                        val showAll: Boolean, val callback: (media: ArrayList<Medium>) -> Unit) :
        AsyncTask<Void, Void, Unit>() {
    var shouldStop = false
    var media = ArrayList<Medium>()

    override fun doInBackground(vararg params: Void): Unit {
        if (showAll) {
            media = getFilesFrom("")
        } else {
            media = getFilesFrom(mPath)
        }

        return Unit
    }

    private fun getFilesFrom(curPath: String): ArrayList<Medium> {
        val curMedia = ArrayList<Medium>()
        val showMedia = context.config.showMedia
        val showHidden = context.config.shouldShowHidden
        val projection = arrayOf(MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.DATE_MODIFIED,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.SIZE)
        val uri = MediaStore.Files.getContentUri("external")
        val selection = if (curPath.isEmpty()) null else "(${MediaStore.Images.Media.DATA} LIKE ? AND ${MediaStore.Images.Media.DATA} NOT LIKE ?)"
        val selectionArgs = if (curPath.isEmpty()) null else arrayOf("$curPath/%", "$curPath/%/%")

        val cur = context.contentResolver.query(uri, projection, selection, selectionArgs, getSorting())
        if (cur.moveToFirst()) {
            var filename: String
            var path: String
            var dateTaken: Long
            var dateModified: Long
            var size: Long
            var isImage: Boolean
            var isVideo: Boolean

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
                if (filename.isEmpty())
                    filename = path.getFilenameFromPath()

                isImage = filename.isImageFast() || filename.isGif()
                isVideo = if (isImage) false else filename.isVideoFast()

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
                curMedia.add(medium)
            } while (cur.moveToNext())
        }
        cur.close()

        Medium.sorting = context.config.getFileSorting(mPath)
        curMedia.sort()

        return curMedia
    }

    private fun getSorting(): String {
        val sorting = context.config.getFileSorting(mPath)
        val sortValue = if (sorting and SORT_BY_NAME > 0)
            MediaStore.Images.Media.DISPLAY_NAME
        else if (sorting and SORT_BY_SIZE > 0)
            MediaStore.Images.Media.SIZE
        else if (sorting and SORT_BY_DATE_MODIFIED > 0)
            MediaStore.Images.Media.DATE_MODIFIED
        else
            MediaStore.Images.Media.DATE_TAKEN

        return if (sorting and SORT_DESCENDING > 0)
            "$sortValue DESC"
        else
            "$sortValue ASC"
    }

    override fun onPostExecute(result: Unit?) {
        super.onPostExecute(result)
        callback.invoke(media)
    }
}
