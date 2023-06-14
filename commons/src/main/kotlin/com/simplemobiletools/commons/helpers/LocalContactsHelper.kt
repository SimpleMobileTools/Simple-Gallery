package com.simplemobiletools.commons.helpers

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.MediaStore
import com.simplemobiletools.commons.extensions.contactsDB
import com.simplemobiletools.commons.extensions.getByteArray
import com.simplemobiletools.commons.extensions.getEmptyContact
import com.simplemobiletools.commons.models.SimpleContact
import com.simplemobiletools.commons.models.contacts.Contact
import com.simplemobiletools.commons.models.contacts.*
import com.simplemobiletools.commons.models.contacts.LocalContact

class LocalContactsHelper(val context: Context) {
    fun getAllContacts(favoritesOnly: Boolean = false): ArrayList<Contact> {
        val contacts = if (favoritesOnly) context.contactsDB.getFavoriteContacts() else context.contactsDB.getContacts()
        val storedGroups = ContactsHelper(context).getStoredGroupsSync()
        return (contacts.map { convertLocalContactToContact(it, storedGroups) }.toMutableList() as? ArrayList<Contact>) ?: arrayListOf()
    }

    fun getContactWithId(id: Int): Contact? {
        val storedGroups = ContactsHelper(context).getStoredGroupsSync()
        return convertLocalContactToContact(context.contactsDB.getContactWithId(id), storedGroups)
    }

    fun insertOrUpdateContact(contact: Contact): Boolean {
        val localContact = convertContactToLocalContact(contact)
        return context.contactsDB.insertOrUpdate(localContact) > 0
    }

    fun addContactsToGroup(contacts: ArrayList<Contact>, groupId: Long) {
        contacts.forEach {
            val localContact = convertContactToLocalContact(it)
            val newGroups = localContact.groups
            newGroups.add(groupId)
            newGroups.distinct()
            localContact.groups = newGroups
            context.contactsDB.insertOrUpdate(localContact)
        }
    }

    fun removeContactsFromGroup(contacts: ArrayList<Contact>, groupId: Long) {
        contacts.forEach {
            val localContact = convertContactToLocalContact(it)
            val newGroups = localContact.groups
            newGroups.remove(groupId)
            localContact.groups = newGroups
            context.contactsDB.insertOrUpdate(localContact)
        }
    }

    fun deleteContactIds(ids: MutableList<Long>) {
        ids.chunked(30).forEach {
            context.contactsDB.deleteContactIds(it)
        }
    }

    fun toggleFavorites(ids: Array<Int>, addToFavorites: Boolean) {
        val isStarred = if (addToFavorites) 1 else 0
        ids.forEach {
            context.contactsDB.updateStarred(isStarred, it)
        }
    }

    fun updateRingtone(id: Int, ringtone: String) {
        context.contactsDB.updateRingtone(ringtone, id)
    }

    private fun getPhotoByteArray(uri: String): ByteArray {
        if (uri.isEmpty()) {
            return ByteArray(0)
        }

        val photoUri = Uri.parse(uri)
        val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, photoUri)

        val fullSizePhotoData = bitmap.getByteArray()
        bitmap.recycle()

        return fullSizePhotoData
    }

    private fun convertLocalContactToContact(localContact: LocalContact?, storedGroups: ArrayList<Group>): Contact? {
        if (localContact == null) {
            return null
        }

        val contactPhoto = if (localContact.photo == null) {
            null
        } else {
            try {
                BitmapFactory.decodeByteArray(localContact.photo, 0, localContact.photo!!.size)
            } catch (e: OutOfMemoryError) {
                null
            }
        }

        return context.getEmptyContact().apply {
            id = localContact.id!!
            prefix = localContact.prefix
            firstName = localContact.firstName
            middleName = localContact.middleName
            surname = localContact.surname
            suffix = localContact.suffix
            nickname = localContact.nickname
            phoneNumbers = localContact.phoneNumbers
            emails = localContact.emails
            addresses = localContact.addresses
            events = localContact.events
            source = SMT_PRIVATE
            starred = localContact.starred
            contactId = localContact.id!!
            thumbnailUri = ""
            photo = contactPhoto
            photoUri = localContact.photoUri
            notes = localContact.notes
            groups = storedGroups.filter { localContact.groups.contains(it.id) } as ArrayList<Group>
            organization = Organization(localContact.company, localContact.jobPosition)
            websites = localContact.websites
            IMs = localContact.IMs
            ringtone = localContact.ringtone
        }
    }

    private fun convertContactToLocalContact(contact: Contact): LocalContact {
        val photoByteArray = if (contact.photoUri.isNotEmpty()) {
            getPhotoByteArray(contact.photoUri)
        } else {
            contact.photo?.getByteArray()
        }

        return getEmptyLocalContact().apply {
            id = if (contact.id <= FIRST_CONTACT_ID) null else contact.id
            prefix = contact.prefix
            firstName = contact.firstName
            middleName = contact.middleName
            surname = contact.surname
            suffix = contact.suffix
            nickname = contact.nickname
            photo = photoByteArray
            phoneNumbers = contact.phoneNumbers
            emails = contact.emails
            events = contact.events
            starred = contact.starred
            addresses = contact.addresses
            notes = contact.notes
            groups = contact.groups.map { it.id }.toMutableList() as ArrayList<Long>
            company = contact.organization.company
            jobPosition = contact.organization.jobPosition
            websites = contact.websites
            IMs = contact.IMs
            ringtone = contact.ringtone
        }
    }

    fun getPrivateSimpleContactsSync(favoritesOnly: Boolean, withPhoneNumbersOnly: Boolean) = getAllContacts(favoritesOnly).mapNotNull {
        convertContactToSimpleContact(it, withPhoneNumbersOnly)
    }
    companion object{
        fun convertContactToSimpleContact(contact: Contact?, withPhoneNumbersOnly: Boolean): SimpleContact?{
            return if (contact == null || (withPhoneNumbersOnly && contact.phoneNumbers.isEmpty())) {
                null
            } else {
                val birthdays = contact.events.filter { it.type == Event.TYPE_BIRTHDAY }.map { it.value }.toMutableList() as ArrayList<String>
                val anniversaries = contact.events.filter { it.type == Event.TYPE_ANNIVERSARY }.map { it.value }.toMutableList() as ArrayList<String>
                SimpleContact(contact.id, contact.id, contact.getNameToDisplay(), contact.photoUri, contact.phoneNumbers, birthdays, anniversaries)
            }
        }
    }
}
