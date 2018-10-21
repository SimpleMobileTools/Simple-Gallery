package com.simplemobiletools.gallery.helpers

import com.simplemobiletools.commons.helpers.MONTH_SECONDS

// shared preferences
const val DIRECTORY_SORT_ORDER = "directory_sort_order"
const val SORT_FOLDER_PREFIX = "sort_folder_"
const val GROUP_FOLDER_PREFIX = "group_folder_"
const val SHOW_HIDDEN_MEDIA = "show_hidden_media"
const val TEMPORARILY_SHOW_HIDDEN = "temporarily_show_hidden"
const val IS_THIRD_PARTY_INTENT = "is_third_party_intent"
const val AUTOPLAY_VIDEOS = "autoplay_videos"
const val LOOP_VIDEOS = "loop_videos"
const val ANIMATE_GIFS = "animate_gifs"
const val MAX_BRIGHTNESS = "max_brightness"
const val CROP_THUMBNAILS = "crop_thumbnails"
const val SCREEN_ROTATION = "screen_rotation"
const val DISPLAY_FILE_NAMES = "display_file_names"
const val DARK_BACKGROUND = "dark_background"
const val PINNED_FOLDERS = "pinned_folders"
const val FILTER_MEDIA = "filter_media"
const val DIR_COLUMN_CNT = "dir_column_cnt"
const val DIR_LANDSCAPE_COLUMN_CNT = "dir_landscape_column_cnt"
const val DIR_HORIZONTAL_COLUMN_CNT = "dir_horizontal_column_cnt"
const val DIR_LANDSCAPE_HORIZONTAL_COLUMN_CNT = "dir_landscape_horizontal_column_cnt"
const val MEDIA_COLUMN_CNT = "media_column_cnt"
const val MEDIA_LANDSCAPE_COLUMN_CNT = "media_landscape_column_cnt"
const val MEDIA_HORIZONTAL_COLUMN_CNT = "media_horizontal_column_cnt"
const val MEDIA_LANDSCAPE_HORIZONTAL_COLUMN_CNT = "media_landscape_horizontal_column_cnt"
const val SHOW_ALL = "show_all"                           // display images and videos from all folders together
const val HIDE_FOLDER_TOOLTIP_SHOWN = "hide_folder_tooltip_shown"
const val EXCLUDED_FOLDERS = "excluded_folders"
const val INCLUDED_FOLDERS = "included_folders"
const val ALBUM_COVERS = "album_covers"
const val HIDE_SYSTEM_UI = "hide_system_ui"
const val DELETE_EMPTY_FOLDERS = "delete_empty_folders"
const val ALLOW_PHOTO_GESTURES = "allow_photo_gestures"
const val ALLOW_VIDEO_GESTURES = "allow_video_gestures"
const val SHOW_MEDIA_COUNT = "show_media_count"
const val TEMP_FOLDER_PATH = "temp_folder_path"
const val VIEW_TYPE_FOLDERS = "view_type_folders"
const val VIEW_TYPE_FILES = "view_type_files"
const val SHOW_EXTENDED_DETAILS = "show_extended_details"
const val EXTENDED_DETAILS = "extended_details"
const val HIDE_EXTENDED_DETAILS = "hide_extended_details"
const val ONE_FINGER_ZOOM = "one_finger_zoom"
const val ALLOW_INSTANT_CHANGE = "allow_instant_change"
const val DO_EXTRA_CHECK = "do_extra_check"
const val WAS_NEW_APP_SHOWN = "was_new_app_shown_clock"
const val LAST_FILEPICKER_PATH = "last_filepicker_path"
const val WAS_OTG_HANDLED = "was_otg_handled"
const val TEMP_SKIP_DELETE_CONFIRMATION = "temp_skip_delete_confirmation"
const val BOTTOM_ACTIONS = "bottom_actions"
const val VISIBLE_BOTTOM_ACTIONS = "visible_bottom_actions"
const val WERE_FAVORITES_PINNED = "were_favorites_pinned"
const val WAS_RECYCLE_BIN_PINNED = "was_recycle_bin_pinned"
const val USE_RECYCLE_BIN = "use_recycle_bin"
const val GROUP_BY = "group_by"
const val EVER_SHOWN_FOLDERS = "ever_shown_folders"
const val SHOW_RECYCLE_BIN_AT_FOLDERS = "show_recycle_bin_at_folders"
const val SHOW_RECYCLE_BIN_LAST = "show_recycle_bin_last"
const val ALLOW_ZOOMING_IMAGES = "allow_zooming_images"
const val WAS_SVG_SHOWING_HANDLED = "was_svg_showing_handled"
const val LAST_BIN_CHECK = "last_bin_check"
const val SHOW_HIGHEST_QUALITY = "show_highest_quality"

