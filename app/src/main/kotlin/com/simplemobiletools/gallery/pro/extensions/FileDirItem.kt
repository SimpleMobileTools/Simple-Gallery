package com.simplemobiletools.gallery.pro.extensions

import android.os.Environment
import com.simplemobiletools.commons.models.FileDirItem

fun FileDirItem.isDownloadsFolder() = path.equals(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString(), true)
