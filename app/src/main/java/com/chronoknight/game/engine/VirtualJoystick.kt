package com.chronoknight.game.engine

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class VirtualJoystick(
    private val baseX: Float,
    private val baseY: Float,
    private val baseRadius: Float = 140f,
    private val stickRadius: Float = 60f
) {
    var isPressed = false
        private set
    var pointerId = -1
        private set

    var deltaX = 0f
        private set
    var deltaY = 0f
        private set

    private var currentX = baseX
    private var currentY = baseY

    private val basePaint = Paint().apply {
        color = Color.argb(100, 255, 255, 255)
        style = Paint.Style.FILL
    }
    private val baseStroke = Paint().apply {
        color = Color.argb(180, 255, 215, 0)
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    private val stickPaint = Paint().apply {
        color = Color.argb(200, 255, 215, 0)
        style = Paint.Style.FILL
    }

    fun onTouchDown(id: Int, x: Float, y: Float): Boolean {
        val dist = sqrt((x - baseX) * (x - baseX) + (y - baseY) * (y - baseY))
        if (dist <= baseRadius * 1.5f && !isPressed) {
            isPressed = true
            pointerId = id
            updatePosition(x, y)
            return true
        }
        return false
    }

    fun onTouchMove(id: Int, x: Float, y: Float) {
        if (isPressed && pointerId == id) {
            updatePosition(x, y)
        }
    }

    fun onTouchUp(id: Int) {
        if (isPressed && pointerId == id) {
            isPressed = false
            pointerId = -1
            currentX = baseX
            currentY = baseY
            deltaX = 0f
            deltaY = 0f
        }
    }

    private fun updatePosition(x: Float, y: Float) {
        val dx = x - baseX
        val dy = y - baseY
        val dist = sqrt(dx * dx + dy * dy)
        if (dist <= baseRadius) {
            currentX = x
            currentY = y
            deltaX = dx / baseRadius
            deltaY = dy / baseRadius
        } else {
            val angle = atan2(dy, dx)
            currentX = baseX + cos(angle) * baseRadius
            currentY = baseY + sin(angle) * baseRadius
            deltaX = cos(angle)
            deltaY = sin(angle)
        }
    }

    fun draw(canvas: Canvas) {
        canvas.drawCircle(baseX, baseY, baseRadius, basePaint)
        canvas.drawCircle(baseX, baseY, baseRadius, baseStroke)
        canvas.drawCircle(currentX, currentY, stickRadius, stickPaint)
    }
}
