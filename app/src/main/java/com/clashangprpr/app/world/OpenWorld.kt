package com.clashangprpr.app.world

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.clashangprpr.app.engine.Assets
import com.clashangprpr.app.model.BlockType
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Massive procedural open world (512x512 tiles) supporting:
 * - Breakable village houses & full freedom to build inside villages
 * - Stone immunity to swords/cannons/bombs (Pickaxe-only mining)
 * - 2-block-thick Stone Fortress immunity against Night Bomber Zombies
 * - Manual Tree Sapling planting & 3-stage Crop Farming (no auto-regrow)
 */
class OpenWorld(val seed: Int) : Collider {
    companion object {
        const val TS = 64f
        const val N = 512
        const val CH = 16
        const val CN = N / CH
        const val CX = 256
        const val CY = 256
        const val VILLAGE_R = 20f
        const val MM = 96
        const val MM_STEP = N / MM

        const val FLOOR = 0
        const val FLOOR2 = 1
        const val TREE = 2
        const val ROCK = 3
        const val ORE = 4
        const val WALL = 5
        const val DEEP = 6

        val ATLAS = arrayOf(
            "tiles/t_medieval.png", "tiles/t_cave.png", "tiles/t_zombie.png",
            "tiles/t_island.png", "tiles/t_cyber.png", "tiles/t_void.png"
        )
        val BIOME_COL = intArrayOf(
            Color.rgb(96, 150, 78), Color.rgb(92, 86, 104), Color.rgb(120, 120, 126),
            Color.rgb(222, 204, 140), Color.rgb(50, 70, 110), Color.rgb(90, 56, 150)
        )
        val ORE_MUL = intArrayOf(
            0xFFFFC080.toInt(), 0xFF90FFB0.toInt(), 0xFFC8A070.toInt(),
            0xFFFFC0E0.toInt(), 0xFF70E8FF.toInt(), 0xFFD890FF.toInt()
        )
    }

    val width = N * TS
    val height = N * TS
    val kind = ByteArray(N * N)
    val variant = ByteArray(N * N)
    val biome = ByteArray(N * N)
    /** Player-placed Minecraft/Fortnite blocks (0 = none, 1..11 = BlockType codes, 5 = open door). */
    val playerBlocks = ByteArray(N * N)
    private val playerBlockDamage = HashMap<Int, Int>()
    private val baseKind = ByteArray(N * N)
    private val modified = HashMap<Int, Int>()
    private val tileHp = HashMap<Int, Int>()

    /** Manually planted tree saplings: tileIndex -> elapsed seconds (matures at 60s into a full TREE). */
    val plantedSaplings = HashMap<Int, Float>()
    /** Manually planted farm crops: tileIndex -> elapsed seconds (stage 1: 0..18s, stage 2: 18..42s, stage 3 ready: >=42s). */
    val plantedCrops = HashMap<Int, Float>()
    /** 0 = Golden Wheat, 1 = Arcane Blue Berries (+Mana), 2 = Fire Chili Pepper (+Speed/Dmg), 3 = Golden Life Root (+HP) */
    val cropVariety = HashMap<Int, Int>()

    private val atlases = arrayOfNulls<Bitmap>(6)
    private val chunks = arrayOfNulls<Bitmap>(CN * CN)
    private val chunkStamp = IntArray(CN * CN)
    private var stamp = 0
    private var bakedThisFrame = 0
    val explored = BooleanArray(MM * MM)
    val minimap: Bitmap = Bitmap.createBitmap(MM, MM, Bitmap.Config.ARGB_8888)

    data class StructureSite(
        val tx: Int,
        val ty: Int,
        val kind: Int,
        val nameAr: String,
        val descAr: String,
        val iconAsset: String,
        val color: Int
    )

    val subVillageTiles = ArrayList<Pair<Int, Int>>()
    val fortressTiles = ArrayList<Pair<Int, Int>>()
    val caveTiles = ArrayList<Pair<Int, Int>>()
    val islandCenters = ArrayList<Pair<Int, Int>>()
    val structureSites = ArrayList<StructureSite>()

    var allowDeepWater = false
    var allowDoorPass = false
    var ignoreObstaclesForAir = false

    private val dst = RectF()
    private val src = Rect()
    private val p = Paint()

    init {
        generate()
        rebuildMinimap()
    }

    // ---------------------------------------------------------------- noise
    private fun hash(x: Int, y: Int, s: Int): Float {
        var h = x * 374761393 + y * 668265263 + s * 1442695041
        h = (h xor (h ushr 13)) * 1274126177
        h = h xor (h ushr 16)
        return (h and 0x7fffffff) / 2147483647f
    }

    private fun vnoise(x: Float, y: Float, s: Int): Float {
        val x0 = floor(x).toInt()
        val y0 = floor(y).toInt()
        val fx = x - x0
        val fy = y - y0
        val sx = fx * fx * (3f - 2f * fx)
        val sy = fy * fy * (3f - 2f * fy)
        val a = hash(x0, y0, s)
        val b = hash(x0 + 1, y0, s)
        val c = hash(x0, y0 + 1, s)
        val d = hash(x0 + 1, y0 + 1, s)
        return a + (b - a) * sx + (c - a) * sy + (a - b - c + d) * sx * sy
    }

    private fun fbm(x: Float, y: Float, s: Int): Float {
        return vnoise(x, y, s) * 0.6f + vnoise(x * 2.1f, y * 2.1f, s + 7) * 0.3f + vnoise(x * 4.3f, y * 4.3f, s + 13) * 0.1f
    }

