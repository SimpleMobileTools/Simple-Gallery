package com.simplemobiletools.gallery.extensions

import android.app.Activity
import android.content.Intent
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.FAQItem
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.gallery.BuildConfig
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.SimpleActivity
import com.simplemobiletools.gallery.dialogs.PickDirectoryDialog
import com.simplemobiletools.gallery.helpers.NOMEDIA
import com.simplemobiletools.gallery.interfaces.MediumDao
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.*

fun Activity.sharePath(path: String) {
    sharePathIntent(path, BuildConfig.APPLICATION_ID)
}

fun Activity.sharePaths(paths: ArrayList<String>) {
    sharePathsIntent(paths, BuildConfig.APPLICATION_ID)
}

fun Activity.shareMediumPath(path: String) {
    sharePath(path)
}

fun Activity.shareMediaPaths(paths: ArrayList<String>) {
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
    val licenses = LICENSE_GLIDE or LICENSE_CROPPER or LICENSE_MULTISELECT or LICENSE_RTL or LICENSE_SUBSAMPLING or LICENSE_PATTERN or
            LICENSE_REPRINT or LICENSE_GIF_DRAWABLE or LICENSE_PHOTOVIEW or LICENSE_PICASSO or LICENSE_EXOPLAYER or LICENSE_PANORAMA_VIEW or LICENSE_SANSELAN or LICENSE_FILTERS

    val faqItems = arrayListOf(
            FAQItem(R.string.faq_5_title_commons, R.string.faq_5_text_commons),
            FAQItem(R.string.faq_1_title, R.string.faq_1_text),
            FAQItem(R.string.faq_2_title, R.string.faq_2_text),
            FAQItem(R.string.faq_3_title, R.string.faq_3_text),
            FAQItem(R.string.faq_4_title, R.string.faq_4_text),
            FAQItem(R.string.faq_5_title, R.string.faq_5_text),
            FAQItem(R.string.faq_6_title, R.string.faq_6_text),
            FAQItem(R.string.faq_7_title, R.string.faq_7_text),
            FAQItem(R.string.faq_8_title, R.string.faq_8_text),
            FAQItem(R.string.faq_10_title, R.string.faq_10_text),
            FAQItem(R.string.faq_11_title, R.string.faq_11_text),
            FAQItem(R.string.faq_12_title, R.string.faq_12_text),
            FAQItem(R.string.faq_2_title_commons, R.string.faq_2_text_commons))

    startAboutActivity(R.string.app_name, licenses, BuildConfig.VERSION_NAME, faqItems, true)
}

fun AppCompatActivity.showSystemUI(toggleActionBarVisibility: Boolean) {
    if (toggleActionBarVisibility) {
        supportActionBar?.show()
    }

    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
}

fun AppCompatActivity.hideSystemUI(toggleActionBarVisibility: Boolean) {
    if (toggleActionBarVisibility) {
        supportActionBar?.hide()
    }

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
                applicationContext.scanFileRecursively(file) {
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
            applicationContext.scanFileRecursively(file) {
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

    tryDeleteFileDirItem(file.toFileDirItem(applicationContext), false, false) {
        callback?.invoke()
    }
}

fun BaseSimpleActivity.toggleFileVisibility(oldPath: String, hide: Boolean, callback: ((newPath: String) -> Unit)? = null) {
    val path = oldPath.getParentPath()
    var filename = oldPath.getFilenameFromPath()
    if ((hide && filename.startsWith('.')) || (!hide && !filename.startsWith('.'))) {
        callback?.invoke(oldPath)
        return
    }

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

fun BaseSimpleActivity.tryDeleteFileDirItem(fileDirItem: FileDirItem, allowDeleteFolder: Boolean = false, deleteFromDatabase: Boolean,
                                            callback: ((wasSuccess: Boolean) -> Unit)? = null) {
    deleteFile(fileDirItem, allowDeleteFolder) {
        if (deleteFromDatabase) {
            Thread {
                galleryDB.MediumDao().deleteMediumPath(fileDirItem.path)
                runOnUiThread {
                    callback?.invoke(it)
                }
            }.start()
        } else {
            callback?.invoke(it)
        }
    }
}

fun BaseSimpleActivity.movePathsInRecycleBin(paths: ArrayList<String>, mediumDao: MediumDao = galleryDB.MediumDao(), callback: ((wasSuccess: Boolean) -> Unit)?) {
    Thread {
        var pathsCnt = paths.size
        paths.forEach {
            val file = File(it)
            val internalFile = File(filesDir.absolutePath, it)
            try {
                if (file.copyRecursively(internalFile, true)) {
                    mediumDao.updateDeleted(it, System.currentTimeMillis())
                    pathsCnt--
                }
            } catch (ignored: Exception) {
            }
        }
        callback?.invoke(pathsCnt == 0)
    }.start()
}

fun BaseSimpleActivity.restoreRecycleBinPath(path: String, callback: () -> Unit) {
    restoreRecycleBinPaths(arrayListOf(path), galleryDB.MediumDao(), callback)
}

fun BaseSimpleActivity.restoreRecycleBinPaths(paths: ArrayList<String>, mediumDao: MediumDao = galleryDB.MediumDao(), callback: () -> Unit) {
    Thread {
        paths.forEach {
            val source = it
            val destination = it.removePrefix(filesDir.absolutePath)

            var inputStream: InputStream? = null
            var out: OutputStream? = null
            try {
                out = getFileOutputStreamSync(destination, source.getMimeType())
                inputStream = getFileInputStreamSync(source)!!
                inputStream.copyTo(out!!)
                if (File(source).length() == File(destination).length()) {
                    mediumDao.updateDeleted(destination, 0)
                }
            } catch (e: Exception) {
                showErrorToast(e)
            } finally {
                inputStream?.close()
                out?.close()
            }
        }

        runOnUiThread {
            callback()
        }
    }.start()
}

fun BaseSimpleActivity.emptyTheRecycleBin(callback: (() -> Unit)? = null) {
    Thread {
        filesDir.deleteRecursively()
        galleryDB.MediumDao().clearRecycleBin()
        galleryDB.DirectoryDao().deleteRecycleBin()
        toast(R.string.recycle_bin_emptied)
        callback?.invoke()
    }.start()
}

fun BaseSimpleActivity.emptyAndDisableTheRecycleBin(callback: () -> Unit) {
    Thread {
        emptyTheRecycleBin {
            config.useRecycleBin = false
            callback()
        }
    }.start()
}

fun BaseSimpleActivity.showRecycleBinEmptyingDialog(callback: () -> Unit) {
    ConfirmationDialog(this, "", R.string.empty_recycle_bin_confirmation, R.string.yes, R.string.no) {
        callback()
    }
}
