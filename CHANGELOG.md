Changelog
==========

Version 6.6.1 *(2019-03-21)*
----------------------------

 * Fixed recognizing of some SD cards
 * Added some stability and translation improvements

Version 6.6.0 *(2019-03-10)*
----------------------------

 * Further improved new file discovery
 * Exclude some folders by default, like fb stickers
 * Added other stability and ux improvements

Version 6.5.5 *(2019-03-05)*
----------------------------

 * Improve new file discovery
 * Fixed some third party intent related glitches
 * Fixed some issues related to grouping thumbnails
 * Added a note at Sorting and Grouping dialog to avoid some confusion
 * Avoid deleting filtered out file types at deleting folders

Version 6.5.4 *(2019-02-28)*
----------------------------

 * Allow customizing file loading priority
 * Fixed the handling of some image picker intents
 * Speeded up renaming files on SD cards
 * Added some file loading performance improvements
 * Some stability improvements

Version 6.5.3 *(2019-02-22)*
----------------------------

 * Added main screen menu buttons for fast Recycle Bin showing/hiding
 * Added a new setting item for changing date and time format
 * Do not shuffle images with Random sorting that often
 * Fixed some glitches related to file rename, delete, move
 * Added many smaller bugfixes and UX improvements

Version 6.5.2 *(2019-02-16)*
----------------------------

 * Added an option to disable rotating fullscreen images with gestures
 * Improved OTG usb device handling
 * Fixed Grouping by date taken
 * Improved the performance at deleting files from the fullscreen view
 * Some stability improvements

Version 6.5.1 *(2019-02-11)*
----------------------------

 * Fixed a glitch with image panning
 * Added a couple stability improvements

Version 6.5.0 *(2019-02-07)*
----------------------------

 * Allow rotating fullscreen images with gestures, if "Allow deep zooming images" option is enabled
 * Zoom out videos and gifs after device rotation

Version 6.4.1 *(2019-01-29)*
----------------------------

 * Fixed some crashes related to zoomable videos
 * Disable the Close Down gesture at GIFs and videos, if they are zoomed in

Version 6.4.0 *(2019-01-29)*
----------------------------

 * Implemented export/importing for app settings and other preferences, like sorting
 * Allow hiding Notch on fullscreen view on Android 9+
 * Some gif/video zoom related improvements
 * Autosave images zoomed at the fullscreen view
 * Many other UX and stability improvements

Version 6.3.2 *(2019-01-23)*
----------------------------

 * Fixed some fullscreen image and gif issues related to zooming
 * Show directly included folders even if they contain a .nomedia file

Version 6.3.1 *(2019-01-22)*
----------------------------

 * Fixed fullscreen images crashing when the app was installed on an SD card
 * A couple other fullscreen image viewer improvements
 * Allow batch rotating only images, ignore other file types

Version 6.3.0 *(2019-01-17)*
----------------------------

 * Allow zooming GIFs and videos
 * Allow sharing images directly from the editor
 * Allow drawing in the editor
 * If a folder is directly excluded, make it a higher priority than some included parent folder
 * Added batch rotating from the thumbnails view
 * Many other smaller improvements

Version 6.2.2 *(2019-01-10)*
----------------------------

 * Reverted to the old way of playing videos, opening them on a separate screen can be enabled in the app settings
 * Added some memory related improvements at displaying fullscreen images
 * Allow showing videos in slideshows

Version 6.2.1 *(2019-01-08)*
----------------------------

 * Fixed some menu buttons at the video player activity
 * Added buttons to the videoplayer for going to the previous/next item
 * Allow pressing play/pause at the video player at fullscreen mode
 * Properly retain exif values after editing a file, when overwriting the source file

Version 6.2.0 *(2019-01-04)*
----------------------------

 * Rewrote video playback, use a separate screen + added fast-forwarding with horizontal swiping
 * Added optional 1:1 pixel ratio zooming with two double taps at fullscreen view
 * Allow adding Copy at the fullscreen bottom actions
 * Always include images at slideshows, never videos
 * Fixed scanning of some predefined folders for images
 * Some other stability/performance/translation improvements

Version 6.1.3 *(2018-12-26)*
----------------------------

 * Fixed a glitch at zooming fullscreen images with double tap
 * Hide favorite items from hidden folders, if showing hidden items is disabled

Version 6.1.2 *(2018-12-24)*
----------------------------

 * Done a few performance improvements here and there
 * Allow changing view type individually per folder
 * Merry Christmas!

Version 6.1.1 *(2018-12-18)*
----------------------------

 * Fixing some crashes

Version 6.1.0 *(2018-12-17)*
----------------------------

 * Added an initial widget implementation for creating homescreen folder shortcuts
 * Added optional grouping of direct subfolders, as a check at the "Change view type" dialog
 * Added an option to set custom crop aspect ratio at the editor
 * Save exif data at edited files on Android 7+
 * Handle only Mass Storage USB devices, ignore the rest
 * Many other smaller UX/stability/performance improvements

Version 6.0.4 *(2018-12-04)*
----------------------------

 * Limit automatic spam folder exclusion to the "/Android/data" folder

Version 6.0.3 *(2018-12-02)*
----------------------------

 * Added multiple predefined aspect ratios at the Editor + remember the last used ratio
 * Fix some issue with deleted items not appearing in the Recycle Bin, causing the app to take up too much space
 * At delete/copy/move operations on folders apply them only on the visible files, take filters/hiding into account
 * Do not exclude whole Data folder by default, be smarter about filtering out spam folders
 * Added support for Sony RAW ".arw" files
 * Optimize video duration fetching at thumbnails

