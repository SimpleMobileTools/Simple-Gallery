package com.simplemobiletools.gallery.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.extensions.config

class IncludedFoldersActivity : SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_included_folders)
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
        FilePickerDialog(this, pickFile = false, showHidden = config.showHiddenFolders) {
            config.addIncludedFolder(it)
        }
    }
}