    // ---------------------------------------------------------------- generation
    private fun generate() {
        val twoPi = 6.2831853f
        val rayCos = FloatArray(6)
        val raySin = FloatArray(6)
        subVillageTiles.clear()
        fortressTiles.clear()
        caveTiles.clear()
        islandCenters.clear()

        for (s in 0 until 6) {
            val ang = (s + 0.5f) * twoPi / 6f
            rayCos[s] = cos(ang)
            raySin[s] = sin(ang)
            val vx = (CX + rayCos[s] * 92f).toInt().coerceIn(40, N - 40)
            val vy = (CY + raySin[s] * 92f).toInt().coerceIn(40, N - 40)
            subVillageTiles.add(Pair(vx, vy))

            val fx = (CX + cos(ang + 0.18f) * 168f).toInt().coerceIn(45, N - 45)
            val fy = (CY + sin(ang + 0.18f) * 168f).toInt().coerceIn(45, N - 45)
            fortressTiles.add(Pair(fx, fy))

            val cx = (CX + cos(ang - 0.20f) * 130f).toInt().coerceIn(40, N - 40)
            val cy = (CY + sin(ang - 0.20f) * 130f).toInt().coerceIn(40, N - 40)
            caveTiles.add(Pair(cx, cy))
        }

        val islandAngles = floatArrayOf(3.40f, 3.68f, 3.95f, 4.10f, 5.45f, 5.85f)
        val islandRadii = floatArrayOf(140f, 195f, 155f, 210f, 175f, 205f)
        for (idx in islandAngles.indices) {
            val ix = (CX + cos(islandAngles[idx]) * islandRadii[idx]).toInt().coerceIn(36, N - 36)
            val iy = (CY + sin(islandAngles[idx]) * islandRadii[idx]).toInt().coerceIn(36, N - 36)
            islandCenters.add(Pair(ix, iy))
        }

        for (y in 0 until N) for (x in 0 until N) {
            val i = y * N + x
            val dx = (x - CX).toFloat()
            val dy = (y - CY).toFloat()
            val d = sqrt(dx * dx + dy * dy)
            val warp = (fbm(x * 0.035f, y * 0.035f, seed + 31) - 0.5f) * 0.85f
            var a = atan2(dy, dx) + warp
            while (a < 0f) a += twoPi
            while (a >= twoPi) a -= twoPi
            val b = ((a / twoPi) * 6f).toInt().coerceIn(0, 5)
            biome[i] = b.toByte()
            val n1 = fbm(x * 0.07f, y * 0.07f, seed)
            val n2 = fbm(x * 0.19f, y * 0.19f, seed + 5)
            val h = hash(x, y, seed + 99)
            val border = x < 4 || y < 4 || x >= N - 4 || y >= N - 4
            var k = FLOOR
            if (border) {
                k = DEEP
            } else if (d < VILLAGE_R) {
                k = if (abs(dx) < 2.5f || abs(dy) < 2.5f) FLOOR2 else FLOOR
                if (d > 15f && n2 > 0.55f && abs(dx) >= 3.5f && abs(dy) >= 3.5f) k = TREE
            } else {
                k = when (b) {
                    0 -> if (n1 > 0.59f && n2 > 0.42f) TREE
                    else if (n1 < 0.22f && n2 > 0.57f) ROCK
                    else if (h > 0.990f) ORE
                    else if (n2 > 0.65f) FLOOR2 else FLOOR
                    1 -> if (n1 > 0.57f) WALL
                    else if (h > 0.983f) ORE
                    else if (n2 > 0.72f) ROCK
                    else if (n2 > 0.58f) FLOOR2 else FLOOR
                    2 -> zombieCell(x, y, n1, n2, h)
                    3 -> {
                        var onArchipelago = false
                        for (isl in islandCenters) {
                            val idx = x - isl.first
                            val idy = y - isl.second
                            if (idx * idx + idy * idy < 18 * 18) {
                                onArchipelago = true
                                break
                            }
                        }
                        if (!onArchipelago && n1 < 0.43f) DEEP
                        else if (n1 < 0.49f && h > 0.93f) ORE
                        else if (n1 > 0.58f && n2 > 0.5f) TREE
                        else if (n2 > 0.75f) ROCK
                        else if (n1 > 0.52f || onArchipelago) FLOOR2 else FLOOR
                    }
                    4 -> cyberCell(x, y, h)
                    else -> if (n1 < 0.38f) DEEP
                    else if (n2 > 0.72f) ROCK
                    else if (h > 0.984f) ORE
                    else if (n2 < 0.25f) WALL
                    else if (n2 > 0.56f) FLOOR2 else FLOOR
                }
                if (!border) {
                    for (s in 0 until 6) {
                        val along = dx * rayCos[s] + dy * raySin[s]
                        if (along > 0f && along < 185f) {
                            val perp = abs(-dx * raySin[s] + dy * rayCos[s])
                            if (perp < 2.4f) {
                                k = FLOOR2
                                break
                            }
                        }
                    }
                }
            }
            kind[i] = k.toByte()
            variant[i] = (hash(x, y, seed + 3) * 4f).toInt().coerceIn(0, 3).toByte()
        }
        stampCentralVillage()
        stampBossArena()
        stampSubVillages()
        stampFortresses()
        stampCaveClearings()
        stampWorldStructures()
        for (i in 0 until N * N) baseKind[i] = kind[i]
    }

    private fun zombieCell(x: Int, y: Int, n1: Float, n2: Float, h: Float): Int {
        val bx = x / 14
        val by = y / 14
        val lx = x % 14
        val ly = y % 14
        val hasB = hash(bx, by, seed + 7) > 0.54f
        if (hasB && lx in 3..10 && ly in 3..9) {
            val edge = lx == 3 || lx == 10 || ly == 3 || ly == 9
            val door = ly == 9 && (lx == 6 || lx == 7)
            if (edge && !door) return if (h < 0.2f) FLOOR else WALL
            return FLOOR2
        }
        if (n1 > 0.62f && n2 > 0.5f) return TREE
        if (h > 0.985f) return ORE
        if (n2 > 0.72f) return ROCK
        return FLOOR
    }

    private fun cyberCell(x: Int, y: Int, h: Float): Int {
        val lx = x % 12
        val ly = y % 12
        val bx = x / 12
        val by = y / 12
        val hasB = hash(bx, by, seed + 11) > 0.45f
        if (hasB && lx in 2..9 && ly in 2..9) return WALL
        if (h > 0.988f) return ORE
        if (lx < 2 || ly < 2) return FLOOR2
        return FLOOR
    }

    private fun setTile(x: Int, y: Int, k: Int) {
        if (x < 0 || y < 0 || x >= N || y >= N) return
        kind[y * N + x] = k.toByte()
    }

    private fun stampHouse(x0: Int, y0: Int, w: Int = 6, h: Int = 5) {
        for (yy in 0 until h) for (xx in 0 until w) {
            val edge = xx == 0 || xx == w - 1 || yy == 0 || yy == h - 1
            val door = yy == h - 1 && (xx == w / 2 - 1 || xx == w / 2)
            setTile(x0 + xx, y0 + yy, if (edge && !door) WALL else FLOOR2)
        }
    }

    private fun stampCentralVillage() {
        val houses = arrayOf(
            intArrayOf(-11, -10), intArrayOf(5, -10),
            intArrayOf(-12, 4), intArrayOf(6, 5),
            intArrayOf(-3, -13), intArrayOf(-3, 9)
        )
        for (hs in houses) stampHouse(CX + hs[0], CY + hs[1])
        for (yy in -5..5) for (xx in -5..5) setTile(CX + xx, CY + yy, FLOOR2)
        // Starter farm plot in Central Village so player sees crops right away!
        for (fy in (CY + 5)..(CY + 7)) for (fx in (CX - 5)..(CX - 3)) {
            val idx = fy * N + fx
            kind[idx] = FLOOR.toByte()
            plantedCrops[idx] = 35f
        }
    }

    private fun stampBossArena() {
        val ax = CX
        val ay = CY - 34
        for (dy in -12..12) for (dx in -12..12) {
            val d2 = dx * dx + dy * dy
            if (d2 <= 144) {
                setTile(ax + dx, ay + dy, if (d2 <= 64 || abs(dx) <= 2 || abs(dy) <= 2) FLOOR2 else FLOOR)
            }
        }
        for (y in (ay + 10)..(CY - 12)) {
            setTile(CX - 1, y, FLOOR2)
            setTile(CX, y, FLOOR2)
            setTile(CX + 1, y, FLOOR2)
        }
    }

    private fun stampSubVillages() {
        for (v in subVillageTiles) {
            val vx = v.first
            val vy = v.second
            for (dy in -10..10) for (dx in -10..10) {
                if (dx * dx + dy * dy <= 100) {
                    setTile(vx + dx, vy + dy, if (abs(dx) <= 1 || abs(dy) <= 1) FLOOR2 else FLOOR)
                }
            }
            stampHouse(vx - 7, vy - 7, 6, 5)
            stampHouse(vx + 2, vy - 7, 6, 5)
            stampHouse(vx - 7, vy + 3, 6, 5)
        }
    }

    private fun stampFortresses() {
        for (f in fortressTiles) {
            val fx = f.first
            val fy = f.second
            for (dy in -9..9) for (dx in -9..9) {
                setTile(fx + dx, fy + dy, FLOOR2)
            }
            for (dy in -7..7) for (dx in -7..7) {
                val edge = abs(dx) == 7 || abs(dy) == 7
                val gate = (abs(dy) == 7 && abs(dx) <= 1) || (abs(dx) == 7 && abs(dy) <= 1)
                if (edge && !gate) setTile(fx + dx, fy + dy, WALL)
            }
        }
    }

    private fun stampCaveClearings() {
        for (c in caveTiles) {
            val cx = c.first
            val cy = c.second
            for (dy in -5..5) for (dx in -5..5) {
                if (dx * dx + dy * dy <= 25) setTile(cx + dx, cy + dy, FLOOR2)
            }
        }
    }

