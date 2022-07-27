package com.simplemobiletools.gallery.pro.activities

import android.os.Bundle
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.getProperTextColor
import com.simplemobiletools.commons.helpers.NavigationIcon
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.interfaces.RefreshRecyclerViewListener
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.adapters.ManageHiddenFoldersAdapter
import com.simplemobiletools.gallery.pro.extensions.addNoMedia
import com.simplemobiletools.gallery.pro.extensions.config
import com.simplemobiletools.gallery.pro.extensions.getNoMediaFolders
import kotlinx.android.synthetic.main.activity_manage_folders.*

class HiddenFoldersActivity : SimpleActivity(), RefreshRecyclerViewListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_folders)
        updateFolders()
        setupOptionsMenu()
        manage_folders_toolbar.title = getString(R.string.hidden_folders)
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(manage_folders_toolbar, NavigationIcon.Arrow)
    }

    private fun updateFolders() {
        getNoMediaFolders {
            runOnUiThread {
                manage_folders_placeholder.apply {
                    text = getString(R.string.hidden_folders_placeholder)
                    beVisibleIf(it.isEmpty())
                    setTextColor(getProperTextColor())
                }

                val adapter = ManageHiddenFoldersAdapter(this, it, this, manage_folders_list) {}
                manage_folders_list.adapter = adapter
            }
        }
    }

    private fun setupOptionsMenu() {
        manage_folders_toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.add_folder -> addFolder()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    override fun refreshItems() {
        updateFolders()
    }

    private fun addFolder() {
        FilePickerDialog(this, config.lastFilepickerPath, false, config.shouldShowHidden, false, true) {
            config.lastFilepickerPath = it
            ensureBackgroundThread {
                addNoMedia(it) {
                    updateFolders()
                }
            }
        }
    }
}
