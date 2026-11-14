package com.clashangprpr.app.world

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.clashangprpr.app.engine.Assets
import com.clashangprpr.app.model.EraType
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class Pt(val x: Float, val y: Float)

/** Anything that walks around and collides with the tile map through its feet box. */
interface Body {
    var x: Float
    var y: Float
    val fOffX: Float
    val fOffY: Float
    val fW: Float
    val fH: Float
}

/** Minimal collision interface shared by the era arenas and the open world. */
interface Collider {
    fun move(b: Body, dx: Float, dy: Float): Boolean
    fun blocked(l: Float, t: Float, rt: Float, b: Float): Boolean
    fun isSolidAt(px: Float, py: Float): Boolean
}

class Decor(val x: Float, val y: Float, val w: Float, val h: Float, val path: String, val tint: Int = 0)

/**
 * One tile based arena per era OR procedural underground Cave Dungeon.
 * Generated from a seed, always fully connected.
 */
class TileMap(val era: EraType, seed: Long) : Collider {
    companion object {
        const val TS = 64f
        const val COLS = 40
        const val ROWS = 30
        const val GATE_COL = 27
        const val GATE_ROW = 15
        const val FLOOR_A = 0
        const val FLOOR_B = 1
        const val SOLID = 2
        const val DEEP = 3
        const val GATE = 4
    }

    val width = COLS * TS
    val height = ROWS * TS
    private val kind = Array(ROWS) { IntArray(COLS) }
    private val variant = Array(ROWS) { IntArray(COLS) }
    private val dyn = Array(ROWS) { BooleanArray(COLS) }
    val decor = ArrayList<Decor>()
    var gateOpen = false
    val gateCenterX: Float get() = GATE_COL * TS + TS / 2f
    val gateCenterY: Float get() = GATE_ROW * TS + TS / 2f

    private val rnd = Random(seed)
    private val atlasPath = when (era) {
        EraType.ERA_1_MEDIEVAL -> "tiles/t_medieval.png"
        EraType.ERA_2_CAVES -> "tiles/t_cave.png"
        EraType.ERA_3_ZOMBIE -> "tiles/t_zombie.png"
        EraType.ERA_4_ISLAND -> "tiles/t_island.png"
        EraType.ERA_5_CYBER -> "tiles/t_cyber.png"
        EraType.ERA_6_VOID -> "tiles/t_void.png"
    }
    private val atlas: Bitmap? = Assets.bmp(atlasPath)
    lateinit var minimap: Bitmap
        private set

    private val src = Rect()
    private val dst = RectF()
    private val p = Paint()
    private val treeThemed = (era == EraType.ERA_1_MEDIEVAL || era == EraType.ERA_4_ISLAND)

    private fun cellOf(v: Float, max: Int): Int = floor(v / TS).toInt().coerceIn(0, max - 1)

    // ---------------------------------------------------------------- generation

    /** Builds a multi-room procedural underground Cave / Dungeon without a locked era gate wall. */
    fun buildCave(floorNo: Int, spawn: Pt, points: List<Pt>) {
        gateOpen = true
        for (r in 0 until ROWS) for (c in 0 until COLS) {
            kind[r][c] = SOLID
            variant[r][c] = rnd.nextInt(4)
        }
        // Carve 7 organic cavern chambers connected by tunnels
        val chambers = ArrayList<Pt>()
        chambers.add(spawn)
        chambers.addAll(points)
        repeat(5) {
            chambers.add(Pt((5 + rnd.nextInt(COLS - 10)) * TS, (5 + rnd.nextInt(ROWS - 10)) * TS))
        }
        for (pt in chambers) {
            val cc = cellOf(pt.x, COLS)
            val cr = cellOf(pt.y, ROWS)
            val rad = 3 + rnd.nextInt(2)
            for (r in (cr - rad)..(cr + rad)) for (c in (cc - rad)..(cc + rad)) {
                if (c < 2 || r < 2 || c >= COLS - 2 || r >= ROWS - 2) continue
                val dx = c - cc
                val dy = r - cr
                if (dx * dx + dy * dy <= rad * rad + 1) {
                    kind[r][c] = if ((c + r + floorNo) % 3 == 0) FLOOR_B else FLOOR_A
                    variant[r][c] = rnd.nextInt(4)
                }
            }
        }
        val sc = cellOf(spawn.x, COLS)
        val sr = cellOf(spawn.y, ROWS)
        for (pt in chambers) {
            carve(sc, sr, cellOf(pt.x, COLS), cellOf(pt.y, ROWS))
        }
        // Outer border stays solid rock
        for (r in 0 until ROWS) for (c in 0 until COLS) {
            if (c < 2 || r < 2 || c >= COLS - 2 || r >= ROWS - 2) {
                kind[r][c] = SOLID
                variant[r][c] = rnd.nextInt(4)
            }
        }
        makeCaveDecor(chambers)
        buildMinimap()
    }

