package com.simplemobiletools.gallery.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.extensions.config
import kotlinx.android.synthetic.main.activity_excluded_folders.*
import kotlinx.android.synthetic.main.item_excluded_folder.view.*

class ExcludedFoldersActivity : SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_excluded_folders)
        updateExcludedFolders()
    }

    private fun updateExcludedFolders() {
        excluded_folders_holder.removeAllViews()
        val folders = config.excludedFolders
        for (folder in folders) {
            layoutInflater.inflate(R.layout.item_excluded_folder, null, false).apply {
                excluded_folder_title.text = folder
                excluded_folders_icon.setOnClickListener {
                    config.removeExcludedFolder(folder)
                    updateExcludedFolders()
                }
                excluded_folders_holder.addView(this)
            }
        }
        updateTextColors(excluded_folders_holder)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_excluded_folders, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.remove_all -> removeAllExcludedFolders()
        }
        return true
    }

    private fun removeAllExcludedFolders() {
        ConfirmationDialog(this, getString(R.string.remove_all_description)) {
            config.removeAllExcludedFolders()
            updateExcludedFolders()
        }
    }
}
