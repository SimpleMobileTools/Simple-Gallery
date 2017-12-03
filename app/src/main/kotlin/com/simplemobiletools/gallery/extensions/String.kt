package com.simplemobiletools.gallery.extensions

import com.bumptech.glide.signature.ObjectKey
import java.io.File

fun String.getFileSignature() = ObjectKey(File(this).lastModified().toString())
