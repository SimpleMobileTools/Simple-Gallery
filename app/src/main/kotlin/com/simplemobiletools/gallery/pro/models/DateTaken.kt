package com.simplemobiletools.gallery.pro.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Date Taken in the MediaStore is unreliable and hard to work with, keep the values in an own database
// It is used at sorting files by date taken, checking EXIF file by file would be way too slow
@Entity(tableName = "date_takens", indices = [Index(value = ["full_path"], unique = true)])
data class DateTaken(
        @PrimaryKey(autoGenerate = true) var id: Int?,
        @ColumnInfo(name = "full_path") var fullPath: String,
        @ColumnInfo(name = "filename") var filename: String,
        @ColumnInfo(name = "parent_path") var parentPath: String,
        @ColumnInfo(name = "date_taken") var taken: Long,
        @ColumnInfo(name = "last_fixed") var lastFixed: Int)
