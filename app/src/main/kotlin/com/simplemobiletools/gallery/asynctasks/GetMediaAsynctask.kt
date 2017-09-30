package com.simplemobiletools.gallery.asynctasks

import android.content.Context
import android.os.AsyncTask
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.helpers.MediaFetcher
import com.simplemobiletools.gallery.models.Medium
import java.util.*

class GetMediaAsynctask(val context: Context, val mPath: String, val isPickVideo: Boolean = false, val isPickImage: Boolean = false,
                        val showAll: Boolean, val callback: (media: ArrayList<Medium>) -> Unit) :
        AsyncTask<Void, Void, ArrayList<Medium>>() {

    override fun doInBackground(vararg params: Void): ArrayList<Medium> {
        return if (showAll) {
            val mediaMap = MediaFetcher(context).getMediaByDirectories(isPickVideo, isPickImage)
            val media = ArrayList<Medium>()
            for ((path, curMedia) in mediaMap) {
                media.addAll(curMedia)
            }

            Medium.sorting = context.config.getFileSorting("")
            media.sort()
            media
        } else {
            MediaFetcher(context).getFilesFrom(mPath, isPickImage, isPickVideo)
        }
    }

    override fun onPostExecute(media: ArrayList<Medium>) {
        super.onPostExecute(media)
        callback(media)
    }
}
