package com.simplemobiletools.gallery.pro.interfaces

import androidx.room.Dao
import androidx.room.Query

@Dao
interface FavoritesDAO {
    @Query("SELECT id FROM favorites WHERE full_path = :path COLLATE NOCASE")
    fun isFavorite(path: String): Boolean
}
