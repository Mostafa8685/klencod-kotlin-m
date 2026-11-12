package com.clashangprpr.app.model

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.clashangprpr.app.engine.SpriteSheet
import kotlin.math.sqrt

data class FloatingText(
    var text: String,
    var x: Float,
    var y: Float,
    var color: Int,
    var lifeTime: Float = 1.0f,
    val isCrit: Boolean = false
) {
    companion object {
        private val paint = Paint().apply {
            isFakeBoldText = true
        }
    }

    fun update(deltaTime: Float): Boolean {
        lifeTime -= deltaTime
        y -= (if (isCrit) 52f else 35f) * deltaTime
        return lifeTime > 0
    }

    fun draw(canvas: Canvas) {
        paint.color = color
        paint.textSize = if (isCrit) 38f else 28f
        paint.alpha = (lifeTime * 255).toInt().coerceIn(0, 255)
        canvas.drawText(text, x, y, paint)
    }
}

class CoinDrop(
    var x: Float,
    var y: Float,
    val value: Int
) {
    var isCollected = false
    private var floatTimer = 0f

    companion object {
        private val fillPaint = Paint().apply {
            color = Color.parseColor("#FFD700")
            style = Paint.Style.FILL
        }
        private val strokePaint = Paint().apply {
            color = Color.parseColor("#FFA000")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        private val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 15f
            style = Paint.Style.FILL
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
    }

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
        canvas.drawCircle(x, currentY, 14f, fillPaint)
        canvas.drawCircle(x, currentY, 14f, strokePaint)
        canvas.drawText("$", x, currentY + 5f, textPaint)
    }
}

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
            anim.update(1.0f)
        }
        anim.draw(canvas, x, y, width, height)
    }
}

