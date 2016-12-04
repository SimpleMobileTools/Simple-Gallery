package com.simplemobiletools.gallery.asynctasks

import android.content.Context
import android.database.Cursor
import android.os.AsyncTask
import android.provider.MediaStore
import com.simplemobiletools.filepicker.extensions.scanFiles
import com.simplemobiletools.gallery.extensions.getLongValue
import com.simplemobiletools.gallery.extensions.getStringValue
import com.simplemobiletools.gallery.helpers.Config
import com.simplemobiletools.gallery.models.Medium
import java.io.File
import java.util.*

class GetMediaAsynctask(val context: Context, val mPath: String, val isPickVideo: Boolean = false, val isPickImage: Boolean = false,
                        val mToBeDeleted: List<String> = ArrayList<String>(), val callback: (media: ArrayList<Medium>) -> Unit) :
        AsyncTask<Void, Void, ArrayList<Medium>>() {
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
            val columns = arrayOf(MediaStore.Images.Media.DATA, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATE_MODIFIED, MediaStore.Images.Media.SIZE)
            var cursor: Cursor? = null

            try {
                cursor = context.contentResolver.query(uri, columns, where, args, null)

                if (cursor?.moveToFirst() == true) {
                    val pathIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                    do {
                        val curPath = cursor.getString(pathIndex) ?: continue
                        if (!mToBeDeleted.contains(curPath)) {
                            val file = File(curPath)
                            val size = cursor.getLongValue(MediaStore.Images.Media.SIZE)

                            if (size == 0L) {
                                invalidFiles.add(file)
                                continue
                            }

                            // exclude images of subdirectories
                            if (file.parent != mPath)
                                continue

                            val name = cursor.getStringValue(MediaStore.Images.Media.DISPLAY_NAME)
                            val timestamp = cursor.getLongValue(MediaStore.Images.Media.DATE_MODIFIED)
                            media.add(Medium(name, curPath, i == 1, timestamp, size))
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
