package com.clashangprpr.app.model

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import com.clashangprpr.app.engine.Assets
import com.clashangprpr.app.engine.SpriteSheet
import com.clashangprpr.app.engine.Ui
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.hypot
import kotlin.math.sqrt

/** World-space text (scaled together with the camera zoom). */
object WText {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    fun draw(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int) {
        p.textSize = size
        p.color = Color.argb(Color.alpha(color), 0, 0, 0)
        c.drawText(s, x + 1.5f, y + 1.5f, p)
        p.color = color
        c.drawText(s, x, y, p)
    }
}

private object Tmp {
    val r = RectF()
    val src = Rect()
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
}

private fun dist(ax: Float, ay: Float, bx: Float, by: Float): Float {
    val dx = ax - bx
    val dy = ay - by
    return sqrt(dx * dx + dy * dy)
}

// ------------------------------------------------------------------ projectiles / fx

class Projectile(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var damage: Int,
    var kind: Int,
    var friendly: Boolean
) {
    companion object {
        const val MAGIC = 0
        const val ORB = 1
        const val BOMB = 2
        const val LASER = 3
        const val ARROW = 4
        const val FIREBALL = 5
        const val CANNONBALL = 6
    }

    var dead = false
    var parried = false
    private var life = when (kind) {
        BOMB -> 0.75f
        FIREBALL -> 1.15f
        CANNONBALL -> 1.45f
        else -> 1.9f
    }
    val hitSet = HashSet<Any>()
    val pierce: Boolean get() = (kind == LASER && friendly)
    private var spin = 0f

    fun reflect() {
        vx = -vx * 2.15f
        vy = -vy * 2.15f
        damage = (damage * 3.0f).toInt() + 90
        friendly = true
        parried = true
        life = 1.85f
        hitSet.clear()
    }

    fun update(dt: Float) {
        life -= dt
        spin += dt * 12f
        if (life <= 0f) {
            dead = true
            return
        }
        x += vx * dt
        y += vy * dt
    }

    fun draw(c: Canvas) {
        if (dead) return
        val p = Tmp.p
        p.style = Paint.Style.FILL
        when (kind) {
            MAGIC -> {
                p.color = Color.argb(90, 0, 229, 255)
                c.drawCircle(x - vx * 0.03f, y - vy * 0.03f, 10f, p)
                p.color = Color.parseColor("#00E5FF")
                c.drawCircle(x, y, 13f, p)
                p.color = Color.WHITE
                c.drawCircle(x, y, 6f, p)
            }
            FIREBALL -> {
                p.color = Color.argb(120, 255, 87, 34)
                c.drawCircle(x - vx * 0.03f, y - vy * 0.03f, 14f, p)
                p.color = Color.parseColor("#FF9800")
                c.drawCircle(x, y, 11f, p)
                p.color = Color.parseColor("#FFF59D")
                c.drawCircle(x, y, 5f, p)
            }
            CANNONBALL -> {
                p.color = Color.argb(100, 255, 140, 0)
                c.drawCircle(x - vx * 0.025f, y - vy * 0.025f, 12f, p)
                p.color = Color.rgb(35, 38, 45)
                c.drawCircle(x, y, 11f, p)
                p.color = Color.rgb(120, 130, 145)
                c.drawCircle(x - 3f, y - 3f, 4f, p)
            }
            ORB -> {
                p.color = if (parried) Color.argb(140, 0, 229, 255) else Color.argb(110, 233, 30, 99)
                c.drawCircle(x, y, 18f, p)
                p.color = if (parried) Color.parseColor("#00E5FF") else Color.parseColor("#E91E63")
                c.drawCircle(x, y, 11f, p)
                p.color = Color.WHITE
                c.drawCircle(x, y, 4.5f, p)
            }
            LASER -> {
                val col = if (friendly) Color.parseColor("#76FF03") else Color.parseColor("#FF1744")
                p.style = Paint.Style.STROKE
                p.strokeCap = Paint.Cap.ROUND
                p.strokeWidth = 11f
                p.color = Color.argb(110, Color.red(col), Color.green(col), Color.blue(col))
                c.drawLine(x - vx * 0.06f, y - vy * 0.06f, x, y, p)
                p.strokeWidth = 5f
                p.color = Color.WHITE
                c.drawLine(x - vx * 0.045f, y - vy * 0.045f, x, y, p)
                p.style = Paint.Style.FILL
            }
            ARROW -> {
                p.style = Paint.Style.STROKE
                p.strokeCap = Paint.Cap.ROUND
                p.strokeWidth = 4f
                p.color = Color.parseColor("#C8A064")
                c.drawLine(x - vx * 0.05f, y - vy * 0.05f, x, y, p)
                p.color = Color.WHITE
                p.strokeWidth = 7f
                c.drawLine(x - vx * 0.008f, y - vy * 0.008f, x, y, p)
                p.style = Paint.Style.FILL
            }
            BOMB -> {
                val b = Assets.bmp("icons/bomb.png")
                if (b != null) {
                    c.save()
                    c.rotate(spin * 20f, x, y)
                    Tmp.r.set(x - 17f, y - 17f, x + 17f, y + 17f)
                    c.drawBitmap(b, null, Tmp.r, null)
                    c.restore()
                } else {
                    p.color = Color.BLACK
                    c.drawCircle(x, y, 14f, p)
                }
            }
        }
    }
}

class ExplosionFx(val x: Float, val y: Float, val radius: Float) {
    private var t = 0f
    private val life = 0.55f
    val done: Boolean get() = t >= life

    fun update(dt: Float) {
        t += dt
    }

    fun draw(c: Canvas) {
        val bm = Assets.filtered(
            "environment/bomb_fx.png", "orange",
            ColorMatrixColorFilter(floatArrayOf(0f, 0f, 1f, 0f, 0f, 0f, 0.62f, 0f, 0f, 0f, 0f, 0f, 0.05f, 0f, 0f, 0f, 0f, 0f, 1f, 0f))
        )
        val k = (t / life).coerceIn(0f, 1f)
        val p = Tmp.p
        p.style = Paint.Style.FILL
        p.color = Color.argb(((1f - k) * 110).toInt(), 255, 140, 30)
        c.drawCircle(x, y, radius * (0.4f + 0.6f * k), p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 5f
        p.color = Color.argb(((1f - k) * 255).toInt(), 255, 220, 120)
        c.drawCircle(x, y, radius * k, p)
        p.style = Paint.Style.FILL
        if (bm != null) {
            val f = min(11, 5 + (k * 7f).toInt())
            Tmp.src.set(f * 48, 0, f * 48 + 48, 48)
            val s = radius * 1.7f
            Tmp.r.set(x - s / 2f, y - s / 2f, x + s / 2f, y + s / 2f)
            c.drawBitmap(bm, Tmp.src, Tmp.r, null)
        }
    }
}

class MeteorStrikeZone(val x: Float, val y: Float, val radius: Float = 115f, var timer: Float = 1.25f) {
    private val maxTimer = timer
    val readyToExplode: Boolean get() = timer <= 0f

