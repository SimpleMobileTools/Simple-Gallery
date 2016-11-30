package com.simplemobiletools.gallery.asynctasks

import android.content.Context
import android.database.Cursor
import android.os.AsyncTask
import android.provider.MediaStore
import com.simplemobiletools.filepicker.extensions.scanFiles
import com.simplemobiletools.gallery.Config
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.SORT_BY_NAME
import com.simplemobiletools.gallery.SORT_DESCENDING
import com.simplemobiletools.gallery.extensions.getHumanizedFilename
import com.simplemobiletools.gallery.models.Directory
import java.io.File
import java.util.*

class GetDirectoriesAsynctask(val context: Context, val isPickVideo: Boolean, val isPickImage: Boolean,
                              val mToBeDeleted: List<String>, val callback: (dirs: ArrayList<Directory>) -> Unit) : AsyncTask<Void, Void, ArrayList<Directory>>() {
    lateinit var mConfig: Config

    override fun onPreExecute() {
        super.onPreExecute()
        mConfig = Config.newInstance(context)
    }

    override fun doInBackground(vararg params: Void): ArrayList<Directory> {
        val directories = LinkedHashMap<String, Directory>()
        val invalidFiles = ArrayList<File>()
        for (i in 0..1) {
            if ((isPickVideo) && i == 0)
                continue

            var uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            if (i == 1) {
                if (isPickImage)
                    continue

                uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
            val columns = arrayOf(MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_MODIFIED)
            val order = getSortOrder()
            var cursor: Cursor? = null

            try {
                cursor = context.contentResolver.query(uri, columns, null, null, order)
                if (cursor != null && cursor.moveToFirst()) {
                    val pathIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                    do {
                        val fullPath: String = cursor.getString(pathIndex) ?: continue
                        val file = File(fullPath)
                        val parentDir = file.parent

                        if (!file.exists() || file.length() == 0L) {
                            invalidFiles.add(file)
                            continue
                        }

                        val dateIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)
                        val timestamp = cursor.getLong(dateIndex)
                        if (directories.containsKey(parentDir)) {
                            val directory: Directory = directories[parentDir]!!
                            val newImageCnt = directory.mediaCnt + 1
                            directory.mediaCnt = newImageCnt
                            directory.addSize(file.length())
                        } else if (!mToBeDeleted.contains(parentDir)) {
                            var dirName = context.getHumanizedFilename(parentDir)
                            if (mConfig.getIsFolderHidden(parentDir)) {
                                dirName += " ${context.resources.getString(R.string.hidden)}"
                            }

                            directories.put(parentDir, Directory(parentDir, fullPath, dirName, 1, timestamp, file.length()))
                        }
                    } while (cursor.moveToNext())
                }
            } finally {
                cursor?.close()
            }
        }

        context.scanFiles(invalidFiles) {}
        val dirs = ArrayList(directories.values)
        filterDirectories(dirs)
        Directory.sorting = mConfig.directorySorting
        dirs.sort()
        return dirs
    }

    override fun onPostExecute(dirs: ArrayList<Directory>) {
        super.onPostExecute(dirs)
        callback.invoke(dirs)
    }

    // sort the files at querying too, just to get the correct thumbnail
    private fun getSortOrder(): String {
        val sorting = mConfig.directorySorting
        var sortBy = MediaStore.Images.Media.DATE_MODIFIED
        if (sorting and SORT_BY_NAME != 0) {
            sortBy = MediaStore.Images.Media.DATA
        }

        if (sorting and SORT_DESCENDING != 0) {
            sortBy += " DESC"
        }
        return sortBy
    }

    private fun filterDirectories(dirs: MutableList<Directory>) {
        if (!mConfig.showHiddenFolders)
            removeHiddenFolders(dirs)

        removeNoMediaFolders(dirs)
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
