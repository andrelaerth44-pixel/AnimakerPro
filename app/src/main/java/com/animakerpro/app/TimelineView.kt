package com.animakerpro.app

import android.content.Context
import android.graphics.*
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.max

class TimelineView(context: Context) : View(context) {
    var document: AnimationDocument? = null
        set(value) { field = value; invalidate() }
    var onFrameSelected: ((Int) -> Unit)? = null
    var onFrameLongPressed: ((Int) -> Unit)? = null

    private val cellWidth = 88f
    private val detector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val index = ((e.x - scrollX) / cellWidth).toInt()
            document?.let { if (index in it.frames.indices) onFrameSelected?.invoke(index) }
            return true
        }
        override fun onLongPress(e: MotionEvent) {
            val index = ((e.x - scrollX) / cellWidth).toInt()
            document?.let { if (index in it.frames.indices) onFrameLongPressed?.invoke(index) }
        }
    })

    init { isHorizontalScrollBarEnabled = true }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val doc = document ?: return
        canvas.drawColor(Color.rgb(23, 25, 30))
        val total = max(width, (doc.frames.size * cellWidth).toInt())
        setMinimumWidth(total)

        val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(65, 69, 78)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val selected = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(75, 140, 255)
            style = Paint.Style.FILL
        }
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 12f
        }

        doc.frames.forEachIndexed { index, frame ->
            val left = index * cellWidth
            val right = left + cellWidth - 4
            if (index == doc.currentFrame) canvas.drawRect(left, 4f, right, height - 4f, selected)
            canvas.drawRect(left, 4f, right, height - 4f, grid)
            val thumb = RectF(left + 4, 18f, right - 4, height - 10f)
            canvas.drawBitmap(frame.bitmap, null, thumb, null)
            canvas.drawText("${index + 1}", left + 5, 15f, text)
            if (frame.exposure > 1) canvas.drawText("x${frame.exposure}", right - 25, 15f, text)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean = detector.onTouchEvent(event)
}
