package com.simplemobiletools.gallery.pro.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.interfaces.RefreshRecyclerViewListener
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.adapters.ManageFoldersAdapter
import com.simplemobiletools.gallery.pro.extensions.config
import kotlinx.android.synthetic.main.activity_manage_folders.*

class IncludedFoldersActivity : SimpleActivity(), RefreshRecyclerViewListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_folders)
        updateFolders()
    }

    private fun updateFolders() {
        val folders = ArrayList<String>()
        config.includedFolders.mapTo(folders) { it }
        manage_folders_placeholder.apply {
            text = getString(R.string.included_activity_placeholder)
            beVisibleIf(folders.isEmpty())
            setTextColor(config.textColor)
        }

        val adapter = ManageFoldersAdapter(this, folders, false, this, manage_folders_list) {}
        manage_folders_list.adapter = adapter
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_add_folder, menu)
        updateMenuItemColors(menu)
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
        showAddIncludedFolderDialog {
            updateFolders()
        }
    }
}