    // ---------------------------------------------------------------- Manual Farming & Tree Sapling Growth (No Auto-Regrow!)
    fun plantSaplingAt(tx: Int, ty: Int): Boolean {
        if (tx < 3 || ty < 3 || tx >= N - 3 || ty >= N - 3) return false
        val i = ty * N + tx
        if (playerBlocks[i].toInt() != 0 || plantedSaplings.containsKey(i) || plantedCrops.containsKey(i)) return false
        val k = kind[i].toInt()
        if (k != FLOOR && k != FLOOR2) return false
        plantedSaplings[i] = 0.1f
        invalidateCell(i)
        return true
    }

    fun plantCropAt(tx: Int, ty: Int, variety: Int = -1): Boolean {
        if (tx < 3 || ty < 3 || tx >= N - 3 || ty >= N - 3) return false
        val i = ty * N + tx
        if (playerBlocks[i].toInt() != 0 || plantedCrops.containsKey(i) || plantedSaplings.containsKey(i)) return false
        val k = kind[i].toInt()
        if (k != FLOOR && k != FLOOR2) return false
        plantedCrops[i] = 0.1f
        cropVariety[i] = if (variety in 0..3) variety else ((tx + ty) and 3)
        invalidateCell(i)
        return true
    }

    fun cropVarietyAt(tx: Int, ty: Int): Int {
        if (tx < 0 || ty < 0 || tx >= N || ty >= N) return 0
        return cropVariety[ty * N + tx] ?: 0
    }

    /** Waters all crops and saplings in a 3x3 area around (centerTx, centerTy), boosting growth by +25s! */
    fun waterCropsAround(centerTx: Int, centerTy: Int): Int {
        var count = 0
        for (dy in -1..1) for (dx in -1..1) {
            val tx = centerTx + dx
            val ty = centerTy + dy
            if (tx < 0 || ty < 0 || tx >= N || ty >= N) continue
            val i = ty * N + tx
            val c = plantedCrops[i]
            if (c != null && c < 42f) {
                plantedCrops[i] = (c + 26f).coerceAtMost(42.5f)
                invalidateCell(i)
                count++
            }
            val s = plantedSaplings[i]
            if (s != null) {
                val next = s + 32f
                if (next >= 60f) {
                    plantedSaplings.remove(i)
                    kind[i] = TREE.toByte()
                    tileHp.remove(i)
                } else {
                    plantedSaplings[i] = next
                }
                invalidateCell(i)
                count++
            }
        }
        return count
    }

    fun isCropReadyAt(tx: Int, ty: Int): Boolean {
        if (tx < 0 || ty < 0 || tx >= N || ty >= N) return false
        val g = plantedCrops[ty * N + tx] ?: return false
        return g >= 42f
    }

    fun hasAnyCropAt(tx: Int, ty: Int): Boolean {
        if (tx < 0 || ty < 0 || tx >= N || ty >= N) return false
        return plantedCrops.containsKey(ty * N + tx)
    }

    fun harvestCropAt(tx: Int, ty: Int): Boolean {
        if (!isCropReadyAt(tx, ty)) return false
        val i = ty * N + tx
        plantedCrops.remove(i)
        invalidateCell(i)
        return true
    }

    fun destroyCropAt(tx: Int, ty: Int): Boolean {
        if (tx < 0 || ty < 0 || tx >= N || ty >= N) return false
        val i = ty * N + tx
        if (plantedCrops.remove(i) != null) {
            invalidateCell(i)
            return true
        }
        return false
    }

    fun updateFarmingAndSaplings(dt: Float) {
        if (plantedSaplings.isNotEmpty()) {
            val sKeys = ArrayList(plantedSaplings.keys)
            for (i in sKeys) {
                val prev = plantedSaplings[i] ?: continue
                val next = prev + dt
                if (next >= 60f) {
                    plantedSaplings.remove(i)
                    kind[i] = TREE.toByte()
                    tileHp.remove(i)
                    invalidateCell(i)
                } else {
                    plantedSaplings[i] = next
                    if ((prev < 28f && next >= 28f)) invalidateCell(i)
                }
            }
        }
        if (plantedCrops.isNotEmpty()) {
            val cKeys = ArrayList(plantedCrops.keys)
            for (i in cKeys) {
                val prev = plantedCrops[i] ?: continue
                if (prev >= 42f) continue
                val next = prev + dt
                plantedCrops[i] = next
                if ((prev < 18f && next >= 18f) || (prev < 42f && next >= 42f)) {
                    invalidateCell(i)
                }
            }
        }
    }

    fun saveFarming(): String {
        val sb = StringBuilder()
        for ((k, v) in plantedSaplings) sb.append("S").append(k).append(':').append(v.toInt()).append(',')
        for ((k, v) in plantedCrops) sb.append("C").append(k).append(':').append(v.toInt()).append(',')
        return sb.toString()
    }

    fun loadFarming(s: String) {
        plantedSaplings.clear()
        plantedCrops.clear()
        for (part in s.split(',')) {
            if (part.length < 3) continue
            val type = part[0]
            val ix = part.indexOf(':')
            if (ix <= 1) continue
            val tileIdx = part.substring(1, ix).toIntOrNull() ?: continue
            val sec = part.substring(ix + 1).toFloatOrNull() ?: continue
            if (tileIdx in 0 until N * N) {
                if (type == 'S') plantedSaplings[tileIdx] = sec
                else if (type == 'C') plantedCrops[tileIdx] = sec
            }
        }
    }


