package com.simplemobiletools.gallery.helpers

import android.content.Context
import com.simplemobiletools.commons.helpers.BaseConfig
import com.simplemobiletools.commons.helpers.SORT_BY_DATE_MODIFIED
import com.simplemobiletools.commons.helpers.SORT_DESCENDING
import com.simplemobiletools.gallery.R
import java.util.*

class Config(context: Context) : BaseConfig(context) {
    companion object {
        fun newInstance(context: Context) = Config(context)
    }

    var fileSorting: Int
        get() = prefs.getInt(SORT_ORDER, SORT_BY_DATE_MODIFIED or SORT_DESCENDING)
        set(order) = prefs.edit().putInt(SORT_ORDER, order).apply()

    var directorySorting: Int
        get() = prefs.getInt(DIRECTORY_SORT_ORDER, SORT_BY_DATE_MODIFIED or SORT_DESCENDING)
        set(order) = prefs.edit().putInt(DIRECTORY_SORT_ORDER, order).apply()

    fun saveFileSorting(path: String, value: Int) {
        if (path.isEmpty()) {
            fileSorting = value
        } else {
            prefs.edit().putInt(SORT_FOLDER_PREFIX + path, value).apply()
        }
    }

    fun getFileSorting(path: String) = prefs.getInt(SORT_FOLDER_PREFIX + path, fileSorting)

    fun removeFileSorting(path: String) {
        prefs.edit().remove(SORT_FOLDER_PREFIX + path).apply()
    }

    fun hasCustomSorting(path: String) = prefs.contains(SORT_FOLDER_PREFIX + path)

    var wasHideFolderTooltipShown: Boolean
        get() = prefs.getBoolean(HIDE_FOLDER_TOOLTIP_SHOWN, false)
        set(wasShown) = prefs.edit().putBoolean(HIDE_FOLDER_TOOLTIP_SHOWN, wasShown).apply()

    var showHiddenFolders: Boolean
        get() = prefs.getBoolean(SHOW_HIDDEN_FOLDERS, false)
        set(showHiddenFolders) = prefs.edit().putBoolean(SHOW_HIDDEN_FOLDERS, showHiddenFolders).apply()

    var temporarilyShowHidden: Boolean
        get() = prefs.getBoolean(TEMPORARILY_SHOW_HIDDEN, false)
        set(temporarilyShowHidden) = prefs.edit().putBoolean(TEMPORARILY_SHOW_HIDDEN, temporarilyShowHidden).apply()

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

    fun addExcludedFolder(path: String) {
        addExcludedFolders(HashSet<String>(Arrays.asList(path)))
    }

    fun addExcludedFolders(paths: Set<String>) {
        val currExcludedFolders = HashSet<String>(excludedFolders)
        currExcludedFolders.addAll(paths)
        excludedFolders = currExcludedFolders
    }

    fun removeExcludedFolder(path: String) {
        val currExcludedFolders = HashSet<String>(excludedFolders)
        currExcludedFolders.remove(path)
        excludedFolders = currExcludedFolders
    }

    var excludedFolders: MutableSet<String>
        get() = prefs.getStringSet(EXCLUDED_FOLDERS, HashSet<String>())
        set(excludedFolders) = prefs.edit().remove(EXCLUDED_FOLDERS).putStringSet(EXCLUDED_FOLDERS, excludedFolders).apply()

    fun addIncludedFolder(path: String) {
        val currIncludedFolders = HashSet<String>(includedFolders)
        currIncludedFolders.add(path)
        includedFolders = currIncludedFolders
    }

    fun removeIncludedFolder(path: String) {
        val currIncludedFolders = HashSet<String>(includedFolders)
        currIncludedFolders.remove(path)
        includedFolders = currIncludedFolders
    }

    var includedFolders: MutableSet<String>
        get() = prefs.getStringSet(INCLUDED_FOLDERS, HashSet<String>())
        set(includedFolders) = prefs.edit().remove(INCLUDED_FOLDERS).putStringSet(INCLUDED_FOLDERS, includedFolders).apply()

    fun saveFolderMedia(path: String, json: String) {
        prefs.edit().putString(SAVE_FOLDER_PREFIX + path, json).apply()
    }

    fun loadFolderMedia(path: String) = prefs.getString(SAVE_FOLDER_PREFIX + path, "")

    var autoplayVideos: Boolean
        get() = prefs.getBoolean(AUTOPLAY_VIDEOS, false)
        set(autoplay) = prefs.edit().putBoolean(AUTOPLAY_VIDEOS, autoplay).apply()

    var animateGifs: Boolean
        get() = prefs.getBoolean(ANIMATE_GIFS, false)
        set(animateGifs) = prefs.edit().putBoolean(ANIMATE_GIFS, animateGifs).apply()

    var maxBrightness: Boolean
        get() = prefs.getBoolean(MAX_BRIGHTNESS, false)
        set(maxBrightness) = prefs.edit().putBoolean(MAX_BRIGHTNESS, maxBrightness).apply()

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
