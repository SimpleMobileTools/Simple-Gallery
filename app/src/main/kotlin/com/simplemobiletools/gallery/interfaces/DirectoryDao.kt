package com.simplemobiletools.gallery.interfaces

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Delete
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy.REPLACE
import android.arch.persistence.room.Query
import com.simplemobiletools.gallery.models.Directory

@Dao
interface DirectoryDao {
    @Query("SELECT path, thumbnail, filename, media_count, last_modified, date_taken, size, is_on_sd_card, media_types FROM directories")
    fun getAll(): List<Directory>

    @Insert(onConflict = REPLACE)
    fun insert(directory: Directory)

    @Insert(onConflict = REPLACE)
    fun insertAll(directories: List<Directory>)

    @Delete
    fun deleteDir(directory: Directory)

    @Query("DELETE FROM directories WHERE path = :path")
    fun deleteDirPath(path: String)
}
