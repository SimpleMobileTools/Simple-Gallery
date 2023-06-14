package com.simplemobiletools.commons.extensions

import java.util.*

fun <T> ArrayList<T>.moveLastItemToFront() {
    val last = removeAt(size - 1)
    add(0, last)
}
