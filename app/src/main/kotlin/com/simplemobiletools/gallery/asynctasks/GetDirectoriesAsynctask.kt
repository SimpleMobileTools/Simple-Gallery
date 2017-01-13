package com.simplemobiletools.gallery.asynctasks

import android.content.Context
import android.os.AsyncTask
import com.simplemobiletools.commons.extensions.isGif
import com.simplemobiletools.commons.extensions.isImageFast
import com.simplemobiletools.commons.extensions.isVideoFast
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.extensions.getHumanizedFilename
import com.simplemobiletools.gallery.extensions.getParents
import com.simplemobiletools.gallery.helpers.Config
import com.simplemobiletools.gallery.helpers.IMAGES
import com.simplemobiletools.gallery.helpers.SORT_BY_DATE_MODIFIED
import com.simplemobiletools.gallery.helpers.VIDEOS
import com.simplemobiletools.gallery.models.Directory
import com.simplemobiletools.gallery.models.Medium
import java.io.File
import java.util.*

class GetDirectoriesAsynctask(val context: Context, val isPickVideo: Boolean, val isPickImage: Boolean,
                              val callback: (dirs: ArrayList<Directory>) -> Unit) : AsyncTask<Void, Void, ArrayList<Directory>>() {
    lateinit var mConfig: Config

    override fun onPreExecute() {
        super.onPreExecute()
        mConfig = Config.newInstance(context)
    }

    override fun doInBackground(vararg params: Void): ArrayList<Directory> {
        val directories = LinkedHashMap<String, Directory>()
        val media = ArrayList<Medium>()
        val showMedia = mConfig.showMedia
        val fileSorting = mConfig.fileSorting
        val parents = context.getParents(isPickImage, isPickVideo)

        parents.mapNotNull { File(it).listFiles() }
                .forEach {
                    for (file in it) {
                        val isImage = file.isImageFast() || file.isGif()
                        val isVideo = file.isVideoFast()

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
                        val path = file.absolutePath
                        val dateModified = if (fileSorting and SORT_BY_DATE_MODIFIED != 0) file.lastModified() else 0
                        media.add(Medium(name, path, isVideo, dateModified, dateModified, size))
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
                if (mConfig.getIsFolderHidden(parentDir)) {
                    dirName += " ${context.resources.getString(R.string.hidden)}"
                }

                directories.put(parentDir, Directory(parentDir, path, dirName, 1, dateModified, dateTaken, size))
            }
        }

        val dirs = ArrayList(directories.values.filter { File(it.path).exists() })

        filterDirectories(dirs)
        Directory.sorting = mConfig.directorySorting
        dirs.sort()

        return movePinnedToFront(dirs)
    }

    private fun movePinnedToFront(dirs: ArrayList<Directory>): ArrayList<Directory> {
        val foundFolders = ArrayList<Directory>()
        val pinnedFolders = mConfig.pinnedFolders

        dirs.forEach { if (pinnedFolders.contains(it.path)) foundFolders.add(it) }
        dirs.removeAll(foundFolders)
        dirs.addAll(0, foundFolders)
        return dirs
    }

    override fun onPostExecute(dirs: ArrayList<Directory>) {
        super.onPostExecute(dirs)
        callback.invoke(dirs)
    }

    private fun filterDirectories(dirs: MutableList<Directory>) {
        if (!mConfig.showHiddenFolders) {
            removeHiddenFolders(dirs)
            removeNoMediaFolders(dirs)
        }
    }

    private fun removeHiddenFolders(dirs: MutableList<Directory>) {
        val hiddenDirs = mConfig.hiddenFolders
        val ignoreDirs = dirs.filter { hiddenDirs.contains(it.path) }
        dirs.removeAll(ignoreDirs)
    }

    private fun removeNoMediaFolders(dirs: MutableList<Directory>) {
        val ignoreDirs = ArrayList<Directory>()
        for (d in dirs) {
            val dir = File(d.path)
            if (dir.exists() && dir.isDirectory) {
                val res = dir.list { file, filename -> filename == ".nomedia" }
                if (res?.isNotEmpty() == true)
                    ignoreDirs.add(d)
            }
        }

        dirs.removeAll(ignoreDirs)
    }
}
