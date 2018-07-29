package com.simplemobiletools.gallery.adapters

import android.content.ContentProviderOperation
import android.media.ExifInterface
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.dialogs.PropertiesDialog
import com.simplemobiletools.commons.dialogs.RenameItemDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.OTG_PATH
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.commons.views.FastScroller
import com.simplemobiletools.commons.views.MyRecyclerView
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.dialogs.DeleteWithRememberDialog
import com.simplemobiletools.gallery.extensions.*
import com.simplemobiletools.gallery.helpers.VIEW_TYPE_LIST
import com.simplemobiletools.gallery.interfaces.MediaOperationsListener
import com.simplemobiletools.gallery.models.Medium
import com.simplemobiletools.gallery.models.ThumbnailItem
import com.simplemobiletools.gallery.models.ThumbnailSection
import kotlinx.android.synthetic.main.photo_video_item_grid.view.*
import kotlinx.android.synthetic.main.thumbnail_section.view.*
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class MediaAdapter(activity: BaseSimpleActivity, var media: MutableList<ThumbnailItem>, val listener: MediaOperationsListener?, val isAGetIntent: Boolean,
                   val allowMultiplePicks: Boolean, recyclerView: MyRecyclerView, fastScroller: FastScroller? = null, itemClick: (Any) -> Unit) :
        MyRecyclerViewAdapter(activity, recyclerView, fastScroller, itemClick) {

    private val INSTANT_LOAD_DURATION = 2000L
    private val IMAGE_LOAD_DELAY = 100L
    private val BATCH_SIZE = 100
    private val ITEM_SECTION = 0
    private val ITEM_MEDIUM = 1

    private val config = activity.config
    private val isListViewType = config.viewTypeFiles == VIEW_TYPE_LIST
    private var visibleItemPaths = ArrayList<String>()
    private var loadImageInstantly = false
    private var delayHandler = Handler(Looper.getMainLooper())
    private var currentMediaHash = media.hashCode()
    private val hasOTGConnected = activity.hasOTGConnected()

    private var scrollHorizontally = config.scrollHorizontally
    private var animateGifs = config.animateGifs
    private var cropThumbnails = config.cropThumbnails
    private var displayFilenames = config.displayFileNames

    init {
        setupDragListener(true)
        enableInstantLoad()
    }

    override fun getActionMenuId() = R.menu.cab_media

    override fun prepareItemSelection(viewHolder: ViewHolder) {
        viewHolder.itemView?.medium_check?.background?.applyColorFilter(primaryColor)
    }

    override fun markViewHolderSelection(select: Boolean, viewHolder: ViewHolder?) {
        viewHolder?.itemView?.medium_check?.beVisibleIf(select)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutType = if (viewType == ITEM_SECTION) {
            R.layout.thumbnail_section
        } else {
            if (isListViewType) {
                R.layout.photo_video_item_list
            } else {
                R.layout.photo_video_item_grid
            }
        }
        return createViewHolder(layoutType, parent)
    }

    override fun onBindViewHolder(holder: MyRecyclerViewAdapter.ViewHolder, position: Int) {
        val tmbItem = media.getOrNull(position) ?: return
        if (tmbItem is Medium) {
            visibleItemPaths.add(tmbItem.path)
        }

        val allowLongPress = !allowMultiplePicks && tmbItem is Medium
        val view = holder.bindView(tmbItem, tmbItem is Medium, allowLongPress) { itemView, adapterPosition ->
            if (tmbItem is Medium) {
                setupThumbnail(itemView, tmbItem)
            } else {
                setupSection(itemView, tmbItem as ThumbnailSection)
            }
        }
        bindViewHolder(holder, position, view)
    }

    override fun getItemCount() = media.size

    override fun getItemViewType(position: Int): Int {
        val tmbItem = media[position]
        return if (tmbItem is ThumbnailSection) {
            ITEM_SECTION
        } else {
            ITEM_MEDIUM
        }
    }

    override fun prepareActionMode(menu: Menu) {
        menu.apply {
            findItem(R.id.cab_rename).isVisible = isOneItemSelected()
            findItem(R.id.cab_open_with).isVisible = isOneItemSelected()
            findItem(R.id.cab_confirm_selection).isVisible = isAGetIntent && allowMultiplePicks && selectedPositions.size > 0
            findItem(R.id.cab_restore_recycle_bin_files).isVisible = getSelectedPaths().all { it.startsWith(activity.filesDir.absolutePath) }

            checkHideBtnVisibility(this)
            checkFavoriteBtnVisibility(this)
        }
    }

    override fun actionItemPressed(id: Int) {
        if (selectedPositions.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_confirm_selection -> confirmSelection()
            R.id.cab_properties -> showProperties()
            R.id.cab_rename -> renameFile()
            R.id.cab_edit -> editFile()
            R.id.cab_hide -> toggleFileVisibility(true)
            R.id.cab_unhide -> toggleFileVisibility(false)
            R.id.cab_add_to_favorites -> toggleFavorites(true)
            R.id.cab_remove_from_favorites -> toggleFavorites(false)
            R.id.cab_restore_recycle_bin_files -> restoreFiles()
            R.id.cab_share -> shareMedia()
            R.id.cab_copy_to -> copyMoveTo(true)
            R.id.cab_move_to -> copyMoveTo(false)
            R.id.cab_select_all -> selectAll()
            R.id.cab_open_with -> activity.openPath(getCurrentPath(), true)
            R.id.cab_fix_date_taken -> fixDateTaken()
            R.id.cab_set_as -> activity.setAs(getCurrentPath())
            R.id.cab_delete -> checkDeleteConfirmation()
        }
    }

    override fun getSelectableItemCount() = media.filter { it is Medium }.size

    override fun getIsItemSelectable(position: Int) = !isASectionTitle(position)

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        if (!activity.isActivityDestroyed()) {
            val itemView = holder.itemView
            visibleItemPaths.remove(itemView?.photo_name?.tag)
            val tmb = itemView?.medium_thumbnail
            if (tmb != null) {
                Glide.with(activity).clear(tmb)
            }
        }
    }

    fun isASectionTitle(position: Int) = media.getOrNull(position) is ThumbnailSection

    private fun checkHideBtnVisibility(menu: Menu) {
        var hiddenCnt = 0
        var unhiddenCnt = 0
        getSelectedMedia().forEach {
            if (it.isHidden()) {
                hiddenCnt++
            } else {
                unhiddenCnt++
            }
        }

        menu.findItem(R.id.cab_hide).isVisible = unhiddenCnt > 0
        menu.findItem(R.id.cab_unhide).isVisible = hiddenCnt > 0
    }

    private fun checkFavoriteBtnVisibility(menu: Menu) {
        var favoriteCnt = 0
        var nonFavoriteCnt = 0
        getSelectedMedia().forEach {
            if (it.isFavorite) {
                favoriteCnt++
            } else {
                nonFavoriteCnt++
            }
        }

        menu.findItem(R.id.cab_add_to_favorites).isVisible = nonFavoriteCnt > 0
        menu.findItem(R.id.cab_remove_from_favorites).isVisible = favoriteCnt > 0
    }

    private fun confirmSelection() {
        listener?.selectedPaths(getSelectedPaths())
    }

    private fun showProperties() {
        if (selectedPositions.size <= 1) {
            PropertiesDialog(activity, (media[selectedPositions.first()] as Medium).path, config.shouldShowHidden)
        } else {
            val paths = getSelectedPaths()
            PropertiesDialog(activity, paths, config.shouldShowHidden)
        }
    }

    private fun renameFile() {
        val oldPath = getCurrentPath()
        RenameItemDialog(activity, oldPath) {
            Thread {
                activity.updateDBMediaPath(oldPath, it)

                activity.runOnUiThread {
                    enableInstantLoad()
                    listener?.refreshItems()
                    finishActMode()
                }
            }.start()
        }
    }

    private fun editFile() {
        activity.openEditor(getCurrentPath())
    }

    private fun toggleFileVisibility(hide: Boolean) {
        Thread {
            getSelectedMedia().forEach {
                activity.toggleFileVisibility(it.path, hide)
            }
            activity.runOnUiThread {
                listener?.refreshItems()
                finishActMode()
            }
        }.start()
    }

    private fun toggleFavorites(add: Boolean) {
        Thread {
            val mediumDao = activity.galleryDB.MediumDao()
            getSelectedMedia().forEach {
                it.isFavorite = add
                mediumDao.updateFavorite(it.path, add)
            }
            activity.runOnUiThread {
                listener?.refreshItems()
                finishActMode()
            }
        }.start()
    }

    private fun restoreFiles() {
        activity.restoreRecycleBinPaths(getSelectedPaths()) {
            listener?.refreshItems()
            finishActMode()
        }
    }

    private fun shareMedia() {
        if (selectedPositions.size == 1 && selectedPositions.first() != -1) {
            activity.shareMediumPath(getSelectedMedia().first().path)
        } else if (selectedPositions.size > 1) {
            activity.shareMediaPaths(getSelectedPaths())
        }
    }

    private fun copyMoveTo(isCopyOperation: Boolean) {
        val paths = getSelectedPaths()

        val fileDirItems = paths.map {
            FileDirItem(it, it.getFilenameFromPath())
        } as ArrayList

        activity.tryCopyMoveFilesTo(fileDirItems, isCopyOperation) {
            config.tempFolderPath = ""
            activity.applicationContext.rescanFolderMedia(it)
            activity.applicationContext.rescanFolderMedia(fileDirItems.first().getParentPath())
            if (!isCopyOperation) {
                listener?.refreshItems()
            }
        }
    }

    private fun fixDateTaken() {
        activity.toast(R.string.fixing)
        Thread {
            try {
                val operations = ArrayList<ContentProviderOperation>()
                val paths = getSelectedPaths()
                for (path in paths) {
                    val dateTime = ExifInterface(path).getAttribute(ExifInterface.TAG_DATETIME) ?: continue
                    val format = "yyyy:MM:dd kk:mm:ss"
                    val formatter = SimpleDateFormat(format, Locale.getDefault())
                    val timestamp = formatter.parse(dateTime).time

                    val uri = activity.getFileUri(path)
                    ContentProviderOperation.newUpdate(uri).apply {
                        val selection = "${MediaStore.Images.Media.DATA} = ?"
                        val selectionArgs = arrayOf(path)
                        withSelection(selection, selectionArgs)
                        withValue(MediaStore.Images.Media.DATE_TAKEN, timestamp)
                        operations.add(build())
                    }

                    if (operations.size % BATCH_SIZE == 0) {
                        activity.contentResolver.applyBatch(MediaStore.AUTHORITY, operations)
                        operations.clear()
                    }
                }

                activity.contentResolver.applyBatch(MediaStore.AUTHORITY, operations)
                activity.toast(R.string.dates_fixed_successfully)
                activity.runOnUiThread {
                    listener?.refreshItems()
                    finishActMode()
                }
            } catch (e: Exception) {
                activity.showErrorToast(e)
            }
        }.start()
    }

    private fun checkDeleteConfirmation() {
        if (config.tempSkipDeleteConfirmation || config.skipDeleteConfirmation) {
            deleteFiles()
        } else {
            askConfirmDelete()
        }
    }

    private fun askConfirmDelete() {
        val items = resources.getQuantityString(R.plurals.delete_items, selectedPositions.size, selectedPositions.size)
        val isRecycleBin = getSelectedPaths().first().startsWith(activity.filesDir.absolutePath)
        val baseString = if (config.useRecycleBin && !isRecycleBin) R.string.move_to_recycle_bin_confirmation else R.string.deletion_confirmation
        val question = String.format(resources.getString(baseString), items)
        DeleteWithRememberDialog(activity, question) {
            config.tempSkipDeleteConfirmation = it
            deleteFiles()
        }
    }

    private fun getCurrentPath() = (media[selectedPositions.first()] as Medium).path

    private fun deleteFiles() {
        if (selectedPositions.isEmpty()) {
            return
        }

        val fileDirItems = ArrayList<FileDirItem>(selectedPositions.size)
        val removeMedia = ArrayList<Medium>(selectedPositions.size)

        if (media.size <= selectedPositions.first()) {
            finishActMode()
            return
        }

        val SAFPath = (media[selectedPositions.first()] as Medium).path
        activity.handleSAFDialog(SAFPath) {
            selectedPositions.sortedDescending().forEach {
                val thumbnailItem = media.getOrNull(it)
                if (thumbnailItem is Medium) {
                    fileDirItems.add(FileDirItem(thumbnailItem.path, thumbnailItem.name))
                    removeMedia.add(thumbnailItem)
                }
            }

            media.removeAll(removeMedia)
            listener?.tryDeleteFiles(fileDirItems)
            removeSelectedItems()
        }
    }

    private fun getSelectedMedia(): List<Medium> {
        val selectedMedia = ArrayList<Medium>(selectedPositions.size)
        selectedPositions.forEach {
            (media.getOrNull(it) as? Medium)?.apply {
                selectedMedia.add(this)
            }
        }
        return selectedMedia
    }

    private fun getSelectedPaths() = getSelectedMedia().map { it.path } as ArrayList<String>

    fun updateMedia(newMedia: ArrayList<ThumbnailItem>) {
        val thumbnailItems = newMedia.clone() as ArrayList<ThumbnailItem>
        if (thumbnailItems.hashCode() != currentMediaHash) {
            currentMediaHash = thumbnailItems.hashCode()
            Handler().postDelayed({
                media = thumbnailItems
                enableInstantLoad()
                notifyDataSetChanged()
                finishActMode()
            }, 100L)
        }
    }

    fun updateDisplayFilenames(displayFilenames: Boolean) {
        this.displayFilenames = displayFilenames
        enableInstantLoad()
        notifyDataSetChanged()
    }

    fun updateAnimateGifs(animateGifs: Boolean) {
        this.animateGifs = animateGifs
        notifyDataSetChanged()
    }

    fun updateCropThumbnails(cropThumbnails: Boolean) {
        this.cropThumbnails = cropThumbnails
        notifyDataSetChanged()
    }

    private fun enableInstantLoad() {
        loadImageInstantly = true
        delayHandler.postDelayed({
            loadImageInstantly = false
        }, INSTANT_LOAD_DURATION)
    }

    fun getItemBubbleText(position: Int, sorting: Int) = (media[position] as? Medium)?.getBubbleText(sorting)

    private fun setupThumbnail(view: View, medium: Medium) {
        view.apply {
            play_outline.beVisibleIf(medium.isVideo())
            photo_name.beVisibleIf(displayFilenames || isListViewType)
            photo_name.text = medium.name
            photo_name.tag = medium.path

            var path = medium.path
            if (hasOTGConnected && path.startsWith(OTG_PATH)) {
                path = path.getOTGPublicPath(context)
            }

            if (loadImageInstantly) {
                activity.loadImage(medium.type, path, medium_thumbnail, scrollHorizontally, animateGifs, cropThumbnails)
            } else {
                medium_thumbnail.setImageDrawable(null)
                medium_thumbnail.isHorizontalScrolling = scrollHorizontally
                delayHandler.postDelayed({
                    val isVisible = visibleItemPaths.contains(medium.path)
                    if (isVisible) {
                        activity.loadImage(medium.type, path, medium_thumbnail, scrollHorizontally, animateGifs, cropThumbnails)
                    }
                }, IMAGE_LOAD_DELAY)
            }

            if (isListViewType) {
                photo_name.setTextColor(textColor)
                play_outline.applyColorFilter(textColor)
            }
        }
    }

    private fun setupSection(view: View, section: ThumbnailSection) {
        view.apply {
            thumbnail_section.text = section.title
            thumbnail_section.setTextColor(textColor)
        }
    }
}
