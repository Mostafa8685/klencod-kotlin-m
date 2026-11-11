package com.clashangprpr.app.model

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.clashangprpr.app.engine.SpriteSheet
import kotlin.math.sqrt

// Player
class Player(
    context: Context,
    var x: Float,
    var y: Float
) {
    val width = 96f
    val height = 96f
    var maxHp = 160
    var hp = 160
    var maxMana = 100f
    var mana = 100f
    var speed = 280f
    var isAttacking = false
    var isDead = false
    var facingLeft = false
    var attackCooldown = 0f

    // RPG Inventory & Resources
    var coins: Int = 40
    var wood: Int = 3
    var stone: Int = 4
    var iron: Int = 2
    var bombs: Int = 2
    var potions: Int = 2
    var hasBossKey: Boolean = false
    var equippedWeapon: Weapon = GameCatalogue.WEAPON_BASIC
    var hasRoyalArmor: Boolean = false

    val idleAnim = SpriteSheet(context, "sprites/player/idle.png", 4)
    val walkAnim = SpriteSheet(context, "sprites/player/walk.png", 4)
    val attackAnim = SpriteSheet(context, "sprites/player/attack.png", 4, frameDuration = 0.08f)
    val deadAnim = SpriteSheet(context, "sprites/player/dead.png", 2)

    val hitbox = RectF()

    fun update(deltaTime: Float, moveX: Float, moveY: Float, mapW: Float, mapH: Float, walls: List<DungeonWall>) {
        if (isDead) {
            deadAnim.update(deltaTime)
            return
        }

        // Mana regeneration (+12 per second)
        mana = (mana + 12f * deltaTime).coerceAtMost(maxMana)

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
                val nextX = x + (moveX / len) * speed * deltaTime
                val nextY = y + (moveY / len) * speed * deltaTime

                // Wall Collision Check (Horizontal)
                val testHitboxX = RectF(nextX + 20f, y + 20f, nextX + width - 20f, y + height - 10f)
                var collidesX = false
                for (w in walls) {
                    if (RectF.intersects(testHitboxX, w.rect)) {
                        collidesX = true
                        break
                    }
                }
                if (!collidesX) x = nextX

                // Wall Collision Check (Vertical)
                val testHitboxY = RectF(x + 20f, nextY + 20f, x + width - 20f, nextY + height - 10f)
                var collidesY = false
                for (w in walls) {
                    if (RectF.intersects(testHitboxY, w.rect)) {
                        collidesY = true
                        break
                    }
                }
                if (!collidesY) y = nextY

                walkAnim.update(deltaTime)
                if (moveX < -0.1f) facingLeft = true
                else if (moveX > 0.1f) facingLeft = false
            } else {
                idleAnim.update(deltaTime)
            }
        }

        x = x.coerceIn(40f, (mapW - width - 40f).coerceAtLeast(40f))
        y = y.coerceIn(40f, (mapH - height - 40f).coerceAtLeast(40f))

        hitbox.set(x + 20f, y + 20f, x + width - 20f, y + height - 10f)
    }

    fun attack() {
        if (!isAttacking && attackCooldown <= 0f && !isDead) {
            isAttacking = true
            attackCooldown = 0.32f
            attackAnim.reset()
        }
    }

    fun castMagic(): Projectile? {
        if (mana >= 25f && !isDead) {
            mana -= 25f
            val vx = if (facingLeft) -600f else 600f
            val startX = if (facingLeft) x - 15f else x + width + 15f
            return Projectile(startX, y + height / 2f, vx, 0f, damage = 70, isMagic = true)
        }
        return null
    }

    fun throwBomb(): Projectile? {
        if (bombs > 0 && !isDead) {
            bombs--
            val vx = if (facingLeft) -450f else 450f
            val startX = if (facingLeft) x - 15f else x + width + 15f
            return Projectile(startX, y + height / 2f, vx, 0f, damage = 95, isMagic = false)
        }
        return null
    }

    fun usePotion(): Boolean {
        if (potions > 0 && hp < maxHp && !isDead) {
            potions--
            hp = (hp + 80).coerceAtMost(maxHp)
            return true
        }
        return false
    }

    fun takeDamage(amount: Int) {
        if (isDead) return
        val finalDmg = if (hasRoyalArmor) (amount * 0.75f).toInt() else amount
        hp -= finalDmg
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

        // Weapon Aura
        if (!isDead && equippedWeapon != GameCatalogue.WEAPON_BASIC) {
            val auraPaint = Paint().apply {
                color = equippedWeapon.color
                style = Paint.Style.STROKE
                strokeWidth = 3f
                alpha = 180
            }
            canvas.drawCircle(x + width / 2f, y + height / 2f, 38f, auraPaint)
        }
    }
}

// Enemy
class Enemy(
    context: Context,
    var x: Float,
    var y: Float,
    val isBoss: Boolean,
    val era: EraType,
    val holdsKey: Boolean = false
) {
    val width = if (isBoss) 140f else 88f
    val height = if (isBoss) 140f else 88f
    var maxHp = if (isBoss) 450 else 70
    var hp = maxHp
    var speed = if (isBoss) 130f else 170f
    var attackDmg = if (isBoss) 28 else 14
    var isDead = false
    var facingLeft = false
    var attackTimer = 0f

    val name: String = when {
        isBoss && era == EraType.ERA_1_MEDIEVAL -> "الفارس الأسود (زعيم القلعة)"
        isBoss && era == EraType.ERA_2_CAVES -> "حارس الكهوف الحجري (زعيم الدهاليز)"
        isBoss && era == EraType.ERA_3_ZOMBIE -> "الزومبي الطافر الأكبر (زعيم نهاية العالم)"
        holdsKey -> "حارس المفتاح الذهبي 🗝️"
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

        if (dist < 650f) {
            facingLeft = dx < 0
            if (dist > 70f) {
                x += (dx / dist) * speed * deltaTime
                y += (dy / dist) * speed * deltaTime
                walkAnim.update(deltaTime)
            } else {
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

        // Key indicator above the key holder enemy
        if (holdsKey && !isDead) {
            val keyPaint = Paint().apply {
                textSize = 30f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("🗝️", x + width / 2, y - 22f, keyPaint)
        }

        if (!isDead && hp < maxHp) {
            val barW = width * 0.8f
            val barH = 10f
            val barX = x + (width - barW) / 2f
            val barY = y - 15f
            val p = Paint()
            p.color = Color.BLACK
            canvas.drawRect(barX, barY, barX + barW, barY + barH, p)
            p.color = if (holdsKey) Color.YELLOW else Color.RED
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
