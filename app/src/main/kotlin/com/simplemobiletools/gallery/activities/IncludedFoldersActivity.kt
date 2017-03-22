package com.simplemobiletools.gallery.activities

import android.graphics.PorterDuff
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.scanPath
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.extensions.config
import kotlinx.android.synthetic.main.activity_included_folders.*
import kotlinx.android.synthetic.main.item_manage_folder.view.*

class IncludedFoldersActivity : SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_included_folders)
        updateIncludedFolders()
    }

    private fun updateIncludedFolders() {
        included_folders_holder.removeAllViews()
        val folders = config.includedFolders
        included_folders_placeholder.beVisibleIf(folders.isEmpty())
        included_folders_placeholder.setTextColor(config.textColor)

        for (folder in folders) {
            layoutInflater.inflate(R.layout.item_manage_folder, null, false).apply {
                managed_folder_title.apply {
                    text = folder
                    setTextColor(config.textColor)
                }
                managed_folders_icon.apply {
                    setColorFilter(config.textColor, PorterDuff.Mode.SRC_IN)
                    setOnClickListener {
                        config.removeIncludedFolder(folder)
                        updateIncludedFolders()
                    }
                }
                included_folders_holder.addView(this)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_included_folders, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_folder -> addIncludedFolder()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun addIncludedFolder() {
        FilePickerDialog(this, pickFile = false, showHidden = config.shouldShowHidden) {
            config.addIncludedFolder(it)
            updateIncludedFolders()
            scanPath(it) {}
        }
    }
}
