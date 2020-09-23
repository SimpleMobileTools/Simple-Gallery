package com.simplemobiletools.gallery.pro.extensions

import android.os.Environment
import com.simplemobiletools.commons.extensions.containsNoMedia
import com.simplemobiletools.commons.helpers.NOMEDIA
import java.io.File
import java.io.IOException

fun String.isThisOrParentIncluded(includedPaths: MutableSet<String>) = includedPaths.any { equals(it, true) } || includedPaths.any { "$this/".startsWith("$it/", true) }

fun String.isThisOrParentExcluded(excludedPaths: MutableSet<String>) = excludedPaths.any { equals(it, true) } || excludedPaths.any { "$this/".startsWith("$it/", true) }

// cache which folders contain .nomedia files to avoid checking them over and over again
fun String.shouldFolderBeVisible(excludedPaths: MutableSet<String>, includedPaths: MutableSet<String>, showHidden: Boolean,
                                 folderNomediaStatuses: HashMap<String, Boolean>, callback: (path: String, hasNoMedia: Boolean) -> Unit): Boolean {
    if (isEmpty()) {
        return false
    }

    val file = File(this)
    if (file.name.startsWith("img_", true)) {
        val files = file.list()
        if (files != null) {
            if (files.any { it.contains("burst", true) }) {
                return false
            }
        }
    }

    if (!showHidden && file.isHidden) {
        return false
    } else if (includedPaths.contains(this)) {
        return true
    }

    val containsNoMedia = if (showHidden) {
        false
    } else {
        file.containsNoMedia()
    }

    return if (!showHidden && containsNoMedia) {
        false
    } else if (excludedPaths.contains(this)) {
        false
    } else if (isThisOrParentIncluded(includedPaths)) {
        true
    } else if (isThisOrParentExcluded(excludedPaths)) {
        false
    } else if (!showHidden && file.isDirectory && file.canonicalFile == file.absoluteFile) {
        var containsNoMediaOrDot = containsNoMedia || contains("/.")
        if (!containsNoMediaOrDot) {
            var curPath = this
            for (i in 0 until count { it == '/' } - 1) {
                curPath = curPath.substringBeforeLast('/')
                val pathToCheck = "$curPath/${NOMEDIA}"
                if (folderNomediaStatuses.contains(pathToCheck)) {
                    if (folderNomediaStatuses[pathToCheck] == true) {
                        containsNoMediaOrDot = true
                        break
                    }
                } else {
                    val noMediaExists = File(pathToCheck).exists()
                    callback(pathToCheck, noMediaExists)
                    if (noMediaExists) {
                        containsNoMediaOrDot = true
                        break
                    }
                }
            }
        }
        !containsNoMediaOrDot
    } else {
        true
    }
}

// recognize /sdcard/DCIM as the same folder as /storage/emulated/0/DCIM
fun String.getDistinctPath(): String {
    return try {
        File(this).canonicalPath.toLowerCase()
    } catch (e: IOException) {
        toLowerCase()
    }
}

fun String.isDownloadsFolder() = equals(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString(), true)
