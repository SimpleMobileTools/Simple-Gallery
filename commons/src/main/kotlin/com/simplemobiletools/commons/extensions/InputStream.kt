package com.simplemobiletools.commons.extensions

import com.simplemobiletools.commons.helpers.MD5
import java.io.InputStream
import java.security.MessageDigest

fun InputStream.getDigest(algorithm: String): String {
    return use { fis ->
        val md = MessageDigest.getInstance(algorithm)
        val buffer = ByteArray(8192)
        generateSequence {
            when (val bytesRead = fis.read(buffer)) {
                -1 -> null
                else -> bytesRead
            }
        }.forEach { bytesRead -> md.update(buffer, 0, bytesRead) }
        md.digest().joinToString("") { "%02x".format(it) }
    }
}

fun InputStream.md5(): String = this.getDigest(MD5)
