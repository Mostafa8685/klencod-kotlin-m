package com.clashangprpr.app.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.sqrt

class DungeonTrap(
    context: Context,
    var x: Float,
    var y: Float,
    val isSaw: Boolean = true
) {
    val width = 64f
    val height = 64f
    val hitbox = RectF(x + 10f, y + 10f, x + width - 10f, y + height - 10f)
    private var bitmap: Bitmap? = null
    private var frameTimer = 0f
    private var currentFrame = 0
    private val totalFrames = if (isSaw) 4 else 7
    private var damageCooldown = 0f

    init {
        try {
            val assetName = if (isSaw) "environment/trap_saw.png" else "environment/trap_fire.png"
            context.assets.open(assetName).use { input ->
                bitmap = BitmapFactory.decodeStream(input)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun update(deltaTime: Float, player: Player) {
        frameTimer += deltaTime
        if (frameTimer > 0.12f) {
            frameTimer = 0f
            currentFrame = (currentFrame + 1) % totalFrames
        }

        if (damageCooldown > 0) damageCooldown -= deltaTime

        if (damageCooldown <= 0f && RectF.intersects(hitbox, player.hitbox)) {
            player.takeDamage(22)
            damageCooldown = 1.0f
        }
    }

    fun draw(canvas: Canvas) {
        val bmp = bitmap ?: return
        val frameW = bmp.width / totalFrames
        val frameH = bmp.height
        val src = Rect(currentFrame * frameW, 0, (currentFrame + 1) * frameW, frameH)
        val dst = RectF(x, y, x + width, y + height)
        canvas.drawBitmap(bmp, src, dst, null)
    }
}
