package com.simplemobiletools.gallery.databases

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import com.simplemobiletools.gallery.interfaces.DirectoryDao
import com.simplemobiletools.gallery.interfaces.MediumDao
import com.simplemobiletools.gallery.models.Directory
import com.simplemobiletools.gallery.models.Medium

@Database(entities = [(Directory::class), (Medium::class)], version = 2)
abstract class GalleryDataBase : RoomDatabase() {

    abstract fun DirectoryDao(): DirectoryDao

    abstract fun MediumDao(): MediumDao

    companion object {
        private var INSTANCE: GalleryDataBase? = null

        fun getInstance(context: Context): GalleryDataBase {
            if (INSTANCE == null) {
                synchronized(GalleryDataBase::class) {
                    INSTANCE = Room.databaseBuilder(context.applicationContext, GalleryDataBase::class.java, "gallery.db")
                            .fallbackToDestructiveMigration()
                            .build()
                }
            }
            return INSTANCE!!
        }

        fun destroyInstance() {
            INSTANCE = null
        }
    }
}
