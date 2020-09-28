package com.simplemobiletools.gallery.pro.extensions

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Suppress("NOTHING_TO_INLINE")
inline fun Instant.toLocalDate(
        zoneId: ZoneId = ZoneId.systemDefault()
): LocalDate = atZone(zoneId).toLocalDate()
