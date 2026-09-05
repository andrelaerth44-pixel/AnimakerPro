package com.animakerpro.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Lightweight GPU-independent guide layer; stroke sampling remains native C++. */
class RulerOverlayView(context: Context) : View(context) {
    enum class Mode { OFF, STRAIGHT, CIRCULAR, ELLIPTICAL, RADIAL, MIRROR, KALEIDOSCOPE, ROTATION, ARRAY, PERSPECTIVE_1, PERSPECTIVE_2, PERSPECTIVE_3 }
    var mode = Mode.OFF
        set(value) { field = value; invalidate() }
    var radialCount = 12
        set(value) { field = value.coerceIn(2, 64); invalidate() }
    var perspectiveDivisions = 4
        set(value) { field = value.coerceIn(1, 20); invalidate() }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.5f }

    init { setWillNotDraw(false); isClickable = false; isFocusable = false }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mode == Mode.OFF) return
        val cx = width / 2f; val cy = height / 2f
        val r = min(width, height) * .42f
        paint.color = Color.argb(150, 70, 160, 255)
        when (mode) {
            Mode.STRAIGHT -> canvas.drawLine(0f, cy, width.toFloat(), cy, paint)
            Mode.CIRCULAR -> canvas.drawCircle(cx, cy, r, paint)
            Mode.ELLIPTICAL -> canvas.drawOval(cx-r, cy-r*.5f, cx+r, cy+r*.5f, paint)
            Mode.RADIAL, Mode.KALEIDOSCOPE, Mode.ROTATION -> {
                canvas.drawCircle(cx, cy, 6f, paint)
                val count = if (mode == Mode.RADIAL) radialCount else 8
                for (i in 0 until count) {
                    val a = i * Math.PI * 2.0 / count
                    canvas.drawLine(cx, cy, cx + cos(a).toFloat()*width, cy + sin(a).toFloat()*height, paint)
                }
            }
            Mode.MIRROR -> canvas.drawLine(cx, 0f, cx, height.toFloat(), paint)
            Mode.ARRAY -> {
                for (i in 1 until perspectiveDivisions) {
                    val x = width * i / perspectiveDivisions.toFloat()
                    val y = height * i / perspectiveDivisions.toFloat()
                    canvas.drawLine(x, 0f, x, height.toFloat(), paint)
                    canvas.drawLine(0f, y, width.toFloat(), y, paint)
                }
            }
            Mode.PERSPECTIVE_1 -> perspective(canvas, listOf(cx to -height*.15f))
            Mode.PERSPECTIVE_2 -> perspective(canvas, listOf(-width*.2f to cy, width*1.2f to cy))
            Mode.PERSPECTIVE_3 -> perspective(canvas, listOf(-width*.2f to cy, width*1.2f to cy, cx to height*1.2f))
            else -> Unit
        }
    }

    private fun perspective(canvas: Canvas, points: List<Pair<Float, Float>>) {
        val corners = floatArrayOf(0f,0f,width.toFloat(),0f,0f,height.toFloat(),width.toFloat(),height.toFloat())
        paint.color = Color.argb(135, 255, 170, 65)
        for ((px,py) in points) {
            canvas.drawCircle(px, py, 6f, paint)
            for (i in corners.indices step 2) canvas.drawLine(px, py, corners[i], corners[i+1], paint)
        }
    }
}
