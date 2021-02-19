package com.simplemobiletools.gallery.pro.helpers

import android.graphics.*
import com.squareup.picasso.Transformation

// taken from https://stackoverflow.com/a/35241525/1967672
class PicassoRoundedCornersTransformation(private val radius: Float) : Transformation {

    override fun transform(source: Bitmap): Bitmap {
        val size = Math.min(source.width, source.height)
        val x = (source.width - size) / 2
        val y = (source.height - size) / 2
        val squaredBitmap = Bitmap.createBitmap(source, x, y, size, size)
        if (squaredBitmap != source) {
            source.recycle()
        }

        val bitmap = Bitmap.createBitmap(size, size, source.config)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        val shader = BitmapShader(squaredBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        paint.shader = shader
        paint.isAntiAlias = true

        val rect = RectF(0f, 0f, source.width.toFloat(), source.height.toFloat())
        canvas.drawRoundRect(rect, radius, radius, paint)
        squaredBitmap.recycle()
        return bitmap
    }

    override fun key() = "rounded_corners"
}
