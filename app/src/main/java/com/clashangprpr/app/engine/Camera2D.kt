package com.clashangprpr.app.engine

class Camera2D(
    var viewWidth: Float,
    var viewHeight: Float,
    var mapWidth: Float,
    var mapHeight: Float
) {
    var x: Float = 0f
    var y: Float = 0f

    fun update(targetX: Float, targetY: Float) {
        if (viewWidth <= 0f || viewHeight <= 0f) return

        val desiredX = targetX - viewWidth / 2f
        val desiredY = targetY - viewHeight / 2f

        x += (desiredX - x) * 0.15f
        y += (desiredY - y) * 0.15f

        if (x < 0) x = 0f
        if (y < 0) y = 0f
        if (x > mapWidth - viewWidth) x = (mapWidth - viewWidth).coerceAtLeast(0f)
        if (y > mapHeight - viewHeight) y = (mapHeight - viewHeight).coerceAtLeast(0f)
    }
}
