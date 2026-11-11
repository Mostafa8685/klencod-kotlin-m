package com.clashangprpr.app.model

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.sqrt

// Magic / Gun Projectile
class Projectile(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val damage: Int = 65,
    val isMagic: Boolean = true
) {
    var isDestroyed = false
    private var life = 1.6f
    val radius = 14f

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

        // Outer glow
        p.color = Color.WHITE
        canvas.drawCircle(x, y, radius * 0.5f, p)
    }
}

// Mining Rock (Stone & Iron)
class MiningRock(
    var x: Float,
    var y: Float,
    val isIron: Boolean = false
) {
    var hp = 3
    var isDestroyed = false
    val width = 72f
    val height = 72f
    val hitbox = RectF(x, y, x + width, y + height)

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
        val p = Paint()
        p.color = if (isIron) Color.parseColor("#546E7A") else Color.parseColor("#78909C")
        canvas.drawRoundRect(hitbox, 16f, 16f, p)

        p.color = if (isIron) Color.parseColor("#FFD54F") else Color.parseColor("#B0BEC5")
        p.textSize = 28f
        p.textAlign = Paint.Align.CENTER
        canvas.drawText(if (isIron) "⛏️" else "🪨", x + width / 2, y + height / 2 + 10f, p)

        // Health dots
        p.color = Color.GREEN
        for (i in 0 until hp) {
            canvas.drawCircle(x + 18f + (i * 18f), y - 8f, 4f, p)
        }
    }
}

// Wood Crate
class WoodCrate(
    var x: Float,
    var y: Float
) {
    var hp = 2
    var isDestroyed = false
    val width = 64f
    val height = 64f
    val hitbox = RectF(x, y, x + width, y + height)

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
        val p = Paint()
        p.color = Color.parseColor("#8D6E63")
        canvas.drawRoundRect(hitbox, 10f, 10f, p)

        p.color = Color.parseColor("#4E342E")
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3f
        canvas.drawRoundRect(hitbox, 10f, 10f, p)

        p.style = Paint.Style.FILL
        p.textSize = 28f
        p.textAlign = Paint.Align.CENTER
        canvas.drawText("🪵", x + width / 2, y + height / 2 + 10f, p)
    }
}

// Dungeon Wall for Maze
class DungeonWall(
    val rect: RectF
) {
    fun draw(canvas: Canvas) {
        val p = Paint().apply {
            color = Color.parseColor("#212121")
            style = Paint.Style.FILL
        }
        canvas.drawRect(rect, p)

        p.color = Color.parseColor("#37474F")
        p.style = Paint.Style.STROKE
        p.strokeWidth = 4f
        canvas.drawRect(rect, p)
    }
}
