package com.clashangprpr.app.engine

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.ceil
import kotlin.math.sqrt

class ActionButton(
    var x: Float,
    var y: Float,
    var radius: Float,
    var label: String,
    var colorPrimary: Int
) {
    var isPressed = false
        private set
    var pointerId = -1
        private set

    var cooldownRatio: Float = 0f      // 0f = ready, 1f = full cooldown
    var cooldownSeconds: Float = 0f

    private val arcRect = RectF()
    private val bgPaint = Paint().apply { style = Paint.Style.FILL }
    private val cdOverlayPaint = Paint().apply {
        color = Color.argb(170, 10, 12, 18)
        style = Paint.Style.FILL
    }
    private val cdArcPaint = Paint().apply {
        color = Color.parseColor("#FFD700")
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    private val strokePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 26f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val cdTextPaint = Paint().apply {
        color = Color.parseColor("#FFD700")
        textSize = 28f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    fun onTouchDown(id: Int, touchX: Float, touchY: Float): Boolean {
        val dist = sqrt((touchX - x) * (touchX - x) + (touchY - y) * (touchY - y))
        if (dist <= radius && !isPressed) {
            isPressed = true
            pointerId = id
            return true
        }
        return false
    }

    fun onTouchUp(id: Int): Boolean {
        if (isPressed && pointerId == id) {
            isPressed = false
            pointerId = -1
            return true
        }
        return false
    }

    fun draw(canvas: Canvas) {
        bgPaint.color = if (isPressed) Color.WHITE else colorPrimary
        textPaint.color = if (isPressed) Color.BLACK else Color.WHITE
        textPaint.textSize = (radius * 0.44f).coerceIn(20f, 32f)

        canvas.drawCircle(x, y, radius, bgPaint)
        canvas.drawCircle(x, y, radius, strokePaint)

        if (cooldownRatio > 0.01f) {
            canvas.drawCircle(x, y, radius, cdOverlayPaint)
            arcRect.set(x - radius + 3f, y - radius + 3f, x + radius - 3f, y + radius - 3f)
            canvas.drawArc(arcRect, -90f, 360f * cooldownRatio.coerceIn(0f, 1f), false, cdArcPaint)
            val secText = "${ceil(cooldownSeconds.toDouble()).toInt()}s"
            canvas.drawText(secText, x, y + 10f, cdTextPaint)
        } else {
            canvas.drawText(label, x, y + 9f, textPaint)
        }
    }
}
