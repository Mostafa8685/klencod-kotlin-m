package com.clashangprpr.app.world

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.clashangprpr.app.engine.SpriteCache
import kotlin.random.Random

data class WorldChunk(
    val cx: Int,
    val cy: Int,
    val biome: Int, // 0=Plains, 1=Forest, 2=Village/Road, 3=Fortress, 4=GangCamp, 5=Prison, 6=SecretOrder
    val decorItems: List<Triple<Float, Float, Int>> // x, y, type (0=tree, 1=rock, 2=grass, 3=pillar)
)

class OpenWorldChunkSystem {
    companion object {
        const val CHUNK_SIZE = 512f
        const val GRID_COUNT = 20 // 20 x 512 = 10240px!
    }

    private val activeChunks = HashMap<Long, WorldChunk>()

    private var treeBmp: Bitmap? = null
    private var pineBmp: Bitmap? = null
    private var rockBmp: Bitmap? = null
    private val bmpDstRect = RectF()

    private val plainsPaint = Paint().apply { color = Color.parseColor("#233A23"); style = Paint.Style.FILL }
    private val forestPaint = Paint().apply { color = Color.parseColor("#182B18"); style = Paint.Style.FILL }
    private val roadPaint = Paint().apply { color = Color.parseColor("#38342C"); style = Paint.Style.FILL }
    private val fortressPaint = Paint().apply { color = Color.parseColor("#2A1822"); style = Paint.Style.FILL }
    private val gangPaint = Paint().apply { color = Color.parseColor("#33221A"); style = Paint.Style.FILL }
    private val prisonPaint = Paint().apply { color = Color.parseColor("#1E242E"); style = Paint.Style.FILL }
    private val secretPaint = Paint().apply { color = Color.parseColor("#141229"); style = Paint.Style.FILL }

