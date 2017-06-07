package com.simplemobiletools.gallery.activities

import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.dialogs.PickDirectoryDialog
import java.io.File
import java.util.*

open class SimpleActivity : BaseSimpleActivity() {
    fun tryCopyMoveFilesTo(files: ArrayList<File>, isCopyOperation: Boolean, callback: () -> Unit) {
        if (files.isEmpty()) {
            toast(R.string.unknown_error_occurred)
            return
        }

        val source = if (files[0].isFile) files[0].parent else files[0].absolutePath
        PickDirectoryDialog(this, source) {
            copyMoveFilesTo(files, source.trimEnd('/'), it, isCopyOperation, true, callback)
        }
    }
}
