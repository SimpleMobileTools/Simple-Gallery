package org.fossify.gallery.dialogs

import android.graphics.Point
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.dialogs.ConfirmationDialog
import org.fossify.commons.dialogs.FilePickerDialog
import org.fossify.commons.extensions.*
import org.fossify.gallery.R
import org.fossify.gallery.databinding.DialogResizeImageWithPathBinding
import org.fossify.gallery.extensions.config

class ResizeWithPathDialog(val activity: BaseSimpleActivity, val size: Point, val path: String, val callback: (newSize: Point, newPath: String) -> Unit) {
    init {
        var realPath = path.getParentPath()
        val binding = DialogResizeImageWithPathBinding.inflate(activity.layoutInflater).apply {
            folder.setText("${activity.humanizePath(realPath).trimEnd('/')}/")

            val fullName = path.getFilenameFromPath()
            val dotAt = fullName.lastIndexOf(".")
            var name = fullName

            if (dotAt > 0) {
                name = fullName.substring(0, dotAt)
                val extension = fullName.substring(dotAt + 1)
                extensionValue.setText(extension)
            }

            filenameValue.setText(name)
            folder.setOnClickListener {
                FilePickerDialog(activity, realPath, false, activity.config.shouldShowHidden, true, true) {
                    folder.setText(activity.humanizePath(it))
                    realPath = it
                }
            }
        }

        val widthView = binding.resizeImageWidth
        val heightView = binding.resizeImageHeight

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

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this) { alertDialog ->
                    alertDialog.showKeyboard(binding.resizeImageWidth)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val width = getViewValue(widthView)
                        val height = getViewValue(heightView)
                        if (width <= 0 || height <= 0) {
                            activity.toast(R.string.invalid_values)
                            return@setOnClickListener
                        }

                        val newSize = Point(getViewValue(widthView), getViewValue(heightView))

                        val filename = binding.filenameValue.value
                        val extension = binding.extensionValue.value
                        if (filename.isEmpty()) {
                            activity.toast(org.fossify.commons.R.string.filename_cannot_be_empty)
                            return@setOnClickListener
                        }

                        if (extension.isEmpty()) {
                            activity.toast(org.fossify.commons.R.string.extension_cannot_be_empty)
                            return@setOnClickListener
                        }

                        val newFilename = "$filename.$extension"
                        val newPath = "${realPath.trimEnd('/')}/$newFilename"
                        if (!newFilename.isAValidFilename()) {
                            activity.toast(org.fossify.commons.R.string.filename_invalid_characters)
                            return@setOnClickListener
                        }

                        if (activity.getDoesFilePathExist(newPath)) {
                            val title = String.format(activity.getString(org.fossify.commons.R.string.file_already_exists_overwrite), newFilename)
                            ConfirmationDialog(activity, title) {
                                callback(newSize, newPath)
                                alertDialog.dismiss()
                            }
                        } else {
                            callback(newSize, newPath)
                            alertDialog.dismiss()
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