    private fun makeCaveDecor(avoid: List<Pt>) {
        decor.clear()
        data class D(val path: String, val w: Float, val h: Float, val tint: Int)
        val pool = listOf(
            D("environment/mushroom_glow.png", 44f, 44f, 0),
            D("environment/mushroom_big.png", 56f, 56f, 0),
            D("environment/crystal_green.png", 46f, 46f, 0),
            D("environment/crystal_white.png", 46f, 46f, 0),
            D("environment/ancient_statue.png", 56f, 56f, 0),
            D("environment/demon_skull.png", 54f, 54f, 0),
            D("environment/dino_bones.png", 84f, 64f, 0),
            D("environment/rock_3.png", 38f, 38f, 0),
            D("environment/rock_4.png", 38f, 38f, 0),
            D("environment/skeleton.png", 44f, 44f, 0)
        )
        var tries = 0
        var placed = 0
        while (placed < 48 && tries < 400) {
            tries++
            val c = 3 + rnd.nextInt(COLS - 6)
            val r = 3 + rnd.nextInt(ROWS - 6)
            if (kind[r][c] != FLOOR_A && kind[r][c] != FLOOR_B) continue
            val x = c * TS + rnd.nextInt(20)
            val y = r * TS + rnd.nextInt(20)
            var near = false
            for (a in avoid) {
                val dx = a.x - x
                val dy = a.y - y
                if (dx * dx + dy * dy < 130f * 130f) {
                    near = true
                    break
                }
            }
            if (near) continue
            val d = pool[rnd.nextInt(pool.size)]
            decor.add(Decor(x, y, d.w, d.h, d.path, d.tint))
            placed++
        }
    }

    fun build(spawn: Pt, points: List<Pt>) {
        when (era) {
            EraType.ERA_4_ISLAND -> baseIsland()
            EraType.ERA_6_VOID -> baseVoid()
            else -> baseLand()
        }
        // outer border
        for (r in 0 until ROWS) for (c in 0 until COLS) {
            if (c < 2 || r < 2 || c >= COLS - 2 || r >= ROWS - 2) {
                kind[r][c] = borderKind()
                variant[r][c] = rnd.nextInt(4)
            }
        }
        // gate wall
        for (r in 0 until ROWS) {
            kind[r][GATE_COL] = SOLID
            variant[r][GATE_COL] = 2
        }
        for (r in GATE_ROW - 1..GATE_ROW + 1) kind[r][GATE_COL] = GATE

        val all = ArrayList<Pt>()
        all.add(spawn)
        all.addAll(points)
        for (pt in all) clearZone(cellOf(pt.x, COLS), cellOf(pt.y, ROWS), 2)
        clearZone(GATE_COL - 2, GATE_ROW, 1)
        clearZone(GATE_COL + 2, GATE_ROW, 1)

        val sc = cellOf(spawn.x, COLS)
        val sr = cellOf(spawn.y, ROWS)
        carve(sc, sr, GATE_COL - 2, GATE_ROW)
        for (pt in points) {
            val c = cellOf(pt.x, COLS)
            val r = cellOf(pt.y, ROWS)
            if (c < GATE_COL) carve(sc, sr, c, r)
            else carve(GATE_COL + 2, GATE_ROW, c, r)
        }
        // the gate cells themselves stay solid until opened
        for (r in GATE_ROW - 1..GATE_ROW + 1) kind[r][GATE_COL] = GATE

        makeDecor(all)
        buildMinimap()
    }

