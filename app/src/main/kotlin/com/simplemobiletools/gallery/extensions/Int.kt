package com.simplemobiletools.gallery.extensions

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import com.simplemobiletools.commons.extensions.adjustAlpha

fun Int.createSelector(): StateListDrawable {
    val statelist = StateListDrawable()
    val selectedDrawable = ColorDrawable(adjustAlpha(0.5f))
    statelist.addState(intArrayOf(android.R.attr.state_selected), selectedDrawable)

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
        val pressedDrawable = ColorDrawable(adjustAlpha(0.2f))
        statelist.addState(intArrayOf(android.R.attr.state_pressed), pressedDrawable)
    } else {
        val pressedDrawable = RippleDrawable(ColorStateList.valueOf(adjustAlpha(0.2f)), null, ColorDrawable(Color.WHITE))
        statelist.addState(intArrayOf(), pressedDrawable)
    }
    return statelist
}
