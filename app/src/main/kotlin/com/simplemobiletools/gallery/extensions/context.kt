package com.simplemobiletools.gallery.extensions

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.simplemobiletools.commons.extensions.humanizePath
import com.simplemobiletools.commons.extensions.isImageVideoGif
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.SettingsActivity
import com.simplemobiletools.gallery.helpers.Config
import com.simplemobiletools.gallery.helpers.IMAGES
import com.simplemobiletools.gallery.helpers.NOMEDIA
import com.simplemobiletools.gallery.helpers.VIDEOS
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

fun Context.getParents(isPickImage: Boolean, isPickVideo: Boolean): ArrayList<String> {
    val uri = MediaStore.Files.getContentUri("external")
    val where = "${getWhereCondition(isPickImage, isPickVideo)} GROUP BY ( ${MediaStore.Files.FileColumns.PARENT} "
    val args = getArgs(isPickImage, isPickVideo)
    val columns = arrayOf(MediaStore.Files.FileColumns.PARENT, MediaStore.Images.Media.DATA)
    var cursor: Cursor? = null
    val parents = ArrayList<String>()

    try {
        cursor = contentResolver.query(uri, columns, where, args, null)
        if (cursor?.moveToFirst() == true) {
            do {
                val curPath = cursor.getStringValue(MediaStore.Images.Media.DATA) ?: ""
                parents.add(File(curPath).parent)
            } while (cursor.moveToNext())
        }
    } finally {
        cursor?.close()
    }

    val filtered = ArrayList<String>()
    parents.mapNotNullTo(filtered, { it })

    if (config.showHiddenFolders) {
        filtered.addAll(getNoMediaFolders())
    } else {
        removeNoMediaFolders(filtered)
    }
    return filtered
}

fun Context.getWhereCondition(isPickImage: Boolean, isPickVideo: Boolean): String {
    val showMedia = config.showMedia
    return if ((isPickImage || showMedia == IMAGES) || (isPickVideo || showMedia == VIDEOS)) {
        "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?)"
    } else {
        "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?)"
    }
}

fun Context.getArgs(isPickImage: Boolean, isPickVideo: Boolean): Array<String> {
    val showMedia = config.showMedia
    return if (isPickImage || showMedia == IMAGES) {
        arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString())
    } else if (isPickVideo || showMedia == VIDEOS) {
        arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
    } else {
        arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(), MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
    }
}

private fun removeNoMediaFolders(paths: MutableList<String>) {
    val ignorePaths = ArrayList<String>()
    for (path in paths) {
        val dir = File(path)
        if (dir.exists() && dir.isDirectory) {
            val res = dir.list { file, filename -> filename == NOMEDIA }
            if (res?.isNotEmpty() == true)
                ignorePaths.add(path)
        }
    }

    paths.removeAll(ignorePaths)
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
                val parent = File(path).parentFile
                if (hasImageVideoGif(parent)) {
                    folders.add(parent.absolutePath)
                }
            } while (cursor.moveToNext())
        }
    } finally {
        cursor?.close()
    }

    return folders
}

fun hasImageVideoGif(dir: File): Boolean {
    if (dir.isDirectory) {
        dir.listFiles()
                .filter(File::isImageVideoGif)
                .forEach { return true }
    }
    return false
}

val Context.config: Config get() = Config.newInstance(this)
