package com.simplemobiletools.gallery.pro.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import com.simplemobiletools.gallery.pro.helpers.RECYCLE_BIN
import com.simplemobiletools.gallery.pro.models.Directory

@Dao
interface DirectoryDao {
    @Query("SELECT path, thumbnail, filename, media_count, last_modified, date_taken, size, location, media_types, sort_value FROM directories")
    suspend fun getAll(): List<Directory>

    @Insert(onConflict = REPLACE)
    suspend fun insert(directory: Directory)

    @Insert(onConflict = REPLACE)
    suspend fun insertAll(directories: List<Directory>)

    @Query("DELETE FROM directories WHERE path = :path COLLATE NOCASE")
    suspend fun deleteDirPath(path: String)

    @Query("UPDATE OR REPLACE directories SET thumbnail = :thumbnail, media_count = :mediaCnt, last_modified = :lastModified, date_taken = :dateTaken, size = :size, media_types = :mediaTypes, sort_value = :sortValue WHERE path = :path COLLATE NOCASE")
    suspend fun updateDirectory(path: String, thumbnail: String, mediaCnt: Int, lastModified: Long, dateTaken: Long, size: Long, mediaTypes: Int, sortValue: String)

    @Query("UPDATE directories SET thumbnail = :thumbnail, filename = :name, path = :newPath WHERE path = :oldPath COLLATE NOCASE")
    suspend fun updateDirectoryAfterRename(thumbnail: String, name: String, newPath: String, oldPath: String)

    @Query("DELETE FROM directories WHERE path = \'$RECYCLE_BIN\' COLLATE NOCASE")
    suspend fun deleteRecycleBin()

    @Query("SELECT thumbnail FROM directories WHERE path = :path")
    suspend fun getDirectoryThumbnail(path: String): String?
}
