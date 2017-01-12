package com.simplemobiletools.gallery.asynctasks

import android.content.Context
import android.database.Cursor
import android.os.AsyncTask
import android.provider.MediaStore
import com.simplemobiletools.commons.extensions.isGif
import com.simplemobiletools.commons.extensions.isImageFast
import com.simplemobiletools.commons.extensions.isVideoFast
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.extensions.getHumanizedFilename
import com.simplemobiletools.gallery.extensions.getStringValue
import com.simplemobiletools.gallery.helpers.Config
import com.simplemobiletools.gallery.helpers.IMAGES
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

    private fun getWhereCondition(): String {
        val showMedia = mConfig.showMedia
        return if ((isPickImage || showMedia == IMAGES) || (isPickVideo || showMedia == VIDEOS)) {
            "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?)"
        } else {
            "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?)"
        }
    }

    private fun getArgs(): Array<String> {
        val showMedia = mConfig.showMedia
        return if (isPickImage || showMedia == IMAGES) {
            arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString())
        } else if (isPickVideo || showMedia == VIDEOS) {
            arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
        } else {
            arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(), MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
        }
    }

    override fun doInBackground(vararg params: Void): ArrayList<Directory> {
        val directories = LinkedHashMap<String, Directory>()
        val media = ArrayList<Medium>()
        val showMedia = mConfig.showMedia
        val uri = MediaStore.Files.getContentUri("external")
        val where = getWhereCondition() + " GROUP BY ( ${MediaStore.Files.FileColumns.PARENT} "
        val args = getArgs()
        val columns = arrayOf(MediaStore.Files.FileColumns.PARENT, MediaStore.Images.Media.DATA)
        var cursor: Cursor? = null

        try {
            cursor = context.contentResolver.query(uri, columns, where, args, null)
            if (cursor?.moveToFirst() == true) {
                do {
                    val curPath = cursor.getStringValue(MediaStore.Images.Media.DATA)
                    val dirPath = File(curPath).parent
                    val dir = File(dirPath).listFiles() ?: continue

                    for (file in dir) {
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
                        val dateModified = file.lastModified()
                        media.add(Medium(name, path, isVideo, dateModified, dateModified, size))
                    }
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
        }

        Medium.sorting = mConfig.fileSorting
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
