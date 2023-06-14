package com.simplemobiletools.commons.helpers

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.PhoneLookup
import android.text.TextUtils
import android.util.SparseArray
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.models.PhoneNumber
import com.simplemobiletools.commons.models.SimpleContact

class SimpleContactsHelper(val context: Context) {
    fun getAvailableContacts(favoritesOnly: Boolean, callback: (ArrayList<SimpleContact>) -> Unit) {
        ensureBackgroundThread {
            val names = getContactNames(favoritesOnly)
            var allContacts = getContactPhoneNumbers(favoritesOnly)
            allContacts.forEach {
                val contactId = it.rawId
                val contact = names.firstOrNull { it.rawId == contactId }
                val name = contact?.name
                if (name != null) {
                    it.name = name
                }

                val photoUri = contact?.photoUri
                if (photoUri != null) {
                    it.photoUri = photoUri
                }
            }

            allContacts = allContacts.filter { it.name.isNotEmpty() }.distinctBy {
                val startIndex = Math.max(0, it.phoneNumbers.first().normalizedNumber.length - 9)
                it.phoneNumbers.first().normalizedNumber.substring(startIndex)
            }.distinctBy { it.rawId }.toMutableList() as ArrayList<SimpleContact>

            // if there are duplicate contacts with the same name, while the first one has phone numbers 1234 and 4567, second one has only 4567,
            // use just the first contact
            val contactsToRemove = ArrayList<SimpleContact>()
            allContacts.groupBy { it.name }.forEach {
                val contacts = it.value.toMutableList() as ArrayList<SimpleContact>
                if (contacts.size > 1) {
                    contacts.sortByDescending { it.phoneNumbers.size }
                    if (contacts.any { it.phoneNumbers.size == 1 } && contacts.any { it.phoneNumbers.size > 1 }) {
                        val multipleNumbersContact = contacts.first()
                        contacts.subList(1, contacts.size).forEach { contact ->
                            if (contact.phoneNumbers.all { multipleNumbersContact.doesContainPhoneNumber(it.normalizedNumber) }) {
                                val contactToRemove = allContacts.firstOrNull { it.rawId == contact.rawId }
                                if (contactToRemove != null) {
                                    contactsToRemove.add(contactToRemove)
                                }
                            }
                        }
                    }
                }
            }

            contactsToRemove.forEach {
                allContacts.remove(it)
            }

            val birthdays = getContactEvents(true)
            var size = birthdays.size()
            for (i in 0 until size) {
                val key = birthdays.keyAt(i)
                allContacts.firstOrNull { it.rawId == key }?.birthdays = birthdays.valueAt(i)
            }

            val anniversaries = getContactEvents(false)
            size = anniversaries.size()
            for (i in 0 until size) {
                val key = anniversaries.keyAt(i)
                allContacts.firstOrNull { it.rawId == key }?.anniversaries = anniversaries.valueAt(i)
            }

            allContacts.sort()
            callback(allContacts)
        }
    }

    private fun getContactNames(favoritesOnly: Boolean): List<SimpleContact> {
        val contacts = ArrayList<SimpleContact>()
        val startNameWithSurname = context.baseConfig.startNameWithSurname
        val uri = Data.CONTENT_URI
        val projection = arrayOf(
            Data.RAW_CONTACT_ID,
            Data.CONTACT_ID,
            StructuredName.PREFIX,
            StructuredName.GIVEN_NAME,
            StructuredName.MIDDLE_NAME,
            StructuredName.FAMILY_NAME,
            StructuredName.SUFFIX,
            StructuredName.PHOTO_THUMBNAIL_URI,
            Organization.COMPANY,
            Organization.TITLE,
            Data.MIMETYPE
        )

        var selection = "(${Data.MIMETYPE} = ? OR ${Data.MIMETYPE} = ?)"

        if (favoritesOnly) {
            selection += " AND ${Data.STARRED} = 1"
        }

        val selectionArgs = arrayOf(
            StructuredName.CONTENT_ITEM_TYPE,
            Organization.CONTENT_ITEM_TYPE
        )

        context.queryCursor(uri, projection, selection, selectionArgs) { cursor ->
            val rawId = cursor.getIntValue(Data.RAW_CONTACT_ID)
            val contactId = cursor.getIntValue(Data.CONTACT_ID)
            val mimetype = cursor.getStringValue(Data.MIMETYPE)
            val photoUri = cursor.getStringValue(StructuredName.PHOTO_THUMBNAIL_URI) ?: ""
            val isPerson = mimetype == StructuredName.CONTENT_ITEM_TYPE
            if (isPerson) {
                val prefix = cursor.getStringValue(StructuredName.PREFIX) ?: ""
                val firstName = cursor.getStringValue(StructuredName.GIVEN_NAME) ?: ""
                val middleName = cursor.getStringValue(StructuredName.MIDDLE_NAME) ?: ""
                val familyName = cursor.getStringValue(StructuredName.FAMILY_NAME) ?: ""
                val suffix = cursor.getStringValue(StructuredName.SUFFIX) ?: ""
                if (firstName.isNotEmpty() || middleName.isNotEmpty() || familyName.isNotEmpty()) {
                    val names = if (startNameWithSurname) {
                        arrayOf(prefix, familyName, middleName, firstName, suffix).filter { it.isNotEmpty() }
                    } else {
                        arrayOf(prefix, firstName, middleName, familyName, suffix).filter { it.isNotEmpty() }
                    }

                    val fullName = TextUtils.join(" ", names)
                    val contact = SimpleContact(rawId, contactId, fullName, photoUri, ArrayList(), ArrayList(), ArrayList())
                    contacts.add(contact)
                }
            }

            val isOrganization = mimetype == Organization.CONTENT_ITEM_TYPE
            if (isOrganization) {
                val company = cursor.getStringValue(Organization.COMPANY) ?: ""
                val jobTitle = cursor.getStringValue(Organization.TITLE) ?: ""
                if (company.isNotEmpty() || jobTitle.isNotEmpty()) {
                    val fullName = "$company $jobTitle".trim()
                    val contact = SimpleContact(rawId, contactId, fullName, photoUri, ArrayList(), ArrayList(), ArrayList())
                    contacts.add(contact)
                }
            }
        }
        return contacts
    }

