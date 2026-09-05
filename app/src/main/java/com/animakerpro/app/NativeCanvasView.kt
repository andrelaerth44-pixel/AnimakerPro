package com.animakerpro.app

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import kotlin.math.atan2

/** Android lifecycle/gesture shell; brush sampling and rendering live in C++. */
class NativeCanvasView(context: Context) : GLSurfaceView(context) {
    private var nativeHandle = 0L
    private var activeStroke = false
    private var zoom = 1f
    private var panX = 0f
    private var panY = 0f
    private var rotation = 0f
    private var lastAngle = 0f
    private var lastSpanX = 0f
    private var lastSpanY = 0f

    var brushSize = 8f
        set(value) { field = value.coerceIn(1f, 200f); syncBrush() }
    var brushOpacity = 1f
        set(value) { field = value.coerceIn(.01f, 1f); syncBrush() }
    var pressureSensitivity = true
        set(value) { field = value; syncBrush() }
    var stabilization = .35f
        set(value) { field = value.coerceIn(0f, .9f); syncBrush() }
    var onionSkin = true
        set(value) { field = value; if (nativeHandle != 0L) nativeSetOnionSkin(nativeHandle, value) }

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            zoom = (zoom * detector.scaleFactor).coerceIn(.15f, 8f)
            syncTransform(); return true
        }
    })

    init {
        setEGLContextClientVersion(3)
        setRenderer(object : Renderer {
            override fun onSurfaceCreated(gl: javax.microedition.khronos.opengles.GL10?, config: javax.microedition.khronos.egl.EGLConfig?) {
                nativeHandle = nativeCreate(); syncBrush(); nativeSetOnionSkin(nativeHandle, onionSkin)
            }
            override fun onSurfaceChanged(gl: javax.microedition.khronos.opengles.GL10?, width: Int, height: Int) { if (nativeHandle != 0L) nativeResize(nativeHandle, width, height) }
            override fun onDrawFrame(gl: javax.microedition.khronos.opengles.GL10?) { if (nativeHandle != 0L) nativeRender(nativeHandle) }
        })
        renderMode = RENDERMODE_CONTINUOUSLY
        preserveEGLContextOnPause = true
    }

    override fun onDetachedFromWindow() {
        if (nativeHandle != 0L) { nativeDestroy(nativeHandle); nativeHandle = 0L }
        super.onDetachedFromWindow()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activeStroke = true
                nativeBeginStroke(nativeHandle, event.x, event.y, pressure(event, 0)); return true
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                activeStroke = false
                if (event.pointerCount >= 2) { lastAngle = angle(event); lastSpanX=(event.getX(0)+event.getX(1))/2f; lastSpanY=(event.getY(0)+event.getY(1))/2f }
                nativeEndStroke(nativeHandle); return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount >= 2) {
                    val cx=(event.getX(0)+event.getX(1))/2f; val cy=(event.getY(0)+event.getY(1))/2f
                    panX += cx-lastSpanX; panY += cy-lastSpanY
                    var delta=angle(event)-lastAngle
                    if(delta>Math.PI)delta-=(2*Math.PI).toFloat(); if(delta< -Math.PI)delta+=(2*Math.PI).toFloat()
                    rotation += delta; lastAngle=angle(event); lastSpanX=cx; lastSpanY=cy; syncTransform(); return true
                }
                if(activeStroke){nativeMoveStroke(nativeHandle,event.x,event.y,pressure(event,0));return true}
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { if(activeStroke)nativeEndStroke(nativeHandle);activeStroke=false;performClick();return true }
        }
        return true
    }

    override fun performClick(): Boolean = super.performClick()
    fun setFrame(index:Int){if(nativeHandle!=0L)nativeSetFrame(nativeHandle,index)}
    fun ensureFrames(count:Int){if(nativeHandle!=0L)nativeEnsureFrames(nativeHandle,count)}
    fun insertFrame(index:Int){if(nativeHandle!=0L)nativeInsertFrame(nativeHandle,index)}
    fun duplicateFrame(source:Int,insert:Int){if(nativeHandle!=0L)nativeDuplicateFrame(nativeHandle,source,insert)}
    fun removeFrame(index:Int){if(nativeHandle!=0L)nativeRemoveFrame(nativeHandle,index)}
    fun makeLoop(start:Int,end:Int){if(nativeHandle!=0L)nativeMakeLoop(nativeHandle,start,end)}
    fun clearCurrentFrame(){if(nativeHandle!=0L)nativeClearFrame(nativeHandle)}
    private fun syncBrush(){if(nativeHandle!=0L)nativeSetBrush(nativeHandle,brushSize,brushOpacity,pressureSensitivity,stabilization)}
    private fun syncTransform(){if(nativeHandle!=0L)nativeSetTransform(nativeHandle,zoom,panX,panY,rotation)}
    private fun pressure(event:MotionEvent,pointer:Int)=event.getPressure(pointer).coerceIn(.05f,1.5f)
    private fun angle(e:MotionEvent)=atan2(e.getY(1)-e.getY(0),e.getX(1)-e.getX(0)).toFloat()

    companion object {
        init{System.loadLibrary("animakerpro_native")}
        @JvmStatic private external fun nativeCreate():Long
        @JvmStatic private external fun nativeDestroy(handle:Long)
        @JvmStatic private external fun nativeResize(handle:Long,width:Int,height:Int)
        @JvmStatic private external fun nativeRender(handle:Long)
        @JvmStatic private external fun nativeBeginStroke(handle:Long,x:Float,y:Float,pressure:Float)
        @JvmStatic private external fun nativeMoveStroke(handle:Long,x:Float,y:Float,pressure:Float)
        @JvmStatic private external fun nativeEndStroke(handle:Long)
        @JvmStatic private external fun nativeSetBrush(handle:Long,size:Float,opacity:Float,pressure:Boolean,stabilization:Float)
        @JvmStatic private external fun nativeSetFrame(handle:Long,frame:Int)
        @JvmStatic private external fun nativeClearFrame(handle:Long)
        @JvmStatic private external fun nativeSetTransform(handle:Long,zoom:Float,panX:Float,panY:Float,rotation:Float)
        @JvmStatic private external fun nativeSetOnionSkin(handle:Long,enabled:Boolean)
        @JvmStatic private external fun nativeEnsureFrames(handle:Long,count:Int)
        @JvmStatic private external fun nativeInsertFrame(handle:Long,index:Int)
        @JvmStatic private external fun nativeDuplicateFrame(handle:Long,source:Int,insert:Int)
        @JvmStatic private external fun nativeRemoveFrame(handle:Long,index:Int)
        @JvmStatic private external fun nativeMakeLoop(handle:Long,start:Int,end:Int)
    }
}
