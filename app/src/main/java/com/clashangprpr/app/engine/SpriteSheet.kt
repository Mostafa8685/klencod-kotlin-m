package com.clashangprpr.app.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF

/**
 * Horizontal strip of square frames. Frame count is derived from the image itself
 * (width / height) so it can never get out of sync with the asset again.
 */
class SpriteSheet(
    private val bitmap: Bitmap?,
    private val frameDuration: Float = 0.12f,
    private val loop: Boolean = true
) {
    constructor(path: String, frameDuration: Float = 0.12f, loop: Boolean = true) :
        this(Assets.bmp(path), frameDuration, loop)

    private val frameH: Int = bitmap?.height ?: 32
    private val frameW: Int = if (frameH > 0) frameH else 32
    val frameCount: Int = if (bitmap != null && frameW > 0) maxOf(1, bitmap.width / frameW) else 1
    var current = 0
        private set
    private var timer = 0f
    private val src = Rect()
    private val dst = RectF()

    val isFinished: Boolean
        get() = !loop && current >= frameCount - 1

    val hasBitmap: Boolean
        get() = bitmap != null

    fun update(dt: Float) {
        if (frameCount <= 1) return
        timer += dt
        while (timer >= frameDuration) {
            timer -= frameDuration
            if (current < frameCount - 1) current++
            else if (loop) current = 0
        }
    }

    fun reset() {
        current = 0
        timer = 0f
    }

    fun setFrame(i: Int) {
        current = i.coerceIn(0, frameCount - 1)
    }

    fun draw(c: Canvas, x: Float, y: Float, w: Float, h: Float, flip: Boolean = false, paint: Paint? = null) {
        val b = bitmap ?: return
        src.set(current * frameW, 0, current * frameW + frameW, frameH)
        dst.set(x, y, x + w, y + h)
        if (flip) {
            c.save()
            c.scale(-1f, 1f, x + w / 2f, y)
            c.drawBitmap(b, src, dst, paint)
            c.restore()
        } else {
            c.drawBitmap(b, src, dst, paint)
        }
    }
}
