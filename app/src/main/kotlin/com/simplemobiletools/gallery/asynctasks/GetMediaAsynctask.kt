package com.simplemobiletools.gallery.asynctasks

import android.content.Context
import android.os.AsyncTask
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.helpers.MediaFetcher
import com.simplemobiletools.gallery.models.Medium
import java.util.*

class GetMediaAsynctask(val context: Context, val mPath: String, val isPickImage: Boolean = false, val isPickVideo: Boolean = false,
                        val showAll: Boolean, val callback: (media: ArrayList<Medium>) -> Unit) :
        AsyncTask<Void, Void, ArrayList<Medium>>() {
    private val mediaFetcher = MediaFetcher(context)

    override fun doInBackground(vararg params: Void): ArrayList<Medium> {
        return if (showAll) {
            val foldersToScan = mediaFetcher.getFoldersToScan("")
            val media = ArrayList<Medium>()
            for (folder in foldersToScan) {
                val newMedia = mediaFetcher.getFilesFrom(folder, isPickImage, isPickVideo)
                media.addAll(newMedia)
            }

            Medium.sorting = context.config.getFileSorting("")
            media.sort()
            media
        } else {
            mediaFetcher.getFilesFrom(mPath, isPickImage, isPickVideo)
        }
    }

    override fun onPostExecute(media: ArrayList<Medium>) {
        super.onPostExecute(media)
        callback(media)
    }

    fun stopFetching() {
        mediaFetcher.shouldStop = true
        cancel(true)
    }
}
