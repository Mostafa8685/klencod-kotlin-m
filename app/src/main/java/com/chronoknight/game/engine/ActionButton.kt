package com.chronoknight.game.engine

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.sqrt

class ActionButton(
    val x: Float,
    val y: Float,
    val radius: Float,
    val label: String,
    val colorPrimary: Int
) {
    var isPressed = false
        private set
    var pointerId = -1
        private set

    private val bgPaint = Paint().apply {
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 34f
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

        canvas.drawCircle(x, y, radius, bgPaint)
        canvas.drawCircle(x, y, radius, strokePaint)
        canvas.drawText(label, x, y + 12f, textPaint)
    }
}
