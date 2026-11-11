package com.clashangprpr.app.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.sqrt

class Projectile(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val damage: Int = 75,
    val isMagic: Boolean = true,
    val isEnemy: Boolean = false,
    val isLaser: Boolean = false
) {
    var isDestroyed = false
    private var life = 1.8f
    val radius = if (isLaser) 18f else if (isEnemy) 12f else 15f

    fun update(deltaTime: Float) {
        life -= deltaTime
        if (life <= 0f) {
            isDestroyed = true
            return
        }
        x += vx * deltaTime
        y += vy * deltaTime
    }

    fun isCollidingWith(rect: RectF): Boolean {
        return rect.contains(x, y)
    }

    fun draw(canvas: Canvas) {
        if (isDestroyed) return
        val p = Paint().apply {
            color = when {
                isLaser -> Color.parseColor("#76FF03") // Neon green laser
                isEnemy -> Color.parseColor("#E91E63") // Enemy bullet/orb
                isMagic -> Color.parseColor("#00E5FF") // Magic cyan flame
                else -> Color.parseColor("#FF6D00")    // Bomb fireball
            }
            style = Paint.Style.FILL
        }
        canvas.drawCircle(x, y, radius, p)
        p.color = Color.WHITE
        canvas.drawCircle(x, y, radius * 0.5f, p)
    }
}

// EMP Power Generator Node for Era 5!
class PowerGeneratorNode(
    var x: Float,
    var y: Float,
    val index: Int
) {
    var hp = 4
    var isDestroyed = false
    val width = 84f
    val height = 84f
    val hitbox = RectF(x, y, x + width, y + height)
    private var pulseTimer = 0f

    fun update(deltaTime: Float) {
        pulseTimer += deltaTime * 5f
    }

    fun hit(): Boolean {
        if (isDestroyed) return false
        hp--
        if (hp <= 0) isDestroyed = true
        return true
    }

    fun draw(canvas: Canvas) {
        if (isDestroyed) return
        val p = Paint()
        // Core generator base
        p.color = Color.parseColor("#263238")
        canvas.drawRoundRect(hitbox, 14f, 14f, p)

        // Pulsing electric field
        p.color = Color.parseColor("#00E5FF")
        p.style = Paint.Style.STROKE
        p.strokeWidth = 4f + kotlin.math.sin(pulseTimer) * 2f
        canvas.drawRoundRect(hitbox, 14f, 14f, p)
        p.style = Paint.Style.FILL

        // Center nuclear icon
        p.color = Color.YELLOW
        p.textSize = 28f
        p.textAlign = Paint.Align.CENTER
        p.isFakeBoldText = true
        canvas.drawText("⚡", x + width / 2, y + height / 2 + 10f, p)

        // Health pips
        p.color = Color.parseColor("#76FF03")
        for (i in 0 until hp) {
            canvas.drawCircle(x + 18f + (i * 16f), y - 8f, 4f, p)
        }

        p.color = Color.WHITE
        p.textSize = 16f
        canvas.drawText("مولد طاقة $index", x + width / 2, y + height + 20f, p)
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
    val width = 76f
    val height = 76f
    val hitbox = RectF(x, y, x + width, y + height)
    private var bitmap: Bitmap? = null

    init {
        try {
            context.assets.open("environment/rock_rgba.png").use { input ->
                bitmap = BitmapFactory.decodeStream(input)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hit(): Boolean {
        if (isDestroyed) return false
        hp--
        if (hp <= 0) {
            isDestroyed = true
        }
        return true
    }

    fun draw(canvas: Canvas) {
        if (isDestroyed) return
        val bmp = bitmap
        if (bmp != null) {
            val src = Rect(0, 0, bmp.width, bmp.height)
            val dst = RectF(x, y, x + width, y + height)
            val p = Paint()
            if (isIron) {
                p.colorFilter = android.graphics.LightingColorFilter(0xFFD0E8FF.toInt(), 0x001530)
            }
            canvas.drawBitmap(bmp, src, dst, p)
        } else {
            val p = Paint().apply { color = Color.GRAY }
            canvas.drawRect(hitbox, p)
        }

        val pDot = Paint().apply { color = if (isIron) Color.CYAN else Color.GREEN }
        for (i in 0 until hp) {
            canvas.drawCircle(x + 16f + (i * 16f), y - 6f, 4f, pDot)
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
    val width = 64f
    val height = 64f
    val hitbox = RectF(x, y, x + width, y + height)
    private var bitmap: Bitmap? = null

    init {
        try {
            val assetName = if (isBarrel) "tiles/barrel.png" else "tiles/crate.png"
            context.assets.open(assetName).use { input ->
                bitmap = BitmapFactory.decodeStream(input)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hit(): Boolean {
        if (isDestroyed) return false
        hp--
        if (hp <= 0) {
            isDestroyed = true
        }
        return true
    }

    fun draw(canvas: Canvas) {
        if (isDestroyed) return
        val bmp = bitmap
        if (bmp != null) {
            val src = Rect(0, 0, bmp.width, bmp.height)
            val dst = RectF(x, y, x + width, y + height)
            canvas.drawBitmap(bmp, src, dst, null)
        } else {
            val p = Paint().apply { color = Color.parseColor("#8D6E63") }
            canvas.drawRect(hitbox, p)
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
    val width = 80f
    val height = 100f
    val hitbox = RectF(x + 15f, y + 25f, x + width - 15f, y + height - 10f)
    private var bitmap: Bitmap? = null

    init {
        try {
            val asset = if (isPine) "tiles/tree_pine.png" else "tiles/tree_green.png"
            context.assets.open(asset).use { input ->
                bitmap = BitmapFactory.decodeStream(input)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hit(): Boolean {
        if (isDestroyed) return false
        hp--
        if (hp <= 0) isDestroyed = true
        return true
    }

    fun draw(canvas: Canvas) {
        if (isDestroyed) return
        val bmp = bitmap
        if (bmp != null) {
            canvas.drawBitmap(bmp, null, RectF(x, y, x + width, y + height), null)
        }
        val p = Paint().apply { color = Color.GREEN }
        for (i in 0 until hp) {
            canvas.drawCircle(x + 25f + (i * 15f), y + 10f, 4f, p)
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

    fun update(deltaTime: Float) {
        glow += deltaTime * 4f
    }

    fun isPlayerNear(px: Float, py: Float): Boolean {
        val dist = sqrt((px - (x + width / 2)) * (px - (x + width / 2)) + (py - (y + height / 2)) * (py - (y + height / 2)))
        return dist < 90f
    }

    fun draw(canvas: Canvas) {
        if (isDug) return
        val p = Paint().apply {
            color = Color.parseColor("#FFA000")
            style = Paint.Style.STROKE
            strokeWidth = 3f + kotlin.math.sin(glow) * 1.5f
        }
        canvas.drawCircle(x + width / 2, y + height / 2, 28f, p)

        val pText = Paint().apply {
            color = Color.YELLOW
            textSize = 28f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText("✖", x + width / 2, y + height / 2 + 10f, pText)

        p.color = Color.WHITE
        p.textSize = 18f
        p.style = Paint.Style.FILL
        canvas.drawText("كنز مدفون", x + width / 2, y - 8f, p)
    }
}
