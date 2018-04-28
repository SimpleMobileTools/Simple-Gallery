package com.simplemobiletools.gallery.interfaces

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy.REPLACE
import android.arch.persistence.room.Query
import com.simplemobiletools.gallery.models.Directory

@Dao
interface DirectoryDao {
    @Query("SELECT path, thumbnail, filename, media_count, last_modified, date_taken, size, location, media_types FROM directories")
    fun getAll(): List<Directory>

    @Insert(onConflict = REPLACE)
    fun insert(directory: Directory)

    @Insert(onConflict = REPLACE)
    fun insertAll(directories: List<Directory>)

    @Query("DELETE FROM directories WHERE path = :path")
    fun deleteDirPath(path: String)

    @Query("UPDATE OR REPLACE directories SET thumbnail = :thumbnail, media_count = :mediaCnt, last_modified = :lastModified, date_taken = :dateTaken, size = :size, media_types = :mediaTypes  WHERE path = :path")
    fun updateDirectory(path: String, thumbnail: String, mediaCnt: Int, lastModified: Long, dateTaken: Long, size: Long, mediaTypes: Int)
}
