package com.animakerpro.app

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private lateinit var document: AnimationDocument
    private lateinit var canvasView: NativeCanvasView
    private lateinit var rulerOverlay: RulerOverlayView
    private lateinit var timeline: TimelineView
    private val handler = Handler(Looper.getMainLooper())
    private var playing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        document = AnimationDocument()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(17,19,24)) }
        root.addView(toolbar(), LinearLayout.LayoutParams(-1, dp(52)))

        val canvasHost = FrameLayout(this).apply { setBackgroundColor(Color.rgb(235,235,235)) }
        canvasView = NativeCanvasView(this)
        rulerOverlay = RulerOverlayView(this)
        canvasHost.addView(canvasView, FrameLayout.LayoutParams(-1,-1))
        canvasHost.addView(rulerOverlay, FrameLayout.LayoutParams(-1,-1))
        root.addView(canvasHost, LinearLayout.LayoutParams(-1,0,1f))

        val timelineScroll = HorizontalScrollView(this).apply { setBackgroundColor(Color.rgb(23,25,30)); isHorizontalScrollBarEnabled = true }
        timeline = TimelineView(this).apply {
            document = this@MainActivity.document
            onFrameSelected = { index -> document.select(index); canvasView.setFrame(index); refresh() }
            onFrameLongPressed = { index -> showFrameActions(index) }
        }
        timelineScroll.addView(timeline, HorizontalScrollView.LayoutParams(-1,dp(118)))
        root.addView(timelineScroll)
        root.addView(animationControls(), LinearLayout.LayoutParams(-1,dp(48)))
        setContentView(root)
    }

    private fun toolbar(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(4),0,dp(4),0); setBackgroundColor(Color.rgb(24,27,33))
        addButton("☰") { showInfo("AnimakerPro", "Canvas nativo C++ • frame-by-frame • rig em desenvolvimento") }
        addButton("🖌") { showBrushSettings() }
        addButton("⌫") { showInfo("Borracha", "O modo borracha usará o mesmo pipeline nativo do pincel.") }
        addButton("▱") { showInfo("Balde", "Fill engine nativo será conectado ao raster core.") }
        addButton("⌁") { showInfo("Laço", "Seleção raster/vetorial está no próximo módulo de ferramentas.") }
        addButton("◉") { showInfo("Seletor de cor", "Color picker nativo será conectado ao framebuffer.") }
        addButton("⌖") { showRulerPicker() }
        addButton("◌") { canvasView.onionSkin = !canvasView.onionSkin; canvasView.invalidate() }
        addView(Space(this), LinearLayout.LayoutParams(0,-1,1f))
        addButton("＋") { addFrame() }; addButton("▣") { duplicateFrame() }; addButton("▤") { showFramesViewer() }
    }

    private fun animationControls(): View = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL; setPadding(dp(4),0,dp(4),0); setBackgroundColor(Color.rgb(20,22,27))
        addButton("|◀") { document.select(0); canvasView.setFrame(0); refresh() }
        addButton("◀") { document.select(document.currentFrame-1); canvasView.setFrame(document.currentFrame); refresh() }
        addButton(if(playing) "■" else "▶") { togglePlayback() }
        addButton("▶") { document.select(document.currentFrame+1); canvasView.setFrame(document.currentFrame); refresh() }
        addButton("▶|") { document.select(document.frames.lastIndex); canvasView.setFrame(document.currentFrame); refresh() }
        addButton("+ Frame") { addFrame() }; addButton("Clone") { duplicateFrame() }; addButton("Loop") { makeLoop() }
    }

    private fun showBrushSettings() {
        val panel = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(20),dp(8),dp(20),0) }
        val sizeLabel=TextView(this).apply{text="Tamanho: ${canvasView.brushSize.toInt()} px";setTextColor(Color.WHITE)}
        val size=SeekBar(this).apply{max=199;progress=canvasView.brushSize.toInt()-1}
        size.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(s:SeekBar?,p:Int,u:Boolean){canvasView.brushSize=(p+1).toFloat();sizeLabel.text="Tamanho: ${p+1} px"};override fun onStartTrackingTouch(s:SeekBar?){};override fun onStopTrackingTouch(s:SeekBar?){}})
        val opacityLabel=TextView(this).apply{text="Opacidade: ${(canvasView.brushOpacity*100).toInt()}%";setTextColor(Color.WHITE)}
        val opacity=SeekBar(this).apply{max=100;progress=(canvasView.brushOpacity*100).toInt()}
        opacity.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(s:SeekBar?,p:Int,u:Boolean){canvasView.brushOpacity=(p.coerceAtLeast(1)/100f);opacityLabel.text="Opacidade: ${p.coerceAtLeast(1)}%"};override fun onStartTrackingTouch(s:SeekBar?){};override fun onStopTrackingTouch(s:SeekBar?){}})
        val stabLabel=TextView(this).apply{text="Estabilização: ${(canvasView.stabilization*100).toInt()}%";setTextColor(Color.WHITE)}
        val stab=SeekBar(this).apply{max=90;progress=(canvasView.stabilization*100).toInt()}
        stab.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(s:SeekBar?,p:Int,u:Boolean){canvasView.stabilization=p/100f;stabLabel.text="Estabilização: $p%"};override fun onStartTrackingTouch(s:SeekBar?){};override fun onStopTrackingTouch(s:SeekBar?){}})
        val pressure=CheckBox(this).apply{text="Pressão / espessura dinâmica";isChecked=canvasView.pressureSensitivity;setTextColor(Color.WHITE)}
        pressure.setOnCheckedChangeListener{_,checked->canvasView.pressureSensitivity=checked}
        panel.addView(sizeLabel);panel.addView(size);panel.addView(opacityLabel);panel.addView(opacity);panel.addView(stabLabel);panel.addView(stab);panel.addView(pressure)
        AlertDialog.Builder(this).setTitle("Pincel C++").setView(panel).setPositiveButton("OK",null).show()
    }

    private fun showRulerPicker(){val modes=RulerOverlayView.Mode.values();val labels=modes.map{it.name.replace('_',' ')}.toTypedArray();AlertDialog.Builder(this).setTitle("Réguas").setSingleChoiceItems(labels,modes.indexOf(rulerOverlay.mode)){d,w->rulerOverlay.mode=modes[w];d.dismiss()}.show()}

    private fun addFrame(){val index=document.addBlank();canvasView.ensureFrames(document.frames.size);canvasView.setFrame(index);refresh()}
    private fun duplicateFrame(){val source=document.currentFrame;val insert=document.duplicate(source);canvasView.ensureFrames(document.frames.size);canvasView.duplicateFrame(source,insert);canvasView.setFrame(insert);refresh()}
    private fun makeLoop(){if(document.frames.size<2)return;val end=document.frames.lastIndex;document.makeLoop(0,end);canvasView.makeLoop(0,end);canvasView.setFrame(document.currentFrame);refresh()}
    private fun togglePlayback(){playing=!playing;if(playing)playNext()else handler.removeCallbacksAndMessages(null)}
    private fun playNext(){if(!playing||document.frames.isEmpty())return;document.select((document.currentFrame+1)%document.frames.size);canvasView.setFrame(document.currentFrame);refresh();handler.postDelayed({playNext()},1000L/document.fps)}

    private fun showFrameActions(index:Int){val options=arrayOf("Selecionar","Duplicar","Excluir","Limpar desenho");AlertDialog.Builder(this).setTitle("Quadro ${index+1}").setItems(options){_,which->when(which){0->document.select(index);1->{document.select(index);duplicateFrame()};2->if(document.frames.size>1){document.select(index);document.remove(index);canvasView.removeFrame(index)};3->{document.select(index);canvasView.clearCurrentFrame()}};canvasView.setFrame(document.currentFrame);refresh()}.show()}
    private fun showFramesViewer(){val selected=BooleanArray(document.frames.size);val labels=document.frames.indices.map{"Quadro ${it+1}"}.toTypedArray();AlertDialog.Builder(this).setTitle("Visualizador de quadros").setMultiChoiceItems(labels,selected){_,which,checked->selected[which]=checked}.setNegativeButton("Fechar",null).setPositiveButton("Duplicar seleção",null).show()}
    private fun showInfo(title:String,message:String)=AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("OK",null).show()
    private fun refresh(){canvasView.invalidate();timeline.invalidate()}
    private fun LinearLayout.addButton(label:String,action:()->Unit){addView(Button(this@MainActivity).apply{text=label;setTextColor(Color.WHITE);setOnClickListener{action()};minWidth=0;minimumWidth=0;setPadding(dp(2),0,dp(2),0)},LinearLayout.LayoutParams(dp(48),-1))}
    private fun dp(value:Int)= (value*resources.displayMetrics.density).toInt()
}
