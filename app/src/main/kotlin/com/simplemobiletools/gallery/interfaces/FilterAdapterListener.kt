package com.simplemobiletools.gallery.interfaces

import com.simplemobiletools.gallery.models.FilterItem

interface FilterAdapterListener {
    fun getCurrentFilter(): FilterItem

    fun setCurrentFilter(position: Int)
}
