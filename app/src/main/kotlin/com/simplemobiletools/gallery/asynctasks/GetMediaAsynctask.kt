package com.simplemobiletools.gallery.asynctasks

import android.content.Context
import android.database.Cursor
import android.os.AsyncTask
import android.provider.MediaStore
import com.simplemobiletools.filepicker.extensions.scanFiles
import com.simplemobiletools.gallery.Config
import com.simplemobiletools.gallery.models.Medium
import java.io.File
import java.util.*
import java.util.regex.Pattern

class GetMediaAsynctask(val context: Context, val mPath: String, val isPickVideo: Boolean, val isPickImage: Boolean,
                        val mToBeDeleted: List<String>, val callback: (media: ArrayList<Medium>) -> Unit) : AsyncTask<Void, Void, ArrayList<Medium>>() {
    lateinit var mConfig: Config

    override fun onPreExecute() {
        super.onPreExecute()
        mConfig = Config.newInstance(context)
    }

    override fun doInBackground(vararg params: Void): ArrayList<Medium> {
        val media = ArrayList<Medium>()
        val invalidFiles = ArrayList<File>()
        for (i in 0..1) {
            if (isPickVideo && i == 0)
                continue

            var uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            if (i == 1) {
                if (isPickImage)
                    continue

                uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
            val where = "${MediaStore.Images.Media.DATA} LIKE ?"
            val args = arrayOf("$mPath%")
            val columns = arrayOf(MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_MODIFIED)
            val pattern = "${Pattern.quote(mPath)}/[^/]*"
            var cursor: Cursor? = null

            try {
                cursor = context.contentResolver.query(uri, columns, where, args, null)

                if (cursor != null && cursor.moveToFirst()) {
                    val pathIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                    do {
                        val curPath = cursor.getString(pathIndex) ?: continue

                        if (curPath.matches(pattern.toRegex()) && !mToBeDeleted.contains(curPath)) {
                            val file = File(curPath)
                            if (file.exists()) {
                                val dateIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)
                                val timestamp = cursor.getLong(dateIndex)
                                media.add(Medium(file.name, curPath, i == 1, timestamp, file.length()))
                            } else {
                                invalidFiles.add(file)
                            }
                        }
                    } while (cursor.moveToNext())
                }
            } finally {
                cursor?.close()
            }
        }

        context.scanFiles(invalidFiles) {}
        Medium.sorting = mConfig.sorting
        media.sort()
        return media
    }

    override fun onPostExecute(media: ArrayList<Medium>) {
        super.onPostExecute(media)
        callback.invoke(media)
    }
}
