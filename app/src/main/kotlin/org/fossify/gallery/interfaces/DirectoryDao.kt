package org.fossify.gallery.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.fossify.gallery.helpers.RECYCLE_BIN
import org.fossify.gallery.models.Directory

@Dao
interface DirectoryDao {
    @Query("SELECT path, thumbnail, filename, media_count, last_modified, date_taken, size, location, media_types, sort_value FROM directories")
    fun getAll(): List<Directory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(directory: Directory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(directories: List<Directory>)

    @Query("DELETE FROM directories WHERE path = :path COLLATE NOCASE")
    fun deleteDirPath(path: String)

    @Query("UPDATE OR REPLACE directories SET thumbnail = :thumbnail, media_count = :mediaCnt, last_modified = :lastModified, date_taken = :dateTaken, size = :size, media_types = :mediaTypes, sort_value = :sortValue WHERE path = :path COLLATE NOCASE")
    fun updateDirectory(path: String, thumbnail: String, mediaCnt: Int, lastModified: Long, dateTaken: Long, size: Long, mediaTypes: Int, sortValue: String)

    @Query("UPDATE directories SET thumbnail = :thumbnail, filename = :name, path = :newPath WHERE path = :oldPath COLLATE NOCASE")
    fun updateDirectoryAfterRename(thumbnail: String, name: String, newPath: String, oldPath: String)

    @Query("DELETE FROM directories WHERE path = \'$RECYCLE_BIN\' COLLATE NOCASE")
    fun deleteRecycleBin()

    @Query("SELECT thumbnail FROM directories WHERE path = :path")
    fun getDirectoryThumbnail(path: String): String?
}
