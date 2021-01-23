package com.simplemobiletools.gallery.pro.activities

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.VIEW_TYPE_GRID
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.commons.views.MyGridLayoutManager
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.adapters.MediaAdapter
import com.simplemobiletools.gallery.pro.asynctasks.GetMediaAsynctask
import com.simplemobiletools.gallery.pro.extensions.*
import com.simplemobiletools.gallery.pro.helpers.*
import com.simplemobiletools.gallery.pro.interfaces.MediaOperationsListener
import com.simplemobiletools.gallery.pro.models.Medium
import com.simplemobiletools.gallery.pro.models.ThumbnailItem
import com.simplemobiletools.gallery.pro.models.ThumbnailSection
import kotlinx.android.synthetic.main.activity_media.*
import kotlinx.android.synthetic.main.activity_search.*
import kotlinx.android.synthetic.main.activity_search.media_empty_text_placeholder
import kotlinx.android.synthetic.main.activity_search.media_grid
import kotlinx.android.synthetic.main.activity_search.media_horizontal_fastscroller
import kotlinx.android.synthetic.main.activity_search.media_vertical_fastscroller
import java.io.File

class SearchActivity : SimpleActivity(), MediaOperationsListener {
    private var mIsSearchOpen = false
    private var mLastSearchedText = ""
    private var mDateFormat = ""
    private var mTimeFormat = ""

