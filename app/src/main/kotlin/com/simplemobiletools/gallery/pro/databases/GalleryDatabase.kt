package com.simplemobiletools.gallery.pro.databases

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.simplemobiletools.gallery.pro.interfaces.*
import com.simplemobiletools.gallery.pro.models.*

@Database(entities = [Directory::class, Medium::class, Widget::class, DateTaken::class, Favorite::class], version = 7)
abstract class GalleryDatabase : RoomDatabase() {

    abstract fun DirectoryDao(): DirectoryDao

    abstract fun MediumDao(): MediumDao

    abstract fun WidgetsDao(): WidgetsDao

    abstract fun DateTakensDAO(): DateTakensDAO

    abstract fun FavoritesDAO(): FavoritesDAO

    companion object {
        private var db: GalleryDatabase? = null

        fun getInstance(context: Context): GalleryDatabase {
            if (db == null) {
                synchronized(GalleryDatabase::class) {
                    if (db == null) {
                        db = Room.databaseBuilder(context.applicationContext, GalleryDatabase::class.java, "gallery.db")
                                .fallbackToDestructiveMigration()
                                .addMigrations(MIGRATION_4_5)
                                .addMigrations(MIGRATION_5_6)
                                .addMigrations(MIGRATION_6_7)
                                .build()
                    }
                }
            }
            return db!!
        }

        fun destroyInstance() {
            db = null
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE media ADD COLUMN video_duration INTEGER default 0 NOT NULL")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `widgets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `widget_id` INTEGER NOT NULL, `folder_path` TEXT NOT NULL)")
                database.execSQL("CREATE UNIQUE INDEX `index_widgets_widget_id` ON `widgets` (`widget_id`)")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `date_takens` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `full_path` TEXT NOT NULL, `file_name` TEXT NOT NULL, `parent_path` TEXT NOT NULL, `last_fixed` INTEGER NOT NULL)")
                database.execSQL("CREATE UNIQUE INDEX `index_date_takens_full_path` ON `date_takens` (`full_path`)")

                database.execSQL("CREATE TABLE IF NOT EXISTS `favorites` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `full_path` TEXT NOT NULL, `file_name` TEXT NOT NULL, `parent_path` TEXT NOT NULL)")
                database.execSQL("CREATE UNIQUE INDEX `index_favorites_full_path` ON `favorites` (`full_path`)")
            }
        }
    }
}
