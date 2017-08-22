package com.simplemobiletools.gallery.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest

class GlideRotateTransformation(context: Context, val rotateRotationAngle: Float) : BitmapTransformation(context) {
    override fun transform(pool: BitmapPool, bitmap: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        if (rotateRotationAngle % 360 == 0f)
            return bitmap

        val matrix = Matrix()
        matrix.postRotate(rotateRotationAngle)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest?) {
    }
}
