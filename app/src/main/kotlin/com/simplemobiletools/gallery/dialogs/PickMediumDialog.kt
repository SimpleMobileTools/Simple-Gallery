package com.simplemobiletools.gallery.dialogs

import android.support.v7.app.AlertDialog
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.SimpleActivity
import com.simplemobiletools.gallery.adapters.MediaAdapter
import com.simplemobiletools.gallery.asynctasks.GetMediaAsynctask
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.models.Medium
import kotlinx.android.synthetic.main.dialog_medium_picker.view.*

class PickMediumDialog(val activity: SimpleActivity, val path: String, val callback: (path: String) -> Unit) {
    var dialog: AlertDialog
    var mediaGrid: RecyclerView
    var shownMedia: ArrayList<Medium> = ArrayList()

    init {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_medium_picker, null)
        mediaGrid = view.media_grid

        dialog = AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            activity.setupDialogStuff(view, this, R.string.select_photo)

            val token = object : TypeToken<List<Medium>>() {}.type
            val media = Gson().fromJson<ArrayList<Medium>>(activity.config.loadFolderMedia(path), token) ?: ArrayList<Medium>(1)

            if (media.isNotEmpty()) {
                gotMedia(media)
            }

            GetMediaAsynctask(activity, path, false, true, false) {
                gotMedia(it)
            }.execute()
        }
    }

    private fun gotMedia(media: ArrayList<Medium>) {
        if (media.hashCode() == shownMedia.hashCode())
            return

        shownMedia = media
        val adapter = MediaAdapter(activity, media, null, true) {
            callback(it.path)
            dialog.dismiss()
        }
        mediaGrid.adapter = adapter
    }
}
