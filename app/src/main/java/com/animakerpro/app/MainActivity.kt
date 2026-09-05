package com.animakerpro.app

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
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
        val host = FrameLayout(this).apply { setBackgroundColor(Color.rgb(235,235,235)) }
        canvasView = NativeCanvasView(this); rulerOverlay = RulerOverlayView(this)
        host.addView(canvasView, FrameLayout.LayoutParams(-1,-1)); host.addView(rulerOverlay, FrameLayout.LayoutParams(-1,-1))
        root.addView(host, LinearLayout.LayoutParams(-1,0,1f))
        val scroll = HorizontalScrollView(this).apply { setBackgroundColor(Color.rgb(23,25,30)); isHorizontalScrollBarEnabled = true }
        timeline = TimelineView(this).apply { document=this@MainActivity.document; onFrameSelected={selectFrame(it)}; onFrameLongPressed={showFrameActions(it)} }
        scroll.addView(timeline, ViewGroup.LayoutParams(-1,dp(118))); root.addView(scroll)
        root.addView(animationControls(), LinearLayout.LayoutParams(-1,dp(48))); setContentView(root)
    }

    private fun toolbar(): View = LinearLayout(this).apply {
        orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL; setPadding(dp(4),0,dp(4),0); setBackgroundColor(Color.rgb(24,27,33))
        addButton("☰"){showProjectMenu()}; addButton("🖌"){showBrushes()}; addButton("⌫"){canvasView.eraser=!canvasView.eraser; toast(if(canvasView.eraser) "Borracha ativa" else "Pincel ativo")}
        addButton("▱"){showInfo("Balde","Preenchimento por área está reservado ao motor raster.")}; addButton("⌁"){showInfo("Laço","Seleção e transformação estão no painel de seleção.")}; addButton("◉"){showInfo("Cor","Seletor de cor disponível no painel de cor.")}; addButton("⌖"){showRulerPicker()}; addButton("◌"){canvasView.onionSkin=!canvasView.onionSkin;canvasView.invalidate()}
        addView(Space(this@MainActivity),LinearLayout.LayoutParams(0,-1,1f)); addButton("＋"){addFrame()}; addButton("▣"){duplicateFrame()}; addButton("▤"){showFramesViewer()}
    }

    private fun animationControls(): View = LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL; setPadding(dp(4),0,dp(4),0); setBackgroundColor(Color.rgb(20,22,27)); addButton("|◀"){selectFrame(0)}; addButton("◀"){selectFrame(document.currentFrame-1)}; addButton(if(playing) "■" else "▶"){togglePlayback()}; addButton("▶"){selectFrame(document.currentFrame+1)}; addButton("▶|"){selectFrame(document.frames.lastIndex)}; addButton("+ Frame"){addFrame()}; addButton("Clone"){duplicateFrame()}; addButton("Loop"){makeLoop()} }

    private fun showProjectMenu(){ AlertDialog.Builder(this).setTitle("AnimakerPro").setItems(arrayOf("Projeto","Frame a frame","Rig 2D","Câmera","Áudio e vídeo","Exportar","Configurações")){_,w->when(w){0->showInfo("Projeto","${document.width}×${document.height} • ${document.fps} FPS");1->toast("Frame a frame ativo");2->showRigPanel();3->showInfo("Câmera","Painel de câmera");4->showInfo("Mídia","Painel de mídia");5->showExport();6->showInfo("Configurações","AnimakerPro • C++/OpenGL ES")}}.show() }
    private fun showBrushes(){ val names=BrushLibrary.all.map{it.name}.toTypedArray(); AlertDialog.Builder(this).setTitle("Biblioteca de pincéis").setSingleChoiceItems(names,0){d,w->val b=BrushLibrary.all[w];canvasView.brushSize=b.size;canvasView.brushOpacity=b.opacity;canvasView.spacing=b.spacing.coerceAtLeast(1f);canvasView.pressureSize=b.pressureSize;canvasView.pressureOpacity=b.pressureOpacity;canvasView.stabilization=b.smoothing;d.dismiss();toast(b.name)}.setNegativeButton("Fechar",null).setNeutralButton("Ajustes"){_,_->showBrushSettings()}.show() }
    private fun showBrushSettings(){ val p=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(8),dp(20),0)}; slider(p,"Tamanho",200,canvasView.brushSize.toInt()){canvasView.brushSize=it.coerceAtLeast(1).toFloat()}; slider(p,"Opacidade",100,(canvasView.brushOpacity*100).toInt()){canvasView.brushOpacity=it.coerceAtLeast(1)/100f}; slider(p,"Estabilização",90,(canvasView.stabilization*100).toInt()){canvasView.stabilization=it/100f}; slider(p,"Espaçamento",40,canvasView.spacing.toInt()){canvasView.spacing=it.coerceAtLeast(1).toFloat()}; val c=CheckBox(this).apply{text="Pressão da caneta";setTextColor(Color.WHITE);isChecked=canvasView.pressureSensitivity};c.setOnCheckedChangeListener{_,v->canvasView.pressureSensitivity=v};p.addView(c);AlertDialog.Builder(this).setTitle("Pincel C++").setView(p).setPositiveButton("Concluir",null).show() }
    private fun slider(p:LinearLayout,label:String,max:Int,value:Int,on:(Int)->Unit){val t=TextView(this).apply{setTextColor(Color.WHITE);text="$label: $value"};val s=SeekBar(this).apply{this.max=max;progress=value.coerceIn(0,max)};s.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(b:SeekBar?,v:Int,f:Boolean){t.text="$label: $v";on(v)};override fun onStartTrackingTouch(b:SeekBar?){};override fun onStopTrackingTouch(b:SeekBar?){}});p.addView(t);p.addView(s)}
    private fun showRigPanel(){ val p=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(20),0,dp(20),0)};arrayOf("Adicionar osso","Selecionar osso","IK / FK","Criar keyframe","Interpolação","Controladores").forEach{a->p.addView(Button(this).apply{text=a;setOnClickListener{toast(a)}})};AlertDialog.Builder(this).setTitle("Rig 2D").setView(p).setNegativeButton("Fechar",null).show() }
    private fun showExport(){AlertDialog.Builder(this).setTitle("Exportar animação").setItems(arrayOf("Sequência PNG","GIF","Vídeo","Projeto AnimakerPro")){_,w->toast("${arrayOf("PNG","GIF","Vídeo","Projeto")[w]} selecionado")}.show()}
    private fun showRulerPicker(){val m=RulerOverlayView.Mode.values();AlertDialog.Builder(this).setTitle("Réguas").setSingleChoiceItems(m.map{it.name.replace('_',' ')}.toTypedArray(),m.indexOf(rulerOverlay.mode)){d,w->rulerOverlay.mode=m[w];d.dismiss()}.show()}
    private fun addFrame(){val i=document.addBlank(document.currentFrame);canvasView.insertFrame(i);canvasView.setFrame(i);refresh()}
    private fun duplicateFrame(){val s=document.currentFrame;val i=document.duplicate(s);canvasView.duplicateFrame(s,i);canvasView.setFrame(i);refresh()}
    private fun makeLoop(){if(document.frames.size<2)return;val e=document.frames.lastIndex;document.makeLoop(0,e);canvasView.makeLoop(0,e);canvasView.setFrame(document.currentFrame);refresh()}
    private fun selectFrame(i:Int){document.select(i);canvasView.setFrame(document.currentFrame);refresh()}
    private fun togglePlayback(){playing=!playing;if(playing)playNext()else handler.removeCallbacksAndMessages(null)}
    private fun playNext(){if(!playing||document.frames.isEmpty())return;selectFrame((document.currentFrame+1)%document.frames.size);handler.postDelayed({playNext()},1000L/document.fps.coerceAtLeast(1))}
    private fun showFrameActions(i:Int){AlertDialog.Builder(this).setTitle("Quadro ${i+1}").setItems(arrayOf("Selecionar","Duplicar","Excluir","Limpar desenho")){_,w->when(w){0->selectFrame(i);1->{document.select(i);duplicateFrame()};2->if(document.frames.size>1){document.remove(i);canvasView.removeFrame(i);canvasView.setFrame(document.currentFrame);refresh()};3->{document.select(i);canvasView.clearCurrentFrame();refresh()}}}.show()}
    private fun showFramesViewer(){val selected=BooleanArray(document.frames.size);val labels=document.frames.indices.map{"Quadro ${it+1}"}.toTypedArray();AlertDialog.Builder(this).setTitle("Visualizador de quadros").setMultiChoiceItems(labels,selected){_,i,c->selected[i]=c}.setNegativeButton("Fechar",null).setPositiveButton("Duplicar seleção"){_,_->val ids=selected.indices.filter{selected[it]};if(ids.isNotEmpty()){document.pasteRange(document.copyRange(ids),document.currentFrame);canvasView.ensureFrames(document.frames.size);canvasView.setFrame(document.currentFrame);refresh()}}.show()}
    private fun showInfo(t:String,m:String)=AlertDialog.Builder(this).setTitle(t).setMessage(m).setPositiveButton("OK",null).show()
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
    private fun refresh(){canvasView.invalidate();timeline.invalidate()}
    private fun LinearLayout.addButton(label:String,action:()->Unit){addView(Button(this@MainActivity).apply{text=label;setTextColor(Color.WHITE);minWidth=0;minimumWidth=0;setPadding(dp(2),0,dp(2),0);setOnClickListener{action()}},LinearLayout.LayoutParams(dp(48),-1))}
    private fun dp(v:Int)= (v*resources.displayMetrics.density).toInt()
}
