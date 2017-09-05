package com.simplemobiletools.gallery.activities

import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.getFilenameFromPath
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.dialogs.PickDirectoryDialog
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.models.Directory
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

    fun addTempFolderIfNeeded(dirs: ArrayList<Directory>): ArrayList<Directory> {
        val directories = ArrayList<Directory>()
        val tempFolderPath = config.tempFolderPath
        if (tempFolderPath.isNotEmpty()) {
            val newFolder = Directory(tempFolderPath, "", tempFolderPath.getFilenameFromPath(), 0, 0, 0, 0L)
            directories.add(newFolder)
        }
        directories.addAll(dirs)
        return directories
    }
}
