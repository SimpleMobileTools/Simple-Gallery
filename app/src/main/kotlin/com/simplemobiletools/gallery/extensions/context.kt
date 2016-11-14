package com.simplemobiletools.gallery.extensions

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore

fun Context.getRealPathFromURI(uri: Uri): String? {
    var cursor: Cursor? = null
    try {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        cursor = contentResolver.query(uri, projection, null, null, null)
        if (cursor != null && cursor.moveToFirst()) {
            val index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            return cursor.getString(index)
        }
        return null
    } finally {
        cursor?.close()
    }
}
