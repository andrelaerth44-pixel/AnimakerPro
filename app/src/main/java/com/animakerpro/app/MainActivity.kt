package com.animakerpro.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private lateinit var document: AnimationDocument
    private lateinit var canvasView: DrawingCanvasView
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

        root.addView(toolbar(), LinearLayout.LayoutParams(-1, dp(56)))

        canvasView = DrawingCanvasView(this).apply { document = this@MainActivity.document }
        root.addView(canvasView, LinearLayout.LayoutParams(-1, 0, 1f))

        val timelineScroll = HorizontalScrollView(this).apply {
            setBackgroundColor(Color.rgb(23, 25, 30))
            isHorizontalScrollBarEnabled = true
        }
        timeline = TimelineView(this).apply {
            document = this@MainActivity.document
            onFrameSelected = { index ->
                document.select(index)
                canvasView.invalidate()
                invalidate()
            }
            onFrameLongPressed = { index -> showFrameActions(index) }
        }
        timelineScroll.addView(timeline, HorizontalScrollView.LayoutParams(-1, dp(132)))
        root.addView(timelineScroll)

        root.addView(animationControls(), LinearLayout.LayoutParams(-1, dp(54)))
        setContentView(root)
    }

    private fun toolbar(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(8), 0, dp(8), 0)
        setBackgroundColor(Color.rgb(24, 27, 33))

        addButton("☰") { showInfo("AnimakerPro", "Android-first professional 2D animation studio.\n\nDrawing + frame animation + professional timeline + rulers.") }
        addButton("✎") { showRulerPicker() }
        addButton("◌") {
            canvasView.onionSkin = !canvasView.onionSkin
            canvasView.invalidate()
        }
        addButton("↶") { /* undo history will move into the document core */ }
        addButton("↷") { /* redo history will move into the document core */ }

        val spacer = Space(this)
        addView(spacer, LinearLayout.LayoutParams(0, -1, 1f))
        addButton("＋") { addFrame() }
        addButton("▣") { duplicateFrame() }
        addButton("▤") { showFramesViewer() }
    }

    private fun animationControls(): View = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(6), 0, dp(6), 0)
        setBackgroundColor(Color.rgb(20, 22, 27))
        addButton("|◀") { document.select(0); refresh() }
        addButton("◀") { document.select(document.currentFrame - 1); refresh() }
        addButton(if (playing) "■" else "▶") { togglePlayback() }
        addButton("▶") { document.select(document.currentFrame + 1); refresh() }
        addButton("▶|") { document.select(document.frames.lastIndex); refresh() }
        addButton("+ Frame") { addFrame() }
        addButton("Clone") { duplicateFrame() }
        addButton("Loop") { makeLoop() }
    }

    private fun addFrame() {
        document.addBlank()
        refresh()
    }

    private fun duplicateFrame() {
        document.duplicate()
        refresh()
    }

    private fun makeLoop() {
        if (document.frames.size < 2) return
        document.makeLoop(0, document.frames.lastIndex)
        refresh()
    }

    private fun togglePlayback() {
        playing = !playing
        if (playing) playNext() else handler.removeCallbacksAndMessages(null)
    }

    private fun playNext() {
        if (!playing) return
        document.select((document.currentFrame + 1) % document.frames.size)
        refresh()
        handler.postDelayed({ playNext() }, 1000L / document.fps)
    }

    private fun refresh() {
        canvasView.invalidate()
        timeline.invalidate()
    }

    private fun showRulerPicker() {
        val modes = DrawingCanvasView.RulerMode.values()
        val labels = modes.map { it.name.replace('_', ' ') }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Régua")
            .setSingleChoiceItems(labels, modes.indexOf(canvasView.rulerMode)) { dialog, which ->
                canvasView.rulerMode = modes[which]
                dialog.dismiss()
            }
            .show()
    }

    private fun showFrameActions(index: Int) {
        val options = arrayOf("Selecionar", "Duplicar", "Excluir")
        AlertDialog.Builder(this)
            .setTitle("Quadro ${index + 1}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> document.select(index)
                    1 -> { document.select(index); document.duplicate() }
                    2 -> { document.select(index); document.remove() }
                }
                refresh()
            }.show()
    }

    private fun showFramesViewer() {
        val selected = BooleanArray(document.frames.size)
        val labels = document.frames.indices.map { "Quadro ${it + 1}" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Visualizador de quadros")
            .setMultiChoiceItems(labels, selected) { _, which, checked -> selected[which] = checked }
            .setNegativeButton("Fechar", null)
            .setPositiveButton("Copiar + colar depois") { _, _ ->
                val indexes = selected.indices.filter { selected[it] }
                val copies = document.copyRange(indexes)
                document.pasteRange(copies)
                refresh()
            }
            .show()
    }

    private fun showInfo(title: String, message: String) {
        AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("OK", null).show()
    }

    private fun LinearLayout.addButton(label: String, action: () -> Unit) {
        addView(Button(this@MainActivity).apply {
            text = label
            setTextColor(Color.WHITE)
            setOnClickListener { action() }
            minWidth = 0
            minimumWidth = 0
        }, LinearLayout.LayoutParams(dp(54), -1))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