    private fun getContactPhoneNumbers(favoritesOnly: Boolean): ArrayList<SimpleContact> {
        val contacts = ArrayList<SimpleContact>()
        val uri = Phone.CONTENT_URI
        val projection = arrayOf(
            Data.RAW_CONTACT_ID,
            Data.CONTACT_ID,
            Phone.NORMALIZED_NUMBER,
            Phone.NUMBER,
            Phone.TYPE,
            Phone.LABEL,
            Phone.IS_PRIMARY
        )

        val selection = if (favoritesOnly) "${Data.STARRED} = 1" else null

        context.queryCursor(uri, projection, selection) { cursor ->
            val normalizedNumber = cursor.getStringValue(Phone.NORMALIZED_NUMBER)
                ?: cursor.getStringValue(Phone.NUMBER)?.normalizePhoneNumber() ?: return@queryCursor

            val rawId = cursor.getIntValue(Data.RAW_CONTACT_ID)
            val contactId = cursor.getIntValue(Data.CONTACT_ID)
            val type = cursor.getIntValue(Phone.TYPE)
            val label = cursor.getStringValue(Phone.LABEL) ?: ""
            val isPrimary = cursor.getIntValue(Phone.IS_PRIMARY) != 0

            if (contacts.firstOrNull { it.rawId == rawId } == null) {
                val contact = SimpleContact(rawId, contactId, "", "", ArrayList(), ArrayList(), ArrayList())
                contacts.add(contact)
            }

            val phoneNumber = PhoneNumber(normalizedNumber, type, label, normalizedNumber, isPrimary)
            contacts.firstOrNull { it.rawId == rawId }?.phoneNumbers?.add(phoneNumber)
        }
        return contacts
    }

    private fun getContactEvents(getBirthdays: Boolean): SparseArray<ArrayList<String>> {
        val eventDates = SparseArray<ArrayList<String>>()
        val uri = Data.CONTENT_URI
        val projection = arrayOf(
            Data.RAW_CONTACT_ID,
            Event.START_DATE
        )

        val selection = "${Event.MIMETYPE} = ? AND ${Event.TYPE} = ?"
        val requiredType = if (getBirthdays) Event.TYPE_BIRTHDAY.toString() else Event.TYPE_ANNIVERSARY.toString()
        val selectionArgs = arrayOf(Event.CONTENT_ITEM_TYPE, requiredType)

        context.queryCursor(uri, projection, selection, selectionArgs) { cursor ->
            val id = cursor.getIntValue(Data.RAW_CONTACT_ID)
            val startDate = cursor.getStringValue(Event.START_DATE) ?: return@queryCursor

            if (eventDates[id] == null) {
                eventDates.put(id, ArrayList())
            }

            eventDates[id]!!.add(startDate)
        }

        return eventDates
    }

    fun getNameFromPhoneNumber(number: String): String {
        if (!context.hasPermission(PERMISSION_READ_CONTACTS)) {
            return number
        }

        val uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        val projection = arrayOf(
            PhoneLookup.DISPLAY_NAME
        )

        try {
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
            cursor.use {
                if (cursor?.moveToFirst() == true) {
                    return cursor.getStringValue(PhoneLookup.DISPLAY_NAME)
                }
            }
        } catch (ignored: Exception) {
        }

        return number
    }

