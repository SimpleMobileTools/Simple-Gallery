package com.simplemobiletools.commons.dialogs

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import kotlinx.android.synthetic.main.dialog_radio_group.view.*

/**
 * A dialog for choosing between internal, root, SD card (optional) storage
 *
 * @param activity has to be activity to avoid some Theme.AppCompat issues
 * @param currPath current path to decide which storage should be preselected
 * @param pickSingleOption if only one option like "Internal" is available, select it automatically
 * @param callback an anonymous function
 *
 */
class StoragePickerDialog(
    val activity: BaseSimpleActivity, val currPath: String, val showRoot: Boolean, pickSingleOption: Boolean,
    val callback: (pickedPath: String) -> Unit
) {
    private val ID_INTERNAL = 1
    private val ID_SD = 2
    private val ID_OTG = 3
    private val ID_ROOT = 4

    private lateinit var radioGroup: RadioGroup
    private var dialog: AlertDialog? = null
    private var defaultSelectedId = 0
    private val availableStorages = ArrayList<String>()

    init {
        availableStorages.add(activity.internalStoragePath)
        when {
            activity.hasExternalSDCard() -> availableStorages.add(activity.sdCardPath)
            activity.hasOTGConnected() -> availableStorages.add("otg")
            showRoot -> availableStorages.add("root")
        }

        if (pickSingleOption && availableStorages.size == 1) {
            callback(availableStorages.first())
        } else {
            initDialog()
        }
    }

    private fun initDialog() {
        val inflater = LayoutInflater.from(activity)
        val resources = activity.resources
        val layoutParams = RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val view = inflater.inflate(R.layout.dialog_radio_group, null)
        radioGroup = view.dialog_radio_group
        val basePath = currPath.getBasePath(activity)

        val internalButton = inflater.inflate(R.layout.radio_button, null) as RadioButton
        internalButton.apply {
            id = ID_INTERNAL
            text = resources.getString(R.string.internal)
            isChecked = basePath == context.internalStoragePath
            setOnClickListener { internalPicked() }
            if (isChecked) {
                defaultSelectedId = id
            }
        }
        radioGroup.addView(internalButton, layoutParams)

        if (activity.hasExternalSDCard()) {
            val sdButton = inflater.inflate(R.layout.radio_button, null) as RadioButton
            sdButton.apply {
                id = ID_SD
                text = resources.getString(R.string.sd_card)
                isChecked = basePath == context.sdCardPath
                setOnClickListener { sdPicked() }
                if (isChecked) {
                    defaultSelectedId = id
                }
            }
            radioGroup.addView(sdButton, layoutParams)
        }

        if (activity.hasOTGConnected()) {
            val otgButton = inflater.inflate(R.layout.radio_button, null) as RadioButton
            otgButton.apply {
                id = ID_OTG
                text = resources.getString(R.string.usb)
                isChecked = basePath == context.otgPath
                setOnClickListener { otgPicked() }
                if (isChecked) {
                    defaultSelectedId = id
                }
            }
            radioGroup.addView(otgButton, layoutParams)
        }

        // allow for example excluding the root folder at the gallery
        if (showRoot) {
            val rootButton = inflater.inflate(R.layout.radio_button, null) as RadioButton
            rootButton.apply {
                id = ID_ROOT
                text = resources.getString(R.string.root)
                isChecked = basePath == "/"
                setOnClickListener { rootPicked() }
                if (isChecked) {
                    defaultSelectedId = id
                }
            }
            radioGroup.addView(rootButton, layoutParams)
        }

        activity.getAlertDialogBuilder().apply {
            activity.setupDialogStuff(view, this, R.string.select_storage) { alertDialog ->
                dialog = alertDialog
            }
        }
    }

    private fun internalPicked() {
        dialog?.dismiss()
        callback(activity.internalStoragePath)
    }

    private fun sdPicked() {
        dialog?.dismiss()
        callback(activity.sdCardPath)
    }

    private fun otgPicked() {
        activity.handleOTGPermission {
            if (it) {
                callback(activity.otgPath)
                dialog?.dismiss()
            } else {
                radioGroup.check(defaultSelectedId)
            }
        }
    }

    private fun rootPicked() {
        dialog?.dismiss()
        callback("/")
    }
}
