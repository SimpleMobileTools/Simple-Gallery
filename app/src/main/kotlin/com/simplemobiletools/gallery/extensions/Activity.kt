package com.simplemobiletools.gallery.extensions

import android.app.Activity
import android.content.Intent
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
import com.simplemobiletools.commons.models.FAQItem
import com.simplemobiletools.commons.models.FileDirItem
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

fun Activity.sharePath(path: String) {
    sharePathIntent(path, BuildConfig.APPLICATION_ID)
}

fun Activity.sharePaths(paths: ArrayList<String>) {
    sharePathsIntent(paths, BuildConfig.APPLICATION_ID)
}

fun Activity.shareMedium(medium: Medium) {
    sharePath(medium.path)
}

fun Activity.shareMedia(media: List<Medium>) {
    val paths = media.map { it.path } as ArrayList
    sharePaths(paths)
}

fun Activity.setAs(path: String) {
    setAsIntent(path, BuildConfig.APPLICATION_ID)
}

fun Activity.openPath(path: String, forceChooser: Boolean) {
    openPathIntent(path, forceChooser, BuildConfig.APPLICATION_ID)
}

fun Activity.openEditor(path: String) {
    openEditorIntent(path, BuildConfig.APPLICATION_ID)
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
    val faqItems = arrayListOf(
            FAQItem(R.string.faq_3_title_commons, R.string.faq_3_text_commons),
            FAQItem(R.string.faq_1_title, R.string.faq_1_text),
            FAQItem(R.string.faq_2_title, R.string.faq_2_text),
            FAQItem(R.string.faq_3_title, R.string.faq_3_text),
            FAQItem(R.string.faq_4_title, R.string.faq_4_text),
            FAQItem(R.string.faq_5_title, R.string.faq_5_text),
            FAQItem(R.string.faq_6_title, R.string.faq_6_text),
            FAQItem(R.string.faq_7_title, R.string.faq_7_text),
            FAQItem(R.string.faq_8_title, R.string.faq_8_text),
            FAQItem(R.string.faq_9_title, R.string.faq_9_text),
            FAQItem(R.string.faq_10_title, R.string.faq_10_text),
            FAQItem(R.string.faq_2_title_commons, R.string.faq_2_text_commons))

    startAboutActivity(R.string.app_name, LICENSE_GLIDE or LICENSE_CROPPER or LICENSE_MULTISELECT or LICENSE_RTL
            or LICENSE_SUBSAMPLING or LICENSE_PATTERN or LICENSE_REPRINT or LICENSE_GIF_DRAWABLE or LICENSE_PHOTOVIEW, BuildConfig.VERSION_NAME, faqItems)
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
    if (file.exists()) {
        callback()
        return
    }

    if (needsStupidWritePermissions(path)) {
        handleSAFDialog(file.absolutePath) {
            val fileDocument = getDocumentFile(path)
            if (fileDocument?.exists() == true && fileDocument.isDirectory) {
                fileDocument.createFile("", NOMEDIA)
                applicationContext.scanFile(file) {
                    callback()
                }
            } else {
                toast(R.string.unknown_error_occurred)
                callback()
            }
        }
    } else {
        try {
            file.createNewFile()
            applicationContext.scanFile(file) {
                callback()
            }
        } catch (e: Exception) {
            showErrorToast(e)
            callback()
        }
    }
}

fun BaseSimpleActivity.removeNoMedia(path: String, callback: (() -> Unit)? = null) {
    val file = File(path, NOMEDIA)
    if (!file.exists()) {
        callback?.invoke()
        return
    }

    deleteFile(file.toFileDirItem(applicationContext)) {
        callback?.invoke()
    }
}

fun BaseSimpleActivity.toggleFileVisibility(oldPath: String, hide: Boolean, callback: ((newPath: String) -> Unit)? = null) {
    val path = oldPath.getParentPath()
    var filename = oldPath.getFilenameFromPath()
    filename = if (hide) {
        ".${filename.trimStart('.')}"
    } else {
        filename.substring(1, filename.length)
    }

    val newPath = "$path$filename"
    renameFile(oldPath, newPath) {
        callback?.invoke(newPath)
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

fun BaseSimpleActivity.tryCopyMoveFilesTo(fileDirItems: ArrayList<FileDirItem>, isCopyOperation: Boolean, callback: (destinationPath: String) -> Unit) {
    if (fileDirItems.isEmpty()) {
        toast(R.string.unknown_error_occurred)
        return
    }

    val source = fileDirItems[0].getParentPath()
    PickDirectoryDialog(this, source) {
        copyMoveFilesTo(fileDirItems, source.trimEnd('/'), it, isCopyOperation, true, config.shouldShowHidden, callback)
    }
}

fun BaseSimpleActivity.addTempFolderIfNeeded(dirs: ArrayList<Directory>): ArrayList<Directory> {
    val directories = ArrayList<Directory>()
    val tempFolderPath = config.tempFolderPath
    if (tempFolderPath.isNotEmpty()) {
        val newFolder = Directory(null, tempFolderPath, "", tempFolderPath.getFilenameFromPath(), 0, 0, 0, 0L, isPathOnSD(tempFolderPath))
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

fun Activity.getCachedDirectories(callback: (ArrayList<Directory>) -> Unit) {
    Thread {
        val directoryDao = galleryDB.DirectoryDao()
        val directories = directoryDao.getAll() as ArrayList<Directory>
        callback(directories)

        directories.filter { !File(it.path).exists() }.forEach {
            directoryDao.deleteDir(it)
        }
    }.start()
}

fun Activity.getCachedMedia(path: String): ArrayList<Medium> {
    val token = object : TypeToken<List<Medium>>() {}.type
    return Gson().fromJson<ArrayList<Medium>>(config.loadFolderMedia(path), token) ?: ArrayList(1)
}
