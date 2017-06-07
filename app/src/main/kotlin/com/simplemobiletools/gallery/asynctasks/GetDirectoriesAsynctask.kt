package com.simplemobiletools.gallery.asynctasks

import android.content.Context
import android.os.AsyncTask
import com.simplemobiletools.commons.extensions.getFilenameFromPath
import com.simplemobiletools.commons.extensions.hasWriteStoragePermission
import com.simplemobiletools.commons.extensions.internalStoragePath
import com.simplemobiletools.commons.extensions.sdCardPath
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.extensions.containsNoMedia
import com.simplemobiletools.gallery.extensions.getFilesFrom
import com.simplemobiletools.gallery.models.Directory
import com.simplemobiletools.gallery.models.Medium
import java.io.File
import java.util.*

class GetDirectoriesAsynctask(val context: Context, val isPickVideo: Boolean, val isPickImage: Boolean,
                              val callback: (dirs: ArrayList<Directory>) -> Unit) : AsyncTask<Void, Void, ArrayList<Directory>>() {
    var config = context.config
    var shouldStop = false
    val showHidden = config.shouldShowHidden

    override fun doInBackground(vararg params: Void): ArrayList<Directory> {
        if (!context.hasWriteStoragePermission())
            return ArrayList()

        val media = context.getFilesFrom("", isPickImage, isPickVideo)
        val excludedPaths = config.excludedFolders
        val directories = groupDirectories(media)
        val dirs = ArrayList(directories.values.filter { File(it.path).exists() }).filter { shouldFolderBeVisible(it.path, excludedPaths) } as ArrayList<Directory>
        Directory.sorting = config.directorySorting
        dirs.sort()
        return movePinnedToFront(dirs)
    }

    private fun groupDirectories(media: ArrayList<Medium>): Map<String, Directory> {
        val albumCovers = config.parseAlbumCovers()
        val hidden = context.resources.getString(R.string.hidden)
        val directories = LinkedHashMap<String, Directory>()
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

                var thumbnail = path
                albumCovers.forEach {
                    if (it.path == parentDir && File(it.tmb).exists()) {
                        thumbnail = it.tmb
                    }
                }

                val directory = Directory(parentDir, thumbnail, dirName, 1, dateModified, dateTaken, size)
                directories.put(parentDir, directory)
            }
        }
        return directories
    }

    private fun shouldFolderBeVisible(path: String, excludedPaths: MutableSet<String>): Boolean {
        val file = File(path)
        return if (isThisOrParentExcluded(path, excludedPaths))
            false
        else if (!config.shouldShowHidden && file.isDirectory && file.canonicalFile == file.absoluteFile) {
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
