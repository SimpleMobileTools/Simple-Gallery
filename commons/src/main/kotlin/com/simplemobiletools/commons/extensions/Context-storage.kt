package com.simplemobiletools.commons.extensions

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.MediaStore.*
import android.text.TextUtils
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.FileDirItem
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URLDecoder
import java.util.*
import java.util.regex.Pattern

private const val ANDROID_DATA_DIR = "/Android/data/"
private const val ANDROID_OBB_DIR = "/Android/obb/"
val DIRS_ACCESSIBLE_ONLY_WITH_SAF = listOf(ANDROID_DATA_DIR, ANDROID_OBB_DIR)
val Context.recycleBinPath: String get() = filesDir.absolutePath

// http://stackoverflow.com/a/40582634/1967672
fun Context.getSDCardPath(): String {
    val directories = getStorageDirectories().filter {
        !it.equals(getInternalStoragePath()) && !it.equals(
            "/storage/emulated/0",
            true
        ) && (baseConfig.OTGPartition.isEmpty() || !it.endsWith(baseConfig.OTGPartition))
    }

    val fullSDpattern = Pattern.compile(SD_OTG_PATTERN)
    var sdCardPath = directories.firstOrNull { fullSDpattern.matcher(it).matches() }
        ?: directories.firstOrNull { !physicalPaths.contains(it.toLowerCase()) } ?: ""

    // on some devices no method retrieved any SD card path, so test if its not sdcard1 by any chance. It happened on an Android 5.1
    if (sdCardPath.trimEnd('/').isEmpty()) {
        val file = File("/storage/sdcard1")
        if (file.exists()) {
            return file.absolutePath
        }

        sdCardPath = directories.firstOrNull() ?: ""
    }

    if (sdCardPath.isEmpty()) {
        val SDpattern = Pattern.compile(SD_OTG_SHORT)
        try {
            File("/storage").listFiles()?.forEach {
                if (SDpattern.matcher(it.name).matches()) {
                    sdCardPath = "/storage/${it.name}"
                }
            }
        } catch (e: Exception) {
        }
    }

    val finalPath = sdCardPath.trimEnd('/')
    baseConfig.sdCardPath = finalPath
    return finalPath
}

fun Context.hasExternalSDCard() = sdCardPath.isNotEmpty()

fun Context.hasOTGConnected(): Boolean {
    return try {
        (getSystemService(Context.USB_SERVICE) as UsbManager).deviceList.any {
            it.value.getInterface(0).interfaceClass == UsbConstants.USB_CLASS_MASS_STORAGE
        }
    } catch (e: Exception) {
        false
    }
}

