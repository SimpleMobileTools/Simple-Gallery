package com.simplemobiletools.gallery.asynctasks

import android.content.Context
import android.os.AsyncTask
import com.simplemobiletools.commons.extensions.getFilenameFromPath
import com.simplemobiletools.commons.extensions.hasWriteStoragePermission
import com.simplemobiletools.commons.extensions.internalStoragePath
import com.simplemobiletools.commons.extensions.sdCardPath
import com.simplemobiletools.commons.helpers.SORT_DESCENDING
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.extensions.containsNoMedia
import com.simplemobiletools.gallery.extensions.getFilesFrom
import com.simplemobiletools.gallery.extensions.sumByLong
import com.simplemobiletools.gallery.models.Directory
import com.simplemobiletools.gallery.models.Medium
import java.io.File
import java.util.*

class GetDirectoriesAsynctask(val context: Context, val isPickVideo: Boolean, val isPickImage: Boolean,
                              val callback: (dirs: ArrayList<Directory>) -> Unit) : AsyncTask<Void, Void, ArrayList<Directory>>() {
    var config = context.config
    var shouldStop = false
    private val showHidden = config.shouldShowHidden

    override fun doInBackground(vararg params: Void): ArrayList<Directory> {
        if (!context.hasWriteStoragePermission())
            return ArrayList()

        val media = context.getFilesFrom("", isPickImage, isPickVideo)
        val excludedPaths = config.excludedFolders
        val includedPaths = config.includedFolders
        val hidden = context.resources.getString(R.string.hidden)
        val directories = groupDirectories(media)

        val removePaths = ArrayList<String>()
        directories.keys.forEach {
            if (!File(it).exists() || !shouldFolderBeVisible(it, excludedPaths, includedPaths)) {
                removePaths.add(it)
            }
        }

        removePaths.forEach {
            directories.remove(it)
        }

        val dirs = ArrayList<Directory>()
        val albumCovers = config.parseAlbumCovers()
        for ((path, curMedia) in directories) {
            Medium.sorting = config.getFileSorting(path)
            curMedia.sort()

            val firstItem = curMedia.first()
            val lastItem = curMedia.last()
            val parentDir = File(firstItem.path).parent
            var thumbnail = firstItem.path
            albumCovers.forEach {
                if (it.path == parentDir && File(it.tmb).exists()) {
                    thumbnail = it.tmb
                }
            }

            var dirName = when (parentDir) {
                context.internalStoragePath -> context.getString(R.string.internal)
                context.sdCardPath -> context.getString(R.string.sd_card)
                else -> parentDir.getFilenameFromPath()
            }

            if (File(path).containsNoMedia()) {
                dirName += " $hidden"
            }

            val lastModified = if (config.directorySorting and SORT_DESCENDING > 0) Math.max(firstItem.modified, lastItem.modified) else Math.min(firstItem.modified, lastItem.modified)
            val dateTaken = if (config.directorySorting and SORT_DESCENDING > 0) Math.max(firstItem.taken, lastItem.taken) else Math.min(firstItem.taken, lastItem.taken)
            val size = curMedia.sumByLong { it.size }
            val directory = Directory(parentDir, thumbnail, dirName, curMedia.size, lastModified, dateTaken, size)
            dirs.add(directory)
        }

        Directory.sorting = config.directorySorting
        dirs.sort()
        return movePinnedToFront(dirs)
    }

    private fun groupDirectories(media: ArrayList<Medium>): HashMap<String, ArrayList<Medium>> {
        val directories = LinkedHashMap<String, ArrayList<Medium>>()
        for (medium in media) {
            if (shouldStop) {
                cancel(true)
                break
            }

            val parentDir = File(medium.path).parent?.toLowerCase() ?: continue
            if (directories.containsKey(parentDir)) {
                directories[parentDir]!!.add(medium)
            } else {
                directories.put(parentDir, arrayListOf(medium))
            }
        }
        return directories
    }

    private fun shouldFolderBeVisible(path: String, excludedPaths: MutableSet<String>, includedPaths: MutableSet<String>): Boolean {
        val file = File(path)
        return if (includedPaths.contains(path)) {
            true
        } else if (isThisOrParentExcluded(path, excludedPaths, includedPaths)) {
            false
        } else if (!showHidden && file.isDirectory && file.canonicalFile == file.absoluteFile) {
            var containsNoMediaOrDot = file.containsNoMedia() || path.contains("/.")
            if (!containsNoMediaOrDot) {
                containsNoMediaOrDot = checkParentHasNoMedia(file.parentFile)
            }
            !containsNoMediaOrDot
        } else {
            true
        }
    }

    private fun checkParentHasNoMedia(file: File): Boolean {
        var curFile = file
        while (true) {
            if (curFile.containsNoMedia()) {
                return true
            }
            curFile = curFile.parentFile
            if (curFile.absolutePath == "/")
                break
        }
        return false
    }

    private fun isThisOrParentExcluded(path: String, excludedPaths: MutableSet<String>, includedPaths: MutableSet<String>) =
            includedPaths.none { path.startsWith(it) } && excludedPaths.any { path.startsWith(it) }

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
        callback(dirs)
    }
}