    /**
     * Generates and stamps 12 Major Procedural World Structures across the 512x512 Open World:
     * 0 = برج الساحر الأثري (Arcane Wizard Tower)
     * 1 = الأطلال الملكية المفقودة (Ancient Royal Ruins)
     * 2 = منجم البارود المهجور (Abandoned Gunpowder Mine)
     * 3 = حطام سفينة القراصنة الغارقة (Sunken Pirate Shipwreck)
     * 4 = مسلة الدم ومقبرة العمالقة (Cursed Blood Obelisk)
     * 5 = قلعة المراقبة الحجرية القابلة للاحتلال (Claimable Sniper Watchtower)
     */
    private fun stampWorldStructures() {
        structureSites.clear()
        val twoPi = 6.2831853f
        val names = arrayOf(
            "برج الساحر الأثري 🔮",
            "الأطلال الملكية المفقودة 🏛️",
            "منجم البارود المهجور 💣",
            "حطام سفينة القراصنة 🚢",
            "مسلة الدم ومقبرة العمالقة 🩸",
            "قلعة المراقبة الحجرية 🏰"
        )
        val descs = arrayOf(
            "برج من حجر السبج والرخام يضم منارة شفاء ومذبح حكمة يرفع قوة سحرك!",
            "معبد ملكي قديم تحرسه فخاخ الأشواك ويحتوي على كنوز ومفاتيح ذهبية!",
            "منجم غني بعروق الحديد وبراميل البارود الجاهزة لتعبئة مدافعك!",
            "حطام سفينة قراصنة عملاقة على الشاطئ مليئة بالذهب وخرائط الكنز!",
            "ساحة تحدٍ مظلمة تمنحك باروداً وشظايا عصور وأختام هاوية عند تفعيلها!",
            "حصن مراقبة جاهز بسمك 2 حجر ونوافذ قنص يمكنك اتخاذه قاعدة متقدمة!"
        )
        val icons = arrayOf(
            "icons/structure_tower.png",
            "icons/structure_ruins.png",
            "icons/structure_mine.png",
            "icons/structure_shipwreck.png",
            "icons/structure_obelisk.png",
            "icons/blueprint_watchtower.png"
        )
        val cols = intArrayOf(
            Color.parseColor("#00E5FF"),
            Color.parseColor("#FFD54F"),
            Color.parseColor("#FF9800"),
            Color.parseColor("#4FC3F7"),
            Color.parseColor("#FF1744"),
            Color.parseColor("#69F0AE")
        )

        for (i in 0 until 12) {
            val k = i % 6
            val ring = if (i < 6) 64f else 148f
            val ang = (i * twoPi / 6f) + (if (i < 6) 0.26f else -0.24f)
            val sx = (CX + cos(ang) * ring).toInt().coerceIn(32, N - 32)
            val sy = (CY + sin(ang) * ring).toInt().coerceIn(32, N - 32)
            val title = if (i < 6) names[k] else "${names[k]} (الشمالي)"
            structureSites.add(StructureSite(sx, sy, k, title, descs[k], icons[k], cols[k]))

            // Stamp physical tile architecture for each structure!
            for (dy in -6..6) for (dx in -6..6) {
                val tx = sx + dx
                val ty = sy + dy
                val idx = ty * N + tx
                if (dx * dx + dy * dy <= 38) {
                    kind[idx] = FLOOR2.toByte()
                    playerBlocks[idx] = 0
                }
            }
            when (k) {
                0 -> { // Arcane Wizard Tower (Circular Obsidian + Glass + Royal Floor + Healing Beacon)
                    for (dy in -4..4) for (dx in -4..4) {
                        val d2 = dx * dx + dy * dy
                        val idx = (sy + dy) * N + (sx + dx)
                        if (d2 in 11..18) {
                            playerBlocks[idx] = if (dy == 4 && abs(dx) <= 1) BlockType.DOOR_CLOSED.code.toByte()
                            else if (abs(dx) == 4 || abs(dy) == 4) BlockType.GLASS_BLOCK.code.toByte()
                            else BlockType.OBSIDIAN_WALL.code.toByte()
                        } else if (d2 <= 10) {
                            playerBlocks[idx] = BlockType.ROYAL_FLOOR.code.toByte()
                        }
                    }
                    playerBlocks[(sy - 2) * N + sx] = BlockType.HEALING_BEACON.code.toByte()
                }
                1 -> { // Ancient Royal Ruins (Marble floors, stone pillars, spike traps)
                    for (dy in -4..4) for (dx in -4..4) {
                        val idx = (sy + dy) * N + (sx + dx)
                        val isPillar = (abs(dx) == 4 || abs(dx) == 2) && (abs(dy) == 4 || abs(dy) == 2)
                        playerBlocks[idx] = when {
                            isPillar -> BlockType.STONE_BRICK.code.toByte()
                            abs(dx) == 3 && abs(dy) <= 1 -> BlockType.SPIKE_TRAP.code.toByte()
                            abs(dx) <= 3 && abs(dy) <= 3 -> BlockType.ROYAL_FLOOR.code.toByte()
                            else -> 0
                        }
                    }
                }
                2 -> { // Abandoned Gunpowder Mine (Timber walls, ore veins, TNT barrels)
                    for (dy in -4..4) for (dx in -4..4) {
                        val idx = (sy + dy) * N + (sx + dx)
                        val edge = abs(dx) == 4 || abs(dy) == 4
                        if (edge && !(dy == 4 && abs(dx) <= 1)) {
                            playerBlocks[idx] = BlockType.FENCE.code.toByte()
                        } else if ((abs(dx) == 3 && abs(dy) == 3)) {
                            kind[idx] = ORE.toByte()
                        } else if (abs(dx) == 2 && dy == -2) {
                            playerBlocks[idx] = BlockType.TNT_BLOCK.code.toByte()
                        }
                    }
                }
                3 -> { // Sunken Pirate Shipwreck (Wooden ship hull + bridge deck)
                    for (dy in -3..3) for (dx in -5..5) {
                        val idx = (sy + dy) * N + (sx + dx)
                        val hullEdge = abs(dy) == 3 && abs(dx) <= 4
                        playerBlocks[idx] = when {
                            hullEdge -> BlockType.WOOD_WALL.code.toByte()
                            abs(dx) <= 4 && abs(dy) <= 2 -> BlockType.WOOD_BRIDGE.code.toByte()
                            else -> 0
                        }
                    }
                }
                4 -> { // Cursed Blood Obelisk Arena (Obsidian corners + Barbed Wire ring)
                    for (dy in -4..4) for (dx in -4..4) {
                        val idx = (sy + dy) * N + (sx + dx)
                        if (abs(dx) == 4 && abs(dy) == 4) {
                            playerBlocks[idx] = BlockType.OBSIDIAN_WALL.code.toByte()
                        } else if ((abs(dx) == 3 || abs(dy) == 3) && !(dy == 3 && abs(dx) <= 1)) {
                            playerBlocks[idx] = BlockType.BARRICADE_WIRE.code.toByte()
                        }
                    }
                }
                5 -> { // Claimable 2-Thick Stone Sniper Watchtower
                    for (dy in -4..4) for (dx in -4..4) {
                        val idx = (sy + dy) * N + (sx + dx)
                        val outer = abs(dx) == 4 || abs(dy) == 4
                        val inner = abs(dx) == 3 || abs(dy) == 3
                        val gate = dy >= 3 && dx == 0
                        val win = (abs(dy) >= 3 && dx == 0 && dy < 0) || (abs(dx) >= 3 && dy == 0)
                        playerBlocks[idx] = when {
                            gate -> BlockType.IRON_GATE.code.toByte()
                            win -> BlockType.FIRING_WINDOW.code.toByte()
                            outer || inner -> BlockType.STONE_BRICK.code.toByte()
                            else -> BlockType.ROYAL_FLOOR.code.toByte()
                        }
                    }
                    playerBlocks[(sy - 1) * N + sx] = BlockType.HEALING_BEACON.code.toByte()
                }
            }
        }
    }

    /** Builds a 7x7 2-Thick Stone Sniper Watchtower with Firing Windows, Royal Floor, and a Healing Beacon! */
    fun buildWatchtowerBlueprintAt(centerTx: Int, centerTy: Int) {
        val x0 = (centerTx - 3).coerceIn(6, N - 14)
        val y0 = (centerTy - 3).coerceIn(6, N - 14)
        for (dy in 0..6) for (dx in 0..6) {
            val tx = x0 + dx
            val ty = y0 + dy
            val i = ty * N + tx
            if (kind[i].toInt() == DEEP) continue
            kind[i] = FLOOR2.toByte()
            val outer = dx == 0 || dx == 6 || dy == 0 || dy == 6
            val inner = dx == 1 || dx == 5 || dy == 1 || dy == 5
            val gate = dy >= 5 && dx == 3
            val win = (dy <= 1 && dx == 3) || (dy == 3 && (dx <= 1 || dx >= 5))
            playerBlocks[i] = when {
                gate -> BlockType.IRON_GATE.code.toByte()
                win -> BlockType.FIRING_WINDOW.code.toByte()
                outer || inner -> BlockType.STONE_BRICK.code.toByte()
                dx == 2 && dy == 2 -> BlockType.HEALING_BEACON.code.toByte()
                dx == 4 && dy == 2 -> BlockType.TORCH.code.toByte()
                else -> BlockType.ROYAL_FLOOR.code.toByte()
            }
            invalidateCell(i)
        }
    }

    /** Builds an 8x8 Fenced Royal Farm Compound with 12 Pre-Planted & Irrigated Multi-Variety Crops! */
    fun buildFarmEstateBlueprintAt(centerTx: Int, centerTy: Int) {
        val x0 = (centerTx - 4).coerceIn(6, N - 16)
        val y0 = (centerTy - 4).coerceIn(6, N - 16)
        for (dy in 0..7) for (dx in 0..7) {
            val tx = x0 + dx
            val ty = y0 + dy
            val i = ty * N + tx
            if (kind[i].toInt() == DEEP) continue
            kind[i] = FLOOR.toByte()
            playerBlocks[i] = 0
            val edge = dx == 0 || dx == 7 || dy == 0 || dy == 7
            val gate = dy == 7 && (dx == 3 || dx == 4)
            if (gate) {
                playerBlocks[i] = BlockType.IRON_GATE.code.toByte()
            } else if (edge) {
                playerBlocks[i] = BlockType.FENCE.code.toByte()
            } else if ((dx == 1 || dx == 6) && (dy == 1 || dy == 6)) {
                playerBlocks[i] = BlockType.TORCH.code.toByte()
            } else if (dx in 2..5 && dy in 2..4) {
                plantedCrops[i] = 32f
                cropVariety[i] = (dx + dy) and 3
            } else {
                playerBlocks[i] = BlockType.ROYAL_FLOOR.code.toByte()
            }
            invalidateCell(i)
        }
    }

