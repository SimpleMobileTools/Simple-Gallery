package com.simplemobiletools.gallery

import android.content.Context
import android.content.SharedPreferences
import java.util.*

class Config private constructor(context: Context) {
    private val mPrefs: SharedPreferences

    companion object {
        fun newInstance(context: Context) = Config(context)
    }

    init {
        mPrefs = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
    }

    var isFirstRun: Boolean
        get() = mPrefs.getBoolean(IS_FIRST_RUN, true)
        set(isFirstRun) = mPrefs.edit().putBoolean(IS_FIRST_RUN, isFirstRun).apply()

    var isDarkTheme: Boolean
        get() = mPrefs.getBoolean(IS_DARK_THEME, true)
        set(isDarkTheme) = mPrefs.edit().putBoolean(IS_DARK_THEME, isDarkTheme).apply()

    var isSameSorting: Boolean
        get() = mPrefs.getBoolean(IS_SAME_SORTING, true)
        set(isSameSorting) = mPrefs.edit().putBoolean(IS_SAME_SORTING, isSameSorting).apply()

    var sorting: Int
        get() = if (isSameSorting) directorySorting else mPrefs.getInt(SORT_ORDER, SORT_BY_DATE or SORT_DESCENDING)
        set(order) = if (isSameSorting) directorySorting = order else mPrefs.edit().putInt(SORT_ORDER, order).apply()

    var directorySorting: Int
        get() = mPrefs.getInt(DIRECTORY_SORT_ORDER, SORT_BY_DATE or SORT_DESCENDING)
        set(order) = mPrefs.edit().putInt(DIRECTORY_SORT_ORDER, order).apply()

    var showHiddenFolders: Boolean
        get() = mPrefs.getBoolean(SHOW_HIDDEN_FOLDERS, false)
        set(showHiddenFolders) = mPrefs.edit().putBoolean(SHOW_HIDDEN_FOLDERS, showHiddenFolders).apply()

    fun addHiddenDirectory(path: String) {
        val currHiddenFolders = HashSet<String>(hiddenFolders)
        currHiddenFolders.add(path)
        hiddenFolders = currHiddenFolders
    }

    fun addHiddenDirectories(paths: Set<String>) {
        val currHiddenFolders = HashSet<String>(hiddenFolders)
        currHiddenFolders.addAll(paths)
        hiddenFolders = currHiddenFolders
    }

    fun removeHiddenDirectory(path: String) {
        val currHiddenFolders = HashSet<String>(hiddenFolders)
        currHiddenFolders.remove(path)
        hiddenFolders = currHiddenFolders
    }

    fun removeHiddenDirectories(paths: Set<String>) {
        val currHiddenFolders = HashSet<String>(hiddenFolders)
        currHiddenFolders.removeAll(paths)
        hiddenFolders = currHiddenFolders
    }

    fun getIsFolderHidden(path: String) = hiddenFolders.contains(path)

    var hiddenFolders: MutableSet<String>
        get() = mPrefs.getStringSet(HIDDEN_FOLDERS, HashSet<String>())
        set(hiddenFolders) = mPrefs.edit().remove(HIDDEN_FOLDERS).putStringSet(HIDDEN_FOLDERS, hiddenFolders).apply()

    var autoplayVideos: Boolean
        get() = mPrefs.getBoolean(AUTOPLAY_VIDEOS, false)
        set(autoplay) = mPrefs.edit().putBoolean(AUTOPLAY_VIDEOS, autoplay).apply()

    var treeUri: String
        get() = mPrefs.getString(TREE_URI, "")
        set(uri) = mPrefs.edit().putString(TREE_URI, uri).apply()

    var displayFileNames: Boolean
        get() = mPrefs.getBoolean(DISPLAY_FILE_NAMES, false)
        set(display) = mPrefs.edit().putBoolean(DISPLAY_FILE_NAMES, display).apply()
}