    fun update(dt: Float) {
        timer -= dt
    }

    fun draw(c: Canvas) {
        val prog = (1f - (timer / maxTimer)).coerceIn(0f, 1f)
        val p = Tmp.p
        p.style = Paint.Style.FILL
        p.color = Color.argb((45 + 90 * prog).toInt(), 255, 23, 68)
        c.drawCircle(x, y, radius, p)
        p.color = Color.argb(140, 255, 82, 82)
        c.drawCircle(x, y, radius * prog, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 4f
        p.color = Color.parseColor("#FF1744")
        c.drawCircle(x, y, radius, p)
        p.style = Paint.Style.FILL
        WText.draw(c, "نيزك! ⚡", x, y + 6f, 16f, Color.WHITE)
    }
}

class ColossalBossAltar(var x: Float, var y: Float) {
    val size = 160f
    private var t = 0f

    fun update(dt: Float) {
        t += dt
    }

    fun isNear(px: Float, py: Float): Boolean = dist(px, py, x + size / 2f, y + size / 2f) < 150f

    fun draw(c: Canvas, sealsCount: Int, bossActive: Boolean) {
        val cx = x + size / 2f
        val cy = y + size / 2f
        val p = Tmp.p
        p.style = Paint.Style.FILL
        p.color = Color.argb(190, 22, 10, 36)
        c.drawCircle(cx, cy, 98f, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 5f
        p.color = if (sealsCount >= 7) Color.parseColor("#FF1744") else Color.parseColor("#AB47BC")
        c.drawCircle(cx, cy, 96f + sin(t * 3f) * 4f, p)
        p.style = Paint.Style.FILL

        val circle = Assets.bmp("environment/magic_circle2.png") ?: Assets.bmp("environment/magic_circle.png")
        if (circle != null) {
            c.save()
            c.rotate(t * 28f, cx, cy)
            Tmp.r.set(cx - 82f, cy - 82f, cx + 82f, cy + 82f)
            c.drawBitmap(circle, null, Tmp.r, null)
            c.restore()
        }

        val sealBmp = Assets.bmp("icons/abyss_seal.png")
        for (i in 0 until 7) {
            val ang = i * (6.283185f / 7f) - 1.57f
            val px = cx + cos(ang) * 86f
            val py = cy + sin(ang) * 86f
            val lit = i < sealsCount
            p.color = if (lit) Color.argb(220, 255, 23, 68) else Color.argb(150, 45, 45, 58)
            c.drawCircle(px, py, 15f, p)
            if (lit && sealBmp != null) {
                Tmp.r.set(px - 12f, py - 12f, px + 12f, py + 12f)
                c.drawBitmap(sealBmp, null, Tmp.r, null)
            }
        }

        val skull = Assets.bmp("environment/demon_skull.png") ?: Assets.bmp("environment/altar.png")
        if (skull != null) {
            val bob = sin(t * 3.5f) * 4f
            Tmp.r.set(cx - 38f, cy - 42f + bob, cx + 38f, cy + 34f + bob)
            c.drawBitmap(skull, null, Tmp.r, null)
        }
        val statusTxt = if (bossActive) "الزعيم العملاق في الساحة!" else "مذبح الزعيم الأسطوري (الأختام: $sealsCount/7)"
        WText.draw(c, statusTxt, cx, y - 16f, 20f, if (sealsCount >= 7) Color.parseColor("#FF5252") else Color.parseColor("#EA80FC"))
        WText.draw(c, "تفاعل لعرض مسار التختيم واستدعاء الزعيم العملاق", cx, y + 6f, 14.5f, Color.parseColor("#FFE082"))
    }
}

/** Storage Chest & Silo (الشونة وصندوق التخزين الكبير لتفريغ الشنطة وتخزين الموارد). */
class StorageChest(var x: Float, var y: Float, val titleAr: String = "شونة وصندوق تخزين") {
    val size = 72f

    fun isNear(px: Float, py: Float): Boolean = dist(px, py, x + size / 2f, y + size / 2f) < 115f

    fun draw(c: Canvas, time: Float) {
        val bm = Assets.bmp("environment/storage_silo.png") ?: Assets.bmp("icons/storage_chest.png") ?: Assets.bmp("environment/chest_gold.png")
        val bob = sin(time * 3f) * 2f
        Tmp.p.style = Paint.Style.FILL
        Tmp.p.color = Color.argb(85, 0, 0, 0)
        Tmp.r.set(x + 8f, y + size - 14f, x + size - 8f, y + size + 2f)
        c.drawOval(Tmp.r, Tmp.p)
        if (bm != null) {
            Tmp.r.set(x, y + bob, x + size, y + size + bob)
            c.drawBitmap(bm, null, Tmp.r, null)
        }
        WText.draw(c, titleAr, x + size / 2f, y - 8f, 18f, Color.parseColor("#FFD54F"))
        WText.draw(c, "تفريغ الشنطة وتخزين الموارد 📦", x + size / 2f, y + 8f, 13f, Color.WHITE)
    }
}

class Particle(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float, val maxLife: Float, val color: Int, val size: Float)

class ParticleSystem {
    val list = ArrayList<Particle>()
    var maxCount = 380
    private val rnd = java.util.Random(5)

    fun burst(x: Float, y: Float, n: Int, color: Int, speed: Float, size: Float, life: Float) {
        if (list.size > maxCount) return
        for (i in 0 until n) {
            val a = rnd.nextFloat() * 6.2832f
            val s = speed * (0.35f + rnd.nextFloat() * 0.65f)
            list.add(Particle(x, y, cos(a) * s, sin(a) * s, life * (0.6f + rnd.nextFloat() * 0.4f), life, color, size))
        }
    }

