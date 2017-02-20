package com.simplemobiletools.gallery.asynctasks

import android.content.Context
import android.os.AsyncTask
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
    val photoExtensions = arrayOf("jpg", "png", "jpeg", "bmp", "webp", "tiff")
    val videoExtensions = arrayOf("webm", "mkv", "flv", "vob", "avi", "wmv", "mp4", "ogv", "qt", "m4p", "mpg", "m4v", "mp2", "mpeg", "3gp")

    override fun onPreExecute() {
        super.onPreExecute()
        showMedia = config.showMedia
        fileSorting = config.fileSorting
    }

    override fun doInBackground(vararg params: Void): ArrayList<Medium> {
        val media = ArrayList<Medium>()

        if (showAll) {
            val parents = context.getParents(isPickImage, isPickVideo)
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
        val files = dir.listFiles() ?: return media
        for (file in files) {
            val filePath = file.absolutePath
            val isImage = localIsImage(filePath) || localIsGif(filePath)
            val isVideo = if (isImage) false else localIsVideo(filePath)

            if (!isImage && !isVideo)
                continue

            if (isVideo && (isPickImage || showMedia == IMAGES))
                continue

            if (isImage && (isPickVideo || showMedia == VIDEOS))
                continue

            val size = file.length()
            if (size == 0L)
                continue

            val name = file.name
            val absolutePath = file.absolutePath
            val dateModified = file.lastModified()
            media.add(Medium(name, absolutePath, isVideo, dateModified, dateModified, 0))
        }
        return media
    }

    private fun localIsImage(path: String) = photoExtensions.any { path.endsWith(".$it", true) }
    private fun localIsGif(path: String) = path.endsWith(".gif", true)
    private fun localIsVideo(path: String) = videoExtensions.any { path.endsWith(".$it", true) }

    override fun onPostExecute(media: ArrayList<Medium>) {
        super.onPostExecute(media)
        callback.invoke(media)
    }
}
