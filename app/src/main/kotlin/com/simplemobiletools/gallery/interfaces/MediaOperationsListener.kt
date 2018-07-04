package com.simplemobiletools.gallery.interfaces

import com.simplemobiletools.commons.models.FileDirItem

interface MediaOperationsListener {
    fun refreshItems()

    fun tryDeleteFiles(fileDirItems: ArrayList<FileDirItem>)

    fun selectedPaths(paths: ArrayList<String>)
}
