package com.simplemobiletools.gallery.pro.dialogs

import android.graphics.Point
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.extensions.ensureWriteAccess
import com.simplemobiletools.gallery.pro.extensions.rescanPathsAndUpdateLastModified
import com.simplemobiletools.gallery.pro.extensions.resizeImage
import kotlinx.android.synthetic.main.dialog_resize_multiple_images.view.resize_factor_edit_text
import kotlinx.android.synthetic.main.dialog_resize_multiple_images.view.resize_factor_input_layout
import kotlinx.android.synthetic.main.dialog_resize_multiple_images.view.resize_progress
import kotlin.math.roundToInt

private const val DEFAULT_RESIZE_FACTOR = "75"

class ResizeMultipleImagesDialog(
    private val activity: BaseSimpleActivity,
    private val imagePaths: List<String>,
    private val imageSizes: List<Point>,
    private val callback: () -> Unit
) {

    private var dialog: AlertDialog? = null
    private val view = activity.layoutInflater.inflate(R.layout.dialog_resize_multiple_images, null)
    private val progressView = view.resize_progress
    private val resizeFactorEditText = view.resize_factor_edit_text

    init {
        resizeFactorEditText.setText(DEFAULT_RESIZE_FACTOR)
        progressView.apply {
            max = imagePaths.size
            setIndicatorColor(activity.getProperPrimaryColor())
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this, R.string.resize_multiple_images) { alertDialog ->
                    dialog = alertDialog
                    alertDialog.showKeyboard(resizeFactorEditText)

                    val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    val negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    positiveButton.setOnClickListener {
                        val resizeFactorText = resizeFactorEditText.text?.toString()
                        if (resizeFactorText.isNullOrEmpty() || resizeFactorText.toInt() !in 10..90) {
                            activity.toast(R.string.resize_factor_error)
                            return@setOnClickListener
                        }

                        val resizeFactor = resizeFactorText.toFloat().div(100)

                        alertDialog.setCanceledOnTouchOutside(false)
                        arrayOf(view.resize_factor_input_layout, positiveButton, negativeButton).forEach {
                            it.isEnabled = false
                            it.alpha = 0.6f
                        }
                        resizeImages(resizeFactor)
                    }
                }
            }
    }

    private fun resizeImages(factor: Float) {
        progressView.show()
        ensureBackgroundThread {
            with(activity) {
                val newSizes = imageSizes.map {
                    val width = (it.x * factor).roundToInt()
                    val height = (it.y * factor).roundToInt()
                    Point(width, height)
                }

                val parentPath = imagePaths.first().getParentPath()
                val pathsToRescan = arrayListOf<String>()

                ensureWriteAccess(parentPath) {
                    for (i in imagePaths.indices) {
                        val path = imagePaths[i]
                        val size = newSizes[i]

                        try {
                            resizeImage(path, size) {
                                if (it) {
                                    pathsToRescan.add(path)
                                    runOnUiThread {
                                        progressView.progress = i + 1
                                    }
                                }
                            }
                        } catch (e: OutOfMemoryError) {
                            toast(R.string.out_of_memory_error)
                        } catch (e: Exception) {
                            showErrorToast(e)
                        }
                    }

                    val failureCount = imagePaths.size - pathsToRescan.size
                    if (failureCount > 0) {
                        toast(getString(R.string.failed_to_resize_images, failureCount))
                    } else {
                        toast(R.string.images_resized_successfully)
                    }

                    rescanPathsAndUpdateLastModified(pathsToRescan)
                    activity.runOnUiThread {
                        dialog?.dismiss()
                        callback.invoke()
                    }
                }
            }
        }
    }
}
