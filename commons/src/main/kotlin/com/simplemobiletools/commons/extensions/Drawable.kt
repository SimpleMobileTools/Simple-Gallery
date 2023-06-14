package com.simplemobiletools.commons.extensions

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

fun Drawable.applyColorFilter(color: Int) = mutate().setColorFilter(color, PorterDuff.Mode.SRC_IN)

fun Drawable.convertToBitmap(): Bitmap {
    val bitmap = if (intrinsicWidth <= 0 || intrinsicHeight <= 0) {
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    } else {
        Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
    }

    if (this is BitmapDrawable) {
        if (this.bitmap != null) {
            return this.bitmap
        }
    }

    val canvas = Canvas(bitmap!!)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap
}
