package com.simplemobiletools.gallery.helpers

import android.content.Context
import com.simplemobiletools.commons.helpers.BaseConfig
import com.simplemobiletools.gallery.R
import java.util.*

class Config(context: Context) : BaseConfig(context) {
    companion object {
        fun newInstance(context: Context) = Config(context)
    }

    var isSameSorting: Boolean
        get() = prefs.getBoolean(IS_SAME_SORTING, true)
        set(isSameSorting) = prefs.edit().putBoolean(IS_SAME_SORTING, isSameSorting).apply()

    var fileSorting: Int
        get() = if (isSameSorting) directorySorting else prefs.getInt(SORT_ORDER, SORT_BY_DATE_MODIFIED or SORT_DESCENDING)
        set(order) = if (isSameSorting) directorySorting = order else prefs.edit().putInt(SORT_ORDER, order).apply()

    var directorySorting: Int
        get() = prefs.getInt(DIRECTORY_SORT_ORDER, SORT_BY_DATE_MODIFIED or SORT_DESCENDING)
        set(order) = prefs.edit().putInt(DIRECTORY_SORT_ORDER, order).apply()

    var showHiddenFolders: Boolean
        get() = prefs.getBoolean(SHOW_HIDDEN_FOLDERS, false)
        set(showHiddenFolders) = prefs.edit().putBoolean(SHOW_HIDDEN_FOLDERS, showHiddenFolders).apply()

    var pinnedFolders: Set<String>
        get() = prefs.getStringSet(PINNED_FOLDERS, HashSet<String>())
        set(pinnedFolders) = prefs.edit().putStringSet(PINNED_FOLDERS, pinnedFolders).apply()

    var showAll: Boolean
        get() = prefs.getBoolean(SHOW_ALL, false)
        set(showAll) = prefs.edit().putBoolean(SHOW_ALL, showAll).apply()

    fun addPinnedFolders(paths: Set<String>) {
        val currPinnedFolders = HashSet<String>(pinnedFolders)
        currPinnedFolders.addAll(paths)
        pinnedFolders = currPinnedFolders
    }

    fun removePinnedFolders(paths: Set<String>) {
        val currPinnedFolders = HashSet<String>(pinnedFolders)
        currPinnedFolders.removeAll(paths)
        pinnedFolders = currPinnedFolders
    }

    fun addHiddenFolder(path: String) {
        addHiddenFolders(HashSet<String>(Arrays.asList(path)))
    }

    fun addHiddenFolders(paths: Set<String>) {
        val currHiddenFolders = HashSet<String>(hiddenFolders)
        currHiddenFolders.addAll(paths)
        hiddenFolders = currHiddenFolders
    }

    fun removeHiddenFolder(path: String) {
        removeHiddenFolders(HashSet<String>(Arrays.asList(path)))
    }

    fun removeHiddenFolders(paths: Set<String>) {
        val currHiddenFolders = HashSet<String>(hiddenFolders)
        currHiddenFolders.removeAll(paths)
        hiddenFolders = currHiddenFolders
    }

    fun getIsFolderHidden(path: String) = hiddenFolders.contains(path)

    fun saveFolderMedia(path: String, json: String) {
        prefs.edit().putString(SAVE_FOLDER_PREFIX + path, json).apply()
    }

    fun loadFolderMedia(path: String) = prefs.getString(SAVE_FOLDER_PREFIX + path, "")

    var hiddenFolders: MutableSet<String>
        get() = prefs.getStringSet(HIDDEN_FOLDERS, HashSet<String>())
        set(hiddenFolders) = prefs.edit().remove(HIDDEN_FOLDERS).putStringSet(HIDDEN_FOLDERS, hiddenFolders).apply()

    var autoplayVideos: Boolean
        get() = prefs.getBoolean(AUTOPLAY_VIDEOS, false)
        set(autoplay) = prefs.edit().putBoolean(AUTOPLAY_VIDEOS, autoplay).apply()

    var loopVideos: Boolean
        get() = prefs.getBoolean(LOOP_VIDEOS, false)
        set(loop) = prefs.edit().putBoolean(LOOP_VIDEOS, loop).apply()

    var displayFileNames: Boolean
        get() = prefs.getBoolean(DISPLAY_FILE_NAMES, false)
        set(display) = prefs.edit().putBoolean(DISPLAY_FILE_NAMES, display).apply()

    var showMedia: Int
        get() = prefs.getInt(SHOW_MEDIA, IMAGES_AND_VIDEOS)
        set(showMedia) = prefs.edit().putInt(SHOW_MEDIA, showMedia).apply()

    var dirColumnCnt: Int
        get() = prefs.getInt(DIR_COLUMN_CNT, context.resources.getInteger(R.integer.directory_columns))
        set(dirColumnCnt) = prefs.edit().putInt(DIR_COLUMN_CNT, dirColumnCnt).apply()

    var mediaColumnCnt: Int
        get() = prefs.getInt(MEDIA_COLUMN_CNT, context.resources.getInteger(R.integer.media_columns))
        set(mediaColumnCnt) = prefs.edit().putInt(MEDIA_COLUMN_CNT, mediaColumnCnt).apply()

    var directories: String
        get() = prefs.getString(DIRECTORIES, "")
        set(directories) = prefs.edit().putString(DIRECTORIES, directories).apply()
}
