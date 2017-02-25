package com.simplemobiletools.gallery.asynctasks

import android.content.Context
import android.os.AsyncTask
import com.simplemobiletools.commons.extensions.isGif
import com.simplemobiletools.commons.extensions.isImageFast
import com.simplemobiletools.commons.extensions.isVideoFast
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.extensions.containsNoMedia
import com.simplemobiletools.gallery.extensions.getHumanizedFilename
import com.simplemobiletools.gallery.extensions.getParents
import com.simplemobiletools.gallery.helpers.IMAGES
import com.simplemobiletools.gallery.helpers.VIDEOS
import com.simplemobiletools.gallery.models.Directory
import com.simplemobiletools.gallery.models.Medium
import java.io.File
import java.util.*

class GetDirectoriesAsynctask(val context: Context, val isPickVideo: Boolean, val isPickImage: Boolean,
                              val callback: (dirs: ArrayList<Directory>) -> Unit) : AsyncTask<Void, Void, ArrayList<Directory>>() {
    var config = context.config

    override fun doInBackground(vararg params: Void): ArrayList<Directory> {
        val directories = LinkedHashMap<String, Directory>()
        val media = ArrayList<Medium>()
        val showMedia = config.showMedia
        val fileSorting = config.fileSorting
        val parents = context.getParents()

        parents.forEach {
            val dir = File(it)
            if (dir.isDirectory) {
                val filenames = dir.list()
                if (filenames?.size ?: 0 > 0) {
                    for (filename in filenames) {
                        val isImage = filename.isImageFast() || filename.isGif()
                        val isVideo = if (isImage) false else filename.isVideoFast()

                        if (!isImage && !isVideo)
                            continue

                        if (isVideo && (isPickImage || showMedia == IMAGES))
                            continue

                        if (isImage && (isPickVideo || showMedia == VIDEOS))
                            continue

                        val file = File(it, filename)
                        val size = file.length()
                        if (size == 0L)
                            continue

                        val dateModified = file.lastModified()
                        val medium = Medium(filename, file.absolutePath, isVideo, dateModified, dateModified, size)
                        media.add(medium)
                    }
                }
            }
        }

        Medium.sorting = fileSorting
        media.sort()

        for ((name, path, isVideo, dateModified, dateTaken, size) in media) {
            val parentDir = File(path).parent ?: continue
            if (directories.containsKey(parentDir)) {
                val directory: Directory = directories[parentDir]!!
                val newImageCnt = directory.mediaCnt + 1
                directory.mediaCnt = newImageCnt
                directory.addSize(size)
            } else {
                var dirName = context.getHumanizedFilename(parentDir)
                if (File(parentDir).containsNoMedia()) {
                    dirName += " ${context.resources.getString(R.string.hidden)}"
                }

                directories.put(parentDir, Directory(parentDir, path, dirName, 1, dateModified, dateTaken, size))
            }
        }

        val dirs = ArrayList(directories.values.filter { File(it.path).exists() })
        Directory.sorting = config.directorySorting
        dirs.sort()

        return movePinnedToFront(dirs)
    }

    private fun movePinnedToFront(dirs: ArrayList<Directory>): ArrayList<Directory> {
        val foundFolders = ArrayList<Directory>()
        val pinnedFolders = config.pinnedFolders

        dirs.forEach { if (pinnedFolders.contains(it.path)) foundFolders.add(it) }
        dirs.removeAll(foundFolders)
        dirs.addAll(0, foundFolders)
        return dirs
    }

    override fun onPostExecute(dirs: ArrayList<Directory>) {
        super.onPostExecute(dirs)
        callback.invoke(dirs)
    }
}
