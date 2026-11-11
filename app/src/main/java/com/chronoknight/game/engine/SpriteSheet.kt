package com.chronoknight.game.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect

class SpriteSheet(
    context: Context,
    assetPath: String,
    private val frameCount: Int,
    private val frameWidth: Int = 32,
    private val frameHeight: Int = 32,
    private val frameDuration: Float = 0.12f
) {
    private var bitmap: Bitmap? = null
    private var currentFrame = 0
    private var timer = 0f
    private val matrix = Matrix()

    init {
        try {
            context.assets.open(assetPath).use { input ->
                bitmap = BitmapFactory.decodeStream(input)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun update(deltaTime: Float) {
        if (frameCount <= 1) return
        timer += deltaTime
        if (timer >= frameDuration) {
            timer = 0f
            currentFrame = (currentFrame + 1) % frameCount
        }
    }

    fun reset() {
        currentFrame = 0
        timer = 0f
    }

    val isFinished: Boolean
        get() = currentFrame == frameCount - 1

    fun draw(
        canvas: Canvas,
        destX: Float,
        destY: Float,
        destWidth: Float,
        destHeight: Float,
        flipX: Boolean = false,
        paint: Paint? = null
    ) {
        val bmp = bitmap ?: return

        val actualFrameW = if (bmp.width >= (currentFrame + 1) * frameWidth) frameWidth else bmp.width / frameCount
        val actualFrameH = if (bmp.height >= frameHeight) frameHeight else bmp.height

        val srcRect = Rect(
            currentFrame * actualFrameW,
            0,
            (currentFrame + 1) * actualFrameW,
            actualFrameH
        )

        matrix.reset()
        val scaleX = destWidth / actualFrameW
        val scaleY = destHeight / actualFrameH

        if (flipX) {
            matrix.postScale(-scaleX, scaleY)
            matrix.postTranslate(destX + destWidth, destY)
        } else {
            matrix.postScale(scaleX, scaleY)
            matrix.postTranslate(destX, destY)
        }

        canvas.save()
        canvas.clipRect(destX, destY, destX + destWidth, destY + destHeight)
        canvas.concat(matrix)
        canvas.drawBitmap(bmp, -srcRect.left.toFloat(), -srcRect.top.toFloat(), paint)
        canvas.restore()
    }
}
