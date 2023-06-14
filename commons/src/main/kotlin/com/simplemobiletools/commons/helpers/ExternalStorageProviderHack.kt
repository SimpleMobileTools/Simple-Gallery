package com.simplemobiletools.commons.helpers

import android.database.Cursor
import android.database.MatrixCursor
import android.database.MergeCursor
import android.net.Uri
import android.provider.DocumentsContract
import com.simplemobiletools.commons.extensions.getStringValue

// On Android 11, ExternalStorageProvider no longer returns Android/data and Android/obb as children
// of the Android directory on primary storage. However, the two child directories are actually
// still accessible.
// Inspired by: https://github.com/zhanghai/MaterialFiles/blob/master/app/src/main/java/me/zhanghai/android/files/provider/document/resolver/ExternalStorageProviderPrimaryAndroidDataHack.kt
object ExternalStorageProviderHack {
    private const val ANDROID_DATA_DISPLAY_NAME = "data"
    private const val ANDROID_OBB_DISPLAY_NAME = "obb"

    private val CHILD_DOCUMENTS_CURSOR_COLUMN_NAMES = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        DocumentsContract.Document.COLUMN_SIZE,
    )

    private fun getAndroidDocumentId(rootDocId: String): String {
        return "$rootDocId:Android"
    }

    private fun getAndroidDataDocumentId(rootDocId: String): String {
        return "${getAndroidDocumentId(rootDocId)}/data"
    }

    private fun getAndroidObbDocumentId(rootDocId: String): String {
        return "${getAndroidDocumentId(rootDocId)}/obb"
    }

    fun transformQueryResult(rootDocId: String, uri: Uri, cursor: Cursor): Cursor {
        val documentId = DocumentsContract.getDocumentId(uri)
        if (uri.authority == EXTERNAL_STORAGE_PROVIDER_AUTHORITY && documentId == getAndroidDocumentId(rootDocId)) {
            var hasDataRow = false
            var hasObbRow = false
            try {
                while (cursor.moveToNext()) {
                    when (cursor.getStringValue(DocumentsContract.Document.COLUMN_DOCUMENT_ID)) {
                        getAndroidDataDocumentId(rootDocId) ->
                            hasDataRow = true
                        getAndroidObbDocumentId(rootDocId) ->
                            hasObbRow = true
                    }
                    if (hasDataRow && hasObbRow) {
                        break
                    }
                }
            } finally {
                cursor.moveToPosition(-1)
            }

            if (hasDataRow && hasObbRow) {
                return cursor
            }

            val extraCursor = MatrixCursor(CHILD_DOCUMENTS_CURSOR_COLUMN_NAMES)
            if (!hasDataRow) {
                extraCursor.newRow()
                    .add(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        getAndroidDataDocumentId(rootDocId)
                    )
                    .add(
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        ANDROID_DATA_DISPLAY_NAME
                    )
                    .add(
                        DocumentsContract.Document.COLUMN_MIME_TYPE,
                        DocumentsContract.Document.MIME_TYPE_DIR
                    )
                    .add(
                        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                        System.currentTimeMillis()
                    )
                    .add(
                        DocumentsContract.Document.COLUMN_SIZE,
                        0L
                    )
            }

            if (!hasObbRow) {
                extraCursor.newRow()
                    .add(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        getAndroidObbDocumentId(rootDocId)
                    )
                    .add(
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        ANDROID_OBB_DISPLAY_NAME
                    )
                    .add(
                        DocumentsContract.Document.COLUMN_MIME_TYPE,
                        DocumentsContract.Document.MIME_TYPE_DIR
                    )
                    .add(
                        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                        System.currentTimeMillis()
                    )
                    .add(
                        DocumentsContract.Document.COLUMN_SIZE,
                        0L
                    )
            }
            return MergeCursor(arrayOf(cursor, extraCursor))
        }
        return cursor
    }
}
