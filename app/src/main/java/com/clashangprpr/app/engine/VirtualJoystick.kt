package com.clashangprpr.app.engine

import android.graphics.Canvas
import android.graphics.Color
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Floating or custom-placed joystick supporting HUD customization (position, scale, opacity). */
class VirtualJoystick {
    var homeX = 0f
    var homeY = 0f
    var baseRadius = 78f
    var customScale = 1.0f
    var customAlpha = 0.90f
    var customXFrac = -1f
    var customYFrac = -1f

    val radius: Float get() = baseRadius * customScale
    var zoneRight = 0f
    var zoneTop = 0f

    var active = false
        private set
    var pointerId = -1
        private set
    var dx = 0f
        private set
    var dy = 0f
        private set

    private var baseX = 0f
    private var baseY = 0f
    private var curX = 0f
    private var curY = 0f

    fun layout(w: Float, h: Float, u: Float) {
        baseRadius = 78f * u
        customXFrac = SaveStore.float("hud_joy_x", -1f)
        customYFrac = SaveStore.float("hud_joy_y", -1f)
        customScale = SaveStore.float("hud_joy_s", 1.0f).coerceIn(0.65f, 1.55f)
        customAlpha = SaveStore.float("hud_joy_a", 0.90f).coerceIn(0.35f, 1.0f)

        if (customXFrac in 0.04f..0.95f && customYFrac in 0.06f..0.94f) {
            homeX = customXFrac * w
            homeY = customYFrac * h
        } else {
            homeX = 150f * u
            homeY = h - 150f * u
        }
        zoneRight = w * 0.46f
        zoneTop = h * 0.20f
        if (!active) {
            baseX = homeX
            baseY = homeY
            curX = homeX
            curY = homeY
        }
    }

    fun saveCustom(w: Float, h: Float) {
        if (w <= 0f || h <= 0f) return
        customXFrac = (homeX / w).coerceIn(0.05f, 0.95f)
        customYFrac = (homeY / h).coerceIn(0.08f, 0.92f)
        baseX = homeX
        baseY = homeY
        curX = homeX
        curY = homeY
        SaveStore.putAll(
            mapOf(
                "hud_joy_x" to customXFrac,
                "hud_joy_y" to customYFrac,
                "hud_joy_s" to customScale,
                "hud_joy_a" to customAlpha
            ),
            false
        )
    }

    fun resetCustom() {
        customXFrac = -1f
        customYFrac = -1f
        customScale = 1.0f
        customAlpha = 0.90f
        SaveStore.putAll(
            mapOf(
                "hud_joy_x" to -1f,
                "hud_joy_y" to -1f,
                "hud_joy_s" to 1.0f,
                "hud_joy_a" to 0.90f
            ),
            false
        )
    }

    fun hitTestHome(x: Float, y: Float): Boolean {
        val ox = x - homeX
        val oy = y - homeY
        return sqrt(ox * ox + oy * oy) <= radius * 1.15f
    }

    fun onDown(id: Int, x: Float, y: Float): Boolean {
        if (active) return false
        val nearHome = hitTestHome(x, y)
        if (!nearHome && (x > zoneRight || y < zoneTop)) return false
        active = true
        pointerId = id
        baseX = x
        baseY = y
        curX = x
        curY = y
        dx = 0f
        dy = 0f
        return true
    }

    fun onMove(id: Int, x: Float, y: Float) {
        if (!active || id != pointerId) return
        val ox = x - baseX
        val oy = y - baseY
        val dist = sqrt(ox * ox + oy * oy)
        if (dist < 0.001f) {
            dx = 0f
            dy = 0f
            curX = baseX
            curY = baseY
            return
        }
        val a = atan2(oy, ox)
        val d = min(dist, radius)
        curX = baseX + cos(a) * d
        curY = baseY + sin(a) * d
        val mag = d / radius
        if (mag < 0.16f) {
            dx = 0f
            dy = 0f
        } else {
            dx = cos(a) * mag
            dy = sin(a) * mag
        }
    }

    fun onUp(id: Int) {
        if (active && id == pointerId) cancel()
    }

    fun cancel() {
        active = false
        pointerId = -1
        dx = 0f
        dy = 0f
        baseX = homeX
        baseY = homeY
        curX = homeX
        curY = homeY
    }

    fun draw(c: Canvas, selectedInEditor: Boolean = false) {
        val u = Ui.u
        val a = (if (active || selectedInEditor) 1f else 0.62f) * customAlpha
        val bx = if (selectedInEditor) homeX else baseX
        val by = if (selectedInEditor) homeY else baseY
        val cx = if (selectedInEditor) homeX else curX
        val cy = if (selectedInEditor) homeY else curY
        Ui.circle(c, bx, by, radius, Color.argb((75 * a).toInt(), 255, 255, 255))
        Ui.ring(c, bx, by, radius, if (selectedInEditor) Color.parseColor("#00E5FF") else Color.argb((210 * a).toInt(), 255, 215, 0), if (selectedInEditor) 5.5f * u else 4f * u)
        Ui.ring(c, bx, by, radius * 0.35f, Color.argb((95 * a).toInt(), 255, 255, 255), 2f * u)
        Ui.circle(c, cx, cy, radius * 0.42f, Color.argb((225 * a).toInt(), 255, 205, 40))
        Ui.ring(c, cx, cy, radius * 0.42f, Color.argb((255 * a).toInt(), 120, 80, 0), 3f * u)
    }
}
