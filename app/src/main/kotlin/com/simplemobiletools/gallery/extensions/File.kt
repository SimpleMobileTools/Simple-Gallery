package com.simplemobiletools.gallery.extensions

import com.simplemobiletools.gallery.helpers.NOMEDIA
import java.io.File

fun File.containsNoMedia() = isDirectory && File(this, NOMEDIA).exists()

fun File.doesParentHaveNoMedia(): Boolean {
    var curFile = this
    while (true) {
        if (curFile.containsNoMedia()) {
            return true
        }
        curFile = curFile.parentFile
        if (curFile.absolutePath == "/") {
            break
        }
    }
    return false
}
