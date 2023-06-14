package com.simplemobiletools.commons.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.simplemobiletools.commons.models.contacts.Group

@Dao
interface GroupsDao {
    @Query("SELECT * FROM groups")
    fun getGroups(): List<Group>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(group: Group): Long

    @Query("DELETE FROM groups WHERE id = :id")
    fun deleteGroupId(id: Long)
}