    private fun borderKind(): Int = if (era == EraType.ERA_4_ISLAND || era == EraType.ERA_6_VOID) DEEP else SOLID

    private fun pickSolidVariant(): Int {
        return if (treeThemed) {
            if (rnd.nextFloat() < 0.72f) rnd.nextInt(2) else 2 + rnd.nextInt(2)
        } else rnd.nextInt(4)
    }

    private fun blob(k: Int, count: Int, maxW: Int, maxH: Int, onlyOn: Int = -1) {
        repeat(count) {
            val w = 1 + rnd.nextInt(maxW)
            val h = 1 + rnd.nextInt(maxH)
            val c0 = 3 + rnd.nextInt(COLS - 6 - w)
            val r0 = 3 + rnd.nextInt(ROWS - 6 - h)
            for (r in r0 until r0 + h) for (c in c0 until c0 + w) {
                if (onlyOn >= 0 && kind[r][c] != onlyOn && !(onlyOn == FLOOR_A && kind[r][c] == FLOOR_B)) continue
                kind[r][c] = k
                variant[r][c] = if (k == SOLID) pickSolidVariant() else rnd.nextInt(4)
            }
        }
    }

    private fun baseLand() {
        for (r in 0 until ROWS) for (c in 0 until COLS) {
            kind[r][c] = FLOOR_A
            variant[r][c] = rnd.nextInt(4)
        }
        blob(FLOOR_B, 34, 5, 3)
        blob(SOLID, 46, 4, 3)
    }

    private fun baseIsland() {
        for (r in 0 until ROWS) for (c in 0 until COLS) {
            kind[r][c] = DEEP
            variant[r][c] = rnd.nextInt(4)
        }
        val cx = 20f
        val cy = 15f
        val rx = 16.5f
        val ry = 12f
        for (r in 0 until ROWS) for (c in 0 until COLS) {
            val dx = (c - cx) / rx
            val dy = (r - cy) / ry
            val n = (sin(c * 0.9f) * cos(r * 1.3f)) * 0.10f + (rnd.nextFloat() - 0.5f) * 0.08f
            val d = dx * dx + dy * dy + n
            if (d < 1f) {
                kind[r][c] = if (d < 0.50f) FLOOR_B else FLOOR_A
                variant[r][c] = rnd.nextInt(4)
            }
        }
        blob(SOLID, 40, 3, 3, FLOOR_A)
        blob(SOLID, 12, 2, 2, FLOOR_B)
    }

    private fun baseVoid() {
        for (r in 0 until ROWS) for (c in 0 until COLS) {
            kind[r][c] = DEEP
            variant[r][c] = rnd.nextInt(4)
        }
        val circles = listOf(
            floatArrayOf(8f, 15f, 6.5f), floatArrayOf(18f, 7f, 4.6f), floatArrayOf(18f, 23f, 4.6f),
            floatArrayOf(13f, 15f, 3.5f), floatArrayOf(33f, 15f, 7.6f), floatArrayOf(31f, 5f, 3.2f),
            floatArrayOf(31f, 25f, 3.2f), floatArrayOf(23f, 15f, 2.6f)
        )
        for (r in 0 until ROWS) for (c in 0 until COLS) {
            for (ci in circles) {
                val dx = c - ci[0]
                val dy = r - ci[1]
                if (dx * dx + dy * dy < ci[2] * ci[2]) {
                    kind[r][c] = if (rnd.nextFloat() < 0.22f) FLOOR_B else FLOOR_A
                    variant[r][c] = rnd.nextInt(4)
                    break
                }
            }
        }
        blob(SOLID, 18, 2, 2, FLOOR_A)
    }

