package com.simplemobiletools.gallery.databases

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import com.simplemobiletools.gallery.interfaces.DirectoryDao
import com.simplemobiletools.gallery.models.Directory

@Database(entities = [(Directory::class)], version = 1)
abstract class DirectoryDataBase : RoomDatabase() {

    abstract fun DirectoryDao(): DirectoryDao

    companion object {
        private var INSTANCE: DirectoryDataBase? = null

        fun getInstance(context: Context): DirectoryDataBase {
            if (INSTANCE == null) {
                synchronized(DirectoryDataBase::class) {
                    INSTANCE = Room.databaseBuilder(context.applicationContext, DirectoryDataBase::class.java, "directories.db").build()
                }
            }
            return INSTANCE!!
        }

        fun destroyInstance() {
            INSTANCE = null
        }
    }
}
