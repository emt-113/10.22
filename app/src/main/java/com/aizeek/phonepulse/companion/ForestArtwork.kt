package com.aizeek.phonepulse.companion

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import java.util.Calendar

/** Shared static illustration for the home, travel album and exported cards. */
object ForestArtwork {
    /** Pearl palette: warm white base with mint / glacier / lavender accents. Dusk stays light so dark text always reads. */
    private object Pearl {
        const val SKY_DAY_TOP = "#FCFAF3"
        const val SKY_DAY_BOTTOM = "#E9F1F8"
        const val SKY_DUSK_TOP = "#EBE7F8"
        const val SKY_DUSK_BOTTOM = "#D5DCF2"
        const val SUN = "#F9E9C8"
        const val SUN_RING = "#FBF0D6"
        const val MOON = "#F6F1E2"
        const val MOON_SHADOW = "#E6DEC8"
        const val STAR = "#B9A6D9"

        const val HILL_FAR_DAY = "#CFE3D8"
        const val HILL_FAR_DUSK = "#C8D0EB"
        const val HILL_NEAR_DAY = "#DFECE4"
        const val HILL_NEAR_DUSK = "#D2DAEF"
        const val MEADOW_DAY = "#F5EEE0"
        const val MEADOW_DUSK = "#E7E9F5"
        const val MEADOW_SHEEN_DAY = "#FAF5EA"
        const val MEADOW_SHEEN_DUSK = "#F1F2FB"
        const val BUSH_DAY = "#CFE3D8"
        const val BUSH_DUSK = "#C8D0EB"

        const val TREE_DAY = "#A5D4BE"
        const val TREE_DAY_LIGHT = "#CDEADD"
        const val TREE_BIG_DAY = "#8BC5AA"
        const val TREE_DUSK = "#B8C5E7"
        const val TREE_DUSK_LIGHT = "#D7DDF3"
        const val TREE_BIG_DUSK = "#9BAAD7"
        const val TRUNK = "#CDB89B"

        const val WOOD = "#E3D2B4"
        const val WOOD_DARK = "#CDB89B"
        const val WOOD_DEEP = "#D8C4A4"
        const val PLANK = "#F3E9D4"
        const val PLATFORM = "#E9DAC0"
        const val LOG = "#D3BE9C"
        const val STUMP = "#E3D2B4"
        const val PEBBLE = "#D8D2E8"
        const val TENT = "#F2D5D8"
        const val TENT_INNER = "#F9E9E3"
        const val POLE = "#CDB89B"

        const val CABIN_WALL = "#EDE0C8"
        const val CABIN_LINE = "#DCCBA9"
        const val ROOF_TERRA = "#E9BFA8"
        const val ROOF_TERRA_SIDE = "#F5D8C9"
        const val ROOF_MINT = "#A5D4BE"
        const val ROOF_MINT_SIDE = "#CDEADD"
        const val ROOF_SLATE = "#B9D4EA"
        const val ROOF_SLATE_SIDE = "#DAE9F6"
        const val ROOF_EDGE = "#C9B39A"
        const val DOOR = "#D2B393"
        const val DOOR_DARK = "#C2A184"
        const val CHIMNEY = "#DCCBB0"
        const val GLASS = "#E7F1EA"
        const val GLASS_GLOW = "#F9E9C8"

        const val MAILBOX = "#F2D5D8"
        const val MAILBOX_DARK = "#E5BFC4"
        const val LETTER = "#F9E9C8"
        const val HAMMOCK = "#F1E7D7"
        const val ROPE = "#CDB89B"

        const val PETAL_PINK = "#F2C9D2"
        const val PETAL_CREAM = "#F9E9C8"
        const val PETAL_LAV = "#D9C9EE"
        const val CORE = "#E8C78F"
        const val STEM = "#A5D4BE"