    private val tileLinePaint = Paint().apply {
        color = Color.argb(28, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    private val decorPaint = Paint().apply { style = Paint.Style.FILL }
    private val symbolPaint = Paint().apply {
        color = Color.argb(140, 255, 215, 0)
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val trianglePath = Path()

    fun initAssets(context: Context) {
        if (treeBmp == null) treeBmp = SpriteCache.getBitmap(context, "tiles/tree_green.png")
        if (pineBmp == null) pineBmp = SpriteCache.getBitmap(context, "tiles/tree_pine.png")
        if (rockBmp == null) rockBmp = SpriteCache.getBitmap(context, "environment/rock_rgba.png")
    }

    fun getZoneName(x: Float, y: Float): String {
        val cx = (x / CHUNK_SIZE).toInt()
        val cy = (y / CHUNK_SIZE).toInt()
        return when {
            cx in 1..4 && cy in 1..4 -> "الملاذ الآمن والقرية"
            cx in 14..18 && cy in 1..5 -> "حصن زعيم الزمن (خطر شديد!)"
            cx in 14..18 && cy in 14..18 -> "المحفل السري الغامض"
            cx in 1..5 && cy in 14..18 -> "مختبر الكيمياء البعيد"
            cx in 8..12 && cy in 13..17 -> "السجن الحديدي المحصّن"
            cx in 8..12 && cy in 5..9 -> "معقل العصابات المسلحة"
            else -> "القارة المفتوحة الشاسعة"
        }
    }

    private fun chunkKey(cx: Int, cy: Int): Long = (cx.toLong() shl 16) or (cy.toLong() and 0xFFFF)

    fun updateAroundPlayer(playerX: Float, playerY: Float) {
        val pcx = (playerX / CHUNK_SIZE).toInt().coerceIn(0, GRID_COUNT - 1)
        val pcy = (playerY / CHUNK_SIZE).toInt().coerceIn(0, GRID_COUNT - 1)

        val it = activeChunks.entries.iterator()
        while (it.hasNext()) {
            val c = it.next().value
            if (kotlin.math.abs(c.cx - pcx) > 3 || kotlin.math.abs(c.cy - pcy) > 3) {
                it.remove()
            }
        }

        for (dy in -2..2) {
            for (dx in -2..2) {
                val cx = pcx + dx
                val cy = pcy + dy
                if (cx in 0 until GRID_COUNT && cy in 0 until GRID_COUNT) {
                    val key = chunkKey(cx, cy)
                    if (!activeChunks.containsKey(key)) {
                        activeChunks[key] = generateChunk(cx, cy)
                    }
                }
            }
        }
    }

    private fun generateChunk(cx: Int, cy: Int): WorldChunk {
        val seed = (cx * 73856093L) xor (cy * 19349663L)
        val rng = Random(seed)
        val biome = when {
            cx in 14..18 && cy in 1..5 -> 3   // Fortress
            cx in 14..18 && cy in 14..18 -> 6 // Secret Order Lodge
            cx in 8..12 && cy in 13..17 -> 5  // Prison
            cx in 8..12 && cy in 5..9 -> 4    // Gang Territory
            (cx in 1..4 && cy in 1..4) || (cx in 1..5 && cy in 14..18) -> 2 // Village/Outpost
            rng.nextFloat() < 0.45f -> 1
            else -> 0
        }
        val items = mutableListOf<Triple<Float, Float, Int>>()
        val baseX = cx * CHUNK_SIZE
        val baseY = cy * CHUNK_SIZE
        for (i in 0 until 10) {
            val px = baseX + 40f + rng.nextFloat() * (CHUNK_SIZE - 80f)
            val py = baseY + 40f + rng.nextFloat() * (CHUNK_SIZE - 80f)
            val t = when (biome) {
                1 -> if (rng.nextFloat() < 0.55f) 0 else 2
                3, 5, 6 -> if (rng.nextFloat() < 0.35f) 1 else 2
                else -> if (rng.nextFloat() < 0.25f) 1 else 2
            }
            items.add(Triple(px, py, t))
        }
        return WorldChunk(cx, cy, biome, items)
    }

    fun drawVisibleChunks(canvas: Canvas, camX: Float, camY: Float, viewW: Float, viewH: Float) {
        val minCx = (camX / CHUNK_SIZE).toInt().coerceIn(0, GRID_COUNT - 1)
        val maxCx = ((camX + viewW) / CHUNK_SIZE).toInt().coerceIn(0, GRID_COUNT - 1)
        val minCy = (camY / CHUNK_SIZE).toInt().coerceIn(0, GRID_COUNT - 1)
        val maxCy = ((camY + viewH) / CHUNK_SIZE).toInt().coerceIn(0, GRID_COUNT - 1)

        for (cy in minCy..maxCy) {
            for (cx in minCx..maxCx) {
                val chunk = activeChunks[chunkKey(cx, cy)] ?: generateChunk(cx, cy)
                val x = cx * CHUNK_SIZE
                val y = cy * CHUNK_SIZE
                val p = when (chunk.biome) {
                    6 -> secretPaint
                    5 -> prisonPaint
                    4 -> gangPaint
                    3 -> fortressPaint
                    2 -> roadPaint
                    1 -> forestPaint
                    else -> plainsPaint
                }
                canvas.drawRect(x, y, x + CHUNK_SIZE, y + CHUNK_SIZE, p)
                canvas.drawRect(x, y, x + CHUNK_SIZE, y + CHUNK_SIZE, tileLinePaint)

                // Draw Secret Lodge geometric symbol in biome 6 center
                if (chunk.biome == 6 && cx == 16 && cy == 16) {
                    val mx = x + CHUNK_SIZE / 2f
                    val my = y + CHUNK_SIZE / 2f
                    canvas.drawCircle(mx, my, 120f, symbolPaint)
                    trianglePath.reset()
                    trianglePath.moveTo(mx, my - 105f)
                    trianglePath.lineTo(mx - 95f, my + 75f)
                    trianglePath.lineTo(mx + 95f, my + 75f)
                    trianglePath.close()
                    canvas.drawPath(trianglePath, symbolPaint)
                    canvas.drawCircle(mx, my + 12f, 22f, symbolPaint)
                }

                for (i in chunk.decorItems.indices) {
                    val (dx, dy, kind) = chunk.decorItems[i]
                    when (kind) {
                        0 -> {
                            val bmp = if ((i % 2) == 0) treeBmp else pineBmp
                            if (bmp != null) {
                                bmpDstRect.set(dx - 28f, dy - 36f, dx + 28f, dy + 36f)
                                canvas.drawBitmap(bmp, null, bmpDstRect, null)
                            }
                        }
                        1 -> {
                            val bmp = rockBmp
                            if (bmp != null) {
                                bmpDstRect.set(dx - 22f, dy - 22f, dx + 22f, dy + 22f)
                                canvas.drawBitmap(bmp, null, bmpDstRect, null)
                            }
                        }
                        else -> {
                            decorPaint.color = if (chunk.biome == 6) Color.argb(90, 255, 215, 0) else Color.argb(65, 255, 255, 255)
                            canvas.drawRect(dx, dy, dx + 8f, dy + 8f, decorPaint)
                        }
                    }
                }
            }
        }
    }

    fun getOpenWorldStaticObstacles(): List<RectF> {
        return listOf(
            // World Borders (10240 x 10240)
            RectF(0f, 0f, 10240f, 64f),
            RectF(0f, 10176f, 10240f, 10240f),
            RectF(0f, 0f, 64f, 10240f),
            RectF(10176f, 0f, 10240f, 10240f),
            // Boss Fortress Walls (North-East: 7400..9400, 600..2400)
            RectF(7400f, 600f, 7470f, 2400f),
            RectF(9330f, 600f, 9400f, 2400f),
            RectF(7400f, 600f, 9400f, 670f),
            // Prison Outer Walls (4300..5900, 6800..8200)
            RectF(4300f, 6800f, 4360f, 8200f),
            RectF(5840f, 6800f, 5900f, 8200f),
            RectF(4300f, 6800f, 5900f, 6860f)
        )
    }
}