    fun getPhotoUriFromPhoneNumber(number: String): String {
        if (!context.hasPermission(PERMISSION_READ_CONTACTS)) {
            return ""
        }

        val uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        val projection = arrayOf(
            PhoneLookup.PHOTO_URI
        )

        try {
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
            cursor.use {
                if (cursor?.moveToFirst() == true) {
                    return cursor.getStringValue(PhoneLookup.PHOTO_URI) ?: ""
                }
            }
        } catch (ignored: Exception) {
        }

        return ""
    }

    fun loadContactImage(path: String, imageView: ImageView, placeholderName: String, placeholderImage: Drawable? = null) {
        val placeholder = placeholderImage ?: BitmapDrawable(context.resources, getContactLetterIcon(placeholderName))

        val options = RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .error(placeholder)
            .centerCrop()

        Glide.with(context)
            .load(path)
            .transition(DrawableTransitionOptions.withCrossFade())
            .placeholder(placeholder)
            .apply(options)
            .apply(RequestOptions.circleCropTransform())
            .into(imageView)
    }

    fun getContactLetterIcon(name: String): Bitmap {
        val letter = name.getNameLetter()
        val size = context.resources.getDimension(R.dimen.normal_icon_size).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val view = TextView(context)
        view.layout(0, 0, size, size)

        val circlePaint = Paint().apply {
            color = letterBackgroundColors[Math.abs(name.hashCode()) % letterBackgroundColors.size].toInt()
            isAntiAlias = true
        }

        val wantedTextSize = size / 2f
        val textPaint = Paint().apply {
            color = circlePaint.color.getContrastColor()
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = wantedTextSize
            style = Paint.Style.FILL
        }

        canvas.drawCircle(size / 2f, size / 2f, size / 2f, circlePaint)

        val xPos = canvas.width / 2f
        val yPos = canvas.height / 2 - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(letter, xPos, yPos, textPaint)
        view.draw(canvas)
        return bitmap
    }

    fun getColoredGroupIcon(title: String): Drawable {
        val icon = context.resources.getDrawable(R.drawable.ic_group_circle_bg)
        val bgColor = letterBackgroundColors[Math.abs(title.hashCode()) % letterBackgroundColors.size].toInt()
        (icon as LayerDrawable).findDrawableByLayerId(R.id.attendee_circular_background).applyColorFilter(bgColor)
        return icon
    }

    fun getContactLookupKey(contactId: String): String {
        val uri = Data.CONTENT_URI
        val projection = arrayOf(Data.CONTACT_ID, Data.LOOKUP_KEY)
        val selection = "${Data.MIMETYPE} = ? AND ${Data.RAW_CONTACT_ID} = ?"
        val selectionArgs = arrayOf(StructuredName.CONTENT_ITEM_TYPE, contactId)

        val cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                val id = cursor.getIntValue(Data.CONTACT_ID)
                val lookupKey = cursor.getStringValue(Data.LOOKUP_KEY)
                return "$lookupKey/$id"
            }
        }

        return ""
    }

    fun deleteContactRawIDs(ids: ArrayList<Int>, callback: () -> Unit) {
        ensureBackgroundThread {
            val uri = Data.CONTENT_URI
            ids.chunked(30).forEach { chunk ->
                val selection = "${Data.RAW_CONTACT_ID} IN (${getQuestionMarks(chunk.size)})"
                val selectionArgs = chunk.map { it.toString() }.toTypedArray()
                context.contentResolver.delete(uri, selection, selectionArgs)
            }
            callback()
        }
    }

    fun getShortcutImage(path: String, placeholderName: String, callback: (image: Bitmap) -> Unit) {
        ensureBackgroundThread {
            val placeholder = BitmapDrawable(context.resources, getContactLetterIcon(placeholderName))
            try {
                val options = RequestOptions()
                    .format(DecodeFormat.PREFER_ARGB_8888)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .error(placeholder)
                    .centerCrop()

                val size = context.resources.getDimension(R.dimen.shortcut_size).toInt()
                val bitmap = Glide.with(context).asBitmap()
                    .load(path)
                    .placeholder(placeholder)
                    .apply(options)
                    .apply(RequestOptions.circleCropTransform())
                    .into(size, size)
                    .get()

                callback(bitmap)
            } catch (ignored: Exception) {
                callback(placeholder.bitmap)
            }
        }
    }

    fun exists(number: String, privateCursor: Cursor?, callback: (Boolean) -> Unit) {
        SimpleContactsHelper(context).getAvailableContacts(false) { contacts ->
            val contact = contacts.firstOrNull { it.doesHavePhoneNumber(number) }
            if (contact != null) {
                callback.invoke(true)
            } else {
                val privateContacts = MyContactsContentProvider.getSimpleContacts(context, privateCursor)
                val privateContact = privateContacts.firstOrNull { it.doesHavePhoneNumber(number) }
                callback.invoke(privateContact != null)
            }
        }
    }
}
