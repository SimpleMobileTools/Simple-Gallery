package com.simplemobiletools.gallery.asynctasks

import android.content.Context
import android.os.AsyncTask
import com.simplemobiletools.commons.extensions.isGif
import com.simplemobiletools.commons.extensions.isImageFast
import com.simplemobiletools.commons.extensions.isVideoFast
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
        AsyncTask<Void, Void, ArrayList<Medium>>() {
    var config = context.config
    var showMedia = IMAGES_AND_VIDEOS
    var fileSorting = 0
    var shouldStop = false

    override fun onPreExecute() {
        super.onPreExecute()
        showMedia = config.showMedia
        fileSorting = config.getFileSorting(mPath)
    }

    override fun doInBackground(vararg params: Void): ArrayList<Medium> {
        val media = ArrayList<Medium>()

        if (showAll) {
            val parents = context.getParents()
            for (parent in parents) {
                media.addAll(getFilesFrom(parent))
            }
        } else {
            media.addAll(getFilesFrom(mPath))
        }

        Medium.sorting = fileSorting
        media.sort()
        return media
    }

    private fun getFilesFrom(path: String): ArrayList<Medium> {
        val media = ArrayList<Medium>()
        val dir = File(path)
        val filenames = dir.list() ?: return media
        for (filename in filenames) {
            if (shouldStop)
                cancel(true)

            val isImage = filename.isImageFast() || filename.isGif()
            val isVideo = if (isImage) false else filename.isVideoFast()

            if (!isImage && !isVideo)
                continue

            if (isVideo && (isPickImage || showMedia == IMAGES))
                continue

            if (isImage && (isPickVideo || showMedia == VIDEOS))
                continue

            val file = File(path, filename)
            val size = file.length()
            if (size == 0L)
                continue

            val dateModified = file.lastModified()
            media.add(Medium(filename, file.absolutePath, isVideo, dateModified, dateModified, size))
        }
        return media
    }

    override fun onPostExecute(media: ArrayList<Medium>) {
        super.onPostExecute(media)
        callback.invoke(media)
    }
}
