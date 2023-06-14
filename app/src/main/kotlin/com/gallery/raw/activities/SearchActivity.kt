package com.gallery.raw.activities

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.VIEW_TYPE_GRID
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.commons.views.MyGridLayoutManager
import com.gallery.raw.R
import com.gallery.raw.adapters.MediaAdapter
import com.gallery.raw.extensions.config
import com.gallery.raw.extensions.deleteDBPath
import com.gallery.raw.extensions.getCachedMedia
import com.gallery.raw.asynctasks.GetMediaAsynctask
import com.gallery.raw.extensions.movePathsInRecycleBin
import com.gallery.raw.extensions.openPath
import com.gallery.raw.helpers.GridSpacingItemDecoration
import com.gallery.raw.helpers.MediaFetcher
import com.gallery.raw.helpers.PATH
import com.gallery.raw.helpers.SHOW_ALL
import com.gallery.raw.interfaces.MediaOperationsListener
import com.gallery.raw.models.Medium
import com.gallery.raw.models.ThumbnailItem
import kotlinx.android.synthetic.main.activity_search.*
import java.io.File

class SearchActivity : SimpleActivity(), MediaOperationsListener {
    private var mLastSearchedText = ""

    private var mCurrAsyncTask: GetMediaAsynctask? = null
    private var mAllMedia = ArrayList<ThumbnailItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        setupOptionsMenu()
        updateMaterialActivityViews(search_coordinator, search_grid, useTransparentNavigation = true, useTopSearchMenu = true)
        search_empty_text_placeholder.setTextColor(getProperTextColor())
        getAllMedia()
        search_fastscroller.updateColors(getProperPrimaryColor())
    }

    override fun onResume() {
        super.onResume()
        updateMenuColors()
    }

    override fun onDestroy() {
        super.onDestroy()
        mCurrAsyncTask?.stopFetching()
    }

    private fun setupOptionsMenu() {
        search_menu.getToolbar().inflateMenu(R.menu.menu_search)
        search_menu.toggleHideOnScroll(true)
        search_menu.setupMenu()
        search_menu.toggleForceArrowBackIcon(true)
        search_menu.focusView()
        search_menu.updateHintText(getString(R.string.search_files))

        search_menu.onNavigateBackClickListener = {
            if (search_menu.getCurrentQuery().isEmpty()) {
                finish()
            } else {
                search_menu.closeSearch()
            }
        }

        search_menu.onSearchTextChangedListener = { text ->
            mLastSearchedText = text
            textChanged(text)
        }

        search_menu.getToolbar().setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.toggle_filename -> toggleFilenameVisibility()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun updateMenuColors() {
        updateStatusbarColor(getProperBackgroundColor())
        search_menu.updateColors()
    }

    private fun textChanged(text: String) {
        ensureBackgroundThread {
            try {
                val filtered = mAllMedia.filter { it is Medium && it.name.contains(text, true) } as ArrayList
                filtered.sortBy { it is Medium && !it.name.startsWith(text, true) }
                val grouped = MediaFetcher(applicationContext).groupMedia(filtered as ArrayList<Medium>, "")
                runOnUiThread {
                    if (grouped.isEmpty()) {
                        search_empty_text_placeholder.text = getString(R.string.no_items_found)
                        search_empty_text_placeholder.beVisible()
                    } else {
                        search_empty_text_placeholder.beGone()
                    }

                    handleGridSpacing(grouped)
                    getMediaAdapter()?.updateMedia(grouped)
                }
            } catch (ignored: Exception) {
            }
        }
    }

    private fun setupAdapter() {
        val currAdapter = search_grid.adapter
        if (currAdapter == null) {
            MediaAdapter(this, mAllMedia, this, false, false, "", search_grid) {
                if (it is Medium) {
                    itemClicked(it.path)
                }
            }.apply {
                search_grid.adapter = this
            }
            setupLayoutManager()
            handleGridSpacing(mAllMedia)
        } else if (mLastSearchedText.isEmpty()) {
            (currAdapter as MediaAdapter).updateMedia(mAllMedia)
            handleGridSpacing(mAllMedia)
        } else {
            textChanged(mLastSearchedText)
        }

        setupScrollDirection()
    }

    private fun handleGridSpacing(media: ArrayList<ThumbnailItem>) {
        val viewType = config.getFolderViewType(SHOW_ALL)
        if (viewType == VIEW_TYPE_GRID) {
            if (search_grid.itemDecorationCount > 0) {
                search_grid.removeItemDecorationAt(0)
            }

            val spanCount = config.mediaColumnCnt
            val spacing = config.thumbnailSpacing
            val decoration = GridSpacingItemDecoration(spanCount, spacing, config.scrollHorizontally, config.fileRoundedCorners, media, true)
            search_grid.addItemDecoration(decoration)
        }
    }

    private fun getMediaAdapter() = search_grid.adapter as? MediaAdapter

    private fun toggleFilenameVisibility() {
        config.displayFileNames = !config.displayFileNames
        getMediaAdapter()?.updateDisplayFilenames(config.displayFileNames)
    }

    private fun itemClicked(path: String) {
        val isVideo = path.isVideoFast()
        if (isVideo) {
            openPath(path, false)
        } else {
            Intent(this, ViewPagerActivity::class.java).apply {
                putExtra(PATH, path)
                putExtra(SHOW_ALL, false)
                startActivity(this)
            }
        }
    }

    private fun setupLayoutManager() {
        val viewType = config.getFolderViewType(SHOW_ALL)
        if (viewType == VIEW_TYPE_GRID) {
            setupGridLayoutManager()
        } else {
            setupListLayoutManager()
        }
    }

    private fun setupGridLayoutManager() {
        val layoutManager = search_grid.layoutManager as MyGridLayoutManager
        if (config.scrollHorizontally) {
            layoutManager.orientation = RecyclerView.HORIZONTAL
            search_grid.layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
        } else {
            layoutManager.orientation = RecyclerView.VERTICAL
            search_grid.layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        layoutManager.spanCount = config.mediaColumnCnt
        val adapter = getMediaAdapter()
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (adapter?.isASectionTitle(position) == true) {
                    layoutManager.spanCount
                } else {
                    1
                }
            }
        }
    }

    private fun setupListLayoutManager() {
        val layoutManager = search_grid.layoutManager as MyGridLayoutManager
        layoutManager.spanCount = 1
        layoutManager.orientation = RecyclerView.VERTICAL
    }

    private fun setupScrollDirection() {
        val viewType = config.getFolderViewType(SHOW_ALL)
        val scrollHorizontally = config.scrollHorizontally && viewType == VIEW_TYPE_GRID
        search_fastscroller.setScrollVertically(!scrollHorizontally)
    }

    private fun getAllMedia() {
        getCachedMedia("") {
            if (it.isNotEmpty()) {
                mAllMedia = it.clone() as ArrayList<ThumbnailItem>
            }
            runOnUiThread {
                setupAdapter()
            }
            startAsyncTask(false)
        }
    }

    private fun startAsyncTask(updateItems: Boolean) {
        mCurrAsyncTask?.stopFetching()
        mCurrAsyncTask = GetMediaAsynctask(applicationContext, "", showAll = true) {
            mAllMedia = it.clone() as ArrayList<ThumbnailItem>
            if (updateItems) {
                textChanged(mLastSearchedText)
            }
        }

        mCurrAsyncTask!!.execute()
    }

    override fun refreshItems() {
        startAsyncTask(true)
    }

    override fun tryDeleteFiles(fileDirItems: ArrayList<FileDirItem>) {
        val filtered = fileDirItems.filter { File(it.path).isFile && it.path.isMediaFile() } as ArrayList
        if (filtered.isEmpty()) {
            return
        }

        if (config.useRecycleBin && !filtered.first().path.startsWith(recycleBinPath)) {
            val movingItems = resources.getQuantityString(R.plurals.moving_items_into_bin, filtered.size, filtered.size)
            toast(movingItems)

            movePathsInRecycleBin(filtered.map { it.path } as ArrayList<String>) {
                if (it) {
                    deleteFilteredFiles(filtered)
                } else {
                    toast(R.string.unknown_error_occurred)
                }
            }
        } else {
            val deletingItems = resources.getQuantityString(R.plurals.deleting_items, filtered.size, filtered.size)
            toast(deletingItems)
            deleteFilteredFiles(filtered)
        }
    }

    private fun deleteFilteredFiles(filtered: ArrayList<FileDirItem>) {
        deleteFiles(filtered) {
            if (!it) {
                toast(R.string.unknown_error_occurred)
                return@deleteFiles
            }

            mAllMedia.removeAll { filtered.map { it.path }.contains((it as? Medium)?.path) }

            ensureBackgroundThread {
                val useRecycleBin = config.useRecycleBin
                filtered.forEach {
                    if (it.path.startsWith(recycleBinPath) || !useRecycleBin) {
                        deleteDBPath(it.path)
                    }
                }
            }
        }
    }

    override fun selectedPaths(paths: ArrayList<String>) {}

    override fun updateMediaGridDecoration(media: ArrayList<ThumbnailItem>) {}
}
