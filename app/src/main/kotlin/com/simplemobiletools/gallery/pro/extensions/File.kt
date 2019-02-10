package com.simplemobiletools.gallery.pro.extensions

import com.simplemobiletools.gallery.pro.helpers.NOMEDIA
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