fun Context.getStorageDirectories(): Array<String> {
    val paths = HashSet<String>()
    val rawExternalStorage = System.getenv("EXTERNAL_STORAGE")
    val rawSecondaryStoragesStr = System.getenv("SECONDARY_STORAGE")
    val rawEmulatedStorageTarget = System.getenv("EMULATED_STORAGE_TARGET")
    if (TextUtils.isEmpty(rawEmulatedStorageTarget)) {
        getExternalFilesDirs(null).filterNotNull().map { it.absolutePath }
            .mapTo(paths) { it.substring(0, it.indexOf("Android/data")) }
    } else {
        val path = Environment.getExternalStorageDirectory().absolutePath
        val folders = Pattern.compile("/").split(path)
        val lastFolder = folders[folders.size - 1]
        var isDigit = false
        try {
            Integer.valueOf(lastFolder)
            isDigit = true
        } catch (ignored: NumberFormatException) {
        }

        val rawUserId = if (isDigit) lastFolder else ""
        if (TextUtils.isEmpty(rawUserId)) {
            paths.add(rawEmulatedStorageTarget!!)
        } else {
            paths.add(rawEmulatedStorageTarget + File.separator + rawUserId)
        }
    }

    if (!TextUtils.isEmpty(rawSecondaryStoragesStr)) {
        val rawSecondaryStorages = rawSecondaryStoragesStr!!.split(File.pathSeparator.toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
        Collections.addAll(paths, *rawSecondaryStorages)
    }
    return paths.map { it.trimEnd('/') }.toTypedArray()
}

fun Context.getHumanReadablePath(path: String): String {
    return getString(
        when (path) {
            "/" -> R.string.root
            internalStoragePath -> R.string.internal
            otgPath -> R.string.usb
            else -> R.string.sd_card
        }
    )
}

fun Context.humanizePath(path: String): String {
    val trimmedPath = path.trimEnd('/')
    val basePath = path.getBasePath(this)
    return when (basePath) {
        "/" -> "${getHumanReadablePath(basePath)}$trimmedPath"
        else -> trimmedPath.replaceFirst(basePath, getHumanReadablePath(basePath))
    }
}

fun Context.getInternalStoragePath() =
    if (File("/storage/emulated/0").exists()) "/storage/emulated/0" else Environment.getExternalStorageDirectory().absolutePath.trimEnd('/')

fun Context.isPathOnSD(path: String) = sdCardPath.isNotEmpty() && path.startsWith(sdCardPath)

fun Context.isPathOnOTG(path: String) = otgPath.isNotEmpty() && path.startsWith(otgPath)

fun Context.isPathOnInternalStorage(path: String) = internalStoragePath.isNotEmpty() && path.startsWith(internalStoragePath)

fun Context.getSAFOnlyDirs(): List<String> {
    return DIRS_ACCESSIBLE_ONLY_WITH_SAF.map { "$internalStoragePath$it" } +
        DIRS_ACCESSIBLE_ONLY_WITH_SAF.map { "$sdCardPath$it" }
}

fun Context.isSAFOnlyRoot(path: String): Boolean {
    return getSAFOnlyDirs().any { "${path.trimEnd('/')}/".startsWith(it) }
}

fun Context.isRestrictedSAFOnlyRoot(path: String): Boolean {
    return isRPlus() && isSAFOnlyRoot(path)
}

// no need to use DocumentFile if an SD card is set as the default storage
fun Context.needsStupidWritePermissions(path: String) = !isRPlus() && (isPathOnSD(path) || isPathOnOTG(path)) && !isSDCardSetAsDefaultStorage()

fun Context.isSDCardSetAsDefaultStorage() = sdCardPath.isNotEmpty() && Environment.getExternalStorageDirectory().absolutePath.equals(sdCardPath, true)

fun Context.hasProperStoredTreeUri(isOTG: Boolean): Boolean {
    val uri = if (isOTG) baseConfig.OTGTreeUri else baseConfig.sdTreeUri
    val hasProperUri = contentResolver.persistedUriPermissions.any { it.uri.toString() == uri }
    if (!hasProperUri) {
        if (isOTG) {
            baseConfig.OTGTreeUri = ""
        } else {
            baseConfig.sdTreeUri = ""
        }
    }
    return hasProperUri
}

fun Context.hasProperStoredAndroidTreeUri(path: String): Boolean {
    val uri = getAndroidTreeUri(path)
    val hasProperUri = contentResolver.persistedUriPermissions.any { it.uri.toString() == uri }
    if (!hasProperUri) {
        storeAndroidTreeUri(path, "")
    }
    return hasProperUri
}

fun Context.getAndroidTreeUri(path: String): String {
    return when {
        isPathOnOTG(path) -> if (isAndroidDataDir(path)) baseConfig.otgAndroidDataTreeUri else baseConfig.otgAndroidObbTreeUri
        isPathOnSD(path) -> if (isAndroidDataDir(path)) baseConfig.sdAndroidDataTreeUri else baseConfig.sdAndroidObbTreeUri
        else -> if (isAndroidDataDir(path)) baseConfig.primaryAndroidDataTreeUri else baseConfig.primaryAndroidObbTreeUri
    }
}

fun isAndroidDataDir(path: String): Boolean {
    val resolvedPath = "${path.trimEnd('/')}/"
    return resolvedPath.contains(ANDROID_DATA_DIR)
}

fun Context.storeAndroidTreeUri(path: String, treeUri: String) {
    return when {
        isPathOnOTG(path) -> if (isAndroidDataDir(path)) baseConfig.otgAndroidDataTreeUri = treeUri else baseConfig.otgAndroidObbTreeUri = treeUri
        isPathOnSD(path) -> if (isAndroidDataDir(path)) baseConfig.sdAndroidDataTreeUri = treeUri else baseConfig.sdAndroidObbTreeUri = treeUri
        else -> if (isAndroidDataDir(path)) baseConfig.primaryAndroidDataTreeUri = treeUri else baseConfig.primaryAndroidObbTreeUri = treeUri
    }
}

fun Context.getSAFStorageId(fullPath: String): String {
    return if (fullPath.startsWith('/')) {
        when {
            fullPath.startsWith(internalStoragePath) -> "primary"
            else -> fullPath.substringAfter("/storage/", "").substringBefore('/')
        }
    } else {
        fullPath.substringBefore(':', "").substringAfterLast('/')
    }
}

fun Context.createDocumentUriFromRootTree(fullPath: String): Uri {
    val storageId = getSAFStorageId(fullPath)

    val relativePath = when {
        fullPath.startsWith(internalStoragePath) -> fullPath.substring(internalStoragePath.length).trim('/')
        else -> fullPath.substringAfter(storageId).trim('/')
    }

    val treeUri = DocumentsContract.buildTreeDocumentUri(EXTERNAL_STORAGE_PROVIDER_AUTHORITY, "$storageId:")
    val documentId = "${storageId}:$relativePath"
    return DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
}

fun Context.createAndroidDataOrObbPath(fullPath: String): String {
    return if (isAndroidDataDir(fullPath)) {
        fullPath.getBasePath(this).trimEnd('/').plus(ANDROID_DATA_DIR)
    } else {
        fullPath.getBasePath(this).trimEnd('/').plus(ANDROID_OBB_DIR)
    }
}

fun Context.createAndroidDataOrObbUri(fullPath: String): Uri {
    val path = createAndroidDataOrObbPath(fullPath)
    return createDocumentUriFromRootTree(path)
}

fun Context.getStorageRootIdForAndroidDir(path: String) =
    getAndroidTreeUri(path).removeSuffix(if (isAndroidDataDir(path)) "%3AAndroid%2Fdata" else "%3AAndroid%2Fobb").substringAfterLast('/').trimEnd('/')

fun Context.isAStorageRootFolder(path: String): Boolean {
    val trimmed = path.trimEnd('/')
    return trimmed.isEmpty() || trimmed.equals(internalStoragePath, true) || trimmed.equals(sdCardPath, true) || trimmed.equals(otgPath, true)
}

fun Context.getMyFileUri(file: File): Uri {
    return if (isNougatPlus()) {
        FileProvider.getUriForFile(this, "$packageName.provider", file)
    } else {
        Uri.fromFile(file)
    }
}

fun Context.tryFastDocumentDelete(path: String, allowDeleteFolder: Boolean): Boolean {
    val document = getFastDocumentFile(path)
    return if (document?.isFile == true || allowDeleteFolder) {
        try {
            DocumentsContract.deleteDocument(contentResolver, document?.uri!!)
        } catch (e: Exception) {
            false
        }
    } else {
        false
    }
}

fun Context.getFastDocumentFile(path: String): DocumentFile? {
    if (isPathOnOTG(path)) {
        return getOTGFastDocumentFile(path)
    }

    if (baseConfig.sdCardPath.isEmpty()) {
        return null
    }

    val relativePath = Uri.encode(path.substring(baseConfig.sdCardPath.length).trim('/'))
    val externalPathPart = baseConfig.sdCardPath.split("/").lastOrNull(String::isNotEmpty)?.trim('/') ?: return null
    val fullUri = "${baseConfig.sdTreeUri}/document/$externalPathPart%3A$relativePath"
    return DocumentFile.fromSingleUri(this, Uri.parse(fullUri))
}

fun Context.getOTGFastDocumentFile(path: String, otgPathToUse: String? = null): DocumentFile? {
    if (baseConfig.OTGTreeUri.isEmpty()) {
        return null
    }

    val otgPath = otgPathToUse ?: baseConfig.OTGPath
    if (baseConfig.OTGPartition.isEmpty()) {
        baseConfig.OTGPartition = baseConfig.OTGTreeUri.removeSuffix("%3A").substringAfterLast('/').trimEnd('/')
        updateOTGPathFromPartition()
    }

    val relativePath = Uri.encode(path.substring(otgPath.length).trim('/'))
    val fullUri = "${baseConfig.OTGTreeUri}/document/${baseConfig.OTGPartition}%3A$relativePath"
    return DocumentFile.fromSingleUri(this, Uri.parse(fullUri))
}

fun Context.getDocumentFile(path: String): DocumentFile? {
    val isOTG = isPathOnOTG(path)
    var relativePath = path.substring(if (isOTG) otgPath.length else sdCardPath.length)
    if (relativePath.startsWith(File.separator)) {
        relativePath = relativePath.substring(1)
    }

    return try {
        val treeUri = Uri.parse(if (isOTG) baseConfig.OTGTreeUri else baseConfig.sdTreeUri)
        var document = DocumentFile.fromTreeUri(applicationContext, treeUri)
        val parts = relativePath.split("/").filter { it.isNotEmpty() }
        for (part in parts) {
            document = document?.findFile(part)
        }
        document
    } catch (ignored: Exception) {
        null
    }
}

fun Context.getSomeDocumentFile(path: String) = getFastDocumentFile(path) ?: getDocumentFile(path)

fun Context.scanFileRecursively(file: File, callback: (() -> Unit)? = null) {
    scanFilesRecursively(arrayListOf(file), callback)
}

fun Context.scanPathRecursively(path: String, callback: (() -> Unit)? = null) {
    scanPathsRecursively(arrayListOf(path), callback)
}

fun Context.scanFilesRecursively(files: List<File>, callback: (() -> Unit)? = null) {
    val allPaths = ArrayList<String>()
    for (file in files) {
        allPaths.addAll(getPaths(file))
    }
    rescanPaths(allPaths, callback)
}

fun Context.scanPathsRecursively(paths: List<String>, callback: (() -> Unit)? = null) {
    val allPaths = ArrayList<String>()
    for (path in paths) {
        allPaths.addAll(getPaths(File(path)))
    }
    rescanPaths(allPaths, callback)
}

fun Context.rescanPath(path: String, callback: (() -> Unit)? = null) {
    rescanPaths(arrayListOf(path), callback)
}

// avoid calling this multiple times in row, it can delete whole folder contents
fun Context.rescanPaths(paths: List<String>, callback: (() -> Unit)? = null) {
    if (paths.isEmpty()) {
        callback?.invoke()
        return
    }

    for (path in paths) {
        Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
            data = Uri.fromFile(File(path))
            sendBroadcast(this)
        }
    }

    var cnt = paths.size
    MediaScannerConnection.scanFile(applicationContext, paths.toTypedArray(), null) { s, uri ->
        if (--cnt == 0) {
            callback?.invoke()
        }
    }
}

