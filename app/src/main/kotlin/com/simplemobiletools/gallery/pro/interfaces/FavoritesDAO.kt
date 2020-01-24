package com.simplemobiletools.gallery.pro.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.simplemobiletools.gallery.pro.models.Favorite

@Dao
interface FavoritesDAO {
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

    @Query("DELETE FROM favorites WHERE full_path = :path COLLATE NOCASE")
    fun deleteFavoritePath(path: String)
}