Version 6.0.2 *(2018-11-19)*
----------------------------

 * Adding a crashfix related to showing video duration

Version 6.0.1 *(2018-11-19)*
----------------------------

 * Added optional displaying video duration on thumbnails
 * Fixed keeping last_modified value at copy/move in some cases
 * Exclude the Data folder by default
 * Many translation, UX and stability improvements

Version 6.0.0 *(2018-11-04)*
----------------------------

 * Initial Pro version

Version 5.1.4 *(2018-11-28)*
----------------------------

 * Make sure the "Upgrade to Pro" popup isn't shown at first launch
 * This version of the app is no longer maintained, please upgrade to the Pro version. You can find the Upgrade button at the top of the app Settings.

Version 5.1.3 *(2018-11-04)*
----------------------------

 * Adding an option to store last video playback position (by mathevs)
 * Adding a "Keep both" conflict resolution at copy/move (by Doubl3MM)
 * Improved panoramic video detection
 * Remove some glitches related to third party file opening
 * Do not exclude the Data folder by default
 * Removed the "Avoid showing Whats New at app startup" option

Version 5.1.2 *(2018-10-30)*
----------------------------

 * Added a new option for password protecting file deletion/move
 * Improved panorama video detection
 * Improved the opening of media files without file extension
 * Disabled move operation on Recycle bin items, use Restore
 * Fixed handling of some third party image picker intents
 * Fixed slideshow looping and a couple other UX glitches
 * Improved the stability of retrieving cached files
 * Hi

Version 5.1.1 *(2018-10-23)*
----------------------------

 * Fixing the inability to delete SD card files

Version 5.1.0 *(2018-10-23)*
----------------------------

 * Added support for panorama videos
 * Added an extra check to avoid trying deleting files without write permission
 * Added an initial implementation of renaming multiple items at once
 * Added some performance improvements at item de/selection
 * Allow enabling hidden item visibility at the copy/move destination picker
 * Allow closing fullscreen view with swipe down gestures (can be disabled in settings)
 * Fixed a glitch with Favorite items getting unselected every day
 * Fixed exposure time displayed at long exposure photos
 * Fixed fullscreen images being sometimes totally zoomed in after device rotation
 * Fixed slideshow direction
 * Made loading initial fullscreen view quicker and fullscreen toggling more reliable
 * Not sure what else, nobody reads this anyway

Version 5.0.1 *(2018-10-17)*
----------------------------

 * Adding some crashfixes

Version 5.0.0 *(2018-10-17)*
----------------------------

 * Increased the minimal required Android OS version to 5 (Lollipop)
 * Rewrote file selection for more robustness
 * Added a new option for showing the Recycle Bin as the last folder
 * Added Search for searching folders by names
 * Replaced the G+ button with Reddit
 * Couple smaller glitch fixes and improvements

Version 4.6.5 *(2018-10-02)*
----------------------------

 * Added notch support for Android 9
 * Allow faster video seeking by dragging a finger at the bottom seekbar
 * Use a different way of displaying fullscreen GIFs
 * Added a new toggle for trying to show the best possible image quality
 * Keep Favorite items marked after moving
 * Fixed some glitches related to toggling fullscreen mode
 * Many other smaller improvements

Version 4.6.4 *(2018-09-22)*
----------------------------

 * Fixed lag at zooming fullscreen images on some devices

Version 4.6.3 *(2018-09-21)*
----------------------------

 * Improved zooming performance at fullscreen view
 * Fixed showing conflict resolution dialog at Move
 * Fixed selection check icons at horizontal scrolling
 * Fixed displaying some fullscreen images, where file path contained percentage sign or hashtag
 * Optimized many database operations
 * Fixed many other smaller issues

Version 4.6.2 *(2018-09-05)*
----------------------------

 * Fixed opening some email client attachments and MMS images
 * Attempt to fix lagging at zooming in on some devices
 * Couple other smaller bugfixes and improvements

Version 4.6.1 *(2018-08-21)*
----------------------------

 * Added a crashfix at loading fullscreen images

Version 4.6.0 *(2018-08-20)*
----------------------------

 * Added support for SVGs
 * Improved fullscreen image quality and performance
 * Properly show files with hastags and percentage signs in their paths
 * Many other smaller UX improvements

Version 4.5.2 *(2018-08-08)*
----------------------------

 * Adding a toggle for disabling deep zoomable images
 * Fix displaying third party images
 * Couple smaller UX fixes

Version 4.5.1 *(2018-08-07)*
----------------------------

 * Adding a crashfix

Version 4.5.0 *(2018-08-07)*
----------------------------

 * Use real Move instead of the old copy/delete if both source and destination are on the internal storage
 * Remake the fullscreen view, always use deep zoomable images with good quality
 * Couple stability improvements

Version 4.4.4 *(2018-08-02)*
----------------------------

 * Adding a crashfix

Version 4.4.3 *(2018-08-01)*
----------------------------

 * Removed the More Donating Options from the Purchase Thank You dialog

Version 4.4.2 *(2018-08-01)*
----------------------------

 * Removed the homepage from About section

Version 4.4.1 *(2018-07-30)*
----------------------------

 * Hide both Play and Pause video buttons after 2 secs
 * Improved Immersive mode fullscreen behaviour
 * Some other stability improvements

Version 4.4.0 *(2018-07-26)*
----------------------------

 * Reworked the editor, added some filters
 * Allow hiding the Recycle Bin from the main screen folders
 * Added a menu item for fixing file Date Taken
 * Fixed some glitches around recycle bin item restoring
 * Fixed some issues with video playback on resume
 * Many other UX and stability improvements

