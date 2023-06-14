package com.simplemobiletools.commons.dialogs

import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.BLOCKED_NUMBERS_EXPORT_EXTENSION
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import kotlinx.android.synthetic.main.dialog_export_blocked_numbers.view.*
import java.io.File

class ExportBlockedNumbersDialog(
    val activity: BaseSimpleActivity,
    val path: String,
    val hidePath: Boolean,
    callback: (file: File) -> Unit,
) {
    private var realPath = if (path.isEmpty()) activity.internalStoragePath else path
    private val config = activity.baseConfig

    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_export_blocked_numbers, null).apply {
            export_blocked_numbers_folder.text = activity.humanizePath(realPath)
            export_blocked_numbers_filename.setText("${activity.getString(R.string.blocked_numbers)}_${activity.getCurrentFormattedDateTime()}")

            if (hidePath) {
                export_blocked_numbers_folder_label.beGone()
                export_blocked_numbers_folder.beGone()
            } else {
                export_blocked_numbers_folder.setOnClickListener {
                    FilePickerDialog(activity, realPath, false, showFAB = true) {
                        export_blocked_numbers_folder.text = activity.humanizePath(it)
                        realPath = it
                    }
                }
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this, R.string.export_blocked_numbers) { alertDialog ->
                    alertDialog.showKeyboard(view.export_blocked_numbers_filename)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val filename = view.export_blocked_numbers_filename.value
                        when {
                            filename.isEmpty() -> activity.toast(R.string.empty_name)
                            filename.isAValidFilename() -> {
                                val file = File(realPath, "$filename$BLOCKED_NUMBERS_EXPORT_EXTENSION")
                                if (!hidePath && file.exists()) {
                                    activity.toast(R.string.name_taken)
                                    return@setOnClickListener
                                }

                                ensureBackgroundThread {
                                    config.lastBlockedNumbersExportPath = file.absolutePath.getParentPath()
                                    callback(file)
                                    alertDialog.dismiss()
                                }
                            }
                            else -> activity.toast(R.string.invalid_name)
                        }
                    }
                }
            }
    }
}
