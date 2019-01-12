package com.simplemobiletools.gallery.pro.models

import android.app.Activity
import android.graphics.Path
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.actions.Action
import com.simplemobiletools.gallery.pro.actions.Line
import com.simplemobiletools.gallery.pro.actions.Move
import com.simplemobiletools.gallery.pro.actions.Quad
import java.io.ObjectInputStream
import java.io.Serializable
import java.security.InvalidParameterException
import java.util.*

// https://stackoverflow.com/a/8127953
class MyPath : Path(), Serializable {
    val actions = LinkedList<Action>()

    private fun readObject(inputStream: ObjectInputStream) {
        inputStream.defaultReadObject()

        val copiedActions = actions.map { it }
        copiedActions.forEach {
            it.perform(this)
        }
    }

    fun readObject(pathData: String, activity: Activity) {
        val tokens = pathData.split("\\s+".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
        var i = 0
        try {
            while (i < tokens.size) {
                when (tokens[i][0]) {
                    'M' -> addAction(Move(tokens[i]))
                    'L' -> addAction(Line(tokens[i]))
                    'Q' -> {
                        // Quad actions are of the following form:
                        // "Qx1,y1 x2,y2"
                        // Since we split the tokens by whitespace, we need to join them again
                        if (i + 1 >= tokens.size)
                            throw InvalidParameterException("Error parsing the data for a Quad.")

                        addAction(Quad(tokens[i] + " " + tokens[i + 1]))
                        ++i
                    }
                }
                ++i
            }
        } catch (e: Exception) {
            activity.toast(R.string.unknown_error_occurred)
        }
    }

    override fun reset() {
        actions.clear()
        super.reset()
    }

    private fun addAction(action: Action) {
        when (action) {
            is Move -> moveTo(action.x, action.y)
            is Line -> lineTo(action.x, action.y)
            is Quad -> quadTo(action.x1, action.y1, action.x2, action.y2)
        }
    }

    override fun moveTo(x: Float, y: Float) {
        actions.add(Move(x, y))
        super.moveTo(x, y)
    }

    override fun lineTo(x: Float, y: Float) {
        actions.add(Line(x, y))
        super.lineTo(x, y)
    }

    override fun quadTo(x1: Float, y1: Float, x2: Float, y2: Float) {
        actions.add(Quad(x1, y1, x2, y2))
        super.quadTo(x1, y1, x2, y2)
    }
}
