package com.clashangprpr.app.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
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
    private val fallbackPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.FILL
    }

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
        val bmp = bitmap
        if (bmp == null || bmp.width == 0 || bmp.height == 0) {
            // Draw safe fallback rectangle if bitmap fails to load
            canvas.drawRect(destX, destY, destX + destWidth, destY + destHeight, fallbackPaint)
            return
        }

        val actualFrameW = if (bmp.width >= (currentFrame + 1) * frameWidth && frameWidth > 0) {
            frameWidth
        } else {
            (bmp.width / frameCount.coerceAtLeast(1)).coerceAtLeast(1)
        }
        val actualFrameH = if (bmp.height >= frameHeight && frameHeight > 0) {
            frameHeight
        } else {
            bmp.height.coerceAtLeast(1)
        }

        val srcLeft = (currentFrame * actualFrameW).coerceIn(0, bmp.width - 1)
        val srcRight = (srcLeft + actualFrameW).coerceIn(1, bmp.width)
        val srcBottom = actualFrameH.coerceIn(1, bmp.height)

        val srcRect = Rect(srcLeft, 0, srcRight, srcBottom)

        matrix.reset()
        val scaleX = destWidth / actualFrameW.toFloat()
        val scaleY = destHeight / actualFrameH.toFloat()

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
        canvas.drawBitmap(bmp, -srcLeft.toFloat(), 0f, paint)
        canvas.restore()
    }
}