Version 4.3.5 *(2018-07-17)*
----------------------------

 * Fixed some Recycle bin related issues
 * A few more UX and stability improvements

Version 4.3.4 *(2018-07-15)*
----------------------------

 * Fixed disappearing launcher icon after changing its color on some devices
 * Fixed some video related errors
 * Added "Set as" as an available action at the fullscreen bottom actions
 * Do the appropriate actions at trying to delete the Recycle Bin or Favorites folders
 * Fixed a glitch with some panorama images not recognized properly
 * Avoid blank screen at toggling "Temporarily show hidden"

Version 4.3.3 *(2018-07-06)*
----------------------------

 * Couple stability improvements and glitch fixes

Version 4.3.2 *(2018-07-04)*
----------------------------

 * Added Panorama photo support
 * Allow customizing visible fullscreen bottom actions
 * Allow forcing portrait/landscape modes at fullscreen view
 * Use Exoplayer for playing videos
 * Many smaller UX and stability improvements

Version 4.3.1 *(2018-06-28)*
----------------------------

 * Adding some crashfixes

Version 4.3.0 *(2018-06-28)*
----------------------------

 * Added a Recycle Bin
 * Allow grouping media thumbnails by different criteria
 * Fixed some calculation glitches around fastscroller
 * Change the fullscreen Edit icon to a pencil
 * Allow sorting "Show All" separately
 * Many smaller stability and UX improvements

Version 4.2.1 *(2018-06-20)*
----------------------------

 * Allow selecting Favorite items for easy access
 * Fix sorting by Date Taken after files have been copied
 * Couple other stability and UX improvements

Version 4.2.0 *(2018-06-18)*
----------------------------

 * Move some actions at the fullscreen view to the bottom of the screen
 * Allow filtering out RAW images separately
 * Add a warning if the user tries deleting a folder
 * Properly reset the temporary Skip Delete Confirmation dialog
 * Show a Pause button over video if not in fullscreen mode
 * Fix some glitches around inserting pin/pattern/fingerprint
 * Many other stability and ux improvements

Version 4.1.1 *(2018-05-26)*
----------------------------

 * Always set folder thumbnail based on folder content sorting
 * Make sure hidden folders have the "(hidden)" appended

Version 4.1.0 *(2018-05-25)*
----------------------------

 * Added sorting by Date Taken
 * Fixed file renaming on Android Oreo
 * Fixed some scrollbar glitches
 * Fixed broken "Use english language" in some cases
 * Make sure only the proper files are shown at "Show all folders content"
 * Many other smaller UX, stability improvements and bugfixes

Version 4.0.0 *(2018-05-13)*
----------------------------

 * Allow customizing the app launcher color
 * Remove the top spinning circle at initial launch
 * Many other bugfixes and UX/stability improvements

Version 3.8.2 *(2018-04-26)*
----------------------------

 * Rewrote media caching and new file discovery for better performance
 * Many OTG file handling improvements

Version 3.8.1 *(2018-04-24)*
----------------------------

 * Rewrote media caching and new file discovery for better performance
 * Some OTG file handling improvements

Version 3.8.0 *(2018-04-22)*
----------------------------

 * Rewrote media caching for better performance
 * Cache all media items, not just 80 per folder
 * Some additional performance and stability improvements

Version 3.7.3 *(2018-04-15)*
----------------------------

 * Show hidden folders when appropriate

Version 3.7.2 *(2018-04-14)*
----------------------------

 * Fix Edit intent handled by other apps
 * Hide folders containing ".nomedia" file, even if explicitly included
 * Remove sorting by Date Taken until proper implementation

Version 3.7.1 *(2018-04-12)*
----------------------------

 * Fix no media being shown to some people
 * Fix some glitches at renaming files
 * Show a count of files being deleted at the confirmation prompt

Version 3.7.0 *(2018-04-10)*
----------------------------

 * Rewrote media file fetching for better performance and new item discovering
 * Make un/hiding folders quicker
 * Make automatic fullscreen toggling animation smoother by delaying it
 * Many other smaller performance and UX improvements

Version 3.6.3 *(2018-03-30)*
----------------------------

 * Couple file scanning and thumbnail displaying updates
 * Show a dialog about the new Simple Clock app to some users

Version 3.6.2 *(2018-03-23)*
----------------------------

 * Fixing some crashes related to file scanning
 * Do not scan Download folder file by file, it can contain many items

Version 3.6.1 *(2018-03-22)*
----------------------------

 * Set proper file mimetype after editing or other file operation
 * Try scanning Screenshots and Download folders more thoroughly
 * Couple stability improvements

Version 3.6.0 *(2018-03-15)*
----------------------------

 * Fix duplicate files at renaming or hiding
 * Improve some third party handling
 * Optimize rotated image saving to avoid Out of memory errors
 * Optimize new item discoveries or folder refreshing
 * Many other smaller performance and UX improvements

Version 3.5.3 *(2018-03-03)*
----------------------------

 * Properly keep last-modified at file copy/move if set so
 * Misc other smaller glitch and translation improvements

Version 3.5.2 *(2018-02-25)*
----------------------------

 * Fixed third party intent uri generation
 * Properly handle files with colon in filename
 * Fix copying whole folders

Version 3.5.1 *(2018-02-25)*
----------------------------

 * Added a toggle for disabling pull-to-refresh
 * Added a toggle for permanent Delete confirmation dialog skipping
 * Fixed saving image after editing

Version 3.5.0 *(2018-02-20)*
----------------------------

 * Added copy/move progress notification
 * Fixed some glitches a round rotating media by aspect ratio
 * Added FAQ
 * Make explicit folder inclusion recursive
 * Added initial OTG device support
 * Rewrote third party intent handling and file handling, fixed some bugs along the way
 * Probably added some new bugs

