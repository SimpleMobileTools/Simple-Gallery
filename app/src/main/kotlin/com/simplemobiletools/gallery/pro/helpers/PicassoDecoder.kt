package com.simplemobiletools.gallery.pro.helpers

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.davemorrissey.labs.subscaleview.ImageDecoder
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.Picasso

class PicassoDecoder(val tag: String, val picasso: Picasso, val degrees: Int) : ImageDecoder {

    override fun decode(context: Context, uri: Uri): Bitmap {
        return picasso
                .load(uri)
                .tag(tag)
                .config(Bitmap.Config.ARGB_8888)
                .memoryPolicy(MemoryPolicy.NO_CACHE)
                .rotate(-degrees.toFloat())
                .get()
    }
}
