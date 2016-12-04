package com.simplemobiletools.gallery.extensions

import android.database.Cursor

fun Cursor.getStringValue(key: String) = getString(getColumnIndex(key))

fun Cursor.getLongValue(key: String) = getLong(getColumnIndex(key))
