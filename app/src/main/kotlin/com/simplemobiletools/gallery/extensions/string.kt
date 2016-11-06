package com.simplemobiletools.gallery.extensions

import java.util.regex.Pattern

fun String.isNameValid(): Boolean {
    val pattern = Pattern.compile("^[-_.A-Za-z0-9()#& ]+$")
    val matcher = pattern.matcher(this)
    return matcher.matches()
}
