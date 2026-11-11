package com.clashangprpr.app.model

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.clashangprpr.app.engine.SpriteSheet
import kotlin.math.sqrt
import kotlin.random.Random

class Player(
    context: Context,
    var x: Float,
    var y: Float
) {
    val width = 96f
    val height = 96f
    var maxHp = 180
    var hp = 180
    var maxMana = 100f
    var mana = 100f
    var speed = 295f
    var isAttacking = false
    var isDashing = false
    var dashTimer = 0f
    var dashCooldown = 0f
    var isDead = false
    var facingLeft = false
    var attackCooldown = 0f
    var invincibilityTimer = 0f

    // Chrono Freeze / Slow-Mo Ability!
    var isSlowMoActive = false
    var slowMoTimer = 0f
    var slowMoCooldown = 0f

    var coins: Int = 70
    var wood: Int = 6
    var stone: Int = 6
    var iron: Int = 5
    var bombs: Int = 2
    var potions: Int = 3
    var hasBossKey: Boolean = false
    var hasPickaxe: Boolean = false
    var hasOceanGem: Boolean = false
    var hasPlasmaCannon: Boolean = false
    var hasGodBlade: Boolean = false
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

        if (slowMoCooldown > 0) slowMoCooldown -= deltaTime
        if (isSlowMoActive) {
            slowMoTimer -= deltaTime
            if (slowMoTimer <= 0f) isSlowMoActive = false
        }

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

    fun activateSlowMo(): Boolean {
        if (!isSlowMoActive && slowMoCooldown <= 0f && mana >= 30f && !isDead) {
            mana -= 30f
            isSlowMoActive = true
            slowMoTimer = 3.5f
            slowMoCooldown = 8.0f
            return true
        }
        return false
    }

    fun dash(): Boolean {
        if (!isDashing && dashCooldown <= 0f && !isDead) {
            isDashing = true
            dashTimer = 0.22f
            dashCooldown = 0.85f
            invincibilityTimer = 0.28f
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
            val vx = if (facingLeft) -700f else 700f
            val startX = if (facingLeft) x - 15f else x + width + 15f
            val dmg = if (hasGodBlade) 160 else if (hasPlasmaCannon) 120 else 85
            val isLaser = hasPlasmaCannon || hasGodBlade
            return Projectile(startX, y + height / 2f, vx, 0f, damage = dmg, isMagic = true, isEnemy = false, isLaser = isLaser)
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

        if (isDashing || isSlowMoActive) {
            val ghostPaint = Paint().apply {
                alpha = if (isSlowMoActive) 170 else 140
                if (isSlowMoActive) colorFilter = android.graphics.LightingColorFilter(0xFF00E5FF.toInt(), 0x002244)
            }
            anim.draw(canvas, x - (if (facingLeft) -25f else 25f), y, width, height, flipX = facingLeft, paint = ghostPaint)
        }

        anim.draw(canvas, x, y, width, height, flipX = facingLeft)

        if (!isDead && equippedWeapon != GameCatalogue.WEAPON_BASIC) {
            val auraPaint = Paint().apply {
                color = if (hasGodBlade) Color.parseColor("#E040FB") else equippedWeapon.color
                style = Paint.Style.STROKE
                strokeWidth = 3.5f
                alpha = 190
            }
            canvas.drawCircle(x + width / 2f, y + height / 2f, 38f, auraPaint)
        }
    }
}

enum class EnemyRole {
    MELEE, SPEARMAN, ARCHER, HEAVY_SHIELD, GHOUL_FAST, ISLAND_TRIBAL, CYBER_SHOOTER, VOID_CLONE, BOSS
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

    var isShielded: Boolean = (isBoss && era == EraType.ERA_5_CYBER)

    var maxHp = when {
        isBoss && era == EraType.ERA_6_VOID -> 1200
        isBoss && era == EraType.ERA_5_CYBER -> 950
        isBoss && era == EraType.ERA_4_ISLAND -> 850
        isBoss -> 700
        role == EnemyRole.VOID_CLONE -> 60
        role == EnemyRole.CYBER_SHOOTER -> 110
        role == EnemyRole.HEAVY_SHIELD -> 140
        role == EnemyRole.ISLAND_TRIBAL -> 95
        role == EnemyRole.ARCHER -> 80
        role == EnemyRole.SPEARMAN -> 95
        role == EnemyRole.GHOUL_FAST -> 75
        else -> 85
    }
    var hp = maxHp

    var speed = when (role) {
        EnemyRole.BOSS -> 155f
        EnemyRole.GHOUL_FAST -> 235f
        EnemyRole.ISLAND_TRIBAL -> 210f
        EnemyRole.CYBER_SHOOTER -> 175f
        EnemyRole.SPEARMAN -> 205f
        EnemyRole.ARCHER -> 160f
        EnemyRole.HEAVY_SHIELD -> 130f
        else -> 180f
    }

    var attackDmg = when {
        isBoss && era == EraType.ERA_6_VOID -> 50
        isBoss && era == EraType.ERA_5_CYBER -> 45
        isBoss && era == EraType.ERA_4_ISLAND -> 42
        isBoss -> 38
        role == EnemyRole.CYBER_SHOOTER -> 22
        role == EnemyRole.HEAVY_SHIELD -> 26
        role == EnemyRole.ISLAND_TRIBAL -> 24
        role == EnemyRole.SPEARMAN -> 22
        role == EnemyRole.ARCHER -> 18
        else -> 20
    }

    var isDead = false
    var facingLeft = false
    var attackTimer = 0f
    var shootCooldown = 0f
    var dodgeCooldown = 0f
    var teleportTimer = 0f

    val name: String = when {
        isBoss && era == EraType.ERA_6_VOID -> "سيد الفراغ الأبدي (الزعيم الأخير)"
        isBoss && era == EraType.ERA_5_CYBER -> "القائد السايبورغ (الميكا)"
        isBoss && era == EraType.ERA_4_ISLAND -> "قبطان القراصنة الأسطوري"
        isBoss && era == EraType.ERA_1_MEDIEVAL -> "الفارس الأسود (زعيم القلعة)"
        isBoss && era == EraType.ERA_2_CAVES -> "حارس الكهوف الحجري"
        isBoss && era == EraType.ERA_3_ZOMBIE -> "الزومبي الطافر الأكبر"
        role == EnemyRole.VOID_CLONE -> "نسخة سرابية من الفراغ"
        role == EnemyRole.CYBER_SHOOTER -> "جندي السايبربنك التكتيكي"
        holdsKey -> "حارس المفتاح"
        role == EnemyRole.ISLAND_TRIBAL -> "محارب الجزيرة المتوحش"
        role == EnemyRole.ARCHER -> "رامي السهام الملعون"
        role == EnemyRole.SPEARMAN -> "فارس الرمح السريع"
        role == EnemyRole.HEAVY_SHIELD -> "الفارس المدرع الثقيل"
        role == EnemyRole.GHOUL_FAST -> "زومبي زاحف سريع"
        else -> "وحش الظلام"
    }

    private val idleAnim: SpriteSheet
    private val walkAnim: SpriteSheet
    private val attackAnim: SpriteSheet
    private val deadAnim: SpriteSheet

    init {
        val folder = when {
            isBoss && era == EraType.ERA_6_VOID -> "sprites/medieval_boss"
            isBoss && era == EraType.ERA_5_CYBER -> "sprites/cyber_boss"
            isBoss && era == EraType.ERA_4_ISLAND -> "sprites/pirate_captain"
            isBoss && era == EraType.ERA_3_ZOMBIE -> "sprites/zombie_titan"
            isBoss -> "sprites/medieval_boss"
            role == EnemyRole.VOID_CLONE -> "sprites/medieval_boss"
            role == EnemyRole.CYBER_SHOOTER -> "sprites/cyber_commando"
            role == EnemyRole.ISLAND_TRIBAL -> "sprites/tribal_warrior"
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
            role == EnemyRole.CYBER_SHOOTER -> "$folder/shot.png"
            role == EnemyRole.SPEARMAN -> "$folder/attack_1.png"
            role == EnemyRole.HEAVY_SHIELD -> "$folder/attack_1.png"
            role == EnemyRole.ARCHER -> "$folder/shot.png"
            isBoss && era == EraType.ERA_5_CYBER -> "$folder/shot.png"
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

        // Slow-Mo factor: when player has slow-mo active, enemies move 70% slower!
        val effectiveDelta = if (player.isSlowMoActive) deltaTime * 0.30f else deltaTime

        if (attackTimer > 0) attackTimer -= effectiveDelta
        if (shootCooldown > 0) shootCooldown -= effectiveDelta
        if (dodgeCooldown > 0) dodgeCooldown -= effectiveDelta

        val dx = player.x - x
        val dy = player.y - y
        val dist = sqrt(dx * dx + dy * dy)

        // Void Boss Teleportation
        if (isBoss && era == EraType.ERA_6_VOID) {
            teleportTimer += effectiveDelta
            if (teleportTimer >= 4.2f && dist > 150f) {
                teleportTimer = 0f
                x = player.x + (if (Random.nextBoolean()) 350f else -350f)
                y = player.y + (if (Random.nextBoolean()) 250f else -250f)
            }
        }

        if (dist < 750f) {
            facingLeft = dx < 0

            // Monster Dodge / Dash Evade
            if (player.isAttacking && dist < 130f && !isBoss && dodgeCooldown <= 0f && Random.nextFloat() < 0.32f) {
                dodgeCooldown = 1.4f
                x -= (dx / dist) * speed * 2.3f * effectiveDelta
                y -= (dy / dist) * speed * 2.3f * effectiveDelta
                walkAnim.update(effectiveDelta * 2f)
                return
            }

            // Cyber Shooter Ranged Burst
            if (role == EnemyRole.CYBER_SHOOTER && dist < 600f) {
                if (shootCooldown <= 0f) {
                    shootCooldown = 2.0f
                    attackAnim.reset()
                    attackTimer = 0.5f
                    val pvx = (dx / dist) * 480f
                    val pvy = (dy / dist) * 480f
                    projectiles.add(Projectile(x + width / 2, y + height / 2, pvx, pvy, damage = attackDmg, isMagic = false, isEnemy = true, isLaser = true))
                }
                if (dist < 200f) {
                    x -= (dx / dist) * speed * 0.8f * effectiveDelta
                    y -= (dy / dist) * speed * 0.8f * effectiveDelta
                    walkAnim.update(effectiveDelta)
                } else {
                    idleAnim.update(effectiveDelta)
                }
                return
            }

            // Archer Ranged Attack
            if (role == EnemyRole.ARCHER && dist < 580f) {
                if (shootCooldown <= 0f) {
                    shootCooldown = 2.2f
                    attackAnim.reset()
                    attackTimer = 0.6f
                    val pvx = (dx / dist) * 420f
                    val pvy = (dy / dist) * 420f
                    projectiles.add(Projectile(x + width / 2, y + height / 2, pvx, pvy, damage = attackDmg, isMagic = false, isEnemy = true))
                }
                if (dist < 180f) {
                    x -= (dx / dist) * speed * 0.8f * effectiveDelta
                    y -= (dy / dist) * speed * 0.8f * effectiveDelta
                    walkAnim.update(effectiveDelta)
                } else {
                    idleAnim.update(effectiveDelta)
                }
                return
            }

            // Boss Attacks
            if (isBoss && shootCooldown <= 0f) {
                shootCooldown = if (era == EraType.ERA_6_VOID) 2.2f else 3.0f
                val angles = if (era == EraType.ERA_6_VOID) listOf(-0.5f, -0.2f, 0f, 0.2f, 0.5f) else listOf(-0.35f, 0f, 0.35f)
                for (ang in angles) {
                    val pvx = (dx / dist) * 410f + ang * 110f
                    val pvy = (dy / dist) * 410f + ang * 110f
                    val isLaser = (era == EraType.ERA_5_CYBER || era == EraType.ERA_6_VOID)
                    projectiles.add(Projectile(x + width / 2, y + height / 2, pvx, pvy, damage = 26, isMagic = false, isEnemy = true, isLaser = isLaser))
                }
            }

            val meleeReach = if (role == EnemyRole.SPEARMAN) 100f else 75f
            if (dist > meleeReach) {
                x += (dx / dist) * speed * effectiveDelta
                y += (dy / dist) * speed * effectiveDelta
                walkAnim.update(effectiveDelta)
            } else {
                if (attackTimer <= 0f) {
                    player.takeDamage(attackDmg)
                    attackTimer = 1.0f
                    attackAnim.reset()
                }
                attackAnim.update(effectiveDelta)
            }
        } else {
            idleAnim.update(effectiveDelta)
        }

        hitbox.set(x + 10f, y + 10f, x + width - 10f, y + height - 10f)
    }

    fun takeDamage(amount: Int): Boolean {
        if (isDead) return false
        if (isShielded) return false

        val finalDmg = if (role == EnemyRole.HEAVY_SHIELD) (amount * 0.6f).toInt() else amount
        hp -= finalDmg
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
            attackTimer > 0.6f -> attackAnim
            else -> walkAnim
        }

        if (role == EnemyRole.VOID_CLONE) {
            val clonePaint = Paint().apply {
                alpha = 150
                colorFilter = android.graphics.LightingColorFilter(0xFFE040FB.toInt(), 0x003344)
            }
            anim.draw(canvas, x, y, width, height, flipX = facingLeft, paint = clonePaint)
        } else {
            anim.draw(canvas, x, y, width, height, flipX = facingLeft)
        }

        if (isShielded && !isDead) {
            val shieldPaint = Paint().apply {
                color = Color.CYAN
                style = Paint.Style.STROKE
                strokeWidth = 5f
            }
            canvas.drawCircle(x + width / 2, y + height / 2, width * 0.65f, shieldPaint)
        }

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
                isShielded -> Color.CYAN
                holdsKey -> Color.YELLOW
                role == EnemyRole.HEAVY_SHIELD -> Color.CYAN
                role == EnemyRole.ARCHER -> Color.GREEN
                else -> Color.RED
            }
            canvas.drawRect(barX, barY, barX + (barW * (hp.toFloat() / maxHp)), barY + barH, p)
        }
    }
}