fun getPaths(file: File): ArrayList<String> {
    val paths = arrayListOf<String>(file.absolutePath)
    if (file.isDirectory) {
        val files = file.listFiles() ?: return paths
        for (curFile in files) {
            paths.addAll(getPaths(curFile))
        }
    }
    return paths
}

fun Context.getFileUri(path: String) = when {
    path.isImageSlow() -> Images.Media.EXTERNAL_CONTENT_URI
    path.isVideoSlow() -> Video.Media.EXTERNAL_CONTENT_URI
    path.isAudioSlow() -> Audio.Media.EXTERNAL_CONTENT_URI
    else -> Files.getContentUri("external")
}

// these functions update the mediastore instantly, MediaScannerConnection.scanFileRecursively takes some time to really get applied
fun Context.deleteFromMediaStore(path: String, callback: ((needsRescan: Boolean) -> Unit)? = null) {
    if (getIsPathDirectory(path)) {
        callback?.invoke(false)
        return
    }

    ensureBackgroundThread {
        try {
            val where = "${MediaColumns.DATA} = ?"
            val args = arrayOf(path)
            val needsRescan = contentResolver.delete(getFileUri(path), where, args) != 1
            callback?.invoke(needsRescan)
        } catch (ignored: Exception) {
            callback?.invoke(true)
        }
    }
}

