package com.simplemobiletools.gallery.activities

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.Utils
import com.simplemobiletools.gallery.adapters.DirectoryAdapter
import com.simplemobiletools.gallery.asynctasks.GetDirectoriesAsynctask
import com.simplemobiletools.gallery.models.Directory
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class PickAlbumActivity : SimpleActivity(), AdapterView.OnItemClickListener, GetDirectoriesAsynctask.GetDirectoriesListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mDirs = ArrayList<Directory>()
    }

    override fun onResume() {
        super.onResume()
        tryloadGallery()
    }

    private fun tryloadGallery() {
        if (Utils.hasStoragePermission(applicationContext)) {
            getDirectories()
        } else {
            Utils.showToast(applicationContext, R.string.no_permissions)
        }
    }

    private fun getDirectories() {
        GetDirectoriesAsynctask(applicationContext, false, false, ArrayList<String>(), this).execute()
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        val path = mDirs!![position].path
    }

    override fun gotDirectories(dirs: ArrayList<Directory>) {
        mDirs = dirs

        val adapter = DirectoryAdapter(this, dirs)
        directories_grid.adapter = adapter
        directories_grid.onItemClickListener = this
    }

    companion object {
        private var mDirs: MutableList<Directory>? = null
    }
}
