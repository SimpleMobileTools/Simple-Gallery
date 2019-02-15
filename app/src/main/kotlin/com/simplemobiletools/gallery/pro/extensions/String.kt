package com.simplemobiletools.gallery.pro.extensions

import android.media.MediaMetadataRetriever
import com.bumptech.glide.signature.ObjectKey
import java.io.File
import java.io.IOException

fun String.getFileSignature() = ObjectKey(getFileKey())

fun String.getFileKey(): String {
    val file = File(this)
    return "${file.absolutePath}${file.lastModified()}"
}

fun String.isThisOrParentIncluded(includedPaths: MutableSet<String>) = includedPaths.any { startsWith(it, true) }

fun String.isThisOrParentExcluded(excludedPaths: MutableSet<String>) = excludedPaths.any { startsWith(it, true) }

fun String.shouldFolderBeVisible(excludedPaths: MutableSet<String>, includedPaths: MutableSet<String>, showHidden: Boolean): Boolean {
    val file = File(this)
    return if (isEmpty()) {
        false
    } else if (!showHidden && file.isHidden) {
        false
    } else if (includedPaths.contains(this)) {
        true
    } else if (!showHidden && file.containsNoMedia()) {
        false
    } else if (excludedPaths.contains(this)) {
        false
    } else if (isThisOrParentIncluded(includedPaths)) {
        true
    } else if (isThisOrParentExcluded(excludedPaths)) {
        false
    } else if (!showHidden && file.isDirectory && file.canonicalFile == file.absoluteFile) {
        var containsNoMediaOrDot = file.containsNoMedia() || contains("/.")
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
