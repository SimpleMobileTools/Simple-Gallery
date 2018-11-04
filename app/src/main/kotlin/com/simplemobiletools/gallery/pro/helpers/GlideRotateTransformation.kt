package com.simplemobiletools.gallery.pro.helpers

import android.graphics.Bitmap
import android.graphics.Matrix
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest

class GlideRotateTransformation(val rotateRotationAngle: Int) : BitmapTransformation() {
    override fun transform(pool: BitmapPool, bitmap: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        if (rotateRotationAngle % 360 == 0)
            return bitmap

        val matrix = Matrix()
        matrix.postRotate(rotateRotationAngle.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
    }
}
