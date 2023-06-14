package com.simplemobiletools.commons.extensions

import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText

// in dialogs, lets use findViewById, because while some dialogs use MyEditText, material theme dialogs use TextInputEditText so the system takes care of it
fun AlertDialog.showKeyboard(editText: AppCompatEditText) {
    window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    editText.apply {
        requestFocus()
        onGlobalLayout {
            setSelection(text.toString().length)
        }
    }
}

fun AlertDialog.hideKeyboard() {
    window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
}