    // ---------------------------------------------------------------- Instant Blueprint Builders (House & 2-Thick Stone Fort)
    fun buildHouseBlueprintAt(centerTx: Int, centerTy: Int) {
        val x0 = (centerTx - 3).coerceIn(5, N - 12)
        val y0 = (centerTy - 6).coerceIn(5, N - 12)
        for (dy in 0..5) for (dx in 0..6) {
            val tx = x0 + dx
            val ty = y0 + dy
            val i = ty * N + tx
            if (kind[i].toInt() == DEEP) continue
            kind[i] = FLOOR2.toByte()
            plantedCrops.remove(i)
            plantedSaplings.remove(i)
            val edge = dx == 0 || dx == 6 || dy == 0 || dy == 5
            val door = dy == 5 && dx == 3
            val win = (dy == 0 && dx == 3) || (dy == 2 && (dx == 0 || dx == 6))
            playerBlocks[i] = when {
                door -> BlockType.DOOR_CLOSED.code.toByte()
                win -> BlockType.FIRING_WINDOW.code.toByte()
                edge -> BlockType.WOOD_WALL.code.toByte()
                (dx == 1 && dy == 1) || (dx == 5 && dy == 1) -> BlockType.TORCH.code.toByte()
                else -> 0
            }
            invalidateCell(i)
        }
    }

    /** Builds a 2-block-thick Stone Fortress (سمك 2 حجر) that is 100% immune to Night Bomber Zombies! */
    fun buildFortress2ThickAt(centerTx: Int, centerTy: Int) {
        val x0 = (centerTx - 4).coerceIn(6, N - 16)
        val y0 = (centerTy - 4).coerceIn(6, N - 16)
        for (dy in 0..8) for (dx in 0..8) {
            val tx = x0 + dx
            val ty = y0 + dy
            val i = ty * N + tx
            if (kind[i].toInt() == DEEP) continue
            kind[i] = FLOOR2.toByte()
            val outerRing = dx == 0 || dx == 8 || dy == 0 || dy == 8
            val innerRing = dx == 1 || dx == 7 || dy == 1 || dy == 7
            val gate = (dy >= 7 && dx == 4)
            val windowSpot = (dy == 0 || dy == 1) && dx == 4
            playerBlocks[i] = when {
                gate -> BlockType.DOOR_CLOSED.code.toByte()
                windowSpot -> BlockType.FIRING_WINDOW.code.toByte()
                outerRing || innerRing -> BlockType.STONE_BRICK.code.toByte()
                (dx == 2 && dy == 2) || (dx == 6 && dy == 2) -> BlockType.TORCH.code.toByte()
                else -> 0
            }
            invalidateCell(i)
        }
    }

    // ---------------------------------------------------------------- Stone & 2-Thick Wall Rules
    fun isStoneWallTile(tx: Int, ty: Int): Boolean {
        if (tx < 0 || ty < 0 || tx >= N || ty >= N) return false
        val i = ty * N + tx
        val pb = playerBlocks[i].toInt()
        if (pb == BlockType.STONE_BRICK.code || pb == BlockType.FIRING_WINDOW.code || pb == BlockType.IRON_GATE.code || pb == BlockType.OBSIDIAN_WALL.code) return true
        return kind[i].toInt() == WALL
    }

    /**
     * Checks if the Stone Wall at (tx, ty) has thickness >= 2 stone blocks (سمك 2 حجر).
     * A 2-thick stone wall has an adjacent stone wall backing it along X or Y (or >=2 orthogonal stone neighbors).
     */
    fun isDoubleStoneWallAt(tx: Int, ty: Int): Boolean {
        if (tx in 0 until N && ty in 0 until N && playerBlocks[ty * N + tx].toInt() == BlockType.OBSIDIAN_WALL.code) return true
        if (!isStoneWallTile(tx, ty)) return false
        // Check if backed by another stone wall horizontally or vertically with a 2x2 or 2-layer thickness
        val left = isStoneWallTile(tx - 1, ty)
        val right = isStoneWallTile(tx + 1, ty)
        val up = isStoneWallTile(tx, ty - 1)
        val down = isStoneWallTile(tx, ty + 1)
        // If there is a 2x2 block of stone or both X and Y have stone neighbors, or a parallel stone layer:
        var count = 0
        if (left) count++
        if (right) count++
        if (up) count++
        if (down) count++
        if ((left || right) && (up || down)) {
            // Check diagonal to confirm 2-thick layer rather than a thin corner
            if (isStoneWallTile(tx - 1, ty - 1) || isStoneWallTile(tx + 1, ty - 1) ||
                isStoneWallTile(tx - 1, ty + 1) || isStoneWallTile(tx + 1, ty + 1)) {
                return true
            }
        }
        // Also if directly backed along X (2 stone tiles side-by-side in a vertical wall) or Y (2 stone tiles in a horizontal wall)
        if ((left && (isStoneWallTile(tx - 1, ty - 1) || isStoneWallTile(tx - 1, ty + 1))) ||
            (right && (isStoneWallTile(tx + 1, ty - 1) || isStoneWallTile(tx + 1, ty + 1))) ||
            (up && (isStoneWallTile(tx - 1, ty - 1) || isStoneWallTile(tx + 1, ty - 1))) ||
            (down && (isStoneWallTile(tx - 1, ty + 1) || isStoneWallTile(tx + 1, ty + 1)))) {
            return true
        }
        return count >= 3
    }

    // ---------------------------------------------------------------- Minecraft & Fortnite Block Placement, Editing & Removal
    fun blockCodeAtTile(tx: Int, ty: Int): Int {
        if (tx < 0 || ty < 0 || tx >= N || ty >= N) return 0
        return playerBlocks[ty * N + tx].toInt()
    }

    fun blockCodeAtPx(px: Float, py: Float): Int {
        val tx = floor(px / TS).toInt()
        val ty = floor(py / TS).toInt()
        return blockCodeAtTile(tx, ty)
    }

    fun canPlaceBlockAt(tx: Int, ty: Int, blk: BlockType, playerCx: Float, playerCy: Float): Boolean {
        if (tx < 3 || ty < 3 || tx >= N - 3 || ty >= N - 3) return false
        val i = ty * N + tx
        if (playerBlocks[i].toInt() != 0) return false
        val k = kind[i].toInt()
        if (blk.bridge) {
            return k == FLOOR || k == FLOOR2 || k == DEEP
        }
        if (k != FLOOR && k != FLOOR2) return false
        if (blk.solid) {
            val tileCx = tx * TS + TS / 2f
            val tileCy = ty * TS + TS / 2f
            if (abs(tileCx - playerCx) < 40f && abs(tileCy - playerCy) < 40f) return false
        }
        return true
    }

    fun placeBlock(tx: Int, ty: Int, blk: BlockType, playerCx: Float, playerCy: Float): Boolean {
        if (!canPlaceBlockAt(tx, ty, blk, playerCx, playerCy)) return false
        val i = ty * N + tx
        plantedCrops.remove(i)
        plantedSaplings.remove(i)
        playerBlocks[i] = blk.code.toByte()
        playerBlockDamage.remove(i)
        invalidateCell(i)
        val mx = tx / MM_STEP
        val my = ty / MM_STEP
        if (mx in 0 until MM && my in 0 until MM) minimap.setPixel(mx, my, Color.rgb(220, 180, 90))
        return true
    }