Version 3.4.1 *(2018-02-09)*
----------------------------

 * Fix some glitches around swiping fullscreen media with instant media switch or gesture brightness change enabled
 * Make changing image brightness with gestures disabled by default
 * Allow skipping forward/backward videos by pressing max/current time
 * Fix some cases of editing third party images
 * Couple other stability improvements

Version 3.4.0 *(2018-02-05)*
----------------------------

 * Allow changing the brightness by vertical gestures on images (by trubitsyn)
 * Properly fetch all media files from recognized folders
 * Make thumbnail info on the main screen a bit easier to read
 * Fix seeing blank thumbnail after deleting files in some cases
 * Reset zoom level on orientation change at fullscreen media
 * Add an optional extra check to avoid showing invalid files
 * Add a toggle to prevent showing What's new on startup
 * Many other stability and performance improvements

Version 3.3.1 *(2018-01-29)*
----------------------------

 * Added a toggle for replacing deep zoomable images with better quality ones
 * Added a toggle for hiding Extended details when the statusbar is hidden
 * Added a toggle for switching media files by clicking on screen sides
 * Disable "Temporarily show hidden" after 10 minutes of backgrounding
 * Split Settings in separate sections

Version 3.3.0 *(2018-01-23)*
----------------------------

 * Added optional one-finger drag zoom at fullscreen media (by gh123man)
 * Allow opening the app even without any media files (by gh123man)
 * Refresh media files in the background when Simple Camera creates a new photo/video
 * Improve fullscreen media rotation by "Device Rotation"

Version 3.2.4 *(2018-01-17)*
----------------------------

 * An F-droid build only, trying to add screenshots there

Version 3.2.3 *(2018-01-14)*
----------------------------

 * An F-droid build only, fixing a compile error

Version 3.2.2 *(2018-01-09)*
----------------------------

 * Some scrolling issues fixed
 * Improve new media file discovery

Version 3.2.1 *(2018-01-08)*
----------------------------

 * Adding a crashfix
 * Couple scrollbar glitch fixes

Version 3.2.0 *(2018-01-07)*
----------------------------

 * Rewrote scrolling to improve the performance
 * Disable "Delete empty folders" by default
 * Added initial Search to media thumbnails screen
 * Apply the hidden folder password protection to "Manage hidden folders"
 * Replace Move with Copy/Delete on Android 7+
 * Improve SD card file support

Version 3.1.2 *(2017-12-30)*
----------------------------

 * Fixed some video related crashes

Version 3.1.1 *(2017-12-29)*
----------------------------

 * Added a new setting item for managing folders hidden with .nomedia
 * Speed up image loading
 * Use copy/delete instead of move on Android 8.x
 * Improved double-tap zoom ratios

Version 3.1.0 *(2017-12-25)*
----------------------------

 * Fixed some issues around picking contact images
 * Misc other improvements

Version 3.0.3 *(2017-12-20)*
----------------------------

 * Added a new Black & White theme with special handling
 * Fixed opening MMS attachments
 * Fixed viewing properties/sharing etc at fullscreen media
 * Apply "Dark background at fullscreen media" to the status bar too
 * Misc performance/stability improvements

Version 3.0.2 *(2017-12-17)*
----------------------------

 * Properly display email attachments
 * Some crashfixes

Version 3.0.1 *(2017-12-06)*
----------------------------

 * Fix missing launcher icon on some devices
 * Added an info bubble at scrolling by dragging
 * Allow zooming gifs
 * Display raw .dng files

Version 3.0.0 *(2017-12-04)*
----------------------------

 * Improved primary color customization
 * Allow setting home and lock screen wallpapers separately on Android 7+
 * Many smaller performance and stability improvements

Version 2.19.0 *(2017-11-23)*
----------------------------

 * Rolled back to displaying images in RGB_565 quality for proper zoom and performance
 * Load directory thumbnails faster if a new medium has been discovered
 * Couple performance and stability improvements

Version 2.18.1 *(2017-11-16)*
----------------------------

 * Fixed some double-tap zoom issues
 * Misc smaller fixes and improvements here and there

Version 2.18.0 *(2017-11-09)*
----------------------------

 * Added an option to use english language on non-english devices
 * Added an option to password protect the whole app
 * Added an option to lock screen orientation at fullscreen view
 * Split the Rotate button to 3 buttons per degrees
 * Changed the way fullscreen images are loaded for better quality
 * Fixed many memory leaks and smaller issues

Version 2.17.4 *(2017-11-06)*
----------------------------

 * Fixed some third party intent handling
 * Increased max columns count to 20
 * Allow rotating JPGs in a lossless way (by ltGuillaume)

Version 2.17.3 *(2017-11-02)*
----------------------------

 * Fixed some corrupt gif file related crashes
 * Rotate jpgs on the internal storage by exif
 * Fixed some invisible SD card content

Version 2.17.2 *(2017-10-29)*
----------------------------

 * Couple more minor fixes

Version 2.17.1 *(2017-10-29)*
----------------------------

 * Show "Set As" and "Edit" menu buttons at videos and gifs too
 * Couple other smaller issues fixed

Version 2.17.0 *(2017-10-28)*
----------------------------

 * Added a toggle for keeping last-modified field at file copy/move/rename
 * Improved GIF animation speed
 * Implemented fileprovider support to third party intents
 * Make rotation by "Device rotation" less sensitive
 * Automatically append "_1" to filename after saving through the Editor
 * Added support for Adaptive icons for Android 8 (by fiepi)
 * Added Dutch translation (by ltGuillaume)
 * Many other smaller improvements

