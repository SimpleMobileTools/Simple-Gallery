package com.simplemobiletools.gallery.pro.asynctasks

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.AsyncTask
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.vr.cardboard.ThreadUtils.runOnUiThread
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.deleteFiles
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.interfaces.DirectoryDao
import java.io.File

class ProgressNotificationAsyncTask(
        private val context: BaseSimpleActivity,
        private val fileDirItems: ArrayList<FileDirItem>,
        private val folders: ArrayList<File>,
        private val mDirectoryDao: DirectoryDao,
        private val listenerCancel: Listener
) : AsyncTask<Void, Int, Void>() {

    interface Listener {
        fun refresh()
    }

    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder

    override fun onPreExecute() {
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("Test",
                    context.resources.getString(R.string.notification_name),
                    NotificationManager.IMPORTANCE_HIGH)

            notificationBuilder = NotificationCompat.Builder(context, channel.id)
                    .setProgress(100, 0, true)
                    .setContentTitle(context.resources.getString(R.string.deleting_folder))
                    .setSmallIcon(R.drawable.ic_delete_vector)
                    .setAutoCancel(true)

            notificationManager.createNotificationChannel(channel)

            notificationManager.notify(0, notificationBuilder.build())
        }
    }

    override fun doInBackground(vararg p0: Void?): Void? {
        context.deleteFiles(fileDirItems) {
            runOnUiThread {
                listenerCancel.refresh()
            }

            ensureBackgroundThread {
                folders.filter { !it.exists() }.forEach {
                    mDirectoryDao.deleteDirPath(it.absolutePath)
                }
            }
        }
        return null
    }

    override fun onPostExecute(result: Void?) {
        notificationManager.cancel(0)
    }
}