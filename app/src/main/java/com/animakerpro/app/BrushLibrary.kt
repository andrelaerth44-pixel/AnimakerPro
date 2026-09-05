package com.animakerpro.app

/**
 * Built-in brush catalog. Parameters are intentionally data-driven so the native
 * renderer can evolve without changing the UI. The presets are original profiles
 * inspired by traditional animation, ink and paint workflows.
 */
data class BrushPreset(
    val id: String,
    val name: String,
    val family: String,
    val size: Float,
    val opacity: Float,
    val spacing: Float,
    val pressureSize: Float,
    val pressureOpacity: Float,
    val taperStart: Float,
    val taperEnd: Float,
    val smoothing: Float,
    val texture: Float = 0f,
    val wetness: Float = 0f
)

object BrushLibrary {
    val all: List<BrushPreset> = listOf(
        BrushPreset("pencil_hard", "Lápis duro", "Lápis", 6f, .92f, .10f, .72f, .12f, .04f, .08f, .38f),
        BrushPreset("pencil_soft", "Lápis macio", "Lápis", 10f, .72f, .12f, .82f, .20f, .08f, .10f, .45f, .20f),
        BrushPreset("blue_pencil", "Lápis azul", "Lápis", 7f, .78f, .10f, .75f, .10f, .04f, .06f, .42f),
        BrushPreset("red_pencil", "Lápis vermelho", "Lápis", 7f, .78f, .10f, .75f, .10f, .04f, .06f, .42f),
        BrushPreset("ink_round", "Nanquin redondo", "Tinta", 8f, 1f, .06f, .58f, .10f, .05f, .18f, .52f),
        BrushPreset("ink_g_pen", "G-pen", "Tinta", 13f, .98f, .05f, .86f, .12f, .12f, .25f, .58f),
        BrushPreset("ink_mapping", "Mapping", "Tinta", 4f, 1f, .07f, .66f, .08f, .02f, .15f, .62f),
        BrushPreset("ink_flat", "Tinta plana", "Tinta", 18f, 1f, .04f, .35f, .05f, .02f, .08f, .50f),
        BrushPreset("marker", "Marcador", "Marcador", 28f, .68f, .08f, .28f, .04f, .03f, .04f, .36f, .12f),
        BrushPreset("marker_felt", "Felt", "Marcador", 22f, .74f, .10f, .35f, .06f, .04f, .06f, .42f, .28f),
        BrushPreset("airbrush", "Aerógrafo", "Pintura", 70f, .16f, .05f, .38f, .30f, .02f, .02f, .30f, .10f),
        BrushPreset("soft_round", "Redondo suave", "Pintura", 42f, .72f, .08f, .45f, .10f, .02f, .03f, .32f),
        BrushPreset("opaque_round", "Redondo opaco", "Pintura", 30f, 1f, .06f, .58f, .06f, .03f, .05f, .42f),
        BrushPreset("watercolor", "Aquarela", "Pintura", 44f, .34f, .12f, .50f, .28f, .08f, .08f, .40f, .30f, .55f),
        BrushPreset("watercolor_wet", "Aquarela molhada", "Pintura", 58f, .25f, .10f, .40f, .38f, .10f, .10f, .32f, .25f, .82f),
        BrushPreset("dry_brush", "Pincel seco", "Pintura", 32f, .72f, .18f, .56f, .10f, .08f, .12f, .36f, .62f),
        BrushPreset("chalk", "Giz", "Textura", 30f, .64f, .22f, .42f, .14f, .06f, .10f, .34f, .78f),
        BrushPreset("charcoal", "Carvão", "Textura", 34f, .58f, .20f, .55f, .18f, .10f, .14f, .30f, .88f),
        BrushPreset("crayon", "Giz de cera", "Textura", 26f, .70f, .16f, .40f, .12f, .05f, .08f, .36f, .72f),
        BrushPreset("oil", "Óleo", "Pintura", 38f, .82f, .09f, .52f, .14f, .06f, .10f, .30f, .42f, .42f),
        BrushPreset("sumi", "Sumi-e", "Tinta", 24f, .88f, .07f, .76f, .20f, .12f, .30f, .54f, .20f, .25f),
        BrushPreset("calligraphy", "Caligrafia", "Tinta", 14f, 1f, .05f, .72f, .08f, .10f, .35f, .60f),
        BrushPreset("pixel", "Pixel", "Especial", 4f, 1f, 1f, 0f, 0f, 0f, 0f, .05f),
        BrushPreset("clean_line", "Clean line", "Animação", 5f, 1f, .04f, .70f, .06f, .12f, .28f, .72f)
    )

    val families: List<String> get() = listOf("Todos") + all.map { it.family }.distinct()
    fun byFamily(family: String): List<BrushPreset> = if (family == "Todos") all else all.filter { it.family == family }
    fun find(id: String): BrushPreset = all.firstOrNull { it.id == id } ?: all.first()
}
