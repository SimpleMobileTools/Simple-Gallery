package com.simplemobiletools.gallery.extensions

import android.app.Activity
import android.content.Intent
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.view.View
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
import com.simplemobiletools.gallery.models.Medium
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

    tryDeleteFileDirItem(file.toFileDirItem(applicationContext)) {
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

    val newPath = "$path/$filename"
    renameFile(oldPath, newPath) {
        callback?.invoke(newPath)
        Thread {
            updateDBMediaPath(oldPath, newPath)
        }.start()
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

fun BaseSimpleActivity.tryDeleteFileDirItem(fileDirItem: FileDirItem, allowDeleteFolder: Boolean = false, callback: ((wasSuccess: Boolean) -> Unit)? = null) {
    deleteFile(fileDirItem, allowDeleteFolder) {
        callback?.invoke(it)

        Thread {
            galleryDB.MediumDao().deleteMediumPath(fileDirItem.path)
        }.start()
    }
}
