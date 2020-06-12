package com.simplemobiletools.gallery.pro.dialogs

import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.extensions.config
import com.simplemobiletools.gallery.pro.helpers.*
import kotlinx.android.synthetic.main.dialog_manage_bottom_actions.view.*

class ManageBottomActionsDialog(val activity: BaseSimpleActivity, val callback: (result: Int) -> Unit) {
    private var view = activity.layoutInflater.inflate(R.layout.dialog_manage_bottom_actions, null)

    init {
        val actions = activity.config.visibleBottomActions
        view.apply {
            manage_bottom_actions_toggle_favorite.isChecked = actions and BOTTOM_ACTION_TOGGLE_FAVORITE != 0
            manage_bottom_actions_edit.isChecked = actions and BOTTOM_ACTION_EDIT != 0
            manage_bottom_actions_share.isChecked = actions and BOTTOM_ACTION_SHARE != 0
            manage_bottom_actions_delete.isChecked = actions and BOTTOM_ACTION_DELETE != 0
            manage_bottom_actions_rotate.isChecked = actions and BOTTOM_ACTION_ROTATE != 0
            manage_bottom_actions_properties.isChecked = actions and BOTTOM_ACTION_PROPERTIES != 0
            manage_bottom_actions_change_orientation.isChecked = actions and BOTTOM_ACTION_CHANGE_ORIENTATION != 0
            manage_bottom_actions_slideshow.isChecked = actions and BOTTOM_ACTION_SLIDESHOW != 0
            manage_bottom_actions_show_on_map.isChecked = actions and BOTTOM_ACTION_SHOW_ON_MAP != 0
            manage_bottom_actions_toggle_visibility.isChecked = actions and BOTTOM_ACTION_TOGGLE_VISIBILITY != 0
            manage_bottom_actions_rename.isChecked = actions and BOTTOM_ACTION_RENAME != 0
            manage_bottom_actions_set_as.isChecked = actions and BOTTOM_ACTION_SET_AS != 0
            manage_bottom_actions_copy.isChecked = actions and BOTTOM_ACTION_COPY != 0
            manage_bottom_actions_move.isChecked = actions and BOTTOM_ACTION_MOVE != 0
            manage_bottom_actions_resize.isChecked = actions and BOTTOM_ACTION_RESIZE != 0
        }

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok) { dialog, which -> dialogConfirmed() }
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
                    activity.setupDialogStuff(view, this)
                }
    }

    private fun dialogConfirmed() {
        var result = 0
        view.apply {
            if (manage_bottom_actions_toggle_favorite.isChecked)
                result += BOTTOM_ACTION_TOGGLE_FAVORITE
            if (manage_bottom_actions_edit.isChecked)
                result += BOTTOM_ACTION_EDIT
            if (manage_bottom_actions_share.isChecked)
                result += BOTTOM_ACTION_SHARE
            if (manage_bottom_actions_delete.isChecked)
                result += BOTTOM_ACTION_DELETE
            if (manage_bottom_actions_rotate.isChecked)
                result += BOTTOM_ACTION_ROTATE
            if (manage_bottom_actions_properties.isChecked)
                result += BOTTOM_ACTION_PROPERTIES
            if (manage_bottom_actions_change_orientation.isChecked)
                result += BOTTOM_ACTION_CHANGE_ORIENTATION
            if (manage_bottom_actions_slideshow.isChecked)
                result += BOTTOM_ACTION_SLIDESHOW
            if (manage_bottom_actions_show_on_map.isChecked)
                result += BOTTOM_ACTION_SHOW_ON_MAP
            if (manage_bottom_actions_toggle_visibility.isChecked)
                result += BOTTOM_ACTION_TOGGLE_VISIBILITY
            if (manage_bottom_actions_rename.isChecked)
                result += BOTTOM_ACTION_RENAME
            if (manage_bottom_actions_set_as.isChecked)
                result += BOTTOM_ACTION_SET_AS
            if (manage_bottom_actions_copy.isChecked)
                result += BOTTOM_ACTION_COPY
            if (manage_bottom_actions_move.isChecked)
                result += BOTTOM_ACTION_MOVE
            if (manage_bottom_actions_resize.isChecked)
                result += BOTTOM_ACTION_RESIZE
        }

        activity.config.visibleBottomActions = result
        callback(result)
    }
}