    fun editBlockAt(tx: Int, ty: Int): Int {
        if (tx < 3 || ty < 3 || tx >= N - 3 || ty >= N - 3) return 0
        val i = ty * N + tx
        val pb = playerBlocks[i].toInt()
        val next = when (pb) {
            BlockType.WOOD_WALL.code, BlockType.STONE_BRICK.code -> BlockType.FIRING_WINDOW.code
            BlockType.FIRING_WINDOW.code -> BlockType.DOOR_CLOSED.code
            BlockType.DOOR_CLOSED.code -> 5
            5 -> BlockType.STONE_BRICK.code
            else -> return 0
        }
        playerBlocks[i] = next.toByte()
        invalidateCell(i)
        return next
    }

    /**
     * Damages a player-built or village wall when attacked by zombies or Bomber Zombie blasts.
     * - If the wall is a 2-block-thick Stone Wall (سمك 2 حجر), it CANNOT be broken by zombies or Bomber Zombies!
     */
    fun damagePlayerBlockAt(tx: Int, ty: Int, dmg: Int = 1, isBomberBlast: Boolean = false): Int {
        if (tx < 0 || ty < 0 || tx >= N || ty >= N) return 0
        val i = ty * N + tx
        // Check if zombies trampled a planted crop!
        if (plantedCrops.containsKey(i)) {
            plantedCrops.remove(i)
            invalidateCell(i)
            return 1
        }
        val pb = playerBlocks[i].toInt()
        val k = kind[i].toInt()
        if (pb == 0 && k != WALL) return 0
        if (pb == BlockType.WOOD_BRIDGE.code || pb == BlockType.LAUNCH_PAD.code) return 0

        // Check 2-thick Stone Wall immunity!
        if (isStoneWallTile(tx, ty) && isDoubleStoneWallAt(tx, ty)) {
            return -1 // -1 means blocked by 2-thick stone wall!
        }

        // If Bomber Zombie blast hits a 1-thick stone wall or wood wall, it shatters it!
        val maxHits = when {
            isBomberBlast -> 1
            pb == BlockType.STONE_BRICK.code || pb == BlockType.FIRING_WINDOW.code || k == WALL -> 8
            else -> 3
        }
        val cur = (playerBlockDamage[i] ?: 0) + dmg
        if (cur >= maxHits) {
            playerBlockDamage.remove(i)
            if (pb != 0) {
                playerBlocks[i] = 0
            } else if (k == WALL) {
                kind[i] = FLOOR2.toByte()
                modified[i] = 99999
            }
            invalidateCell(i)
            return 2 // broken!
        }
        playerBlockDamage[i] = cur
        return 1 // damaged
    }

    fun toggleDoorAt(tx: Int, ty: Int): Boolean {
        if (tx < 0 || ty < 0 || tx >= N || ty >= N) return false
        val i = ty * N + tx
        val pb = playerBlocks[i].toInt()
        if (pb == 4) {
            playerBlocks[i] = 5
            invalidateCell(i)
            return true
        } else if (pb == 5) {
            playerBlocks[i] = 4
            invalidateCell(i)
            return true
        }
        return false
    }

    fun removeBlockAt(tx: Int, ty: Int): BlockType? {
        if (tx < 0 || ty < 0 || tx >= N || ty >= N) return null
        val i = ty * N + tx
        val pb = playerBlocks[i].toInt()
        if (pb == 0) return null
        playerBlocks[i] = 0
        playerBlockDamage.remove(i)
        invalidateCell(i)
        val code = if (pb == 5) 4 else pb
        return BlockType.byCode(code)
    }

    fun savePlayerBlocks(): String {
        val sb = StringBuilder()
        var count = 0
        for (i in 0 until N * N) {
            val pb = playerBlocks[i].toInt()
            if (pb != 0) {
                if (count++ > 1800) break
                sb.append(i).append(':').append(pb).append(',')
            }
        }
        return sb.toString()
    }

    fun loadPlayerBlocks(s: String) {
        for (part in s.split(',')) {
            if (part.isEmpty()) continue
            val ix = part.indexOf(':')
            if (ix <= 0) continue
            val i = part.substring(0, ix).toIntOrNull() ?: continue
            val b = part.substring(ix + 1).toIntOrNull() ?: continue
            if (i in 0 until N * N) {
                playerBlocks[i] = b.toByte()
            }
        }
    }

    // ---------------------------------------------------------------- queries
    fun kindAt(i: Int): Int = kind[i].toInt()

    fun kindAtPx(px: Float, py: Float): Int {
        val x = floor(px / TS).toInt().coerceIn(0, N - 1)
        val y = floor(py / TS).toInt().coerceIn(0, N - 1)
        return kind[y * N + x].toInt()
    }

    fun isWaterAt(px: Float, py: Float): Boolean {
        val x = floor(px / TS).toInt().coerceIn(0, N - 1)
        val y = floor(py / TS).toInt().coerceIn(0, N - 1)
        val i = y * N + x
        if (playerBlocks[i].toInt() == BlockType.WOOD_BRIDGE.code) return false
        return kind[i].toInt() == DEEP
    }

    fun biomeAtPx(px: Float, py: Float): Int {
        val x = floor(px / TS).toInt().coerceIn(0, N - 1)
        val y = floor(py / TS).toInt().coerceIn(0, N - 1)
        return biome[y * N + x].toInt()
    }

    fun inVillage(px: Float, py: Float, scale: Float = 1f): Boolean {
        val tx = px / TS
        val ty = py / TS
        val dx = tx - CX
        val dy = ty - CY
        if (sqrt(dx * dx + dy * dy) < VILLAGE_R * scale) return true
        for (v in subVillageTiles) {
            val vdx = tx - v.first
            val vdy = ty - v.second
            if (sqrt(vdx * vdx + vdy * vdy) < 11f * scale) return true
        }
        return false
    }

    fun freeAt(px: Float, py: Float): Boolean = !blocked(px - 22f, py - 22f, px + 22f, py + 22f)

    val spawnX: Float get() = CX * TS + 8f
    val spawnY: Float get() = (CY + 3) * TS

    private fun solidCell(c: Int, r: Int): Boolean {
        if (c < 0 || r < 0 || c >= N || r >= N) return true
        if (ignoreObstaclesForAir) return false
        val i = r * N + c
        val pb = playerBlocks[i].toInt()
        if (pb != 0) {
            if (pb == BlockType.WOOD_BRIDGE.code || pb == 5 || pb == BlockType.TORCH.code || pb == BlockType.LAUNCH_PAD.code) return false
            if (pb == BlockType.DOOR_CLOSED.code || pb == BlockType.IRON_GATE.code) return !allowDoorPass
            return true
        }
        val k = kind[i].toInt()
        if (k == DEEP) return !allowDeepWater
        return k >= TREE
    }

    override fun blocked(l: Float, t: Float, rt: Float, b: Float): Boolean {
        val c0 = floor(l / TS).toInt()
        val c1 = floor((rt - 0.01f) / TS).toInt()
        val r0 = floor(t / TS).toInt()
        val r1 = floor((b - 0.01f) / TS).toInt()
        for (r in r0..r1) for (c in c0..c1) if (solidCell(c, r)) return true
        return false
    }

    fun isSolidForProjectile(px: Float, py: Float, friendly: Boolean): Boolean {
        val c = floor(px / TS).toInt()
        val r = floor(py / TS).toInt()
        if (c < 0 || r < 0 || c >= N || r >= N) return true
        val pb = playerBlocks[r * N + c].toInt()
        if (pb == BlockType.FENCE.code) return false
        if (pb == BlockType.FIRING_WINDOW.code && friendly) return false
        return solidCell(c, r)
    }

    override fun isSolidAt(px: Float, py: Float): Boolean = isSolidForProjectile(px, py, false)

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

    // ---------------------------------------------------------------- tile interaction (Village WALL is now breakable with Pickaxe!)
    fun breakable(i: Int): Boolean {
        if (playerBlocks[i].toInt() != 0) return true
        val k = kind[i].toInt()
        return k == TREE || k == ROCK || k == ORE || k == WALL
    }

