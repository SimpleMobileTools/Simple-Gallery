package com.simplemobiletools.gallery.pro.extensions

import android.media.MediaMetadataRetriever
import android.os.Environment
import com.simplemobiletools.commons.extensions.doesThisOrParentHaveNoMedia
import com.simplemobiletools.commons.helpers.NOMEDIA
import java.io.File
import java.io.IOException

fun String.isThisOrParentIncluded(includedPaths: MutableSet<String>) = includedPaths.any { equals(it, true) } || includedPaths.any { "$this/".startsWith("$it/", true) }

fun String.isThisOrParentExcluded(excludedPaths: MutableSet<String>) = excludedPaths.any { equals(it, true) } || excludedPaths.any { "$this/".startsWith("$it/", true) }

fun String.shouldFolderBeVisible(excludedPaths: MutableSet<String>, includedPaths: MutableSet<String>, showHidden: Boolean): Boolean {
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
        File(this, NOMEDIA).exists()
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
            containsNoMediaOrDot = file.doesThisOrParentHaveNoMedia()
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

fun String.getVideoDuration(): Int {
    var seconds = 0
    try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(this)
        seconds = Math.round(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toInt() / 1000f)
    } catch (e: Exception) {
    }
    return seconds
}

fun String.isDownloadsFolder() = equals(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString(), true)
