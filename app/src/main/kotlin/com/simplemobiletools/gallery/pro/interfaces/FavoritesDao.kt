package com.simplemobiletools.gallery.pro.interfaces

import androidx.room.*
import com.simplemobiletools.gallery.pro.models.Favorite

@Dao
interface FavoritesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(favorite: Favorite)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(favorites: List<Favorite>)

    @Query("SELECT full_path FROM favorites")
    fun getFavoriteRawPaths(): List<String>

    @Query("SELECT favorites.full_path FROM favorites INNER JOIN media ON favorites.full_path = media.full_path WHERE media.deleted_ts = 0")
    fun getValidFavoritePaths(): List<String>

    @Query("SELECT id FROM favorites WHERE full_path = :path COLLATE NOCASE")
    fun isFavorite(path: String): Boolean

    @Query("UPDATE OR REPLACE favorites SET filename = :newFilename, full_path = :newFullPath, parent_path = :newParentPath WHERE full_path = :oldPath COLLATE NOCASE")
    fun updateFavorite(newFilename: String, newFullPath: String, newParentPath: String, oldPath: String)

    @Delete
    fun deleteFavorites(vararg favorite: Favorite)

    @Query("DELETE FROM favorites WHERE full_path = :path COLLATE NOCASE")
    fun deleteFavoritePath(path: String)

    @Query("DELETE FROM favorites")
    fun clearFavorites()
}
