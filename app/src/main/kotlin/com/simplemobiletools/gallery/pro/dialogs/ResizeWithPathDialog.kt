package com.simplemobiletools.gallery.pro.dialogs

import android.graphics.Point
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.extensions.config
import kotlinx.android.synthetic.main.dialog_resize_image_with_path.view.*

class ResizeWithPathDialog(val activity: BaseSimpleActivity, val size: Point, val path: String, val callback: (newSize: Point, newPath: String) -> Unit) {
    init {
        var realPath = path.getParentPath()
        val view = activity.layoutInflater.inflate(R.layout.dialog_resize_image_with_path, null).apply {
            image_path.text = "${activity.humanizePath(realPath).trimEnd('/')}/"

            val fullName = path.getFilenameFromPath()
            val dotAt = fullName.lastIndexOf(".")
            var name = fullName

            if (dotAt > 0) {
                name = fullName.substring(0, dotAt)
                val extension = fullName.substring(dotAt + 1)
                image_extension.setText(extension)
            }

            image_name.setText(name)
            image_path.setOnClickListener {
                FilePickerDialog(activity, realPath, false, activity.config.shouldShowHidden, true, true) {
                    image_path.text = activity.humanizePath(it)
                    realPath = it
                }
            }
        }

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

                heightView.setText((width / ratio).toInt().toString())
            }
        }

        heightView.onTextChangeListener {
            if (heightView.hasFocus()) {
                var height = getViewValue(heightView)
                if (height > size.y) {
                    heightView.setText(size.y.toString())
                    height = size.y
                }

                widthView.setText((height * ratio).toInt().toString())
            }
        }

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
                    activity.setupDialogStuff(view, this) {
                        showKeyboard(view.image_width)
                        getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            val width = getViewValue(widthView)
                            val height = getViewValue(heightView)
                            if (width <= 0 || height <= 0) {
                                activity.toast(R.string.invalid_values)
                                return@setOnClickListener
                            }

                            val newSize = Point(getViewValue(widthView), getViewValue(heightView))

                            val filename = view.image_name.value
                            val extension = view.image_extension.value
                            if (filename.isEmpty()) {
                                activity.toast(R.string.filename_cannot_be_empty)
                                return@setOnClickListener
                            }

                            if (extension.isEmpty()) {
                                activity.toast(R.string.extension_cannot_be_empty)
                                return@setOnClickListener
                            }

                            val newFilename = "$filename.$extension"
                            val newPath = "${realPath.trimEnd('/')}/$newFilename"
                            if (!newFilename.isAValidFilename()) {
                                activity.toast(R.string.filename_invalid_characters)
                                return@setOnClickListener
                            }

                            if (activity.getDoesFilePathExist(newPath)) {
                                val title = String.format(activity.getString(R.string.file_already_exists_overwrite), newFilename)
                                ConfirmationDialog(activity, title) {
                                    callback(newSize, newPath)
                                    dismiss()
                                }
                            } else {
                                callback(newSize, newPath)
                                dismiss()
                            }
                        }
                    }
                }
    }

    private fun getViewValue(view: EditText): Int {
        val textValue = view.value
        return if (textValue.isEmpty()) 0 else textValue.toInt()
    }
}