fun Context.rescanAndDeletePath(path: String, callback: () -> Unit) {
    val SCAN_FILE_MAX_DURATION = 1000L
    val scanFileHandler = Handler(Looper.getMainLooper())
    scanFileHandler.postDelayed({
        callback()
    }, SCAN_FILE_MAX_DURATION)

    MediaScannerConnection.scanFile(applicationContext, arrayOf(path), null) { path, uri ->
        scanFileHandler.removeCallbacksAndMessages(null)
        try {
            applicationContext.contentResolver.delete(uri, null, null)
        } catch (e: Exception) {
        }
        callback()
    }
}

fun Context.updateInMediaStore(oldPath: String, newPath: String) {
    ensureBackgroundThread {
        val values = ContentValues().apply {
            put(MediaColumns.DATA, newPath)
            put(MediaColumns.DISPLAY_NAME, newPath.getFilenameFromPath())
            put(MediaColumns.TITLE, newPath.getFilenameFromPath())
        }
        val uri = getFileUri(oldPath)
        val selection = "${MediaColumns.DATA} = ?"
        val selectionArgs = arrayOf(oldPath)

        try {
            contentResolver.update(uri, values, selection, selectionArgs)
        } catch (ignored: Exception) {
        }
    }
}

fun Context.updateLastModified(path: String, lastModified: Long) {
    val values = ContentValues().apply {
        put(MediaColumns.DATE_MODIFIED, lastModified / 1000)
    }
    File(path).setLastModified(lastModified)
    val uri = getFileUri(path)
    val selection = "${MediaColumns.DATA} = ?"
    val selectionArgs = arrayOf(path)

    try {
        contentResolver.update(uri, values, selection, selectionArgs)
    } catch (ignored: Exception) {
    }
}

