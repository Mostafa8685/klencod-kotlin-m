package com.clashangprpr.app.world

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.clashangprpr.app.model.Enemy
import com.clashangprpr.app.model.EraType
import com.clashangprpr.app.model.HubGate
import com.clashangprpr.app.model.MerchantNPC
import com.clashangprpr.app.model.Player
import kotlin.math.sin
import kotlin.random.Random

class WorldEnvironmentSystem {
    var timeOfDay: Float = 0.25f // 0..1 (0.25 = noon, 0.75 = midnight)
    var isRaining: Boolean = false
    var showMinimap: Boolean = true

    private class RainDrop(var x: Float, var y: Float, var speed: Float)
    private val raindrops = List(55) {
        RainDrop(Random.nextFloat() * 1600f, Random.nextFloat() * 900f, 550f + Random.nextFloat() * 250f)
    }

    private val rainPaint = Paint().apply {
        color = Color.argb(120, 140, 200, 255)
        strokeWidth = 2.5f
    }
    private val tintPaint = Paint().apply { style = Paint.Style.FILL }
    private val caveDarkPaint = Paint().apply { style = Paint.Style.FILL }

    // Minimap Paints
    private val miniBgPaint = Paint().apply {
        color = Color.argb(180, 12, 16, 26)
        style = Paint.Style.FILL
    }
    private val miniBorderPaint = Paint().apply {
        color = Color.parseColor("#FFD700")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val miniDotPaint = Paint().apply { style = Paint.Style.FILL }
    private val miniRect = RectF()

    fun onEraChanged(era: EraType) {
        isRaining = (era == EraType.ERA_4_ISLAND || era == EraType.ERA_3_ZOMBIE)
        timeOfDay = if (era == EraType.ERA_3_ZOMBIE) 0.55f else 0.25f
    }

    fun update(deltaTime: Float, era: EraType, screenW: Float, screenH: Float) {
        if (era == EraType.ERA_3_ZOMBIE || era == EraType.ERA_4_ISLAND) {
            timeOfDay = (timeOfDay + deltaTime * 0.012f) % 1.0f
        }
        if (isRaining && screenW > 10f && screenH > 10f) {
            for (i in raindrops.indices) {
                val r = raindrops[i]
                r.x -= 120f * deltaTime
                r.y += r.speed * deltaTime
                if (r.y > screenH) {
                    r.y = -10f
                    r.x = Random.nextFloat() * (screenW + 200f)
                }
            }
        }
    }

    fun isNight(): Boolean = timeOfDay > 0.62f || timeOfDay < 0.12f

    fun drawAtmosphereOverlay(
        canvas: Canvas,
        era: EraType,
        screenW: Float,
        screenH: Float,
        playerScreenX: Float,
        playerScreenY: Float
    ) {
        // Weather: Rain
        if (isRaining) {
            for (i in raindrops.indices) {
                val r = raindrops[i]
                canvas.drawLine(r.x, r.y, r.x - 8f, r.y + 18f, rainPaint)
            }
        }

        // Cave Dynamic Lighting (Era 2): darkness around edges with warm torchlight around player
        if (era == EraType.ERA_2_CAVES) {
            val radius = (screenH * 0.58f).coerceAtLeast(280f)
            caveDarkPaint.shader = RadialGradient(
                playerScreenX,
                playerScreenY,
                radius,
                intArrayOf(
                    Color.argb(0, 0, 0, 0),
                    Color.argb(95, 8, 10, 18),
                    Color.argb(215, 6, 8, 14)
                ),
                floatArrayOf(0.0f, 0.55f, 1.0f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, screenW, screenH, caveDarkPaint)
            caveDarkPaint.shader = null
        } else if (era == EraType.ERA_3_ZOMBIE || era == EraType.ERA_4_ISLAND) {
            // Day/Night smooth atmospheric tint
            val nightFactor = ((sin((timeOfDay - 0.5f) * Math.PI * 2.0) + 1.0) * 0.5).toFloat()
            val alpha = (nightFactor * 115f).toInt().coerceIn(0, 125)
            if (alpha > 10) {
                tintPaint.color = Color.argb(alpha, 10, 15, 42)
                canvas.drawRect(0f, 0f, screenW, screenH, tintPaint)
            }
        }
    }

    fun drawMinimap(
        canvas: Canvas,
        screenW: Float,
        era: EraType,
        player: Player,
        enemies: List<Enemy>,
        merchant: MerchantNPC?,
        hubGates: List<HubGate>,
        portalX: Float,
        portalY: Float,
        portalActive: Boolean
    ) {
        if (!showMinimap) return
        val mapW = era.mapWidth.coerceAtLeast(1f)
        val mapH = era.mapHeight.coerceAtLeast(1f)
        val w = 156f
        val h = (w * (mapH / mapW)).coerceIn(85f, 120f)
        val left = screenW - w - 22f
        val top = 118f

        miniRect.set(left, top, left + w, top + h)
        canvas.drawRoundRect(miniRect, 10f, 10f, miniBgPaint)
        canvas.drawRoundRect(miniRect, 10f, 10f, miniBorderPaint)

        // Merchant dot (Yellow)
        if (merchant != null) {
            miniDotPaint.color = Color.YELLOW
            val mx = left + (merchant.x / mapW) * w
            val my = top + (merchant.y / mapH) * h
            canvas.drawCircle(mx, my, 4f, miniDotPaint)
        }

        // Hub gates (Cyan / Green)
        for (i in hubGates.indices) {
            val g = hubGates[i]
            miniDotPaint.color = if (g.isCompleted) Color.GREEN else Color.CYAN
            val gx = left + (g.x / mapW) * w
            val gy = top + (g.y / mapH) * h
            canvas.drawCircle(gx, gy, 4.5f, miniDotPaint)
        }

        // Portal dot
        if (portalActive) {
            miniDotPaint.color = Color.CYAN
            val px = left + (portalX / mapW) * w
            val py = top + (portalY / mapH) * h
            canvas.drawCircle(px, py, 5f, miniDotPaint)
        }

        // Enemies (Red)
        miniDotPaint.color = Color.RED
        for (i in enemies.indices) {
            val e = enemies[i]
            if (e.isDead) continue
            val ex = left + (e.x / mapW) * w
            val ey = top + (e.y / mapH) * h
            canvas.drawCircle(ex, ey, if (e.isBoss) 5f else 3f, miniDotPaint)
        }

        // Player (Bright White/Green)
        miniDotPaint.color = Color.parseColor("#76FF03")
        val plX = left + ((player.x + player.width / 2) / mapW) * w
        val plY = top + ((player.y + player.height / 2) / mapH) * h
        canvas.drawCircle(plX, plY, 4.5f, miniDotPaint)
    }
}
