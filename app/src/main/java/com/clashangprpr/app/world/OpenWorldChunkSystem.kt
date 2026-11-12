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
    val biome: Int, // 0=Village, 1=MedievalForest, 2=CaveMining, 3=ZombiePrison, 4=PirateCoast, 5=CyberGang, 6=VoidLodge
    val decorItems: List<Triple<Float, Float, Int>> // x, y, type (0=tree, 1=rock, 2=building/ruin, 3=web/crystal)
)

class OpenWorldChunkSystem {
    companion object {
        const val CHUNK_SIZE = 512f
        const val GRID_COUNT = 64 // 64 x 512 = 32768px (10x previous world!)
    }

    private val activeChunks = HashMap<Long, WorldChunk>()

    private var treeBmp: Bitmap? = null
    private var pineBmp: Bitmap? = null
    private var rockBmp: Bitmap? = null
    private var webBmp: Bitmap? = null
    private var crystalBmp: Bitmap? = null
    private val bmpDstRect = RectF()

    private val biomePaints = arrayOf(
        Paint().apply { color = Color.parseColor("#2A3D28"); style = Paint.Style.FILL }, // 0 Village
        Paint().apply { color = Color.parseColor("#1B331B"); style = Paint.Style.FILL }, // 1 Medieval Forest
        Paint().apply { color = Color.parseColor("#22202B"); style = Paint.Style.FILL }, // 2 Cave/Mining
        Paint().apply { color = Color.parseColor("#2B1A1E"); style = Paint.Style.FILL }, // 3 Zombie/Prison
        Paint().apply { color = Color.parseColor("#1A3545"); style = Paint.Style.FILL }, // 4 Pirate Coast
        Paint().apply { color = Color.parseColor("#182234"); style = Paint.Style.FILL }, // 5 Cyber/Gang
        Paint().apply { color = Color.parseColor("#160F29"); style = Paint.Style.FILL }  // 6 Void/Lodge
    )

