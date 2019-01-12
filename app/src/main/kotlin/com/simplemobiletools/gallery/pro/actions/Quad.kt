package com.simplemobiletools.gallery.pro.actions

import android.graphics.Path
import java.io.Writer
import java.security.InvalidParameterException

class Quad : Action {

    val x1: Float
    val y1: Float
    val x2: Float
    val y2: Float

    constructor(data: String) {
        if (!data.startsWith("Q"))
            throw InvalidParameterException("The Quad data should start with 'Q'.")

        try {
            val parts = data.split("\\s+".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
            val xy1 = parts[0].substring(1).split(",".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
            val xy2 = parts[1].split(",".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()

            x1 = xy1[0].trim().toFloat()
            y1 = xy1[1].trim().toFloat()
            x2 = xy2[0].trim().toFloat()
            y2 = xy2[1].trim().toFloat()
        } catch (ignored: Exception) {
            throw InvalidParameterException("Error parsing the given Quad data.")
        }
    }

    constructor(x1: Float, y1: Float, x2: Float, y2: Float) {
        this.x1 = x1
        this.y1 = y1
        this.x2 = x2
        this.y2 = y2
    }

    override fun perform(path: Path) {
        path.quadTo(x1, y1, x2, y2)
    }

    override fun perform(writer: Writer) {
        writer.write("Q$x1,$y1 $x2,$y2")
    }
}
