package com.simplemobiletools.commons.helpers

import android.content.ContentValues
import android.net.Uri
import com.simplemobiletools.commons.models.SharedTheme

class MyContentProvider {
    companion object {
        private const val AUTHORITY = "com.simplemobiletools.commons.provider"
        const val SHARED_THEME_ACTIVATED = "com.simplemobiletools.commons.SHARED_THEME_ACTIVATED"
        const val SHARED_THEME_UPDATED = "com.simplemobiletools.commons.SHARED_THEME_UPDATED"
        val MY_CONTENT_URI = Uri.parse("content://$AUTHORITY/themes")

        const val COL_ID = "_id"    // used in Simple Thank You
        const val COL_TEXT_COLOR = "text_color"
        const val COL_BACKGROUND_COLOR = "background_color"
        const val COL_PRIMARY_COLOR = "primary_color"
        const val COL_ACCENT_COLOR = "accent_color"
        const val COL_APP_ICON_COLOR = "app_icon_color"
        const val COL_LAST_UPDATED_TS = "last_updated_ts"

        fun fillThemeContentValues(sharedTheme: SharedTheme) = ContentValues().apply {
            put(COL_TEXT_COLOR, sharedTheme.textColor)
            put(COL_BACKGROUND_COLOR, sharedTheme.backgroundColor)
            put(COL_PRIMARY_COLOR, sharedTheme.primaryColor)
            put(COL_ACCENT_COLOR, sharedTheme.accentColor)
            put(COL_APP_ICON_COLOR, sharedTheme.appIconColor)
            put(COL_LAST_UPDATED_TS, System.currentTimeMillis() / 1000)
        }
    }
}
