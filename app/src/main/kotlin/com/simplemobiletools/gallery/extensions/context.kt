package com.simplemobiletools.gallery.extensions

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.simplemobiletools.filepicker.extensions.humanizePath
import com.simplemobiletools.gallery.activities.SettingsActivity

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

fun Context.getHumanizedFilename(path: String): String {
    val humanized = humanizePath(path)
    return humanized.substring(humanized.lastIndexOf("/") + 1)
}

fun Context.launchCamera() {
    startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
}

fun Context.launchSettings() {
    startActivity(Intent(this, SettingsActivity::class.java))
}
