package com.clashangprpr.app.model

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.clashangprpr.app.engine.SpriteSheet
import kotlin.math.sqrt

// Floating Text for damage or coins
data class FloatingText(
    var text: String,
    var x: Float,
    var y: Float,
    var color: Int,
    var lifeTime: Float = 1.0f
) {
    fun update(deltaTime: Float): Boolean {
        lifeTime -= deltaTime
        y -= 35f * deltaTime
        return lifeTime > 0
    }

    fun draw(canvas: Canvas) {
        val p = Paint().apply {
            color = this@FloatingText.color
            textSize = 28f
            isFakeBoldText = true
            alpha = (lifeTime * 255).toInt().coerceIn(0, 255)
        }
        canvas.drawText(text, x, y, p)
    }
}

// Gold Coin Drop
class CoinDrop(
    var x: Float,
    var y: Float,
    val value: Int
) {
    var isCollected = false
    private var floatTimer = 0f

    fun update(deltaTime: Float) {
        floatTimer += deltaTime * 4f
    }

    fun isPlayerNear(px: Float, py: Float): Boolean {
        val dist = sqrt((px - x) * (px - x) + (py - y) * (py - y))
        return dist < 65f
    }

    fun draw(canvas: Canvas) {
        if (isCollected) return
        val currentY = y + kotlin.math.sin(floatTimer) * 5f
        val p = Paint().apply {
            color = Color.parseColor("#FFD700")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(x, currentY, 14f, p)
        p.color = Color.parseColor("#FFA000")
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3f
        canvas.drawCircle(x, currentY, 14f, p)

        p.color = Color.BLACK
        p.textSize = 16f
        p.style = Paint.Style.FILL
        p.textAlign = Paint.Align.CENTER
        canvas.drawText("🪙", x, currentY + 6f, p)
    }
}

// Treasure Chest
class TreasureChest(
    context: Context,
    var x: Float,
    var y: Float,
    val goldReward: Int = 40
) {
    var isOpened = false
    private val anim = SpriteSheet(context, "environment/chest.png", 4)
    val width = 64f
    val height = 64f

    fun open(): Int {
        if (isOpened) return 0
        isOpened = true
        return goldReward
    }

    fun isPlayerNear(px: Float, py: Float): Boolean {
        val dist = sqrt((px - (x + width / 2)) * (px - (x + width / 2)) + (py - (y + height / 2)) * (py - (y + height / 2)))
        return dist < 85f
    }

    fun draw(canvas: Canvas) {
        if (isOpened) {
            anim.update(1.0f) // advance to open frame
        }
        anim.draw(canvas, x, y, width, height)
    }
}

// Castle Merchant NPC
class MerchantNPC(
    context: Context,
    var x: Float,
    var y: Float
) {
    val width = 96f
    val height = 96f
    private val idleAnim = SpriteSheet(context, "sprites/merchant/idle.png", 4)

    fun update(deltaTime: Float) {
        idleAnim.update(deltaTime)
    }

    fun isPlayerNear(px: Float, py: Float): Boolean {
        val dist = sqrt((px - (x + width / 2)) * (px - (x + width / 2)) + (py - (y + height / 2)) * (py - (y + height / 2)))
        return dist < 120f
    }

    fun draw(canvas: Canvas) {
        idleAnim.draw(canvas, x, y, width, height)

        val textPaint = Paint().apply {
            color = Color.parseColor("#FFD700")
            textSize = 24f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText("التاجر إلريك (Merchant)", x + width / 2, y - 10f, textPaint)
    }
}

// Castle Prop (Fire / Torch / Gate)
class WorldProp(
    context: Context,
    val assetPath: String,
    val frameCount: Int,
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float
) {
    private val anim = SpriteSheet(context, assetPath, frameCount)

    fun update(deltaTime: Float) {
        anim.update(deltaTime)
    }

    fun draw(canvas: Canvas) {
        anim.draw(canvas, x, y, width, height)
    }
}
