package com.simplemobiletools.gallery.pro.dialogs

import android.graphics.Point
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.extensions.config
import com.simplemobiletools.gallery.pro.extensions.ensureWriteAccess
import com.simplemobiletools.gallery.pro.extensions.fixDateTaken
import com.simplemobiletools.gallery.pro.extensions.resizeImage
import kotlinx.android.synthetic.main.dialog_resize_multiple_images.view.resize_factor_edit_text
import kotlinx.android.synthetic.main.dialog_resize_multiple_images.view.resize_factor_info
import kotlinx.android.synthetic.main.dialog_resize_multiple_images.view.resize_factor_input_layout
import kotlinx.android.synthetic.main.dialog_resize_multiple_images.view.resize_progress
import java.io.File
import kotlin.math.roundToInt

private const val DEFAULT_RESIZE_FACTOR = "75"
private const val RESIZE_FACTOR_ERROR_DELAY = 800L

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
        setupViews(view)
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
                        val resizeFactor = try {
                            resizeFactorText?.toFloat()?.div(100)
                        } catch (e: Exception) {
                            null
                        }

                        if (resizeFactor == null) {
                            activity.toast(R.string.resize_factor_error)
                            return@setOnClickListener
                        }

                        alertDialog.setCanceledOnTouchOutside(false)
                        arrayOf(view.resize_factor_input_layout, view.resize_factor_info, positiveButton, negativeButton).forEach {
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

                    rescanPaths(pathsToRescan) {
                        fixDateTaken(pathsToRescan, false)
                        for (path in pathsToRescan) {
                            val file = File(path)
                            val lastModified = file.lastModified()
                            if (config.keepLastModified && lastModified != 0L) {
                                File(file.absolutePath).setLastModified(lastModified)
                                updateLastModified(file.absolutePath, lastModified)
                            }
                        }
                    }
                    activity.runOnUiThread {
                        dialog?.dismiss()
                        callback.invoke()
                    }
                }
            }
        }
    }

    private fun setupViews(view: View) {
        val handler = Handler(Looper.getMainLooper())
        val resizeFactorInputLayout = view.resize_factor_input_layout
        view.resize_factor_edit_text.apply {
            setText(DEFAULT_RESIZE_FACTOR)
            doAfterTextChanged {
                resizeFactorInputLayout.error = null
                handler.removeCallbacksAndMessages(null)
                handler.postDelayed({
                    val factorText = it?.toString()
                    if (factorText.isNullOrEmpty() || factorText.toInt() !in 10..90) {
                        resizeFactorInputLayout.error = activity.getString(R.string.resize_factor_error)
                    } else {
                        resizeFactorInputLayout.error = null
                    }
                }, RESIZE_FACTOR_ERROR_DELAY)
            }
        }

        progressView.apply {
            max = imagePaths.size
            setIndicatorColor(activity.getProperPrimaryColor())
        }
    }
}