// slideshow
const val SLIDESHOW_INTERVAL = "slideshow_interval"
const val SLIDESHOW_INCLUDE_PHOTOS = "slideshow_include_photos"
const val SLIDESHOW_INCLUDE_VIDEOS = "slideshow_include_videos"
const val SLIDESHOW_INCLUDE_GIFS = "slideshow_include_gifs"
const val SLIDESHOW_RANDOM_ORDER = "slideshow_random_order"
const val SLIDESHOW_USE_FADE = "slideshow_use_fade"
const val SLIDESHOW_MOVE_BACKWARDS = "slideshow_move_backwards"
const val SLIDESHOW_LOOP = "loop_slideshow"
const val SLIDESHOW_DEFAULT_INTERVAL = 5
const val SLIDESHOW_SCROLL_DURATION = 500L

const val NOMEDIA = ".nomedia"
const val FAVORITES = "favorites"
const val RECYCLE_BIN = "recycle_bin"
const val SHOW_FAVORITES = "show_favorites"
const val SHOW_RECYCLE_BIN = "show_recycle_bin"
const val MAX_COLUMN_COUNT = 20
const val SHOW_TEMP_HIDDEN_DURATION = 300000L
const val CLICK_MAX_DURATION = 150
const val DRAG_THRESHOLD = 8
const val MONTH_MILLISECONDS = MONTH_SECONDS * 1000L
const val HIDE_PLAY_PAUSE_DELAY = 500L
const val PLAY_PAUSE_VISIBLE_ALPHA = 0.8f
const val MIN_SKIP_LENGTH = 2000

const val DIRECTORY = "directory"
const val MEDIUM = "medium"
const val PATH = "path"
const val GET_IMAGE_INTENT = "get_image_intent"
const val GET_VIDEO_INTENT = "get_video_intent"
const val GET_ANY_INTENT = "get_any_intent"
const val SET_WALLPAPER_INTENT = "set_wallpaper_intent"
const val IS_VIEW_INTENT = "is_view_intent"
const val PICKED_PATHS = "picked_paths"

// rotations
const val ROTATE_BY_SYSTEM_SETTING = 0
const val ROTATE_BY_DEVICE_ROTATION = 1
const val ROTATE_BY_ASPECT_RATIO = 2

// view types
const val VIEW_TYPE_GRID = 1
const val VIEW_TYPE_LIST = 2

// extended details values
const val EXT_NAME = 1
const val EXT_PATH = 2
const val EXT_SIZE = 4
const val EXT_RESOLUTION = 8
const val EXT_LAST_MODIFIED = 16
const val EXT_DATE_TAKEN = 32
const val EXT_CAMERA_MODEL = 64
const val EXT_EXIF_PROPERTIES = 128
const val EXT_DURATION = 256
const val EXT_ARTIST = 512
const val EXT_ALBUM = 1024

// media types
const val TYPE_IMAGES = 1
const val TYPE_VIDEOS = 2
const val TYPE_GIFS = 4
const val TYPE_RAWS = 8
const val TYPE_SVGS = 16

const val LOCAITON_INTERNAL = 1
const val LOCATION_SD = 2
const val LOCATION_OTG = 3

const val GROUP_BY_NONE = 1
const val GROUP_BY_LAST_MODIFIED = 2
const val GROUP_BY_DATE_TAKEN = 4
const val GROUP_BY_FILE_TYPE = 8
const val GROUP_BY_EXTENSION = 16
const val GROUP_BY_FOLDER = 32
const val GROUP_DESCENDING = 1024

// bottom actions
const val BOTTOM_ACTION_TOGGLE_FAVORITE = 1
const val BOTTOM_ACTION_EDIT = 2
const val BOTTOM_ACTION_SHARE = 4
const val BOTTOM_ACTION_DELETE = 8
const val BOTTOM_ACTION_ROTATE = 16
const val BOTTOM_ACTION_PROPERTIES = 32
const val BOTTOM_ACTION_CHANGE_ORIENTATION = 64
const val BOTTOM_ACTION_SLIDESHOW = 128
const val BOTTOM_ACTION_SHOW_ON_MAP = 256
const val BOTTOM_ACTION_TOGGLE_VISIBILITY = 512
const val BOTTOM_ACTION_RENAME = 1024
const val BOTTOM_ACTION_SET_AS = 2048

const val DEFAULT_BOTTOM_ACTIONS = BOTTOM_ACTION_TOGGLE_FAVORITE or BOTTOM_ACTION_EDIT or BOTTOM_ACTION_SHARE or BOTTOM_ACTION_DELETE