    private val tileLinePaint = Paint().apply {
        color = Color.argb(24, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    private val buildingPaint = Paint().apply {
        color = Color.argb(160, 48, 42, 38)
        style = Paint.Style.FILL
    }
    private val buildingRoofPaint = Paint().apply {
        color = Color.argb(190, 90, 55, 45)
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val symbolPaint = Paint().apply {
        color = Color.argb(150, 255, 215, 0)
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val trianglePath = Path()

    fun initAssets(context: Context) {
        if (treeBmp == null) treeBmp = SpriteCache.getBitmap(context, "tiles/tree_green.png")
        if (pineBmp == null) pineBmp = SpriteCache.getBitmap(context, "tiles/tree_pine.png")
        if (rockBmp == null) rockBmp = SpriteCache.getBitmap(context, "environment/rock_rgba.png")
        if (webBmp == null) webBmp = SpriteCache.getBitmap(context, "environment/prop_web.png")
        if (crystalBmp == null) crystalBmp = SpriteCache.getBitmap(context, "environment/crystal_green.png")
    }

    fun getBiomeAt(cx: Int, cy: Int): Int {
        return when {
            cx in 1..6 && cy in 1..6 -> 0        // Safe Village & Farms
            cx in 1..24 && cy in 1..20 -> 1      // Medieval Fortress & Forests
            cx in 1..22 && cy in 21..42 -> 2     // Dark Caves & Rare Mining Zone
            cx in 1..24 && cy in 43..62 -> 3     // Zombie City & Iron Prison
            cx in 25..46 && cy in 40..62 -> 4    // Pirate Coast & Islands
            cx in 25..46 && cy in 16..39 -> 5    // Cyber Gang & Far Alchemist
            else -> 6                            // Secret Masonic Lodge & Void Fortress
        }
    }

    fun getZoneName(x: Float, y: Float): String {
        val cx = (x / CHUNK_SIZE).toInt()
        val cy = (y / CHUNK_SIZE).toInt()
        return when (getBiomeAt(cx, cy)) {
            0 -> "القرية الآمنة والمزارع"
            1 -> "إقليم القلعة والغابات"
            2 -> "إقليم الكهوف ومنطقة التعدين النادرة"
            3 -> "إقليم الزومبي والسجن الحديدي"
            4 -> "إقليم ساحل القراصنة والكنوز"
            5 -> "إقليم العصابات ومختبر الكيمياء"
            else -> "إقليم الفراغ والمحفل السري"
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
        val biome = getBiomeAt(cx, cy)
        val items = mutableListOf<Triple<Float, Float, Int>>()
        val baseX = cx * CHUNK_SIZE
        val baseY = cy * CHUNK_SIZE

        for (i in 0 until 8) {
            val px = baseX + 48f + rng.nextFloat() * (CHUNK_SIZE - 96f)
            val py = baseY + 48f + rng.nextFloat() * (CHUNK_SIZE - 96f)
            val t = when (biome) {
                1, 4 -> if (rng.nextFloat() < 0.6f) 0 else if (rng.nextFloat() < 0.35f) 2 else 1
                2 -> if (rng.nextFloat() < 0.55f) 3 else 1
                3, 5 -> if (rng.nextFloat() < 0.45f) 2 else 1
                else -> if (rng.nextFloat() < 0.35f) 2 else 3
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
                canvas.drawRect(x, y, x + CHUNK_SIZE, y + CHUNK_SIZE, biomePaints[chunk.biome])
                canvas.drawRect(x, y, x + CHUNK_SIZE, y + CHUNK_SIZE, tileLinePaint)

                // Draw Secret Lodge geometric symbol
                if (chunk.biome == 6 && cx == 54 && cy == 54) {
                    val mx = x + CHUNK_SIZE / 2f
                    val my = y + CHUNK_SIZE / 2f
                    canvas.drawCircle(mx, my, 140f, symbolPaint)
                    trianglePath.reset()
                    trianglePath.moveTo(mx, my - 120f)
                    trianglePath.lineTo(mx - 105f, my + 85f)
                    trianglePath.lineTo(mx + 105f, my + 85f)
                    trianglePath.close()
                    canvas.drawPath(trianglePath, symbolPaint)
                    canvas.drawCircle(mx, my + 14f, 24f, symbolPaint)
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
                            rockBmp?.let {
                                bmpDstRect.set(dx - 24f, dy - 24f, dx + 24f, dy + 24f)
                                canvas.drawBitmap(it, null, bmpDstRect, null)
                            }
                        }
                        2 -> {
                            // Procedural village/ruin structure footprint
                            canvas.drawRect(dx - 45f, dy - 35f, dx + 45f, dy + 35f, buildingPaint)
                            canvas.drawRect(dx - 45f, dy - 35f, dx + 45f, dy + 35f, buildingRoofPaint)
                        }
                        else -> {
                            val bmp = if (chunk.biome == 2) crystalBmp else webBmp
                            if (bmp != null) {
                                bmpDstRect.set(dx - 22f, dy - 22f, dx + 22f, dy + 22f)
                                canvas.drawBitmap(bmp, null, bmpDstRect, null)
                            }
                        }
                    }
                }
            }
        }
    }

    fun getOpenWorldStaticObstacles(): List<RectF> {
        return listOf(
            // World Borders (32768 x 32768)
            RectF(0f, 0f, 32768f, 64f),
            RectF(0f, 32704f, 32768f, 32768f),
            RectF(0f, 0f, 64f, 32768f),
            RectF(32704f, 0f, 32768f, 32768f),
            // Grand Boss Fortress Walls (North-East: 27000..30000, 2000..4800)
            RectF(27000f, 2000f, 27080f, 4800f),
            RectF(29920f, 2000f, 30000f, 4800f),
            RectF(27000f, 2000f, 30000f, 2080f)
        )
    }
}
