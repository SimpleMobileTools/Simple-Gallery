package com.simplemobiletools.commons.models.contacts

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.simplemobiletools.commons.models.PhoneNumber

@Entity(tableName = "contacts", indices = [(Index(value = ["id"], unique = true))])
data class LocalContact(
    @PrimaryKey(autoGenerate = true) var id: Int?,
    @ColumnInfo(name = "prefix") var prefix: String,
    @ColumnInfo(name = "first_name") var firstName: String,
    @ColumnInfo(name = "middle_name") var middleName: String,
    @ColumnInfo(name = "surname") var surname: String,
    @ColumnInfo(name = "suffix") var suffix: String,
    @ColumnInfo(name = "nickname") var nickname: String,
    @ColumnInfo(name = "photo", typeAffinity = ColumnInfo.BLOB) var photo: ByteArray?,
    @ColumnInfo(name = "photo_uri") var photoUri: String,
    @ColumnInfo(name = "phone_numbers") var phoneNumbers: ArrayList<PhoneNumber>,
    @ColumnInfo(name = "emails") var emails: ArrayList<Email>,
    @ColumnInfo(name = "events") var events: ArrayList<Event>,
    @ColumnInfo(name = "starred") var starred: Int,
    @ColumnInfo(name = "addresses") var addresses: ArrayList<Address>,
    @ColumnInfo(name = "notes") var notes: String,
    @ColumnInfo(name = "groups") var groups: ArrayList<Long>,
    @ColumnInfo(name = "company") var company: String,
    @ColumnInfo(name = "job_position") var jobPosition: String,
    @ColumnInfo(name = "websites") var websites: ArrayList<String>,
    @ColumnInfo(name = "ims") var IMs: ArrayList<IM>,
    @ColumnInfo(name = "ringtone") var ringtone: String?
) {
    override fun equals(other: Any?) = id == (other as? LocalContact?)?.id

    override fun hashCode() = id ?: 0
}
