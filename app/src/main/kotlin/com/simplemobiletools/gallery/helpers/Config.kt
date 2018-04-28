package com.simplemobiletools.gallery.helpers

import android.content.Context
import android.content.res.Configuration
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.simplemobiletools.commons.helpers.BaseConfig
import com.simplemobiletools.commons.helpers.SORT_BY_DATE_MODIFIED
import com.simplemobiletools.commons.helpers.SORT_BY_DATE_TAKEN
import com.simplemobiletools.commons.helpers.SORT_DESCENDING
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.models.AlbumCover
import java.util.*

class Config(context: Context) : BaseConfig(context) {
    companion object {
        fun newInstance(context: Context) = Config(context)
    }

    var directorySorting: Int
        get(): Int {
            var sort = prefs.getInt(DIRECTORY_SORT_ORDER, SORT_BY_DATE_MODIFIED or SORT_DESCENDING)
            if (sort and SORT_BY_DATE_TAKEN != 0) {
                sort = sort - SORT_BY_DATE_TAKEN + SORT_BY_DATE_MODIFIED
            }
            return sort
        }
        set(order) = prefs.edit().putInt(DIRECTORY_SORT_ORDER, order).apply()

    fun saveFileSorting(path: String, value: Int) {
        if (path.isEmpty()) {
            sorting = value
        } else {
            prefs.edit().putInt(SORT_FOLDER_PREFIX + path.toLowerCase(), value).apply()
        }
    }

    fun getFileSorting(path: String): Int {
        var sort = prefs.getInt(SORT_FOLDER_PREFIX + path.toLowerCase(), sorting)
        if (sort and SORT_BY_DATE_TAKEN != 0) {
            sort = sort - SORT_BY_DATE_TAKEN + SORT_BY_DATE_MODIFIED
        }
        return sort
    }

    fun removeFileSorting(path: String) {
        prefs.edit().remove(SORT_FOLDER_PREFIX + path.toLowerCase()).apply()
    }

    fun hasCustomSorting(path: String) = prefs.contains(SORT_FOLDER_PREFIX + path.toLowerCase())

    var wasHideFolderTooltipShown: Boolean
        get() = prefs.getBoolean(HIDE_FOLDER_TOOLTIP_SHOWN, false)
        set(wasShown) = prefs.edit().putBoolean(HIDE_FOLDER_TOOLTIP_SHOWN, wasShown).apply()

    var shouldShowHidden = showHiddenMedia || temporarilyShowHidden

    var showHiddenMedia: Boolean
        get() = prefs.getBoolean(SHOW_HIDDEN_MEDIA, false)
        set(showHiddenFolders) = prefs.edit().putBoolean(SHOW_HIDDEN_MEDIA, showHiddenFolders).apply()

    var temporarilyShowHidden: Boolean
        get() = prefs.getBoolean(TEMPORARILY_SHOW_HIDDEN, false)
        set(temporarilyShowHidden) = prefs.edit().putBoolean(TEMPORARILY_SHOW_HIDDEN, temporarilyShowHidden).apply()

    var isThirdPartyIntent: Boolean
        get() = prefs.getBoolean(IS_THIRD_PARTY_INTENT, false)
        set(isThirdPartyIntent) = prefs.edit().putBoolean(IS_THIRD_PARTY_INTENT, isThirdPartyIntent).apply()

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
        get() = prefs.getStringSet(EXCLUDED_FOLDERS, getDataFolder())
        set(excludedFolders) = prefs.edit().remove(EXCLUDED_FOLDERS).putStringSet(EXCLUDED_FOLDERS, excludedFolders).apply()

    private fun getDataFolder(): Set<String> {
        val folders = HashSet<String>()
        val dataFolder = context.externalCacheDir?.parentFile?.parent?.trimEnd('/') ?: ""
        if (dataFolder.endsWith("data"))
            folders.add(dataFolder)
        return folders
    }

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