    private fun clearZone(cc: Int, cr: Int, rad: Int) {
        for (r in cr - rad..cr + rad) for (c in cc - rad..cc + rad) {
            if (c < 2 || r < 2 || c >= COLS - 2 || r >= ROWS - 2) continue
            if (c == GATE_COL) continue
            kind[r][c] = if ((c + r) % 4 == 0) FLOOR_B else FLOOR_A
            variant[r][c] = rnd.nextInt(4)
        }
    }

    private fun carveCell(c: Int, r: Int) {
        for (dc in -1..1) for (dr in -1..1) {
            val cc = c + dc
            val rr = r + dr
            if (cc < 2 || rr < 2 || cc >= COLS - 2 || rr >= ROWS - 2) continue
            if (cc == GATE_COL && !gateOpen) continue
            kind[rr][cc] = if ((cc * 7 + rr * 3) % 5 == 0) FLOOR_B else FLOOR_A
        }
    }

    private fun carve(c0: Int, r0: Int, c1: Int, r1: Int) {
        var c = c0
        var r = r0
        val sc = if (c1 > c0) 1 else -1
        while (c != c1) {
            carveCell(c, r)
            c += sc
        }
        val sr = if (r1 > r0) 1 else -1
        while (r != r1) {
            carveCell(c, r)
            r += sr
        }
        carveCell(c1, r1)
    }

    private fun makeDecor(avoid: List<Pt>) {
        decor.clear()
        data class D(val path: String, val w: Float, val h: Float, val tint: Int)
        val pool: List<D> = when (era) {
            EraType.ERA_1_MEDIEVAL -> listOf(D("environment/rock_1.png", 36f, 36f, 0), D("environment/rock_3.png", 34f, 34f, 0))
            EraType.ERA_2_CAVES -> listOf(
                D("environment/crystal_green.png", 46f, 46f, 0), D("environment/crystal_white.png", 46f, 46f, 0),
                D("environment/mushroom_glow.png", 40f, 40f, 0), D("environment/skeleton.png", 44f, 44f, 0)
            )
            EraType.ERA_3_ZOMBIE -> listOf(
                D("tiles/crate.png", 40f, 40f, 0), D("tiles/barrel.png", 40f, 40f, 0),
                D("environment/skeleton.png", 44f, 44f, 0), D("environment/rock_2.png", 30f, 30f, 0xFF999999.toInt())
            )
            EraType.ERA_4_ISLAND -> listOf(D("environment/rock_water.png", 36f, 36f, 0), D("environment/crystal_white.png", 38f, 38f, 0))
            EraType.ERA_5_CYBER -> listOf(D("environment/crystal_white.png", 40f, 40f, 0xFF40D0FF.toInt()), D("tiles/crate.png", 36f, 36f, 0xFF7090C0.toInt()))
            EraType.ERA_6_VOID -> listOf(D("environment/crystal_white.png", 52f, 52f, 0xFFD060FF.toInt()), D("environment/crystal_green.png", 46f, 46f, 0xFFB060FF.toInt()))
        }
        var tries = 0
        var placed = 0
        while (placed < 46 && tries < 400) {
            tries++
            val c = 3 + rnd.nextInt(COLS - 6)
            val r = 3 + rnd.nextInt(ROWS - 6)
            if (c == GATE_COL) continue
            if (kind[r][c] != FLOOR_A && kind[r][c] != FLOOR_B) continue
            val x = c * TS + rnd.nextInt(24)
            val y = r * TS + rnd.nextInt(24)
            var near = false
            for (a in avoid) {
                val dx = a.x - x
                val dy = a.y - y
                if (dx * dx + dy * dy < 150f * 150f) {
                    near = true
                    break
                }
            }
            if (near) continue
            val d = pool[rnd.nextInt(pool.size)]
            decor.add(Decor(x, y, d.w, d.h, d.path, d.tint))
            placed++
        }
    }

