package com.simplemobiletools.gallery.extensions

import android.widget.TextView

val TextView.value: String get() = this.text.toString().trim()
