package com.simplemobiletools.gallery.pro.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View

class EditorDrawCanvas(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private var backgroundBitmap: Bitmap? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()

        if (backgroundBitmap != null) {
            val left = (width - backgroundBitmap!!.width) / 2
            val top = (height - backgroundBitmap!!.height) / 2
            canvas.drawBitmap(backgroundBitmap!!, left.toFloat(), top.toFloat(), null)
        }

        canvas.restore()
    }

    fun updateBackgroundBitmap(bitmap: Bitmap) {
        backgroundBitmap = bitmap
        invalidate()
    }
}
