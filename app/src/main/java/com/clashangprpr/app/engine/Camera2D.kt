package com.clashangprpr.app.engine

class Camera2D(var mapW: Float, var mapH: Float) {
    var x = 0f
    var y = 0f
    /** visible area in WORLD units (screen size / zoom) */
    var viewW = 1280f
    var viewH = 720f

    private fun clampAxis(v: Float, mapSize: Float, view: Float): Float {
        if (mapSize <= view) return (mapSize - view) / 2f
        return v.coerceIn(0f, mapSize - view)
    }

    fun snapTo(tx: Float, ty: Float) {
        x = clampAxis(tx - viewW / 2f, mapW, viewW)
        y = clampAxis(ty - viewH / 2f, mapH, viewH)
    }

    fun update(tx: Float, ty: Float, dt: Float) {
        val k = minOf(1f, 7f * dt)
        val dx = tx - viewW / 2f
        val dy = ty - viewH / 2f
        x += (dx - x) * k
        y += (dy - y) * k
        x = clampAxis(x, mapW, viewW)
        y = clampAxis(y, mapH, viewH)
    }
}
