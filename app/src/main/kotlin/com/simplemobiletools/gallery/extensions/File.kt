package com.simplemobiletools.gallery.extensions

import com.simplemobiletools.gallery.helpers.NOMEDIA
import java.io.File

fun File.containsNoMedia() = isDirectory && File(this, NOMEDIA).exists()

fun File.doesThisOrParentHaveNoMedia(): Boolean {
    var curFile = this
    while (true) {
        if (curFile.containsNoMedia()) {
            return true
        }
        curFile = curFile.parentFile ?: break
        if (curFile.absolutePath == "/") {
            break
        }
    }
    return false
}
