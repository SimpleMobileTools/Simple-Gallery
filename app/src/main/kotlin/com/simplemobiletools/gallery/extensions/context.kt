package com.simplemobiletools.gallery.extensions

import android.content.Context
import android.media.MediaScannerConnection

fun Context.scanFile(paths: Array<String>) = MediaScannerConnection.scanFile(this, paths, null, null)
