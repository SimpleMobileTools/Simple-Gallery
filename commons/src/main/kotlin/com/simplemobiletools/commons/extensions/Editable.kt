package com.simplemobiletools.commons.extensions

import android.text.Editable
import android.text.style.BackgroundColorSpan

fun Editable.clearBackgroundSpans() {
    val spans = getSpans(0, length, Any::class.java)
    for (span in spans) {
        if (span is BackgroundColorSpan) {
            removeSpan(span)
        }
    }
}
