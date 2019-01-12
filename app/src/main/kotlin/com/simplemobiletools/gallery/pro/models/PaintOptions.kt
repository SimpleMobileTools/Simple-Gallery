package com.simplemobiletools.gallery.pro.models

import android.graphics.Color

data class PaintOptions(var color: Int = Color.BLACK, var strokeWidth: Float = 5f, var isEraser: Boolean = false) {
    fun getColorToExport() = if (isEraser) "none" else "#${Integer.toHexString(color).substring(2)}"
}