    fun update(dt: Float) {
        var i = list.size - 1
        while (i >= 0) {
            val p = list[i]
            p.life -= dt
            if (p.life <= 0f) {
                list.removeAt(i)
            } else {
                p.x += p.vx * dt
                p.y += p.vy * dt
                p.vx *= (1f - 3f * dt).coerceAtLeast(0f)
                p.vy *= (1f - 3f * dt).coerceAtLeast(0f)
            }
            i--
        }
    }

    fun draw(c: Canvas) {
        val pt = Tmp.p
        pt.style = Paint.Style.FILL
        for (p in list) {
            pt.color = p.color
            pt.alpha = (255 * (p.life / p.maxLife)).toInt().coerceIn(0, 255)
            c.drawRect(p.x - p.size / 2f, p.y - p.size / 2f, p.x + p.size / 2f, p.y + p.size / 2f, pt)
        }
    }
}

class FloatingText(val text: String, var x: Float, var y: Float, val color: Int, val size: Float = 26f) {
    var life = 1.1f
    val dead: Boolean get() = life <= 0f

    fun update(dt: Float) {
        life -= dt
        y -= 46f * dt
    }

    fun draw(c: Canvas) {
        val a = (255 * min(1f, life / 0.4f)).toInt().coerceIn(0, 255)
        WText.draw(c, text, x, y, size, Color.argb(a, Color.red(color), Color.green(color), Color.blue(color)))
    }
}

class CoinDrop(var x: Float, var y: Float, val value: Int) {
    var collected = false
    private var t = (x * 0.013f)
    private var vx = (Math.random().toFloat() - 0.5f) * 220f
    private var vy = -120f - Math.random().toFloat() * 80f
    private var popTime = 0.35f

    fun update(dt: Float, p: Player) {
        t += dt
        if (popTime > 0f) {
            popTime -= dt
            x += vx * dt
            y += vy * dt
            vy += 520f * dt
            return
        }
        val d = dist(p.cx, p.cy, x, y)
        if (d < 190f && !p.isDead) {
            val k = 420f * dt / max(1f, d)
            x += (p.cx - x) * k
            y += (p.cy - y) * k
        }
        if (d < 34f && !p.isDead) collected = true
    }

    fun draw(c: Canvas) {
        val b = Assets.bmp("icons/coin.png") ?: return
        val bob = sin(t * 6f) * 3f
        Tmp.r.set(x - 14f, y - 14f + bob, x + 14f, y + 14f + bob)
        c.drawBitmap(b, null, Tmp.r, null)
    }
}

// ------------------------------------------------------------------ interactables

class TreasureChest(
    var x: Float,
    var y: Float,
    val gold: Int,
    val isGem: Boolean = false,
    val requiresGoldKey: Boolean = false
) {
    var isOpened = false
    private val anim = SpriteSheet("environment/chest.png", 0.1f, false)
    val size = 64f

    fun open(): Int {
        if (isOpened) return 0
        isOpened = true
        anim.setFrame(anim.frameCount - 1)
        return gold
    }

    fun isNear(px: Float, py: Float): Boolean = !isOpened && dist(px, py, x + size / 2f, y + size / 2f) < 105f

    fun draw(c: Canvas, time: Float) {
        if (requiresGoldKey) {
            val bm = Assets.bmp(if (isOpened) "environment/chest_gold.png" else "environment/chest_wood.png")
            if (bm != null) {
                Tmp.r.set(x, y, x + size, y + size)
                c.drawBitmap(bm, null, Tmp.r, null)
            } else {
                anim.draw(c, x, y, size, size)
            }
            if (!isOpened) {
                WText.draw(c, "صندوق ذهبي مقفل", x + size / 2f, y - 6f, 16f, Color.parseColor("#FFD54F"))
            }
        } else {
            anim.draw(c, x, y, size, size)
        }
        if (!isOpened) {
            val a = (120 + 100 * sin(time * 3f)).toInt().coerceIn(0, 255)
            Tmp.p.style = Paint.Style.FILL
            Tmp.p.color = Color.argb(a, 255, 215, 64)
            c.drawCircle(x + size - 8f, y + 10f, 5f, Tmp.p)
        }
    }
}

class MerchantNPC(var x: Float, var y: Float) {
    val size = 96f
    private val idle = SpriteSheet("sprites/merchant/idle.png", 0.22f)

    fun update(dt: Float) = idle.update(dt)

    fun isNear(px: Float, py: Float): Boolean = dist(px, py, x + size / 2f, y + size * 0.6f) < 130f

    fun draw(c: Canvas) {
        Tmp.p.style = Paint.Style.FILL
        Tmp.p.color = Color.argb(85, 0, 0, 0)
        Tmp.r.set(x + 22f, y + 76f, x + 74f, y + 92f)
        c.drawOval(Tmp.r, Tmp.p)
        idle.draw(c, x, y, size, size)
        WText.draw(c, "التاجر إلريك", x + size / 2f, y - 6f, 20f, Color.parseColor("#FFD54F"))
    }
}

class WorldProp(val path: String, var x: Float, var y: Float, val w: Float, val h: Float, dur: Float = 0.12f) {
    private val anim = SpriteSheet(path, dur)
    fun update(dt: Float) = anim.update(dt)
    fun draw(c: Canvas) = anim.draw(c, x, y, w, h)
}

class Bed(var x: Float, var y: Float) {
    fun isNear(px: Float, py: Float): Boolean = dist(px, py, x + 48f, y + 32f) < 105f

    fun draw(c: Canvas) {
        val p = Tmp.p
        p.style = Paint.Style.FILL
        p.color = Color.rgb(78, 52, 36)
        Tmp.r.set(x, y, x + 96f, y + 64f)
        c.drawRoundRect(Tmp.r, 8f, 8f, p)
        p.color = Color.rgb(236, 232, 224)
        Tmp.r.set(x + 5f, y + 6f, x + 91f, y + 58f)
        c.drawRoundRect(Tmp.r, 6f, 6f, p)
        p.color = Color.rgb(250, 250, 246)
        Tmp.r.set(x + 9f, y + 12f, x + 31f, y + 40f)
        c.drawRoundRect(Tmp.r, 6f, 6f, p)
        p.color = Color.rgb(176, 48, 56)
        Tmp.r.set(x + 36f, y + 6f, x + 91f, y + 58f)
        c.drawRoundRect(Tmp.r, 6f, 6f, p)
        WText.draw(c, "سرير للنوم", x + 48f, y - 6f, 18f, Color.parseColor("#FFCC80"))
    }
}

class FurnaceObj(var x: Float, var y: Float) {
    val size = 68f
    private val fire = SpriteSheet("environment/fire.png", 0.10f)

