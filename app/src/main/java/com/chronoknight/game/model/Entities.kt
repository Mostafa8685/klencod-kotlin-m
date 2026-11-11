package com.chronoknight.game.model

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.chronoknight.game.engine.SpriteSheet
import kotlin.math.sqrt

// Player
class Player(
    context: Context,
    var x: Float,
    var y: Float
) {
    val width = 96f
    val height = 96f
    var maxHp = 150
    var hp = 150
    var speed = 280f
    var isAttacking = false
    var isDead = false
    var facingLeft = false
    var attackCooldown = 0f

    val idleAnim = SpriteSheet(context, "sprites/player/idle.png", 4)
    val walkAnim = SpriteSheet(context, "sprites/player/walk.png", 4)
    val attackAnim = SpriteSheet(context, "sprites/player/attack.png", 4, frameDuration = 0.08f)
    val deadAnim = SpriteSheet(context, "sprites/player/dead.png", 2)

    val hitbox = RectF()

    fun update(deltaTime: Float, moveX: Float, moveY: Float, mapW: Float, mapH: Float) {
        if (isDead) {
            deadAnim.update(deltaTime)
            return
        }

        if (attackCooldown > 0) attackCooldown -= deltaTime

        if (isAttacking) {
            attackAnim.update(deltaTime)
            if (attackAnim.isFinished) {
                isAttacking = false
                attackAnim.reset()
            }
        } else {
            val len = sqrt(moveX * moveX + moveY * moveY)
            if (len > 0.1f) {
                x += (moveX / len) * speed * deltaTime
                y += (moveY / len) * speed * deltaTime
                walkAnim.update(deltaTime)
                if (moveX < -0.1f) facingLeft = true
                else if (moveX > 0.1f) facingLeft = false
            } else {
                idleAnim.update(deltaTime)
            }
        }

        // Clamp inside map bounds
        x = x.coerceIn(40f, mapW - width - 40f)
        y = y.coerceIn(40f, mapH - height - 40f)

        hitbox.set(x + 20f, y + 20f, x + width - 20f, y + height - 10f)
    }

    fun attack() {
        if (!isAttacking && attackCooldown <= 0f && !isDead) {
            isAttacking = true
            attackCooldown = 0.35f
            attackAnim.reset()
        }
    }

    fun takeDamage(amount: Int) {
        if (isDead) return
        hp -= amount
        if (hp <= 0) {
            hp = 0
            isDead = true
            deadAnim.reset()
        }
    }

    fun draw(canvas: Canvas) {
        val anim = when {
            isDead -> deadAnim
            isAttacking -> attackAnim
            else -> if (idleAnim.isFinished) idleAnim else walkAnim
        }
        anim.draw(canvas, x, y, width, height, flipX = facingLeft)
    }
}

