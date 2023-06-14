package com.simplemobiletools.commons.extensions

import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.BackgroundColorSpan
import android.widget.EditText
import android.widget.TextView
import androidx.core.graphics.ColorUtils

val EditText.value: String get() = text.toString().trim()

fun EditText.onTextChangeListener(onTextChangedAction: (newText: String) -> Unit) = addTextChangedListener(object : TextWatcher {
    override fun afterTextChanged(s: Editable?) {
        onTextChangedAction(s.toString())
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
})

fun EditText.highlightText(highlightText: String, color: Int) {
    val content = text.toString()
    var indexOf = content.indexOf(highlightText, 0, true)
    val wordToSpan = SpannableString(text)
    var offset = 0

    while (offset < content.length && indexOf != -1) {
        indexOf = content.indexOf(highlightText, offset, true)

        if (indexOf == -1) {
            break
        } else {
            val spanBgColor = BackgroundColorSpan(ColorUtils.setAlphaComponent(color, 128))
            val spanFlag = Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            wordToSpan.setSpan(spanBgColor, indexOf, indexOf + highlightText.length, spanFlag)
            setText(wordToSpan, TextView.BufferType.SPANNABLE)
        }

        offset = indexOf + 1
    }
}