    fun update(dt: Float) = fire.update(dt)

    fun isNear(px: Float, py: Float): Boolean = dist(px, py, x + size / 2f, y + size / 2f) < 110f

    fun draw(c: Canvas, time: Float) {
        val bm = Assets.bmp("icons/furnace.png")
        if (bm != null) {
            Tmp.r.set(x, y, x + size, y + size)
            c.drawBitmap(bm, null, Tmp.r, null)
        } else {
            Tmp.p.style = Paint.Style.FILL
            Tmp.p.color = Color.rgb(90, 95, 105)
            Tmp.r.set(x, y, x + size, y + size)
            c.drawRoundRect(Tmp.r, 10f, 10f, Tmp.p)
        }
        fire.draw(c, x + 16f, y + 20f, 36f, 36f)
        val glow = (90 + 50 * sin(time * 5f)).toInt().coerceIn(0, 255)
        Tmp.p.style = Paint.Style.FILL
        Tmp.p.color = Color.argb(glow, 255, 140, 0)
        c.drawCircle(x + size / 2f, y + size * 0.65f, 22f, Tmp.p)
        WText.draw(c, "فرن صهر وطبخ", x + size / 2f, y - 8f, 18f, Color.parseColor("#FFAB40"))
    }
}

class CampfireNode(var x: Float, var y: Float, val nameAr: String, var isLit: Boolean = true) {
    val size = 64f
    private val fire = SpriteSheet("environment/fire.png", 0.11f)

    fun update(dt: Float) {
        if (isLit) fire.update(dt)
    }

    fun isNear(px: Float, py: Float): Boolean = dist(px, py, x + size / 2f, y + size / 2f) < 115f

    fun draw(c: Canvas, time: Float) {
        val base = Assets.bmp("environment/bonefire.png")
        if (base != null) {
            Tmp.r.set(x, y + 10f, x + size, y + size + 10f)
            c.drawBitmap(base, null, Tmp.r, null)
        }
        if (isLit) {
            val glow = (70 + 40 * sin(time * 4f)).toInt().coerceIn(0, 255)
            Tmp.p.style = Paint.Style.FILL
            Tmp.p.color = Color.argb(glow, 255, 160, 40)
            c.drawCircle(x + size / 2f, y + size / 2f, 44f, Tmp.p)
            fire.draw(c, x + 6f, y - 12f, 52f, 56f)
        }
        WText.draw(
            c, if (isLit) "مخيم: $nameAr" else "مخيم منطفئ ($nameAr)",
            x + size / 2f, y - 16f, 17f,
            if (isLit) Color.parseColor("#FFD54F") else Color.LTGRAY
        )
    }
}

class CaveEntrance(var x: Float, var y: Float, val biome: Int, val nameAr: String, val seed: Long) {
    val w = 128f
    val h = 112f
    var maxFloorCleared = 0

    fun isNear(px: Float, py: Float): Boolean = dist(px, py, x + w / 2f, y + h * 0.65f) < 125f

    fun draw(c: Canvas, time: Float) {
        val gate = Assets.bmp("environment/cave_gate.png")
        if (gate != null) {
            Tmp.r.set(x, y, x + w, y + h)
            c.drawBitmap(gate, null, Tmp.r, null)
        } else {
            Tmp.p.style = Paint.Style.FILL
            Tmp.p.color = Color.rgb(30, 28, 38)
            Tmp.r.set(x, y, x + w, y + h)
            c.drawRoundRect(Tmp.r, 20f, 20f, Tmp.p)
        }
        val crys = Assets.bmp(if (biome % 2 == 0) "environment/crystal_green.png" else "environment/crystal_white.png")
        if (crys != null) {
            Tmp.r.set(x - 10f, y + h - 42f, x + 28f, y + h)
            c.drawBitmap(crys, null, Tmp.r, null)
            Tmp.r.set(x + w - 28f, y + h - 42f, x + w + 10f, y + h)
            c.drawBitmap(crys, null, Tmp.r, null)
        }
        val pulse = (110 + 80 * sin(time * 3.2f + biome)).toInt().coerceIn(0, 255)
        Tmp.p.style = Paint.Style.FILL
        Tmp.p.color = Color.argb(pulse, 130, 255, 220)
        c.drawCircle(x + w / 2f, y + h * 0.62f, 18f, Tmp.p)
        WText.draw(c, nameAr, x + w / 2f, y - 8f, 19f, Color.parseColor("#80CBC4"))
    }
}

class CaveExitNode(var x: Float, var y: Float, val isNextFloor: Boolean, val floorNo: Int) {
    val size = 96f

    fun isNear(px: Float, py: Float): Boolean = dist(px, py, x + size / 2f, y + size / 2f) < 105f

    fun draw(c: Canvas, time: Float) {
        val path = if (isNextFloor) "environment/magic_circle2.png" else "tiles/door.png"
        val bm = Assets.bmp(path) ?: Assets.bmp("environment/magic_circle.png")
        val cx = x + size / 2f
        val cy = y + size / 2f
        val glowCol = if (isNextFloor) Color.argb(120, 224, 64, 251) else Color.argb(120, 105, 240, 174)
        Tmp.p.style = Paint.Style.FILL
        Tmp.p.color = glowCol
        c.drawCircle(cx, cy, 44f + sin(time * 4f) * 5f, Tmp.p)
        if (bm != null) {
            Tmp.r.set(x + 8f, y + 8f, x + size - 8f, y + size - 8f)
            c.drawBitmap(bm, null, Tmp.r, null)
        }
        val label = if (isNextFloor) "النزول للطابق ${floorNo + 1}" else "الخروج للسطح"
        WText.draw(c, label, cx, y - 6f, 19f, if (isNextFloor) Color.parseColor("#EA80FC") else Color.parseColor("#69F0AE"))
    }
}

class CaveCocoon(var x: Float, var y: Float, val biome: Int) {
    var hp = 5
    var isDestroyed = false
    val hitbox = RectF(x - 4f, y - 4f, x + 84f, y + 84f)
    var flash = 0f
    var spawnTimer = 7f

    fun hit(): Boolean {
        if (isDestroyed) return false
        hp--
        flash = 0.14f
        if (hp <= 0) isDestroyed = true
        return true
    }

