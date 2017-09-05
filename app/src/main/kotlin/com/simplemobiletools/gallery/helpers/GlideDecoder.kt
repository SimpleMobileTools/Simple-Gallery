package com.simplemobiletools.gallery.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.ExifInterface
import android.net.Uri
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.davemorrissey.labs.subscaleview.decoder.ImageDecoder

class GlideDecoder : ImageDecoder {
    override fun decode(context: Context, uri: Uri): Bitmap {
        val exif = android.media.ExifInterface(uri.path)
        val orientation = exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL)

        val options = RequestOptions()
                .format(DecodeFormat.PREFER_ARGB_8888)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .transform(GlideRotateTransformation(context, getRotationDegrees(orientation)))

        val drawable = Glide.with(context)
                .load(uri)
                .apply(options)
                .submit()
                .get()

        return drawableToBitmap(drawable)
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            if (drawable.bitmap != null) {
                return drawable.bitmap
            }
        }

        val bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        } else {
            Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        }

        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    // rotating backwards intentionally, as SubsamplingScaleImageView will rotate it properly at displaying
    private fun getRotationDegrees(orientation: Int) = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_270 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_90 -> 270f
        else -> 0f
    }
}
