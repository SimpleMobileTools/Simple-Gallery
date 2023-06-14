package com.simplemobiletools.commons.extensions

import android.content.ContentValues
import android.provider.MediaStore
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.models.FileDirItem
import java.io.File
import java.io.InputStream
import java.io.OutputStream

fun BaseSimpleActivity.copySingleFileSdk30(source: FileDirItem, destination: FileDirItem): Boolean {
    val directory = destination.getParentPath()
    if (!createDirectorySync(directory)) {
        val error = String.format(getString(R.string.could_not_create_folder), directory)
        showErrorToast(error)
        return false
    }

    var inputStream: InputStream? = null
    var out: OutputStream? = null
    try {

        out = getFileOutputStreamSync(destination.path, source.path.getMimeType())
        inputStream = getFileInputStreamSync(source.path)!!

        var copiedSize = 0L
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var bytes = inputStream.read(buffer)
        while (bytes >= 0) {
            out!!.write(buffer, 0, bytes)
            copiedSize += bytes
            bytes = inputStream.read(buffer)
        }

        out?.flush()

        return if (source.size == copiedSize && getDoesFilePathExist(destination.path)) {
            if (baseConfig.keepLastModified) {
                copyOldLastModified(source.path, destination.path)
                val lastModified = File(source.path).lastModified()
                if (lastModified != 0L) {
                    File(destination.path).setLastModified(lastModified)
                }
            }
            true
        } else {
            false
        }
    } finally {
        inputStream?.close()
        out?.close()
    }
}

fun BaseSimpleActivity.copyOldLastModified(sourcePath: String, destinationPath: String) {
    val projection = arrayOf(MediaStore.Images.Media.DATE_TAKEN, MediaStore.Images.Media.DATE_MODIFIED)
    val uri = MediaStore.Files.getContentUri("external")
    val selection = "${MediaStore.MediaColumns.DATA} = ?"
    var selectionArgs = arrayOf(sourcePath)
    val cursor = applicationContext.contentResolver.query(uri, projection, selection, selectionArgs, null)

    cursor?.use {
        if (cursor.moveToFirst()) {
            val dateTaken = cursor.getLongValue(MediaStore.Images.Media.DATE_TAKEN)
            val dateModified = cursor.getIntValue(MediaStore.Images.Media.DATE_MODIFIED)

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DATE_TAKEN, dateTaken)
                put(MediaStore.Images.Media.DATE_MODIFIED, dateModified)
            }

            selectionArgs = arrayOf(destinationPath)
            applicationContext.contentResolver.update(uri, values, selection, selectionArgs)
        }
    }
}
