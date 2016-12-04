package com.simplemobiletools.gallery.extensions

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.simplemobiletools.filepicker.extensions.getMimeType
import com.simplemobiletools.filepicker.extensions.toast
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.REQUEST_SET_WALLPAPER
import com.simplemobiletools.gallery.models.Medium
import java.io.File
import java.util.*

fun Activity.shareMedium(medium: Medium) {
    val shareTitle = resources.getString(R.string.share_via)
    val file = File(medium.path)
    val uri = Uri.fromFile(file)
    Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, uri)
        type = medium.getMimeType()
        startActivity(Intent.createChooser(this, shareTitle))
    }
}

fun Activity.shareMedia(media: List<Medium>) {
    val shareTitle = resources.getString(R.string.share_via)
    Intent().apply {
        action = Intent.ACTION_SEND_MULTIPLE
        type = "image/* video/*"
        val uris = ArrayList<Uri>(media.size)
        media.map { File(it.path) }
                .mapTo(uris) { Uri.fromFile(it) }

        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        startActivity(Intent.createChooser(this, shareTitle))
    }
}

fun Activity.setAsWallpaper(file: File) {
    val intent = Intent(Intent.ACTION_ATTACH_DATA)
    val uri = Uri.fromFile(file)
    var mimeType = getMimeType(uri.toString())
    if (mimeType.isEmpty()) mimeType = "image/jpeg"
    intent.setDataAndType(uri, mimeType)
    val chooser = Intent.createChooser(intent, getString(R.string.set_as_wallpaper_with))

    if (intent.resolveActivity(packageManager) != null) {
        startActivityForResult(chooser, REQUEST_SET_WALLPAPER)
    } else {
        toast(R.string.no_wallpaper_setter_found)
    }
}
