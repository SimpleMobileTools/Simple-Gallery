package com.simplemobiletools.gallery.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.GridView
import com.simplemobiletools.filepicker.asynctasks.CopyMoveTask
import com.simplemobiletools.filepicker.extensions.*
import com.simplemobiletools.fileproperties.dialogs.PropertiesDialog
import com.simplemobiletools.gallery.Constants
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.Utils
import com.simplemobiletools.gallery.adapters.DirectoryAdapter
import com.simplemobiletools.gallery.asynctasks.GetDirectoriesAsynctask
import com.simplemobiletools.gallery.dialogs.ChangeSortingDialog
import com.simplemobiletools.gallery.dialogs.CopyDialog
import com.simplemobiletools.gallery.dialogs.RenameDirectoryDialog
import com.simplemobiletools.gallery.models.Directory
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.util.*

class MainActivity : SimpleActivity(), AdapterView.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener, GetDirectoriesAsynctask.GetDirectoriesListener {
    companion object {
        private val STORAGE_PERMISSION = 1
        private val PICK_MEDIA = 2
        private val PICK_WALLPAPER = 3

        lateinit var mDirs: MutableList<Directory>
        private var mSnackbar: Snackbar? = null
        lateinit var mToBeDeleted: MutableList<String>
        private var mActionMode: ActionMode? = null
        private var mState: Parcelable? = null

        private var mIsSnackbarShown = false
        private var mIsPickImageIntent = false
        private var mIsPickVideoIntent = false
        private var mIsGetImageContentIntent = false
        private var mIsGetVideoContentIntent = false
        private var mIsGetAnyContentIntent = false
        private var mIsSetWallpaperIntent = false
        private var mIsThirdPartyIntent = false
        private var mIsGettingDirs = false
        private var mSelectedItemsCnt = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mIsPickImageIntent = isPickImageIntent(intent)
        mIsPickVideoIntent = isPickVideoIntent(intent)
        mIsGetImageContentIntent = isGetImageContentIntent(intent)
        mIsGetVideoContentIntent = isGetVideoContentIntent(intent)
        mIsGetAnyContentIntent = isGetAnyContentIntent(intent)
        mIsSetWallpaperIntent = isSetWallpaperIntent(intent)
        mIsThirdPartyIntent = mIsPickImageIntent || mIsPickVideoIntent || mIsGetImageContentIntent || mIsGetVideoContentIntent ||
                mIsGetAnyContentIntent || mIsSetWallpaperIntent

        mToBeDeleted = ArrayList<String>()
        directories_holder.setOnRefreshListener(this)
        mDirs = ArrayList<Directory>()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (mIsThirdPartyIntent)
            return false

        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.sort -> {
                showSortingDialog()
                true
            }
            R.id.camera -> {
                startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
                true
            }
            R.id.settings -> {
                startActivity(Intent(applicationContext, SettingsActivity::class.java))
                true
            }
            R.id.about -> {
                startActivity(Intent(applicationContext, AboutActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        tryloadGallery()
        if (mState != null)
            directories_grid.onRestoreInstanceState(mState)
    }

    override fun onPause() {
        super.onPause()
        deleteDirs()
        mState = directories_grid.onSaveInstanceState()
    }

    override fun onDestroy() {
        super.onDestroy()
        mConfig.isFirstRun = false
    }

    private fun tryloadGallery() {
        if (hasStoragePermission()) {
            getDirectories()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == STORAGE_PERMISSION) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getDirectories()
            } else {
                toast(R.string.no_permissions)
                finish()
            }
        }
    }

    private fun getDirectories() {
        if (mIsGettingDirs)
            return

        mIsGettingDirs = true
        GetDirectoriesAsynctask(applicationContext, mIsPickVideoIntent || mIsGetVideoContentIntent, mIsPickImageIntent || mIsGetImageContentIntent,
                mToBeDeleted, this).execute()
    }

    private fun showSortingDialog() {
        ChangeSortingDialog(this, true, object : ChangeSortingDialog.OnChangeSortingListener {
            override fun sortingChanged() {
                getDirectories()
            }
        })
    }

    private fun prepareForDeleting() {
        toast(R.string.deleting)
        val items = directories_grid.checkedItemPositions
        val cnt = items.size()
        var deletedCnt = 0
        for (i in 0..cnt - 1) {
            if (items.valueAt(i)) {
                val id = items.keyAt(i)
                val path = mDirs[id].path
                mToBeDeleted.add(path)
                deletedCnt++
            }
        }

        for (path in mToBeDeleted) {
            if (isShowingPermDialog(File(path))) {
                return
            }
        }

        notifyDeletion(deletedCnt)
    }

    private fun notifyDeletion(cnt: Int) {
        getDirectories()

        val res = resources
        val msg = res.getQuantityString(R.plurals.folders_deleted, cnt, cnt)
        mSnackbar = Snackbar.make(coordinator_layout, msg, Snackbar.LENGTH_INDEFINITE)
        mSnackbar!!.apply {
            setAction(res.getString(R.string.undo), undoDeletion)
            setActionTextColor(Color.WHITE)
            show()
        }
        mIsSnackbarShown = true
    }

    private fun deleteDirs() {
        if (mToBeDeleted.isEmpty())
            return

        mSnackbar?.dismiss()
        mIsSnackbarShown = false

        val updatedFiles = ArrayList<File>()
        for (delPath in mToBeDeleted) {
            val dir = File(delPath)
            if (dir.exists()) {
                val files = dir.listFiles()
                for (file in files) {
                    if (file.isFile && file.isPhotoVideo()) {
                        updatedFiles.add(file)
                        deleteItem(file)
                    }
                }
                updatedFiles.add(dir)
                if (dir.listFiles().size == 0)
                    deleteItem(dir)
            }
        }

        scanFiles(updatedFiles) {}
        mToBeDeleted.clear()
    }

    private fun deleteItem(file: File) {
        if (needsStupidWritePermissions(file.absolutePath)) {
            if (!isShowingPermDialog(file)) {
                getFileDocument(file.absolutePath, mConfig.treeUri).delete()
            }
        } else {
            file.delete()
        }
    }

    private val undoDeletion = View.OnClickListener {
        mSnackbar!!.dismiss()
        mIsSnackbarShown = false
        mToBeDeleted.clear()
        getDirectories()
    }

    private fun showProperties() {
        val items = directories_grid.checkedItemPositions
        if (items.size() == 1) {
            PropertiesDialog(this, selectedPaths.toTypedArray()[0], false)
        } else {
            val paths = ArrayList<String>(items.size())
            val cnt = items.size()
            for (i in 0..cnt - 1) {
                if (items.valueAt(i)) {
                    val id = items.keyAt(i)
                    paths.add(mDirs[id].path)
                }
            }

            PropertiesDialog(this, paths, false)
        }
    }

    private fun editDirectory() {
        val items = directories_grid.checkedItemPositions
        val cnt = items.size()
        for (i in 0..cnt - 1) {
            if (items.valueAt(i)) {
                val id = items.keyAt(i)
                val path = mDirs[id].path
                renameDir(path)
                break
            }
        }
    }

    private fun renameDir(path: String) {
        val dir = File(path)
        if (isAStorageRootFolder(path)) {
            toast(R.string.rename_folder_root)
            return
        }

        RenameDirectoryDialog(this, dir, object : RenameDirectoryDialog.OnRenameDirListener {
            override fun onRenameDirSuccess(changedPaths: ArrayList<String>) {
                mActionMode!!.finish()
                applicationContext.scanPaths(changedPaths) { scanCompleted(path) }
            }
        })
    }

    private fun displayCopyDialog() {
        val files = ArrayList<File>()
        val items = directories_grid.checkedItemPositions
        val cnt = items.size()
        for (i in 0..cnt - 1) {
            if (items.valueAt(i)) {
                val id = items.keyAt(i)
                val dir = File(mDirs[id].path)
                files.addAll(dir.listFiles())
            }
        }

        CopyDialog(this, files, object : CopyMoveTask.CopyMoveListener {
            override fun copySucceeded(deleted: Boolean, copiedAll: Boolean) {
                if (deleted) {
                    getDirectories()
                    toast(if (copiedAll) R.string.moving_success else R.string.moving_success_partial)
                } else {
                    toast(if (copiedAll) R.string.copying_success else R.string.copying_success_partial)
                }
            }

            override fun copyFailed() {
                toast(R.string.copy_move_failed)
            }
        })
    }

    private fun isPickImageIntent(intent: Intent) = isPickIntent(intent) && (hasImageContentData(intent) || isImageType(intent))

    private fun isPickVideoIntent(intent: Intent) = isPickIntent(intent) && (hasVideoContentData(intent) || isVideoType(intent))

    private fun isPickIntent(intent: Intent) = intent.action == Intent.ACTION_PICK

    private fun isGetContentIntent(intent: Intent) = intent.action == Intent.ACTION_GET_CONTENT && intent.type != null

    private fun isGetImageContentIntent(intent: Intent) = isGetContentIntent(intent) &&
            (intent.type.startsWith("image/") || intent.type == MediaStore.Images.Media.CONTENT_TYPE)

    private fun isGetVideoContentIntent(intent: Intent) = isGetContentIntent(intent) &&
            (intent.type.startsWith("video/") || intent.type == MediaStore.Video.Media.CONTENT_TYPE)

    private fun isGetAnyContentIntent(intent: Intent) = isGetContentIntent(intent) && intent.type == "*/*"

    private fun isSetWallpaperIntent(intent: Intent?) = intent?.action == Intent.ACTION_SET_WALLPAPER

    private fun hasImageContentData(intent: Intent) = intent.data == MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    private fun hasVideoContentData(intent: Intent) = intent.data == MediaStore.Video.Media.EXTERNAL_CONTENT_URI

    private fun isImageType(intent: Intent) = (intent.type?.startsWith("image/") == true || intent.type == MediaStore.Images.Media.CONTENT_TYPE)

    private fun isVideoType(intent: Intent) = (intent.type?.startsWith("video/") == true || intent.type == MediaStore.Video.Media.CONTENT_TYPE)

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PICK_MEDIA && resultData != null) {
                Intent().apply {
                    val path = resultData.data.path
                    val uri = Uri.fromFile(File(path))
                    if (mIsGetImageContentIntent || mIsGetVideoContentIntent || mIsGetAnyContentIntent) {
                        val type = Utils.getMimeType(path)
                        setDataAndTypeAndNormalize(uri, type)
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    } else if (mIsPickImageIntent || mIsPickVideoIntent) {
                        data = uri
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }

                    setResult(Activity.RESULT_OK, this)
                }
                finish()
            } else if (requestCode == PICK_WALLPAPER) {
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
        super.onActivityResult(requestCode, resultCode, resultData)
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        Intent(this, MediaActivity::class.java).apply {
            putExtra(Constants.DIRECTORY, mDirs[position].path)

            if (mIsSetWallpaperIntent) {
                putExtra(Constants.SET_WALLPAPER_INTENT, true)
                startActivityForResult(this, PICK_WALLPAPER)
            } else {
                putExtra(Constants.GET_IMAGE_INTENT, mIsPickImageIntent || mIsGetImageContentIntent)
                putExtra(Constants.GET_VIDEO_INTENT, mIsPickVideoIntent || mIsGetVideoContentIntent)
                putExtra(Constants.GET_ANY_INTENT, mIsGetAnyContentIntent)
                startActivityForResult(this, PICK_MEDIA)
            }
        }
    }

