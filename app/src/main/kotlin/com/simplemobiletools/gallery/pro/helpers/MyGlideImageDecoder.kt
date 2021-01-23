package com.simplemobiletools.gallery.pro.helpers

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.ObjectKey
import com.davemorrissey.labs.subscaleview.ImageDecoder

class MyGlideImageDecoder(val degrees: Int, val signature: ObjectKey) : ImageDecoder {

    override fun decode(context: Context, uri: Uri): Bitmap {
        val options = RequestOptions()
            .format(DecodeFormat.PREFER_ARGB_8888)
            .signature(signature)
            .fitCenter()

        val builder = Glide.with(context)
            .asBitmap()
            .load(uri.toString().substringAfter("file://"))
            .apply(options)
            .transform(RotateTransformation(-degrees))
            .into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)

        return builder.get()
    }
}
