package com.simplemobiletools.gallery.pro.dialogs

import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.showKeyboard
import com.simplemobiletools.commons.extensions.value
import com.simplemobiletools.gallery.pro.R
import kotlinx.android.synthetic.main.dialog_custom_aspect_ratio.view.*

class CustomAspectRatioDialog(val activity: BaseSimpleActivity, val defaultCustomAspectRatio: Pair<Float, Float>?, val callback: (aspectRatio: Pair<Float, Float>) -> Unit) {
    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_custom_aspect_ratio, null).apply {
            aspect_ratio_width.setText(defaultCustomAspectRatio?.first?.toInt()?.toString() ?: "")
            aspect_ratio_height.setText(defaultCustomAspectRatio?.second?.toInt()?.toString() ?: "")
        }

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
                    activity.setupDialogStuff(view, this) {
                        showKeyboard(view.aspect_ratio_width)
                        getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            val width = getViewValue(view.aspect_ratio_width)
                            val height = getViewValue(view.aspect_ratio_height)
                            callback(Pair(width, height))
                            dismiss()
                        }
                    }
                }
    }

    private fun getViewValue(view: EditText): Float {
        val textValue = view.value
        return if (textValue.isEmpty()) 0f else textValue.toFloat()
    }
}
