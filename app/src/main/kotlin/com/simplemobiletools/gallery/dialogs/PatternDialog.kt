package com.simplemobiletools.gallery.dialogs

import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.SimpleActivity
import com.simplemobiletools.gallery.adapters.PasswordTypesAdapter
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.views.MyDialogViewPager
import com.simplemobiletools.gallery.views.PatternTab
import kotlinx.android.synthetic.main.dialog_pattern.view.*

class PatternDialog(val activity: SimpleActivity, val requiredHash: String, val callback: (hash: String) -> Unit) : PatternTab.HashListener {
    var dialog: AlertDialog? = null
    val view = LayoutInflater.from(activity).inflate(R.layout.dialog_pattern, null)

    init {
        view.apply {
            val textColor = context.config.textColor
            dialog_tab_layout.setTabTextColors(textColor, textColor)

            val viewPager = findViewById(R.id.dialog_tab_view_pager) as MyDialogViewPager
            viewPager.adapter = PasswordTypesAdapter(context, requiredHash, this@PatternDialog)
        }

        dialog = AlertDialog.Builder(activity)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            activity.setupDialogStuff(view, this)
        }
    }

    override fun receivedHash(hash: String) {
        callback(hash)
        dialog!!.dismiss()
    }
}
