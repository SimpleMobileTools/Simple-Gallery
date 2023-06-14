package com.simplemobiletools.commons.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.Shader.TileMode
import android.util.AttributeSet
import android.view.View

class ColorPickerSquare(context: Context, attrs: AttributeSet) : View(context, attrs) {
    var paint: Paint? = null
    var luar: Shader = LinearGradient(0f, 0f, 0f, measuredHeight.toFloat(), Color.WHITE, Color.BLACK, Shader.TileMode.CLAMP)
    val color = floatArrayOf(1f, 1f, 1f)

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (paint == null) {
            paint = Paint()
            luar = LinearGradient(0f, 0f, 0f, measuredHeight.toFloat(), Color.WHITE, Color.BLACK, TileMode.CLAMP)
        }
        val rgb = Color.HSVToColor(color)
        val dalam = LinearGradient(0f, 0f, measuredWidth.toFloat(), 0f, Color.WHITE, rgb, TileMode.CLAMP)
        val shader = ComposeShader(luar, dalam, PorterDuff.Mode.MULTIPLY)
        paint!!.shader = shader
        canvas.drawRect(0f, 0f, measuredWidth.toFloat(), measuredHeight.toFloat(), paint!!)
    }

    fun setHue(hue: Float) {
        color[0] = hue
        invalidate()
    }
}
