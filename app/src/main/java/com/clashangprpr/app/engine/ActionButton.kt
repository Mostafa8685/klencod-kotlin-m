package com.clashangprpr.app.engine

import android.graphics.Canvas
import android.graphics.Color
import kotlin.math.sqrt

/**
 * On-screen action button supporting Fortnite/PUBG-style custom HUD placement,
 * custom scaling (0.65x..1.55x), custom opacity, cooldown sweep, and count badge.
 */
class ActionButton(
    var iconPath: String,
    var label: String,
    var accent: Int,
    var tintIcon: Int? = null,
    val hudKey: String = ""
) {
    var cx = 0f
    var cy = 0f
    var baseR = 40f
    var customScale = 1.0f
    var customAlpha = 0.92f
    var customXFrac = -1f
    var customYFrac = -1f

    val r: Float get() = baseR * customScale
    var visible = true
    var enabled = true
    /** remaining fraction 0..1 of the cooldown (0 = ready) */
    var cooldown = 0f
    var badge: String? = null
    var isPressed = false
        private set
    var pointerId = -1
        private set

    fun placeDefaultOrCustom(w: Float, h: Float, u: Float, defDx: Float, defDy: Float, defR: Float) {
        baseR = defR * u
        if (hudKey.isNotEmpty()) {
            customXFrac = SaveStore.float("hud_${hudKey}_x", -1f)
            customYFrac = SaveStore.float("hud_${hudKey}_y", -1f)
            customScale = SaveStore.float("hud_${hudKey}_s", 1.0f).coerceIn(0.65f, 1.55f)
            customAlpha = SaveStore.float("hud_${hudKey}_a", 0.92f).coerceIn(0.35f, 1.0f)
        }
        if (customXFrac in 0.03f..0.97f && customYFrac in 0.05f..0.95f) {
            cx = customXFrac * w
            cy = customYFrac * h
        } else {
            cx = w - defDx * u
            cy = h - defDy * u
        }
    }

    fun saveCustom(w: Float, h: Float) {
        if (hudKey.isEmpty() || w <= 0f || h <= 0f) return
        customXFrac = (cx / w).coerceIn(0.04f, 0.96f)
        customYFrac = (cy / h).coerceIn(0.06f, 0.94f)
        SaveStore.putAll(
            mapOf(
                "hud_${hudKey}_x" to customXFrac,
                "hud_${hudKey}_y" to customYFrac,
                "hud_${hudKey}_s" to customScale,
                "hud_${hudKey}_a" to customAlpha
            ),
            false
        )
    }

    fun resetCustom() {
        customXFrac = -1f
        customYFrac = -1f
        customScale = 1.0f
        customAlpha = 0.92f
        if (hudKey.isNotEmpty()) {
            SaveStore.putAll(
                mapOf(
                    "hud_${hudKey}_x" to -1f,
                    "hud_${hudKey}_y" to -1f,
                    "hud_${hudKey}_s" to 1.0f,
                    "hud_${hudKey}_a" to 0.92f
                ),
                false
            )
        }
    }

    fun hitTest(x: Float, y: Float): Boolean {
        val dx = x - cx
        val dy = y - cy
        return sqrt(dx * dx + dy * dy) <= r * 1.18f
    }

    fun onDown(id: Int, x: Float, y: Float): Boolean {
        if (!visible || isPressed) return false
        if (hitTest(x, y)) {
            isPressed = true
            pointerId = id
            return true
        }
        return false
    }

    fun onUp(id: Int) {
        if (isPressed && pointerId == id) {
            isPressed = false
            pointerId = -1
        }
    }

    fun cancel() {
        isPressed = false
        pointerId = -1
    }

    fun draw(c: Canvas, forceVisible: Boolean = false, selectedInEditor: Boolean = false) {
        if (!visible && !forceVisible) return
        val u = Ui.u
        val rr = if (isPressed) r * 0.92f else r
        val baseDim = if (enabled || forceVisible) (255 * customAlpha).toInt().coerceIn(70, 255) else (120 * customAlpha).toInt().coerceIn(50, 180)
        Ui.circle(c, cx + 2f * u, cy + 4f * u, rr, Color.argb((90 * customAlpha).toInt(), 0, 0, 0))
        Ui.circle(c, cx, cy, rr, Color.argb((215 * customAlpha).toInt(), 20, 22, 30))
        Ui.circle(c, cx, cy, rr * 0.86f, Ui.withAlpha(accent, if (isPressed) 135 else (65 * customAlpha).toInt()))
        Ui.ring(c, cx, cy, rr - 2f * u, if (selectedInEditor) Color.parseColor("#FFD54F") else Ui.withAlpha(accent, baseDim), if (selectedInEditor) 5.5f * u else 3.8f * u)
        val iconSize = rr * 1.05f / u
        Ui.icon(c, iconPath, cx, cy - rr * 0.10f, iconSize, tintIcon, baseDim)
        if (label.isNotEmpty()) {
            Ui.text(c, label, cx, cy + rr * 0.72f, 11.2f * customScale.coerceIn(0.85f, 1.25f), Color.argb(baseDim, 255, 255, 255))
        }
        if (cooldown > 0.01f && !forceVisible) {
            Ui.pie(c, cx, cy, rr - 3f * u, 360f * cooldown.coerceIn(0f, 1f), Color.argb(165, 0, 0, 0))
        }
        val b = badge
        if (b != null) {
            val bx = cx + rr * 0.72f
            val by = cy - rr * 0.72f
            Ui.circle(c, bx, by, 13f * u, Color.argb(240, 14, 16, 24))
            Ui.ring(c, bx, by, 13f * u, accent, 2f * u)
            Ui.text(c, b, bx, by + 5f * u, 12.5f, Color.WHITE)
        }
    }
}
