package com.simplemobiletools.gallery.extensions

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.gallery.BuildConfig
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.SimpleActivity
import com.simplemobiletools.gallery.helpers.IS_FROM_GALLERY
import com.simplemobiletools.gallery.helpers.NOMEDIA
import com.simplemobiletools.gallery.helpers.REQUEST_EDIT_IMAGE
import com.simplemobiletools.gallery.helpers.REQUEST_SET_AS
import com.simplemobiletools.gallery.models.Directory
import com.simplemobiletools.gallery.models.Medium
import com.simplemobiletools.gallery.views.MySquareImageView
import java.io.File
import java.util.*

fun Activity.shareUri(uri: Uri) {
    val shareTitle = resources.getString(R.string.share_via)
    Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, ensurePublicUri(uri))
        type = getMimeTypeFromUri(uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(this, shareTitle))
    }
}

fun Activity.shareMedium(medium: Medium) {
    val shareTitle = resources.getString(R.string.share_via)
    val file = File(medium.path)
    val uri = getFilePublicUri(file, BuildConfig.APPLICATION_ID)

    Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, uri)
        type = getMimeTypeFromUri(uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(this, shareTitle))
    }
}

fun Activity.shareMedia(media: List<Medium>) {
    val shareTitle = resources.getString(R.string.share_via)
    val uris = media.map { getFilePublicUri(File(it.path), BuildConfig.APPLICATION_ID) } as ArrayList

    Intent().apply {
        action = Intent.ACTION_SEND_MULTIPLE
        type = uris.getMimeType()
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        startActivity(Intent.createChooser(this, shareTitle))
    }
}

fun Activity.setAs(uri: Uri) {
    val newUri = ensurePublicUri(uri)
    Intent().apply {
        action = Intent.ACTION_ATTACH_DATA
        setDataAndType(newUri, getMimeTypeFromUri(newUri))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val chooser = Intent.createChooser(this, getString(R.string.set_as))

        if (resolveActivity(packageManager) != null) {
            startActivityForResult(chooser, REQUEST_SET_AS)
        } else {
            toast(R.string.no_capable_app_found)
        }
    }
}

fun Activity.openFile(uri: Uri) {
    val newUri = ensurePublicUri(uri)
    Intent().apply {
        action = Intent.ACTION_VIEW
        setDataAndType(newUri, getMimeTypeFromUri(newUri))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra(IS_FROM_GALLERY, true)

        if (resolveActivity(packageManager) != null) {
            val chooser = Intent.createChooser(this, getString(R.string.open_with))
            startActivity(chooser)
        } else {
            toast(R.string.no_app_found)
        }
    }
}

fun Activity.openEditor(uri: Uri) {
    val newUri = ensurePublicUri(uri)
    Intent().apply {
        action = Intent.ACTION_EDIT
        setDataAndType(newUri, getMimeTypeFromUri(newUri))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        if (isNougatPlus()) {
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
        }

        if (resolveActivity(packageManager) != null) {
            val chooser = Intent.createChooser(this, getString(R.string.edit_image_with))
            startActivityForResult(chooser, REQUEST_EDIT_IMAGE)
        } else {
            toast(R.string.no_editor_found)
        }
    }
}

fun Activity.launchCamera() {
    val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
    if (intent.resolveActivity(packageManager) != null) {
        startActivity(intent)
    } else {
        toast(R.string.no_camera_app_found)
    }
}

fun SimpleActivity.launchAbout() {
    startAboutActivity(R.string.app_name, LICENSE_KOTLIN or LICENSE_GLIDE or LICENSE_CROPPER or LICENSE_MULTISELECT or LICENSE_RTL
            or LICENSE_PHOTOVIEW or LICENSE_SUBSAMPLING or LICENSE_PATTERN, BuildConfig.VERSION_NAME)
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

    scanFile(file) {
        callback()
    }
}

fun SimpleActivity.removeNoMedia(path: String, callback: () -> Unit) {
    val file = File(path, NOMEDIA)
    deleteFile(file) {
        callback()
    }
}

fun SimpleActivity.toggleFileVisibility(oldFile: File, hide: Boolean, callback: (newFile: File) -> Unit) {
    val path = oldFile.parent
    var filename = oldFile.name
    filename = if (hide) {
        ".${filename.trimStart('.')}"
    } else {
        filename.substring(1, filename.length)
    }
    val newFile = File(path, filename)
    renameFile(oldFile, newFile) {
        callback(newFile)
    }
}

fun Activity.loadImage(path: String, target: MySquareImageView, verticalScroll: Boolean) {
    target.isVerticalScrolling = verticalScroll
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
    val options = RequestOptions()
            .signature(path.getFileSignature())
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .format(DecodeFormat.PREFER_ARGB_8888)

    val builder = Glide.with(applicationContext)
            .asBitmap()
            .load(path)

    if (config.cropThumbnails) options.centerCrop() else options.fitCenter()
    builder.apply(options).into(target)
}

fun Activity.loadJpg(path: String, target: MySquareImageView) {
    val options = RequestOptions()
            .signature(path.getFileSignature())
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)

    val builder = Glide.with(applicationContext)
            .load(path)

    if (config.cropThumbnails) options.centerCrop() else options.fitCenter()
    builder.apply(options).transition(DrawableTransitionOptions.withCrossFade()).into(target)
}

fun Activity.loadAnimatedGif(path: String, target: MySquareImageView) {
    val options = RequestOptions()
            .signature(path.getFileSignature())
            .diskCacheStrategy(DiskCacheStrategy.NONE)

    val builder = Glide.with(applicationContext)
            .asGif()
            .load(path)

    if (config.cropThumbnails) options.centerCrop() else options.fitCenter()
    builder.apply(options).transition(DrawableTransitionOptions.withCrossFade()).into(target)
}

fun Activity.loadStaticGif(path: String, target: MySquareImageView) {
    val options = RequestOptions()
            .signature(path.getFileSignature())
            .diskCacheStrategy(DiskCacheStrategy.DATA)

    val builder = Glide.with(applicationContext)
            .asBitmap()
            .load(path)

    if (config.cropThumbnails) options.centerCrop() else options.fitCenter()
    builder.apply(options).into(target)
}

fun Activity.getCachedDirectories(): ArrayList<Directory> {
    val token = object : TypeToken<List<Directory>>() {}.type
    return Gson().fromJson<ArrayList<Directory>>(config.directories, token) ?: ArrayList<Directory>(1)
}

fun Activity.getCachedMedia(path: String): ArrayList<Medium> {
    val token = object : TypeToken<List<Medium>>() {}.type
    return Gson().fromJson<ArrayList<Medium>>(config.loadFolderMedia(path), token) ?: ArrayList(1)
}
