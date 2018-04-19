package com.simplemobiletools.gallery.asynctasks

import android.content.Context
import android.os.AsyncTask
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.OTG_PATH
import com.simplemobiletools.commons.helpers.PERMISSION_WRITE_STORAGE
import com.simplemobiletools.commons.helpers.SORT_DESCENDING
import com.simplemobiletools.commons.helpers.sumByLong
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.extensions.checkAppendingHidden
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.helpers.MediaFetcher
import com.simplemobiletools.gallery.models.Directory
import com.simplemobiletools.gallery.models.Medium
import java.io.File

class GetDirectoriesAsynctask(val context: Context, val isPickVideo: Boolean, val isPickImage: Boolean,
                              val callback: (dirs: ArrayList<Directory>) -> Unit) : AsyncTask<Void, Void, ArrayList<Directory>>() {
    private val mediaFetcher = MediaFetcher(context)

    override fun doInBackground(vararg params: Void): ArrayList<Directory> {
        if (!context.hasPermission(PERMISSION_WRITE_STORAGE)) {
            return ArrayList()
        }

        val config = context.config
        val groupedMedia = mediaFetcher.getMediaByDirectories(isPickVideo, isPickImage)
        val directories = ArrayList<Directory>()
        val hidden = context.getString(R.string.hidden)
        val albumCovers = config.parseAlbumCovers()
        val hasOTG = context.hasOTGConnected() && context.config.OTGBasePath.isNotEmpty()
        val includedFolders = config.includedFolders

        for ((path, curMedia) in groupedMedia) {
            Medium.sorting = config.getFileSorting(path)
            curMedia.sort()

            val firstItem = curMedia.first()
            val lastItem = curMedia.last()
            val parentDir = if (hasOTG && firstItem.path.startsWith(OTG_PATH)) {
                firstItem.path.getParentPath()
            } else {
                File(firstItem.path).parent
            } ?: continue

            var thumbnail = curMedia.firstOrNull { context.getDoesFilePathExist(it.path) }?.path ?: ""
            if (thumbnail.startsWith(OTG_PATH)) {
                thumbnail = thumbnail.getOTGPublicPath(context)
            }

            albumCovers.forEach {
                if (it.path == parentDir && context.getDoesFilePathExist(it.tmb)) {
                    thumbnail = it.tmb
                }
            }

            val dirName = context.checkAppendingHidden(parentDir, hidden, includedFolders)
            val lastModified = if (config.directorySorting and SORT_DESCENDING > 0) Math.max(firstItem.modified, lastItem.modified) else Math.min(firstItem.modified, lastItem.modified)
            val dateTaken = if (config.directorySorting and SORT_DESCENDING > 0) Math.max(firstItem.taken, lastItem.taken) else Math.min(firstItem.taken, lastItem.taken)
            val size = curMedia.sumByLong { it.size }
            val directory = Directory(null, parentDir, thumbnail, dirName, curMedia.size, lastModified, dateTaken, size, context.isPathOnSD(parentDir))
            directories.add(directory)
        }

        return directories
    }

    override fun onPostExecute(dirs: ArrayList<Directory>) {
        super.onPostExecute(dirs)
        callback(dirs)
    }

    fun stopFetching() {
        mediaFetcher.shouldStop = true
        cancel(true)
    }
}
