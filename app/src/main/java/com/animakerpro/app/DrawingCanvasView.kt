package com.animakerpro.app

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class DrawingCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    enum class RulerMode {
        OFF, STRAIGHT, CIRCULAR, ELLIPTICAL, RADIAL, MIRROR, KALEIDOSCOPE, ROTATION,
        ARRAY, PERSPECTIVE_1, PERSPECTIVE_2, PERSPECTIVE_3
    }

    var document: AnimationDocument? = null
        set(value) { field = value; invalidate() }
    var onionSkin = true
    var rulerMode = RulerMode.OFF
        set(value) { field = value; invalidate() }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private var lastX = 0f
    private var lastY = 0f
    private var strokePath: Path? = null
    private var zoom = 1f
    private var panX = 0f
    private var panY = 0f
    private var lastDistance = 0f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val doc = document ?: return
        canvas.drawColor(Color.rgb(30, 32, 38))

        val bitmap = doc.frames[doc.currentFrame].bitmap
        val scale = min(width.toFloat() / doc.width, height.toFloat() / doc.height) * zoom
        val left = (width - doc.width * scale) / 2f + panX
        val top = (height - doc.height * scale) / 2f + panY
        val dst = RectF(left, top, left + doc.width * scale, top + doc.height * scale)

        canvas.drawBitmap(bitmap, null, dst, null)
        if (onionSkin && doc.currentFrame > 0) {
            val onion = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = 70 }
            canvas.drawBitmap(doc.frames[doc.currentFrame - 1].bitmap, null, dst, onion)
        }

        drawRuler(canvas, dst)
        strokePath?.let { canvas.drawPath(it, paint) }
    }

    private fun drawRuler(canvas: Canvas, rect: RectF) {
        val guide = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(145, 60, 180, 255)
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        val cx = rect.centerX()
        val cy = rect.centerY()
        when (rulerMode) {
            RulerMode.STRAIGHT -> canvas.drawLine(rect.left, cy, rect.right, cy, guide)
            RulerMode.CIRCULAR -> canvas.drawCircle(cx, cy, min(rect.width(), rect.height()) * .32f, guide)
            RulerMode.ELLIPTICAL -> canvas.drawOval(
                RectF(cx - rect.width() * .32f, cy - rect.height() * .18f,
                    cx + rect.width() * .32f, cy + rect.height() * .18f), guide)
            RulerMode.RADIAL -> {
                canvas.drawCircle(cx, cy, 8f, guide)
                for (i in 0 until 12) {
                    val a = i * Math.PI / 6.0
                    canvas.drawLine(cx, cy, cx + cos(a).toFloat() * rect.width(), cy + sin(a).toFloat() * rect.height(), guide)
                }
            }
            RulerMode.MIRROR -> {
                canvas.drawLine(cx, rect.top, cx, rect.bottom, guide)
            }
            RulerMode.KALEIDOSCOPE, RulerMode.ROTATION -> {
                canvas.drawCircle(cx, cy, 8f, guide)
                for (i in 0 until 8) {
                    val a = i * Math.PI / 4.0
                    canvas.drawLine(cx, cy, cx + cos(a).toFloat() * rect.width(), cy + sin(a).toFloat() * rect.height(), guide)
                }
            }
            RulerMode.PERSPECTIVE_1 -> drawPerspective(canvas, rect, listOf(PointF(cx, rect.top - rect.height() * .25f)))
            RulerMode.PERSPECTIVE_2 -> drawPerspective(canvas, rect, listOf(PointF(rect.left - rect.width() * .35f, cy), PointF(rect.right + rect.width() * .35f, cy)))
            RulerMode.PERSPECTIVE_3 -> drawPerspective(canvas, rect, listOf(PointF(rect.left - rect.width() * .3f, cy), PointF(rect.right + rect.width() * .3f, cy), PointF(cx, rect.bottom + rect.height() * .5f)))
            RulerMode.ARRAY -> {
                for (i in 1 until 5) canvas.drawLine(rect.left + rect.width() * i / 5f, rect.top, rect.left + rect.width() * i / 5f, rect.bottom, guide)
                for (i in 1 until 5) canvas.drawLine(rect.left, rect.top + rect.height() * i / 5f, rect.right, rect.top + rect.height() * i / 5f, guide)
            }
            else -> Unit
        }
    }

    private fun drawPerspective(canvas: Canvas, rect: RectF, points: List<PointF>) {
        val guide = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(145, 255, 180, 60)
            strokeWidth = 2f
        }
        points.forEach { p ->
            canvas.drawCircle(p.x, p.y, 7f, guide)
            canvas.drawLine(p.x, p.y, rect.left, rect.top, guide)
            canvas.drawLine(p.x, p.y, rect.right, rect.top, guide)
            canvas.drawLine(p.x, p.y, rect.left, rect.bottom, guide)
            canvas.drawLine(p.x, p.y, rect.right, rect.bottom, guide)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val doc = document ?: return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                strokePath = Path().apply { moveTo(event.x, event.y) }
                return true
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount >= 2) lastDistance = distance(event)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount >= 2) {
                    val d = distance(event)
                    if (lastDistance > 0f) zoom = (zoom * (d / lastDistance)).coerceIn(.25f, 5f)
                    lastDistance = d
                    invalidate()
                    return true
                }
                val x = event.x
                val y = event.y
                val path = strokePath ?: Path().also { strokePath = it }
                if (rulerMode == RulerMode.STRAIGHT) {
                    path.reset(); path.moveTo(lastX, lastY); path.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
                lastX = x; lastY = y
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                strokePath?.let { path ->
                    val target = doc.frames[doc.currentFrame].bitmap
                    val canvas = Canvas(target)
                    canvas.save()
                    val scale = min(width.toFloat() / doc.width, height.toFloat() / doc.height) * zoom
                    val left = (width - doc.width * scale) / 2f + panX
                    val top = (height - doc.height * scale) / 2f + panY
                    canvas.translate(-left, -top)
                    canvas.scale(1f / scale, 1f / scale)
                    val sourcePaint = Paint(paint).apply {
                        strokeWidth = 5f / scale
                        color = Color.BLACK
                    }
                    canvas.drawPath(path, sourcePaint)
                    canvas.restore()
                }
                strokePath = null
                invalidate()
                return true
            }
        }
        return true
    }

    private fun distance(e: MotionEvent): Float {
        if (e.pointerCount < 2) return 0f
        val dx = e.getX(0) - e.getX(1)
        val dy = e.getY(0) - e.getY(1)
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}
