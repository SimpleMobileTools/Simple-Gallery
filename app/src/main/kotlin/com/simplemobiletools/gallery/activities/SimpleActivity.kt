package com.simplemobiletools.gallery.activities

import android.support.v4.util.Pair
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.asynctasks.CopyMoveTask
import com.simplemobiletools.commons.extensions.isPathOnSD
import com.simplemobiletools.commons.extensions.scanFiles
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.dialogs.PickAlbumDialog
import java.io.File
import java.util.*

open class SimpleActivity : BaseSimpleActivity() {
    var copyMoveCallback: (() -> Unit)? = null

    fun tryCopyMoveFilesTo(files: ArrayList<File>, isCopyOperation: Boolean, callback: () -> Unit) {
        if (files.isEmpty()) {
            toast(R.string.unknown_error_occurred)
            return
        }

        val source = if (files[0].isFile) files[0].parent.trimEnd('/') else files[0].absolutePath.trimEnd('/')
        PickAlbumDialog(this, source) {
            copyMoveFilesTo(files, source, it, isCopyOperation, callback)
        }
    }

    private fun copyMoveFilesTo(files: ArrayList<File>, source: String, destination: String, isCopyOperation: Boolean, callback: () -> Unit) {
        if (source == destination) {
            toast(R.string.source_and_destination_same)
            return
        }

        val destinationFolder = File(destination)
        if (!destinationFolder.exists()) {
            toast(R.string.invalid_destination)
            return
        }

        if (files.size == 1) {
            if (File(destinationFolder.absolutePath, files[0].name).exists()) {
                toast(R.string.name_taken)
                return
            }
        }

        handleSAFDialog(destinationFolder) {
            copyMoveCallback = callback
            if (isCopyOperation) {
                toast(R.string.copying)
                val pair = Pair<ArrayList<File>, File>(files, destinationFolder)
                CopyMoveTask(this, isCopyOperation, true, copyMoveListener).execute(pair)
            } else {
                if (isPathOnSD(source) || isPathOnSD(destinationFolder.absolutePath)) {
                    handleSAFDialog(files[0]) {
                        toast(R.string.moving)
                        val pair = Pair<ArrayList<File>, File>(files, destinationFolder)
                        CopyMoveTask(this, isCopyOperation, true, copyMoveListener).execute(pair)
                    }
                } else {
                    val updatedFiles = ArrayList<File>(files.size * 2)
                    updatedFiles.addAll(files)
                    for (file in files) {
                        val newFile = File(destinationFolder, file.name)
                        if (!newFile.exists() && file.renameTo(newFile))
                            updatedFiles.add(newFile)
                    }

                    scanFiles(updatedFiles) {
                        runOnUiThread {
                            copyMoveListener.copySucceeded(true, files.size * 2 == updatedFiles.size)
                        }
                    }
                }
            }
        }
    }

    private val copyMoveListener = object : CopyMoveTask.CopyMoveListener {
        override fun copySucceeded(copyOnly: Boolean, copiedAll: Boolean) {
            if (copyOnly) {
                toast(if (copiedAll) R.string.copying_success else R.string.copying_success_partial)
            } else {
                toast(if (copiedAll) R.string.moving_success else R.string.moving_success_partial)
            }
            copyMoveCallback?.invoke()
            copyMoveCallback = null
        }

        override fun copyFailed() {
            toast(R.string.copy_move_failed)
            copyMoveCallback = null
        }
    }
}
