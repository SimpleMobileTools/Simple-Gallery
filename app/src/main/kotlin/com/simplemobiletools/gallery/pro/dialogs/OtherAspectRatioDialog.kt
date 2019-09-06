package com.simplemobiletools.gallery.pro.dialogs

import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.gallery.pro.R
import kotlinx.android.synthetic.main.dialog_other_aspect_ratio.view.*

class OtherAspectRatioDialog(val activity: BaseSimpleActivity, val lastOtherAspectRatio: Pair<Float, Float>?, val callback: (aspectRatio: Pair<Float, Float>) -> Unit) {
    private val dialog: AlertDialog

    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_other_aspect_ratio, null).apply {
            other_aspect_ratio_2_1.setOnClickListener { ratioPicked(Pair(2f, 1f)) }
            other_aspect_ratio_3_2.setOnClickListener { ratioPicked(Pair(3f, 2f)) }
            other_aspect_ratio_4_3.setOnClickListener { ratioPicked(Pair(4f, 3f)) }
            other_aspect_ratio_5_3.setOnClickListener { ratioPicked(Pair(5f, 3f)) }
            other_aspect_ratio_16_9.setOnClickListener { ratioPicked(Pair(16f, 9f)) }
            other_aspect_ratio_19_9.setOnClickListener { ratioPicked(Pair(19f, 9f)) }
            other_aspect_ratio_custom.setOnClickListener { customRatioPicked() }

            other_aspect_ratio_1_2.setOnClickListener { ratioPicked(Pair(1f, 2f)) }
            other_aspect_ratio_2_3.setOnClickListener { ratioPicked(Pair(2f, 3f)) }
            other_aspect_ratio_3_4.setOnClickListener { ratioPicked(Pair(3f, 4f)) }
            other_aspect_ratio_3_5.setOnClickListener { ratioPicked(Pair(3f, 5f)) }
            other_aspect_ratio_9_16.setOnClickListener { ratioPicked(Pair(9f, 16f)) }
            other_aspect_ratio_9_19.setOnClickListener { ratioPicked(Pair(9f, 19f)) }

            val radio1SelectedItemId = when (lastOtherAspectRatio) {
                Pair(2f, 1f) -> other_aspect_ratio_2_1.id
                Pair(3f, 2f) -> other_aspect_ratio_3_2.id
                Pair(4f, 3f) -> other_aspect_ratio_4_3.id
                Pair(5f, 3f) -> other_aspect_ratio_5_3.id
                Pair(16f, 9f) -> other_aspect_ratio_16_9.id
                Pair(19f, 9f) -> other_aspect_ratio_19_9.id
                else -> 0
            }
            other_aspect_ratio_dialog_radio_1.check(radio1SelectedItemId)

            val radio2SelectedItemId = when (lastOtherAspectRatio) {
                Pair(1f, 2f) -> other_aspect_ratio_1_2.id
                Pair(2f, 3f) -> other_aspect_ratio_2_3.id
                Pair(3f, 4f) -> other_aspect_ratio_3_4.id
                Pair(3f, 5f) -> other_aspect_ratio_3_5.id
                Pair(9f, 16f) -> other_aspect_ratio_9_16.id
                Pair(9f, 19f) -> other_aspect_ratio_9_19.id
                else -> 0
            }
            other_aspect_ratio_dialog_radio_2.check(radio2SelectedItemId)

            if (radio1SelectedItemId == 0 && radio2SelectedItemId == 0) {
                other_aspect_ratio_dialog_radio_1.check(other_aspect_ratio_custom.id)
            }
        }

        dialog = AlertDialog.Builder(activity)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
                    activity.setupDialogStuff(view, this)
                }
    }

    private fun customRatioPicked() {
        CustomAspectRatioDialog(activity, lastOtherAspectRatio) {
            callback(it)
            dialog.dismiss()
        }
    }

    private fun ratioPicked(pair: Pair<Float, Float>) {
        callback(pair)
        dialog.dismiss()
    }
}
