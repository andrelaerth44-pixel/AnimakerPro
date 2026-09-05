package com.animakerpro.app

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import kotlin.math.atan2

/** Android gesture shell; sampling/rendering stay in the native C++ engine. */
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

    var brushSize = 8f; set(value){field=value.coerceIn(1f,200f);syncBrush()}
    var brushOpacity = 1f; set(value){field=value.coerceIn(.01f,1f);syncBrush()}
    var pressureSensitivity = true; set(value){field=value;syncBrush()}
    var stabilization = .35f; set(value){field=value.coerceIn(0f,.9f);syncBrush()}
    var pressureSize = 1f; set(value){field=value.coerceIn(0f,2f);syncAdvanced()}
    var pressureOpacity = .25f; set(value){field=value.coerceIn(0f,1f);syncAdvanced()}
    var spacing = 3f; set(value){field=value.coerceIn(1f,40f);syncAdvanced()}
    var eraser = false; set(value){field=value;if(nativeHandle!=0L)nativeSetEraser(nativeHandle,value)}
    var onionSkin = true; set(value){field=value;if(nativeHandle!=0L)nativeSetOnionSkin(nativeHandle,value)}

    private val scaleDetector=ScaleGestureDetector(context,object:ScaleGestureDetector.SimpleOnScaleGestureListener(){override fun onScale(d:ScaleGestureDetector):Boolean{zoom=(zoom*d.scaleFactor).coerceIn(.15f,8f);syncTransform();return true}})
    init{setEGLContextClientVersion(3);setRenderer(object:Renderer{
        override fun onSurfaceCreated(gl:javax.microedition.khronos.opengles.GL10?,config:javax.microedition.khronos.egl.EGLConfig?){nativeHandle=nativeCreate();syncBrush();syncAdvanced();nativeSetEraser(nativeHandle,eraser);nativeSetOnionSkin(nativeHandle,onionSkin)}
        override fun onSurfaceChanged(gl:javax.microedition.khronos.opengles.GL10?,w:Int,h:Int){if(nativeHandle!=0L)nativeResize(nativeHandle,w,h)}
        override fun onDrawFrame(gl:javax.microedition.khronos.opengles.GL10?){if(nativeHandle!=0L)nativeRender(nativeHandle)}});renderMode=RENDERMODE_CONTINUOUSLY;preserveEGLContextOnPause=true}
    override fun onDetachedFromWindow(){if(nativeHandle!=0L){nativeDestroy(nativeHandle);nativeHandle=0L};super.onDetachedFromWindow()}
    override fun onTouchEvent(e:MotionEvent):Boolean{scaleDetector.onTouchEvent(e);when(e.actionMasked){MotionEvent.ACTION_DOWN->{activeStroke=true;nativeBeginStroke(nativeHandle,e.x,e.y,pressure(e,0));return true};MotionEvent.ACTION_POINTER_DOWN->{activeStroke=false;if(e.pointerCount>=2){lastAngle=angle(e);lastSpanX=(e.getX(0)+e.getX(1))/2f;lastSpanY=(e.getY(0)+e.getY(1))/2f};nativeEndStroke(nativeHandle);return true};MotionEvent.ACTION_MOVE->{if(e.pointerCount>=2){val cx=(e.getX(0)+e.getX(1))/2f;val cy=(e.getY(0)+e.getY(1))/2f;panX+=cx-lastSpanX;panY+=cy-lastSpanY;var d=angle(e)-lastAngle;if(d>Math.PI)d-=(2*Math.PI).toFloat();if(d< -Math.PI)d+=(2*Math.PI).toFloat();rotation+=d;lastAngle=angle(e);lastSpanX=cx;lastSpanY=cy;syncTransform();return true};if(activeStroke){nativeMoveStroke(nativeHandle,e.x,e.y,pressure(e,0));return true}};MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->{if(activeStroke)nativeEndStroke(nativeHandle);activeStroke=false;performClick();return true}};return true}
    override fun performClick()=super.performClick()
    fun setFrame(i:Int){if(nativeHandle!=0L)nativeSetFrame(nativeHandle,i)};fun ensureFrames(c:Int){if(nativeHandle!=0L)nativeEnsureFrames(nativeHandle,c)};fun insertFrame(i:Int){if(nativeHandle!=0L)nativeInsertFrame(nativeHandle,i)};fun duplicateFrame(s:Int,i:Int){if(nativeHandle!=0L)nativeDuplicateFrame(nativeHandle,s,i)};fun removeFrame(i:Int){if(nativeHandle!=0L)nativeRemoveFrame(nativeHandle,i)};fun makeLoop(s:Int,e:Int){if(nativeHandle!=0L)nativeMakeLoop(nativeHandle,s,e)};fun clearCurrentFrame(){if(nativeHandle!=0L)nativeClearFrame(nativeHandle)}
    private fun syncBrush(){if(nativeHandle!=0L)nativeSetBrush(nativeHandle,brushSize,brushOpacity,pressureSensitivity,stabilization)};private fun syncAdvanced(){if(nativeHandle!=0L)nativeSetBrushAdvanced(nativeHandle,pressureSize,pressureOpacity,spacing,0f,0f)};private fun syncTransform(){if(nativeHandle!=0L)nativeSetTransform(nativeHandle,zoom,panX,panY,rotation)};private fun pressure(e:MotionEvent,p:Int)=e.getPressure(p).coerceIn(.05f,1.5f);private fun angle(e:MotionEvent)=atan2(e.getY(1)-e.getY(0),e.getX(1)-e.getX(0)).toFloat()
    companion object{init{System.loadLibrary("animakerpro_native")};@JvmStatic private external fun nativeCreate():Long;@JvmStatic private external fun nativeDestroy(h:Long);@JvmStatic private external fun nativeResize(h:Long,w:Int,hh:Int);@JvmStatic private external fun nativeRender(h:Long);@JvmStatic private external fun nativeBeginStroke(h:Long,x:Float,y:Float,p:Float);@JvmStatic private external fun nativeMoveStroke(h:Long,x:Float,y:Float,p:Float);@JvmStatic private external fun nativeEndStroke(h:Long);@JvmStatic private external fun nativeSetBrush(h:Long,s:Float,o:Float,p:Boolean,st:Float);@JvmStatic private external fun nativeSetBrushAdvanced(h:Long,pS:Float,pO:Float,sp:Float,tS:Float,tE:Float);@JvmStatic private external fun nativeSetEraser(h:Long,en:Boolean);@JvmStatic private external fun nativeSetFrame(h:Long,f:Int);@JvmStatic private external fun nativeClearFrame(h:Long);@JvmStatic private external fun nativeSetTransform(h:Long,z:Float,x:Float,y:Float,r:Float);@JvmStatic private external fun nativeSetOnionSkin(h:Long,en:Boolean);@JvmStatic private external fun nativeEnsureFrames(h:Long,c:Int);@JvmStatic private external fun nativeInsertFrame(h:Long,i:Int);@JvmStatic private external fun nativeDuplicateFrame(h:Long,s:Int,i:Int);@JvmStatic private external fun nativeRemoveFrame(h:Long,i:Int);@JvmStatic private external fun nativeMakeLoop(h:Long,s:Int,e:Int)}
}
