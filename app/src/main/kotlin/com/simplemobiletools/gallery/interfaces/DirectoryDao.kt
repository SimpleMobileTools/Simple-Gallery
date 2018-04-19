package com.simplemobiletools.gallery.interfaces

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy.REPLACE
import android.arch.persistence.room.Query
import com.simplemobiletools.gallery.models.Directory

@Dao
interface DirectoryDao {
    @Query("SELECT * from directories")
    fun getAll(): List<Directory>

    @Insert(onConflict = REPLACE)
    fun insert(directory: Directory)
}
