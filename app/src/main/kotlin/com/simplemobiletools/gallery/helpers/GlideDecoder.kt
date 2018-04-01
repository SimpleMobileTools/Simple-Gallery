package com.simplemobiletools.gallery.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.ExifInterface
import android.net.Uri
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.davemorrissey.labs.subscaleview.decoder.ImageDecoder
import com.simplemobiletools.gallery.activities.ViewPagerActivity
import com.simplemobiletools.gallery.extensions.getFileSignature

class GlideDecoder : ImageDecoder {
    override fun decode(context: Context, uri: Uri): Bitmap {
        val exif = android.media.ExifInterface(uri.path)
        val orientation = exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL)

        val targetWidth = if (ViewPagerActivity.screenWidth == 0) Target.SIZE_ORIGINAL else ViewPagerActivity.screenWidth
        val targetHeight = if (ViewPagerActivity.screenHeight == 0) Target.SIZE_ORIGINAL else ViewPagerActivity.screenHeight

        ViewPagerActivity.wasDecodedByGlide = true
        val options = RequestOptions()
                .signature(uri.path.getFileSignature())
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .override(targetWidth, targetHeight)

        val degrees = getRotationDegrees(orientation)
        if (degrees != 0) {
            options.transform(GlideRotateTransformation(context, getRotationDegrees(orientation)))
        }

        val drawable = Glide.with(context)
                .load(uri)
                .apply(options)
                .submit()
                .get()

        return drawableToBitmap(drawable)
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
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
        ExifInterface.ORIENTATION_ROTATE_270 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_90 -> 270
        else -> 0
    }
}
