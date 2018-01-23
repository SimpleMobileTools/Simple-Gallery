package com.simplemobiletools.gallery.extensions

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.gallery.BuildConfig
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.SimpleActivity
import com.simplemobiletools.gallery.dialogs.PickDirectoryDialog
import com.simplemobiletools.gallery.helpers.NOMEDIA
import com.simplemobiletools.gallery.helpers.TYPE_GIF
import com.simplemobiletools.gallery.helpers.TYPE_IMAGE
import com.simplemobiletools.gallery.helpers.TYPE_VIDEO
import com.simplemobiletools.gallery.models.Directory
import com.simplemobiletools.gallery.models.Medium
import com.simplemobiletools.gallery.views.MySquareImageView
import pl.droidsonroids.gif.GifDrawable
import java.io.File
import java.util.*

fun Activity.shareUri(uri: Uri) {
    shareUri(uri, BuildConfig.APPLICATION_ID)
}

fun Activity.shareUris(uris: ArrayList<Uri>) {
    shareUris(uris, BuildConfig.APPLICATION_ID)
}

fun Activity.shareMedium(medium: Medium) {
    val file = File(medium.path)
    shareUri(Uri.fromFile(file))
}

fun Activity.shareMedia(media: List<Medium>) {
    val uris = media.map { Uri.fromFile(File(it.path)) } as ArrayList
    shareUris(uris)
}

fun Activity.setAs(uri: Uri) {
    setAs(uri, BuildConfig.APPLICATION_ID)
}

fun Activity.openFile(uri: Uri, forceChooser: Boolean) {
    openFile(uri, forceChooser, BuildConfig.APPLICATION_ID)
}

fun Activity.openEditor(uri: Uri) {
    openEditor(uri, BuildConfig.APPLICATION_ID)
}

fun Activity.launchCamera() {
    val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
    if (intent.resolveActivity(packageManager) != null) {
        startActivity(intent)
    } else {
        toast(R.string.no_app_found)
    }
}

fun SimpleActivity.launchAbout() {
    startAboutActivity(R.string.app_name, LICENSE_KOTLIN or LICENSE_GLIDE or LICENSE_CROPPER or LICENSE_MULTISELECT or LICENSE_RTL
            or LICENSE_SUBSAMPLING or LICENSE_PATTERN or LICENSE_REPRINT or LICENSE_GIF_DRAWABLE or LICENSE_PHOTOVIEW, BuildConfig.VERSION_NAME)
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

fun BaseSimpleActivity.addNoMedia(path: String, callback: () -> Unit) {
    val file = File(path, NOMEDIA)
    if (file.exists())
        return

    if (needsStupidWritePermissions(path)) {
        handleSAFDialog(file) {
            val fileDocument = getFileDocument(path)
            if (fileDocument?.exists() == true && fileDocument.isDirectory) {
                fileDocument.createFile("", NOMEDIA)
            } else {
                toast(R.string.unknown_error_occurred)
            }
        }
    } else {
        try {
            file.createNewFile()
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }

    applicationContext.scanFile(file) {
        callback()
    }
}

fun BaseSimpleActivity.removeNoMedia(path: String, callback: (() -> Unit)? = null) {
    val file = File(path, NOMEDIA)
    deleteFile(file) {
        callback?.invoke()
    }
}

fun BaseSimpleActivity.toggleFileVisibility(oldFile: File, hide: Boolean, callback: ((newFile: File) -> Unit)? = null) {
    val path = oldFile.parent
    var filename = oldFile.name
    filename = if (hide) {
        ".${filename.trimStart('.')}"
    } else {
        filename.substring(1, filename.length)
    }
    val newFile = File(path, filename)
    renameFile(oldFile, newFile) {
        callback?.invoke(newFile)
    }
}

fun Activity.loadImage(type: Int, path: String, target: MySquareImageView, horizontalScroll: Boolean, animateGifs: Boolean, cropThumbnails: Boolean) {
    target.isHorizontalScrolling = horizontalScroll
    if (type == TYPE_IMAGE || type == TYPE_VIDEO) {
        if (type == TYPE_IMAGE && path.isPng()) {
            loadPng(path, target, cropThumbnails)
        } else {
            loadJpg(path, target, cropThumbnails)
        }
    } else if (type == TYPE_GIF) {
        try {
            val gifDrawable = GifDrawable(path)
            target.setImageDrawable(gifDrawable)
            if (animateGifs) {
                gifDrawable.start()
            } else {
                gifDrawable.stop()
            }

            target.scaleType = if (cropThumbnails) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_CENTER
        } catch (e: Exception) {
            loadJpg(path, target, cropThumbnails)
        } catch (e: OutOfMemoryError) {
            loadJpg(path, target, cropThumbnails)
        }
    }
}

fun BaseSimpleActivity.tryCopyMoveFilesTo(files: ArrayList<File>, isCopyOperation: Boolean, callback: () -> Unit) {
    if (files.isEmpty()) {
        toast(R.string.unknown_error_occurred)
        return
    }

    val source = if (files[0].isFile) files[0].parent else files[0].absolutePath
    PickDirectoryDialog(this, source) {
        copyMoveFilesTo(files, source.trimEnd('/'), it, isCopyOperation, true, callback)
    }
}

fun BaseSimpleActivity.addTempFolderIfNeeded(dirs: ArrayList<Directory>): ArrayList<Directory> {
    val directories = ArrayList<Directory>()
    val tempFolderPath = config.tempFolderPath
    if (tempFolderPath.isNotEmpty()) {
        val newFolder = Directory(tempFolderPath, "", tempFolderPath.getFilenameFromPath(), 0, 0, 0, 0L)
        directories.add(newFolder)
    }
    directories.addAll(dirs)
    return directories
}

fun Activity.loadPng(path: String, target: MySquareImageView, cropThumbnails: Boolean) {
    val options = RequestOptions()
            .signature(path.getFileSignature())
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .format(DecodeFormat.PREFER_ARGB_8888)

    val builder = Glide.with(applicationContext)
            .asBitmap()
            .load(path)

    if (cropThumbnails) options.centerCrop() else options.fitCenter()
    builder.apply(options).into(target)
}

fun Activity.loadJpg(path: String, target: MySquareImageView, cropThumbnails: Boolean) {
    val options = RequestOptions()
            .signature(path.getFileSignature())
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)

    val builder = Glide.with(applicationContext)
            .load(path)

    if (cropThumbnails) options.centerCrop() else options.fitCenter()
    builder.apply(options).transition(DrawableTransitionOptions.withCrossFade()).into(target)
}

fun Activity.getCachedDirectories(): ArrayList<Directory> {
    val token = object : TypeToken<List<Directory>>() {}.type
    return Gson().fromJson<ArrayList<Directory>>(config.directories, token) ?: ArrayList<Directory>(1)
}

fun Activity.getCachedMedia(path: String): ArrayList<Medium> {
    val token = object : TypeToken<List<Medium>>() {}.type
    return Gson().fromJson<ArrayList<Medium>>(config.loadFolderMedia(path), token) ?: ArrayList(1)
}
