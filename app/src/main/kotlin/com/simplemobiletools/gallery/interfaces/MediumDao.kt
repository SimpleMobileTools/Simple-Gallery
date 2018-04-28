package com.simplemobiletools.gallery.interfaces

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy.REPLACE
import android.arch.persistence.room.Query
import com.simplemobiletools.gallery.models.Medium

@Dao
interface MediumDao {
    @Query("SELECT * FROM media")
    fun getAll(): List<Medium>

    @Query("SELECT filename, full_path, parent_path, last_modified, date_taken, size, type FROM media WHERE parent_path = :path")
    fun getMediaFromPath(path: String): List<Medium>

    @Insert(onConflict = REPLACE)
    fun insert(medium: Medium)

    @Insert(onConflict = REPLACE)
    fun insertAll(media: List<Medium>)

    @Query("DELETE FROM media WHERE full_path = :path")
    fun deleteMediumPath(path: String)

    @Query("UPDATE OR REPLACE media SET filename = :newFilename, full_path = :newFullPath, parent_path = :newParentPath WHERE full_path = :oldPath")
    fun updateMedium(oldPath: String, newParentPath: String, newFilename: String, newFullPath: String)
}
