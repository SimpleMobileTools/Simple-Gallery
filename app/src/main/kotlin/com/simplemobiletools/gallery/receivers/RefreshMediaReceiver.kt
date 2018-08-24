package com.simplemobiletools.gallery.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.REFRESH_PATH
import com.simplemobiletools.gallery.extensions.galleryDB
import com.simplemobiletools.gallery.helpers.*
import com.simplemobiletools.gallery.models.Medium
import java.io.File

class RefreshMediaReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val path = intent.getStringExtra(REFRESH_PATH) ?: return

        Thread {
            val medium = Medium(null, path.getFilenameFromPath(), path, path.getParentPath(), System.currentTimeMillis(), System.currentTimeMillis(),
                    File(path).length(), getFileType(path), false, 0L)
            context.galleryDB.MediumDao().insert(medium)
        }.start()
    }

    private fun getFileType(path: String) = when {
        path.isImageFast() -> TYPE_IMAGES
        path.isVideoFast() -> TYPE_VIDEOS
        path.isGif() -> TYPE_GIFS
        path.isRawFast() -> TYPE_RAWS
        else -> TYPE_SVGS
    }
}
