package com.simplemobiletools.gallery.pro.actions

import android.graphics.Path
import java.io.Writer
import java.security.InvalidParameterException

class Line : Action {

    val x: Float
    val y: Float

    constructor(data: String) {
        if (!data.startsWith("L"))
            throw InvalidParameterException("The Line data should start with 'L'.")

        try {
            val xy = data.substring(1).split(",".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
            x = xy[0].trim().toFloat()
            y = xy[1].trim().toFloat()
        } catch (ignored: Exception) {
            throw InvalidParameterException("Error parsing the given Line data.")
        }
    }

    constructor(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    override fun perform(path: Path) {
        path.lineTo(x, y)
    }

    override fun perform(writer: Writer) {
        writer.write("L$x,$y")
    }
}