Version 2.16.1 *(2017-10-24)*
----------------------------

 * Added a toggle for hiding folder media count on the main screen
 * Fixed SD card folders not being visible on some devices
 * Fixed videos not playing properly in some cases
 * Do not modify last_modified at copy/move/rename
 * Added support for 3gpp videos

Version 2.16.0 *(2017-10-19)*
----------------------------

 * Added sorting by path
 * Added an option to show customizable extended details over fullscreen media
 * Allow selecting Album cover photos from any folders
 * Added a checkbox for skipping Delete confirmation dialog

Version 2.15.2 *(2017-10-06)*
----------------------------

 * Properly display SD card content to Android 4 users
 * Fix displaying some third party media, like Bluemail attachments
 * Fix media picking intents if "Show all folders content" is enabled

Version 2.15.1 *(2017-10-01)*
----------------------------

 * Updated commons library with minor fixes

Version 2.15.0 *(2017-10-01)*
----------------------------

 * Added fingerprint to hidden item protection
 * Added a new List view type
 * Fixed an issue with some hidden items being shown at "Show all folders content"
 * Fixed typing in color hex codes manually with some keyboards
 * Do not autosave rotated images in any case
 * Tons of other performance, stability and UX improvements

Version 2.14.4 *(2017-09-18)*
----------------------------

 * Revert to the old way of loading fullscreen images to avoid issues on Android 7+

Version 2.14.3 *(2017-09-17)*
----------------------------

 * Removed some error toast messages after delete, or if image loading failed
 * Fixed some visual glitches at horizontal scrolling
 * Disable pull-to-refresh at horizontal scrolling
 * Many other smaller bugfixes and improvements

Version 2.14.2 *(2017-09-11)*
----------------------------

 * Fixing some glitches with fullscreen images
 * Add an extra check to avoid displaying non-existing media
 * Fix opening media from third party intents

Version 2.14.1 *(2017-09-07)*
----------------------------

 * Fixing some glitches around fullscreen view

Version 2.14.0 *(2017-09-05)*
----------------------------

 * Simplified the way of creating new folders
 * Added a loop option to slideshows, slowed down the swipe animation
 * Added an option to filter out gifs from slideshows
 * Improved the quality of fullscreen images
 * Properly allow excluding the root folder

Version 2.13.4 *(2017-09-01)*
----------------------------

 * Improved the image loading performance
 * Added a switch for disabling video gestures
 * Added a switch for deleting empty folders after deleting content
 * Show excluded folder content at third party intent if needed

Version 2.13.3 *(2017-08-29)*
----------------------------

 * Fixing copy/move actions on some devices

Version 2.13.2 *(2017-08-28)*
----------------------------

 * Moved media type filter from Settings to the Action menu
 * Allow filtering GIFs out
 * Make sure we always show manually included folders
 * Properly show hidden files, when open from some File Manager

Version 2.13.1 *(2017-08-16)*
----------------------------

 * Show a folder if its both excluded and included
 * Many translation improvements

Version 2.13.0 *(2017-08-07)*
----------------------------

 * Allow changing the screen brightness and volume at videos by vertically dragging the screen sides
 * Fixed sorting of numbers in filenames
 * Fixed a glitch with hidden files sometimes temporarily visible
 * Unified thumbnail corner icon sizes and style

Version 2.12.6 *(2017-08-05)*
----------------------------

 * Added slideshow at the fullscreen view
 * Replaced the foreground color of selected items with a check
 * Made copy/move to SD card a lot faster

Version 2.12.5 *(2017-08-03)*
----------------------------

 * Updating file operation on SD card

Version 2.12.4 *(2017-08-03)*
----------------------------

 * Fixed SD card file operations

Version 2.12.3 *(2017-08-02)*
----------------------------

 * Added pattern/pin protection for showing hidden items
 * Hopefully fixed unintentional SD card file deleting

Version 2.12.2 *(2017-07-09)*
----------------------------

 * Added a toggle for replacing Share with Rotate at fullscreen media
 * Some crashfixes and translation updates

Version 2.12.1 *(2017-07-02)*
----------------------------

 * Couple crashfixes

Version 2.12.0 *(2017-07-01)*
----------------------------

 * Added a button for disabling "Temporarily show hidden"
 * Updated Glide (library used for loading images) to 4.0.0
 * Made playing gifs smooth

Version 2.11.5 *(2017-06-29)*
----------------------------

 * Added an indicator of folders located on SD cards
 * Improved the way of rotating jpg images on the internal storage by modifying the exif tags + added autosave

Version 2.11.4 *(2017-06-26)*
----------------------------

 * Added an option for automatically hiding the system UI at entering fullscreen mode
 * Fix deleting SD card files on some devices
 * Couple crashfixes

Version 2.11.3 *(2017-06-24)*
----------------------------

 * Added optional horizontal scrolling

Version 2.11.1 *(2017-06-19)*
----------------------------

 * Fixed a crash at starting video

Version 2.11.0 *(2017-06-18)*
----------------------------

 * Store column count separately for portrait and landscape modes
 * Improve zooming at double taping fullscreen images
 * Allow opening a third party editor from our Editor screen
 * Many crashfixes and smaller improvements

Version 2.10.10 *(2017-06-07)*
----------------------------

 * Some crashfixes

Version 2.10.9 *(2017-06-06)*
----------------------------

 * Allow setting custom folder covers
 * Properly handle manually included folders
 * Improve the performance at opening fullscreen media

Version 2.10.8 *(2017-06-02)*
----------------------------

 * Always properly show hidden files from third party intents
 * Properly handle Crop intent used for example at selecting contact photos
 * Couple smaller fixes and crashfixes

