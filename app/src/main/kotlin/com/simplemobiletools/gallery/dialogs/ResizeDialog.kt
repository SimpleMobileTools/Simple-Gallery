package com.simplemobiletools.gallery.dialogs

import android.graphics.Point
import androidx.appcompat.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.showKeyboard
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.extensions.value
import com.simplemobiletools.gallery.R
import kotlinx.android.synthetic.main.resize_image.view.*

class ResizeDialog(val activity: BaseSimpleActivity, val size: Point, val callback: (newSize: Point) -> Unit) {
    init {
        val view = activity.layoutInflater.inflate(R.layout.resize_image, null)
        val widthView = view.image_width
        val heightView = view.image_height

        widthView.setText(size.x.toString())
        heightView.setText(size.y.toString())

        val ratio = size.x / size.y.toFloat()

        widthView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
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

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        heightView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
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

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

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

    fun getViewValue(view: EditText): Int {
        val textValue = view.value
        return if (textValue.isEmpty()) 0 else textValue.toInt()
    }
}
