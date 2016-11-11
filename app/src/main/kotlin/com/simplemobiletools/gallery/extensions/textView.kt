package com.simplemobiletools.gallery.extensions

import android.widget.TextView

val TextView.value: String get() = text.toString().trim()
