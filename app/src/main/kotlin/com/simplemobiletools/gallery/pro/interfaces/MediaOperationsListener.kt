package com.simplemobiletools.gallery.pro.interfaces

import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.gallery.pro.models.ThumbnailItem

interface MediaOperationsListener {
    fun refreshItems()

    fun tryDeleteFiles(fileDirItems: ArrayList<FileDirItem>)

    fun selectedPaths(paths: ArrayList<String>)

    fun updateMediaGridDecoration(media: ArrayList<ThumbnailItem>)
}