Version 2.10.7 *(2017-05-29)*
----------------------------

 * Show hidden folders when they should be shown
 * Add an overwrite confirmation dialog when replacing the original image with edited
 * Reuse the list of media at fullscreen view from thumbnails, increasing performance
 * Some crashfixes

Version 2.10.6 *(2017-05-26)*
----------------------------

 * Properly show hidden media when appropriate
 * Some crashfixes

Version 2.10.5 *(2017-05-25)*
----------------------------

 * Check new files or folders periodically
 * Try zooming in just to fit the screen on doubleclick
 * Do not show excluded and hidden files at All Folders Content view

Version 2.10.4 *(2017-05-21)*
----------------------------

 * Hide subfolders of folders with .nomedia too

Version 2.10.3 *(2017-05-21)*
----------------------------

 * Really hide hidden folders when appropriate

Version 2.10.2 *(2017-05-21)*
----------------------------

 * Properly hide .nomedia folders when appropriate

Version 2.10.1 *(2017-05-21)*
----------------------------

 * Catch exceptions thrown during media obtaining

Version 2.10.0 *(2017-05-20)*
----------------------------

 * Rewrite the way of fetching media, should improve performance
 * Fix sorting by Date Taken

Version 2.9.1 *(2017-05-14)*
----------------------------

 * Allow selecting multiple items by finger dragging
 * Added an option to always use black background at fullscreen media
 * Rewrite selecting thumbnails to make it more reliable
 * Not sure what else, nobody reads this anyway

Version 2.9.0 *(2017-04-26)*
----------------------------

 * Allow hiding individual files by prepending filenames with a dot
 * Fix setting wallpaper on some devices
 * Preload 2 images per side in fullscreen mode
 * Fixed many memory leaks
 * Try fetching directories right after app install, before first launch
 * Many other bugfixes and improvements

Version 2.8.6 *(2017-04-17)*
----------------------------

 * Fixing a crash at getting the sd card path

Version 2.8.5 *(2017-04-17)*
----------------------------

 * Added horizontal and vertical image flipping in the image editor
 * Fixed an issue with fullscreen image often not appearing
 * Fixed deleting files from SD cards on some devices
 * Fixed some memory leaks and other smaller issues

Version 2.8.4 *(2017-04-13)*
----------------------------

 * Allow setting portrait wallpapers
 * Added more fields in photo Properties dialog
 * Try fixing the issue with not appearing fullscreen images to some people
 * Couple crashfixes

Version 2.8.3 *(2017-04-04)*
----------------------------

 * Fix displaying folder thumbnails

Version 2.8.2 *(2017-04-03)*
----------------------------

 * Fix folder sorting

Version 2.8.1 *(2017-04-03)*
----------------------------

 * Allow more indepth zoom at fullscreen images
 * Improved thumbnail caching to make sure its up to date
 * Couple other smaller improvements

Version 2.8.0 *(2017-04-01)*
----------------------------

 * Improved fullscreen image zooming
 * Added more settings related to screen autorotating
 * Split copy and move functions for ease of use
 * Fixed gif sharing

Version 2.7.4 *(2017-03-25)*
----------------------------

 * Added an option to temporarily show hidden folders
 * Added a setting for preventing thumbnail croping to square
 * Added a setting for auto-rotating the screen depending on photo aspect ratio

Version 2.7.3 *(2017-03-18)*
----------------------------

 * Improved the fullscreen images loading
 * Updated the launcher icon

Version 2.7.2 *(2017-03-18)*
----------------------------

 * Added an option to use max brightness at viewing fullscreen media
 * Added an option to manually include folders which contain media, but were not recognized by the app
 * Many improvements around SD card detecting and file operations
 * Replaced Glide with Picasso at loading most thumbnails and fullscreen images
 * Many other optimizations and improvements

Version 2.7.1 *(2017-03-18)*
----------------------------

 * Added an option to use max brightness at viewing fullscreen media
 * Added an option to manually include folders which contain media, but were not recognized by the app
 * Many improvements around SD card detecting and file operations
 * Replaced Glide with Picasso at loading most thumbnails and fullscreen images
 * Many other optimizations and improvements

Version 2.7.0 *(2017-03-18)*
----------------------------

 * Added an option to use max brightness at viewing fullscreen media
 * Added an option to manually include folders which contain media, but were not recognized by the app
 * Many improvements around SD card detecting and file operations
 * Replaced Glide with Picasso at loading most thumbnails and fullscreen images
 * Many other optimizations and improvements

Version 2.6.6 *(2017-03-09)*
----------------------------

 * Fixed some editor and sharing issues

Version 2.6.5 *(2017-03-07)*
----------------------------

 * Fix some file uri issues below android 7

Version 2.6.4 *(2017-03-06)*
----------------------------

 * Some folder exclusion and Android 7 related fixes

Version 2.6.3 *(2017-03-06)*
----------------------------

 * Adding one more crashfix

Version 2.6.2 *(2017-03-05)*
----------------------------

 * Fixing some issues, mostly related to Android 7

Version 2.6.1 *(2017-03-05)*
----------------------------

 * Fix a video playing crash at Android Nougat

Version 2.6.0 *(2017-03-04)*
----------------------------

 * Exclude the subfolders of excluded folders too
 * Added an easy way of excluding parent folders from the exclude confirmation dialog
 * Added draggable scrollbars
 * Allow setting a third party video player as the default
 * Many other bugfixes and improvements

Version 2.5.3 *(2017-03-01)*
----------------------------

 * Added an option to toggle autoplaying gifs at thumbnails

