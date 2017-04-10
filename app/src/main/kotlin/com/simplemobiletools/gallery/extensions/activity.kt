package com.simplemobiletools.gallery.extensions

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.ViewConfiguration
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.StringSignature
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.gallery.BuildConfig
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.SimpleActivity
import com.simplemobiletools.gallery.helpers.NOMEDIA
import com.simplemobiletools.gallery.helpers.REQUEST_EDIT_IMAGE
import com.simplemobiletools.gallery.helpers.REQUEST_SET_WALLPAPER
import com.simplemobiletools.gallery.models.Medium
import com.simplemobiletools.gallery.views.MySquareImageView
import java.io.File
import java.util.*

fun Activity.shareUri(medium: Medium, uri: Uri) {
    val shareTitle = resources.getString(R.string.share_via)
    Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, uri)
        type = medium.getMimeType()
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(this, shareTitle))
    }
}

fun Activity.shareMedium(medium: Medium) {
    val shareTitle = resources.getString(R.string.share_via)
    val file = File(medium.path)
    val uri = Uri.fromFile(file)
    Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, uri)
        type = medium.getMimeType()
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(this, shareTitle))
    }
}

fun Activity.shareMedia(media: List<Medium>) {
    val shareTitle = resources.getString(R.string.share_via)
    val uris = ArrayList<Uri>(media.size)
    Intent().apply {
        action = Intent.ACTION_SEND_MULTIPLE
        type = "image/* video/*"
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        media.map { File(it.path) }
                .mapTo(uris) { Uri.fromFile(it) }

        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        startActivity(Intent.createChooser(this, shareTitle))
    }
}

fun Activity.setAsWallpaper(file: File) {
    val uri = Uri.fromFile(file)
    Intent().apply {
        action = Intent.ACTION_ATTACH_DATA
        setDataAndType(uri, file.getMimeType("image/*"))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val chooser = Intent.createChooser(this, getString(R.string.set_as_wallpaper_with))

        if (resolveActivity(packageManager) != null) {
            startActivityForResult(chooser, REQUEST_SET_WALLPAPER)
        } else {
            toast(R.string.no_wallpaper_setter_found)
        }
    }
}

fun Activity.openWith(file: File, forceChooser: Boolean = true) {
    val uri = Uri.fromFile(file)
    Intent().apply {
        action = Intent.ACTION_VIEW
        setDataAndType(uri, file.getMimeType())
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val chooser = Intent.createChooser(this, getString(R.string.open_with))

        if (resolveActivity(packageManager) != null) {
            startActivity(if (forceChooser) chooser else this)
        } else {
            toast(R.string.no_app_found)
        }
    }
}

fun Activity.openEditor(file: File) {
    val uri = Uri.fromFile(file)
    Intent().apply {
        action = Intent.ACTION_EDIT
        setDataAndType(uri, "image/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        if (resolveActivity(packageManager) != null) {
            startActivityForResult(this, REQUEST_EDIT_IMAGE)
        } else {
            toast(R.string.no_editor_found)
        }
    }
}

fun Activity.hasNavBar(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        val display = windowManager.defaultDisplay

        val realDisplayMetrics = DisplayMetrics()
        display.getRealMetrics(realDisplayMetrics)

        val realHeight = realDisplayMetrics.heightPixels
        val realWidth = realDisplayMetrics.widthPixels

        val displayMetrics = DisplayMetrics()
        display.getMetrics(displayMetrics)

        val displayHeight = displayMetrics.heightPixels
        val displayWidth = displayMetrics.widthPixels

        realWidth - displayWidth > 0 || realHeight - displayHeight > 0
    } else {
        val hasMenuKey = ViewConfiguration.get(this).hasPermanentMenuKey()
        val hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK)
        !hasMenuKey && !hasBackKey
    }
}

fun SimpleActivity.launchAbout() {
    startAboutActivity(R.string.app_name, LICENSE_KOTLIN or LICENSE_GLIDE or LICENSE_CROPPER or LICENSE_MULTISELECT or LICENSE_RTL
            or LICENSE_PHOTOVIEW or LICENSE_SUBSAMPLING, BuildConfig.VERSION_NAME)
}

fun AppCompatActivity.showSystemUI() {
    supportActionBar?.show()
    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
}

fun AppCompatActivity.hideSystemUI() {
    supportActionBar?.hide()
    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LOW_PROFILE or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE
}

fun SimpleActivity.addNoMedia(path: String, callback: () -> Unit) {
    val file = File(path, NOMEDIA)
    if (file.exists())
        return

    if (needsStupidWritePermissions(path)) {
        handleSAFDialog(file) {
            getFileDocument(path)?.createFile("", NOMEDIA)
        }
    } else {
        try {
            file.createNewFile()
        } catch (e: Exception) {
            toast(R.string.unknown_error_occurred)
        }
    }
    scanFile(file) {
        callback.invoke()
    }
}

fun SimpleActivity.removeNoMedia(path: String, callback: () -> Unit) {
    val file = File(path, NOMEDIA)
    deleteFile(file) {
        scanFile(File(path)) {
            callback()
        }
    }
}

fun Activity.getFileSignature(path: String) = StringSignature(File(path).lastModified().toString())
fun Activity.loadImage(path: String, target: MySquareImageView) {
    if (path.isImageFast() || path.isVideoFast()) {
        if (path.isPng()) {
            loadPng(path, target)
        } else {
            loadJpg(path, target)
        }
    } else if (path.isGif()) {
        if (config.animateGifs) {
            loadAnimatedGif(path, target)
        } else {
            loadStaticGif(path, target)
        }
    }
}

fun Activity.loadPng(path: String, target: MySquareImageView) {
    val builder = Glide.with(this)
            .load(path)
            .asBitmap()
            .signature(getFileSignature(path))
            .diskCacheStrategy(DiskCacheStrategy.RESULT)
            .format(DecodeFormat.PREFER_ARGB_8888)

    if (config.cropThumbnails) builder.centerCrop() else builder.fitCenter()
    builder.into(target)
}

fun Activity.loadJpg(path: String, target: MySquareImageView) {
    val builder = Glide.with(this)
            .load(path)
            .signature(getFileSignature(path))
            .diskCacheStrategy(DiskCacheStrategy.RESULT)
            .crossFade()

    if (config.cropThumbnails) builder.centerCrop() else builder.fitCenter()
    builder.into(target)
}

fun Activity.loadAnimatedGif(path: String, target: MySquareImageView) {
    val builder = Glide.with(this)
            .load(path)
            .asGif()
            .signature(getFileSignature(path))
            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
            .crossFade()

    if (config.cropThumbnails) builder.centerCrop() else builder.fitCenter()
    builder.into(target)
}

fun Activity.loadStaticGif(path: String, target: MySquareImageView) {
    val builder = Glide.with(this)
            .load(path)
            .asBitmap()
            .signature(getFileSignature(path))
            .diskCacheStrategy(DiskCacheStrategy.SOURCE)

    if (config.cropThumbnails) builder.centerCrop() else builder.fitCenter()
    builder.into(target)
}
