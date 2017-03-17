package com.simplemobiletools.gallery.asynctasks

import android.content.Context
import android.os.AsyncTask
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.extensions.containsNoMedia
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
    var shouldStop = false

    override fun doInBackground(vararg params: Void): ArrayList<Directory> {
        val media = ArrayList<Medium>()
        val showMedia = config.showMedia
        val fileSorting = config.fileSorting
        val parents = context.getParents()
        removeExcludedFolders(parents)

        parents.forEach {
            val filenames = File(it).list()
            if (filenames?.size ?: 0 > 0) {
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

        Medium.sorting = fileSorting
        media.sort()

        val directories = groupDirectories(media)
        val dirs = ArrayList(directories.values.filter { File(it.path).exists() })
        Directory.sorting = config.directorySorting
        dirs.sort()

        return movePinnedToFront(dirs)
    }

    private fun groupDirectories(media: ArrayList<Medium>): Map<String, Directory> {
        val hidden = context.resources.getString(R.string.hidden)
        val directories = LinkedHashMap<String, Directory>()
        val showHidden = config.showHiddenFolders
        for ((name, path, isVideo, dateModified, dateTaken, size) in media) {
            if (shouldStop)
                cancel(true)

            val parentDir = File(path).parent ?: continue
            if (directories.containsKey(parentDir)) {
                val directory = directories[parentDir]!!
                val newImageCnt = directory.mediaCnt + 1
                directory.mediaCnt = newImageCnt
                directory.addSize(size)
            } else {
                var dirName = parentDir.getFilenameFromPath()
                if (parentDir == context.internalStoragePath) {
                    dirName = context.getString(R.string.internal)
                } else if (parentDir == context.sdCardPath) {
                    dirName = context.getString(R.string.sd_card)
                }

                if (File(parentDir).containsNoMedia()) {
                    dirName += " $hidden"

                    if (!showHidden)
                        continue
                }

                val directory = Directory(parentDir, path, dirName, 1, dateModified, dateTaken, size)
                directories.put(parentDir, directory)
            }
        }
        return directories
    }

    private fun removeExcludedFolders(paths: MutableList<String>) {
        val excludedPaths = config.excludedFolders
        val ignorePaths = paths.filter { isThisOrParentExcluded(it, excludedPaths) }
        paths.removeAll(ignorePaths)
    }

    private fun isThisOrParentExcluded(path: String, excludedPaths: MutableSet<String>) = excludedPaths.any { path.startsWith(it) }

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