Version 2.5.2 *(2017-02-28)*
----------------------------

 * Allow setting different sorting per folder
 * Couple changes to make the copy/move progress easier
 * Misc smaller fixes

Version 2.5.1 *(2017-02-27)*
----------------------------

 * Couple performance improvements and bugfixes

Version 2.5.0 *(2017-02-26)*
----------------------------

 * Rethink the way of obtaining images/videos
 * Use the proper way of hiding folders, by using .nomedia
 * Add a section in Settings for managing excluded folders

Version 2.4.0 *(2017-02-20)*
----------------------------

 * Improve the performance of loading the images, deleting and renaming
 * Couple other bug and crashfixes

Version 2.3.9 *(2017-02-17)*
----------------------------

 * Add a rotate option at fullscreen view
 * Attempt to fix delete issues from some devices with SD cards

Version 2.3.8 *(2017-02-10)*
----------------------------

 * Allow easily changing the column count
 * Misc performance improvements and bugfixes

Version 2.3.7 *(2017-02-09)*
----------------------------

 * Fixed some issues with inability to delete files from SD card
 * Some crashfixes

Version 2.3.6 *(2017-02-07)*
----------------------------

 * Some crashfixes

Version 2.3.5 *(2017-01-23)*
----------------------------

 * Allow selecting colors by hex codes
 * Added a button for restoring default colors

Version 2.3.4 *(2017-01-22)*
----------------------------

 * Allow showing photos and videos on the map, if coords are available
 * Some crashfixes

Version 2.3.3 *(2017-01-16)*
----------------------------

 * Fix a rare bug which could remove unwanted folders from SD card
 * Remove the Source field from copy dialog to make it simpler

Version 2.3.2 *(2017-01-15)*
----------------------------

 * Remember 40 photos per folder for instant loading
 * Remember the scroll position when opening fullscreen photo
 * Make the launcher icon a bit smaller

Version 2.3.1 *(2017-01-14)*
----------------------------

 * Properly display some third party photos, like K9 mail attachments
 * Cache 30 photos per folder for instant loading
 * Add some additional Exif data to photo properties
 * Change the app launcher icon

Version 2.3.0 *(2017-01-13)*
----------------------------

 * Rework the way of obtaining images

Version 2.2.3 *(2017-01-10)*
----------------------------

 * Revert to the previous way of obtaining images and videos
 * Use the updated colorpicker dialog

Version 2.2.2 *(2017-01-08)*
----------------------------

 * Fix deleting folders on sd card
 * Some threading fixes related to delete

Version 2.2.1 *(2017-01-07)*
----------------------------

 * Allow zooming pngs and gifs
 * Allow creating new folders at Copy/Move or Save as dialog destinations
 * Update the way the app is obtaining images, so it will show more of them
 * Couple smaller improvements

Version 2.2.0 *(2017-01-06)*
----------------------------

 * Refactor the way fullscreen images are displayed, should improve quality
 * Display images in .nomedia folders when "Show hidden folders" is checked

Version 2.1.3 *(2017-01-04)*
----------------------------

 * Store displayed directories and display them instantly on next app open
 * Some crashfixes

Version 2.1.2 *(2016-12-29)*
----------------------------

 * Try using another library for loading images, if one fails

Version 2.1.1 *(2016-12-28)*
----------------------------

 * Add an option to loop videos
 * Do not display the Whats new dialog to new users
 * Use the text color for dialog buttons

Version 2.1 *(2016-12-27)*
----------------------------

 * Improve the performance by removing file validity check

Version 2.0 *(2016-12-27)*
----------------------------

 * Added color customization
 * Added a Select all button to folders
 * Added a Whats new dialog
 * Misc bug and crashfixes
 * Massive rewrites under the hood

Version 1.51 *(2016-12-11)*
----------------------------

 * Allow changing the column count by pinching

Version 1.50 *(2016-12-10)*
----------------------------

 * Allow displaying images or videos only
 * Misc updates

Version 1.49 *(2016-12-08)*
----------------------------

 * More fixes related to copy/move/delete
 * Add a Select All button at selecting media
 * Some visual swag

Version 1.48 *(2016-12-07)*
----------------------------

 * Couple fixes related to file operations

Version 1.47 *(2016-12-06)*
----------------------------

 * Add image resizing to the editor
 * Allow displaying all photos and videos together
 * Many small improvements

Version 1.46 *(2016-12-05)*
----------------------------

 * Allow pinning folders at the top
 * Add a new sorting option, by Date Taken
 * Fix a couple issues related to copy/move/delete
 * Misc bugfixes
 * Misc new bugs

Version 1.45 *(2016-12-04)*
----------------------------

 * Display pngs in higher quality
 * Allow swiping between images when app is open from another app
 * Allow changing the save path in the editor
 * Remove the Undo function when deleting from the fullscreen viewpager
 * Hugely improved the performance of loading albums and images
 * Many small bugfixes and improvements

Version 1.44 *(2016-12-01)*
----------------------------

 * Added an extra check to avoid unintentional folder deleting from SD cads
 * Fix some crashes at sorting albums on the main screen

Version 1.43 *(2016-11-30)*
----------------------------

 * Added Russian translation, updated some others
 * Attempt to fix deleting images in some cases
 * Do not initially hide the system UI at opening fullscreen photo/video

Version 1.42 *(2016-11-22)*
----------------------------

 * Fully hide the navigation bar in fullscreen mode
 * Fix marking selected items on Android 5
 * Misc smaller improvements

Version 1.41 *(2016-11-21)*
----------------------------

 * Cache thumbnails for better performance
 * Some crashfixes

