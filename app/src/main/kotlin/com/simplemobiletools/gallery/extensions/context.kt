package com.simplemobiletools.gallery.extensions

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.database.Cursor
import android.media.AudioManager
import android.net.Uri
import android.provider.MediaStore
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.SORT_BY_DATE_MODIFIED
import com.simplemobiletools.commons.helpers.SORT_BY_NAME
import com.simplemobiletools.commons.helpers.SORT_BY_SIZE
import com.simplemobiletools.commons.helpers.SORT_DESCENDING
import com.simplemobiletools.gallery.activities.SettingsActivity
import com.simplemobiletools.gallery.helpers.*
import com.simplemobiletools.gallery.models.Medium
import java.io.File

val Context.portrait get() = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
val Context.audioManager get() = getSystemService(Context.AUDIO_SERVICE) as AudioManager

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

fun Context.launchSettings() {
    startActivity(Intent(this, SettingsActivity::class.java))
}

val Context.config: Config get() = Config.newInstance(this)

fun Context.getFilesFrom(curPath: String, isPickImage: Boolean, isPickVideo: Boolean): ArrayList<Medium> {
    val projection = arrayOf(MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.SIZE)
    val uri = MediaStore.Files.getContentUri("external")
    val selection = if (curPath.isEmpty()) null else "(${MediaStore.Images.Media.DATA} LIKE ? AND ${MediaStore.Images.Media.DATA} NOT LIKE ?)"
    val selectionArgs = if (curPath.isEmpty()) null else arrayOf("$curPath/%", "$curPath/%/%")

    return try {
        val cur = contentResolver.query(uri, projection, selection, selectionArgs, getSortingForFolder(curPath))
        parseCursor(this, cur, isPickImage, isPickVideo, curPath)
    } catch (e: Exception) {
        ArrayList()
    }
}

private fun parseCursor(context: Context, cur: Cursor, isPickImage: Boolean, isPickVideo: Boolean, curPath: String): ArrayList<Medium> {
    val curMedia = ArrayList<Medium>()
    val config = context.config
    val filterMedia = config.filterMedia
    val showHidden = config.shouldShowHidden
    val includedFolders = config.includedFolders.map { "$it/" }
    val excludedFolders = config.excludedFolders.map { "$it/" }
    val noMediaFolders = context.getNoMediaFolders()
    val isThirdPartyIntent = config.isThirdPartyIntent

    cur.use {
        if (cur.moveToFirst()) {
            do {
                try {
                    val path = cur.getStringValue(MediaStore.Images.Media.DATA)
                    var size = cur.getLongValue(MediaStore.Images.Media.SIZE)
                    if (size == 0L) {
                        size = File(path).length()
                    }

                    if (size <= 0L) {
                        continue
                    }

                    var filename = cur.getStringValue(MediaStore.Images.Media.DISPLAY_NAME) ?: ""
                    if (filename.isEmpty())
                        filename = path.getFilenameFromPath()

                    val isImage = filename.isImageFast()
                    val isVideo = if (isImage) false else filename.isVideoFast()
                    val isGif = if (isImage || isVideo) false else filename.isGif()

                    if (!isImage && !isVideo && !isGif)
                        continue

                    if (isVideo && (isPickImage || filterMedia and VIDEOS == 0))
                        continue

                    if (isImage && (isPickVideo || filterMedia and IMAGES == 0))
                        continue

                    if (isGif && filterMedia and GIFS == 0)
                        continue

                    if (!showHidden && filename.startsWith('.'))
                        continue

                    var isExcluded = false
                    excludedFolders.forEach {
                        if (path.startsWith(it)) {
                            isExcluded = true
                            includedFolders.forEach {
                                if (path.startsWith(it)) {
                                    isExcluded = false
                                }
                            }
                        }
                    }

                    if (!isExcluded && !showHidden) {
                        noMediaFolders.forEach {
                            if (path.startsWith(it)) {
                                isExcluded = true
                            }
                        }
                    }

                    if (!isExcluded && !showHidden && path.contains("/.")) {
                        isExcluded = true
                    }

                    if (!isExcluded || isThirdPartyIntent) {
                        val dateTaken = cur.getLongValue(MediaStore.Images.Media.DATE_TAKEN)
                        val dateModified = cur.getIntValue(MediaStore.Images.Media.DATE_MODIFIED) * 1000L

                        val medium = Medium(filename, path, isVideo, dateModified, dateTaken, size)
                        curMedia.add(medium)
                    }
                } catch (e: Exception) {
                    continue
                }
            } while (cur.moveToNext())
        }
    }

    config.includedFolders.filter { it.isNotEmpty() && (curPath.isEmpty() || it == curPath) }.forEach {
        getMediaInFolder(it, curMedia, isPickImage, isPickVideo, filterMedia)
    }

    if (isThirdPartyIntent && curPath.isNotEmpty() && curMedia.isEmpty()) {
        getMediaInFolder(curPath, curMedia, isPickImage, isPickVideo, filterMedia)
    }

    Medium.sorting = config.getFileSorting(curPath)
    curMedia.sort()

    return curMedia
}

private fun getMediaInFolder(folder: String, curMedia: ArrayList<Medium>, isPickImage: Boolean, isPickVideo: Boolean, filterMedia: Int) {
    val files = File(folder).listFiles() ?: return
    for (file in files) {
        val size = file.length()
        if (size <= 0L) {
            continue
        }

        val filename = file.name
        val isImage = filename.isImageFast()
        val isVideo = if (isImage) false else filename.isVideoFast()
        val isGif = if (isImage || isVideo) false else filename.isGif()

        if (!isImage && !isVideo)
            continue

        if (isVideo && (isPickImage || filterMedia and VIDEOS == 0))
            continue

        if (isImage && (isPickVideo || filterMedia and IMAGES == 0))
            continue

        if (isGif && filterMedia and GIFS == 0)
            continue

        val dateTaken = file.lastModified()
        val dateModified = file.lastModified()

        val medium = Medium(filename, file.absolutePath, isVideo, dateModified, dateTaken, size)
        val isAlreadyAdded = curMedia.any { it.path == file.absolutePath }
        if (!isAlreadyAdded)
            curMedia.add(medium)
    }
}

fun Context.getSortingForFolder(path: String): String {
    val sorting = config.getFileSorting(path)
    val sortValue = when {
        sorting and SORT_BY_NAME > 0 -> MediaStore.Images.Media.DISPLAY_NAME
        sorting and SORT_BY_SIZE > 0 -> MediaStore.Images.Media.SIZE
        sorting and SORT_BY_DATE_MODIFIED > 0 -> MediaStore.Images.Media.DATE_MODIFIED
        else -> MediaStore.Images.Media.DATE_TAKEN
    }

    return if (sorting and SORT_DESCENDING > 0)
        "$sortValue DESC"
    else
        "$sortValue ASC"
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
                    folders.add("${noMediaFile.parent}/")
            } while (cursor.moveToNext())
        }
    } finally {
        cursor?.close()
    }

    return folders
}

fun Context.getLastMediaModified(): Int {
    val max = "max"
    val uri = MediaStore.Files.getContentUri("external")
    val projection = arrayOf(MediaStore.Images.Media._ID, "MAX(${MediaStore.Images.Media.DATE_MODIFIED}) AS $max")
    var cursor: Cursor? = null
    try {
        cursor = contentResolver.query(uri, projection, null, null, null)
        if (cursor?.moveToFirst() == true) {
            return cursor.getIntValue(max)
        }
    } finally {
        cursor?.close()
    }
    return 0
}
