package com.simplemobiletools.gallery.pro.dialogs

import android.graphics.Point
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.gallery.pro.R
import kotlinx.android.synthetic.main.dialog_resize_image.view.*

class ResizeDialog(val activity: BaseSimpleActivity, val size: Point, val callback: (newSize: Point) -> Unit) {
    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_resize_image, null)
        val widthView = view.image_width
        val heightView = view.image_height

        widthView.setText(size.x.toString())
        heightView.setText(size.y.toString())

        val ratio = size.x / size.y.toFloat()

        widthView.onTextChangeListener {
            if (widthView.hasFocus()) {
                var width = getViewValue(widthView)
                if (width > size.x) {
                    widthView.setText(size.x.toString())
                    width = size.x
                }

                if (view.keep_aspect_ratio.isChecked) {
                    heightView.setText((width / ratio).toInt().toString())
                }
            }
        }

        heightView.onTextChangeListener {
            if (heightView.hasFocus()) {
                var height = getViewValue(heightView)
                if (height > size.y) {
                    heightView.setText(size.y.toString())
                    height = size.y
                }

                if (view.keep_aspect_ratio.isChecked) {
                    widthView.setText((height * ratio).toInt().toString())
                }
            }
        }

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
                    activity.setupDialogStuff(view, this, R.string.resize_and_save) {
                        showKeyboard(view.image_width)
                        getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            val width = getViewValue(widthView)
                            val height = getViewValue(heightView)
                            if (width <= 0 || height <= 0) {
                                activity.toast(R.string.invalid_values)
                                return@setOnClickListener
                            }

                            val newSize = Point(getViewValue(widthView), getViewValue(heightView))
                            callback(newSize)
                            dismiss()
                        }
                    }
                }
    }

    private fun getViewValue(view: EditText): Int {
        val textValue = view.value
        return if (textValue.isEmpty()) 0 else textValue.toInt()
    }
}
