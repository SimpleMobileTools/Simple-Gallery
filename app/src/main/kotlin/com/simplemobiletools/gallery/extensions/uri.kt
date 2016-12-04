package com.simplemobiletools.gallery.extensions

import android.net.Uri
import com.simplemobiletools.filepicker.extensions.getMimeType

fun Uri.getImageMimeType(): String {
    val mimeType = getMimeType(toString())
    return if (mimeType.isNotEmpty())
        mimeType
    else
        "image/jpeg"
}
