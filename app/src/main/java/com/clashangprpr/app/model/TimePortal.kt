package com.clashangprpr.app.model

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.sin
import kotlin.math.sqrt

class TimePortal(var x: Float, var y: Float) {
    var isActive = false
    private var pulse = 0f

    companion object {
        private val outerPaint = Paint().apply { style = Paint.Style.FILL }
        private val innerPaint = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 5f }
        private val labelPaint = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
    }

    fun update(deltaTime: Float) {
        pulse += deltaTime * 4f
    }

    fun isPlayerInside(px: Float, py: Float): Boolean {
        val dist = sqrt((px - x) * (px - x) + (py - y) * (py - y))
        return dist < 95f
    }

    fun draw(canvas: Canvas) {
        val r = 52f + sin(pulse) * 6f
        outerPaint.color = if (isActive) Color.argb(190, 0, 229, 255) else Color.argb(120, 80, 80, 100)
        canvas.drawCircle(x, y, r, outerPaint)

        innerPaint.color = if (isActive) Color.WHITE else Color.GRAY
        canvas.drawCircle(x, y, r * 0.72f, innerPaint)

        val msg = if (isActive) "بوابة العودة / العبور" else "البوابة مقفلة"
        canvas.drawText(msg, x, y - r - 12f, labelPaint)
    }
}

class HubGate(
    val x: Float,
    val y: Float,
    val targetEra: EraType,
    val shortTitle: String,
    var isCompleted: Boolean = false
) {
    private var pulse = 0f

    companion object {
        private val fillPaint = Paint().apply { style = Paint.Style.FILL }
        private val ringPaint = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 5f }
        private val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        private val statusPaint = Paint().apply {
            textSize = 16f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
    }

    fun update(deltaTime: Float) {
        pulse += deltaTime * 3.5f
    }

    fun isPlayerNear(px: Float, py: Float): Boolean {
        val dist = sqrt((px - x) * (px - x) + (py - y) * (py - y))
        return dist < 105f
    }

    fun draw(canvas: Canvas) {
        val r = 54f + sin(pulse) * 4f
        fillPaint.color = when {
            isCompleted -> Color.argb(190, 46, 125, 50)
            targetEra == EraType.ERA_6_VOID -> Color.argb(200, 142, 36, 170)
            else -> Color.argb(180, 2, 136, 209)
        }
        canvas.drawCircle(x, y, r, fillPaint)

        ringPaint.color = when {
            isCompleted -> Color.parseColor("#81C784")
            targetEra == EraType.ERA_6_VOID -> Color.parseColor("#E040FB")
            else -> Color.parseColor("#FFD700")
        }
        canvas.drawCircle(x, y, r * 0.78f, ringPaint)

        canvas.drawText(shortTitle, x, y - r - 16f, titlePaint)
        statusPaint.color = if (isCompleted) Color.parseColor("#A5D6A7") else Color.parseColor("#FFD54F")
        canvas.drawText(if (isCompleted) "مكتمل (إعادة)" else "دخول البوابة", x, y + r + 24f, statusPaint)
    }
}
