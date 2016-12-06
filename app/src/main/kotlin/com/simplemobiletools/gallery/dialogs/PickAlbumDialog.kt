package com.simplemobiletools.gallery.dialogs

import android.app.AlertDialog
import android.os.Environment
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import com.simplemobiletools.filepicker.dialogs.FilePickerDialog
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.SimpleActivity
import com.simplemobiletools.gallery.adapters.DirectoryAdapter
import com.simplemobiletools.gallery.asynctasks.GetDirectoriesAsynctask
import com.simplemobiletools.gallery.helpers.Config
import kotlinx.android.synthetic.main.dialog_album_picker.view.*
import java.util.*

class PickAlbumDialog(val activity: SimpleActivity, val callback: (path: String) -> Unit) {
    var dialog: AlertDialog
    var directoriesGrid: RecyclerView

    init {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_album_picker, null)
        directoriesGrid = view.directories_grid

        dialog = AlertDialog.Builder(activity)
                .setTitle(activity.resources.getString(R.string.select_destination))
                .setView(view)
                .setNeutralButton(R.string.other_folder, { dialogInterface, i -> showOtherFolder() })
                .setPositiveButton(R.string.ok, null)
                .create()

        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
        GetDirectoriesAsynctask(activity, false, false, ArrayList<String>()) {
            val adapter = DirectoryAdapter(activity, it, null) {
                callback.invoke(it.path)
                dialog.dismiss()
            }
            directoriesGrid.adapter = adapter
        }.execute()
    }

    fun showOtherFolder() {
        val initialPath = Environment.getExternalStorageDirectory().toString()
        val showHidden = Config.newInstance(activity).showHiddenFolders
        FilePickerDialog(activity, initialPath, false, showHidden, object : FilePickerDialog.OnFilePickerListener {
            override fun onSuccess(pickedPath: String) {
                callback.invoke(pickedPath)
            }

            override fun onFail(error: FilePickerDialog.FilePickerResult) {
            }
        })
    }
}