    fun isStoneMaterialAt(i: Int): Boolean {
        val pb = playerBlocks[i].toInt()
        if (pb == BlockType.STONE_BRICK.code || pb == BlockType.FIRING_WINDOW.code || pb == BlockType.IRON_GATE.code) return true
        val k = kind[i].toInt()
        return k == ROCK || k == ORE || k == WALL
    }

    /** 0 = nothing, 1 = damaged, 2 = broken (No auto-regrow; modified tiles stay cleared unless replanted!) */
    fun damageTile(i: Int, day: Int): Int {
        if (!breakable(i)) return 0
        val k = kind[i].toInt()
        val need = when (k) {
            TREE -> 3
            WALL -> 3
            ROCK -> 4
            else -> 5
        }
        val d = (tileHp[i] ?: 0) + 1
        if (d >= need) {
            tileHp.remove(i)
            kind[i] = (if (k == WALL) FLOOR2 else FLOOR).toByte()
            modified[i] = 99999
            invalidateCell(i)
            return 2
        }
        tileHp[i] = d
        return 1
    }

    /** Auto-regrow is disabled per user request! Trees must be planted from saplings by hand. */
    fun regrow(day: Int, px: Float, py: Float) {
        // Intentionally empty: resources no longer auto-respawn; player plants saplings and crops manually!
    }

    fun saveMods(): String {
        val sb = StringBuilder()
        var count = 0
        for ((k, v) in modified) {
            if (count++ > 800) break
            sb.append(k).append('.').append(v).append(',')
        }
        return sb.toString()
    }

    fun loadMods(s: String) {
        for (part in s.split(',')) {
            if (part.isEmpty()) continue
            val ix = part.indexOf('.')
            if (ix <= 0) continue
            val i = part.substring(0, ix).toIntOrNull() ?: continue
            val d = part.substring(ix + 1).toIntOrNull() ?: continue
            if (i in 0 until N * N) {
                kind[i] = FLOOR.toByte()
                modified[i] = d
            }
        }
    }

    fun saveExplored(): String {
        val sb = StringBuilder(MM * MM)
        for (b in explored) sb.append(if (b) '1' else '0')
        return sb.toString()
    }

    fun loadExplored(s: String) {
        for (i in 0 until min(s.length, explored.size)) explored[i] = s[i] == '1'
        rebuildMinimap()
    }

    // ---------------------------------------------------------------- minimap / exploration
    private fun cellColor(mx: Int, my: Int): Int {
        val x = mx * MM_STEP + MM_STEP / 2
        val y = my * MM_STEP + MM_STEP / 2
        val i = y * N + x
        if (playerBlocks[i].toInt() != 0) return Color.rgb(220, 180, 90)
        val k = kind[i].toInt()
        val bc = BIOME_COL[biome[i].toInt()]
        return when (k) {
            DEEP -> Color.rgb(16, 36, 68)
            WALL -> Color.rgb(Color.red(bc) / 3, Color.green(bc) / 3, Color.blue(bc) / 3)
            TREE -> Color.rgb(Color.red(bc) / 2, Color.green(bc) / 2 + 20, Color.blue(bc) / 2)
            FLOOR2 -> Color.rgb(min(255, Color.red(bc) + 25), min(255, Color.green(bc) + 25), min(255, Color.blue(bc) + 25))
            else -> bc
        }
    }

    private fun rebuildMinimap() {
        for (my in 0 until MM) for (mx in 0 until MM) {
            minimap.setPixel(mx, my, if (explored[my * MM + mx]) cellColor(mx, my) else Color.argb(235, 6, 8, 16))
        }
    }

    fun reveal(px: Float, py: Float, radiusTiles: Int): Boolean {
        val cx = (px / TS).toInt() / MM_STEP
        val cy = (py / TS).toInt() / MM_STEP
        val rr = radiusTiles / MM_STEP + 1
        var changed = false
        for (my in max(0, cy - rr)..min(MM - 1, cy + rr)) for (mx in max(0, cx - rr)..min(MM - 1, cx + rr)) {
            val dx = mx - cx
            val dy = my - cy
            if (dx * dx + dy * dy > rr * rr) continue
            val ix = my * MM + mx
            if (!explored[ix]) {
                explored[ix] = true
                minimap.setPixel(mx, my, cellColor(mx, my))
                changed = true
            }
        }
        return changed
    }

    // ---------------------------------------------------------------- rendering (chunk cache)
    private fun atlas(b: Int): Bitmap? {
        var a = atlases[b]
        if (a == null) {
            a = Assets.bmp(ATLAS[b])
            atlases[b] = a
        }
        return a
    }

    fun invalidateCell(i: Int) {
        val x = i % N
        val y = i / N
        val ci = (y / CH) * CN + (x / CH)
        val b = chunks[ci]
        if (b != null) {
            chunks[ci] = null
            b.recycle()
        }
    }

