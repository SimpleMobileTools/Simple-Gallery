package com.simplemobiletools.commons.databases

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.simplemobiletools.commons.helpers.Converters
import com.simplemobiletools.commons.helpers.FIRST_CONTACT_ID
import com.simplemobiletools.commons.helpers.FIRST_GROUP_ID
import com.simplemobiletools.commons.helpers.getEmptyLocalContact
import com.simplemobiletools.commons.interfaces.ContactsDao
import com.simplemobiletools.commons.models.contacts.Group
import com.simplemobiletools.commons.models.contacts.LocalContact
import com.simplemobiletools.commons.interfaces.GroupsDao
import java.util.concurrent.Executors

@Database(entities = [LocalContact::class, Group::class], version = 3)
@TypeConverters(Converters::class)
abstract class ContactsDatabase : RoomDatabase() {

    abstract fun ContactsDao(): ContactsDao

    abstract fun GroupsDao(): GroupsDao

    companion object {
        private var db: ContactsDatabase? = null

        fun getInstance(context: Context): ContactsDatabase {
            if (db == null) {
                synchronized(ContactsDatabase::class) {
                    if (db == null) {
                        db = Room.databaseBuilder(context.applicationContext, ContactsDatabase::class.java, "local_contacts.db")
                            .addCallback(object : Callback() {
                                override fun onCreate(db: SupportSQLiteDatabase) {
                                    super.onCreate(db)
                                    increaseAutoIncrementIds()
                                }
                            })
                            .addMigrations(MIGRATION_1_2)
                            .addMigrations(MIGRATION_2_3)
                            .build()
                    }
                }
            }
            return db!!
        }

        fun destroyInstance() {
            db = null
        }

        // start autoincrement ID from FIRST_CONTACT_ID/FIRST_GROUP_ID to avoid conflicts
        // Room doesn't seem to have a built in way for it, so just create a contact/group and delete it
        private fun increaseAutoIncrementIds() {
            Executors.newSingleThreadExecutor().execute {
                val emptyContact = getEmptyLocalContact()
                emptyContact.id = FIRST_CONTACT_ID
                db!!.ContactsDao().apply {
                    insertOrUpdate(emptyContact)
                    deleteContactId(FIRST_CONTACT_ID)
                }

                val emptyGroup = Group(FIRST_GROUP_ID, "")
                db!!.GroupsDao().apply {
                    insertOrUpdate(emptyGroup)
                    deleteGroupId(FIRST_GROUP_ID)
                }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.apply {
                    execSQL("ALTER TABLE contacts ADD COLUMN photo_uri TEXT NOT NULL DEFAULT ''")
                }
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.apply {
                    execSQL("ALTER TABLE contacts ADD COLUMN ringtone TEXT DEFAULT ''")
                }
            }
        }
    }
}
