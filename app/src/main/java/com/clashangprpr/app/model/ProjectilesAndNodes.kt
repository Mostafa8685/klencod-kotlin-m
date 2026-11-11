package com.clashangprpr.app.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF

// Magic / Gun Projectile
class Projectile(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val damage: Int = 75,
    val isMagic: Boolean = true
) {
    var isDestroyed = false
    private var life = 1.6f
    val radius = 16f

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
            color = if (isMagic) Color.parseColor("#00E5FF") else Color.parseColor("#FF6D00")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(x, y, radius, p)
        p.color = Color.WHITE
        canvas.drawCircle(x, y, radius * 0.5f, p)
    }
}

// Mining Rock with Real Pixel Art Rock Bitmap
class MiningRock(
    context: Context,
    var x: Float,
    var y: Float,
    val isIron: Boolean = false
) {
    var hp = 4
    var isDestroyed = false
    val width = 80f
    val height = 80f
    val hitbox = RectF(x, y, x + width, y + height)
    private var bitmap: Bitmap? = null

    init {
        try {
            context.assets.open("environment/rock_pillar.png").use { input ->
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
                // Tint iron rock with golden/bluish hue
                p.colorFilter = android.graphics.LightingColorFilter(0xFFCFE8FF.toInt(), 0x001122)
            }
            canvas.drawBitmap(bmp, src, dst, p)
        } else {
            val p = Paint().apply { color = Color.GRAY }
            canvas.drawRect(hitbox, p)
        }

        // HP dots
        val pDot = Paint().apply { color = if (isIron) Color.CYAN else Color.GREEN }
        for (i in 0 until hp) {
            canvas.drawCircle(x + 16f + (i * 16f), y - 6f, 4f, pDot)
        }
    }
}

// Wood Crate with Real Pixel Art Crate / Barrel Bitmap
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

// Dungeon Wall with Tiled Stone Brick Texture
class DungeonWall(
    val rect: RectF,
    private val wallBitmap: Bitmap?
) {
    fun draw(canvas: Canvas) {
        val bmp = wallBitmap
        if (bmp != null) {
            canvas.save()
            canvas.clipRect(rect)
            val tileW = 64f
            val tileH = 48f
            var cx = rect.left
            while (cx < rect.right) {
                var cy = rect.top
                while (cy < rect.bottom) {
                    canvas.drawBitmap(bmp, null, RectF(cx, cy, cx + tileW, cy + tileH), null)
                    cy += tileH
                }
                cx += tileW
            }
            canvas.restore()
        } else {
            val p = Paint().apply { color = Color.DKGRAY }
            canvas.drawRect(rect, p)
        }

        // Outline border
        val pBorder = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRect(rect, pBorder)
    }
}