    fun update(dt: Float): Boolean {
        if (isDestroyed) return false
        if (flash > 0f) flash -= dt
        spawnTimer -= dt
        if (spawnTimer <= 0f) {
            spawnTimer = 11f
            return true
        }
        return false
    }

    fun draw(c: Canvas, time: Float) {
        if (isDestroyed) return
        val path = if (biome % 2 == 0) "environment/cocoon.png" else "environment/demon_skull.png"
        val bm = Assets.bmp(path) ?: Assets.bmp("environment/cocoon.png") ?: return
        val off = if (flash > 0f) 4f else sin(time * 4f) * 2f
        Tmp.r.set(x + off, y, x + 80f + off, y + 80f)
        c.drawBitmap(bm, null, Tmp.r, null)
        WText.draw(c, "عش وحوش ($hp)", x + 40f, y - 6f, 16f, Color.parseColor("#FF8A80"))
    }
}

class CaveTotem(var x: Float, var y: Float, val biome: Int) {
    var activated = false
    val w = 88f
    val h = 96f

    fun isNear(px: Float, py: Float): Boolean = !activated && dist(px, py, x + w / 2f, y + h / 2f) < 110f

    fun draw(c: Canvas, time: Float) {
        val bm = Assets.bmp("environment/cave_totem.png") ?: Assets.bmp("environment/altar.png")
        if (bm != null) {
            Tmp.r.set(x, y, x + w, y + h)
            c.drawBitmap(bm, null, Tmp.r, null)
        }
        if (!activated) {
            val a = (110 + 90 * sin(time * 4f)).toInt().coerceIn(0, 255)
            Tmp.p.style = Paint.Style.STROKE
            Tmp.p.strokeWidth = 3f
            Tmp.p.color = Color.argb(a, 255, 82, 82)
            c.drawCircle(x + w / 2f, y + h / 2f, 46f, Tmp.p)
            Tmp.p.style = Paint.Style.FILL
            WText.draw(c, "مذبح التحدي (غنائم مضاعفة)", x + w / 2f, y - 8f, 17f, Color.parseColor("#FF8A80"))
        } else {
            WText.draw(c, "مذبح التحدي (مكتمل)", x + w / 2f, y - 8f, 16f, Color.GRAY)
        }
    }
}

/**
 * Castle / Fortress Cannon Turret:
 * - Can be mounted on castle walls (`mountedOnWall = true`) or on the ground.
 * - Requires Gunpowder (`ammo > 0`) to fire when friendly; player refills it with Gunpowder!
 */
class CannonTurret(var x: Float, var y: Float, val friendly: Boolean, var mountedOnWall: Boolean = false) {
    var hp = if (friendly) 14 else 7
    var isDestroyed = false
    val size = 76f
    val hitbox = RectF(x - 4f, y - 4f, x + size + 4f, y + size + 4f)
    private var fireCd = 1.55f
    var flash = 0f
    val maxAmmo = 25
    var ammo = if (friendly) 12 else 999

    fun isNear(px: Float, py: Float): Boolean = !isDestroyed && friendly && dist(px, py, x + size / 2f, y + size / 2f) < 115f

    fun hit(): Boolean {
        if (isDestroyed) return false
        hp--
        flash = 0.14f
        if (hp <= 0) isDestroyed = true
        return true
    }

    fun update(dt: Float, player: Player, enemies: List<Enemy>, proj: MutableList<Projectile>): Boolean {
        if (isDestroyed) return false
        if (flash > 0f) flash -= dt
        fireCd -= dt
        if (fireCd > 0f) return false
        val cx = x + size / 2f
        val cy = y + size / 2f
        if (friendly) {
            if (ammo <= 0) return false
            var best: Enemy? = null
            var bestD = 620f
            for (en in enemies) {
                if (en.isDead) continue
                val d = dist(cx, cy, en.cx, en.cy)
                if (d < bestD) {
                    bestD = d
                    best = en
                }
            }
            val t = best ?: return false
            val dx = t.cx - cx
            val dy = t.cy - cy
            val l = max(1f, sqrt(dx * dx + dy * dy))
            ammo--
            proj.add(Projectile(cx, cy, dx / l * 540f, dy / l * 540f, 105, Projectile.CANNONBALL, true))
            fireCd = 1.65f
            return true
        } else {
            if (player.isDead) return false
            val d = dist(cx, cy, player.cx, player.cy)
            if (d > 580f) return false
            val dx = player.cx - cx
            val dy = player.cy - cy
            val l = max(1f, d)
            proj.add(Projectile(cx, cy, dx / l * 420f, dy / l * 420f, 24, Projectile.CANNONBALL, false))
            fireCd = 2.4f
            return true
        }
    }

    fun draw(c: Canvas) {
        if (isDestroyed) return
        if (mountedOnWall) {
            Tmp.p.style = Paint.Style.FILL
            Tmp.p.color = Color.rgb(75, 80, 92)
            Tmp.r.set(x + 6f, y + 10f, x + size - 6f, y + size - 4f)
            c.drawRoundRect(Tmp.r, 10f, 10f, Tmp.p)
            Tmp.p.style = Paint.Style.STROKE
            Tmp.p.strokeWidth = 3f
            Tmp.p.color = Color.parseColor("#FFD54F")
            c.drawRoundRect(Tmp.r, 10f, 10f, Tmp.p)
            Tmp.p.style = Paint.Style.FILL
        }
        val bm = Assets.bmp("environment/cannon_fort.png") ?: Assets.bmp("environment/pirate_cannon.png") ?: Assets.bmp("environment/cannon.png")
        val off = if (flash > 0f) 3f else 0f
        if (bm != null) {
            Tmp.r.set(x + off, y, x + size + off, y + size)
            c.drawBitmap(bm, null, Tmp.r, null)
        } else {
            Tmp.p.style = Paint.Style.FILL
            Tmp.p.color = Color.DKGRAY
            c.drawCircle(x + size / 2f, y + size / 2f, 28f, Tmp.p)
        }
        val lbl = if (friendly) {
            if (ammo > 0) "مدفع السور (بارود: $ammo/$maxAmmo)" else "مدفع السور (فرغ البارود! املأه 💥)"
        } else {
            "مدفع القراصنة ($hp)"
        }
        val col = if (!friendly) Color.parseColor("#FF8A80") else if (ammo > 0) Color.parseColor("#80D8FF") else Color.parseColor("#FF5252")
        WText.draw(c, lbl, x + size / 2f, y - 6f, 15.5f, col)
    }
}

class FortressBanner(var x: Float, var y: Float, val titleAr: String) {
    fun draw(c: Canvas, time: Float) {
        val bm = Assets.bmp("environment/pirate_flag.png")
        val sway = sin(time * 3f) * 3f
        if (bm != null) {
            Tmp.r.set(x + sway, y, x + 72f + sway, y + 72f)
            c.drawBitmap(bm, null, Tmp.r, null)
        }
        WText.draw(c, titleAr, x + 36f, y - 8f, 18f, Color.parseColor("#FF5252"))
    }
}

class BoatDock(var x: Float, var y: Float) {
    fun isNear(px: Float, py: Float): Boolean = dist(px, py, x + 40f, y + 40f) < 120f

