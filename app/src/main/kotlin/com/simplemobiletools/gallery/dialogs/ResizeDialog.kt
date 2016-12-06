package com.simplemobiletools.gallery.dialogs

import android.app.AlertDialog
import android.graphics.Point
import android.util.Size
import android.view.LayoutInflater
import android.view.WindowManager
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.SimpleActivity
import kotlinx.android.synthetic.main.resize_image.view.*

class ResizeDialog(val activity: SimpleActivity, val size: Point, val callback: (size: Size) -> Unit) {
    init {
        val view = LayoutInflater.from(activity).inflate(R.layout.resize_image, null)
        view.image_width.setText(size.x.toString())
        view.image_height.setText(size.y.toString())

        AlertDialog.Builder(activity)
                .setTitle(activity.resources.getString(R.string.resize_and_save))
                .setView(view)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            setCanceledOnTouchOutside(true)
            show()
            getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener({

            })
        }
    }
}
