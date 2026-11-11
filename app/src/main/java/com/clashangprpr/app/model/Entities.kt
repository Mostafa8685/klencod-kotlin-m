package com.clashangprpr.app.model

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.clashangprpr.app.engine.SpriteSheet
import kotlin.math.sqrt

class Player(
    context: Context,
    var x: Float,
    var y: Float
) {
    val width = 96f
    val height = 96f
    var maxHp = 170
    var hp = 170
    var maxMana = 100f
    var mana = 100f
    var speed = 290f
    var isAttacking = false
    var isDashing = false
    var dashTimer = 0f
    var dashCooldown = 0f
    var isDead = false
    var facingLeft = false
    var attackCooldown = 0f
    var invincibilityTimer = 0f

    var coins: Int = 60
    var wood: Int = 5
    var stone: Int = 5
    var iron: Int = 3
    var bombs: Int = 2
    var potions: Int = 3
    var hasBossKey: Boolean = false
    var equippedWeapon: Weapon = GameCatalogue.WEAPON_BASIC
    var hasRoyalArmor: Boolean = false

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

        mana = (mana + 15f * deltaTime).coerceAtMost(maxMana)

        if (attackCooldown > 0) attackCooldown -= deltaTime
        if (dashCooldown > 0) dashCooldown -= deltaTime
        if (invincibilityTimer > 0) invincibilityTimer -= deltaTime

        if (isDashing) {
            dashTimer -= deltaTime
            val dashSpeed = speed * 2.2f
            val dirX = if (facingLeft) -1f else 1f
            x += dirX * dashSpeed * deltaTime
            walkAnim.update(deltaTime * 2f)
            if (dashTimer <= 0f) {
                isDashing = false
            }
        } else if (isAttacking) {
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

        x = x.coerceIn(50f, (mapW - width - 50f).coerceAtLeast(50f))
        y = y.coerceIn(50f, (mapH - height - 50f).coerceAtLeast(50f))

        hitbox.set(x + 22f, y + 22f, x + width - 22f, y + height - 12f)
    }

    fun dash(): Boolean {
        if (!isDashing && dashCooldown <= 0f && !isDead) {
            isDashing = true
            dashTimer = 0.22f
            dashCooldown = 0.85f
            invincibilityTimer = 0.28f // i-frames!
            return true
        }
        return false
    }

    fun attack() {
        if (!isAttacking && attackCooldown <= 0f && !isDead) {
            isAttacking = true
            attackCooldown = 0.28f
            attackAnim.reset()
        }
    }

    fun castMagic(): Projectile? {
        if (mana >= 25f && !isDead) {
            mana -= 25f
            val vx = if (facingLeft) -680f else 680f
            val startX = if (facingLeft) x - 15f else x + width + 15f
            return Projectile(startX, y + height / 2f, vx, 0f, damage = 85, isMagic = true, isEnemy = false)
        }
        return null
    }

    fun throwBomb(): Projectile? {
        if (bombs > 0 && !isDead) {
            bombs--
            val vx = if (facingLeft) -500f else 500f
            val startX = if (facingLeft) x - 15f else x + width + 15f
            return Projectile(startX, y + height / 2f, vx, 0f, damage = 120, isMagic = false, isEnemy = false)
        }
        return null
    }

    fun usePotion(): Boolean {
        if (potions > 0 && hp < maxHp && !isDead) {
            potions--
            hp = (hp + 85).coerceAtMost(maxHp)
            return true
        }
        return false
    }

    fun takeDamage(amount: Int): Boolean {
        if (isDead || invincibilityTimer > 0f) return false
        val finalDmg = if (hasRoyalArmor) (amount * 0.70f).toInt() else amount
        hp -= finalDmg
        invincibilityTimer = 0.2f
        if (hp <= 0) {
            hp = 0
            isDead = true
            deadAnim.reset()
        }
        return true
    }

    fun draw(canvas: Canvas) {
        val anim = when {
            isDead -> deadAnim
            isAttacking -> attackAnim
            else -> if (idleAnim.isFinished) idleAnim else walkAnim
        }

        // Ghost trail when dashing
        if (isDashing) {
            val dashPaint = Paint().apply { alpha = 140 }
            anim.draw(canvas, x - (if (facingLeft) -25f else 25f), y, width, height, flipX = facingLeft, paint = dashPaint)
        }

        anim.draw(canvas, x, y, width, height, flipX = facingLeft)

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

enum class EnemyRole {
    MELEE, SPEARMAN, ARCHER, HEAVY_SHIELD, GHOUL_FAST, BOSS
}

class Enemy(
    context: Context,
    var x: Float,
    var y: Float,
    val role: EnemyRole,
    val era: EraType,
    val holdsKey: Boolean = false
) {
    val isBoss: Boolean = (role == EnemyRole.BOSS)
    val width = if (isBoss) 150f else 88f
    val height = if (isBoss) 150f else 88f

    var maxHp = when (role) {
        EnemyRole.BOSS -> 700
        EnemyRole.HEAVY_SHIELD -> 130
        EnemyRole.ARCHER -> 75
        EnemyRole.SPEARMAN -> 90
        EnemyRole.GHOUL_FAST -> 70
        else -> 80
    }
    var hp = maxHp

    var speed = when (role) {
        EnemyRole.BOSS -> 145f
        EnemyRole.GHOUL_FAST -> 230f
        EnemyRole.SPEARMAN -> 200f
        EnemyRole.ARCHER -> 160f
        EnemyRole.HEAVY_SHIELD -> 130f
        else -> 180f
    }

    var attackDmg = when (role) {
        EnemyRole.BOSS -> 38
        EnemyRole.HEAVY_SHIELD -> 24
        EnemyRole.SPEARMAN -> 22
        EnemyRole.ARCHER -> 16
        else -> 18
    }

    var isDead = false
    var facingLeft = false
    var attackTimer = 0f
    var shootCooldown = 0f

    val name: String = when {
        isBoss && era == EraType.ERA_1_MEDIEVAL -> "الفارس الأسود (زعيم القلعة)"
        isBoss && era == EraType.ERA_2_CAVES -> "حارس الكهوف الحجري (زعيم الدهاليز)"
        isBoss && era == EraType.ERA_3_ZOMBIE -> "الزومبي الطافر الأكبر (زعيم نهاية العالم)"
        holdsKey -> "حارس المفتاح الذهبي"
        role == EnemyRole.ARCHER -> "رامي السهام الملعون"
        role == EnemyRole.SPEARMAN -> "فارس الرمح السريع"
        role == EnemyRole.HEAVY_SHIELD -> "الفارس المدرع الثقيل"
        role == EnemyRole.GHOUL_FAST -> "زومبي سريع متوحش"
        else -> "وحش الظلام"
    }

    private val idleAnim: SpriteSheet
    private val walkAnim: SpriteSheet
    private val attackAnim: SpriteSheet
    private val deadAnim: SpriteSheet

    init {
        val folder = when {
            isBoss && era == EraType.ERA_1_MEDIEVAL -> "sprites/medieval_boss"
            isBoss && era == EraType.ERA_3_ZOMBIE -> "sprites/zombie_titan"
            isBoss -> "sprites/medieval_boss"
            role == EnemyRole.SPEARMAN -> "sprites/spearman"
            role == EnemyRole.ARCHER -> "sprites/archer"
            role == EnemyRole.HEAVY_SHIELD -> "sprites/heavy_knight"
            role == EnemyRole.GHOUL_FAST -> "sprites/zombie_ghoul"
            era == EraType.ERA_3_ZOMBIE -> "sprites/zombie_walker"
            else -> "sprites/player"
        }

        idleAnim = SpriteSheet(context, "$folder/idle.png", 4)
        walkAnim = SpriteSheet(context, "$folder/walk.png", 4)

        val attackFile = when {
            role == EnemyRole.SPEARMAN -> "$folder/attack_1.png"
            role == EnemyRole.HEAVY_SHIELD -> "$folder/attack_1.png"
            role == EnemyRole.ARCHER -> "$folder/shot.png"
            isBoss -> "$folder/attack.png"
            else -> "$folder/attack.png"
        }
        attackAnim = SpriteSheet(context, attackFile, 4, frameDuration = 0.1f)
        deadAnim = SpriteSheet(context, "$folder/dead.png", 2)
    }

    val hitbox = RectF()

    fun update(deltaTime: Float, player: Player, projectiles: MutableList<Projectile>) {
        if (isDead) {
            deadAnim.update(deltaTime)
            return
        }

        if (attackTimer > 0) attackTimer -= deltaTime
        if (shootCooldown > 0) shootCooldown -= deltaTime

        val dx = player.x - x
        val dy = player.y - y
        val dist = sqrt(dx * dx + dy * dy)

        if (dist < 750f) {
            facingLeft = dx < 0

            // Archer AI: Keeps distance and shoots arrows!
            if (role == EnemyRole.ARCHER && dist < 580f) {
                if (shootCooldown <= 0f) {
                    shootCooldown = 2.2f
                    attackAnim.reset()
                    attackTimer = 0.6f
                    val pvx = (dx / dist) * 420f
                    val pvy = (dy / dist) * 420f
                    projectiles.add(Projectile(x + width / 2, y + height / 2, pvx, pvy, damage = attackDmg, isMagic = false, isEnemy = true))
                }
                // Back away if player gets too close
                if (dist < 180f) {
                    x -= (dx / dist) * speed * 0.8f * deltaTime
                    y -= (dy / dist) * speed * 0.8f * deltaTime
                    walkAnim.update(deltaTime)
                } else {
                    idleAnim.update(deltaTime)
                }
                return
            }

            // Boss Shockwave Burst when Enraged (HP < 50%)
            if (isBoss && hp < maxHp / 2 && shootCooldown <= 0f) {
                shootCooldown = 3.2f
                val angles = listOf(-0.35f, 0f, 0.35f)
                for (ang in angles) {
                    val pvx = (dx / dist) * 390f + ang * 100f
                    val pvy = (dy / dist) * 390f + ang * 100f
                    projectiles.add(Projectile(x + width / 2, y + height / 2, pvx, pvy, damage = 25, isMagic = false, isEnemy = true))
                }
            }

            val meleeReach = if (role == EnemyRole.SPEARMAN) 100f else 75f
            if (dist > meleeReach) {
                x += (dx / dist) * speed * deltaTime
                y += (dy / dist) * speed * deltaTime
                walkAnim.update(deltaTime)
            } else {
                if (attackTimer <= 0f) {
                    player.takeDamage(attackDmg)
                    attackTimer = 1.0f
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
        // Heavy shield blocks 40% frontal damage
        val finalDmg = if (role == EnemyRole.HEAVY_SHIELD) (amount * 0.6f).toInt() else amount
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
            attackTimer > 0.6f -> attackAnim
            else -> walkAnim
        }
        anim.draw(canvas, x, y, width, height, flipX = facingLeft)

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
            p.color = when {
                holdsKey -> Color.YELLOW
                role == EnemyRole.HEAVY_SHIELD -> Color.CYAN
                role == EnemyRole.ARCHER -> Color.GREEN
                else -> Color.RED
            }
            canvas.drawRect(barX, barY, barX + (barW * (hp.toFloat() / maxHp)), barY + barH, p)
        }
    }
}