    fun draw(c: Canvas, time: Float) {
        val bob = sin(time * 3.5f) * 3f
        val bm = Assets.bmp("icons/boat.png")
        if (bm != null) {
            Tmp.r.set(x, y + bob, x + 80f, y + 80f + bob)
            c.drawBitmap(bm, null, Tmp.r, null)
        }
        WText.draw(c, "مرسى المراكب", x + 40f, y - 6f + bob, 17f, Color.parseColor("#80DEEA"))
    }
}

class CraftTable(var x: Float, var y: Float) {
    val size = 64f
    fun isNear(px: Float, py: Float): Boolean = dist(px, py, x + size / 2f, y + size / 2f) < 110f

    fun draw(c: Canvas, time: Float) {
        val crate = Assets.tinted("tiles/crate.png", 0xFFD9A864.toInt())
        if (crate != null) {
            Tmp.r.set(x, y, x + size, y + size)
            c.drawBitmap(crate, null, Tmp.r, null)
        }
        val axe = Assets.tinted("ui/ico/axe.png", 0xFFFFE082.toInt())
        if (axe != null) {
            val bob = sin(time * 3f) * 3f
            Tmp.r.set(x + 16f, y - 26f + bob, x + 48f, y + 6f + bob)
            c.drawBitmap(axe, null, Tmp.r, null)
        }
        WText.draw(c, "طاولة الصناعة (4×4)", x + size / 2f, y - 34f, 18f, Color.parseColor("#FFCC80"))
    }
}

class TimePortal(var x: Float, var y: Float, val labelAr: String = "بوابة الزمن") {
    var isActive = false
    private var t = 0f
    val size = 140f

    fun update(dt: Float) {
        t += dt
    }

    fun isNear(px: Float, py: Float): Boolean = isActive && dist(px, py, x + size / 2f, y + size / 2f) < 110f

    fun draw(c: Canvas) {
        val cx = x + size / 2f
        val cy = y + size / 2f
        val circle = Assets.bmp("environment/magic_circle.png")
        val p = Tmp.p
        p.style = Paint.Style.FILL
        if (isActive) {
            p.color = Color.argb((70 + 30 * sin(t * 4f)).toInt(), 0, 229, 255)
            c.drawCircle(cx, cy, size * 0.62f, p)
        }
        if (circle != null) {
            c.save()
            c.rotate(t * (if (isActive) 60f else 12f), cx, cy)
            Tmp.r.set(cx - size / 2f, cy - size / 2f, cx + size / 2f, cx + size / 2f)
            p.alpha = if (isActive) 255 else 90
            c.drawBitmap(circle, null, Tmp.r, p)
            p.alpha = 255
            c.restore()
        }
        p.style = Paint.Style.STROKE
        p.strokeWidth = 4f
        for (i in 0 until 3) {
            val rr = size * (0.18f + 0.11f * i) + sin(t * 3f + i) * 4f
            p.color = if (isActive) Color.argb(220, 130, 255, 255) else Color.argb(80, 160, 160, 170)
            c.drawCircle(cx, cy, rr, p)
        }
        p.style = Paint.Style.FILL
        WText.draw(
            c, if (isActive) labelAr else "$labelAr (مغلقة)", cx, y - 8f, 20f,
            if (isActive) Color.parseColor("#80F0FF") else Color.GRAY
        )
    }
}

// ------------------------------------------------------------------ resource nodes

class MiningRock(var x: Float, var y: Float, val isIron: Boolean) {
    var hp = 4
    var isDestroyed = false
    val hitbox = RectF(x - 6f, y - 6f, x + 70f, y + 70f)
    var flash = 0f

    fun hit(): Boolean {
        if (isDestroyed) return false
        hp--
        flash = 0.12f
        if (hp <= 0) isDestroyed = true
        return true
    }

    fun update(dt: Float) {
        if (flash > 0f) flash -= dt
    }

    fun draw(c: Canvas, time: Float) {
        if (isDestroyed) return
        val bm = if (isIron) Assets.tinted("environment/rock_rgba.png", 0xFFB4CCFF.toInt(), 0x001030) else Assets.bmp("environment/rock_3.png") ?: Assets.bmp("environment/rock_rgba.png")
        val off = if (flash > 0f) 3f else 0f
        if (bm != null) {
            Tmp.r.set(x + off, y, x + 64f + off, y + 64f)
            c.drawBitmap(bm, null, Tmp.r, null)
        }
        Tmp.p.style = Paint.Style.FILL
        val ore = if (isIron) Color.rgb(120, 220, 255) else Color.rgb(255, 214, 80)
        val tw = (sin(time * 5f + x) * 0.5f + 0.5f)
        Tmp.p.color = Color.argb((120 + 120 * tw).toInt(), Color.red(ore), Color.green(ore), Color.blue(ore))
        c.drawRect(x + 18f, y + 22f, x + 24f, y + 28f, Tmp.p)
        c.drawRect(x + 40f, y + 36f, x + 46f, y + 42f, Tmp.p)
        c.drawRect(x + 30f, y + 14f, x + 34f, y + 18f, Tmp.p)
    }
}

class WoodCrate(var x: Float, var y: Float, val isBarrel: Boolean) {
    var hp = 2
    var isDestroyed = false
    val hitbox = RectF(x - 4f, y - 4f, x + 68f, y + 68f)
    var flash = 0f

    fun hit(): Boolean {
        if (isDestroyed) return false
        hp--
        flash = 0.12f
        if (hp <= 0) isDestroyed = true
        return true
    }

    fun update(dt: Float) {
        if (flash > 0f) flash -= dt
    }