    var autoplayVideos: Boolean
        get() = prefs.getBoolean(AUTOPLAY_VIDEOS, false)
        set(autoplay) = prefs.edit().putBoolean(AUTOPLAY_VIDEOS, autoplay).apply()

    var animateGifs: Boolean
        get() = prefs.getBoolean(ANIMATE_GIFS, false)
        set(animateGifs) = prefs.edit().putBoolean(ANIMATE_GIFS, animateGifs).apply()

    var maxBrightness: Boolean
        get() = prefs.getBoolean(MAX_BRIGHTNESS, false)
        set(maxBrightness) = prefs.edit().putBoolean(MAX_BRIGHTNESS, maxBrightness).apply()

    var cropThumbnails: Boolean
        get() = prefs.getBoolean(CROP_THUMBNAILS, true)
        set(cropThumbnails) = prefs.edit().putBoolean(CROP_THUMBNAILS, cropThumbnails).apply()

    var screenRotation: Int
        get() = prefs.getInt(SCREEN_ROTATION, ROTATE_BY_SYSTEM_SETTING)
        set(screenRotation) = prefs.edit().putInt(SCREEN_ROTATION, screenRotation).apply()

    var loopVideos: Boolean
        get() = prefs.getBoolean(LOOP_VIDEOS, false)
        set(loop) = prefs.edit().putBoolean(LOOP_VIDEOS, loop).apply()

    var displayFileNames: Boolean
        get() = prefs.getBoolean(DISPLAY_FILE_NAMES, false)
        set(display) = prefs.edit().putBoolean(DISPLAY_FILE_NAMES, display).apply()

    var blackBackground: Boolean
        get() = prefs.getBoolean(DARK_BACKGROUND, false)
        set(darkBackground) = prefs.edit().putBoolean(DARK_BACKGROUND, darkBackground).apply()

    var filterMedia: Int
        get() = prefs.getInt(FILTER_MEDIA, TYPE_IMAGES or TYPE_VIDEOS or TYPE_GIFS)
        set(filterMedia) = prefs.edit().putInt(FILTER_MEDIA, filterMedia).apply()

    var dirColumnCnt: Int
        get() = prefs.getInt(getDirectoryColumnsField(), getDefaultDirectoryColumnCount())
        set(dirColumnCnt) = prefs.edit().putInt(getDirectoryColumnsField(), dirColumnCnt).apply()

    var oneFingerZoom: Boolean
        get() = prefs.getBoolean(ONE_FINGER_ZOOM, false)
        set(oneFingerZoom) = prefs.edit().putBoolean(ONE_FINGER_ZOOM, oneFingerZoom).apply()

    var allowInstantChange: Boolean
        get() = prefs.getBoolean(ALLOW_INSTANT_CHANGE, false)
        set(allowInstantChange) = prefs.edit().putBoolean(ALLOW_INSTANT_CHANGE, allowInstantChange).apply()

    var replaceZoomableImages: Boolean
        get() = prefs.getBoolean(REPLACE_ZOOMABLE_IMAGES, false)
        set(replaceZoomableImages) = prefs.edit().putBoolean(REPLACE_ZOOMABLE_IMAGES, replaceZoomableImages).apply()

    private fun getDirectoryColumnsField(): String {
        val isPortrait = context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        return if (isPortrait) {
            if (scrollHorizontally) {
                DIR_HORIZONTAL_COLUMN_CNT
            } else {
                DIR_COLUMN_CNT
            }
        } else {
            if (scrollHorizontally) {
                DIR_LANDSCAPE_HORIZONTAL_COLUMN_CNT
            } else {
                DIR_LANDSCAPE_COLUMN_CNT
            }
        }
    }

    private fun getDefaultDirectoryColumnCount() = context.resources.getInteger(if (scrollHorizontally) R.integer.directory_columns_horizontal_scroll
    else R.integer.directory_columns_vertical_scroll)

    var mediaColumnCnt: Int
        get() = prefs.getInt(getMediaColumnsField(), getDefaultMediaColumnCount())
        set(mediaColumnCnt) = prefs.edit().putInt(getMediaColumnsField(), mediaColumnCnt).apply()

