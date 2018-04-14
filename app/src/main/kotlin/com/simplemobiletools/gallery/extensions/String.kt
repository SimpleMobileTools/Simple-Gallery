package com.simplemobiletools.gallery.extensions

import com.bumptech.glide.signature.ObjectKey
import java.io.File

fun String.getFileSignature(): ObjectKey {
    val file = File(this)
    return ObjectKey("${file.absolutePath}${file.lastModified()}")
}

fun String.isThisOrParentIncluded(includedPaths: MutableSet<String>) = includedPaths.any { startsWith(it, true) }

fun String.isThisOrParentExcluded(excludedPaths: MutableSet<String>) = excludedPaths.any { startsWith(it, true) }
