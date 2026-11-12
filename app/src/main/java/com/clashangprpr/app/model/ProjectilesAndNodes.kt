package com.clashangprpr.app.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LightingColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.clashangprpr.app.engine.SpriteCache
import kotlin.math.sqrt

class Projectile(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val damage: Int = 75,
    val isMagic: Boolean = true,
    val isEnemy: Boolean = false,
    val isLaser: Boolean = false,
    val isBomb: Boolean = false
) {
    var isDestroyed = false
    private var life = if (isBomb) 1.1f else 1.8f
    val radius = when {
        isBomb -> 18f
        isLaser -> 18f
        isEnemy -> 12f
        else -> 15f
    }

    companion object {
        private val outerPaint = Paint().apply { style = Paint.Style.FILL }
        private val innerPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        private val bombFusePaint = Paint().apply {
            color = Color.YELLOW
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
    }

    fun update(deltaTime: Float): Boolean {
        life -= deltaTime
        if (life <= 0f) {
            isDestroyed = true
            return isBomb
        }
        x += vx * deltaTime
        y += vy * deltaTime
        return false
    }

    fun isCollidingWith(rect: RectF): Boolean {
        return rect.contains(x, y)
    }

    fun draw(canvas: Canvas) {
        if (isDestroyed) return
        outerPaint.color = when {
            isBomb -> Color.parseColor("#FF6D00")
            isLaser -> Color.parseColor("#76FF03")
            isEnemy -> Color.parseColor("#E91E63")
            isMagic -> Color.parseColor("#00E5FF")
            else -> Color.parseColor("#FF6D00")
        }
        canvas.drawCircle(x, y, radius, outerPaint)
        if (isBomb) {
            canvas.drawCircle(x, y, radius + 3f, bombFusePaint)
        }
        canvas.drawCircle(x, y, radius * 0.5f, innerPaint)
    }
}

class ExplosionEffect(
    val x: Float,
    val y: Float,
    val maxRadius: Float = 185f
) {
    private var timer = 0f
    private val duration = 0.38f

    companion object {
        private val fillPaint = Paint().apply { style = Paint.Style.FILL }
        private val ringPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
    }

    fun update(deltaTime: Float): Boolean {
        timer += deltaTime
        return timer < duration
    }

    fun draw(canvas: Canvas) {
        val progress = (timer / duration).coerceIn(0f, 1f)
        val r = maxRadius * progress
        val alpha = ((1f - progress) * 200).toInt().coerceIn(0, 255)

        fillPaint.color = Color.argb(alpha / 2, 255, 110, 0)
        canvas.drawCircle(x, y, r, fillPaint)

        ringPaint.color = Color.argb(alpha, 255, 215, 0)
        canvas.drawCircle(x, y, r, ringPaint)
    }
}

class PowerGeneratorNode(
    var x: Float,
    var y: Float,
    val index: Int
) {
    var hp = 4
    var isDestroyed = false
    var lastHitSwingId = -1
    val width = 84f
    val height = 84f
    val hitbox = RectF(x, y, x + width, y + height)
    private var pulseTimer = 0f

    companion object {
        private val basePaint = Paint().apply {
            color = Color.parseColor("#263238")
            style = Paint.Style.FILL
        }
        private val strokePaint = Paint().apply {
            color = Color.parseColor("#00E5FF")
            style = Paint.Style.STROKE
        }
        private val iconPaint = Paint().apply {
            color = Color.YELLOW
            textSize = 28f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        private val pipPaint = Paint().apply {
            color = Color.parseColor("#76FF03")
            style = Paint.Style.FILL
        }
        private val labelPaint = Paint().apply {
            color = Color.WHITE
            textSize = 16f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
    }

    fun update(deltaTime: Float) {
        pulseTimer += deltaTime * 5f
    }

    fun hit(swingId: Int = -2): Boolean {
        if (isDestroyed) return false
        if (swingId >= 0 && lastHitSwingId == swingId) return false
        if (swingId >= 0) lastHitSwingId = swingId
        hp--
        if (hp <= 0) isDestroyed = true
        return true
    }

    fun draw(canvas: Canvas) {
        if (isDestroyed) return
        canvas.drawRoundRect(hitbox, 14f, 14f, basePaint)

        strokePaint.strokeWidth = 4f + kotlin.math.sin(pulseTimer) * 2f
        canvas.drawRoundRect(hitbox, 14f, 14f, strokePaint)

        canvas.drawText("⚡", x + width / 2, y + height / 2 + 10f, iconPaint)

        for (i in 0 until hp) {
            canvas.drawCircle(x + 18f + (i * 16f), y - 8f, 4f, pipPaint)
        }

        canvas.drawText("مولد طاقة $index", x + width / 2, y + height + 20f, labelPaint)
    }
}

class MiningRock(
    context: Context,
    var x: Float,
    var y: Float,
    val isIron: Boolean = false
) {
    var hp = 4
    var isDestroyed = false
    var lastHitSwingId = -1
    val width = 76f
    val height = 76f
    val hitbox = RectF(x, y, x + width, y + height)
    private var bitmap: Bitmap? = SpriteCache.getBitmap(context, "environment/rock_rgba.png")
    private val srcRect = Rect()
    private val dstRect = RectF(x, y, x + width, y + height)

    companion object {
        private val ironPaint = Paint().apply {
            colorFilter = LightingColorFilter(0xFFD0E8FF.toInt(), 0x001530)
        }
        private val fallbackPaint = Paint().apply { color = Color.GRAY }
        private val ironDotPaint = Paint().apply { color = Color.CYAN }
        private val stoneDotPaint = Paint().apply { color = Color.GREEN }
    }

    fun hit(swingId: Int = -2): Boolean {
        if (isDestroyed) return false
        if (swingId >= 0 && lastHitSwingId == swingId) return false
        if (swingId >= 0) lastHitSwingId = swingId
        hp--
        if (hp <= 0) isDestroyed = true
        return true
    }

    fun draw(canvas: Canvas) {
        if (isDestroyed) return
        val bmp = bitmap
        if (bmp != null) {
            srcRect.set(0, 0, bmp.width, bmp.height)
            canvas.drawBitmap(bmp, srcRect, dstRect, if (isIron) ironPaint else null)
        } else {
            canvas.drawRect(hitbox, fallbackPaint)
        }

        val dotPaint = if (isIron) ironDotPaint else stoneDotPaint
        for (i in 0 until hp) {
            canvas.drawCircle(x + 16f + (i * 16f), y - 6f, 4f, dotPaint)
        }
    }
}

class WoodCrate(
    context: Context,
    var x: Float,
    var y: Float,
    val isBarrel: Boolean = false
) {
    var hp = 2
    var isDestroyed = false
    var lastHitSwingId = -1
    val width = 64f
    val height = 64f
    val hitbox = RectF(x, y, x + width, y + height)
    private var bitmap: Bitmap? = SpriteCache.getBitmap(context, if (isBarrel) "tiles/barrel.png" else "tiles/crate.png")
    private val srcRect = Rect()
    private val dstRect = RectF(x, y, x + width, y + height)

    companion object {
        private val fallbackPaint = Paint().apply { color = Color.parseColor("#8D6E63") }
    }

    fun hit(swingId: Int = -2): Boolean {
        if (isDestroyed) return false
        if (swingId >= 0 && lastHitSwingId == swingId) return false
        if (swingId >= 0) lastHitSwingId = swingId
        hp--
        if (hp <= 0) isDestroyed = true
        return true
    }

    fun draw(canvas: Canvas) {
        if (isDestroyed) return
        val bmp = bitmap
        if (bmp != null) {
            srcRect.set(0, 0, bmp.width, bmp.height)
            canvas.drawBitmap(bmp, srcRect, dstRect, null)
        } else {
            canvas.drawRect(hitbox, fallbackPaint)
        }
    }
}

class ChoppableTree(
    context: Context,
    var x: Float,
    var y: Float,
    val isPine: Boolean = false
) {
    var hp = 3
    var isDestroyed = false
    var lastHitSwingId = -1
    val width = 80f
    val height = 100f
    val hitbox = RectF(x + 15f, y + 25f, x + width - 15f, y + height - 10f)
    private var bitmap: Bitmap? = SpriteCache.getBitmap(context, if (isPine) "tiles/tree_pine.png" else "tiles/tree_green.png")
    private val dstRect = RectF(x, y, x + width, y + height)

    companion object {
        private val dotPaint = Paint().apply { color = Color.GREEN }
    }

    fun hit(swingId: Int = -2): Boolean {
        if (isDestroyed) return false
        if (swingId >= 0 && lastHitSwingId == swingId) return false
        if (swingId >= 0) lastHitSwingId = swingId
        hp--
        if (hp <= 0) isDestroyed = true
        return true
    }

    fun draw(canvas: Canvas) {
        if (isDestroyed) return
        val bmp = bitmap
        if (bmp != null) {
            canvas.drawBitmap(bmp, null, dstRect, null)
        }
        for (i in 0 until hp) {
            canvas.drawCircle(x + 25f + (i * 15f), y + 10f, 4f, dotPaint)
        }
    }
}

class BuriedTreasureMound(
    var x: Float,
    var y: Float
) {
    var isDug = false
    val width = 74f
    val height = 74f
    private var glow = 0f

    companion object {
        private val ringPaint = Paint().apply {
            color = Color.parseColor("#FFA000")
            style = Paint.Style.STROKE
        }
        private val xPaint = Paint().apply {
            color = Color.YELLOW
            textSize = 28f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        private val labelPaint = Paint().apply {
            color = Color.WHITE
            textSize = 18f
            textAlign = Paint.Align.CENTER
            style = Paint.Style.FILL
            isFakeBoldText = true
        }
    }

    fun update(deltaTime: Float) {
        glow += deltaTime * 4f
    }

    fun isPlayerNear(px: Float, py: Float): Boolean {
        val dist = sqrt((px - (x + width / 2)) * (px - (x + width / 2)) + (py - (y + height / 2)) * (py - (y + height / 2)))
        return dist < 90f
    }

    fun draw(canvas: Canvas) {
        if (isDug) return
        ringPaint.strokeWidth = 3f + kotlin.math.sin(glow) * 1.5f
        canvas.drawCircle(x + width / 2, y + height / 2, 28f, ringPaint)
        canvas.drawText("✖", x + width / 2, y + height / 2 + 10f, xPaint)
        canvas.drawText("كنز مدفون", x + width / 2, y - 8f, labelPaint)
    }
}


class PlantedExplosive(var x: Float, var y: Float) {
    var fuseTimer: Float = 2.4f
    var isDetonated: Boolean = false
    val damage: Int = 220

    companion object {
        private val bodyPaint = Paint().apply { color = Color.parseColor("#37474F"); style = Paint.Style.FILL }
        private val ledPaint = Paint().apply { style = Paint.Style.FILL }
        private val ringPaint = Paint().apply { color = Color.argb(110, 255, 82, 82); style = Paint.Style.STROKE; strokeWidth = 2.5f }
    }

    fun update(deltaTime: Float): Boolean {
        if (isDetonated) return false
        fuseTimer -= deltaTime
        if (fuseTimer <= 0f) {
            isDetonated = true
            return true
        }
        return false
    }

    fun draw(canvas: Canvas) {
        if (isDetonated) return
        canvas.drawCircle(x, y, 16f, bodyPaint)
        ledPaint.color = if (((fuseTimer * 8f).toInt() % 2) == 0) Color.RED else Color.YELLOW
        canvas.drawCircle(x, y, 6f, ledPaint)
        canvas.drawCircle(x, y, 220f, ringPaint)
    }
}

class PlayerBarricade(var x: Float, var y: Float) {
    var hp: Int = 10
    var isDestroyed: Boolean = false
    val width = 76f
    val height = 76f
    val hitbox = RectF(x, y, x + width, y + height)

    companion object {
        private val woodPaint = Paint().apply { color = Color.parseColor("#5D4037"); style = Paint.Style.FILL }
        private val borderPaint = Paint().apply { color = Color.parseColor("#8D6E63"); style = Paint.Style.STROKE; strokeWidth = 4f }
        private val labelPaint = Paint().apply { color = Color.WHITE; textSize = 14f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    }

    fun draw(canvas: Canvas) {
        if (isDestroyed) return
        canvas.drawRoundRect(hitbox, 8f, 8f, woodPaint)
        canvas.drawRoundRect(hitbox, 8f, 8f, borderPaint)
        canvas.drawText("متراس ($hp)", x + width / 2, y - 6f, labelPaint)
    }
}

class LootVault(var x: Float, var y: Float, val goldAmount: Int, val vaultName: String) {
    var isLooted: Boolean = false
    val width = 72f
    val height = 64f
    val hitbox = RectF(x, y, x + width, y + height)

    companion object {
        private val fillPaint = Paint().apply { color = Color.parseColor("#455A64"); style = Paint.Style.FILL }
        private val goldPaint = Paint().apply { color = Color.parseColor("#FFD700"); style = Paint.Style.STROKE; strokeWidth = 4f }
        private val textPaint = Paint().apply { color = Color.parseColor("#FFD700"); textSize = 17f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    }

    fun isPlayerNear(px: Float, py: Float): Boolean {
        val cx = x + width / 2
        val cy = y + height / 2
        return sqrt((px - cx) * (px - cx) + (py - cy) * (py - cy)) < 95f
    }

    fun draw(canvas: Canvas) {
        if (isLooted) return
        canvas.drawRoundRect(hitbox, 10f, 10f, fillPaint)
        canvas.drawRoundRect(hitbox, 10f, 10f, goldPaint)
        canvas.drawText("سرقة: $vaultName", x + width / 2, y - 10f, textPaint)
    }
}


class PlayerGrave(
    val x: Float,
    val y: Float,
    var timeLeft: Float = 60f,
    val coins: Int,
    val wood: Int,
    val stone: Int,
    val iron: Int,
    val crystal: Int,
    val mythril: Int
) {
    var isCollected = false
    companion object {
        private val stonePaint = Paint().apply { color = Color.parseColor("#607D8B"); style = Paint.Style.FILL }
        private val borderPaint = Paint().apply { color = Color.parseColor("#CFD8DC"); style = Paint.Style.STROKE; strokeWidth = 3f }
        private val timerPaint = Paint().apply { color = Color.parseColor("#FF5252"); textSize = 18f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    }
    fun update(deltaTime: Float): Boolean {
        if (isCollected) return false
        timeLeft -= deltaTime
        return timeLeft > 0f
    }
    fun isPlayerNear(px: Float, py: Float): Boolean = sqrt((px - x) * (px - x) + (py - y) * (py - y)) < 85f
    fun draw(canvas: Canvas) {
        if (isCollected) return
        canvas.drawRoundRect(x - 24f, y - 34f, x + 24f, y + 20f, 10f, 10f, stonePaint)
        canvas.drawRoundRect(x - 24f, y - 34f, x + 24f, y + 20f, 10f, 10f, borderPaint)
        val sec = timeLeft.toInt().coerceAtLeast(0)
        canvas.drawText("قبرك (${sec}s)", x, y - 42f, timerPaint)
    }
}

class FarmPlot(var x: Float, var y: Float) {
    var stage: Int = 0 // 0=Empty, 1=Growing, 2=Ready
    var growthTimer: Float = 0f
    val width = 84f
    val height = 84f

    companion object {
        private val soilPaint = Paint().apply { color = Color.parseColor("#4E342E"); style = Paint.Style.FILL }
        private val sproutPaint = Paint().apply { color = Color.parseColor("#66BB6A"); style = Paint.Style.FILL }
        private val readyPaint = Paint().apply { color = Color.parseColor("#FFD54F"); style = Paint.Style.FILL }
        private val borderPaint = Paint().apply { color = Color.parseColor("#8D6E63"); style = Paint.Style.STROKE; strokeWidth = 3f }
        private val labelPaint = Paint().apply { color = Color.WHITE; textSize = 15f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    }

    fun update(deltaTime: Float) {
        if (stage == 1) {
            growthTimer -= deltaTime
            if (growthTimer <= 0f) stage = 2
        }
    }

    fun isPlayerNear(px: Float, py: Float): Boolean {
        val cx = x + width / 2
        val cy = y + height / 2
        return sqrt((px - cx) * (px - cx) + (py - cy) * (py - cy)) < 90f
    }

    fun draw(canvas: Canvas) {
        canvas.drawRoundRect(x, y, x + width, y + height, 8f, 8f, soilPaint)
        canvas.drawRoundRect(x, y, x + width, y + height, 8f, 8f, borderPaint)
        when (stage) {
            1 -> {
                canvas.drawCircle(x + 28f, y + 42f, 10f, sproutPaint)
                canvas.drawCircle(x + 56f, y + 42f, 10f, sproutPaint)
                canvas.drawText("ينمو (${growthTimer.toInt()}s)", x + width / 2, y - 6f, labelPaint)
            }
            2 -> {
                canvas.drawCircle(x + 28f, y + 42f, 14f, readyPaint)
                canvas.drawCircle(x + 56f, y + 42f, 14f, readyPaint)
                canvas.drawText("جاهز للحصاد!", x + width / 2, y - 6f, labelPaint)
            }
            else -> {
                canvas.drawText("أرض زراعية", x + width / 2, y - 6f, labelPaint)
            }
        }
    }
}

class RareMiningNode(
    context: Context,
    var x: Float,
    var y: Float,
    val oreType: Int // 0=Stone, 1=Iron, 2=Crystal, 3=Mythril
) {
    var hp = if (oreType >= 2) 6 else 4
    var isDestroyed = false
    var lastHitSwingId = -1
    val width = 78f
    val height = 78f
    val hitbox = RectF(x, y, x + width, y + height)
    private var bitmap: Bitmap? = SpriteCache.getBitmap(context, "environment/rock_rgba.png")
    private val dstRect = RectF(x, y, x + width, y + height)

    companion object {
        private val crystalPaint = Paint().apply { colorFilter = LightingColorFilter(0xFF00E5FF.toInt(), 0x001144) }
        private val mythrilPaint = Paint().apply { colorFilter = LightingColorFilter(0xFFE040FB.toInt(), 0x002244) }
        private val ironPaint = Paint().apply { colorFilter = LightingColorFilter(0xFFD0E8FF.toInt(), 0x001530) }
        private val labelPaint = Paint().apply { color = Color.WHITE; textSize = 14f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    }

    fun hit(swingId: Int = -2): Boolean {
        if (isDestroyed) return false
        if (swingId >= 0 && lastHitSwingId == swingId) return false
        if (swingId >= 0) lastHitSwingId = swingId
        hp--
        if (hp <= 0) isDestroyed = true
        return true
    }

    fun draw(canvas: Canvas) {
        if (isDestroyed) return
        val p = when (oreType) {
            3 -> mythrilPaint
            2 -> crystalPaint
            1 -> ironPaint
            else -> null
        }
        bitmap?.let { canvas.drawBitmap(it, null, dstRect, p) }
        val name = when (oreType) {
            3 -> "ميثريل"
            2 -> "كريستال"
            1 -> "حديد"
            else -> "حجر"
        }
        canvas.drawText("$name ($hp)", x + width / 2, y - 6f, labelPaint)
    }
}
