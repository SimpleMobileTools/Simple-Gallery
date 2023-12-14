package org.fossify.gallery.dialogs

import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.gallery.databinding.DialogManageBottomActionsBinding
import org.fossify.gallery.extensions.config
import org.fossify.gallery.helpers.*

class ManageBottomActionsDialog(val activity: BaseSimpleActivity, val callback: (result: Int) -> Unit) {
    private val binding = DialogManageBottomActionsBinding.inflate(activity.layoutInflater)

    init {
        val actions = activity.config.visibleBottomActions
        binding.apply {
            manageBottomActionsToggleFavorite.isChecked = actions and BOTTOM_ACTION_TOGGLE_FAVORITE != 0
            manageBottomActionsEdit.isChecked = actions and BOTTOM_ACTION_EDIT != 0
            manageBottomActionsShare.isChecked = actions and BOTTOM_ACTION_SHARE != 0
            manageBottomActionsDelete.isChecked = actions and BOTTOM_ACTION_DELETE != 0
            manageBottomActionsRotate.isChecked = actions and BOTTOM_ACTION_ROTATE != 0
            manageBottomActionsProperties.isChecked = actions and BOTTOM_ACTION_PROPERTIES != 0
            manageBottomActionsChangeOrientation.isChecked = actions and BOTTOM_ACTION_CHANGE_ORIENTATION != 0
            manageBottomActionsSlideshow.isChecked = actions and BOTTOM_ACTION_SLIDESHOW != 0
            manageBottomActionsShowOnMap.isChecked = actions and BOTTOM_ACTION_SHOW_ON_MAP != 0
            manageBottomActionsToggleVisibility.isChecked = actions and BOTTOM_ACTION_TOGGLE_VISIBILITY != 0
            manageBottomActionsRename.isChecked = actions and BOTTOM_ACTION_RENAME != 0
            manageBottomActionsSetAs.isChecked = actions and BOTTOM_ACTION_SET_AS != 0
            manageBottomActionsCopy.isChecked = actions and BOTTOM_ACTION_COPY != 0
            manageBottomActionsMove.isChecked = actions and BOTTOM_ACTION_MOVE != 0
            manageBottomActionsResize.isChecked = actions and BOTTOM_ACTION_RESIZE != 0
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok) { dialog, which -> dialogConfirmed() }
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this)
            }
    }

    private fun dialogConfirmed() {
        var result = 0
        binding.apply {
            if (manageBottomActionsToggleFavorite.isChecked)
                result += BOTTOM_ACTION_TOGGLE_FAVORITE
            if (manageBottomActionsEdit.isChecked)
                result += BOTTOM_ACTION_EDIT
            if (manageBottomActionsShare.isChecked)
                result += BOTTOM_ACTION_SHARE
            if (manageBottomActionsDelete.isChecked)
                result += BOTTOM_ACTION_DELETE
            if (manageBottomActionsRotate.isChecked)
                result += BOTTOM_ACTION_ROTATE
            if (manageBottomActionsProperties.isChecked)
                result += BOTTOM_ACTION_PROPERTIES
            if (manageBottomActionsChangeOrientation.isChecked)
                result += BOTTOM_ACTION_CHANGE_ORIENTATION
            if (manageBottomActionsSlideshow.isChecked)
                result += BOTTOM_ACTION_SLIDESHOW
            if (manageBottomActionsShowOnMap.isChecked)
                result += BOTTOM_ACTION_SHOW_ON_MAP
            if (manageBottomActionsToggleVisibility.isChecked)
                result += BOTTOM_ACTION_TOGGLE_VISIBILITY
            if (manageBottomActionsRename.isChecked)
                result += BOTTOM_ACTION_RENAME
            if (manageBottomActionsSetAs.isChecked)
                result += BOTTOM_ACTION_SET_AS
            if (manageBottomActionsCopy.isChecked)
                result += BOTTOM_ACTION_COPY
            if (manageBottomActionsMove.isChecked)
                result += BOTTOM_ACTION_MOVE
            if (manageBottomActionsResize.isChecked)
                result += BOTTOM_ACTION_RESIZE
        }

        activity.config.visibleBottomActions = result
        callback(result)
    }
}
