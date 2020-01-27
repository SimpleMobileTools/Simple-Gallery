package com.simplemobiletools.gallery.pro.interfaces

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import com.simplemobiletools.gallery.pro.models.Medium

@Dao
interface MediumDao {
    @Query("SELECT filename, full_path, parent_path, last_modified, date_taken, size, type, video_duration, is_favorite, deleted_ts FROM media WHERE deleted_ts = 0 AND parent_path = :path COLLATE NOCASE")
    fun getMediaFromPath(path: String): List<Medium>

    @Query("SELECT filename, full_path, parent_path, last_modified, date_taken, size, type, video_duration, is_favorite, deleted_ts FROM media WHERE deleted_ts = 0 AND is_favorite = 1")
    fun getFavorites(): List<Medium>

    @Query("SELECT filename, full_path, parent_path, last_modified, date_taken, size, type, video_duration, is_favorite, deleted_ts FROM media WHERE deleted_ts != 0")
    fun getDeletedMedia(): List<Medium>

    @Insert(onConflict = REPLACE)
    fun insert(medium: Medium)

    @Insert(onConflict = REPLACE)
    fun insertAll(media: List<Medium>)

    @Delete
    fun deleteMedia(vararg medium: Medium)

    @Query("DELETE FROM media WHERE full_path = :path COLLATE NOCASE")
    fun deleteMediumPath(path: String)

    @Query("DELETE FROM media WHERE deleted_ts < :timestmap AND deleted_ts != 0")
    fun deleteOldRecycleBinItems(timestmap: Long)

    @Query("UPDATE OR REPLACE media SET filename = :newFilename, full_path = :newFullPath, parent_path = :newParentPath WHERE full_path = :oldPath COLLATE NOCASE")
    fun updateMedium(newFilename: String, newFullPath: String, newParentPath: String, oldPath: String)

    @Query("UPDATE OR REPLACE media SET full_path = :newPath, deleted_ts = :deletedTS WHERE full_path = :oldPath COLLATE NOCASE")
    fun updateDeleted(newPath: String, deletedTS: Long, oldPath: String)

    @Query("UPDATE media SET date_taken = :dateTaken WHERE full_path = :path COLLATE NOCASE")
    fun updateFavoriteDateTaken(path: String, dateTaken: Long)

    @Query("UPDATE media SET is_favorite = 0")
    fun clearFavorites()

    @Query("DELETE FROM media WHERE deleted_ts != 0")
    fun clearRecycleBin()
}
