package com.simplemobiletools.gallery.extensions

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.Point
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.WindowManager
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.SORT_BY_DATE_MODIFIED
import com.simplemobiletools.commons.helpers.SORT_BY_NAME
import com.simplemobiletools.commons.helpers.SORT_BY_SIZE
import com.simplemobiletools.commons.helpers.SORT_DESCENDING
import com.simplemobiletools.gallery.activities.SettingsActivity
import com.simplemobiletools.gallery.helpers.*
import com.simplemobiletools.gallery.models.Directory
import com.simplemobiletools.gallery.models.Medium
import java.io.File
import java.util.LinkedHashMap
import kotlin.collections.ArrayList
import kotlin.collections.component1
import kotlin.collections.component2

val Context.portrait get() = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
val Context.audioManager get() = getSystemService(Context.AUDIO_SERVICE) as AudioManager
val Context.windowManager: WindowManager get() = getSystemService(Context.WINDOW_SERVICE) as WindowManager
val Context.navigationBarRight: Boolean get() = usableScreenSize.x < realScreenSize.x
val Context.navigationBarBottom: Boolean get() = usableScreenSize.y < realScreenSize.y
val Context.navigationBarHeight: Int get() = if (navigationBarBottom) navigationBarSize.y else 0

internal val Context.navigationBarSize: Point
    get() = when {
        navigationBarRight -> Point(realScreenSize.x - usableScreenSize.x, usableScreenSize.y)
        navigationBarBottom -> Point(usableScreenSize.x, realScreenSize.y - usableScreenSize.y)
        else -> Point()
    }

val Context.usableScreenSize: Point
    get() {
        val size = Point()
        windowManager.defaultDisplay.getSize(size)
        return size
    }

val Context.realScreenSize: Point
    get() {
        val size = Point()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            windowManager.defaultDisplay.getRealSize(size)
        return size
    }

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
    val selection = getSelectionQuery(curPath)
    val selectionArgs = getSelectionArgsQuery(curPath)

    return try {
        val cur = contentResolver.query(uri, projection, selection, selectionArgs, getSortingForFolder(curPath))
        parseCursor(this, cur, isPickImage, isPickVideo, curPath)
    } catch (e: Exception) {
        ArrayList()
    }
}

fun Context.getSelectionQuery(path: String): String {
    val dataQuery = "${MediaStore.Images.Media.DATA} LIKE ?"
    return if (path.isEmpty()) {
        var query = "($dataQuery)"
        if (hasExternalSDCard()) {
            query += " OR ($dataQuery)"
        }
        query
    } else {
        "($dataQuery AND ${MediaStore.Images.Media.DATA} NOT LIKE ?)"
    }
}

fun Context.getSelectionArgsQuery(path: String): Array<String> {
    return if (path.isEmpty()) {
        if (hasExternalSDCard()) arrayOf("$internalStoragePath/%", "$sdCardPath/%") else arrayOf("$internalStoragePath/%")
    } else {
        arrayOf("$path/%", "$path/%/%")
    }
}

private fun parseCursor(context: Context, cur: Cursor, isPickImage: Boolean, isPickVideo: Boolean, curPath: String): ArrayList<Medium> {
    val curMedia = ArrayList<Medium>()
    val config = context.config
    val filterMedia = config.filterMedia
    val showHidden = config.shouldShowHidden
    val includedFolders = config.includedFolders.map { "${it.trimEnd('/')}/" }
    val excludedFolders = config.excludedFolders.map { "${it.trimEnd('/')}/" }
    val noMediaFolders = context.getNoMediaFolders()
    val isThirdPartyIntent = config.isThirdPartyIntent

    cur.use {
        if (cur.moveToFirst()) {
            do {
                try {
                    val path = cur.getStringValue(MediaStore.Images.Media.DATA)

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

                    var size = cur.getLongValue(MediaStore.Images.Media.SIZE)
                    val file = File(path)
                    if (size == 0L) {
                        size = file.length()
                    }

                    if (size <= 0L)
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
                        if (!file.exists())
                            continue

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

        val size = file.length()
        if (size <= 0L)
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

fun Context.movePinnedDirectoriesToFront(dirs: ArrayList<Directory>): ArrayList<Directory> {
    val foundFolders = ArrayList<Directory>()
    val pinnedFolders = config.pinnedFolders

    dirs.forEach { if (pinnedFolders.contains(it.path)) foundFolders.add(it) }
    dirs.removeAll(foundFolders)
    dirs.addAll(0, foundFolders)
    return dirs
}

fun Context.getMediaByDirectories(isPickVideo: Boolean, isPickImage: Boolean): HashMap<String, ArrayList<Medium>> {
    val media = getFilesFrom("", isPickImage, isPickVideo)
    val excludedPaths = config.excludedFolders
    val includedPaths = config.includedFolders
    val showHidden = config.shouldShowHidden
    val directories = groupDirectories(media)

    val removePaths = ArrayList<String>()
    for ((path, curMedia) in directories) {
        // make sure the path has uppercase letters wherever appropriate
        val groupPath = File(curMedia.first().path).parent
        if (!File(groupPath).exists() || !shouldFolderBeVisible(groupPath, excludedPaths, includedPaths, showHidden)) {
            removePaths.add(groupPath.toLowerCase())
        }
    }

    removePaths.forEach {
        directories.remove(it)
    }

    return directories
}

private fun groupDirectories(media: ArrayList<Medium>): HashMap<String, ArrayList<Medium>> {
    val directories = LinkedHashMap<String, ArrayList<Medium>>()
    for (medium in media) {
        val parentDir = File(medium.path).parent?.toLowerCase() ?: continue
        if (directories.containsKey(parentDir)) {
            directories[parentDir]!!.add(medium)
        } else {
            directories.put(parentDir, arrayListOf(medium))
        }
    }
    return directories
}

private fun shouldFolderBeVisible(path: String, excludedPaths: MutableSet<String>, includedPaths: MutableSet<String>, showHidden: Boolean): Boolean {
    val file = File(path)
    return if (includedPaths.contains(path)) {
        true
    } else if (isThisOrParentExcluded(path, excludedPaths, includedPaths)) {
        false
    } else if (!showHidden && file.isDirectory && file.canonicalFile == file.absoluteFile) {
        var containsNoMediaOrDot = file.containsNoMedia() || path.contains("/.")
        if (!containsNoMediaOrDot) {
            containsNoMediaOrDot = checkParentHasNoMedia(file.parentFile)
        }
        !containsNoMediaOrDot
    } else {
        true
    }
}

private fun checkParentHasNoMedia(file: File): Boolean {
    var curFile = file
    while (true) {
        if (curFile.containsNoMedia()) {
            return true
        }
        curFile = curFile.parentFile
        if (curFile.absolutePath == "/")
            break
    }
    return false
}

private fun isThisOrParentExcluded(path: String, excludedPaths: MutableSet<String>, includedPaths: MutableSet<String>) =
        includedPaths.none { path.startsWith(it) } && excludedPaths.any { path.startsWith(it) }

@Suppress("UNCHECKED_CAST")
fun Context.getSortedDirectories(source: ArrayList<Directory>): ArrayList<Directory> {
    Directory.sorting = config.directorySorting
    val dirs = source.clone() as ArrayList<Directory>
    dirs.sort()
    return movePinnedDirectoriesToFront(dirs)
}
