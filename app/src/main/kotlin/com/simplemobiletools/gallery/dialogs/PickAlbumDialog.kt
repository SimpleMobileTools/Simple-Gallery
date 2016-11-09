package com.simplemobiletools.gallery.dialogs

import android.app.Activity
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.GridView
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.adapters.DirectoryAdapter
import com.simplemobiletools.gallery.asynctasks.GetDirectoriesAsynctask
import com.simplemobiletools.gallery.models.Directory
import kotlinx.android.synthetic.main.activity_main.view.*
import java.util.*

class PickAlbumDialog(val activity: Activity, val listener: OnPickAlbumListener) : AdapterView.OnItemClickListener, GetDirectoriesAsynctask.GetDirectoriesListener {
    val context = activity.applicationContext
    var grid: GridView? = null
    var dirs = ArrayList<Directory>()
    var dialog: AlertDialog? = null

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_album_picker, null)
        grid = view.directories_grid

        dialog = AlertDialog.Builder(activity)
                .setTitle(context.resources.getString(R.string.select_destination))
                .setView(view)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            show()
        }

        GetDirectoriesAsynctask(context, false, false, ArrayList<String>(), this).execute()
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        listener.onSuccess(dirs[position].path)
        dialog?.dismiss()
    }

    override fun gotDirectories(dirs: ArrayList<Directory>) {
        this.dirs = dirs

        val adapter = DirectoryAdapter(context, dirs)

        grid?.adapter = adapter
        grid?.onItemClickListener = this
    }

    interface OnPickAlbumListener {
        fun onSuccess(path: String)
    }
}
