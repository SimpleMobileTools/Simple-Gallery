package com.simplemobiletools.gallery.dialogs

import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.SimpleActivity
import com.simplemobiletools.gallery.adapters.DirectoryAdapter
import com.simplemobiletools.gallery.asynctasks.GetDirectoriesAsynctask
import com.simplemobiletools.gallery.models.Directory
import java.util.*

class PickAlbumDialog(val activity: SimpleActivity, val listener: OnPickAlbumListener) : GetDirectoriesAsynctask.GetDirectoriesListener {
    val context = activity.applicationContext
    var dialog: AlertDialog

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_album_picker, null)

        dialog = AlertDialog.Builder(activity)
                .setTitle(context.resources.getString(R.string.select_destination))
                .setView(view)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create()

        dialog.show()

        GetDirectoriesAsynctask(context, false, false, ArrayList<String>(), this).execute()
    }

    override fun gotDirectories(dirs: ArrayList<Directory>) {
        DirectoryAdapter(activity, dirs) {
            listener.onSuccess(it.path)
            dialog.dismiss()
        }
    }

    interface OnPickAlbumListener {
        fun onSuccess(path: String)
    }
}
