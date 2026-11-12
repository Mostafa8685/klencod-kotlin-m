package com.clashangprpr.app.model

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.random.Random

class Particle(
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var size: Float = 6f,
    var color: Int = Color.WHITE,
    var life: Float = 0f,
    var maxLife: Float = 0.4f
)

class ParticleSystem(private val maxParticles: Int = 140) {
    private val particles = mutableListOf<Particle>()
    private val paint = Paint().apply { style = Paint.Style.FILL }

    fun emitBurst(x: Float, y: Float, count: Int, color: Int, speed: Float = 220f, size: Float = 7f, life: Float = 0.35f) {
        for (i in 0 until count) {
            if (particles.size >= maxParticles) {
                particles.removeAt(0)
            }
            val angle = Random.nextFloat() * Math.PI * 2.0
            val spd = speed * (0.4f + Random.nextFloat() * 0.8f)
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = (kotlin.math.cos(angle) * spd).toFloat(),
                    vy = (kotlin.math.sin(angle) * spd).toFloat(),
                    size = size * (0.7f + Random.nextFloat() * 0.6f),
                    color = color,
                    life = life * (0.7f + Random.nextFloat() * 0.5f),
                    maxLife = life
                )
            )
        }
    }

    fun update(deltaTime: Float) {
        val it = particles.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.life -= deltaTime
            if (p.life <= 0f) {
                it.remove()
            } else {
                p.x += p.vx * deltaTime
                p.y += p.vy * deltaTime
                p.vx *= 0.92f
                p.vy *= 0.92f
            }
        }
    }

    fun draw(canvas: Canvas) {
        for (i in particles.indices) {
            val p = particles[i]
            paint.color = p.color
            paint.alpha = ((p.life / p.maxLife) * 255).toInt().coerceIn(0, 255)
            val half = p.size / 2f
            canvas.drawRect(p.x - half, p.y - half, p.x + half, p.y + half, paint)
        }
    }

    fun clear() {
        particles.clear()
    }
}
