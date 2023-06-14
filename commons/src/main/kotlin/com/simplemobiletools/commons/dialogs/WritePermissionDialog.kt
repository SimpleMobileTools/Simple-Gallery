package com.simplemobiletools.commons.dialogs

import android.app.Activity
import android.text.Html
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.getAlertDialogBuilder
import com.simplemobiletools.commons.extensions.humanizePath
import com.simplemobiletools.commons.extensions.setupDialogStuff
import kotlinx.android.synthetic.main.dialog_write_permission.view.*
import kotlinx.android.synthetic.main.dialog_write_permission_otg.view.*

class WritePermissionDialog(activity: Activity, val mode: Mode, val callback: () -> Unit) {
    sealed class Mode {
        object Otg : Mode()
        object SdCard : Mode()
        data class OpenDocumentTreeSDK30(val path: String) : Mode()
        object CreateDocumentSDK30 : Mode()
    }

    private var dialog: AlertDialog? = null

    init {
        val layout = if (mode == Mode.SdCard) R.layout.dialog_write_permission else R.layout.dialog_write_permission_otg
        val view = activity.layoutInflater.inflate(layout, null)
        var dialogTitle = R.string.confirm_storage_access_title

        val glide = Glide.with(activity)
        val crossFade = DrawableTransitionOptions.withCrossFade()
        when (mode) {
            Mode.Otg -> {
                view.write_permissions_dialog_otg_text.setText(R.string.confirm_usb_storage_access_text)
                glide.load(R.drawable.img_write_storage_otg).transition(crossFade).into(view.write_permissions_dialog_otg_image)
            }
            Mode.SdCard -> {
                glide.load(R.drawable.img_write_storage).transition(crossFade).into(view.write_permissions_dialog_image)
                glide.load(R.drawable.img_write_storage_sd).transition(crossFade).into(view.write_permissions_dialog_image_sd)
            }
            is Mode.OpenDocumentTreeSDK30 -> {
                dialogTitle = R.string.confirm_folder_access_title
                val humanizedPath = activity.humanizePath(mode.path)
                view.write_permissions_dialog_otg_text.text =
                    Html.fromHtml(activity.getString(R.string.confirm_storage_access_android_text_specific, humanizedPath))
                glide.load(R.drawable.img_write_storage_sdk_30).transition(crossFade).into(view.write_permissions_dialog_otg_image)

                view.write_permissions_dialog_otg_image.setOnClickListener {
                    dialogConfirmed()
                }
            }
            Mode.CreateDocumentSDK30 -> {
                dialogTitle = R.string.confirm_folder_access_title
                view.write_permissions_dialog_otg_text.text = Html.fromHtml(activity.getString(R.string.confirm_create_doc_for_new_folder_text))
                glide.load(R.drawable.img_write_storage_create_doc_sdk_30).transition(crossFade).into(view.write_permissions_dialog_otg_image)

                view.write_permissions_dialog_otg_image.setOnClickListener {
                    dialogConfirmed()
                }
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { dialog, which -> dialogConfirmed() }
            .setOnCancelListener {
                BaseSimpleActivity.funAfterSAFPermission?.invoke(false)
                BaseSimpleActivity.funAfterSAFPermission = null
            }
            .apply {
                activity.setupDialogStuff(view, this, dialogTitle) { alertDialog ->
                    dialog = alertDialog
                }
            }
    }

    private fun dialogConfirmed() {
        dialog?.dismiss()
        callback()
    }
}
