package com.clashangprpr.app.model

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LightingColorFilter
import android.graphics.Paint
import android.graphics.RectF
import com.clashangprpr.app.engine.CollisionSystem
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
    var hitFlashTimer = 0f
    var attackSwingId = 0
    var comboStep = 0
    var comboResetTimer = 0f
    var isWhirlwindActive = false

    // Chrono Freeze / Slow-Mo Ability
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
    var hasGun: Boolean = true
    var isInvisible: Boolean = false
    var invisTimer: Float = 0f
    var equippedWeapon: Weapon = GameCatalogue.WEAPON_BASIC
    var hasRoyalArmor: Boolean = false
    var hasNanoArmor: Boolean = false

    val idleAnim = SpriteSheet(context, "sprites/player/idle.png", 4)
    val walkAnim = SpriteSheet(context, "sprites/player/walk.png", 4)
    val attackAnim = SpriteSheet(context, "sprites/player/attack.png", 4, frameDuration = 0.08f)
    val deadAnim = SpriteSheet(context, "sprites/player/dead.png", 2)

    val hitbox = RectF()
    private val shadowRect = RectF()
    private var isMoving = false

    private val shadowPaint = Paint().apply {
        color = Color.argb(115, 0, 0, 0)
        style = Paint.Style.FILL
    }
    private val hitFlashPaint = Paint().apply {
        colorFilter = LightingColorFilter(0xFFFFFFFF.toInt(), 0x00FF5555)
    }
    private val dashGhostPaint = Paint().apply { alpha = 140 }
    private val invisPaint = Paint().apply { alpha = 85 }
    private val slowMoGhostPaint = Paint().apply {
        alpha = 170
        colorFilter = LightingColorFilter(0xFF00E5FF.toInt(), 0x002244)
    }
    private val auraPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
        alpha = 190
    }

    fun recalculateMaxHp(skillBonusHp: Int = 0) {
        val armorBonus = when {
            hasNanoArmor -> 140
            hasRoyalArmor -> 70
            else -> 0
        }
        maxHp = 180 + armorBonus + skillBonusHp
        if (hp > maxHp) hp = maxHp
    }

    fun update(
        deltaTime: Float,
        moveX: Float,
        moveY: Float,
        mapW: Float,
        mapH: Float,
        obstacles: List<RectF>,
        speedMult: Float = 1.0f
    ) {
        if (isDead) {
            deadAnim.update(deltaTime)
            return
        }

        mana = (mana + 15f * deltaTime).coerceAtMost(maxMana)

        if (attackCooldown > 0) attackCooldown -= deltaTime
        if (comboResetTimer > 0) {
            comboResetTimer -= deltaTime
            if (comboResetTimer <= 0f) comboStep = 0
        }
        if (dashCooldown > 0) dashCooldown -= deltaTime
        if (invincibilityTimer > 0) invincibilityTimer -= deltaTime
        if (hitFlashTimer > 0) hitFlashTimer -= deltaTime

        if (slowMoCooldown > 0) slowMoCooldown -= deltaTime
        if (isSlowMoActive) {
            slowMoTimer -= deltaTime
            if (slowMoTimer <= 0f) isSlowMoActive = false
        }
        if (isInvisible) {
            invisTimer -= deltaTime
            if (invisTimer <= 0f) isInvisible = false
        }

        var dx = 0f
        var dy = 0f
        val effectiveSpeed = speed * speedMult

        if (isDashing) {
            dashTimer -= deltaTime
            val dashSpeed = effectiveSpeed * 2.2f
            val dirX = if (facingLeft) -1f else 1f
            dx = dirX * dashSpeed * deltaTime
            walkAnim.update(deltaTime * 2f)
            isMoving = true
            if (dashTimer <= 0f) {
                isDashing = false
            }
        } else if (isAttacking) {
            attackAnim.update(deltaTime)
            isMoving = false
            if (attackAnim.isFinished) {
                isAttacking = false
                attackAnim.reset()
            }
        } else {
            val len = sqrt(moveX * moveX + moveY * moveY)
            if (len > 0.1f) {
                isMoving = true
                dx = (moveX / len) * effectiveSpeed * deltaTime
                dy = (moveY / len) * effectiveSpeed * deltaTime
                walkAnim.update(deltaTime)
                if (moveX < -0.1f) facingLeft = true
                else if (moveX > 0.1f) facingLeft = false
            } else {
                isMoving = false
                idleAnim.update(deltaTime)
            }
        }

        if (dx != 0f || dy != 0f) {
            val moved = CollisionSystem.moveWithCollision(
                x, y, dx, dy,
                22f, 22f, width - 44f, height - 34f,
                width, height, obstacles, mapW, mapH
            )
            x = moved.first
            y = moved.second
        } else {
            x = x.coerceIn(54f, (mapW - width - 54f).coerceAtLeast(54f))
            y = y.coerceIn(54f, (mapH - height - 54f).coerceAtLeast(54f))
        }

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

    fun attack(): Boolean {
        if (!isAttacking && attackCooldown <= 0f && !isDead) {
            isAttacking = true
            comboStep = (comboStep + 1) % 3
            isWhirlwindActive = (comboStep == 2)
            comboResetTimer = 1.25f
            attackCooldown = if (isWhirlwindActive) 0.36f else 0.24f
            attackSwingId++
            attackAnim.reset()
            return true
        }
        return false
    }

    fun activateInvisibility(): Boolean {
        if (!isInvisible && !isDead) {
            isInvisible = true
            invisTimer = 12.0f
            return true
        }
        return false
    }

    fun shootAimedGun(enemies: List<Enemy>, moveX: Float, moveY: Float, dmgMult: Float = 1.0f): Projectile? {
        if (!hasGun || isDead || mana < 8f) return null
        mana -= 8f
        val cx = x + width / 2f
        val cy = y + height / 2f
        var bestEnemy: Enemy? = null
        var bestDist = 680f
        for (i in enemies.indices) {
            val e = enemies[i]
            if (e.isDead) continue
            val ex = e.x + e.width / 2f
            val ey = e.y + e.height / 2f
            val d = sqrt((ex - cx) * (ex - cx) + (ey - cy) * (ey - cy))
            if (d < bestDist) {
                bestDist = d
                bestEnemy = e
            }
        }
        val dirX: Float
        val dirY: Float
        if (bestEnemy != null) {
            val dx = (bestEnemy.x + bestEnemy.width / 2f) - cx
            val dy = (bestEnemy.y + bestEnemy.height / 2f) - cy
            val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
            dirX = dx / len
            dirY = dy / len
            facingLeft = dirX < 0
        } else {
            val mLen = sqrt(moveX * moveX + moveY * moveY)
            if (mLen > 0.1f) {
                dirX = moveX / mLen
                dirY = moveY / mLen
            } else {
                dirX = if (facingLeft) -1f else 1f
                dirY = 0f
            }
        }
        val dmg = (55 * dmgMult).toInt()
        return Projectile(cx + dirX * 36f, cy + dirY * 36f, dirX * 820f, dirY * 820f, damage = dmg, isMagic = false, isEnemy = false, isLaser = true)
    }

    fun castMagic(dmgMult: Float = 1.0f): Projectile? {
        if (mana >= 25f && !isDead) {
            mana -= 25f
            val vx = if (facingLeft) -700f else 700f
            val startX = if (facingLeft) x - 15f else x + width + 15f
            val baseDmg = when {
                hasGodBlade -> 160
                hasPlasmaCannon -> 120
                else -> 85
            }
            val dmg = (baseDmg * dmgMult).toInt()
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
            return Projectile(startX, y + height / 2f, vx, 0f, damage = 120, isMagic = false, isEnemy = false, isBomb = true)
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
        val dmgMult = when {
            hasNanoArmor -> 0.55f
            hasRoyalArmor -> 0.70f
            else -> 1.0f
        }
        val finalDmg = (amount * dmgMult).toInt().coerceAtLeast(1)
        hp -= finalDmg
        invincibilityTimer = 0.22f
        hitFlashTimer = 0.12f
        if (hp <= 0) {
            hp = 0
            isDead = true
            deadAnim.reset()
        }
        return true
    }

    fun draw(canvas: Canvas) {
        if (!isDead) {
            shadowRect.set(x + 20f, y + height - 18f, x + width - 20f, y + height - 2f)
            canvas.drawOval(shadowRect, shadowPaint)
        }

        val anim = when {
            isDead -> deadAnim
            isAttacking -> attackAnim
            isMoving -> walkAnim
            else -> idleAnim
        }

        if (isDashing || isSlowMoActive) {
            val p = if (isSlowMoActive) slowMoGhostPaint else dashGhostPaint
            anim.draw(canvas, x - (if (facingLeft) -25f else 25f), y, width, height, flipX = facingLeft, paint = p)
        }

        val mainPaint = when {
            hitFlashTimer > 0f -> hitFlashPaint
            isInvisible -> invisPaint
            else -> null
        }
        anim.draw(canvas, x, y, width, height, flipX = facingLeft, paint = mainPaint)

        if (!isDead && equippedWeapon != GameCatalogue.WEAPON_BASIC) {
            auraPaint.color = if (hasGodBlade) Color.parseColor("#E040FB") else equippedWeapon.color
            canvas.drawCircle(x + width / 2f, y + height / 2f, 38f, auraPaint)
        }
    }
}

enum class EnemyRole {
    MELEE, SPEARMAN, ARCHER, HEAVY_SHIELD, GHOUL_FAST, ISLAND_TRIBAL, CYBER_SHOOTER, VOID_CLONE, MAGE, ASSASSIN, GANG_MEMBER, GANG_BOSS, PRISON_GUARD, SECRET_ORDER_MAGE, BOSS
}

enum class AIState {
    IDLE, PATROL, CHASE, ATTACK, DEAD
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

    var aiState: AIState = AIState.PATROL
    private val homeX = x
    private val homeY = y
    private var patrolDir = 1f
    private var patrolTimer = Random.nextFloat() * 2f
    private val flankSign = if (Random.nextBoolean()) 1f else -1f
    var alertedTimer: Float = 0f

    var isShielded: Boolean = (isBoss && era == EraType.ERA_5_CYBER)
    var lastHitSwingId: Int = -1
    var hitFlashTimer: Float = 0f
    var knockbackVx: Float = 0f
    var knockbackVy: Float = 0f

    // Boss Telegraph Slam Attack
    var isTelegraphingSlam: Boolean = false
    var slamTargetX: Float = 0f
    var slamTargetY: Float = 0f
    var slamTimer: Float = 0f
    var slamCooldown: Float = 4.5f
    val slamRadius: Float = 145f

    var maxHp = when {
        isBoss && era == EraType.ERA_2_OPEN_WORLD -> 2800
        isBoss && era == EraType.ERA_6_VOID -> 1200
        isBoss && era == EraType.ERA_5_CYBER -> 950
        isBoss && era == EraType.ERA_4_ISLAND -> 850
        isBoss -> 700
        role == EnemyRole.VOID_CLONE -> 65
        role == EnemyRole.ASSASSIN -> 90
        role == EnemyRole.MAGE -> 85
        role == EnemyRole.GANG_BOSS -> 420
        role == EnemyRole.SECRET_ORDER_MAGE -> 180
        role == EnemyRole.PRISON_GUARD -> 160
        role == EnemyRole.GANG_MEMBER -> 120
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
        EnemyRole.BOSS -> 160f
        EnemyRole.ASSASSIN -> 260f
        EnemyRole.GHOUL_FAST -> 235f
        EnemyRole.ISLAND_TRIBAL -> 210f
        EnemyRole.CYBER_SHOOTER -> 175f
        EnemyRole.SPEARMAN -> 205f
        EnemyRole.MAGE -> 150f
        EnemyRole.ARCHER -> 160f
        EnemyRole.HEAVY_SHIELD -> 130f
        else -> 180f
    }

    var attackDmg = when {
        isBoss && era == EraType.ERA_2_OPEN_WORLD -> 62
        isBoss && era == EraType.ERA_6_VOID -> 50
        isBoss && era == EraType.ERA_5_CYBER -> 45
        isBoss && era == EraType.ERA_4_ISLAND -> 42
        isBoss -> 38
        role == EnemyRole.GANG_BOSS -> 44
        role == EnemyRole.SECRET_ORDER_MAGE -> 34
        role == EnemyRole.PRISON_GUARD -> 30
        role == EnemyRole.GANG_MEMBER -> 26
        role == EnemyRole.ASSASSIN -> 28
        role == EnemyRole.MAGE -> 25
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
        role == EnemyRole.ASSASSIN -> "قاتل الظلال السريع"
        role == EnemyRole.GANG_BOSS -> "زعيم العصابة المسلحة"
        role == EnemyRole.GANG_MEMBER -> "عضو عصابة"
        role == EnemyRole.PRISON_GUARD -> "سجّان الحصن"
        role == EnemyRole.SECRET_ORDER_MAGE -> "حارس المحفل السري"
        role == EnemyRole.MAGE -> "ساحر الشظية"
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
    private val shadowRect = RectF()

    private val shadowPaint = Paint().apply {
        color = Color.argb(110, 0, 0, 0)
        style = Paint.Style.FILL
    }
    private val hitFlashPaint = Paint().apply {
        colorFilter = LightingColorFilter(0xFFFFFFFF.toInt(), 0x00FFFFFF)
    }
    private val clonePaint = Paint().apply {
        alpha = 160
        colorFilter = LightingColorFilter(0xFFE040FB.toInt(), 0x003344)
    }
    private val shieldPaint = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    private val telegraphFillPaint = Paint().apply {
        color = Color.argb(80, 255, 23, 68)
        style = Paint.Style.FILL
    }
    private val telegraphRingPaint = Paint().apply {
        color = Color.parseColor("#FF1744")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val keyPaint = Paint().apply {
        color = Color.YELLOW
        textSize = 24f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val hpBgPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }
    private val hpBarPaint = Paint().apply {
        style = Paint.Style.FILL
    }

    init {
        val folder = when {
            isBoss && era == EraType.ERA_6_VOID -> "sprites/void_overlord"
            isBoss && era == EraType.ERA_5_CYBER -> "sprites/cyber_boss"
            isBoss && era == EraType.ERA_4_ISLAND -> "sprites/pirate_captain"
            isBoss && era == EraType.ERA_3_ZOMBIE -> "sprites/zombie_titan"
            isBoss && era == EraType.ERA_2_CAVES -> "sprites/cave_golem_boss"
            isBoss -> "sprites/medieval_boss"
            role == EnemyRole.VOID_CLONE -> "sprites/void_clone"
            role == EnemyRole.GANG_BOSS -> "sprites/pirate_captain"
            role == EnemyRole.GANG_MEMBER -> "sprites/cyber_commando"
            role == EnemyRole.PRISON_GUARD -> "sprites/heavy_knight"
            role == EnemyRole.SECRET_ORDER_MAGE -> "sprites/void_clone"
            role == EnemyRole.MAGE -> "sprites/chronomancer"
            role == EnemyRole.ASSASSIN -> "sprites/guard_elite"
            holdsKey && (era == EraType.ERA_1_MEDIEVAL || era == EraType.ERA_2_CAVES) -> "sprites/guard_elite"
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
            role == EnemyRole.CYBER_SHOOTER || role == EnemyRole.GANG_MEMBER -> "$folder/shot.png"
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

    fun applyKnockback(fromX: Float, fromY: Float, force: Float = 360f) {
        if (isBoss) return
        val cx = x + width / 2f
        val cy = y + height / 2f
        val dx = cx - fromX
        val dy = cy - fromY
        val d = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        knockbackVx = (dx / d) * force
        knockbackVy = (dy / d) * force
    }

    fun update(deltaTime: Float, player: Player, projectiles: MutableList<Projectile>, obstacles: List<RectF>) {
        if (hitFlashTimer > 0f) hitFlashTimer -= deltaTime

        if (isDead) {
            aiState = AIState.DEAD
            isTelegraphingSlam = false
            deadAnim.update(deltaTime)
            return
        }

        val mapW = era.mapWidth
        val mapH = era.mapHeight

        if (kotlin.math.abs(knockbackVx) > 5f || kotlin.math.abs(knockbackVy) > 5f) {
            val moved = CollisionSystem.moveWithCollision(
                x, y, knockbackVx * deltaTime, knockbackVy * deltaTime,
                10f, 10f, width - 20f, height - 20f, width, height, obstacles, mapW, mapH
            )
            x = moved.first
            y = moved.second
            knockbackVx *= 0.82f
            knockbackVy *= 0.82f
        }

        val effectiveDelta = if (player.isSlowMoActive) deltaTime * 0.30f else deltaTime

        if (attackTimer > 0) attackTimer -= effectiveDelta
        if (shootCooldown > 0) shootCooldown -= effectiveDelta
        if (dodgeCooldown > 0) dodgeCooldown -= effectiveDelta

        val dx = player.x - x
        val dy = player.y - y
        val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)

        // Boss Telegraph Slam logic
        if (isBoss) {
            if (isTelegraphingSlam) {
                slamTimer -= effectiveDelta
                if (slamTimer <= 0f) {
                    isTelegraphingSlam = false
                    val px = player.x + player.width / 2f
                    val py = player.y + player.height / 2f
                    val sDist = sqrt((px - slamTargetX) * (px - slamTargetX) + (py - slamTargetY) * (py - slamTargetY))
                    if (sDist <= slamRadius) {
                        player.takeDamage(attackDmg + 15)
                    }
                }
            } else {
                slamCooldown -= effectiveDelta
                if (slamCooldown <= 0f && dist < 420f) {
                    slamCooldown = 6.0f
                    isTelegraphingSlam = true
                    slamTimer = 0.95f
                    slamTargetX = player.x + player.width / 2f
                    slamTargetY = player.y + player.height / 2f
                }
            }
        }

        // Void Boss Teleportation
        if (isBoss && era == EraType.ERA_6_VOID) {
            teleportTimer += effectiveDelta
            if (teleportTimer >= 4.2f && dist > 150f) {
                teleportTimer = 0f
                val targetX = (player.x + (if (Random.nextBoolean()) 320f else -320f)).coerceIn(80f, mapW - width - 80f)
                val targetY = (player.y + (if (Random.nextBoolean()) 220f else -220f)).coerceIn(80f, mapH - height - 80f)
                if (!CollisionSystem.isPointBlocked(targetX + width / 2, targetY + height / 2, obstacles, mapW, mapH)) {
                    x = targetX
                    y = targetY
                }
            }
        }

        if (alertedTimer > 0f) alertedTimer -= effectiveDelta
        val aggroRange = if (alertedTimer > 0f) 1500f else if (isBoss) 1150f else 880f
        if (!player.isInvisible && dist < aggroRange) {
            aiState = AIState.CHASE
            facingLeft = dx < 0

            // Assassin Blink Dash behind/around player
            if (role == EnemyRole.ASSASSIN && dodgeCooldown <= 0f && dist < 240f) {
                dodgeCooldown = 2.5f
                val mx = -(dy / dist) * speed * 1.8f * effectiveDelta * (if (Random.nextBoolean()) 1f else -1f)
                val my = (dx / dist) * speed * 1.8f * effectiveDelta
                val moved = CollisionSystem.moveWithCollision(
                    x, y, mx, my, 10f, 10f, width - 20f, height - 20f, width, height, obstacles, mapW, mapH
                )
                x = moved.first
                y = moved.second
            }

            // Monster Dodge
            if (player.isAttacking && dist < 130f && !isBoss && dodgeCooldown <= 0f && Random.nextFloat() < 0.30f) {
                dodgeCooldown = 1.4f
                val mx = -(dx / dist) * speed * 2.2f * effectiveDelta
                val my = -(dy / dist) * speed * 2.2f * effectiveDelta
                val moved = CollisionSystem.moveWithCollision(
                    x, y, mx, my, 10f, 10f, width - 20f, height - 20f, width, height, obstacles, mapW, mapH
                )
                x = moved.first
                y = moved.second
                walkAnim.update(effectiveDelta * 2f)
                hitbox.set(x + 10f, y + 10f, x + width - 10f, y + height - 10f)
                return
            }

            // Ranged Roles: CYBER_SHOOTER, ARCHER, MAGE
            if ((role == EnemyRole.CYBER_SHOOTER || role == EnemyRole.ARCHER || role == EnemyRole.MAGE || role == EnemyRole.GANG_MEMBER || role == EnemyRole.SECRET_ORDER_MAGE) && dist < 640f) {
                aiState = AIState.ATTACK
                if (shootCooldown <= 0f) {
                    shootCooldown = if (role == EnemyRole.MAGE) 2.4f else 2.0f
                    attackAnim.reset()
                    attackTimer = 0.55f
                    val spd = if (role == EnemyRole.CYBER_SHOOTER) 480f else 420f
                    val pvx = (dx / dist) * spd
                    val pvy = (dy / dist) * spd
                    val isLas = (role == EnemyRole.CYBER_SHOOTER)
                    val isMag = (role == EnemyRole.MAGE)
                    projectiles.add(Projectile(x + width / 2, y + height / 2, pvx, pvy, damage = attackDmg, isMagic = isMag, isEnemy = true, isLaser = isLas))
                }
                if (dist < 200f) {
                    val mx = -(dx / dist) * speed * 0.8f * effectiveDelta
                    val my = -(dy / dist) * speed * 0.8f * effectiveDelta
                    val moved = CollisionSystem.moveWithCollision(
                        x, y, mx, my, 10f, 10f, width - 20f, height - 20f, width, height, obstacles, mapW, mapH
                    )
                    x = moved.first
                    y = moved.second
                    walkAnim.update(effectiveDelta)
                } else {
                    idleAnim.update(effectiveDelta)
                }
                hitbox.set(x + 10f, y + 10f, x + width - 10f, y + height - 10f)
                return
            }

            // Boss Ranged Spread
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

            val meleeReach = if (role == EnemyRole.SPEARMAN) 100f else if (isBoss) 110f else 75f
            if (dist > meleeReach) {
                val flankFactor = if (dist in 140f..420f && !isBoss) 0.35f * flankSign else 0f
                val mx = ((dx / dist) - (dy / dist) * flankFactor) * speed * effectiveDelta
                val my = ((dy / dist) + (dx / dist) * flankFactor) * speed * effectiveDelta
                val moved = CollisionSystem.moveWithCollision(
                    x, y, mx, my, 10f, 10f, width - 20f, height - 20f, width, height, obstacles, mapW, mapH
                )
                x = moved.first
                y = moved.second
                walkAnim.update(effectiveDelta)
            } else {
                aiState = AIState.ATTACK
                if (attackTimer <= 0f) {
                    player.takeDamage(attackDmg)
                    attackTimer = 1.0f
                    attackAnim.reset()
                }
                attackAnim.update(effectiveDelta)
            }
        } else {
            // Patrol state around home position
            aiState = AIState.PATROL
            patrolTimer += effectiveDelta
            if (patrolTimer >= 2.2f) {
                patrolTimer = 0f
                patrolDir = -patrolDir
            }
            facingLeft = patrolDir < 0
            val moved = CollisionSystem.moveWithCollision(
                x, y, patrolDir * speed * 0.35f * effectiveDelta, 0f,
                10f, 10f, width - 20f, height - 20f, width, height, obstacles, mapW, mapH
            )
            x = moved.first
            y = moved.second
            if (kotlin.math.abs(x - homeX) > 150f) patrolDir = if (x > homeX) -1f else 1f
            walkAnim.update(effectiveDelta * 0.7f)
        }

        hitbox.set(x + 10f, y + 10f, x + width - 10f, y + height - 10f)
    }

    fun takeDamage(amount: Int): Boolean {
        if (isDead || isShielded) return false
        val finalDmg = if (role == EnemyRole.HEAVY_SHIELD) (amount * 0.6f).toInt() else amount
        hp -= finalDmg
        hitFlashTimer = 0.11f
        if (hp <= 0) {
            hp = 0
            isDead = true
            isTelegraphingSlam = false
            deadAnim.reset()
        }
        return true
    }

    fun draw(canvas: Canvas) {
        if (isBoss && isTelegraphingSlam && !isDead) {
            val progress = (1f - (slamTimer / 0.95f)).coerceIn(0f, 1f)
            canvas.drawCircle(slamTargetX, slamTargetY, slamRadius, telegraphRingPaint)
            canvas.drawCircle(slamTargetX, slamTargetY, slamRadius * progress, telegraphFillPaint)
        }

        if (!isDead) {
            shadowRect.set(x + 16f, y + height - 16f, x + width - 16f, y + height - 2f)
            canvas.drawOval(shadowRect, shadowPaint)
        }

        val anim = when {
            isDead -> deadAnim
            attackTimer > 0.6f -> attackAnim
            else -> walkAnim
        }

        val activePaint = when {
            hitFlashTimer > 0f -> hitFlashPaint
            role == EnemyRole.VOID_CLONE -> clonePaint
            else -> null
        }

        anim.draw(canvas, x, y, width, height, flipX = facingLeft, paint = activePaint)

        if (isShielded && !isDead) {
            canvas.drawCircle(x + width / 2, y + height / 2, width * 0.65f, shieldPaint)
        }

        if (holdsKey && !isDead) {
            canvas.drawText("مفتاح", x + width / 2, y - 24f, keyPaint)
        }

        if (!isDead) {
            val barW = width * 0.8f
            val barH = 10f
            val barX = x + (width - barW) / 2f
            val barY = y - 15f
            canvas.drawRect(barX, barY, barX + barW, barY + barH, hpBgPaint)
            hpBarPaint.color = when {
                isShielded -> Color.CYAN
                holdsKey -> Color.YELLOW
                role == EnemyRole.HEAVY_SHIELD -> Color.CYAN
                role == EnemyRole.ARCHER || role == EnemyRole.MAGE -> Color.GREEN
                else -> Color.RED
            }
            canvas.drawRect(barX, barY, barX + (barW * (hp.toFloat() / maxHp)), barY + barH, hpBarPaint)
        }
    }
}
