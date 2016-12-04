package com.simplemobiletools.gallery

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue

class Utils {
    companion object {
        fun getActionBarHeight(context: Context, res: Resources): Int {
            val tv = TypedValue()
            var height = 0
            if (context.theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
                height = TypedValue.complexToDimensionPixelSize(tv.data, res.displayMetrics)
            }
            return height
        }

        fun getStatusBarHeight(res: Resources): Int {
            val id = res.getIdentifier("status_bar_height", "dimen", "android")
            return if (id > 0) {
                res.getDimensionPixelSize(id)
            } else
                0
        }

        fun getNavBarHeight(res: Resources): Int {
            val id = res.getIdentifier("navigation_bar_height", "dimen", "android")
            return if (id > 0) {
                res.getDimensionPixelSize(id)
            } else
                0
        }
    }
}