    private var mSearchMenuItem: MenuItem? = null
    private var mCurrAsyncTask: GetMediaAsynctask? = null
    private var mAllMedia = ArrayList<ThumbnailItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        media_empty_text_placeholder.setTextColor(config.textColor)
        mDateFormat = config.dateFormat
        mTimeFormat = getTimeFormat()
        getAllMedia()
    }

    override fun onDestroy() {
        super.onDestroy()
        mCurrAsyncTask?.stopFetching()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)
        setupSearch(menu)
        updateMenuItemColors(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.toggle_filename -> toggleFilenameVisibility()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun setupSearch(menu: Menu) {
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        mSearchMenuItem = menu.findItem(R.id.search)
        (mSearchMenuItem?.actionView as? SearchView)?.apply {
            setSearchableInfo(searchManager.getSearchableInfo(componentName))
            isSubmitButtonEnabled = false
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String) = false

                override fun onQueryTextChange(newText: String): Boolean {
                    if (mIsSearchOpen) {
                        mLastSearchedText = newText
                        textChanged(newText)
                    }
                    return true
                }
            })
        }

        MenuItemCompat.setOnActionExpandListener(mSearchMenuItem, object : MenuItemCompat.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                mIsSearchOpen = true
                return true
            }

            // this triggers on device rotation too, avoid doing anything
            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                if (mIsSearchOpen) {
                    mIsSearchOpen = false
                    mLastSearchedText = ""
                }
                return true
            }
        })
        mSearchMenuItem?.expandActionView()
    }

    private fun textChanged(text: String) {
        ensureBackgroundThread {
            try {
                val filtered = mAllMedia.filter { it is Medium && it.name.contains(text, true) } as ArrayList
                filtered.sortBy { it is Medium && !it.name.startsWith(text, true) }
                val grouped = MediaFetcher(applicationContext).groupMedia(filtered as ArrayList<Medium>, "")
                runOnUiThread {
                    if (grouped.isEmpty()) {
                        media_empty_text_placeholder.text = getString(R.string.no_items_found)
                        media_empty_text_placeholder.beVisible()
                    } else {
                        media_empty_text_placeholder.beGone()
                    }

                    handleGridSpacing(grouped)
                    getMediaAdapter()?.updateMedia(grouped)
                    measureRecyclerViewContent(grouped)
                }
            } catch (ignored: Exception) {
            }
        }
    }

    private fun setupAdapter() {
        val currAdapter = media_grid.adapter
        if (currAdapter == null) {
            val fastscroller = if (config.scrollHorizontally) media_horizontal_fastscroller else media_vertical_fastscroller
            MediaAdapter(this, ArrayList(), this, false, false, "", media_grid, fastscroller) {
                if (it is Medium) {
                    itemClicked(it.path)
                }
            }.apply {
                media_grid.adapter = this
            }
            setupLayoutManager()
            handleGridSpacing(mAllMedia)
            measureRecyclerViewContent(mAllMedia)
        } else if (mLastSearchedText.isEmpty()) {
            (currAdapter as MediaAdapter).updateMedia(mAllMedia)
            handleGridSpacing(mAllMedia)
            measureRecyclerViewContent(mAllMedia)
        } else {
            textChanged(mLastSearchedText)
        }

        setupScrollDirection()
    }

    private fun handleGridSpacing(media: ArrayList<ThumbnailItem>) {
        val viewType = config.getFolderViewType(SHOW_ALL)
        if (viewType == VIEW_TYPE_GRID) {
            if (media_grid.itemDecorationCount > 0) {
                media_grid.removeItemDecorationAt(0)
            }

            val spanCount = config.mediaColumnCnt
            val spacing = config.thumbnailSpacing
            val decoration = GridSpacingItemDecoration(spanCount, spacing, config.scrollHorizontally, config.fileRoundedCorners, media, true)
            media_grid.addItemDecoration(decoration)
        }
    }

    private fun getMediaAdapter() = media_grid.adapter as? MediaAdapter

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
        val layoutManager = media_grid.layoutManager as MyGridLayoutManager
        if (config.scrollHorizontally) {
            layoutManager.orientation = RecyclerView.HORIZONTAL
            media_grid.layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
        } else {
            layoutManager.orientation = RecyclerView.VERTICAL
            media_grid.layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
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
        val layoutManager = media_grid.layoutManager as MyGridLayoutManager
        layoutManager.spanCount = 1
        layoutManager.orientation = RecyclerView.VERTICAL
    }

    private fun setupScrollDirection() {
        val viewType = config.getFolderViewType(SHOW_ALL)
        val allowHorizontalScroll = config.scrollHorizontally && viewType == VIEW_TYPE_GRID
        media_vertical_fastscroller.isHorizontal = false
        media_vertical_fastscroller.beGoneIf(allowHorizontalScroll)

        media_horizontal_fastscroller.isHorizontal = true
        media_horizontal_fastscroller.beVisibleIf(allowHorizontalScroll)

        val sorting = config.getFolderSorting(SHOW_ALL)
        if (allowHorizontalScroll) {
            media_horizontal_fastscroller.setViews(media_grid) {
                media_horizontal_fastscroller.updateBubbleText(getBubbleTextItem(it, sorting))
            }
        } else {
            media_vertical_fastscroller.setViews(media_grid) {
                media_vertical_fastscroller.updateBubbleText(getBubbleTextItem(it, sorting))
            }
        }
    }

    private fun getBubbleTextItem(index: Int, sorting: Int): String {
        var realIndex = index
        val mediaAdapter = getMediaAdapter()
        if (mediaAdapter?.isASectionTitle(index) == true) {
            realIndex++
        }
        return mediaAdapter?.getItemBubbleText(realIndex, sorting, mDateFormat, mTimeFormat) ?: ""
    }

    private fun measureRecyclerViewContent(media: ArrayList<ThumbnailItem>) {
        media_grid.onGlobalLayout {
            if (config.scrollHorizontally) {
                calculateContentWidth(media)
            } else {
                calculateContentHeight(media)
            }
        }
    }

    private fun calculateContentWidth(media: ArrayList<ThumbnailItem>) {
        val layoutManager = media_grid.layoutManager as MyGridLayoutManager
        val thumbnailWidth = layoutManager.getChildAt(0)?.width ?: 0
        val fullWidth = ((media.size - 1) / layoutManager.spanCount + 1) * thumbnailWidth
        media_horizontal_fastscroller.setContentWidth(fullWidth)
        media_horizontal_fastscroller.setScrollToX(media_grid.computeHorizontalScrollOffset())
    }

    private fun calculateContentHeight(media: ArrayList<ThumbnailItem>) {
        val layoutManager = media_grid.layoutManager as MyGridLayoutManager
        val pathToCheck = SHOW_ALL
        val hasSections = config.getFolderGrouping(pathToCheck) and GROUP_BY_NONE == 0 && !config.scrollHorizontally
        val sectionTitleHeight = if (hasSections) layoutManager.getChildAt(0)?.height ?: 0 else 0
        val thumbnailHeight = if (hasSections) layoutManager.getChildAt(1)?.height ?: 0 else layoutManager.getChildAt(0)?.height ?: 0

        var fullHeight = 0
        var curSectionItems = 0
        media.forEach {
            if (it is ThumbnailSection) {
                fullHeight += sectionTitleHeight
                if (curSectionItems != 0) {
                    val rows = ((curSectionItems - 1) / layoutManager.spanCount + 1)
                    fullHeight += rows * thumbnailHeight
                }
                curSectionItems = 0
            } else {
                curSectionItems++
            }
        }

        fullHeight += ((curSectionItems - 1) / layoutManager.spanCount + 1) * thumbnailHeight
        media_vertical_fastscroller.setContentHeight(fullHeight)
        media_vertical_fastscroller.setScrollToY(media_grid.computeVerticalScrollOffset())
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

    override fun selectedPaths(paths: ArrayList<String>) {
    }

    override fun updateMediaGridDecoration(media: ArrayList<ThumbnailItem>) {
    }
}
