package com.animakerpro.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color

/** Core frame model. Designed to evolve into the shared C++/OpenToonz document layer. */
class AnimationDocument(
    val width: Int = 1280,
    val height: Int = 720,
    var fps: Int = 24
) {
    data class Frame(val bitmap: Bitmap, var exposure: Int = 1, var label: String = "")

    val frames = mutableListOf<Frame>()
    var currentFrame: Int = 0
        private set

    init {
        frames += Frame(blankBitmap())
    }

    fun select(index: Int) {
        if (frames.isNotEmpty()) currentFrame = index.coerceIn(0, frames.lastIndex)
    }

    fun addBlank(after: Int = currentFrame): Int {
        val index = (after + 1).coerceIn(0, frames.size)
        frames.add(index, Frame(blankBitmap()))
        currentFrame = index
        return index
    }

    fun duplicate(index: Int = currentFrame): Int {
        val source = frames[index]
        val copy = source.bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val insert = index + 1
        frames.add(insert, Frame(copy, source.exposure, source.label))
        currentFrame = insert
        return insert
    }

    fun duplicateRange(indices: List<Int>, after: Int = currentFrame) {
        if (indices.isEmpty()) return
        val copies = indices.sorted().map { i ->
            val f = frames[i]
            Frame(f.bitmap.copy(Bitmap.Config.ARGB_8888, true), f.exposure, f.label)
        }
        var insert = (after + 1).coerceIn(0, frames.size)
        frames.addAll(insert, copies)
        currentFrame = insert
    }

    fun copyRange(indices: List<Int>): List<Frame> = indices.sorted().map { i ->
        val f = frames[i]
        Frame(f.bitmap.copy(Bitmap.Config.ARGB_8888, true), f.exposure, f.label)
    }

    fun pasteRange(copies: List<Frame>, after: Int = currentFrame) {
        if (copies.isEmpty()) return
        val cloned = copies.map { Frame(it.bitmap.copy(Bitmap.Config.ARGB_8888, true), it.exposure, it.label) }
        val insert = (after + 1).coerceIn(0, frames.size)
        frames.addAll(insert, cloned)
        currentFrame = insert
    }

    fun makeLoop(start: Int, end: Int) {
        if (start < 0 || end >= frames.size || start > end) return
        val copies = copyRange((start..end).toList())
        pasteRange(copies, frames.lastIndex)
    }

    fun remove(index: Int = currentFrame) {
        if (frames.size <= 1) return
        frames[index].bitmap.recycle()
        frames.removeAt(index)
        currentFrame = currentFrame.coerceAtMost(frames.lastIndex)
    }

    private fun blankBitmap(): Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
        Canvas(it).drawColor(Color.WHITE)
    }
}
