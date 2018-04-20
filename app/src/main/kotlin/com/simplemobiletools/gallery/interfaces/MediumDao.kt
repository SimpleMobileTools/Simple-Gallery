package com.simplemobiletools.gallery.interfaces

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy.REPLACE
import android.arch.persistence.room.Query
import com.simplemobiletools.gallery.models.Medium

@Dao
interface MediumDao {
    @Query("SELECT * from media")
    fun getAll(): List<Medium>

    @Insert(onConflict = REPLACE)
    fun insert(medium: Medium)

    @Insert(onConflict = REPLACE)
    fun insertAll(media: List<Medium>)
}
