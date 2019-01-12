package com.simplemobiletools.gallery.pro.actions

import android.graphics.Path
import java.io.Writer
import java.security.InvalidParameterException

class Move : Action {

    val x: Float
    val y: Float

    constructor(data: String) {
        if (!data.startsWith("M"))
            throw InvalidParameterException("The Move data should start with 'M'.")

        try {
            val xy = data.substring(1).split(",".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
            x = xy[0].trim().toFloat()
            y = xy[1].trim().toFloat()
        } catch (ignored: Exception) {
            throw InvalidParameterException("Error parsing the given Move data.")
        }
    }

    constructor(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    override fun perform(path: Path) {
        path.moveTo(x, y)
    }

    override fun perform(writer: Writer) {
        writer.write("M$x,$y")
    }
}