fun Context.getOTGItems(path: String, shouldShowHidden: Boolean, getProperFileSize: Boolean, callback: (ArrayList<FileDirItem>) -> Unit) {
    val items = ArrayList<FileDirItem>()
    val OTGTreeUri = baseConfig.OTGTreeUri
    var rootUri = try {
        DocumentFile.fromTreeUri(applicationContext, Uri.parse(OTGTreeUri))
    } catch (e: Exception) {
        showErrorToast(e)
        baseConfig.OTGPath = ""
        baseConfig.OTGTreeUri = ""
        baseConfig.OTGPartition = ""
        null
    }

    if (rootUri == null) {
        callback(items)
        return
    }

    val parts = path.split("/").dropLastWhile { it.isEmpty() }
    for (part in parts) {
        if (path == otgPath) {
            break
        }

        if (part == "otg:" || part == "") {
            continue
        }

        val file = rootUri!!.findFile(part)
        if (file != null) {
            rootUri = file
        }
    }

    val files = rootUri!!.listFiles().filter { it.exists() }

    val basePath = "${baseConfig.OTGTreeUri}/document/${baseConfig.OTGPartition}%3A"
    for (file in files) {
        val name = file.name ?: continue
        if (!shouldShowHidden && name.startsWith(".")) {
            continue
        }

        val isDirectory = file.isDirectory
        val filePath = file.uri.toString().substring(basePath.length)
        val decodedPath = otgPath + "/" + URLDecoder.decode(filePath, "UTF-8")
        val fileSize = when {
            getProperFileSize -> file.getItemSize(shouldShowHidden)
            isDirectory -> 0L
            else -> file.length()
        }

        val childrenCount = if (isDirectory) {
            file.listFiles().size
        } else {
            0
        }

        val lastModified = file.lastModified()
        val fileDirItem = FileDirItem(decodedPath, name, isDirectory, childrenCount, fileSize, lastModified)
        items.add(fileDirItem)
    }

    callback(items)
}

@RequiresApi(Build.VERSION_CODES.O)
fun Context.getAndroidSAFFileItems(path: String, shouldShowHidden: Boolean, getProperFileSize: Boolean = true, callback: (ArrayList<FileDirItem>) -> Unit) {
    val items = ArrayList<FileDirItem>()
    val rootDocId = getStorageRootIdForAndroidDir(path)
    val treeUri = getAndroidTreeUri(path).toUri()
    val documentId = createAndroidSAFDocumentId(path)
    val childrenUri = try {
        DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
    } catch (e: Exception) {
        showErrorToast(e)
        storeAndroidTreeUri(path, "")
        null
    }

    if (childrenUri == null) {
        callback(items)
        return
    }

    val projection = arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE, Document.COLUMN_LAST_MODIFIED)
    try {
        val rawCursor = contentResolver.query(childrenUri, projection, null, null)!!
        val cursor = ExternalStorageProviderHack.transformQueryResult(rootDocId, childrenUri, rawCursor)
        cursor.use {
            if (cursor.moveToFirst()) {
                do {
                    val docId = cursor.getStringValue(Document.COLUMN_DOCUMENT_ID)
                    val name = cursor.getStringValue(Document.COLUMN_DISPLAY_NAME)
                    val mimeType = cursor.getStringValue(Document.COLUMN_MIME_TYPE)
                    val lastModified = cursor.getLongValue(Document.COLUMN_LAST_MODIFIED)
                    val isDirectory = mimeType == Document.MIME_TYPE_DIR
                    val filePath = docId.substring("${getStorageRootIdForAndroidDir(path)}:".length)
                    if (!shouldShowHidden && name.startsWith(".")) {
                        continue
                    }

                    val decodedPath = path.getBasePath(this) + "/" + URLDecoder.decode(filePath, "UTF-8")
                    val fileSize = when {
                        getProperFileSize -> getFileSize(treeUri, docId)
                        isDirectory -> 0L
                        else -> getFileSize(treeUri, docId)
                    }

                    val childrenCount = if (isDirectory) {
                        getDirectChildrenCount(rootDocId, treeUri, docId, shouldShowHidden)
                    } else {
                        0
                    }

                    val fileDirItem = FileDirItem(decodedPath, name, isDirectory, childrenCount, fileSize, lastModified)
                    items.add(fileDirItem)
                } while (cursor.moveToNext())
            }
        }
    } catch (e: Exception) {
        showErrorToast(e)
    }
    callback(items)
}

fun Context.getDirectChildrenCount(rootDocId: String, treeUri: Uri, documentId: String, shouldShowHidden: Boolean): Int {
    return try {
        val projection = arrayOf(Document.COLUMN_DOCUMENT_ID)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
        val rawCursor = contentResolver.query(childrenUri, projection, null, null, null)!!
        val cursor = ExternalStorageProviderHack.transformQueryResult(rootDocId, childrenUri, rawCursor)
        if (shouldShowHidden) {
            cursor.count
        } else {
            var count = 0
            cursor.use {
                while (cursor.moveToNext()) {
                    val docId = cursor.getStringValue(Document.COLUMN_DOCUMENT_ID)
                    if (!docId.getFilenameFromPath().startsWith('.') || shouldShowHidden) {
                        count++
                    }
                }
            }
            count
        }
    } catch (e: Exception) {
        0
    }
}

