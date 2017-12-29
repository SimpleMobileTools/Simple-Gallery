package com.simplemobiletools.gallery.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.interfaces.RefreshRecyclerViewListener
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.extensions.getNoMediaFolders
import kotlinx.android.synthetic.main.activity_manage_folders.*

class HiddenFoldersActivity : SimpleActivity(), RefreshRecyclerViewListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_folders)
        updateFolders()
    }

    private fun updateFolders() {
        val folders = getNoMediaFolders()
        manage_folders_placeholder.apply {
            text = getString(R.string.hidden_folders_placeholder)
            beVisibleIf(folders.isEmpty())
            setTextColor(config.textColor)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_add_folder, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_folder -> addFolder()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun refreshItems() {
        updateFolders()
    }

    private fun addFolder() {
        FilePickerDialog(this, pickFile = false, showHidden = config.shouldShowHidden) {

        }
    }
}
