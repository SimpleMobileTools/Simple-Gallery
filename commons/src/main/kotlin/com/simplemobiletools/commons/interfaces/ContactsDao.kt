package com.simplemobiletools.commons.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.simplemobiletools.commons.models.contacts.LocalContact

@Dao
interface ContactsDao {
    @Query("SELECT * FROM contacts")
    fun getContacts(): List<LocalContact>

    @Query("SELECT * FROM contacts WHERE starred = 1")
    fun getFavoriteContacts(): List<LocalContact>

    @Query("SELECT * FROM contacts WHERE id = :id")
    fun getContactWithId(id: Int): LocalContact?

    @Query("SELECT * FROM contacts WHERE phone_numbers LIKE :number")
    fun getContactWithNumber(number: String): LocalContact?

    @Query("UPDATE contacts SET starred = :isStarred WHERE id = :id")
    fun updateStarred(isStarred: Int, id: Int)

    @Query("UPDATE contacts SET ringtone = :ringtone WHERE id = :id")
    fun updateRingtone(ringtone: String, id: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(contact: LocalContact): Long

    @Query("DELETE FROM contacts WHERE id = :id")
    fun deleteContactId(id: Int)

    @Query("DELETE FROM contacts WHERE id IN (:ids)")
    fun deleteContactIds(ids: List<Long>)
}
