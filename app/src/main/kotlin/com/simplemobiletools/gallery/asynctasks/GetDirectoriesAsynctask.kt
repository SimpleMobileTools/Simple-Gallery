package com.simplemobiletools.gallery.asynctasks

import android.content.Context
import android.os.AsyncTask
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.OTG_PATH
import com.simplemobiletools.commons.helpers.PERMISSION_WRITE_STORAGE
import com.simplemobiletools.commons.helpers.SORT_DESCENDING
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.extensions.containsNoMedia
import com.simplemobiletools.gallery.extensions.sumByLong
import com.simplemobiletools.gallery.helpers.MediaFetcher
import com.simplemobiletools.gallery.models.Directory
import com.simplemobiletools.gallery.models.Medium
import java.io.File

class GetDirectoriesAsynctask(val context: Context, val isPickVideo: Boolean, val isPickImage: Boolean,
                              val callback: (dirs: ArrayList<Directory>) -> Unit) : AsyncTask<Void, Void, ArrayList<Directory>>() {
    val mediaFetcher = MediaFetcher(context)

    override fun doInBackground(vararg params: Void): ArrayList<Directory> {
        if (!context.hasPermission(PERMISSION_WRITE_STORAGE)) {
            return ArrayList()
        }

        val config = context.config
        val groupedMedia = mediaFetcher.getMediaByDirectories(isPickVideo, isPickImage)
        val directories = ArrayList<Directory>()
        val hidden = context.resources.getString(R.string.hidden)
        val albumCovers = config.parseAlbumCovers()
        val hasOTG = context.hasOTGConnected() && context.config.OTGBasePath.isNotEmpty()

        for ((path, curMedia) in groupedMedia) {
            Medium.sorting = config.getFileSorting(path)
            curMedia.sort()

            val firstItem = curMedia.first()
            val lastItem = curMedia.last()
            val parentDir = if (hasOTG && context.isPathOnOTG(firstItem.path)) firstItem.path.getParentPath() else File(firstItem.path).parent
            var thumbnail = firstItem.path
            albumCovers.forEach {
                if (it.path == parentDir && File(it.tmb).exists()) {
                    thumbnail = it.tmb
                }
            }

            var dirName = when (parentDir) {
                context.internalStoragePath -> context.getString(R.string.internal)
                context.sdCardPath -> context.getString(R.string.sd_card)
                OTG_PATH -> context.getString(R.string.otg)
                else -> {
                    if (parentDir.startsWith(OTG_PATH)) {
                        parentDir.trimEnd('/').substringAfterLast('/')
                    } else {
                        parentDir.getFilenameFromPath()
                    }
                }
            }

            if (File(parentDir).containsNoMedia()) {
                dirName += " $hidden"
            }

            val lastModified = if (config.directorySorting and SORT_DESCENDING > 0) Math.max(firstItem.modified, lastItem.modified) else Math.min(firstItem.modified, lastItem.modified)
            val dateTaken = if (config.directorySorting and SORT_DESCENDING > 0) Math.max(firstItem.taken, lastItem.taken) else Math.min(firstItem.taken, lastItem.taken)
            val size = curMedia.sumByLong { it.size }
            val directory = Directory(parentDir, thumbnail, dirName, curMedia.size, lastModified, dateTaken, size, context.isPathOnSD(parentDir))
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
