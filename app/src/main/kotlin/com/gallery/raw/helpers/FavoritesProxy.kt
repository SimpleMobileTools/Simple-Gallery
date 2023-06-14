package com.gallery.raw.helpers

import android.util.Log
import com.simplemobiletools.commons.extensions.getFilenameFromPath
import com.simplemobiletools.commons.extensions.getParentPath
import com.gallery.raw.interfaces.FavoritesDao
import com.gallery.raw.models.Favorite
import java.io.File


class FavoritesProxy(private val favoritesDao: FavoritesDao) {

    companion object {
        const val TAG = "FavoritesProxy"

        fun getFavoriteFromPath(path: String): Favorite {
            Log.d(TAG, "getFavoriteFromPath($path)")
            val file = File(path)
            return Favorite(null, path, path.getFilenameFromPath(), path.getParentPath(), file.lastModified())
        }
    }

    fun deleteFavoritePath(path: String) {
        Log.d(TAG, "deleteFavoritePath($path)")
        favoritesDao.deleteFavoritePath(path)
    }

    fun updateFavorite(newFilename: String, newPath: String, newParentPath: String, oldPath: String) {
        Log.d(TAG, "updateFavorite($newFilename, $newPath, $newParentPath, $oldPath)")
        return favoritesDao.updateFavorite(newFilename, newPath, newParentPath, oldPath)
    }

    fun getValidFavoritePaths(): List<String> {
        val sb = java.lang.StringBuilder()
        favoritesDao.getValidFavoritePaths().forEach {
            sb.append("[$it], ")
        }
        Log.d(TAG, "getValidFavoritePaths -> " + sb.toString())
        return favoritesDao.getValidFavoritePaths()
    }

    fun insert(favoriteFromPath: Favorite) {
        Log.d(TAG, "insert($favoriteFromPath)")
        favoritesDao.insert(favoriteFromPath)
    }

    fun isFavorite(path: String): Boolean {
        Log.d(TAG, "isFavorite($path)")
        val file = File(path)
        return favoritesDao.isFavorite(path.getFilenameFromPath(), file.lastModified())
    }

    fun insertAll(favorites: ArrayList<Favorite>) {
        val sb = java.lang.StringBuilder()
        favorites.forEach {
            sb.append("$it, ")
        }
        Log.d(TAG, "insertAll(${sb.toString()})")
        favoritesDao.insertAll(favorites)
    }

    fun clearFavorites() {
        Log.d(TAG, "clearFavorites()")
        favoritesDao.clearFavorites()
    }
}