    private fun buildMinimap() {
        val bm = Bitmap.createBitmap(COLS, ROWS, Bitmap.Config.ARGB_8888)
        var floorCol = 0
        var solidCol = 0
        var deepCol = 0
        when (era) {
            EraType.ERA_1_MEDIEVAL -> { floorCol = Color.rgb(96, 150, 78); solidCol = Color.rgb(60, 70, 90); deepCol = Color.rgb(20, 24, 40) }
            EraType.ERA_2_CAVES -> { floorCol = Color.rgb(92, 86, 104); solidCol = Color.rgb(30, 28, 38); deepCol = Color.rgb(8, 8, 12) }
            EraType.ERA_3_ZOMBIE -> { floorCol = Color.rgb(120, 120, 126); solidCol = Color.rgb(130, 76, 64); deepCol = Color.rgb(16, 20, 24) }
            EraType.ERA_4_ISLAND -> { floorCol = Color.rgb(222, 204, 140); solidCol = Color.rgb(70, 120, 60); deepCol = Color.rgb(44, 140, 200) }
            EraType.ERA_5_CYBER -> { floorCol = Color.rgb(50, 70, 110); solidCol = Color.rgb(12, 14, 26); deepCol = Color.rgb(4, 6, 12) }
            EraType.ERA_6_VOID -> { floorCol = Color.rgb(90, 56, 150); solidCol = Color.rgb(36, 18, 66); deepCol = Color.rgb(4, 3, 10) }
        }
        for (r in 0 until ROWS) for (c in 0 until COLS) {
            bm.setPixel(c, r, when (kind[r][c]) {
                FLOOR_A, FLOOR_B -> floorCol
                SOLID -> solidCol
                DEEP -> deepCol
                else -> Color.rgb(255, 200, 40)
            })
        }
        minimap = bm
    }

    // ---------------------------------------------------------------- collision

    fun setDynamic(px: Float, py: Float, solid: Boolean) {
        val c = cellOf(px, COLS)
        val r = cellOf(py, ROWS)
        dyn[r][c] = solid
    }

    private fun solidCell(c: Int, r: Int): Boolean {
        if (c < 0 || r < 0 || c >= COLS || r >= ROWS) return true
        val k = kind[r][c]
        if (k == SOLID || k == DEEP) return true
        if (k == GATE && !gateOpen) return true
        return dyn[r][c]
    }

    override fun blocked(l: Float, t: Float, rt: Float, b: Float): Boolean {
        val c0 = floor(l / TS).toInt()
        val c1 = floor((rt - 0.01f) / TS).toInt()
        val r0 = floor(t / TS).toInt()
        val r1 = floor((b - 0.01f) / TS).toInt()
        for (r in r0..r1) for (c in c0..c1) if (solidCell(c, r)) return true
        return false
    }

    override fun isSolidAt(px: Float, py: Float): Boolean = solidCell(floor(px / TS).toInt(), floor(py / TS).toInt())

    /** Slides a body: each axis is resolved on its own so walls feel smooth. */
    override fun move(b: Body, dx: Float, dy: Float): Boolean {
        var hit = false
        if (dx != 0f) {
            val nx = b.x + dx
            if (!blocked(nx + b.fOffX, b.y + b.fOffY, nx + b.fOffX + b.fW, b.y + b.fOffY + b.fH)) b.x = nx else hit = true
        }
        if (dy != 0f) {
            val ny = b.y + dy
            if (!blocked(b.x + b.fOffX, ny + b.fOffY, b.x + b.fOffX + b.fW, ny + b.fOffY + b.fH)) b.y = ny else hit = true
        }
        return hit
    }

    // ---------------------------------------------------------------- drawing

    private fun treePath(v: Int): String? {
        if (!treeThemed) return null
        return when (v) {
            0 -> "tiles/tree_green.png"
            1 -> if (era == EraType.ERA_1_MEDIEVAL) "tiles/tree_pine.png" else "tiles/tree_green.png"
            3 -> if (era == EraType.ERA_1_MEDIEVAL) "environment/rock_rgba.png" else "environment/rock_2.png"
            else -> null
        }
    }