    /*override fun onItemCheckedStateChanged(mode: ActionMode, position: Int, id: Long, checked: Boolean) {
        if (checked) {
            mSelectedItemsCnt++
        } else {
            mSelectedItemsCnt--
        }

        if (mSelectedItemsCnt > 0) {
            mode.title = mSelectedItemsCnt.toString()
        }

        mode.invalidate()
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        val inflater = mode.menuInflater
        inflater.inflate(R.menu.cab_directories, menu)
        mActionMode = mode
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        menu.findItem(R.id.cab_edit).isVisible = mSelectedItemsCnt == 1

        var hiddenCnt = 0
        var unhiddenCnt = 0
        val items = directories_grid.checkedItemPositions
        val cnt = items.size()
        for (i in 0..cnt - 1) {
            if (items.valueAt(i)) {
                val id = items.keyAt(i)
                if (mConfig.getIsFolderHidden(mDirs[id].path))
                    hiddenCnt++
                else
                    unhiddenCnt++
            }
        }

        menu.findItem(R.id.cab_hide).isVisible = unhiddenCnt > 0
        menu.findItem(R.id.cab_unhide).isVisible = hiddenCnt > 0

        return true
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.cab_properties -> {
                showProperties()
                true
            }
            R.id.cab_edit -> {
                editDirectory()
                true
            }
            R.id.cab_delete -> {
                prepareForDeleting()
                mode.finish()
                true
            }
            R.id.cab_hide -> {
                hideFolders()
                mode.finish()
                true
            }
            R.id.cab_unhide -> {
                unhideFolders()
                mode.finish()
                true
            }
            R.id.cab_copy_move -> {
                displayCopyDialog()
                true
            }
            else -> false
        }
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        mSelectedItemsCnt = 0
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (mIsSnackbarShown) {
            deleteDirs()
        }

        return false
    }*/