// Enemy
class Enemy(
    context: Context,
    var x: Float,
    var y: Float,
    val isBoss: Boolean,
    val era: EraType
) {
    val width = if (isBoss) 140f else 88f
    val height = if (isBoss) 140f else 88f
    var maxHp = if (isBoss) 400 else 60
    var hp = maxHp
    var speed = if (isBoss) 130f else 170f
    var attackDmg = if (isBoss) 25 else 12
    var isDead = false
    var facingLeft = false
    var attackTimer = 0f

    val name: String = when {
        isBoss && era == EraType.ERA_1_MEDIEVAL -> "الفارس الأسود (زعيم القلعة)"
        isBoss && era == EraType.ERA_2_CAVES -> "حارس الكهوف الحجري (زعيم الدهاليز)"
        isBoss && era == EraType.ERA_3_ZOMBIE -> "الزومبي الطافر الأكبر (زعيم نهاية العالم)"
        era == EraType.ERA_3_ZOMBIE -> "زومبي جائع"
        era == EraType.ERA_2_CAVES -> "حارس الكهف"
        else -> "فارس ملعون"
    }

    private val idleAnim: SpriteSheet
    private val walkAnim: SpriteSheet
    private val attackAnim: SpriteSheet
    private val deadAnim: SpriteSheet

    init {
        val folder = when {
            isBoss && era == EraType.ERA_1_MEDIEVAL -> "sprites/medieval_boss"
            era == EraType.ERA_3_ZOMBIE && isBoss -> "sprites/zombie_titan"
            era == EraType.ERA_3_ZOMBIE -> "sprites/zombie"
            isBoss && era == EraType.ERA_2_CAVES -> "sprites/medieval_boss"
            else -> "sprites/player"
        }
        idleAnim = SpriteSheet(context, "$folder/idle.png", 4)
        walkAnim = SpriteSheet(context, "$folder/walk.png", 4)
        attackAnim = SpriteSheet(context, "$folder/attack.png", 4, frameDuration = 0.1f)
        deadAnim = SpriteSheet(context, "$folder/dead.png", 2)
    }

    val hitbox = RectF()

    fun update(deltaTime: Float, player: Player) {
        if (isDead) {
            deadAnim.update(deltaTime)
            return
        }

        if (attackTimer > 0) attackTimer -= deltaTime

        val dx = player.x - x
        val dy = player.y - y
        val dist = sqrt(dx * dx + dy * dy)

        // Aggro distance
        if (dist < 650f) {
            facingLeft = dx < 0
            if (dist > 70f) {
                x += (dx / dist) * speed * deltaTime
                y += (dy / dist) * speed * deltaTime
                walkAnim.update(deltaTime)
            } else {
                // In melee range
                if (attackTimer <= 0f) {
                    player.takeDamage(attackDmg)
                    attackTimer = 1.2f
                    attackAnim.reset()
                }
                attackAnim.update(deltaTime)
            }
        } else {
            idleAnim.update(deltaTime)
        }

        hitbox.set(x + 10f, y + 10f, x + width - 10f, y + height - 10f)
    }

    fun takeDamage(amount: Int) {
        if (isDead) return
        hp -= amount
        if (hp <= 0) {
            hp = 0
            isDead = true
            deadAnim.reset()
        }
    }

    fun draw(canvas: Canvas) {
        val anim = when {
            isDead -> deadAnim
            attackTimer > 0.8f -> attackAnim
            else -> walkAnim
        }
        anim.draw(canvas, x, y, width, height, flipX = facingLeft)

        // Health bar above enemy
        if (!isDead && hp < maxHp) {
            val barW = width * 0.8f
            val barH = 10f
            val barX = x + (width - barW) / 2f
            val barY = y - 15f
            val p = Paint()
            p.color = Color.BLACK
            canvas.drawRect(barX, barY, barX + barW, barY + barH, p)
            p.color = Color.RED
            canvas.drawRect(barX, barY, barX + (barW * (hp.toFloat() / maxHp)), barY + barH, p)
        }
    }
}

// Time Portal
class TimePortal(
    var x: Float,
    var y: Float
) {
    val radius = 70f
    var isActive = false
    private var glowAngle = 0f

    private val portalPaint = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }
    private val innerPaint = Paint().apply {
        color = Color.argb(160, 0, 255, 255)
        style = Paint.Style.FILL
    }

    fun update(deltaTime: Float) {
        glowAngle += deltaTime * 5f
    }

    fun draw(canvas: Canvas) {
        if (!isActive) return
        val currentRadius = radius + kotlin.math.sin(glowAngle) * 8f
        canvas.drawCircle(x, y, currentRadius, innerPaint)
        canvas.drawCircle(x, y, currentRadius, portalPaint)

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 28f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText("بوابة الزمن (Time Portal)", x, y - radius - 15f, textPaint)
    }

    fun isPlayerInside(px: Float, py: Float): Boolean {
        if (!isActive) return false
        val dist = sqrt((px - x) * (px - x) + (py - y) * (py - y))
        return dist <= radius + 20f
    }
}
