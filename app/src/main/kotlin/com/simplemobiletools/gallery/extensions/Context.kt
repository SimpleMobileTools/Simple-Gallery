package com.simplemobiletools.gallery.extensions

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.Point
import android.media.AudioManager
import android.os.Build
import android.provider.MediaStore
import android.view.WindowManager
import com.google.gson.Gson
import com.simplemobiletools.commons.extensions.getStringValue
import com.simplemobiletools.commons.extensions.humanizePath
import com.simplemobiletools.commons.helpers.OTG_PATH
import com.simplemobiletools.gallery.activities.SettingsActivity
import com.simplemobiletools.gallery.asynctasks.GetDirectoriesAsynctask
import com.simplemobiletools.gallery.asynctasks.GetMediaAsynctask
import com.simplemobiletools.gallery.helpers.Config
import com.simplemobiletools.gallery.helpers.NOMEDIA
import com.simplemobiletools.gallery.helpers.SAVE_DIRS_CNT
import com.simplemobiletools.gallery.helpers.SAVE_MEDIA_CNT
import com.simplemobiletools.gallery.models.Directory
import com.simplemobiletools.gallery.models.Medium
import java.io.File

val Context.portrait get() = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
val Context.audioManager get() = getSystemService(Context.AUDIO_SERVICE) as AudioManager
val Context.windowManager: WindowManager get() = getSystemService(Context.WINDOW_SERVICE) as WindowManager
val Context.navigationBarRight: Boolean get() = usableScreenSize.x < realScreenSize.x
val Context.navigationBarBottom: Boolean get() = usableScreenSize.y < realScreenSize.y
val Context.navigationBarHeight: Int get() = if (navigationBarBottom) navigationBarSize.y else 0
val Context.navigationBarWidth: Int get() = if (navigationBarRight) navigationBarSize.x else 0

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

fun Context.getHumanizedFilename(path: String): String {
    val humanized = humanizePath(path)
    return humanized.substring(humanized.lastIndexOf("/") + 1)
}

fun Context.launchSettings() {
    startActivity(Intent(applicationContext, SettingsActivity::class.java))
}

val Context.config: Config get() = Config.newInstance(applicationContext)

fun Context.movePinnedDirectoriesToFront(dirs: ArrayList<Directory>): ArrayList<Directory> {
    val foundFolders = ArrayList<Directory>()
    val pinnedFolders = config.pinnedFolders

    dirs.forEach {
        if (pinnedFolders.contains(it.path))
            foundFolders.add(it)
    }

    dirs.removeAll(foundFolders)
    dirs.addAll(0, foundFolders)
    return dirs
}

@Suppress("UNCHECKED_CAST")
fun Context.getSortedDirectories(source: ArrayList<Directory>): ArrayList<Directory> {
    Directory.sorting = config.directorySorting
    val dirs = source.clone() as ArrayList<Directory>
    dirs.sort()
    return movePinnedDirectoriesToFront(dirs)
}

fun Context.getNoMediaFolders(callback: (folders: ArrayList<String>) -> Unit) {
    Thread {
        val folders = ArrayList<String>()

        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.Files.FileColumns.DATA)
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? AND ${MediaStore.Files.FileColumns.TITLE} LIKE ?"
        val selectionArgs = arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_NONE.toString(), "%$NOMEDIA%")
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        var cursor: Cursor? = null

        try {
            cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
            if (cursor?.moveToFirst() == true) {
                do {
                    val path = cursor.getStringValue(MediaStore.Files.FileColumns.DATA) ?: continue
                    val noMediaFile = File(path)
                    if (noMediaFile.exists() && noMediaFile.name == NOMEDIA) {
                        folders.add("${noMediaFile.parent}/")
                    }
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
        }

        callback(folders)
    }.start()
}

fun Context.isPathInMediaStore(path: String): Boolean {
    if (path.startsWith(OTG_PATH)) {
        return false
    }

    val projection = arrayOf(MediaStore.Images.Media.DATE_MODIFIED)
    val uri = MediaStore.Files.getContentUri("external")
    val selection = "${MediaStore.MediaColumns.DATA} = ?"
    val selectionArgs = arrayOf(path)
    val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)

    cursor?.use {
        return cursor.moveToFirst()
    }
    return false
}

fun Context.updateStoredFolderItems(path: String) {
    GetMediaAsynctask(this, path, false, false, false) {
        storeFolderItems(path, it)
    }.execute()
}

fun Context.storeFolderItems(path: String, items: ArrayList<Medium>) {
    try {
        val subList = items.subList(0, Math.min(SAVE_MEDIA_CNT, items.size))
        val json = Gson().toJson(subList)
        config.saveFolderMedia(path, json)
    } catch (ignored: Exception) {
    } catch (ignored: OutOfMemoryError) {
    }
}

fun Context.updateStoredDirectories() {
    GetDirectoriesAsynctask(this, false, false) {
        if (!config.temporarilyShowHidden) {
            storeDirectoryItems(it)
        }
    }.execute()
}

fun Context.storeDirectoryItems(items: ArrayList<Directory>) {
    val subList = items.subList(0, Math.min(SAVE_DIRS_CNT, items.size))
    val directories = Gson().toJson(subList)
    config.directories = directories
}
