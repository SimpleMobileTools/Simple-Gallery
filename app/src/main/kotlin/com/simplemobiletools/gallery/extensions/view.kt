package com.simplemobiletools.gallery.extensions

import android.view.View

fun View.beVisibleIf(beVisible: Boolean) = if (beVisible) visibility = View.VISIBLE else visibility = View.GONE