class MerchantNPC(
    context: Context,
    var x: Float,
    var y: Float
) {
    val width = 96f
    val height = 96f
    val hitbox = RectF(x + 18f, y + 20f, x + width - 18f, y + height - 8f)
    private val idleAnim = SpriteSheet(context, "sprites/merchant/idle.png", 4)
    private val shadowRect = RectF()

    companion object {
        private val shadowPaint = Paint().apply {
            color = Color.argb(100, 0, 0, 0)
            style = Paint.Style.FILL
        }
        private val textPaint = Paint().apply {
            color = Color.parseColor("#FFD700")
            textSize = 22f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
    }

    fun update(deltaTime: Float) {
        idleAnim.update(deltaTime)
    }

    fun isPlayerNear(px: Float, py: Float): Boolean {
        val dist = sqrt((px - (x + width / 2)) * (px - (x + width / 2)) + (py - (y + height / 2)) * (py - (y + height / 2)))
        return dist < 120f
    }

    fun draw(canvas: Canvas) {
        shadowRect.set(x + 18f, y + height - 16f, x + width - 18f, y + height)
        canvas.drawOval(shadowRect, shadowPaint)
        idleAnim.draw(canvas, x, y, width, height)
        canvas.drawText("التاجر إلريك", x + width / 2, y - 10f, textPaint)
    }
}

class ChronomancerNPC(
    context: Context,
    var x: Float,
    var y: Float
) {
    val width = 96f
    val height = 96f
    val hitbox = RectF(x + 18f, y + 20f, x + width - 18f, y + height - 8f)
    private val idleAnim = SpriteSheet(context, "sprites/chronomancer/idle.png", 4)
    private val shadowRect = RectF()

    companion object {
        private val shadowPaint = Paint().apply {
            color = Color.argb(100, 0, 0, 0)
            style = Paint.Style.FILL
        }
        private val textPaint = Paint().apply {
            color = Color.parseColor("#00E5FF")
            textSize = 22f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
    }

    fun update(deltaTime: Float) {
        idleAnim.update(deltaTime)
    }

    fun isPlayerNear(px: Float, py: Float): Boolean {
        val dist = sqrt((px - (x + width / 2)) * (px - (x + width / 2)) + (py - (y + height / 2)) * (py - (y + height / 2)))
        return dist < 120f
    }

    fun draw(canvas: Canvas) {
        shadowRect.set(x + 18f, y + height - 16f, x + width - 18f, y + height)
        canvas.drawOval(shadowRect, shadowPaint)
        idleAnim.draw(canvas, x, y, width, height)
        canvas.drawText("حارس الساعة", x + width / 2, y - 10f, textPaint)
    }
}

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


class AlchemistNPC(context: Context, var x: Float, var y: Float) {
    val width = 96f
    val height = 96f
    val hitbox = RectF(x + 18f, y + 20f, x + width - 18f, y + height - 8f)
    private val idleAnim = SpriteSheet(context, "sprites/chronomancer/idle.png", 4)
    private val shadowRect = RectF()
    companion object {
        private val shadowPaint = Paint().apply { color = Color.argb(100, 0, 0, 0); style = Paint.Style.FILL }
        private val textPaint = Paint().apply { color = Color.parseColor("#E040FB"); textSize = 21f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    }
    fun update(deltaTime: Float) { idleAnim.update(deltaTime) }
    fun isPlayerNear(px: Float, py: Float): Boolean = sqrt((px - (x + width / 2)) * (px - (x + width / 2)) + (py - (y + height / 2)) * (py - (y + height / 2))) < 125f
    fun draw(canvas: Canvas) {
        shadowRect.set(x + 18f, y + height - 16f, x + width - 18f, y + height)
        canvas.drawOval(shadowRect, shadowPaint)
        idleAnim.draw(canvas, x, y, width, height)
        canvas.drawText("تاجر الكيمياء (الإخفاء)", x + width / 2, y - 10f, textPaint)
    }
}

class BlacksmithNPC(context: Context, var x: Float, var y: Float) {
    val width = 96f
    val height = 96f
    val hitbox = RectF(x + 18f, y + 20f, x + width - 18f, y + height - 8f)
    private val idleAnim = SpriteSheet(context, "sprites/guard_elite/idle.png", 4)
    private val shadowRect = RectF()
    companion object {
        private val shadowPaint = Paint().apply { color = Color.argb(100, 0, 0, 0); style = Paint.Style.FILL }
        private val textPaint = Paint().apply { color = Color.parseColor("#FF8A65"); textSize = 21f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    }
    fun update(deltaTime: Float) { idleAnim.update(deltaTime) }
    fun isPlayerNear(px: Float, py: Float): Boolean = sqrt((px - (x + width / 2)) * (px - (x + width / 2)) + (py - (y + height / 2)) * (py - (y + height / 2))) < 125f
    fun draw(canvas: Canvas) {
        shadowRect.set(x + 18f, y + height - 16f, x + width - 18f, y + height)
        canvas.drawOval(shadowRect, shadowPaint)
        idleAnim.draw(canvas, x, y, width, height)
        canvas.drawText("الحداد (تطوير الأسلحة)", x + width / 2, y - 10f, textPaint)
    }
}

class SageNPC(context: Context, var x: Float, var y: Float) {
    val width = 96f
    val height = 96f
    val hitbox = RectF(x + 18f, y + 20f, x + width - 18f, y + height - 8f)
    private val idleAnim = SpriteSheet(context, "sprites/chronomancer/idle.png", 4)
    private val shadowRect = RectF()
    companion object {
        private val shadowPaint = Paint().apply { color = Color.argb(100, 0, 0, 0); style = Paint.Style.FILL }
        private val textPaint = Paint().apply { color = Color.parseColor("#64FFDA"); textSize = 21f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    }
    fun update(deltaTime: Float) { idleAnim.update(deltaTime) }
    fun isPlayerNear(px: Float, py: Float): Boolean = sqrt((px - (x + width / 2)) * (px - (x + width / 2)) + (py - (y + height / 2)) * (py - (y + height / 2))) < 125f
    fun draw(canvas: Canvas) {
        shadowRect.set(x + 18f, y + height - 16f, x + width - 18f, y + height)
        canvas.drawOval(shadowRect, shadowPaint)
        idleAnim.draw(canvas, x, y, width, height)
        canvas.drawText("الحكيم (المنقذ)", x + width / 2, y - 10f, textPaint)
    }
}

class QuestGiverNPC(context: Context, var x: Float, var y: Float, val npcName: String, val questId: String) {
    val width = 96f
    val height = 96f
    val hitbox = RectF(x + 18f, y + 20f, x + width - 18f, y + height - 8f)
    private val idleAnim = SpriteSheet(context, "sprites/merchant/idle.png", 4)
    private val shadowRect = RectF()
    companion object {
        private val shadowPaint = Paint().apply { color = Color.argb(100, 0, 0, 0); style = Paint.Style.FILL }
        private val textPaint = Paint().apply { color = Color.parseColor("#81C784"); textSize = 20f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    }
    fun update(deltaTime: Float) { idleAnim.update(deltaTime) }
    fun isPlayerNear(px: Float, py: Float): Boolean = sqrt((px - (x + width / 2)) * (px - (x + width / 2)) + (py - (y + height / 2)) * (py - (y + height / 2))) < 125f
    fun draw(canvas: Canvas) {
        shadowRect.set(x + 18f, y + height - 16f, x + width - 18f, y + height)
        canvas.drawOval(shadowRect, shadowPaint)
        idleAnim.draw(canvas, x, y, width, height)
        canvas.drawText("$npcName (!)", x + width / 2, y - 10f, textPaint)
    }
}
