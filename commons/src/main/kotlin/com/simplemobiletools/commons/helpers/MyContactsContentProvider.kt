package com.simplemobiletools.commons.helpers

import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.simplemobiletools.commons.extensions.getIntValue
import com.simplemobiletools.commons.extensions.getStringValue
import com.simplemobiletools.commons.models.PhoneNumber
import com.simplemobiletools.commons.models.SimpleContact
import com.simplemobiletools.commons.models.contacts.Contact

// used for sharing privately stored contacts in Simple Contacts with Simple Dialer, Simple SMS Messenger and Simple Calendar Pro
class MyContactsContentProvider {
    companion object {
        private const val AUTHORITY = "com.simplemobiletools.commons.contactsprovider"
        val CONTACTS_CONTENT_URI = Uri.parse("content://$AUTHORITY/contacts")

        const val FAVORITES_ONLY = "favorites_only"
        const val COL_RAW_ID = "raw_id"
        const val COL_CONTACT_ID = "contact_id"
        const val COL_NAME = "name"
        const val COL_PHOTO_URI = "photo_uri"
        const val COL_PHONE_NUMBERS = "phone_numbers"
        const val COL_BIRTHDAYS = "birthdays"
        const val COL_ANNIVERSARIES = "anniversaries"

        fun getSimpleContacts(context: Context, cursor: Cursor?): ArrayList<SimpleContact> {
            val contacts = ArrayList<SimpleContact>()
            val packageName = context.packageName.removeSuffix(".debug")
            if (packageName != "com.simplemobiletools.dialer" && packageName != "com.simplemobiletools.smsmessenger" && packageName != "com.simplemobiletools.calendar.pro") {
                return contacts
            }

            try {
                cursor?.use {
                    if (cursor.moveToFirst()) {
                        do {
                            val rawId = cursor.getIntValue(COL_RAW_ID)
                            val contactId = cursor.getIntValue(COL_CONTACT_ID)
                            val name = cursor.getStringValue(COL_NAME)
                            val photoUri = cursor.getStringValue(COL_PHOTO_URI)
                            val phoneNumbersJson = cursor.getStringValue(COL_PHONE_NUMBERS)
                            val birthdaysJson = cursor.getStringValue(COL_BIRTHDAYS)
                            val anniversariesJson = cursor.getStringValue(COL_ANNIVERSARIES)

                            val phoneNumbersToken = object : TypeToken<ArrayList<PhoneNumber>>() {}.type
                            val phoneNumbers = Gson().fromJson<ArrayList<PhoneNumber>>(phoneNumbersJson, phoneNumbersToken) ?: ArrayList()

                            val stringsToken = object : TypeToken<ArrayList<String>>() {}.type
                            val birthdays = Gson().fromJson<ArrayList<String>>(birthdaysJson, stringsToken) ?: ArrayList()
                            val anniversaries = Gson().fromJson<ArrayList<String>>(anniversariesJson, stringsToken) ?: ArrayList()

                            val contact = SimpleContact(rawId, contactId, name, photoUri, phoneNumbers, birthdays, anniversaries)
                            contacts.add(contact)
                        } while (cursor.moveToNext())
                    }
                }
            } catch (ignored: Exception) {
            }
            return contacts
        }

        fun getContacts(context: Context, cursor: Cursor?): ArrayList<Contact> {
            val contacts = ArrayList<Contact>()
            val packageName = context.packageName.removeSuffix(".debug")
            if (packageName != "com.simplemobiletools.dialer" && packageName != "com.simplemobiletools.smsmessenger" && packageName != "com.simplemobiletools.calendar.pro") {
                return contacts
            }

            try {
                cursor?.use {
                    if (cursor.moveToFirst()) {
                        do {
                            val rawId = cursor.getIntValue(COL_RAW_ID)
                            val contactId = cursor.getIntValue(COL_CONTACT_ID)
                            val name = cursor.getStringValue(COL_NAME)
                            val photoUri = cursor.getStringValue(COL_PHOTO_URI)
                            val phoneNumbersJson = cursor.getStringValue(COL_PHONE_NUMBERS)
                            val birthdaysJson = cursor.getStringValue(COL_BIRTHDAYS)
                            val anniversariesJson = cursor.getStringValue(COL_ANNIVERSARIES)

                            val phoneNumbersToken = object : TypeToken<ArrayList<PhoneNumber>>() {}.type
                            val phoneNumbers = Gson().fromJson<ArrayList<PhoneNumber>>(phoneNumbersJson, phoneNumbersToken) ?: ArrayList()

                            val stringsToken = object : TypeToken<ArrayList<String>>() {}.type
                            val birthdays = Gson().fromJson<ArrayList<String>>(birthdaysJson, stringsToken) ?: ArrayList()
                            val anniversaries = Gson().fromJson<ArrayList<String>>(anniversariesJson, stringsToken) ?: ArrayList()

                            val names = if (name.contains(",")) {
                                name.split(",")
                            } else {
                                name.split(" ")
                            }

                            var firstName = names.firstOrNull() ?: ""
                            if (name.contains(",")) {
                                firstName += ", "
                            }

                            val middleName = if (names.size >= 3) {
                                names.subList(1, names.size - 1).joinToString(" ")
                            } else {
                                ""
                            }

                            val surname = names.lastOrNull()?.takeIf { names.size > 1 } ?: ""

                            val contact = Contact(
                                id = rawId,
                                contactId = contactId,
                                firstName = firstName,
                                middleName = middleName,
                                surname = surname,
                                photoUri = photoUri,
                                phoneNumbers = phoneNumbers,
                                source = SMT_PRIVATE
                            ).also {
                                it.birthdays = birthdays
                                it.anniversaries = anniversaries
                            }

                            contacts.add(contact)
                        } while (cursor.moveToNext())
                    }
                }
            } catch (ignored: Exception) {
            }
            return contacts
        }
    }
}
