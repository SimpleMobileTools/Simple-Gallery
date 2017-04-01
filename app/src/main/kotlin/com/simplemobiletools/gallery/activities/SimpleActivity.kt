package com.simplemobiletools.gallery.activities

import android.os.Bundle
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    protected fun copyMoveFilesTo(source: String, files: ArrayList<File>, isCopyOperation: Boolean, callback: () -> Unit) {
        copyMoveCallback = callback
        val currPath = source.trimEnd('/')
        PickAlbumDialog(this, currPath) {
            val destinationFolder = File(it)
            if (currPath == it.trimEnd('/')) {
                toast(R.string.source_and_destination_same)
                return@PickAlbumDialog
            }

            if (!destinationFolder.exists()) {
                toast(R.string.invalid_destination)
                return@PickAlbumDialog
            }

            if (files.size == 1) {
                if (File(destinationFolder.absolutePath, files[0].name).exists()) {
                    toast(R.string.name_taken)
                    return@PickAlbumDialog
                }
            }

            handleSAFDialog(destinationFolder) {
                if (isCopyOperation) {
                    toast(R.string.copying)
                    val pair = Pair<ArrayList<File>, File>(files, destinationFolder)
                    CopyMoveTask(this, isCopyOperation, true, copyMoveListener).execute(pair)
                } else {
                    if (isPathOnSD(currPath) || isPathOnSD(destinationFolder.absolutePath)) {
                        handleSAFDialog(files[0]) {
                            toast(R.string.moving)
                            val pair = Pair<ArrayList<File>, File>(files, destinationFolder)
                            CopyMoveTask(this, isCopyOperation, true, copyMoveListener).execute(pair)
                        }
                    } else {
                        val updatedFiles = ArrayList<File>(files.size * 2)
                        updatedFiles.addAll(files)
                        for (file in files) {
                            val destination = File(destinationFolder, file.name)
                            if (!destination.exists() && file.renameTo(destination))
                                updatedFiles.add(destination)
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