    private fun bake(cx: Int, cy: Int): Bitmap {
        val bmp = Bitmap.createBitmap(CH * 32, CH * 32, Bitmap.Config.RGB_565)
        val cv = Canvas(bmp)
        val rock1 = Assets.bmp("environment/rock_rgba.png")
        val rock3 = Assets.bmp("environment/rock_3.png") ?: rock1
        val rock4 = Assets.bmp("environment/rock_4.png") ?: rock1
        for (ly in 0 until CH) for (lx in 0 until CH) {
            val x = cx * CH + lx
            val y = cy * CH + ly
            val i = y * N + x
            val a = atlas(biome[i].toInt()) ?: continue
            val v = variant[i].toInt() and 3
            val k = kind[i].toInt()
            var col = v
            var row = 0
            if (k == FLOOR2) col = 4 + v
            else if (k == WALL) {
                col = 2
                row = 1
            } else if (k == DEEP) {
                col = 4
                row = 1
            }
            src.set(col * 32, row * 32, col * 32 + 32, row * 32 + 32)
            dst.set(lx * 32f, ly * 32f, lx * 32f + 32f, ly * 32f + 32f)
            cv.drawBitmap(a, src, dst, null)
            if (k == ROCK) {
                val rk = when (v) {
                    1 -> rock3
                    2 -> rock4
                    else -> rock1
                }
                if (rk != null) {
                    dst.set(lx * 32f + 2f, ly * 32f + 3f, lx * 32f + 30f, ly * 32f + 31f)
                    cv.drawBitmap(rk, null, dst, null)
                }
            } else if (k == ORE) {
                val ore = Assets.tinted("environment/rock_rgba.png", ORE_MUL[biome[i].toInt()], 0x001020)
                if (ore != null) {
                    dst.set(lx * 32f + 2f, ly * 32f + 3f, lx * 32f + 30f, ly * 32f + 31f)
                    cv.drawBitmap(ore, null, dst, null)
                }
                p.color = ORE_MUL[biome[i].toInt()]
                cv.drawRect(lx * 32f + 9f, ly * 32f + 11f, lx * 32f + 12f, ly * 32f + 14f, p)
                cv.drawRect(lx * 32f + 20f, ly * 32f + 18f, lx * 32f + 23f, ly * 32f + 21f, p)
            }

            // Draw manually planted saplings or farm crops!
            val sapTime = plantedSaplings[i]
            if (sapTime != null) {
                val sapBmp = Assets.bmp(if (sapTime < 28f) "tiles/sapling_stage1.png" else "tiles/sapling_stage2.png")
                if (sapBmp != null) {
                    dst.set(lx * 32f, ly * 32f, lx * 32f + 32f, ly * 32f + 32f)
                    cv.drawBitmap(sapBmp, null, dst, null)
                }
            }
            val cropTime = plantedCrops[i]
            if (cropTime != null) {
                val stg = when {
                    cropTime < 18f -> 1
                    cropTime < 42f -> 2
                    else -> 3
                }
                val cropBmp = Assets.bmp("tiles/crop_stage$stg.png")
                if (cropBmp != null) {
                    dst.set(lx * 32f, ly * 32f, lx * 32f + 32f, ly * 32f + 32f)
                    cv.drawBitmap(cropBmp, null, dst, null)
                }
            }

            // Draw player-placed Minecraft & Fortnite-style blocks
            val pb = playerBlocks[i].toInt()
            if (pb != 0) {
                dst.set(lx * 32f, ly * 32f, lx * 32f + 32f, ly * 32f + 32f)
                when (pb) {
                    BlockType.WOOD_WALL.code -> Assets.bmp("tiles/block_wood.png")?.let { cv.drawBitmap(it, null, dst, null) }
                    BlockType.STONE_BRICK.code -> Assets.bmp("tiles/block_stone_brick.png")?.let { cv.drawBitmap(it, null, dst, null) }
                    BlockType.WOOD_BRIDGE.code -> Assets.bmp("tiles/block_bridge.png")?.let { cv.drawBitmap(it, null, dst, null) }
                    BlockType.DOOR_CLOSED.code -> Assets.bmp("tiles/door.png")?.let { cv.drawBitmap(it, null, dst, null) }
                    5 -> {
                        dst.set(lx * 32f, ly * 32f, lx * 32f + 9f, ly * 32f + 32f)
                        Assets.bmp("tiles/door.png")?.let { cv.drawBitmap(it, null, dst, null) }
                    }
                    BlockType.FENCE.code -> Assets.bmp("tiles/block_fence.png")?.let { cv.drawBitmap(it, null, dst, null) }
                    BlockType.TORCH.code -> {
                        p.color = Color.argb(95, 255, 190, 60)
                        cv.drawCircle(lx * 32f + 16f, ly * 32f + 16f, 15f, p)
                        dst.set(lx * 32f + 6f, ly * 32f + 4f, lx * 32f + 26f, ly * 32f + 28f)
                        Assets.bmp("icons/pot17.png")?.let { cv.drawBitmap(it, null, dst, null) }
                    }
                    BlockType.GLASS_BLOCK.code -> Assets.bmp("tiles/block_glass.png")?.let { cv.drawBitmap(it, null, dst, null) }
                    BlockType.TNT_BLOCK.code -> Assets.bmp("tiles/block_tnt.png")?.let { cv.drawBitmap(it, null, dst, null) }
                    BlockType.FIRING_WINDOW.code -> Assets.bmp("tiles/block_window.png")?.let { cv.drawBitmap(it, null, dst, null) }
                    BlockType.LAUNCH_PAD.code -> Assets.bmp("tiles/block_launchpad.png")?.let { cv.drawBitmap(it, null, dst, null) }
                    BlockType.IRON_GATE.code -> Assets.bmp("tiles/iron_gate.png")?.let { cv.drawBitmap(it, null, dst, null) }
                    BlockType.OBSIDIAN_WALL.code -> Assets.bmp("tiles/block_obsidian.png")?.let { cv.drawBitmap(it, null, dst, null) }
                    BlockType.ROYAL_FLOOR.code -> Assets.bmp("tiles/block_royal_floor.png")?.let { cv.drawBitmap(it, null, dst, null) }
                    BlockType.SPIKE_TRAP.code -> Assets.bmp("tiles/block_spikes.png")?.let { cv.drawBitmap(it, null, dst, null) }
                    BlockType.BARRICADE_WIRE.code -> Assets.bmp("tiles/block_wire.png")?.let { cv.drawBitmap(it, null, dst, null) }
                    BlockType.HEALING_BEACON.code -> Assets.bmp("tiles/block_beacon.png")?.let { cv.drawBitmap(it, null, dst, null) }
                }
            }
        }
        return bmp
    }

    private fun chunkBitmap(cx: Int, cy: Int, force: Boolean): Bitmap? {
        val ci = cy * CN + cx
        var b = chunks[ci]
        if (b == null) {
            if (!force && bakedThisFrame >= 3) return null
            bakedThisFrame++
            b = bake(cx, cy)
            chunks[ci] = b
            evictOld(ci)
        }
        chunkStamp[ci] = stamp
        return b
    }

    private fun evictOld(keep: Int) {
        var count = 0
        for (b in chunks) if (b != null) count++
        if (count <= 16) return
        var oldest = -1
        var oldestStamp = Int.MAX_VALUE
        for (i in chunks.indices) {
            if (i == keep || chunks[i] == null) continue
            if (chunkStamp[i] < oldestStamp) {
                oldestStamp = chunkStamp[i]
                oldest = i
            }
        }
        if (oldest >= 0) {
            chunks[oldest]?.recycle()
            chunks[oldest] = null
        }
    }

    fun warm(px: Float, py: Float) {
        stamp++
        val cs = CH * TS
        val pcx = (px / cs).toInt()
        val pcy = (py / cs).toInt()
        for (cy in max(0, pcy - 1)..min(CN - 1, pcy + 1)) for (cx in max(0, pcx - 1)..min(CN - 1, pcx + 1)) {
            chunkBitmap(cx, cy, true)
        }
    }

    fun draw(c: Canvas, camX: Float, camY: Float, viewW: Float, viewH: Float) {
        stamp++
        bakedThisFrame = 0
        val cs = CH * TS
        val c0 = max(0, floor(camX / cs).toInt())
        val c1 = min(CN - 1, floor((camX + viewW) / cs).toInt())
        val r0 = max(0, floor(camY / cs).toInt())
        val r1 = min(CN - 1, floor((camY + viewH) / cs).toInt())
        for (cy in r0..r1) for (cx in c0..c1) {
            val b = chunkBitmap(cx, cy, false)
            if (b != null) {
                dst.set(cx * cs, cy * cs, cx * cs + cs + 1f, cy * cs + cs + 1f)
                c.drawBitmap(b, null, dst, null)
            } else {
                p.color = Color.rgb(12, 14, 24)
                c.drawRect(cx * cs, cy * cs, cx * cs + cs, cy * cs + cs, p)
            }
        }
    }

    fun drawTrees(c: Canvas, camX: Float, camY: Float, viewW: Float, viewH: Float) {
        val c0 = max(0, floor(camX / TS).toInt() - 1)
        val c1 = min(N - 1, floor((camX + viewW) / TS).toInt() + 1)
        val r0 = max(0, floor(camY / TS).toInt() - 1)
        val r1 = min(N - 1, floor((camY + viewH) / TS).toInt() + 2)
        for (r in r0..r1) for (cc in c0..c1) {
            val i = r * N + cc
            val pb = playerBlocks[i].toInt()
            if (pb == BlockType.TORCH.code) {
                val x = cc * TS
                val y = r * TS
                p.color = Color.argb(80, 255, 200, 60)
                c.drawCircle(x + 32f, y + 32f, 88f, p)
            }
            val cropT = plantedCrops[i]
            if (cropT != null && cropT >= 42f) {
                val x = cc * TS
                val y = r * TS
                WTextLabel.draw(c, "جاهز للحصاد 🌾", x + 32f, y - 4f)
            }
            if (kind[i].toInt() != TREE) continue
            val b = biome[i].toInt()
            val v = variant[i].toInt() and 1
            val bm = when (b) {
                0 -> Assets.bmp(if (v == 0) "tiles/tree_green.png" else "tiles/tree_pine.png")
                1 -> Assets.bmp(if (v == 0) "environment/mushroom_big.png" else "environment/mushroom_glow.png")
                2 -> Assets.tinted("tiles/tree_pine.png", 0xFF8A7A6A.toInt())
                else -> Assets.bmp("tiles/tree_green.png")
            } ?: continue
            val x = cc * TS
            val y = r * TS
            dst.set(x - 8f, y - 40f, x + 72f, y + 62f)
            c.drawBitmap(bm, null, dst, null)
        }
    }
}

private object WTextLabel {
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 13.5f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    fun draw(c: Canvas, s: String, x: Float, y: Float) {
        tp.color = Color.BLACK
        c.drawText(s, x + 1.2f, y + 1.2f, tp)
        tp.color = Color.parseColor("#FFD54F")
        c.drawText(s, x, y, tp)
    }
}