    private fun getMediaColumnsField(): String {
        val isPortrait = context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        return if (isPortrait) {
            if (scrollHorizontally) {
                MEDIA_HORIZONTAL_COLUMN_CNT
            } else {
                MEDIA_COLUMN_CNT
            }
        } else {
            if (scrollHorizontally) {
                MEDIA_LANDSCAPE_HORIZONTAL_COLUMN_CNT
            } else {
                MEDIA_LANDSCAPE_COLUMN_CNT
            }
        }
    }

    private fun getDefaultMediaColumnCount() = context.resources.getInteger(if (scrollHorizontally) R.integer.media_columns_horizontal_scroll
    else R.integer.media_columns_vertical_scroll)

    var albumCovers: String
        get() = prefs.getString(ALBUM_COVERS, "")
        set(albumCovers) = prefs.edit().putString(ALBUM_COVERS, albumCovers).apply()

    fun parseAlbumCovers(): ArrayList<AlbumCover> {
        val listType = object : TypeToken<List<AlbumCover>>() {}.type
        return Gson().fromJson<ArrayList<AlbumCover>>(albumCovers, listType) ?: ArrayList(1)
    }

    var hideSystemUI: Boolean
        get() = prefs.getBoolean(HIDE_SYSTEM_UI, false)
        set(hideSystemUI) = prefs.edit().putBoolean(HIDE_SYSTEM_UI, hideSystemUI).apply()

    var replaceShare: Boolean
        get() = prefs.getBoolean(REPLACE_SHARE_WITH_ROTATE, false)
        set(replaceShare) = prefs.edit().putBoolean(REPLACE_SHARE_WITH_ROTATE, replaceShare).apply()

    var deleteEmptyFolders: Boolean
        get() = prefs.getBoolean(DELETE_EMPTY_FOLDERS, false)
        set(deleteEmptyFolders) = prefs.edit().putBoolean(DELETE_EMPTY_FOLDERS, deleteEmptyFolders).apply()

    var allowPhotoGestures: Boolean
        get() = prefs.getBoolean(ALLOW_PHOTO_GESTURES, false)
        set(allowPhotoGestures) = prefs.edit().putBoolean(ALLOW_PHOTO_GESTURES, allowPhotoGestures).apply()

    var allowVideoGestures: Boolean
        get() = prefs.getBoolean(ALLOW_VIDEO_GESTURES, true)
        set(allowVideoGestures) = prefs.edit().putBoolean(ALLOW_VIDEO_GESTURES, allowVideoGestures).apply()

    var showMediaCount: Boolean
        get() = prefs.getBoolean(SHOW_MEDIA_COUNT, true)
        set(showMediaCount) = prefs.edit().putBoolean(SHOW_MEDIA_COUNT, showMediaCount).apply()

    var slideshowInterval: Int
        get() = prefs.getInt(SLIDESHOW_INTERVAL, SLIDESHOW_DEFAULT_INTERVAL)
        set(slideshowInterval) = prefs.edit().putInt(SLIDESHOW_INTERVAL, slideshowInterval).apply()

    var slideshowIncludePhotos: Boolean
        get() = prefs.getBoolean(SLIDESHOW_INCLUDE_PHOTOS, true)
        set(slideshowIncludePhotos) = prefs.edit().putBoolean(SLIDESHOW_INCLUDE_PHOTOS, slideshowIncludePhotos).apply()

    var slideshowIncludeVideos: Boolean
        get() = prefs.getBoolean(SLIDESHOW_INCLUDE_VIDEOS, false)
        set(slideshowIncludeVideos) = prefs.edit().putBoolean(SLIDESHOW_INCLUDE_VIDEOS, slideshowIncludeVideos).apply()

