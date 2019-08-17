package com.simplemobiletools.gallery.pro.helpers

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.davemorrissey.labs.subscaleview.ImageDecoder

class MyGlideImageDecoder(val degrees: Int, val width: Int, val height: Int) : ImageDecoder {

    override fun decode(context: Context, uri: Uri): Bitmap {
        val options = RequestOptions()
                .format(DecodeFormat.PREFER_ARGB_8888)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .fitCenter()

        val builder = Glide.with(context)
                .asBitmap()
                .load(uri)
                .apply(options)
                .transform(RotateTransformation(-degrees))
                .into(width, height)

        return builder.get()
    }
}
