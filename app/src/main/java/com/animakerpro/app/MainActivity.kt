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

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(17, 19, 24))
        }
        root.addView(toolbar(), LinearLayout.LayoutParams(-1, dp(52)))

        val host = FrameLayout(this).apply { setBackgroundColor(Color.rgb(235, 235, 235)) }
        canvasView = NativeCanvasView(this)
        rulerOverlay = RulerOverlayView(this)
        host.addView(canvasView, FrameLayout.LayoutParams(-1, -1))
        host.addView(rulerOverlay, FrameLayout.LayoutParams(-1, -1))
        root.addView(host, LinearLayout.LayoutParams(-1, 0, 1f))

        val scroll = HorizontalScrollView(this).apply {
            setBackgroundColor(Color.rgb(23, 25, 30))
            isHorizontalScrollBarEnabled = true
        }
        timeline = TimelineView(this).apply {
            document = this@MainActivity.document
            onFrameSelected = { index ->
                val doc = this@MainActivity.document
                doc.select(index)
                canvasView.setFrame(doc.currentFrame)
                refresh()
            }
            onFrameLongPressed = { index -> showFrameActions(index) }
        }
        scroll.addView(timeline, HorizontalScrollView.LayoutParams(-1, dp(118)))
        root.addView(scroll)
        root.addView(animationControls(), LinearLayout.LayoutParams(-1, dp(48)))
        setContentView(root)
    }

    private fun toolbar(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(4), 0, dp(4), 0)
        setBackgroundColor(Color.rgb(24, 27, 33))
        addButton("☰") { showInfo("AnimakerPro", "Canvas nativo C++ • frame-by-frame • rig em desenvolvimento") }
        addButton("🖌") { showBrushSettings() }
        addButton("⌫") { showInfo("Borracha", "Modo borracha nativo será ligado ao raster core.") }
        addButton("▱") { showInfo("Balde", "Fill engine nativo será ligado ao raster core.") }
        addButton("⌁") { showInfo("Laço", "Seleção nativa será ligada ao raster core.") }
        addButton("◉") { showInfo("Seletor de cor", "Color picker nativo será ligado ao framebuffer.") }
        addButton("⌖") { showRulerPicker() }
        addButton("◌") { canvasView.onionSkin = !canvasView.onionSkin; canvasView.invalidate() }
        addView(Space(this@MainActivity), LinearLayout.LayoutParams(0, -1, 1f))
        addButton("＋") { addFrame() }
        addButton("▣") { duplicateFrame() }
        addButton("▤") { showFramesViewer() }
    }

    private fun animationControls(): View = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(4), 0, dp(4), 0)
        setBackgroundColor(Color.rgb(20, 22, 27))
        addButton("|◀") { selectFrame(0) }
        addButton("◀") { selectFrame(document.currentFrame - 1) }
        addButton(if (playing) "■" else "▶") { togglePlayback() }
        addButton("▶") { selectFrame(document.currentFrame + 1) }
        addButton("▶|") { selectFrame(document.frames.lastIndex) }
        addButton("+ Frame") { addFrame() }
        addButton("Clone") { duplicateFrame() }
        addButton("Loop") { makeLoop() }
    }

    private fun selectFrame(index: Int) {
        document.select(index)
        canvasView.setFrame(document.currentFrame)
        refresh()
    }

    private fun showBrushSettings() {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }
        val sizeLabel = TextView(this).apply {
            text = "Tamanho: ${canvasView.brushSize.toInt()} px"
            setTextColor(Color.WHITE)
        }
        val size = SeekBar(this).apply {
            max = 199
            progress = canvasView.brushSize.toInt().coerceIn(1, 200) - 1
        }
        size.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) {
                canvasView.brushSize = (value + 1).toFloat()
                sizeLabel.text = "Tamanho: ${value + 1} px"
            }
            override fun onStartTrackingTouch(bar: SeekBar?) = Unit
            override fun onStopTrackingTouch(bar: SeekBar?) = Unit
        })

        val opacityLabel = TextView(this).apply {
            text = "Opacidade: ${(canvasView.brushOpacity * 100).toInt()}%"
            setTextColor(Color.WHITE)
        }
        val opacity = SeekBar(this).apply {
            max = 100
            progress = (canvasView.brushOpacity * 100).toInt().coerceIn(1, 100)
        }
        opacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) {
                val v = value.coerceAtLeast(1)
                canvasView.brushOpacity = v / 100f
                opacityLabel.text = "Opacidade: $v%"
            }
            override fun onStartTrackingTouch(bar: SeekBar?) = Unit
            override fun onStopTrackingTouch(bar: SeekBar?) = Unit
        })

        val stabilizationLabel = TextView(this).apply {
            text = "Estabilização: ${(canvasView.stabilization * 100).toInt()}%"
            setTextColor(Color.WHITE)
        }
        val stabilization = SeekBar(this).apply {
            max = 90
            progress = (canvasView.stabilization * 100).toInt().coerceIn(0, 90)
        }
        stabilization.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) {
                canvasView.stabilization = value / 100f
                stabilizationLabel.text = "Estabilização: $value%"
            }
            override fun onStartTrackingTouch(bar: SeekBar?) = Unit
            override fun onStopTrackingTouch(bar: SeekBar?) = Unit
        })

        val pressure = CheckBox(this).apply {
            text = "Pressão / espessura dinâmica"
            isChecked = canvasView.pressureSensitivity
            setTextColor(Color.WHITE)
        }
        pressure.setOnCheckedChangeListener { _, enabled -> canvasView.pressureSensitivity = enabled }

        panel.addView(sizeLabel)
        panel.addView(size)
        panel.addView(opacityLabel)
        panel.addView(opacity)
        panel.addView(stabilizationLabel)
        panel.addView(stabilization)
        panel.addView(pressure)
        AlertDialog.Builder(this).setTitle("Pincel C++").setView(panel).setPositiveButton("OK", null).show()
    }

    private fun showRulerPicker() {
        val modes = RulerOverlayView.Mode.values()
        val labels = modes.map { it.name.replace('_', ' ') }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Réguas")
            .setSingleChoiceItems(labels, modes.indexOf(rulerOverlay.mode)) { dialog, which ->
                rulerOverlay.mode = modes[which]
                dialog.dismiss()
            }
            .show()
    }

    private fun addFrame() {
        val index = document.addBlank(document.currentFrame)
        canvasView.insertFrame(index)
        canvasView.setFrame(index)
        refresh()
    }

    private fun duplicateFrame() {
        val source = document.currentFrame
        val index = document.duplicate(source)
        canvasView.duplicateFrame(source, index)
        canvasView.setFrame(index)
        refresh()
    }

    private fun makeLoop() {
        if (document.frames.size < 2) return
        val end = document.frames.lastIndex
        document.makeLoop(0, end)
        canvasView.makeLoop(0, end)
        canvasView.setFrame(document.currentFrame)
        refresh()
    }

    private fun togglePlayback() {
        playing = !playing
        if (playing) playNext() else handler.removeCallbacksAndMessages(null)
    }

    private fun playNext() {
        if (!playing || document.frames.isEmpty()) return
        selectFrame((document.currentFrame + 1) % document.frames.size)
        handler.postDelayed({ playNext() }, 1000L / document.fps.coerceAtLeast(1))
    }

    private fun showFrameActions(index: Int) {
        val actions = arrayOf("Selecionar", "Duplicar", "Excluir", "Limpar desenho")
        AlertDialog.Builder(this).setTitle("Quadro ${index + 1}").setItems(actions) { _, choice ->
            when (choice) {
                0 -> selectFrame(index)
                1 -> { document.select(index); duplicateFrame() }
                2 -> if (document.frames.size > 1) {
                    document.remove(index)
                    canvasView.removeFrame(index)
                    canvasView.setFrame(document.currentFrame)
                    refresh()
                }
                3 -> { document.select(index); canvasView.clearCurrentFrame(); refresh() }
            }
        }.show()
    }

    private fun showFramesViewer() {
        val selected = BooleanArray(document.frames.size)
        val labels = document.frames.indices.map { "Quadro ${it + 1}" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Visualizador de quadros")
            .setMultiChoiceItems(labels, selected) { _, index, checked -> selected[index] = checked }
            .setNegativeButton("Fechar", null)
            .setPositiveButton("Duplicar seleção") { _, _ ->
                val indices = selected.mapIndexedNotNull { i, checked -> if (checked) i else null }
                if (indices.isNotEmpty()) {
                    val copies = document.copyRange(indices)
                    document.pasteRange(copies, document.currentFrame)
                    canvasView.ensureFrames(document.frames.size)
                    canvasView.setFrame(document.currentFrame)
                    refresh()
                }
            }
            .show()
    }

    private fun showInfo(title: String, message: String) =
        AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("OK", null).show()

    private fun refresh() {
        canvasView.invalidate()
        timeline.invalidate()
    }

    private fun LinearLayout.addButton(label: String, action: () -> Unit) {
        addView(Button(this@MainActivity).apply {
            text = label
            setTextColor(Color.WHITE)
            setOnClickListener { action() }
            minWidth = 0
            minimumWidth = 0
            setPadding(dp(2), 0, dp(2), 0)
        }, LinearLayout.LayoutParams(dp(48), -1))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