fun Context.getProperChildrenCount(rootDocId: String, treeUri: Uri, documentId: String, shouldShowHidden: Boolean): Int {
    val projection = arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_MIME_TYPE)
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
    val rawCursor = contentResolver.query(childrenUri, projection, null, null, null)!!
    val cursor = ExternalStorageProviderHack.transformQueryResult(rootDocId, childrenUri, rawCursor)
    return if (cursor.count > 0) {
        var count = 0
        cursor.use {
            while (cursor.moveToNext()) {
                val docId = cursor.getStringValue(Document.COLUMN_DOCUMENT_ID)
                val mimeType = cursor.getStringValue(Document.COLUMN_MIME_TYPE)
                if (mimeType == Document.MIME_TYPE_DIR) {
                    count++
                    count += getProperChildrenCount(rootDocId, treeUri, docId, shouldShowHidden)
                } else if (!docId.getFilenameFromPath().startsWith('.') || shouldShowHidden) {
                    count++
                }
            }
        }
        count
    } else {
        1
    }
}

fun Context.getFileSize(treeUri: Uri, documentId: String): Long {
    val projection = arrayOf(Document.COLUMN_SIZE)
    val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
    return contentResolver.query(documentUri, projection, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            cursor.getLongValue(Document.COLUMN_SIZE)
        } else {
            0L
        }
    } ?: 0L
}

fun Context.createAndroidSAFDocumentId(path: String): String {
    val basePath = path.getBasePath(this)
    val relativePath = path.substring(basePath.length).trim('/')
    val storageId = getStorageRootIdForAndroidDir(path)
    return "$storageId:$relativePath"
}


fun Context.getAndroidSAFUri(path: String): Uri {
    val treeUri = getAndroidTreeUri(path).toUri()
    val documentId = createAndroidSAFDocumentId(path)
    return DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
}

fun Context.getAndroidSAFDocument(path: String): DocumentFile? {
    val basePath = path.getBasePath(this)
    val androidPath = File(basePath, "Android").path
    var relativePath = path.substring(androidPath.length)
    if (relativePath.startsWith(File.separator)) {
        relativePath = relativePath.substring(1)
    }

    return try {
        val treeUri = getAndroidTreeUri(path).toUri()
        var document = DocumentFile.fromTreeUri(applicationContext, treeUri)
        val parts = relativePath.split("/").filter { it.isNotEmpty() }
        for (part in parts) {
            document = document?.findFile(part)
        }
        document
    } catch (ignored: Exception) {
        null
    }
}

fun Context.getSomeAndroidSAFDocument(path: String): DocumentFile? = getFastAndroidSAFDocument(path) ?: getAndroidSAFDocument(path)

fun Context.getFastAndroidSAFDocument(path: String): DocumentFile? {
    val treeUri = getAndroidTreeUri(path)
    if (treeUri.isEmpty()) {
        return null
    }

    val uri = getAndroidSAFUri(path)
    return DocumentFile.fromSingleUri(this, uri)
}

fun Context.getAndroidSAFChildrenUri(path: String): Uri {
    val treeUri = getAndroidTreeUri(path).toUri()
    val documentId = createAndroidSAFDocumentId(path)
    return DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
}

fun Context.createAndroidSAFDirectory(path: String): Boolean {
    return try {
        val treeUri = getAndroidTreeUri(path).toUri()
        val parentPath = path.getParentPath()
        if (!getDoesFilePathExist(parentPath)) {
            createAndroidSAFDirectory(parentPath)
        }
        val documentId = createAndroidSAFDocumentId(parentPath)
        val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
        DocumentsContract.createDocument(contentResolver, parentUri, Document.MIME_TYPE_DIR, path.getFilenameFromPath()) != null
    } catch (e: IllegalStateException) {
        showErrorToast(e)
        false
    }
}

fun Context.createAndroidSAFFile(path: String): Boolean {
    return try {
        val treeUri = getAndroidTreeUri(path).toUri()
        val parentPath = path.getParentPath()
        if (!getDoesFilePathExist(parentPath)) {
            createAndroidSAFDirectory(parentPath)
        }

        val documentId = createAndroidSAFDocumentId(path.getParentPath())
        val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
        DocumentsContract.createDocument(contentResolver, parentUri, path.getMimeType(), path.getFilenameFromPath()) != null
    } catch (e: IllegalStateException) {
        showErrorToast(e)
        false
    }
}

fun Context.renameAndroidSAFDocument(oldPath: String, newPath: String): Boolean {
    return try {
        val treeUri = getAndroidTreeUri(oldPath).toUri()
        val documentId = createAndroidSAFDocumentId(oldPath)
        val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
        DocumentsContract.renameDocument(contentResolver, parentUri, newPath.getFilenameFromPath()) != null
    } catch (e: IllegalStateException) {
        showErrorToast(e)
        false
    }
}