        const val WALL_TOP = "#F5ECDD"
        const val WALL_BOTTOM = "#FBF6EC"
        const val WALL_LINE = "#EAE0CC"
        const val FLOOR = "#E9DAC0"
        const val FLOOR_LINE = "#DBCBA9"
        const val RUG = "#E7DCF2"
        const val BED = "#F9E9E3"
        const val PILLOW = "#FCF8F1"
        const val FRAME = "#D2B393"

        const val WATER = "#C4DCEF"
        const val WATER_DUSK = "#BCD0EC"
        const val WATER_LINE = "#EAF4FB"
        const val BRIDGE = "#E3D2B4"
        const val BRIDGE_RAIL = "#C9B39A"
        const val REED = "#A5D4BE"
        const val MUSH_STEM = "#F3E9D4"
        const val MUSH_PINK = "#F2C9D2"
        const val MUSH_LAV = "#D9C9EE"
        const val MUSH_DOT = "#FCF8F1"

        const val F_PIP = "#D7E5DA"
        const val F_OTTO = "#E9DAC1"
        const val F_HAZEL = "#F2D5D8"
        const val F_HAZEL_TAIL = "#EBC9D4"
        const val F_PIP_CREST = "#E0A895"
        const val F_MUZZLE = "#FCF8F1"
        const val F_EYE = "#5A5470"
        const val F_NOSE = "#8A7B8F"
        const val F_SHADOW = "#DCD6CE"
    }

