package com.simplemobiletools.gallery.pro.activities

import android.content.Intent
import com.simplemobiletools.commons.activities.BaseSplashActivity
import com.simplemobiletools.commons.extensions.getSharedPrefs
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.gallery.pro.extensions.config
import com.simplemobiletools.gallery.pro.extensions.favoritesDB
import com.simplemobiletools.gallery.pro.extensions.getFavoriteFromPath
import com.simplemobiletools.gallery.pro.extensions.mediaDB
import com.simplemobiletools.gallery.pro.helpers.DIRECTORY_GROUPING_DIRECT_SUBFOLDERS
import com.simplemobiletools.gallery.pro.helpers.DIRECTORY_GROUPING_NONE
import com.simplemobiletools.gallery.pro.helpers.GROUP_DIRECT_SUBFOLDERS
import com.simplemobiletools.gallery.pro.models.Favorite

class SplashActivity : BaseSplashActivity() {
    override fun initActivity() {
        // check if previously selected favorite items have been properly migrated into the new Favorites table
        maybeMigrateDirectoryGrouping()
        if (config.wereFavoritesMigrated) {
            launchActivity()
        } else {
            if (config.appRunCount == 0) {
                config.wereFavoritesMigrated = true
                launchActivity()
            } else {
                config.wereFavoritesMigrated = true
                ensureBackgroundThread {
                    val favorites = ArrayList<Favorite>()
                    val favoritePaths = mediaDB.getFavorites().map { it.path }.toMutableList() as ArrayList<String>
                    favoritePaths.forEach {
                        favorites.add(getFavoriteFromPath(it))
                    }
                    favoritesDB.insertAll(favorites)

                    runOnUiThread {
                        launchActivity()
                    }
                }
            }
        }
    }

    private fun maybeMigrateDirectoryGrouping() {
        if (config.appRunCount == 0) {
            config.wasDirectoryGroupingMigrated = true
        } else {
            if (!config.wasDirectoryGroupingMigrated) {
                config.directoryGrouping = if (getSharedPrefs().getBoolean(GROUP_DIRECT_SUBFOLDERS, false)) {
                    DIRECTORY_GROUPING_DIRECT_SUBFOLDERS
                } else {
                    DIRECTORY_GROUPING_NONE
                }
                config.wasDirectoryGroupingMigrated = true
            }
        }
    }

    private fun launchActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
