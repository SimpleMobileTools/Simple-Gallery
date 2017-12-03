package com.simplemobiletools.gallery.extensions

import java.util.*

fun <E> ArrayList<E>.sumByLong(selector: (E) -> Long) = map { selector(it) }.sum()
