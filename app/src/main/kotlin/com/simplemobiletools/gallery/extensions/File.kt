package com.simplemobiletools.gallery.extensions

import com.simplemobiletools.gallery.helpers.NOMEDIA
import java.io.File

fun File.containsNoMedia() = isDirectory && File(this, NOMEDIA).exists()
