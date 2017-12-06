package com.simplemobiletools.gallery.extensions

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Point
import android.media.AudioManager
import android.os.Build
import android.view.WindowManager
import com.simplemobiletools.commons.extensions.humanizePath
import com.simplemobiletools.gallery.activities.SettingsActivity
import com.simplemobiletools.gallery.helpers.Config
import com.simplemobiletools.gallery.models.Directory

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