Version 1.40 *(2016-11-20)*
----------------------------

 * Minor bugfixes

Version 1.39 *(2016-11-20)*
----------------------------

 * Fixing some licenses

Version 1.38 *(2016-11-19)*
----------------------------

 * Added a confirmation dialog before deleting anything
 * Allow deleting only photos and videos with the app
 * Added more responsive directory/photo column counts
 * Some performance improvements and bugfixes

Version 1.37 *(2016-11-17)*
----------------------------

 * Fix copying folders on SD card

Version 1.36 *(2016-11-17)*
----------------------------

 * Do not be so strict at limiting characters at file names
 * Fix setting folders as un/hidden
 * A lot more smaller fixes

Version 1.35 *(2016-11-15)*
----------------------------

 * Some stability and performance fixes

Version 1.34 *(2016-11-14)*
----------------------------

 * Improve the wallpaper quality in some cases

Version 1.33 *(2016-11-13)*
----------------------------

 * Misc small improvements

Version 1.32 *(2016-11-13)*
----------------------------

 * Added Chinese translation
 * Couple small bugfixes

Version 1.31 *(2016-11-12)*
----------------------------

 * Add a Move function
 * Many small improvements, mostly related to Copy/Move

Version 1.30 *(2016-11-10)*
----------------------------

 * Some improvements to the Copy function
 * Allow displaying filenames at the thumbnail view
 * Changing the bottom shadow to gradient

Version 1.29 *(2016-11-09)*
----------------------------

 * Adding an initial implementation of the Copy feature
 * Sort items by date_modified instead of date_taken
 * Make "Save as" the only saving option in the editor
 * Autoplay videos only if selected so

Version 1.28 *(2016-11-07)*
----------------------------

 * Couple file operation fixes on devices without SD card
 * Set the default folder sorting by Date, descending
 * Couple Spanish string corrections

Version 1.27 *(2016-11-06)*
----------------------------

 * Add a "Save as" option to the editor
 * Fix file operations on SD card
 * Do not delete folders recursively, just the direct children
 * Many other small improvements

Version 1.26 *(2016-11-03)*
----------------------------

 * Add a menu item for Properties

Version 1.25 *(2016-10-25)*
----------------------------

 * Change the light themes window backgrounds to white
 * Change the default theme to dark
 * Add an option for autoplaying videos
 * Misc performance improvements

Version 1.24 *(2016-10-09)*
----------------------------

 * Ignore folders containing a .nomedia file
 * Allow hiding folders
 * Misc bugfixes

Version 1.23 *(2016-10-08)*
----------------------------

 * Add a Crop / rotate function to images
 * Allow opening images and videos with third party apps
 * Allow setting images as Wallpaper directly from the app
 * Improve the support of Right to Left layouts

Version 1.22 *(2016-10-03)*
----------------------------

 * Fix some glitches at video playback at orientation change
 * Added portuguese translation

Version 1.21 *(2016-09-25)*
----------------------------

 * Prevent screen turning off at playing video
 * Fix marking selected items on Android 5
 * Fill the inside of Play button with transparent grey for bettervisibility

Version 1.20 *(2016-09-07)*
----------------------------

 * Added German translation

Version 1.19 *(2016-08-26)*
----------------------------

 * Add an Invite friends button
 * Allow different sorting for images and albums
 * Allow using the app at attachment picker at email clients
 * Rescan all files in current folder at Pull to refresh
 * Update app icon and launcher name

Version 1.18 *(2016-07-26)*
----------------------------

 * Make the dark theme really dark

Version 1.17 *(2016-07-24)*
----------------------------

 * Allow sorting the items
 * Make the fullscreen photos properly zoomable

Version 1.16 *(2016-07-20)*
----------------------------

 * Added a dark theme
 * Allow sharing multiple items at once
 * Implement Pull to refresh
 * Added Swedish translation

Version 1.15 *(2016-07-15)*
----------------------------

 * Use the proper action at clicking the Camera
 * Remove a bug with duplicate listing of the app at opening images
 * Add Italian and Japanese translations
 * Properly highlight the selected items at delete

Version 1.14 *(2016-07-13)*
----------------------------

 * Correct some interactions with third party apps

Version 1.13 *(2016-07-09)*
----------------------------

 * Add a Camera button to the main screen
 * Offer the app at opening photos from some more third party apps

Version 1.12 *(2016-07-07)*
----------------------------

 * Improve GIF support

Version 1.11 *(2016-07-05)*
----------------------------

 * Use the new Google Plus logo

Version 1.9 *(2016-07-01)*
----------------------------

 * Adjust everything properly if used on a tablet
 * Show a Rate us button to returning users

Version 1.8 *(2016-06-27)*
----------------------------

 * Allow setting Wallpaper with the app
 * Add Google Plus link in About section

Version 1.7 *(2016-06-23)*
----------------------------

 * Increased the video seekbar height for easier interaction
 * Made loading of images in detail screen faster
 * Added file/directory path at the Edit dialog

Version 1.6 *(2016-06-19)*
----------------------------

 * Add a Facebook page link to the About section

Version 1.5 *(2016-06-18)*
----------------------------

 * Sort the items only by timestamp, not by type

Version 1.4 *(2016-06-15)*
----------------------------

 * Allow using the app as an image/video chooser

Version 1.3 *(2016-06-14)*
----------------------------

 * Offer opening any device photos and videos with the app

Version 1.2 *(2016-06-13)*
----------------------------

 * Properly handle Marshmallow+ Storage permission

Version 1.1 *(2016-06-09)*
----------------------------

 * New: added support for videos

Version 1.0 *(2016-06-05)*
----------------------------

 * Initial release