    fun draw(c: Canvas) {
        if (isDestroyed) return
        val bm = Assets.bmp(if (isBarrel) "tiles/barrel.png" else "tiles/crate.png") ?: return
        val off = if (flash > 0f) 3f else 0f
        Tmp.r.set(x + off, y, x + 64f + off, y + 64f)
        c.drawBitmap(bm, null, Tmp.r, null)
    }
}

class ChoppableTree(var x: Float, var y: Float, val isPine: Boolean) {
    var hp = 3
    var isDestroyed = false
    val hitbox = RectF(x - 6f, y - 6f, x + 70f, y + 70f)
    var flash = 0f

    fun hit(): Boolean {
        if (isDestroyed) return false
        hp--
        flash = 0.12f
        if (hp <= 0) isDestroyed = true
        return true
    }

    fun update(dt: Float) {
        if (flash > 0f) flash -= dt
    }

    fun draw(c: Canvas) {
        if (isDestroyed) return
        val bm = Assets.bmp(if (isPine) "tiles/tree_pine.png" else "tiles/tree_green.png") ?: return
        val off = if (flash > 0f) 3f else 0f
        Tmp.r.set(x - 8f + off, y - 40f, x + 72f + off, y + 62f)
        c.drawBitmap(bm, null, Tmp.r, null)
    }
}

class BuriedMound(var x: Float, var y: Float) {
    var isDug = false
    private var t = 0f

    fun update(dt: Float) {
        t += dt
    }

    fun isNear(px: Float, py: Float): Boolean = !isDug && dist(px, py, x + 32f, y + 32f) < 95f

    fun draw(c: Canvas) {
        if (isDug) return
        val p = Tmp.p
        p.style = Paint.Style.FILL
        p.color = Color.rgb(176, 140, 88)
        Tmp.r.set(x + 6f, y + 22f, x + 58f, y + 58f)
        c.drawOval(Tmp.r, p)
        p.color = Color.rgb(148, 112, 66)
        Tmp.r.set(x + 14f, y + 18f, x + 50f, y + 46f)
        c.drawOval(Tmp.r, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 5f
        p.strokeCap = Paint.Cap.ROUND
        p.color = Color.argb((170 + 80 * sin(t * 4f)).toInt().coerceIn(0, 255), 255, 214, 64)
        c.drawLine(x + 22f, y + 24f, x + 42f, y + 44f, p)
        c.drawLine(x + 42f, y + 24f, x + 22f, y + 44f, p)
        p.style = Paint.Style.FILL
        WText.draw(c, "كنز مدفون", x + 32f, y + 8f, 18f, Color.parseColor("#FFE082"))
    }
}

class PowerGenerator(var x: Float, var y: Float, val index: Int) {
    var hp = 5
    var isDestroyed = false
    val hitbox = RectF(x - 6f, y - 6f, x + 90f, y + 90f)
    private var t = 0f
    var flash = 0f

    fun update(dt: Float) {
        t += dt * 5f
        if (flash > 0f) flash -= dt
    }

    fun hit(): Boolean {
        if (isDestroyed) return false
        hp--
        flash = 0.12f
        if (hp <= 0) isDestroyed = true
        return true
    }

    fun draw(c: Canvas) {
        val p = Tmp.p
        val w = 84f
        if (isDestroyed) {
            p.style = Paint.Style.FILL
            p.color = Color.rgb(34, 36, 42)
            Tmp.r.set(x + 8f, y + 34f, x + w - 8f, y + w - 6f)
            c.drawRoundRect(Tmp.r, 8f, 8f, p)
            return
        }
        Tmp.r.set(x, y, x + w, y + w)
        p.style = Paint.Style.FILL
        p.color = if (flash > 0f) Color.WHITE else Color.rgb(38, 50, 56)
        c.drawRoundRect(Tmp.r, 14f, 14f, p)
        p.color = Color.rgb(20, 28, 34)
        Tmp.r.set(x + 8f, y + 8f, x + w - 8f, y + w - 8f)
        c.drawRoundRect(Tmp.r, 10f, 10f, p)
        p.color = Color.argb((160 + 90 * sin(t)).toInt().coerceIn(0, 255), 0, 229, 255)
        c.drawCircle(x + w / 2f, y + w / 2f, 15f + sin(t) * 2f, p)
        p.color = Color.WHITE
        c.drawCircle(x + w / 2f, y + w / 2f, 6f, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 4f + sin(t) * 1.5f
        p.color = Color.parseColor("#00E5FF")
        Tmp.r.set(x, y, x + w, y + w)
        c.drawRoundRect(Tmp.r, 14f, 14f, p)
        p.style = Paint.Style.FILL
        p.color = Color.parseColor("#76FF03")
        for (i in 0 until hp) c.drawCircle(x + 14f + i * 14f, y - 10f, 4f, p)
        WText.draw(c, "مولد طاقة $index", x + w / 2f, y + w + 20f, 16f, Color.WHITE)
    }
}

// ------------------------------------------------------------------ traps

class DungeonTrap(val baseX: Float, val baseY: Float, val isSaw: Boolean) {
    var x = baseX
    var y = baseY
    private var t = (baseX * 0.01f) % 6f
    private var dmgCd = 0f
    val hitbox = RectF()
    private val fire = SpriteSheet("environment/fire.png", 0.08f)

    val isFireActive: Boolean get() = !isSaw && (t % 3f) > 1.9f
    private val isWarning: Boolean get() = !isSaw && (t % 3f) in 1.4f..1.9f

    fun update(dt: Float, p: Player) {
        t += dt
        if (dmgCd > 0f) dmgCd -= dt
        if (isSaw) {
            x = baseX + sin(t * 1.7f) * 110f
            hitbox.set(x + 12f, y + 12f, x + 52f, y + 52f)
        } else {
            fire.update(dt)
            hitbox.set(x + 6f, y - 24f, x + 58f, y + 58f)
        }
        val active = isSaw || isFireActive
        if (active && dmgCd <= 0f && !p.isDead && RectF.intersects(hitbox, p.hitbox)) {
            p.takeDamage(22)
            dmgCd = 0.8f
        }
    }

