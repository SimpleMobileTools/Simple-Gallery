package com.simplemobiletools.commons.extensions

import android.database.Cursor

fun Cursor.getStringValue(key: String) = getString(getColumnIndex(key))

fun Cursor.getStringValueOrNull(key: String) = if (isNull(getColumnIndex(key))) null else getString(getColumnIndex(key))

fun Cursor.getIntValue(key: String) = getInt(getColumnIndex(key))

fun Cursor.getIntValueOrNull(key: String) = if (isNull(getColumnIndex(key))) null else getInt(getColumnIndex(key))

fun Cursor.getLongValue(key: String) = getLong(getColumnIndex(key))

fun Cursor.getLongValueOrNull(key: String) = if (isNull(getColumnIndex(key))) null else getLong(getColumnIndex(key))

fun Cursor.getBlobValue(key: String) = getBlob(getColumnIndex(key))
