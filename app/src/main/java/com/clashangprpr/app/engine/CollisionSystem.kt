package com.clashangprpr.app.engine

import android.graphics.RectF

object CollisionSystem {
    private val testRect = RectF()

    fun moveWithCollision(
        x: Float,
        y: Float,
        dx: Float,
        dy: Float,
        hitboxOffsetX: Float,
        hitboxOffsetY: Float,
        hitboxW: Float,
        hitboxH: Float,
        totalW: Float,
        totalH: Float,
        obstacles: List<RectF>,
        mapW: Float,
        mapH: Float,
        margin: Float = 54f
    ): Pair<Float, Float> {
        var newX = (x + dx).coerceIn(margin, (mapW - totalW - margin).coerceAtLeast(margin))
        testRect.set(
            newX + hitboxOffsetX,
            y + hitboxOffsetY,
            newX + hitboxOffsetX + hitboxW,
            y + hitboxOffsetY + hitboxH
        )
        for (i in obstacles.indices) {
            if (RectF.intersects(testRect, obstacles[i])) {
                newX = x
                break
            }
        }

        var newY = (y + dy).coerceIn(margin, (mapH - totalH - margin).coerceAtLeast(margin))
        testRect.set(
            newX + hitboxOffsetX,
            newY + hitboxOffsetY,
            newX + hitboxOffsetX + hitboxW,
            newY + hitboxOffsetY + hitboxH
        )
        for (i in obstacles.indices) {
            if (RectF.intersects(testRect, obstacles[i])) {
                newY = y
                break
            }
        }

        return Pair(newX, newY)
    }

    fun isPointBlocked(px: Float, py: Float, obstacles: List<RectF>, mapW: Float, mapH: Float): Boolean {
        if (px < 48f || py < 48f || px > mapW - 48f || py > mapH - 48f) return true
        for (i in obstacles.indices) {
            if (obstacles[i].contains(px, py)) return true
        }
        return false
    }
}
