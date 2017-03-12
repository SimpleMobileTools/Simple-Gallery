package com.simplemobiletools.gallery.extensions

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.simplemobiletools.commons.extensions.humanizePath
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.SettingsActivity
import com.simplemobiletools.gallery.helpers.Config
import com.simplemobiletools.gallery.helpers.NOMEDIA
import java.io.File
import java.util.*

fun Context.getRealPathFromURI(uri: Uri): String? {
    var cursor: Cursor? = null
    try {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        cursor = contentResolver.query(uri, projection, null, null, null)
        if (cursor?.moveToFirst() == true) {
            val index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            return cursor.getString(index)
        }
    } catch (e: Exception) {
    } finally {
        cursor?.close()
    }
    return null
}

fun Context.getHumanizedFilename(path: String): String {
    val humanized = humanizePath(path)
    return humanized.substring(humanized.lastIndexOf("/") + 1)
}

fun Context.launchCamera() {
    val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
    if (intent.resolveActivity(packageManager) != null) {
        startActivity(intent)
    } else {
        toast(R.string.no_camera_app_found)
    }
}

fun Context.launchSettings() {
    startActivity(Intent(this, SettingsActivity::class.java))
}

fun Context.getParents(): ArrayList<String> {
    val uri = MediaStore.Files.getContentUri("external")
    val columns = arrayOf(MediaStore.Files.FileColumns.PARENT, MediaStore.Images.Media.DATA)
    val where = "${MediaStore.Images.Media.DATA} IS NOT NULL) GROUP BY (${MediaStore.Files.FileColumns.PARENT} "
    val parentsSet = HashSet<String>()

    var cursor: Cursor? = null
    try {
        cursor = contentResolver.query(uri, columns, where, null, null)
        if (cursor?.moveToFirst() == true) {
            do {
                val curPath = cursor.getStringValue(MediaStore.Images.Media.DATA) ?: continue
                val parent = File(curPath).parent ?: continue
                parentsSet.add(parent)
            } while (cursor.moveToNext())
        }
    } finally {
        cursor?.close()
    }

    val noMediaFolders = getNoMediaFolders()
    val parents = ArrayList<String>()
    if (config.showHiddenFolders) {
        parentsSet.addAll(noMediaFolders)
    }

    parentsSet.filterTo(parents, {
        if (File(it).isDirectory) {
            if (!config.showHiddenFolders) {
                isFolderVisible(it, noMediaFolders)
            } else
                true
        } else {
            false
        }
    })

    return parents
}

private fun isFolderVisible(path: String, noMediaFolders: ArrayList<String>): Boolean {
    return if (path.contains("/.")) {
        false
    } else {
        !noMediaFolders.any { path.startsWith(it) }
    }
}

fun Context.getNoMediaFolders(): ArrayList<String> {
    val folders = ArrayList<String>()
    val noMediaCondition = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_NONE}"

    val uri = MediaStore.Files.getContentUri("external")
    val columns = arrayOf(MediaStore.Files.FileColumns.DATA)
    val where = "$noMediaCondition AND ${MediaStore.Files.FileColumns.TITLE} LIKE ?"
    val args = arrayOf("%$NOMEDIA%")
    var cursor: Cursor? = null

    try {
        cursor = contentResolver.query(uri, columns, where, args, null)
        if (cursor?.moveToFirst() == true) {
            do {
                val path = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)) ?: continue
                val noMediaFile = File(path)
                if (noMediaFile.exists())
                    folders.add(noMediaFile.parent)
            } while (cursor.moveToNext())
        }
    } finally {
        cursor?.close()
    }

    return folders
}

val Context.config: Config get() = Config.newInstance(this)
