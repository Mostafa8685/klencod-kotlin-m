package com.clashangprpr.app.model

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.sqrt

class TimePortal(
    var x: Float,
    var y: Float
) {
    val radius = 70f
    var isActive: Boolean = false
    private var glowAngle = 0f

    private val portalPaint = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }
    private val innerPaint = Paint().apply {
        color = Color.argb(160, 0, 255, 255)
        style = Paint.Style.FILL
    }

    fun update(deltaTime: Float) {
        glowAngle += deltaTime * 5f
    }

    fun draw(canvas: Canvas) {
        if (!isActive) return
        val currentRadius = radius + kotlin.math.sin(glowAngle) * 8f
        canvas.drawCircle(x, y, currentRadius, innerPaint)
        canvas.drawCircle(x, y, currentRadius, portalPaint)

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 28f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText("بوابة الزمن", x, y - radius - 15f, textPaint)
    }

    fun isPlayerInside(px: Float, py: Float): Boolean {
        if (!isActive) return false
        val dist = sqrt((px - x) * (px - x) + (py - y) * (py - y))
        return dist <= radius + 20f
    }
}
