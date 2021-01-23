package com.simplemobiletools.gallery.pro.extensions

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue

fun Resources.getActionBarHeight(context: Context): Int {
    val tv = TypedValue()
    return if (context.theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
        TypedValue.complexToDimensionPixelSize(tv.data, displayMetrics)
    } else
        0
}

fun Resources.getStatusBarHeight(): Int {
    val id = getIdentifier("status_bar_height", "dimen", "android")
    return if (id > 0) {
        getDimensionPixelSize(id)
    } else
        0
}

fun Resources.getNavBarHeight(): Int {
    val id = getIdentifier("navigation_bar_height", "dimen", "android")
    return if (id > 0) {
        getDimensionPixelSize(id)
    } else
        0
}
