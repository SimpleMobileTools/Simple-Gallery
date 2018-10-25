package com.simplemobiletools.gallery.databases

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.simplemobiletools.gallery.objects.MyExecutor
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
                                .setQueryExecutor(MyExecutor.myExecutor)
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
