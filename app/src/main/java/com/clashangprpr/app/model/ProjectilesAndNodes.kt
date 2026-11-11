package com.clashangprpr.app.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF

class Projectile(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val damage: Int = 75,
    val isMagic: Boolean = true,
    val isEnemy: Boolean = false
) {
    var isDestroyed = false
    private var life = 1.8f
    val radius = if (isEnemy) 12f else 15f

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
                isEnemy -> Color.parseColor("#E91E63") // Dark enemy orb
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