    private class Brush(val canvas: Canvas) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
        fun color(value: String) { paint.color = Color.parseColor(value); paint.shader = null; paint.style = Paint.Style.FILL }
        fun oval(x: Float, y: Float, w: Float, h: Float, color: String) { color(color); canvas.drawOval(x, y, x + w, y + h, paint) }
        fun rect(x: Float, y: Float, w: Float, h: Float, color: String, radius: Float = 0f) {
            color(color); canvas.drawRoundRect(x, y, x + w, y + h, radius, radius, paint)
        }
        fun path(color: String, block: Path.() -> Unit) { color(color); canvas.drawPath(Path().apply(block), paint) }
        fun line(x: Float, y: Float, endX: Float, endY: Float, color: String, width: Float = 2f) {
            color(color); paint.strokeWidth = width; canvas.drawLine(x, y, endX, endY, paint)
        }
        fun gradient(w: Float, h: Float, top: String, bottom: String) {
            paint.shader = LinearGradient(0f, 0f, 0f, h, Color.parseColor(top), Color.parseColor(bottom), Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, w, h, paint); paint.shader = null
        }
        fun tree(x: Float, y: Float, scale: Float, color: String, light: String) {
            canvas.save(); canvas.translate(x, y); canvas.scale(scale, scale)
            rect(-5f, -50f, 10f, 105f, Pearl.TRUNK, 4f)
            path(color) { moveTo(0f, -145f); quadTo(-15f, -82f, -46f, -53f); lineTo(-28f, -54f)
                quadTo(-37f, -28f, -60f, 0f); quadTo(0f, 22f, 60f, 0f); lineTo(28f, -54f)
                lineTo(45f, -52f); quadTo(13f, -89f, 0f, -145f); close() }
            path(light) { moveTo(0f, -132f); quadTo(-13f, -92f, -30f, -56f); quadTo(-20f, -30f, -26f, -10f)
                lineTo(-14f, -12f); quadTo(-17f, -40f, -7f, -70f); quadTo(-2f, -96f, 3f, -128f); close() }
            canvas.restore()
        }
        fun flower(x: Float, y: Float, color: String) {
            line(x, y, x - 2, y - 12, Pearl.STEM)
            oval(x - 7, y - 17, 10f, 7f, color); oval(x - 3, y - 20, 7f, 10f, color)
            oval(x - 3, y - 16, 4f, 4f, Pearl.CORE)
        }
    }

    fun home(canvas: Canvas, width: Float, height: Float, state: CompanionState, interior: Boolean = false,
             night: Boolean = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) !in 6..18) {
        canvas.save(); canvas.scale(width / 400f, height / 420f); canvas.clipRect(0f, 0f, 400f, 420f)
        val b = Brush(canvas)
        if (interior && state.home.stage >= HomeStage.CABIN) {
            interior(b, state)
        } else {
            b.gradient(400f, 420f, if (night) Pearl.SKY_DUSK_TOP else Pearl.SKY_DAY_TOP,
                if (night) Pearl.SKY_DUSK_BOTTOM else Pearl.SKY_DAY_BOTTOM)
            b.oval(304f, 24f, 44f, 44f, if (night) Pearl.MOON_SHADOW else Pearl.SUN_RING)
            b.oval(310f, 30f, 32f, 32f, if (night) Pearl.MOON else Pearl.SUN)
            if (night) {
                b.oval(318f, 38f, 6f, 6f, Pearl.MOON_SHADOW)
                b.oval(303f, 45f, 4f, 4f, Pearl.MOON_SHADOW)
                b.oval(322f, 52f, 3f, 3f, Pearl.MOON_SHADOW)
                listOf(30f to 40f, 135f to 66f, 234f to 20f, 275f to 77f, 367f to 103f).forEach { (x, y) -> b.oval(x, y, 2.5f, 2.5f, Pearl.STAR) }
            }
            b.path(if (night) Pearl.HILL_FAR_DUSK else Pearl.HILL_FAR_DAY) { moveTo(0f, 130f); quadTo(65f, 83f, 130f, 151f); quadTo(245f, 75f, 400f, 129f); lineTo(400f, 420f); lineTo(0f, 420f); close() }
            listOf(18f to 159f, 65f to 185f, 330f to 183f, 391f to 140f).forEach { (x, y) ->
                b.tree(x, y, 0.88f, if (night) Pearl.TREE_DUSK else Pearl.TREE_DAY, if (night) Pearl.TREE_DUSK_LIGHT else Pearl.TREE_DAY_LIGHT)
            }
            b.path(if (night) Pearl.HILL_NEAR_DUSK else Pearl.HILL_NEAR_DAY) { moveTo(0f, 268f); quadTo(165f, 192f, 400f, 266f); lineTo(400f, 420f); lineTo(0f, 420f); close() }
            b.oval(67f, 248f, 300f, 100f, if (night) Pearl.MEADOW_DUSK else Pearl.MEADOW_DAY)
            b.oval(120f, 262f, 190f, 58f, if (night) Pearl.MEADOW_SHEEN_DUSK else Pearl.MEADOW_SHEEN_DAY)
            when (state.home.stage) {
                HomeStage.CLEARING -> {
                    b.rect(168f, 260f, 95f, 21f, Pearl.PLATFORM, 10f)
                    b.line(181f, 270f, 249f, 270f, Pearl.PLANK, 3f)
                    b.rect(247f, 230f, 24f, 30f, Pearl.STUMP, 7f)
                    b.rect(217f, 221f, 21f, 28f, Pearl.LOG, 5f)
                    b.oval(210f, 209f, 36f, 14f, Pearl.STUMP)
                    b.oval(258f, 246f, 10f, 7f, Pearl.PEBBLE)
                    b.oval(152f, 252f, 8f, 6f, Pearl.PEBBLE)
                }
                HomeStage.CAMP -> {
                    b.path(Pearl.TENT) { moveTo(135f, 260f); lineTo(213f, 155f); lineTo(298f, 263f); close() }
                    b.path(Pearl.TENT_INNER) { moveTo(213f, 155f); lineTo(247f, 263f); lineTo(298f, 263f); close() }
                    b.path(Pearl.POLE) { moveTo(180f, 260f); lineTo(213f, 190f); lineTo(242f, 260f); close() }
                    b.line(213f, 149f, 213f, 268f, Pearl.POLE, 4f)
                    b.oval(196f, 250f, 12f, 8f, Pearl.PEBBLE)
                }
                HomeStage.FOUNDATION -> {
                    b.rect(140f, 257f, 170f, 18f, Pearl.PLATFORM, 4f)
                    for (i in 0..6) b.line(145f, 262f + i * 2, 300f, 262f + i * 2, Pearl.PLANK, 1f)
                    listOf(151f, 235f, 298f).forEach { b.line(it, 257f, it, 159f, Pearl.WOOD, 8f) }
                    b.line(151f, 159f, 298f, 159f, Pearl.WOOD, 8f)
                    b.line(235f, 159f, 252f, 123f, Pearl.WOOD_DARK, 6f); b.line(252f, 123f, 298f, 159f, Pearl.WOOD_DARK, 6f)
                }
                HomeStage.CABIN, HomeStage.COZY -> cabin(b, state, night)
            }
            if (state.home.stage >= HomeStage.CAMP) {
                b.line(91f, 278f, 91f, 234f, Pearl.WOOD_DARK, 5f)
                b.rect(74f, 210f, 37f, 28f, Pearl.MAILBOX, 9f); b.line(80f, 222f, 104f, 222f, Pearl.MAILBOX_DARK, 2f)
                if (state.letters.any { !it.read }) { b.rect(109f, 203f, 3f, 18f, Pearl.WOOD_DARK); b.rect(111f, 203f, 12f, 8f, Pearl.LETTER, 2f) }
            }
            if (state.home.stage == HomeStage.COZY) {
                b.path(Pearl.HAMMOCK) { moveTo(226f, 267f); cubicTo(212f, 295f, 302f, 310f, 270f, 420f); lineTo(229f, 420f)
                    cubicTo(260f, 326f, 163f, 306f, 207f, 267f); close() }
                b.rect(315f, 264f, 62f, 9f, Pearl.WOOD_DEEP, 3f)
                b.line(323f, 271f, 320f, 286f, Pearl.ROPE, 4f); b.line(366f, 271f, 369f, 286f, Pearl.ROPE, 4f)
            }
            drawFurniture(b, state, false)
            RabbitArtwork.draw(canvas, state, 98f, 210f, 155f)
            val visitor = state.friends.firstOrNull { it.meetings.lastOrNull()?.visiting == true }
            if (visitor != null && state.home.stage >= HomeStage.CABIN) friend(canvas, visitor.id, 286f, 288f, 0.7f)
            b.tree(-5f, 283f, 1.6f, if (night) Pearl.TREE_BIG_DUSK else Pearl.TREE_BIG_DAY, if (night) Pearl.TREE_DUSK_LIGHT else Pearl.TREE_DAY_LIGHT)
            b.tree(409f, 310f, 1.5f, if (night) Pearl.TREE_BIG_DUSK else Pearl.TREE_BIG_DAY, if (night) Pearl.TREE_DUSK_LIGHT else Pearl.TREE_DAY_LIGHT)
            for (i in 0..8) b.flower(28f + i * 43, 387f + (i % 3) * 8, if (i % 2 == 0) Pearl.PETAL_PINK else Pearl.PETAL_CREAM)
            b.oval(20f, 342f, 12f, 7f, if (night) Pearl.BUSH_DUSK else Pearl.BUSH_DAY)
            b.oval(367f, 355f, 14f, 8f, if (night) Pearl.BUSH_DUSK else Pearl.BUSH_DAY)
        }
        canvas.restore()
    }

    private fun cabin(b: Brush, state: CompanionState, night: Boolean) {
        b.rect(139f, 156f, 178f, 110f, Pearl.CABIN_WALL, 3f)
        for (i in 0..7) b.line(139f, 168f + i * 13, 317f, 168f + i * 13, Pearl.CABIN_LINE, 1.3f)
        val roofColor = when (state.home.roof) { "moss" -> Pearl.ROOF_MINT; "slate" -> Pearl.ROOF_SLATE; else -> Pearl.ROOF_TERRA }
        val roofSide = when (state.home.roof) { "moss" -> Pearl.ROOF_MINT_SIDE; "slate" -> Pearl.ROOF_SLATE_SIDE; else -> Pearl.ROOF_TERRA_SIDE }
        b.path(roofColor) { moveTo(317f, 156f); lineTo(341f, 141f); lineTo(341f, 249f); lineTo(317f, 266f); close() }
        b.path(roofColor) { moveTo(123f, 165f); lineTo(212f, 97f); lineTo(331f, 161f); close() }
        b.path(roofSide) { moveTo(212f, 97f); lineTo(239f, 83f); lineTo(348f, 143f); lineTo(331f, 161f); close() }
        b.line(124f, 165f, 212f, 97f, Pearl.ROOF_EDGE, 5f); b.line(212f, 97f, 331f, 161f, Pearl.ROOF_EDGE, 5f)
        b.rect(258f, 101f, 16f, 27f, Pearl.CHIMNEY, 2f)
        b.rect(197f, 198f, 39f, 68f, Pearl.DOOR, 17f); b.rect(203f, 205f, 27f, 59f, Pearl.DOOR_DARK, 13f)
        b.oval(222f, 234f, 4f, 4f, Pearl.GLASS_GLOW)
        listOf(153f, 260f).forEach { x ->
            b.rect(x, 186f, 36f, 38f, Pearl.WOOD_DARK, 5f); b.rect(x + 4f, 190f, 28f, 30f, if (night) Pearl.GLASS_GLOW else Pearl.GLASS, 3f)
            b.line(x + 18f, 190f, x + 18f, 220f, Pearl.WOOD_DEEP, 3f); b.line(x + 4f, 205f, x + 32f, 205f, Pearl.WOOD_DEEP, 3f)
            b.rect(x - 3f, 223f, 42f, 6f, Pearl.CABIN_LINE, 2f)
        }
        b.rect(190f, 265f, 54f, 9f, Pearl.WOOD_DEEP, 3f)
    }

    private fun interior(b: Brush, state: CompanionState) {
        b.gradient(400f, 420f, Pearl.WALL_TOP, Pearl.WALL_BOTTOM)
        for (i in 0..12) b.line(0f, i * 22f, 400f, i * 22f, Pearl.WALL_LINE, 1.5f)
        b.rect(0f, 267f, 400f, 153f, Pearl.FLOOR)
        for (i in 0..6) b.line(0f, 273f + i * 24f, 400f, 273f + i * 24f, Pearl.FLOOR_LINE, 2f)
        b.rect(246f, 64f, 94f, 107f, Pearl.WOOD_DARK, 18f)
        b.rect(253f, 71f, 80f, 93f, "#E3EDF6", 13f)
        b.oval(283f, 86f, 21f, 21f, Pearl.SUN); b.tree(290f, 159f, 0.4f, Pearl.TREE_DAY, Pearl.TREE_DAY_LIGHT)
        b.line(293f, 72f, 293f, 164f, Pearl.WOOD_DARK, 4f); b.line(253f, 116f, 333f, 116f, Pearl.WOOD_DARK, 4f)
        b.rect(239f, 170f, 108f, 10f, Pearl.WOOD_DARK, 3f)
        b.rect(39f, 260f, 109f, 39f, Pearl.FRAME, 7f); b.rect(37f, 254f, 117f, 16f, Pearl.BED, 8f)
        b.rect(40f, 244f, 33f, 12f, Pearl.PILLOW, 5f); b.line(46f, 290f, 46f, 316f, Pearl.DOOR_DARK, 5f)
        b.rect(258f, 243f, 88f, 13f, Pearl.WOOD_DEEP, 5f)
        b.line(270f, 255f, 267f, 303f, Pearl.DOOR_DARK, 7f); b.line(337f, 255f, 341f, 303f, Pearl.DOOR_DARK, 7f)
        b.oval(72f, 324f, 230f, 60f, Pearl.RUG)
        if (state.showcase.isNotEmpty()) {
            b.rect(35f, 167f, 150f, 6f, Pearl.WOOD_DARK, 2f)
            state.showcase.forEachIndexed { index, id ->
                b.canvas.save(); b.canvas.translate(35f + index * 24, 141f)
                CompanionArtwork.item(b.canvas, id, 25f); b.canvas.restore()
            }
        }
        state.home.wallPicture?.let { id -> state.letters.firstOrNull { it.id == id }?.let { letter ->
            b.rect(115f, 64f, 94f, 66f, Pearl.FRAME, 4f)
            b.canvas.save(); b.canvas.translate(121f, 70f); scenery(b.canvas, 82f, 54f, letter.placeId, letter.artwork); b.canvas.restore()
        } }
        drawFurniture(b, state, true)
        RabbitArtwork.draw(b.canvas, state, 150f, 204f, 176f)
    }

    private fun drawFurniture(b: Brush, state: CompanionState, inside: Boolean) {
        state.home.furniture.forEach { (slot, id) ->
            val position = when (slot) {
                "ENTRY" -> if (!inside) 244f to 268f else null
                "GARDEN" -> if (!inside) 294f to 300f else null
                "TABLE" -> if (inside) 284f to 210f else null
                "WINDOW" -> if (inside) 266f to 146f else null
                "WALL" -> if (inside) 50f to 110f else null
                else -> null
            } ?: return@forEach
            b.canvas.save(); b.canvas.translate(position.first, position.second)
            CompanionArtwork.item(b.canvas, id, 46f); b.canvas.restore()
        }
    }

    fun scenery(canvas: Canvas, width: Float, height: Float, placeId: String, variant: Int = 0) {
        canvas.save(); canvas.scale(width / 400f, height / 240f); canvas.clipRect(0f, 0f, 400f, 240f)
        val b = Brush(canvas)
        val night = placeId == "lake" || variant == 1
        b.gradient(400f, 240f, if (night) Pearl.SKY_DUSK_TOP else Pearl.SKY_DAY_TOP,
            if (night) Pearl.SKY_DUSK_BOTTOM else Pearl.SKY_DAY_BOTTOM)
        val sunX = if (variant == 0) 298f else 74f
        b.oval(sunX - 5f, 23f, 39f, 39f, if (night) Pearl.MOON_SHADOW else Pearl.SUN_RING)
        b.oval(sunX, 28f, 29f, 29f, if (night) Pearl.MOON else Pearl.SUN)
        if (night) listOf(40f to 30f, 120f to 52f, 200f to 26f, 350f to 44f).forEach { (x, y) -> b.oval(x, y, 2.5f, 2.5f, Pearl.STAR) }
        b.path(if (night) Pearl.HILL_FAR_DUSK else Pearl.HILL_FAR_DAY) { moveTo(0f, 123f); quadTo(87f, 46f, 182f, 121f); quadTo(300f, 69f, 400f, 102f); lineTo(400f, 240f); lineTo(0f, 240f); close() }
        for (i in 0..7) b.tree(i * 59f - 12, 161f + i % 3 * 8, 0.6f, if (night) Pearl.TREE_DUSK else Pearl.TREE_DAY, if (night) Pearl.TREE_DUSK_LIGHT else Pearl.TREE_DAY_LIGHT)
        when (placeId) {
            "creek", "lake" -> {
                b.path(if (night) Pearl.WATER_DUSK else Pearl.WATER) { moveTo(0f, 159f); quadTo(222f, 129f, 400f, 165f); lineTo(400f, 240f); lineTo(0f, 240f); close() }
                for (i in 0..5) b.line(35f + i * 52, 180f + i % 3 * 14, 62f + i * 52, 180f + i % 3 * 14, Pearl.WATER_LINE)
                if (placeId == "creek") {
                    b.path(Pearl.BRIDGE) { moveTo(79f, 178f); quadTo(204f, 63f, 335f, 178f); lineTo(335f, 201f)
                        quadTo(200f, 103f, 79f, 201f); close() }
                    b.line(79f, 178f, 79f, 199f, Pearl.BRIDGE_RAIL, 8f)
                    b.line(335f, 178f, 335f, 199f, Pearl.BRIDGE_RAIL, 8f)
                } else for (i in 0..8) b.line(334f + i * 6, 240f, 327f + i * 7, 173f - i % 3 * 12, Pearl.REED, 3f)
            }
            "mushroom" -> for (i in 0..4) {
                val x = 45f + i * 78; val y = 172f + i % 2 * 28
                b.rect(x, y, 11f, 45f, Pearl.MUSH_STEM, 5f)
                b.path(if (variant == 0) Pearl.MUSH_PINK else Pearl.MUSH_LAV) { moveTo(x - 29, y + 5); cubicTo(x - 24, y - 38, x + 35, y - 38, x + 41, y + 5); close() }
                b.oval(x - 12, y - 13, 8f, 6f, Pearl.MUSH_DOT)
            }
            "meadow" -> for (i in 0..24) b.flower(10f + i * 17, 195f + i % 4 * 11, if (i % 3 == 0) Pearl.PETAL_LAV else Pearl.PETAL_CREAM)
            "ridge" -> {
                b.path(Pearl.HILL_NEAR_DAY) { moveTo(0f, 240f); quadTo(232f, 112f, 400f, 155f); lineTo(400f, 240f); close() }
                b.tree(325f, 158f, 0.93f, Pearl.TREE_BIG_DAY, Pearl.TREE_DAY_LIGHT)
            }
            else -> {
                b.path(Pearl.MEADOW_DAY) { moveTo(120f, 240f); quadTo(296f, 159f, 214f, 125f); quadTo(329f, 169f, 204f, 240f); close() }
                b.rect(97f, 181f, 48f, 29f, Pearl.WOOD_DARK, 5f); b.oval(91f, 174f, 60f, 17f, Pearl.WOOD)
            }
        }
        canvas.restore()
    }

    fun friend(canvas: Canvas, id: String, x: Float = 0f, y: Float = 0f, scale: Float = 1f) {
        canvas.save(); canvas.translate(x, y); canvas.scale(scale, scale)
        val b = Brush(canvas)
        val body = when (id) { "pip" -> Pearl.F_PIP; "otto" -> Pearl.F_OTTO; else -> Pearl.F_HAZEL }
        if (id == "hazel") b.oval(3f, 24f, 35f, 51f, Pearl.F_HAZEL_TAIL)
        b.oval(22f, 32f, 43f, 47f, body); b.oval(18f, 12f, 48f, 41f, body)
        if (id != "pip") { b.oval(20f, 6f, 13f, 18f, body); b.oval(50f, 6f, 13f, 18f, body) }
        else {
            b.path(Pearl.F_PIP_CREST) { moveTo(24f, 16f); lineTo(37f, 0f); lineTo(48f, 17f); close() }
            b.path(Pearl.CORE) { moveTo(63f, 29f); lineTo(79f, 34f); lineTo(63f, 39f); close() }
        }
        b.oval(32f, 47f, 27f, 24f, Pearl.F_MUZZLE)
        b.oval(32f, 28f, 4f, 5f, Pearl.F_EYE); b.oval(51f, 28f, 4f, 5f, Pearl.F_EYE)
        b.oval(39f, 37f, 8f, 4f, Pearl.F_NOSE); b.oval(18f, 75f, 52f, 5f, Pearl.F_SHADOW)
        canvas.restore()
    }
}