fun Context.getAndroidSAFFileSize(path: String): Long {
    val treeUri = getAndroidTreeUri(path).toUri()
    val documentId = createAndroidSAFDocumentId(path)
    return getFileSize(treeUri, documentId)
}

fun Context.getAndroidSAFFileCount(path: String, countHidden: Boolean): Int {
    val treeUri = getAndroidTreeUri(path).toUri()
    if (treeUri == Uri.EMPTY) {
        return 0
    }

    val documentId = createAndroidSAFDocumentId(path)
    val rootDocId = getStorageRootIdForAndroidDir(path)
    return getProperChildrenCount(rootDocId, treeUri, documentId, countHidden)
}

fun Context.getAndroidSAFDirectChildrenCount(path: String, countHidden: Boolean): Int {
    val treeUri = getAndroidTreeUri(path).toUri()
    if (treeUri == Uri.EMPTY) {
        return 0
    }

    val documentId = createAndroidSAFDocumentId(path)
    val rootDocId = getStorageRootIdForAndroidDir(path)
    return getDirectChildrenCount(rootDocId, treeUri, documentId, countHidden)
}

fun Context.getAndroidSAFLastModified(path: String): Long {
    val treeUri = getAndroidTreeUri(path).toUri()
    if (treeUri == Uri.EMPTY) {
        return 0L
    }

    val documentId = createAndroidSAFDocumentId(path)
    val projection = arrayOf(Document.COLUMN_LAST_MODIFIED)
    val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
    return contentResolver.query(documentUri, projection, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            cursor.getLongValue(Document.COLUMN_LAST_MODIFIED)
        } else {
            0L
        }
    } ?: 0L
}

fun Context.deleteAndroidSAFDirectory(path: String, allowDeleteFolder: Boolean = false, callback: ((wasSuccess: Boolean) -> Unit)? = null) {
    val treeUri = getAndroidTreeUri(path).toUri()
    val documentId = createAndroidSAFDocumentId(path)
    try {
        val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
        val document = DocumentFile.fromSingleUri(this, uri)
        val fileDeleted = (document!!.isFile || allowDeleteFolder) && DocumentsContract.deleteDocument(applicationContext.contentResolver, document.uri)
        callback?.invoke(fileDeleted)
    } catch (e: Exception) {
        showErrorToast(e)
        callback?.invoke(false)
        storeAndroidTreeUri(path, "")
    }
}

fun Context.trySAFFileDelete(fileDirItem: FileDirItem, allowDeleteFolder: Boolean = false, callback: ((wasSuccess: Boolean) -> Unit)? = null) {
    var fileDeleted = tryFastDocumentDelete(fileDirItem.path, allowDeleteFolder)
    if (!fileDeleted) {
        val document = getDocumentFile(fileDirItem.path)
        if (document != null && (fileDirItem.isDirectory == document.isDirectory)) {
            try {
                fileDeleted = (document.isFile || allowDeleteFolder) && DocumentsContract.deleteDocument(applicationContext.contentResolver, document.uri)
            } catch (ignored: Exception) {
                baseConfig.sdTreeUri = ""
                baseConfig.sdCardPath = ""
            }
        }
    }

    if (fileDeleted) {
        deleteFromMediaStore(fileDirItem.path)
        callback?.invoke(true)
    }
}

fun Context.getFileInputStreamSync(path: String): InputStream? {
    return when {
        isRestrictedSAFOnlyRoot(path) -> {
            val uri = getAndroidSAFUri(path)
            applicationContext.contentResolver.openInputStream(uri)
        }
        isAccessibleWithSAFSdk30(path) -> {
            try {
                FileInputStream(File(path))
            } catch (e: Exception) {
                val uri = createDocumentUriUsingFirstParentTreeUri(path)
                applicationContext.contentResolver.openInputStream(uri)
            }
        }
        isPathOnOTG(path) -> {
            val fileDocument = getSomeDocumentFile(path)
            applicationContext.contentResolver.openInputStream(fileDocument?.uri!!)
        }
        else -> FileInputStream(File(path))
    }
}

fun Context.updateOTGPathFromPartition() {
    val otgPath = "/storage/${baseConfig.OTGPartition}"
    baseConfig.OTGPath = if (getOTGFastDocumentFile(otgPath, otgPath)?.exists() == true) {
        "/storage/${baseConfig.OTGPartition}"
    } else {
        "/mnt/media_rw/${baseConfig.OTGPartition}"
    }
}

