@file:Suppress("NOTHING_TO_INLINE")

package com.simplemobiletools.gallery.pro.extensions

import java.time.Instant
import java.time.ZoneId

inline fun Long.toInstant(
        isMillis: Boolean = true
): Instant = if (isMillis) Instant.ofEpochMilli(this) else Instant.ofEpochSecond(this)

inline fun Long.toLocalDate(
        isMillis: Boolean = true,
        zoneId: ZoneId = ZoneId.systemDefault()
) = toInstant(isMillis).toLocalDate(zoneId)
