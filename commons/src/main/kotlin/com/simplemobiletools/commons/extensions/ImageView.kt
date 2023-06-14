package com.simplemobiletools.commons.extensions

import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.widget.ImageView
import androidx.annotation.DrawableRes

fun ImageView.setFillWithStroke(fillColor: Int, backgroundColor: Int, drawRectangle: Boolean = false) {
    GradientDrawable().apply {
        shape = if (drawRectangle) GradientDrawable.RECTANGLE else GradientDrawable.OVAL
        setColor(fillColor)
        background = this

        if (backgroundColor == fillColor || fillColor == -2 && backgroundColor == -1) {
            val strokeColor = backgroundColor.getContrastColor().adjustAlpha(0.5f)
            setStroke(2, strokeColor)
        }
    }
}

fun ImageView.applyColorFilter(color: Int) = setColorFilter(color, PorterDuff.Mode.SRC_IN)

fun ImageView.setImageResourceOrBeGone(@DrawableRes imageRes: Int?) {
    if (imageRes != null) {
        beVisible()
        setImageResource(imageRes)
    } else {
        beGone()
    }
}
