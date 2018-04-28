package com.simplemobiletools.gallery.extensions

import com.simplemobiletools.gallery.helpers.TYPE_GIFS
import com.simplemobiletools.gallery.helpers.TYPE_IMAGES
import com.simplemobiletools.gallery.helpers.TYPE_VIDEOS
import com.simplemobiletools.gallery.models.Medium

fun ArrayList<Medium>.getDirMediaTypes(): Int {
    var types = 0
    if (any { it.isImage() }) {
        types += TYPE_IMAGES
    }

    if (any { it.isVideo() }) {
        types += TYPE_VIDEOS
    }

    if (any { it.isGif() }) {
        types += TYPE_GIFS
    }

    return types
}