    fun draw(c: Canvas) {
        val p = Tmp.p
        if (isSaw) {
            p.style = Paint.Style.STROKE
            p.strokeWidth = 6f
            p.color = Color.argb(110, 20, 20, 30)
            c.drawLine(baseX - 110f, baseY + 32f, baseX + 174f, baseY + 32f, p)
            p.style = Paint.Style.FILL
            val bm = Assets.bmp("environment/blades.png") ?: return
            val f = ((t * 14f).toInt() % 4)
            Tmp.src.set(0, f * 48, 48, f * 48 + 48)
            Tmp.r.set(x, y, x + 64f, y + 64f)
            c.drawBitmap(bm, Tmp.src, Tmp.r, null)
        } else {
            p.style = Paint.Style.FILL
            p.color = Color.rgb(40, 40, 52)
            Tmp.r.set(x + 8f, y + 24f, x + 56f, y + 58f)
            c.drawRoundRect(Tmp.r, 8f, 8f, p)
            p.color = if (isWarning) Color.argb(220, 255, 90, 30) else Color.argb(140, 120, 30, 20)
            Tmp.r.set(x + 18f, y + 32f, x + 46f, y + 50f)
            c.drawRoundRect(Tmp.r, 6f, 6f, p)
            if (isFireActive) fire.draw(c, x - 4f, y - 40f, 72f, 96f)
        }
    }
}


/**
 * Autonomous Allied Iron Golem & Castle Guardian (الغولم الحديدي الحارس الآلي):
 * Patrols around your fortress/village, intercepts Night Bomber Zombies & Siege Zombies,
 * and unleashes ground-slam shockwaves!
 */
class IronGolemGuard(var x: Float, var y: Float, val homeX: Float = x, val homeY: Float = y) {
    var hp = 680
    val maxHp = 680
    var attackCd = 0f
    var slamFlash = 0f
    var facingLeft = false
    private var animT = 0f
    val isDead: Boolean get() = hp <= 0

    fun update(dt: Float, enemies: List<Enemy>, particles: ParticleSystem): Enemy? {
        if (isDead) return null
        animT += dt
        if (attackCd > 0f) attackCd -= dt
        if (slamFlash > 0f) slamFlash -= dt
        // Slowly regenerate HP over time
        if (hp < maxHp && (animT * 10).toInt() % 25 == 0) hp = min(maxHp, hp + 1)

        var bestTarget: Enemy? = null
        var bestD = 520f
        for (en in enemies) {
            if (en.isDead) continue
            val d = dist(en.cx, en.cy, x, y)
            // Prioritize Night Bomber Zombies!
            val scoreD = if (en.isBomberZombie) d * 0.6f else d
            if (scoreD < bestD) {
                bestD = scoreD
                bestTarget = en
            }
        }

        if (bestTarget != null) {
            val dx = bestTarget.cx - x
            val dy = bestTarget.cy - y
            val dist = max(1f, sqrt(dx * dx + dy * dy))
            facingLeft = dx < 0f
            if (dist > 64f) {
                val spd = 168f
                x += (dx / dist) * spd * dt
                y += (dy / dist) * spd * dt
            }
            if (dist <= 92f && attackCd <= 0f) {
                attackCd = 0.85f
                slamFlash = 0.28f
                particles.burst(bestTarget.cx, bestTarget.cy, 12, Color.parseColor("#80DEEA"), 210f, 6f, 0.4f)
                return bestTarget
            }
        } else {
            // Patrol gently around home coordinates
            val tx = homeX + cos(animT * 0.7f) * 90f
            val ty = homeY + sin(animT * 0.7f) * 90f
            val dx = tx - x
            val dy = ty - y
            val d = sqrt(dx * dx + dy * dy)
            if (d > 10f) {
                facingLeft = dx < 0f
                x += (dx / d) * 85f * dt
                y += (dy / d) * 85f * dt
            }
        }
        return null
    }

    fun draw(c: Canvas, time: Float) {
        if (isDead) return
        val bob = sin(time * 4.5f) * 3f
        val bmp = Assets.bmp("icons/iron_golem.png")
        if (bmp != null) {
            val r = RectF(x - 42f, y - 52f + bob, x + 42f, y + 32f + bob)
            c.drawBitmap(bmp, null, r, null)
        }
        if (slamFlash > 0f) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            p.style = Paint.Style.STROKE
            p.strokeWidth = 4f
            p.color = Color.argb((slamFlash / 0.28f * 220).toInt().coerceIn(0, 255), 0, 229, 255)
            c.drawCircle(x, y, 86f * (1f - slamFlash), p)
        }
        // Health bar & title
        val bw = 66f
        val bar = RectF(x - bw / 2f, y - 62f, x + bw / 2f, y - 54f)
        Ui.bar(c, bar, hp.toFloat() / maxHp, Color.parseColor("#00E5FF"))
        WText.draw(c, "الغولم الحارس 🤖", x, y - 66f, 13f, Color.parseColor("#80DEEA"))
    }
}


/**
 * Interactive Centerpiece Monument inside each of the 12 Open-World Structures!
 * Allows the player to claim structure rewards, blessings, gunpowder caches, or trigger structure challenges.
 */
class AncientStructureNode(
    var x: Float,
    var y: Float,
    val kind: Int,
    val nameAr: String,
    val descAr: String,
    val iconAsset: String,
    val accentColor: Int
) {
    var claimed = false
    private var t = 0f

    fun update(dt: Float) {
        t += dt
    }

    fun isNear(px: Float, py: Float): Boolean = !claimed && dist(px, py, x, y) < 125f

    fun draw(c: Canvas, time: Float) {
        val pulse = 0.75f + 0.25f * sin(time * 3.8f)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        // Outer glowing aura ring on floor
        p.style = Paint.Style.FILL
        p.color = Color.argb(if (claimed) 28 else (75 * pulse).toInt().coerceIn(20, 140), Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
        c.drawCircle(x, y, 54f, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3f
        p.color = if (claimed) Color.GRAY else accentColor
        c.drawCircle(x, y, 44f + sin(time * 3f) * 3f, p)

        val bob = if (claimed) 0f else sin(time * 4f) * 4f
        val bmp = Assets.bmp(iconAsset)
        if (bmp != null) {
            val r = RectF(x - 32f, y - 36f + bob, x + 32f, y + 28f + bob)
            c.drawBitmap(bmp, null, r, null)
        }
        val status = if (claimed) "$nameAr (تم الاكتشاف ✓)" else "★ $nameAr ★"
        WText.draw(c, status, x, y - 46f, 14.5f, if (claimed) Color.LTGRAY else accentColor)
        if (!claimed) {
            WText.draw(c, "اقترب وتفاعل للاستكشاف والغنائم 🏛️", x, y + 44f, 12.5f, Color.WHITE)
        }
    }
}