fun Context.getDoesFilePathExist(path: String, otgPathToUse: String? = null): Boolean {
    val otgPath = otgPathToUse ?: baseConfig.OTGPath
    return when {
        isRestrictedSAFOnlyRoot(path) -> getFastAndroidSAFDocument(path)?.exists() ?: false
        otgPath.isNotEmpty() && path.startsWith(otgPath) -> getOTGFastDocumentFile(path)?.exists() ?: false
        else -> File(path).exists()
    }
}

fun Context.getIsPathDirectory(path: String): Boolean {
    return when {
        isRestrictedSAFOnlyRoot(path) -> getFastAndroidSAFDocument(path)?.isDirectory ?: false
        isPathOnOTG(path) -> getOTGFastDocumentFile(path)?.isDirectory ?: false
        else -> File(path).isDirectory
    }
}

fun Context.getFolderLastModifieds(folder: String): HashMap<String, Long> {
    val lastModifieds = HashMap<String, Long>()
    val projection = arrayOf(
        Images.Media.DISPLAY_NAME,
        Images.Media.DATE_MODIFIED
    )

    val uri = Files.getContentUri("external")
    val selection = "${Images.Media.DATA} LIKE ? AND ${Images.Media.DATA} NOT LIKE ? AND ${Images.Media.MIME_TYPE} IS NOT NULL" // avoid selecting folders
    val selectionArgs = arrayOf("$folder/%", "$folder/%/%")

    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                do {
                    try {
                        val lastModified = cursor.getLongValue(Images.Media.DATE_MODIFIED) * 1000
                        if (lastModified != 0L) {
                            val name = cursor.getStringValue(Images.Media.DISPLAY_NAME)
                            lastModifieds["$folder/$name"] = lastModified
                        }
                    } catch (e: Exception) {
                    }
                } while (cursor.moveToNext())
            }
        }
    } catch (e: Exception) {
    }

    return lastModifieds
}

// avoid these being set as SD card paths
private val physicalPaths = arrayListOf(
    "/storage/sdcard1", // Motorola Xoom
    "/storage/extsdcard", // Samsung SGS3
    "/storage/sdcard0/external_sdcard", // User request
    "/mnt/extsdcard", "/mnt/sdcard/external_sd", // Samsung galaxy family
    "/mnt/external_sd", "/mnt/media_rw/sdcard1", // 4.4.2 on CyanogenMod S3
    "/removable/microsd", // Asus transformer prime
    "/mnt/emmc", "/storage/external_SD", // LG
    "/storage/ext_sd", // HTC One Max
    "/storage/removable/sdcard1", // Sony Xperia Z1
    "/data/sdext", "/data/sdext2", "/data/sdext3", "/data/sdext4", "/sdcard1", // Sony Xperia Z
    "/sdcard2", // HTC One M8s
    "/storage/usbdisk0",
    "/storage/usbdisk1",
    "/storage/usbdisk2"
)

// Convert paths like /storage/emulated/0/Pictures/Screenshots/first.jpg to content://media/external/images/media/131799
// so that we can refer to the file in the MediaStore.
// If we found no mediastore uri for a given file, do not return its path either to avoid some mismatching
fun Context.getUrisPathsFromFileDirItems(fileDirItems: List<FileDirItem>): Pair<ArrayList<String>, ArrayList<Uri>> {
    val fileUris = ArrayList<Uri>()
    val successfulFilePaths = ArrayList<String>()
    val allIds = getMediaStoreIds(this)
    val filePaths = fileDirItems.map { it.path }
    filePaths.forEach { path ->
        for ((filePath, mediaStoreId) in allIds) {
            if (filePath.lowercase() == path.lowercase()) {
                val baseUri = getFileUri(filePath)
                val uri = ContentUris.withAppendedId(baseUri, mediaStoreId)
                fileUris.add(uri)
                successfulFilePaths.add(path)
            }
        }
    }

    return Pair(successfulFilePaths, fileUris)
}

fun getMediaStoreIds(context: Context): HashMap<String, Long> {
    val ids = HashMap<String, Long>()
    val projection = arrayOf(
        Images.Media.DATA,
        Images.Media._ID
    )

    val uri = Files.getContentUri("external")

    try {
        context.queryCursor(uri, projection) { cursor ->
            try {
                val id = cursor.getLongValue(Images.Media._ID)
                if (id != 0L) {
                    val path = cursor.getStringValue(Images.Media.DATA)
                    ids[path] = id
                }
            } catch (e: Exception) {
            }
        }
    } catch (e: Exception) {
    }

    return ids
}

fun Context.getFileUrisFromFileDirItems(fileDirItems: List<FileDirItem>): List<Uri> {
    val fileUris = getUrisPathsFromFileDirItems(fileDirItems).second
    if (fileUris.isEmpty()) {
        fileDirItems.map { fileDirItem ->
            fileUris.add(fileDirItem.assembleContentUri())
        }
    }

    return fileUris
}
