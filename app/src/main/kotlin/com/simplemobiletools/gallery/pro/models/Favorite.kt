package com.simplemobiletools.gallery.pro.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "favorites", indices = [Index(value = ["full_path"], unique = true)])
data class Favorite(
    @PrimaryKey(autoGenerate = true) var id: Long?,
    @ColumnInfo(name = "full_path") @SerialName("full_path") var fullPath: String,
    @ColumnInfo(name = "filename") var filename: String,
    @ColumnInfo(name = "parent_path") @SerialName("parent_path") var parentPath: String
)