    private fun hideFolders() {
        mConfig.addHiddenDirectories(selectedPaths)
        getDirectories()
    }

    private fun unhideFolders() {
        mConfig.removeHiddenDirectories(selectedPaths)
        getDirectories()
    }

    private val selectedPaths: Set<String>
        get() {
            val items = directories_grid.checkedItemPositions
            val selectedPaths = HashSet<String>()
            val cnt = items.size()
            for (i in 0..cnt - 1) {
                if (items.valueAt(i)) {
                    val id = items.keyAt(i)
                    selectedPaths.add(mDirs[id].path)
                }
            }
            return selectedPaths
        }

    private fun scanCompleted(path: String) {
        val dir = File(path)
        if (dir.isDirectory) {
            getDirectories()

            runOnUiThread { toast(R.string.rename_folder_ok) }
        }
    }

    override fun onRefresh() {
        getDirectories()
        directories_holder.isRefreshing = false
    }

    override fun gotDirectories(dirs: ArrayList<Directory>) {
        mIsGettingDirs = false
        if (dirs.toString() == mDirs.toString()) {
            return
        }
        mDirs = dirs

        val adapter = DirectoryAdapter(this, mDirs)
        directories_grid.apply {
            this@apply.adapter = adapter
            onItemClickListener = this@MainActivity
            //setMultiChoiceModeListener(this)
            //setOnTouchListener(this)
            choiceMode = GridView.CHOICE_MODE_MULTIPLE_MODAL
        }
    }
}
