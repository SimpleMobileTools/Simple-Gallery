package com.simplemobiletools.gallery.databases

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import com.simplemobiletools.gallery.interfaces.DirectoryDao
import com.simplemobiletools.gallery.interfaces.MediumDao
import com.simplemobiletools.gallery.models.Directory
import com.simplemobiletools.gallery.models.Medium

@Database(entities = [(Directory::class), (Medium::class)], version = 4)
abstract class GalleryDatabase : RoomDatabase() {

    abstract fun DirectoryDao(): DirectoryDao

    abstract fun MediumDao(): MediumDao

    companion object {
        private var db: GalleryDatabase? = null

        fun getInstance(context: Context): GalleryDatabase {
            if (db == null) {
                synchronized(GalleryDatabase::class) {
                    if (db == null) {
                        db = Room.databaseBuilder(context.applicationContext, GalleryDatabase::class.java, "gallery.db")
                                .fallbackToDestructiveMigration()
                                .build()
                        db!!.openHelper.setWriteAheadLoggingEnabled(true)
                    }
                }
            }
            return db!!
        }

        fun destroyInstance() {
            db = null
        }
    }
}
