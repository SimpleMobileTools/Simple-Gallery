package com.simplemobiletools.gallery.interfaces

interface SecurityTab {
    fun initTab(requiredHash: String, listener: HashListener)
}
