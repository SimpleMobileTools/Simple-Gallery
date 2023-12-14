package org.fossify.gallery.extensions

import org.fossify.commons.helpers.SORT_DESCENDING

fun Int.isSortingAscending() = this and SORT_DESCENDING == 0
