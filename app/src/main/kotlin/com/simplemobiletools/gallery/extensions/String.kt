package com.simplemobiletools.gallery.extensions

import com.bumptech.glide.signature.ObjectKey
import java.io.File

fun String.getFileSignature(): ObjectKey {
    val file = File(this)
    return ObjectKey("${file.absolutePath}${file.lastModified()}")
}

fun String.isThisOrParentIncluded(includedPaths: MutableSet<String>) = includedPaths.any { startsWith(it, true) }

fun String.isThisOrParentExcluded(excludedPaths: MutableSet<String>) = excludedPaths.any { startsWith(it, true) }

fun String.shouldFolderBeVisible(excludedPaths: MutableSet<String>, includedPaths: MutableSet<String>, showHidden: Boolean): Boolean {
    val file = File(this)
    return if (isEmpty()) {
        false
    } else if (!showHidden && file.containsNoMedia()) {
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
