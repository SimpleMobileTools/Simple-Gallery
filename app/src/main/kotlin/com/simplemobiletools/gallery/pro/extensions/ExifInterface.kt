package com.simplemobiletools.gallery.pro.extensions

import androidx.exifinterface.media.ExifInterface
import java.lang.reflect.Field
import java.lang.reflect.Modifier

fun ExifInterface.copyNonDimensionAttributesTo(destination: ExifInterface) {
    val attributes = ExifInterfaceAttributes.AllNonDimensionAttributes

    attributes.forEach {
        val value = getAttribute(it)
        if (value != null) {
            destination.setAttribute(it, value)
        }
    }

    try {
        destination.saveAttributes()
    } catch (ignored: Exception) {
    }
}

private class ExifInterfaceAttributes {
    companion object {
        val AllNonDimensionAttributes = getAllNonDimensionExifAttributes()

        private fun getAllNonDimensionExifAttributes(): List<String> {
            val tagFields = ExifInterface::class.java.fields.filter { field -> isExif(field) }

            val excludeAttributes = arrayListOf(
                ExifInterface.TAG_IMAGE_LENGTH,
                ExifInterface.TAG_IMAGE_WIDTH,
                ExifInterface.TAG_PIXEL_X_DIMENSION,
                ExifInterface.TAG_PIXEL_Y_DIMENSION,
                ExifInterface.TAG_THUMBNAIL_IMAGE_LENGTH,
                ExifInterface.TAG_THUMBNAIL_IMAGE_WIDTH,
                ExifInterface.TAG_ORIENTATION)

            return tagFields
                .map { tagField -> tagField.get(null) as String }
                .filter { x -> !excludeAttributes.contains(x) }
                .distinct()
        }

        private fun isExif(field: Field): Boolean {
            return field.type == String::class.java &&
                    isPublicStaticFinal(field.modifiers) &&
                    field.name.startsWith("TAG_")
        }

        private const val publicStaticFinal = Modifier.PUBLIC or Modifier.STATIC or Modifier.FINAL

        private fun isPublicStaticFinal(modifiers: Int): Boolean {
            return modifiers and publicStaticFinal > 0
        }
    }
}
