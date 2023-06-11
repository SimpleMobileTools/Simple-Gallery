package com.simplemobiletools.gallery.pro.dialogs

import android.content.DialogInterface
import android.view.View
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.extensions.config
import com.simplemobiletools.gallery.pro.helpers.Config.Companion.SORT_BY_FAVORITE
import com.simplemobiletools.gallery.pro.helpers.SHOW_ALL
import kotlinx.android.synthetic.main.dialog_change_sorting.view.*

class ChangeSortingDialog(
    val activity: BaseSimpleActivity, val isDirectorySorting: Boolean, val showFolderCheckbox: Boolean,
    val path: String = "", val callback: () -> Unit
) :
    DialogInterface.OnClickListener {
    private var currSorting = 0
    private var config = activity.config
    private var pathToUse = if (!isDirectorySorting && path.isEmpty()) SHOW_ALL else path
    private var view: View

    init {
        currSorting = if (isDirectorySorting) config.directorySorting else config.getFolderSorting(pathToUse)
        view = activity.layoutInflater.inflate(R.layout.dialog_change_sorting, null).apply {
            use_for_this_folder_divider.beVisibleIf(showFolderCheckbox || (currSorting and SORT_BY_NAME != 0 || currSorting and SORT_BY_PATH != 0))

            sorting_dialog_numeric_sorting.beVisibleIf(showFolderCheckbox && (currSorting and SORT_BY_NAME != 0 || currSorting and SORT_BY_PATH != 0))
            sorting_dialog_numeric_sorting.isChecked = currSorting and SORT_USE_NUMERIC_VALUE != 0

            sorting_dialog_use_for_this_folder.beVisibleIf(showFolderCheckbox)
            sorting_dialog_use_for_this_folder.isChecked = config.hasCustomSorting(pathToUse)
            sorting_dialog_bottom_note.beVisibleIf(!isDirectorySorting)
            sorting_dialog_radio_custom.beVisibleIf(isDirectorySorting)
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, this)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this, R.string.sort_by)
            }

        setupSortRadio()
        setupOrderRadio()
    }

    private fun setupSortRadio() {
        val sortingRadio = view.sorting_dialog_radio_sorting
        sortingRadio.setOnCheckedChangeListener { group, checkedId ->
            val isSortingByNameOrPath = checkedId == sortingRadio.sorting_dialog_radio_name.id || checkedId == sortingRadio.sorting_dialog_radio_path.id
            view.sorting_dialog_numeric_sorting.beVisibleIf(isSortingByNameOrPath)
            view.use_for_this_folder_divider.beVisibleIf(view.sorting_dialog_numeric_sorting.isVisible() || view.sorting_dialog_use_for_this_folder.isVisible())

            val hideSortOrder = checkedId == sortingRadio.sorting_dialog_radio_custom.id || checkedId == sortingRadio.sorting_dialog_radio_random.id
            view.sorting_dialog_radio_order.beGoneIf(hideSortOrder)
            view.sorting_dialog_order_divider.beGoneIf(hideSortOrder)
        }

        val sortBtn = when {
            currSorting and SORT_BY_FAVORITE != 0 -> sortingRadio.sorting_dialog_radio_favorite
            currSorting and SORT_BY_PATH != 0 -> sortingRadio.sorting_dialog_radio_path
            currSorting and SORT_BY_SIZE != 0 -> sortingRadio.sorting_dialog_radio_size
            currSorting and SORT_BY_DATE_MODIFIED != 0 -> sortingRadio.sorting_dialog_radio_last_modified
            currSorting and SORT_BY_DATE_TAKEN != 0 -> sortingRadio.sorting_dialog_radio_date_taken
            currSorting and SORT_BY_RANDOM != 0 -> sortingRadio.sorting_dialog_radio_random
            currSorting and SORT_BY_CUSTOM != 0 -> sortingRadio.sorting_dialog_radio_custom
            else -> sortingRadio.sorting_dialog_radio_name
        }
        sortBtn.isChecked = true
    }

    private fun setupOrderRadio() {
        val orderRadio = view.sorting_dialog_radio_order
        var orderBtn = orderRadio.sorting_dialog_radio_ascending

        if (currSorting and SORT_DESCENDING != 0) {
            orderBtn = orderRadio.sorting_dialog_radio_descending
        }
        orderBtn.isChecked = true
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        val sortingRadio = view.sorting_dialog_radio_sorting
        var sorting = when (sortingRadio.checkedRadioButtonId) {
            R.id.sorting_dialog_radio_favorite -> SORT_BY_FAVORITE
            R.id.sorting_dialog_radio_name -> SORT_BY_NAME
            R.id.sorting_dialog_radio_path -> SORT_BY_PATH
            R.id.sorting_dialog_radio_size -> SORT_BY_SIZE
            R.id.sorting_dialog_radio_last_modified -> SORT_BY_DATE_MODIFIED
            R.id.sorting_dialog_radio_random -> SORT_BY_RANDOM
            R.id.sorting_dialog_radio_custom -> SORT_BY_CUSTOM
            else -> SORT_BY_DATE_TAKEN
        }

        if (view.sorting_dialog_radio_order.checkedRadioButtonId == R.id.sorting_dialog_radio_descending) {
            sorting = sorting or SORT_DESCENDING
        }

        if (view.sorting_dialog_numeric_sorting.isChecked) {
            sorting = sorting or SORT_USE_NUMERIC_VALUE
        }

        if (isDirectorySorting) {
            config.directorySorting = sorting
        } else {
            if (view.sorting_dialog_use_for_this_folder.isChecked) {
                config.saveCustomSorting(pathToUse, sorting)
            } else {
                config.removeCustomSorting(pathToUse)
                config.sorting = sorting
            }
        }

        if (currSorting != sorting) {
            callback()
        }
    }
}
