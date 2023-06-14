package com.simplemobiletools.commons.helpers

// taken from https://gist.github.com/MichaelRocks/1b94bb44c7804e999dbf31dac86955ec
// make IMG_5.jpg come before IMG_10.jpg
class AlphanumericComparator {
    fun compare(string1: String, string2: String): Int {
        var thisMarker = 0
        var thatMarker = 0
        val s1Length = string1.length
        val s2Length = string2.length

        while (thisMarker < s1Length && thatMarker < s2Length) {
            val thisChunk = getChunk(string1, s1Length, thisMarker)
            thisMarker += thisChunk.length

            val thatChunk = getChunk(string2, s2Length, thatMarker)
            thatMarker += thatChunk.length

            // If both chunks contain numeric characters, sort them numerically.
            var result: Int
            if (isDigit(thisChunk[0]) && isDigit(thatChunk[0])) {
                // Simple chunk comparison by length.
                val thisChunkLength = thisChunk.length
                result = thisChunkLength - thatChunk.length
                // If equal, the first different number counts.
                if (result == 0) {
                    for (i in 0 until thisChunkLength) {
                        result = thisChunk[i] - thatChunk[i]
                        if (result != 0) {
                            return result
                        }
                    }
                }
            } else {
                result = thisChunk.compareTo(thatChunk)
            }

            if (result != 0) {
                return result
            }
        }

        return s1Length - s2Length
    }

    private fun getChunk(string: String, length: Int, marker: Int): String {
        var current = marker
        val chunk = StringBuilder()
        var c = string[current]
        chunk.append(c)
        current++
        if (isDigit(c)) {
            while (current < length) {
                c = string[current]
                if (!isDigit(c)) {
                    break
                }
                chunk.append(c)
                current++
            }
        } else {
            while (current < length) {
                c = string[current]
                if (isDigit(c)) {
                    break
                }
                chunk.append(c)
                current++
            }
        }
        return chunk.toString()
    }

    private fun isDigit(ch: Char) = ch in '0'..'9'
}