    var slideshowIncludeGIFs: Boolean
        get() = prefs.getBoolean(SLIDESHOW_INCLUDE_GIFS, false)
        set(slideshowIncludeGIFs) = prefs.edit().putBoolean(SLIDESHOW_INCLUDE_GIFS, slideshowIncludeGIFs).apply()

    var slideshowRandomOrder: Boolean
        get() = prefs.getBoolean(SLIDESHOW_RANDOM_ORDER, false)
        set(slideshowRandomOrder) = prefs.edit().putBoolean(SLIDESHOW_RANDOM_ORDER, slideshowRandomOrder).apply()

    var slideshowUseFade: Boolean
        get() = prefs.getBoolean(SLIDESHOW_USE_FADE, false)
        set(slideshowUseFade) = prefs.edit().putBoolean(SLIDESHOW_USE_FADE, slideshowUseFade).apply()

    var slideshowMoveBackwards: Boolean
        get() = prefs.getBoolean(SLIDESHOW_MOVE_BACKWARDS, false)
        set(slideshowMoveBackwards) = prefs.edit().putBoolean(SLIDESHOW_MOVE_BACKWARDS, slideshowMoveBackwards).apply()

    var loopSlideshow: Boolean
        get() = prefs.getBoolean(SLIDESHOW_LOOP, false)
        set(loopSlideshow) = prefs.edit().putBoolean(SLIDESHOW_LOOP, loopSlideshow).apply()

    var tempFolderPath: String
        get() = prefs.getString(TEMP_FOLDER_PATH, "")
        set(tempFolderPath) = prefs.edit().putString(TEMP_FOLDER_PATH, tempFolderPath).apply()

    var viewTypeFolders: Int
        get() = prefs.getInt(VIEW_TYPE_FOLDERS, VIEW_TYPE_GRID)
        set(viewTypeFolders) = prefs.edit().putInt(VIEW_TYPE_FOLDERS, viewTypeFolders).apply()

    var viewTypeFiles: Int
        get() = prefs.getInt(VIEW_TYPE_FILES, VIEW_TYPE_GRID)
        set(viewTypeFiles) = prefs.edit().putInt(VIEW_TYPE_FILES, viewTypeFiles).apply()

    var showExtendedDetails: Boolean
        get() = prefs.getBoolean(SHOW_EXTENDED_DETAILS, false)
        set(showExtendedDetails) = prefs.edit().putBoolean(SHOW_EXTENDED_DETAILS, showExtendedDetails).apply()

    var hideExtendedDetails: Boolean
        get() = prefs.getBoolean(HIDE_EXTENDED_DETAILS, false)
        set(hideExtendedDetails) = prefs.edit().putBoolean(HIDE_EXTENDED_DETAILS, hideExtendedDetails).apply()

    var extendedDetails: Int
        get() = prefs.getInt(EXTENDED_DETAILS, EXT_RESOLUTION or EXT_LAST_MODIFIED or EXT_EXIF_PROPERTIES)
        set(extendedDetails) = prefs.edit().putInt(EXTENDED_DETAILS, extendedDetails).apply()

    var doExtraCheck: Boolean
        get() = prefs.getBoolean(DO_EXTRA_CHECK, false)
        set(doExtraCheck) = prefs.edit().putBoolean(DO_EXTRA_CHECK, doExtraCheck).apply()

    var wasNewAppShown: Boolean
        get() = prefs.getBoolean(WAS_NEW_APP_SHOWN, false)
        set(wasNewAppShown) = prefs.edit().putBoolean(WAS_NEW_APP_SHOWN, wasNewAppShown).apply()

    var lastFilepickerPath: String
        get() = prefs.getString(LAST_FILEPICKER_PATH, "")
        set(lastFilepickerPath) = prefs.edit().putString(LAST_FILEPICKER_PATH, lastFilepickerPath).apply()

    var wasOTGHandled: Boolean
        get() = prefs.getBoolean(WAS_OTG_HANDLED, false)
        set(wasOTGHandled) = prefs.edit().putBoolean(WAS_OTG_HANDLED, wasOTGHandled).apply()
}