    private fun drawAtlas(c: Canvas, col: Int, row: Int) {
        val a = atlas ?: return
        src.set(col * 32, row * 32, col * 32 + 32, row * 32 + 32)
        c.drawBitmap(a, src, dst, null)
    }

    fun draw(c: Canvas, camX: Float, camY: Float, viewW: Float, viewH: Float, time: Float) {
        val c0 = max(0, floor(camX / TS).toInt())
        val c1 = min(COLS - 1, floor((camX + viewW) / TS).toInt())
        val r0 = max(0, floor(camY / TS).toInt())
        val r1 = min(ROWS - 1, floor((camY + viewH) / TS).toInt())
        val wave = (time * 1.6f).toInt()
        if (atlas == null) {
            c.drawColor(Color.rgb(20, 20, 30))
        }
        for (r in r0..r1) for (cc in c0..c1) {
            val x = cc * TS
            val y = r * TS
            dst.set(x, y, x + TS + 1f, y + TS + 1f)
            val v = variant[r][cc]
            when (kind[r][cc]) {
                FLOOR_A -> drawAtlas(c, v, 0)
                FLOOR_B -> drawAtlas(c, 4 + v, 0)
                SOLID -> drawAtlas(c, v, 1)
                DEEP -> drawAtlas(c, 4 + ((wave + cc + r) and 1), 1)
                GATE -> if (gateOpen) drawAtlas(c, 4 + (v and 3), 0) else drawAtlas(c, 2, 1)
            }
        }
        // overlays (trees / rocks) in a second pass so neighbours never paint over them
        if (treeThemed) {
            for (r in r0..r1) for (cc in c0..c1) {
                if (kind[r][cc] != SOLID) continue
                val path = treePath(variant[r][cc]) ?: continue
                val bm = Assets.bmp(path) ?: continue
                val x = cc * TS
                val y = r * TS
                if (variant[r][cc] == 3) dst.set(x + 4f, y + 4f, x + TS - 4f, y + TS - 2f)
                else dst.set(x - 6f, y - 16f, x + TS + 6f, y + TS + 2f)
                c.drawBitmap(bm, null, dst, null)
            }
        }
        if (!gateOpen) drawGate(c, time)
    }

    private fun drawGate(c: Canvas, time: Float) {
        val x = GATE_COL * TS
        val y = (GATE_ROW - 1) * TS
        val h = 3 * TS
        p.style = Paint.Style.FILL
        p.color = Color.rgb(22, 24, 34)
        c.drawRect(x, y, x + TS, y + h, p)
        p.color = Color.rgb(96, 84, 60)
        c.drawRect(x - 3f, y - 6f, x + TS + 3f, y + 8f, p)
        c.drawRect(x - 3f, y + h - 8f, x + TS + 3f, y + h + 6f, p)
        p.color = Color.rgb(150, 150, 165)
        var bx = x + 8f
        while (bx < x + TS) {
            c.drawRect(bx, y + 8f, bx + 6f, y + h - 8f, p)
            bx += 16f
        }
        val glow = (140 + 100 * sin(time * 3f)).toInt().coerceIn(0, 255)
        p.color = Color.argb(glow, 255, 200, 40)
        c.drawCircle(x + TS / 2f, y + h / 2f, 15f, p)
        p.color = Color.rgb(40, 30, 10)
        c.drawCircle(x + TS / 2f, y + h / 2f, 6f, p)
        c.drawRect(x + TS / 2f - 3f, y + h / 2f, x + TS / 2f + 3f, y + h / 2f + 16f, p)
    }

    fun drawDecor(c: Canvas, camX: Float, camY: Float, viewW: Float, viewH: Float) {
        for (d in decor) {
            if (d.x + d.w < camX || d.x > camX + viewW || d.y + d.h < camY || d.y > camY + viewH) continue
            val bm = (if (d.tint != 0) Assets.tinted(d.path, d.tint) else Assets.bmp(d.path)) ?: continue
            dst.set(d.x, d.y, d.x + d.w, d.y + d.h)
            c.drawBitmap(bm, null, dst, null)
        }
    }
}
