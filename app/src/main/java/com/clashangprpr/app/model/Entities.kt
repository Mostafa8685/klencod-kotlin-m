package com.clashangprpr.app.model

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LightingColorFilter
import android.graphics.Paint
import android.graphics.RectF
import com.clashangprpr.app.engine.Assets
import com.clashangprpr.app.engine.SaveStore
import com.clashangprpr.app.engine.SpriteSheet
import com.clashangprpr.app.engine.Ui
import com.clashangprpr.app.world.Body
import com.clashangprpr.app.world.Collider
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.hypot
import kotlin.math.sqrt

private object Fx {
    val white = Paint().apply { colorFilter = LightingColorFilter(0x000000, 0xFFFFFF) }
    val red = Paint().apply { colorFilter = LightingColorFilter(0xFFFFFF, 0x703030) }
    val blue = Paint().apply { colorFilter = LightingColorFilter(0xB0C8FF, 0x101840) }
    val gold = Paint().apply { colorFilter = LightingColorFilter(0xFFE082, 0x302000) }
    val purple = Paint().apply { colorFilter = LightingColorFilter(0xE080FF, 0x380048) }
    val bomberOrange = Paint().apply { colorFilter = LightingColorFilter(0xFFFFAA, 0x652000) }
    val ghost = Paint().apply { alpha = 120 }
    val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(85, 0, 0, 0) }
    val slash = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 8f; strokeCap = Paint.Cap.ROUND }
    val bar = Paint(Paint.ANTI_ALIAS_FLAG)
    val oval = RectF()
}

class Player(override var x: Float, override var y: Float) : Body {
    companion object {
        const val DASH_CD = 6.0f
        const val PARRY_CD = 2.4f
        const val PARRY_DUR = 0.48f
        const val GRAPPLE_CD = 3.2f
        const val GRAPPLE_DUR = 0.26f
        const val SLOW_CD = 9f
        const val SLOW_DUR = 3.5f
        const val MAGIC_CD = 0.42f
        const val BOMB_CD = 0.6f
        const val MAGIC_COST = 20f
        const val SLOW_COST = 30f
    }

    val width = 96f
    val height = 96f
    override val fOffX = 33f
    override val fOffY = 66f
    override val fW = 30f
    override val fH = 22f

    var hp = 180
    var maxHp = 180
    var mana = 100f
    var maxMana = 100
    var coins = 95
    var wood = 14
    var stone = 14
    var iron = 5
    var wool = 3
    var gunpowder = 12
    var treeSaplings = 5
    var cropSeeds = 6
    var wheat = 3
    var bombs = 2
    var potions = 2
    var speedPots = 1
    var stew = 2
    var goldKeys = 1
    var treasureMaps = 1
    var bedsItem = 1
    var furnacesItem = 1
    var cannonsItem = 1
    var campfiresItem = 1
    var storageChestsItem = 1
    var houseBlueprints = 1
    var fortBlueprints = 1
    var towerBlueprints = 1
    var farmBlueprints = 1

    // Storage Silo / Chest balances (الشونة وصندوق التخزين لتفريغ الشنطة)
    var stWood = 20
    var stStone = 20
    var stIron = 6
    var stWool = 4
    var stGunpowder = 10
    var stWheat = 4
    var stSeeds = 4
    var stSaplings = 4
    var stBlkWood = 16
    var stBlkStone = 16

    // Minecraft & Fortnite-style building block counts
    var blkWood = 20
    var blkStone = 18
    var blkBridge = 12
    var blkDoor = 3
    var blkFence = 8
    var blkTorch = 8
    var blkGlass = 6
    var blkTnt = 2
    var blkWindow = 6
    var blkLaunchPad = 2
    var blkIronGate = 2
    var blkObsidian = 6
    var blkRoyalFloor = 8
    var blkSpikes = 6
    var blkWire = 6
    var blkBeacon = 2
    var golemsItem = 1
    var hasWateringCan = true
    var manaBerries = 3
    var firePeppers = 2
    /** 0 = None, 1 = Lifesteal (مصاص الدماء), 2 = Chain Lightning (البرق المتسلسل) */
    var weaponEnchant = 0
    /** 0 = Single Block (بلوك فردي), 1 = 5-Block Wall Line (سور خطي 5), 2 = 5x5 Room (غرفة 5×5) */
    var buildBrushMode = 0

    /** Selected slot index (0..8) in the bottom Minecraft Hotbar */
    var hotbarIdx = 0

    // Skill-based Fortnite/Minecraft combat & Endgame progression fields
    var parryTimer = 0f
    var parryCd = 0f
    var grappleTimer = 0f
    var grappleCd = 0f
    var grappleStartX = 0f
    var grappleStartY = 0f
    var grappleTargetX = 0f
    var grappleTargetY = 0f
    var airTimer = 0f
    private val airDuration = 0.76f
    var justLandedSlam = false
    var launchPadCd = 0f

    var comboCount = 0
    var comboTimer = 0f
    var maxCombo = 0

    var abyssalSeals = 0
    var sealMask = 0
    var zombiesKilled = 0
    var bloodMoonsCleared = 0
    var playSeconds = 0f
    var colossalBossDefeated = false

    val mats = IntArray(6)
    var weaponLvl = 0
    var armorLvl = 0
    var helmetTier = 0
    var chestTier = 0
    var legsTier = 0
    var bootsTier = 0
    var apples = 3
    var rawMeat = 2
    var cookedMeat = 2
    var hasBossKey = false
    var hasPickaxe = true
    var hasOceanGem = false
    var hasPlasma = false
    var hasBoat = false
    var inBoat = false
    var spellIdx = 0
    val owned = HashSet<String>()
    var weapon: Weapon = GameCatalogue.WEAPON_BASIC
    var armorId = ""
    var upgHp = 0
    var upgMana = 0

    var aimX = 1f
    var aimY = 0f
    var facingLeft = false
    var moving = false
    var isDead = false
    var isAttacking = false
    var swingTime = 0f
    var attackCd = 0f
    val swingHit = HashSet<Any>()
    var isDashing = false
    var dashTimer = 0f
    var dashSpeedAfter = 0f
    var dashCd = 0f
    var slowTimer = 0f
    var slowCd = 0f
    var magicCd = 0f
    var bombCd = 0f
    var invincible = 0f
    var hurtTimer = 0f
    var speedBuffTimer = 0f
    var visionBuffTimer = 0f
    var shieldBuffTimer = 0f

    private val idleA = SpriteSheet("sprites/player/idle.png", 0.2f)
    private val walkA = SpriteSheet("sprites/player/walk.png", 0.11f)
    private val attackA = SpriteSheet("sprites/player/attack.png", 0.07f, false)
    private val deadA = SpriteSheet("sprites/player/dead.png", 0.25f, false)

    val cx: Float get() = x + width / 2f
    val cy: Float get() = y + 60f
    val hitbox = RectF()
    val currentSpell: SpellType get() = SpellType.values()[spellIdx.coerceIn(0, SpellType.values().size - 1)]

    init {
        owned.add("sword_basic")
        owned.add("dagger_silver")
        recomputeStats()
        hp = maxHp
    }

    val bagMax: Int = 240
    fun bagUsed(): Int {
        return wood + stone + iron + wool + gunpowder + treeSaplings + cropSeeds + wheat +
            apples + rawMeat + cookedMeat + stew + blkWood + blkStone + blkBridge + blkFence + blkTorch + blkWindow
    }

    /** Transfers excess bag items into the Storage Silo (الشونة وصندوق التخزين) so the bag is emptied! */
    fun emptyBagToStorage(): Int {
        var moved = 0
        fun xfer(cur: Int, keep: Int): Pair<Int, Int> {
            val send = max(0, cur - keep)
            moved += send
            return Pair(cur - send, send)
        }
        var p = xfer(wood, 4); wood = p.first; stWood += p.second
        p = xfer(stone, 4); stone = p.first; stStone += p.second
        p = xfer(iron, 2); iron = p.first; stIron += p.second
        p = xfer(wool, 1); wool = p.first; stWool += p.second
        p = xfer(gunpowder, 5); gunpowder = p.first; stGunpowder += p.second
        p = xfer(wheat, 1); wheat = p.first; stWheat += p.second
        p = xfer(cropSeeds, 2); cropSeeds = p.first; stSeeds += p.second
        p = xfer(treeSaplings, 2); treeSaplings = p.first; stSaplings += p.second
        p = xfer(blkWood, 8); blkWood = p.first; stBlkWood += p.second
        p = xfer(blkStone, 8); blkStone = p.first; stBlkStone += p.second
        return moved
    }

    fun withdrawAllFromStorage(): Int {
        val total = stWood + stStone + stIron + stWool + stGunpowder + stWheat + stSeeds + stSaplings + stBlkWood + stBlkStone
        wood += stWood; stWood = 0
        stone += stStone; stStone = 0
        iron += stIron; stIron = 0
        wool += stWool; stWool = 0
        gunpowder += stGunpowder; stGunpowder = 0
        wheat += stWheat; stWheat = 0
        cropSeeds += stSeeds; stSeeds = 0
        treeSaplings += stSaplings; stSaplings = 0
        blkWood += stBlkWood; stBlkWood = 0
        blkStone += stBlkStone; stBlkStone = 0
        return total
    }

    /** Cycles through all owned weapons for instant mid-combat weapon switching! */
    fun switchNextWeapon(): Weapon {
        val ownedWeapons = GameCatalogue.weapons.filter { owned.contains(it.id) }
        if (ownedWeapons.isEmpty()) return weapon
        val curIdx = ownedWeapons.indexOfFirst { it.id == weapon.id }
        val nextIdx = (curIdx + 1) % ownedWeapons.size
        weapon = ownedWeapons[nextIdx]
        return weapon
    }

    fun addCombo(pts: Int = 1) {
        comboCount = min(50, comboCount + pts)
        if (comboCount > maxCombo) maxCombo = comboCount
        comboTimer = 4.6f
    }

    fun skillRank(): String = when {
        comboCount >= 20 -> "SS أسطوري 🔥"
        comboCount >= 14 -> "S خارق ⚡"
        comboCount >= 9 -> "A محترف ⚔️"
        comboCount >= 5 -> "B مهاري"
        comboCount >= 2 -> "C سريع"
        else -> ""
    }

    fun comboDmgMul(): Float = 1f + min(0.85f, comboCount * 0.04f)
    fun comboSpdMul(): Float = 1f + min(0.32f, comboCount * 0.016f)

    fun unlockSeal(idx: Int): Boolean {
        if (idx !in 0..6) return false
        val bit = 1 shl idx
        if ((sealMask and bit) != 0) return false
        sealMask = sealMask or bit
        abyssalSeals = Integer.bitCount(sealMask).coerceIn(0, 7)
        return true
    }

    fun readinessPercent(): Int {
        val sealScore = (abyssalSeals * 42) / 7
        val wepScore = (weaponLvl * 18) / CraftingRegistry.MAX_UPG
        val armScore = (armorLvl * 16) / CraftingRegistry.MAX_UPG
        val hpScore = (upgHp * 10) / 8
        val mythicScore = (if (owned.contains("abyss_breaker") || owned.contains("godblade")) 8 else 0) +
            (if (owned.contains("armor_titan") || owned.contains("armor_nano")) 6 else 0)
        return (sealScore + wepScore + armScore + hpScore + mythicScore).coerceIn(0, 100)
    }

    fun blockCountFor(b: BlockType): Int = when (b) {
        BlockType.WOOD_WALL -> blkWood
        BlockType.STONE_BRICK -> blkStone
        BlockType.WOOD_BRIDGE -> blkBridge
        BlockType.DOOR_CLOSED -> blkDoor
        BlockType.FENCE -> blkFence
        BlockType.TORCH -> blkTorch
        BlockType.GLASS_BLOCK -> blkGlass
        BlockType.TNT_BLOCK -> blkTnt
        BlockType.FIRING_WINDOW -> blkWindow
        BlockType.LAUNCH_PAD -> blkLaunchPad
        BlockType.IRON_GATE -> blkIronGate
        BlockType.OBSIDIAN_WALL -> blkObsidian
        BlockType.ROYAL_FLOOR -> blkRoyalFloor
        BlockType.SPIKE_TRAP -> blkSpikes
        BlockType.BARRICADE_WIRE -> blkWire
        BlockType.HEALING_BEACON -> blkBeacon
    }

    fun addBlockCount(b: BlockType, delta: Int) {
        when (b) {
            BlockType.WOOD_WALL -> blkWood = max(0, blkWood + delta)
            BlockType.STONE_BRICK -> blkStone = max(0, blkStone + delta)
            BlockType.WOOD_BRIDGE -> blkBridge = max(0, blkBridge + delta)
            BlockType.DOOR_CLOSED -> blkDoor = max(0, blkDoor + delta)
            BlockType.FENCE -> blkFence = max(0, blkFence + delta)
            BlockType.TORCH -> blkTorch = max(0, blkTorch + delta)
            BlockType.GLASS_BLOCK -> blkGlass = max(0, blkGlass + delta)
            BlockType.TNT_BLOCK -> blkTnt = max(0, blkTnt + delta)
            BlockType.FIRING_WINDOW -> blkWindow = max(0, blkWindow + delta)
            BlockType.LAUNCH_PAD -> blkLaunchPad = max(0, blkLaunchPad + delta)
            BlockType.IRON_GATE -> blkIronGate = max(0, blkIronGate + delta)
            BlockType.OBSIDIAN_WALL -> blkObsidian = max(0, blkObsidian + delta)
            BlockType.ROYAL_FLOOR -> blkRoyalFloor = max(0, blkRoyalFloor + delta)
            BlockType.SPIKE_TRAP -> blkSpikes = max(0, blkSpikes + delta)
            BlockType.BARRICADE_WIRE -> blkWire = max(0, blkWire + delta)
            BlockType.HEALING_BEACON -> blkBeacon = max(0, blkBeacon + delta)
        }
    }

    fun selectedBlockType(): BlockType? = when (hotbarIdx) {
        2 -> BlockType.WOOD_WALL
        3 -> BlockType.STONE_BRICK
        4 -> BlockType.FIRING_WINDOW
        5 -> BlockType.SPIKE_TRAP
        6 -> BlockType.OBSIDIAN_WALL
        7 -> BlockType.BARRICADE_WIRE
        8 -> BlockType.TNT_BLOCK
        else -> null
    }

    fun armorReduction(): Float {
        val piecesRed = helmetTier * 0.03f + chestTier * 0.05f + legsTier * 0.04f + bootsTier * 0.02f
        val base = (GameCatalogue.armorById(armorId)?.reduction ?: 0f) + armorLvl * 0.02f + piecesRed + (if (shieldBuffTimer > 0f) 0.25f else 0f)
        return min(0.80f, base)
    }

    fun recomputeStats() {
        val bonus = GameCatalogue.armorById(armorId)?.hpBonus ?: 0
        val piecesHp = helmetTier * 35 + chestTier * 50 + legsTier * 40 + bootsTier * 25
        maxHp = 180 + bonus + piecesHp + upgHp * 35 + armorLvl * 30
        maxMana = 100 + upgMana * 30
        hp = min(hp, maxHp)
        mana = min(mana, maxMana.toFloat())
    }

    fun equipBest() {
        var best = GameCatalogue.WEAPON_BASIC
        for (w in GameCatalogue.weapons) if (owned.contains(w.id) && w.damage > best.damage) best = w
        weapon = best
    }

    fun resetState() {
        isDead = false
        isAttacking = false
        isDashing = false
        parryTimer = 0f
        parryCd = 0f
        grappleTimer = 0f
        grappleCd = 0f
        airTimer = 0f
        justLandedSlam = false
        slowTimer = 0f
        slowCd = 0f
        dashCd = 0f
        dashSpeedAfter = 0f
        attackCd = 0f
        magicCd = 0f
        bombCd = 0f
        hurtTimer = 0f
        invincible = 1.5f
        hasBossKey = false
        hasOceanGem = false
        refillAll()
    }

    fun refillAll() {
        hp = maxHp
        mana = maxMana.toFloat()
    }

    val hasGodblade: Boolean get() = owned.contains("godblade") || owned.contains("abyss_breaker")

    fun activateParry(): Boolean {
        if (isDead || parryCd > 0f || parryTimer > 0f) return false
        parryTimer = PARRY_DUR
        parryCd = PARRY_CD
        return true
    }

    fun startGrapple(tx: Float, ty: Float): Boolean {
        if (isDead || grappleCd > 0f || grappleTimer > 0f) return false
        grappleStartX = cx
        grappleStartY = cy
        grappleTargetX = tx
        grappleTargetY = ty
        grappleTimer = GRAPPLE_DUR
        grappleCd = GRAPPLE_CD
        invincible = max(invincible, 0.35f)
        addCombo(1)
        return true
    }

    fun triggerLaunchPad() {
        if (isDead || airTimer > 0f || launchPadCd > 0f) return
        airTimer = airDuration
        launchPadCd = 1.4f
        invincible = max(invincible, airDuration + 0.15f)
        addCombo(2)
    }

    fun update(dt: Float, mx: Float, my: Float, world: Collider) {
        if (isDead) {
            deadA.update(dt)
            return
        }
        playSeconds += dt
        if (attackCd > 0f) attackCd -= dt
        if (dashCd > 0f) dashCd -= dt
        if (dashSpeedAfter > 0f) dashSpeedAfter -= dt
        if (parryTimer > 0f) parryTimer -= dt
        if (parryCd > 0f) parryCd -= dt
        if (grappleCd > 0f) grappleCd -= dt
        if (launchPadCd > 0f) launchPadCd -= dt
        if (slowCd > 0f) slowCd -= dt
        if (magicCd > 0f) magicCd -= dt
        if (bombCd > 0f) bombCd -= dt
        if (invincible > 0f) invincible -= dt
        if (hurtTimer > 0f) hurtTimer -= dt
        if (slowTimer > 0f) slowTimer -= dt
        if (speedBuffTimer > 0f) speedBuffTimer -= dt
        if (visionBuffTimer > 0f) visionBuffTimer -= dt
        if (shieldBuffTimer > 0f) shieldBuffTimer -= dt
        if (comboTimer > 0f) {
            comboTimer -= dt
            if (comboTimer <= 0f) comboCount = 0
        }
        mana = min(maxMana.toFloat(), mana + 17f * dt)

        val len = sqrt(mx * mx + my * my)
        moving = len > 0.1f
        if (moving) {
            aimX = mx / len
            aimY = my / len
            if (abs(mx) > 0.2f) facingLeft = mx < 0f
        }

        if (grappleTimer > 0f) {
            grappleTimer -= dt
            val dx = grappleTargetX - cx
            val dy = grappleTargetY - cy
            val d = sqrt(dx * dx + dy * dy)
            if (d > 18f) {
                world.move(this, (dx / d) * 1050f * dt, (dy / d) * 1050f * dt)
            } else {
                grappleTimer = 0f
            }
        } else if (airTimer > 0f) {
            airTimer -= dt
            val airSpd = 460f * comboSpdMul()
            val dirX = if (moving) mx else aimX * 0.7f
            val dirY = if (moving) my else aimY * 0.7f
            world.move(this, dirX * airSpd * dt, dirY * airSpd * dt)
            if (airTimer <= 0f) {
                airTimer = 0f
                justLandedSlam = true
            }
        } else if (isDashing) {
            dashTimer -= dt
            world.move(this, aimX * 740f * dt, aimY * 740f * dt)
            if (dashTimer <= 0f) {
                isDashing = false
                dashSpeedAfter = 1.6f
            }
        } else if (moving) {
            var spd = 260f * (1f + bootsTier * 0.06f) * (if (isAttacking) 0.72f else 1f) * comboSpdMul()
            if (dashSpeedAfter > 0f) spd *= 1.38f
            if (speedBuffTimer > 0f) spd *= 1.35f
            if (inBoat) spd *= 1.32f
            world.move(this, mx * spd * dt, my * spd * dt)
        }
        if (isAttacking) {
            swingTime += dt
            attackA.update(dt)
            if (swingTime >= 0.28f) isAttacking = false
        }
        idleA.update(dt)
        walkA.update(dt)
        hitbox.set(x + 28f, y + 26f, x + 68f, y + 88f)
    }

    val inHitWindow: Boolean get() = isAttacking && swingTime in 0.05f..0.22f

    fun attackRect(out: RectF) {
        val r = weapon.reach
        val ax = cx + aimX * r * 0.60f
        val ay = cy + aimY * r * 0.60f
        val half = r * 0.64f
        out.set(ax - half, ay - half, ax + half, ay + half)
    }

    fun attack(): Boolean {
        if (isDead || isAttacking || attackCd > 0f || isDashing) return false
        isAttacking = true
        swingTime = 0f
        attackCd = weapon.cooldown / comboSpdMul().coerceAtMost(1.25f)
        swingHit.clear()
        attackA.reset()
        return true
    }

    fun dash(): Boolean {
        if (isDead || isDashing || dashCd > 0f) return false
        isDashing = true
        dashTimer = 0.26f
        dashCd = DASH_CD
        invincible = max(invincible, 0.42f)
        addCombo(1)
        return true
    }

    fun slowmo(): Boolean {
        if (isDead || slowCd > 0f || slowTimer > 0f || mana < SLOW_COST) return false
        mana -= SLOW_COST
        slowTimer = SLOW_DUR
        slowCd = SLOW_CD
        return true
    }

    fun nextSpell() {
        spellIdx = (spellIdx + 1) % SpellType.values().size
    }

    fun castSpell(projList: MutableList<Projectile>): Boolean {
        val sp = currentSpell
        if (isDead || magicCd > 0f || mana < sp.manaCost) return false
        mana -= sp.manaCost
        magicCd = sp.cooldown
        val god = hasGodblade
        val baseDmg = ((if (owned.contains("abyss_breaker")) 230 else if (god) 170 else if (hasPlasma) 128 else 84) + weaponLvl * 14) * comboDmgMul()
        val dmgInt = baseDmg.toInt()
        addCombo(1)
        when (sp) {
            SpellType.ARCANE_BOLT -> {
                val kind = if (god || hasPlasma) Projectile.LASER else Projectile.MAGIC
                projList.add(Projectile(cx + aimX * 30f, cy + aimY * 30f, aimX * 680f, aimY * 680f, dmgInt, kind, true))
            }
            SpellType.FIRE_NOVA -> {
                for (i in 0 until 8) {
                    val a = i * (Math.PI.toFloat() * 2f / 8f)
                    projList.add(Projectile(cx, cy, cos(a) * 490f, sin(a) * 490f, (dmgInt * 0.85f).toInt(), Projectile.FIREBALL, true))
                }
            }
            SpellType.FROST_HEAL -> {
                hp = min(maxHp, hp + 48)
                shieldBuffTimer = 8f
                for (i in 0 until 6) {
                    val a = i * (Math.PI.toFloat() * 2f / 6f)
                    projList.add(Projectile(cx, cy, cos(a) * 430f, sin(a) * 430f, (dmgInt * 0.65f).toInt(), Projectile.MAGIC, true))
                }
            }
            SpellType.VOID_LASER -> {
                projList.add(Projectile(cx + aimX * 30f, cy + aimY * 30f, aimX * 790f, aimY * 790f, (dmgInt * 1.38f).toInt(), Projectile.LASER, true))
            }
        }
        return true
    }

    fun throwBomb(): Projectile? {
        if (isDead || bombs <= 0 || bombCd > 0f) return null
        bombs--
        bombCd = BOMB_CD
        return Projectile(cx + aimX * 30f, cy + aimY * 30f, aimX * 430f, aimY * 430f, 150, Projectile.BOMB, true)
    }

    fun usePotion(): Boolean {
        if (isDead || potions <= 0 || hp >= maxHp) return false
        potions--
        hp = min(maxHp, hp + 90)
        return true
    }

    fun useSpeedPotion(): Boolean {
        if (isDead || speedPots <= 0) return false
        speedPots--
        speedBuffTimer = 45f
        visionBuffTimer = 60f
        return true
    }

    fun takeDamage(amount: Int): Boolean {
        if (isDead || invincible > 0f || airTimer > 0f) return false
        if (parryTimer > 0f) {
            parryTimer = 0f
            parryCd = 0.5f
            dashCd = 0f
            invincible = 0.65f
            mana = min(maxMana.toFloat(), mana + 28f)
            addCombo(3)
            return false
        }
        val dmg = max(1, (amount * (1f - armorReduction())).toInt())
        hp -= dmg
        invincible = 0.5f
        hurtTimer = 0.22f
        comboCount = max(0, comboCount - 3)
        if (hp <= 0) {
            hp = 0
            isDead = true
            deadA.reset()
        }
        return true
    }

    fun cooldownFrac(cd: Float, total: Float): Float = if (cd > 0f) (cd / total).coerceIn(0f, 1f) else 0f

    fun draw(c: Canvas, time: Float) {
        if (grappleTimer > 0f && !isDead) {
            Fx.slash.color = Color.parseColor("#00E5FF")
            Fx.slash.strokeWidth = 6f
            c.drawLine(cx, cy - 12f, grappleTargetX, grappleTargetY, Fx.slash)
            Fx.bar.style = Paint.Style.FILL
            Fx.bar.color = Color.WHITE
            c.drawCircle(grappleTargetX, grappleTargetY, 8f, Fx.bar)
            Fx.slash.strokeWidth = 8f
        }

        val airLift = if (airTimer > 0f) {
            val prog = (airTimer / airDuration).coerceIn(0f, 1f)
            sin(prog * Math.PI.toFloat()) * 92f
        } else 0f

        if (inBoat && airLift <= 0f) {
            Fx.bar.style = Paint.Style.FILL
            Fx.bar.color = Color.argb(120, 0, 188, 212)
            Fx.oval.set(x + 8f, y + 66f, x + 88f, y + 98f)
            c.drawOval(Fx.oval, Fx.bar)
            Fx.bar.color = Color.rgb(121, 85, 72)
            Fx.oval.set(x + 12f, y + 62f, x + 84f, y + 94f)
            c.drawRoundRect(Fx.oval, 16f, 16f, Fx.bar)
            Fx.bar.color = Color.rgb(161, 136, 127)
            Fx.oval.set(x + 18f, y + 66f, x + 78f, y + 88f)
            c.drawRoundRect(Fx.oval, 12f, 12f, Fx.bar)
        } else {
            Fx.oval.set(x + 26f, y + 76f, x + 70f, y + 92f)
            c.drawOval(Fx.oval, Fx.shadow)
            if (airTimer > 0f) {
                Fx.slash.color = Color.parseColor("#00E5FF")
                Fx.slash.strokeWidth = 3.5f
                c.drawCircle(cx, cy + 20f, 36f, Fx.slash)
                Fx.slash.strokeWidth = 8f
            }
        }
        var paint: Paint? = null
        if (hurtTimer > 0f) paint = Fx.white
        else if (parryTimer > 0f) paint = Fx.gold
        else if (isDashing || airTimer > 0f) paint = Fx.ghost
        else if (slowTimer > 0f) paint = Fx.blue
        else if (speedBuffTimer > 0f && ((time * 10f).toInt() % 2 == 0)) paint = Fx.gold
        else if (invincible > 0f && ((time * 18f).toInt() % 2 == 0)) paint = Fx.ghost
        val sheet = when {
            isDead -> deadA
            isAttacking -> attackA
            moving || isDashing || airTimer > 0f -> walkA
            else -> idleA
        }
        sheet.draw(c, x, y - airLift, width, height, facingLeft, paint)

        if (parryTimer > 0f && !isDead) {
            Fx.slash.color = Color.parseColor("#FFD54F")
            Fx.slash.alpha = 235
            Fx.slash.strokeWidth = 6f
            c.drawCircle(cx, cy - 6f - airLift, 48f, Fx.slash)
            Fx.slash.color = Color.parseColor("#00E5FF")
            Fx.slash.strokeWidth = 3f
            c.drawCircle(cx, cy - 6f - airLift, 54f, Fx.slash)
            Fx.slash.strokeWidth = 8f
        }
        if (shieldBuffTimer > 0f && !isDead) {
            Fx.slash.color = Color.parseColor("#69F0AE")
            Fx.slash.alpha = (140 + 70 * sin(time * 6f)).toInt().coerceIn(0, 255)
            Fx.slash.strokeWidth = 4f
            c.drawCircle(cx, cy - 6f - airLift, 44f, Fx.slash)
            Fx.slash.strokeWidth = 8f
        }
        if (isAttacking && !isDead) {
            val p = (swingTime / 0.28f).coerceIn(0f, 1f)
            val ang = Math.toDegrees(atan2(aimY.toDouble(), aimX.toDouble())).toFloat()
            val rad = weapon.reach * 0.84f
            Fx.oval.set(cx - rad, cy - rad - airLift, cx + rad, cy + rad - airLift)
            Fx.slash.color = weapon.color
            Fx.slash.alpha = (235 * (1f - p)).toInt().coerceIn(0, 255)
            c.drawArc(Fx.oval, ang - 72f, 144f * min(1f, p * 2.2f), false, Fx.slash)
        }
    }

    // -------------------------------------------------------- persistence
    fun save(eraIndex: Int) {
        val m = HashMap<String, Any>()
        m["era"] = eraIndex
        m["mode"] = "era"
        m["coins"] = coins
        m["wood"] = wood
        m["stone"] = stone
        m["iron"] = iron
        m["wool"] = wool
        m["powder"] = gunpowder
        m["saplings"] = treeSaplings
        m["cSeeds"] = cropSeeds
        m["wheat"] = wheat
        m["stChestI"] = storageChestsItem
        m["houseBp"] = houseBlueprints
        m["fortBp"] = fortBlueprints
        m["towerBp"] = towerBlueprints
        m["farmBp"] = farmBlueprints
        m["stWood"] = stWood
        m["stStone"] = stStone
        m["stIron"] = stIron
        m["stWool"] = stWool
        m["stPowder"] = stGunpowder
        m["stWheat"] = stWheat
        m["stSeeds"] = stSeeds
        m["stSap"] = stSaplings
        m["stBWood"] = stBlkWood
        m["stBStone"] = stBlkStone
        m["bombs"] = bombs
        m["potions"] = potions
        m["speedPots"] = speedPots
        m["stew"] = stew
        m["goldKeys"] = goldKeys
        m["tMaps"] = treasureMaps
        m["bedsI"] = bedsItem
        m["furnI"] = furnacesItem
        m["canI"] = cannonsItem
        m["campI"] = campfiresItem
        m["bWood"] = blkWood
        m["bStone"] = blkStone
        m["bBridge"] = blkBridge
        m["bDoor"] = blkDoor
        m["bFence"] = blkFence
        m["bTorch"] = blkTorch
        m["bGlass"] = blkGlass
        m["bTnt"] = blkTnt
        m["bWin"] = blkWindow
        m["bPad"] = blkLaunchPad
        m["bIGate"] = blkIronGate
        m["bObs"] = blkObsidian
        m["bRoy"] = blkRoyalFloor
        m["bSpk"] = blkSpikes
        m["bWir"] = blkWire
        m["bBcn"] = blkBeacon
        m["golemsI"] = golemsItem
        m["wCan"] = hasWateringCan
        m["mBer"] = manaBerries
        m["fPep"] = firePeppers
        m["wEnch"] = weaponEnchant
        m["bBrush"] = buildBrushMode
        m["seals"] = abyssalSeals
        m["sealMask"] = sealMask
        m["zKills"] = zombiesKilled
        m["bMoons"] = bloodMoonsCleared
        m["playSec"] = playSeconds
        m["cBossDef"] = colossalBossDefeated
        m["boat"] = hasBoat
        m["spell"] = spellIdx
        m["owned"] = owned.joinToString(",")
        m["armor"] = armorId
        m["upgHp"] = upgHp
        m["upgMana"] = upgMana
        m["mats"] = mats.joinToString(",")
        m["wLvl"] = weaponLvl
        m["apples"] = apples
        m["rawm"] = rawMeat
        m["cookm"] = cookedMeat
        m["aLvl"] = armorLvl
        m["helmT"] = helmetTier
        m["chestT"] = chestTier
        m["legsT"] = legsTier
        m["bootsT"] = bootsTier
        m["pickaxe"] = hasPickaxe
        m["plasma"] = hasPlasma
        SaveStore.putAll(m)
    }

    fun load(): Int {
        coins = SaveStore.int("coins", 95)
        wood = SaveStore.int("wood", 14)
        stone = SaveStore.int("stone", 14)
        iron = SaveStore.int("iron", 5)
        wool = SaveStore.int("wool", 3)
        gunpowder = SaveStore.int("powder", 12)
        treeSaplings = SaveStore.int("saplings", 5)
        cropSeeds = SaveStore.int("cSeeds", 6)
        wheat = SaveStore.int("wheat", 3)
        storageChestsItem = SaveStore.int("stChestI", 1)
        houseBlueprints = SaveStore.int("houseBp", 1)
        fortBlueprints = SaveStore.int("fortBp", 1)
        towerBlueprints = SaveStore.int("towerBp", 1)
        farmBlueprints = SaveStore.int("farmBp", 1)
        stWood = SaveStore.int("stWood", 20)
        stStone = SaveStore.int("stStone", 20)
        stIron = SaveStore.int("stIron", 6)
        stWool = SaveStore.int("stWool", 4)
        stGunpowder = SaveStore.int("stPowder", 10)
        stWheat = SaveStore.int("stWheat", 4)
        stSeeds = SaveStore.int("stSeeds", 4)
        stSaplings = SaveStore.int("stSap", 4)
        stBlkWood = SaveStore.int("stBWood", 16)
        stBlkStone = SaveStore.int("stBStone", 16)
        bombs = SaveStore.int("bombs", 2)
        potions = SaveStore.int("potions", 2)
        speedPots = SaveStore.int("speedPots", 1)
        stew = SaveStore.int("stew", 2)
        goldKeys = SaveStore.int("goldKeys", 1)
        treasureMaps = SaveStore.int("tMaps", 1)
        bedsItem = SaveStore.int("bedsI", 1)
        furnacesItem = SaveStore.int("furnI", 1)
        cannonsItem = SaveStore.int("canI", 1)
        campfiresItem = SaveStore.int("campI", 1)
        blkWood = SaveStore.int("bWood", 20)
        blkStone = SaveStore.int("bStone", 18)
        blkBridge = SaveStore.int("bBridge", 12)
        blkDoor = SaveStore.int("bDoor", 3)
        blkFence = SaveStore.int("bFence", 8)
        blkTorch = SaveStore.int("bTorch", 8)
        blkGlass = SaveStore.int("bGlass", 6)
        blkTnt = SaveStore.int("bTnt", 2)
        blkWindow = SaveStore.int("bWin", 6)
        blkLaunchPad = SaveStore.int("bPad", 2)
        blkIronGate = SaveStore.int("bIGate", 2)
        blkObsidian = SaveStore.int("bObs", 6)
        blkRoyalFloor = SaveStore.int("bRoy", 8)
        blkSpikes = SaveStore.int("bSpk", 6)
        blkWire = SaveStore.int("bWir", 6)
        blkBeacon = SaveStore.int("bBcn", 2)
        golemsItem = SaveStore.int("golemsI", 1)
        hasWateringCan = SaveStore.bool("wCan", true)
        manaBerries = SaveStore.int("mBer", 3)
        firePeppers = SaveStore.int("fPep", 2)
        weaponEnchant = SaveStore.int("wEnch", 0)
        buildBrushMode = SaveStore.int("bBrush", 0)
        sealMask = SaveStore.int("sealMask", 0)
        abyssalSeals = SaveStore.int("seals", Integer.bitCount(sealMask))
        zombiesKilled = SaveStore.int("zKills", 0)
        bloodMoonsCleared = SaveStore.int("bMoons", 0)
        playSeconds = SaveStore.float("playSec", 0f)
        colossalBossDefeated = SaveStore.bool("cBossDef", false)
        hasBoat = SaveStore.bool("boat", false)
        spellIdx = SaveStore.int("spell", 0)
        owned.clear()
        owned.add("sword_basic")
        owned.add("dagger_silver")
        for (s in SaveStore.str("owned", "sword_basic,dagger_silver").split(",")) if (s.isNotEmpty()) owned.add(s)
        armorId = SaveStore.str("armor", "")
        upgHp = SaveStore.int("upgHp", 0)
        upgMana = SaveStore.int("upgMana", 0)
        val ms = SaveStore.str("mats", "0,0,0,0,0,0").split(",")
        for (i in 0 until 6) mats[i] = if (i < ms.size) (ms[i].toIntOrNull() ?: 0) else 0
        weaponLvl = SaveStore.int("wLvl", 0)
        apples = SaveStore.int("apples", 3)
        rawMeat = SaveStore.int("rawm", 2)
        cookedMeat = SaveStore.int("cookm", 2)
        armorLvl = SaveStore.int("aLvl", 0)
        helmetTier = SaveStore.int("helmT", 0)
        chestTier = SaveStore.int("chestT", 0)
        legsTier = SaveStore.int("legsT", 0)
        bootsTier = SaveStore.int("bootsT", 0)
        hasPickaxe = SaveStore.bool("pickaxe", true)
        hasPlasma = SaveStore.bool("plasma", false)
        hasBossKey = false
        hasOceanGem = false
        equipBest()
        recomputeStats()
        refillAll()
        isDead = false
        return SaveStore.int("era", 0)
    }
}

// ------------------------------------------------------------------ Sheep (الخراف)

class Sheep(override var x: Float, override var y: Float) : Body {
    override val fOffX = 12f
    override val fOffY = 28f
    override val fW = 36f
    override val fH = 22f

    var hp = 3
    var isDead = false
    var woolReady = true
    private var regrowTimer = 0f
    private var moveTimer = 2f
    private var vx = 0f
    private var vy = 0f
    private var fleeTimer = 0f
    private var flash = 0f
    private var bob = (x * 0.02f)
    val hitbox = RectF()
    val cx: Float get() = x + 30f
    val cy: Float get() = y + 30f

    fun hit(fromX: Float, fromY: Float): Boolean {
        if (isDead) return false
        hp--
        flash = 0.16f
        fleeTimer = 3.5f
        val dx = cx - fromX
        val dy = cy - fromY
        val l = max(1f, sqrt(dx * dx + dy * dy))
        vx = dx / l * 190f
        vy = dy / l * 190f
        if (hp <= 0) isDead = true
        return true
    }

    fun update(dt: Float, world: Collider) {
        if (isDead) return
        bob += dt * 6f
        if (flash > 0f) flash -= dt
        if (!woolReady) {
            regrowTimer += dt
            if (regrowTimer > 45f) {
                regrowTimer = 0f
                woolReady = true
            }
        }
        if (fleeTimer > 0f) {
            fleeTimer -= dt
            world.move(this, vx * dt, vy * dt)
        } else {
            moveTimer -= dt
            if (moveTimer <= 0f) {
                moveTimer = 1.8f + (abs(x.toInt()) % 3)
                if ((bob.toInt() % 3) == 0) {
                    vx = 0f
                    vy = 0f
                } else {
                    val a = (bob * 3.7f) % 6.283f
                    vx = cos(a) * 58f
                    vy = sin(a) * 58f
                }
            }
            if (vx != 0f || vy != 0f) world.move(this, vx * dt, vy * dt)
        }
        hitbox.set(x + 6f, y + 6f, x + 54f, y + 54f)
    }

    fun draw(c: Canvas) {
        if (isDead) return
        val b = if (vx != 0f || vy != 0f) sin(bob) * 2.5f else 0f
        Fx.oval.set(x + 10f, y + 42f, x + 50f, y + 54f)
        c.drawOval(Fx.oval, Fx.shadow)
        val p = Fx.bar
        p.style = Paint.Style.FILL
        p.color = Color.rgb(62, 39, 35)
        c.drawRect(x + 16f, y + 36f + b, x + 21f, y + 48f, p)
        c.drawRect(x + 38f, y + 36f - b, x + 43f, y + 48f, p)
        p.color = if (flash > 0f) Color.rgb(255, 180, 180) else if (woolReady) Color.rgb(245, 245, 250) else Color.rgb(215, 204, 200)
        Fx.oval.set(x + 10f, y + 14f + b, x + 48f, y + 42f + b)
        c.drawOval(Fx.oval, p)
        if (woolReady) {
            c.drawCircle(x + 22f, y + 18f + b, 11f, p)
            c.drawCircle(x + 34f, y + 18f + b, 11f, p)
            c.drawCircle(x + 28f, y + 26f + b, 12f, p)
        }
        val headX = if (vx < -2f) x + 10f else x + 46f
        p.color = Color.rgb(78, 52, 46)
        c.drawCircle(headX, y + 24f + b, 9f, p)
        p.color = Color.WHITE
        c.drawCircle(headX + (if (vx < -2f) -3f else 3f), y + 22f + b, 2.2f, p)
        WText.draw(c, "خروف", x + 30f, y + 4f, 15f, Color.parseColor("#FFF59D"))
    }
}

// ------------------------------------------------------------------ Villagers (قرويون للتبادل والتجارة والسرقة بستايل ماين كرافت)

enum class VillagerRole(val titleAr: String, val color: Int) {
    TRADER("تاجر القرية", Color.parseColor("#81C784")),
    BLACKSMITH("حداد الدروع والأسلحة", Color.parseColor("#FFB74D")),
    ELDER("حكيم القرية والمهندس", Color.parseColor("#64B5F6")),
    FARMER("مزارع وطاهٍ", Color.parseColor("#AED581")),
    PIRATE_SMUGGLER("تاجر الميناء والبارود", Color.parseColor("#CE93D8"))
}

class VillagerNPC(
    override var x: Float,
    override var y: Float,
    val nameAr: String,
    val role: VillagerRole,
    val tradeGiveText: String,
    val tradeGetText: String,
    val tradeId: String
) : Body {
    override val fOffX = 28f
    override val fOffY = 60f
    override val fW = 28f
    override val fH = 20f

    val homeX = x
    val homeY = y
    val size = 88f
    private val sheet: SpriteSheet
    private val tintP: Paint?
    private var walkTimer = 2f
    private var vx = 0f
    private var vy = 0f
    var stealCd = 0f
    var level = 1
    var xp = 0

    init {
        val path = when (role) {
            VillagerRole.BLACKSMITH -> "sprites/kit8/idle.png"
            VillagerRole.ELDER -> "sprites/priest1/idle.png"
            VillagerRole.PIRATE_SMUGGLER -> "sprites/priest2/idle.png"
            VillagerRole.TRADER -> "sprites/priest3/idle.png"
            else -> "sprites/kit3/idle.png"
        }
        sheet = SpriteSheet(Assets.firstExisting(path, "sprites/merchant/idle.png") ?: "sprites/merchant/idle.png", 0.2f)
        tintP = when (role) {
            VillagerRole.FARMER -> Paint().apply { colorFilter = LightingColorFilter(0xFFEEAA, 0) }
            else -> null
        }
    }

    fun addTradeXp(): Boolean {
        xp++
        if (xp >= level * 2 && level < 5) {
            level++
            xp = 0
            return level == 5
        }
        return false
    }

    fun update(dt: Float, world: Collider) {
        sheet.update(dt)
        if (stealCd > 0f) stealCd -= dt
        walkTimer -= dt
        if (walkTimer <= 0f) {
            walkTimer = 2.4f + (abs(x.toInt()) % 3)
            val dx = homeX - x
            val dy = homeY - y
            if (dx * dx + dy * dy > 160f * 160f) {
                val l = max(1f, sqrt(dx * dx + dy * dy))
                vx = dx / l * 48f
                vy = dy / l * 48f
            } else if ((walkTimer.toInt() % 2) == 0) {
                vx = 0f
                vy = 0f
            } else {
                val a = (x * 0.1f + walkTimer) * 2.5f
                vx = cos(a) * 42f
                vy = sin(a) * 42f
            }
        }
        if (vx != 0f || vy != 0f) world.move(this, vx * dt, vy * dt)
    }

    fun isNear(px: Float, py: Float): Boolean {
        val dx = px - (x + size / 2f)
        val dy = py - (y + size * 0.6f)
        return sqrt(dx * dx + dy * dy) < 120f
    }

    fun draw(c: Canvas) {
        Fx.oval.set(x + 20f, y + 68f, x + 68f, y + 84f)
        c.drawOval(Fx.oval, Fx.shadow)
        sheet.draw(c, x, y, size, size, vx < 0f, tintP)
        WText.draw(c, "$nameAr (مستوى $level)", x + size / 2f, y - 18f, 17f, role.color)
        WText.draw(c, tradeGiveText, x + size / 2f, y - 2f, 13.5f, Color.WHITE)
    }
}

enum class EnemyRole { MELEE, SPEARMAN, ARCHER, HEAVY, GHOUL, TRIBAL, COMMANDO, CLONE, BOSS }

class Enemy(
    override var x: Float,
    override var y: Float,
    val role: EnemyRole,
    val era: EraType,
    val holdsKey: Boolean = false,
    val isColossalBoss: Boolean = false
) : Body {
    val isBoss = role == EnemyRole.BOSS || isColossalBoss
    private val eraMul = 1f + 0.12f * era.ordinal
    var isSiegeZombie = false
    /** Night Bomber Zombie (الزومبي المفجر الليلي): rushes houses/walls and explodes! */
    var isBomberZombie = false
    var wantBomberDetonate = false

    val size: Float = when {
        isColossalBoss -> 216f
        role == EnemyRole.BOSS -> 128f + 8f * era.ordinal
        role == EnemyRole.HEAVY -> 100f
        role == EnemyRole.CLONE -> 92f
        role == EnemyRole.COMMANDO -> 90f
        else -> 84f
    }
    val maxHp: Int = when {
        isColossalBoss -> 25000
        else -> (when (role) {
            EnemyRole.MELEE -> 85
            EnemyRole.SPEARMAN -> 90
            EnemyRole.ARCHER -> 60
            EnemyRole.HEAVY -> 220
            EnemyRole.GHOUL -> 75
            EnemyRole.TRIBAL -> 95
            EnemyRole.COMMANDO -> 85
            EnemyRole.CLONE -> 110
            EnemyRole.BOSS -> 650 + 160 * era.ordinal
        } * (if (isBoss) 1f else eraMul)).toInt()
    }
    var hp = maxHp
    val damage: Int = when {
        isColossalBoss -> 92
        else -> (when (role) {
            EnemyRole.MELEE -> 16
            EnemyRole.SPEARMAN -> 15
            EnemyRole.ARCHER -> 12
            EnemyRole.HEAVY -> 24
            EnemyRole.GHOUL -> 15
            EnemyRole.TRIBAL -> 17
            EnemyRole.COMMANDO -> 13
            EnemyRole.CLONE -> 20
            EnemyRole.BOSS -> 26 + 3 * era.ordinal
        } * (if (isBoss) 1f else (1f + 0.06f * era.ordinal))).toInt()
    }
    val speed: Float = when {
        isColossalBoss -> 134f
        role == EnemyRole.MELEE -> 108f
        role == EnemyRole.SPEARMAN -> 100f
        role == EnemyRole.ARCHER -> 90f
        role == EnemyRole.HEAVY -> 74f
        role == EnemyRole.GHOUL -> 154f
        role == EnemyRole.TRIBAL -> 118f
        role == EnemyRole.COMMANDO -> 92f
        role == EnemyRole.CLONE -> 132f
        else -> 98f
    }
    val reach: Float = when {
        isColossalBoss -> 154f
        role == EnemyRole.SPEARMAN -> 104f
        role == EnemyRole.BOSS -> 112f
        role == EnemyRole.HEAVY -> 86f
        else -> 74f
    }
    val ranged = role == EnemyRole.ARCHER || role == EnemyRole.COMMANDO
    val reward: Int = if (isColossalBoss) 5000 else if (isBoss) 160 else (10 + 2 * era.ordinal + (if (role == EnemyRole.HEAVY) 8 else 0))

    override val fOffX = size * 0.34f
    override val fOffY = size * 0.68f
    override val fW = size * 0.32f
    override val fH = size * 0.22f

    val homeX = x
    val homeY = y
    var aggro = false
    var isDead = false
    var deadTimer = 0f
    var facingLeft = true
    var moving = false
    var hurtTimer = 0f
    var stunTimer = 0f
    var windup = 0f
    private var cooldown = 0.4f
    private var shootTimer = 1.5f
    private var volleyTimer = 3f
    private var meteorTimer = 4.5f
    var summonTimer = 9f
    var wantSummon = false
    var wantMeteorRain = false
    var wantBreakWall = false
    private var phase4ShieldUsed = false
    private var kx = 0f
    private var ky = 0f
    private var stuck = 0f
    private var sideSign = 1f
    var shielded = false
    var lastKnownHp = hp

    val phase2: Boolean get() = isBoss && hp < maxHp / 2
    val bossPhase: Int
        get() {
            if (!isColossalBoss) return if (phase2) 2 else 1
            val f = hp.toFloat() / max(1, maxHp)
            return when {
                f > 0.80f -> 1
                f > 0.60f -> 2
                f > 0.40f -> 3
                f > 0.20f -> 4
                else -> 5
            }
        }

    val cx: Float get() = x + size / 2f
    val cy: Float get() = y + size * 0.7f
    val hitbox = RectF()

    private val folder: String
    private val tintPaint: Paint?
    private val idleA: SpriteSheet
    private val walkA: SpriteSheet
    private val attackA: SpriteSheet
    private val deadA: SpriteSheet

    init {
        var f = "kit7"
        var tint = 0
        when {
            isColossalBoss -> {
                f = "zombie_titan"
                tint = 0xFFFF6090.toInt()
            }
            role == EnemyRole.BOSS -> when (era) {
                EraType.ERA_1_MEDIEVAL -> f = "kit2"
                EraType.ERA_2_CAVES -> { f = "kit1"; tint = 0xFFC09070.toInt() }
                EraType.ERA_3_ZOMBIE -> f = "zombie_titan"
                EraType.ERA_4_ISLAND -> f = "pirate_captain"
                EraType.ERA_5_CYBER -> f = "cyber_boss"
                EraType.ERA_6_VOID -> { f = "kit2"; tint = 0xFFD070FF.toInt() }
            }
            role == EnemyRole.MELEE -> f = when (era) {
                EraType.ERA_3_ZOMBIE -> "zombie"
                EraType.ERA_4_ISLAND -> "tribal_warrior"
                else -> "kit7"
            }
            role == EnemyRole.SPEARMAN -> {
                f = if (era == EraType.ERA_4_ISLAND) "island_spearman" else "kit5"
                if (era == EraType.ERA_2_CAVES) tint = 0xFF9AAAAA.toInt()
            }
            role == EnemyRole.ARCHER -> f = "archer"
            role == EnemyRole.HEAVY -> {
                f = when (era) {
                    EraType.ERA_5_CYBER -> "cyber_heavy"
                    EraType.ERA_1_MEDIEVAL -> "kit8"
                    else -> "kit9"
                }
                if (era == EraType.ERA_2_CAVES) tint = 0xFFAA9988.toInt()
            }
            role == EnemyRole.GHOUL -> {
                f = "zombie_ghoul"
                if (era == EraType.ERA_2_CAVES) tint = 0xFF99BBAA.toInt()
            }
            role == EnemyRole.TRIBAL -> f = "tribal_warrior"
            role == EnemyRole.COMMANDO -> {
                f = "cyber_commando"
                if (era == EraType.ERA_6_VOID) tint = 0xFFD090FF.toInt()
            }
            role == EnemyRole.CLONE -> { f = "vampire"; tint = 0xFFD080FF.toInt() }
        }
        folder = f
        tintPaint = if (tint != 0) Paint().apply { colorFilter = LightingColorFilter(tint, 0) } else null
        idleA = SpriteSheet(Assets.firstExisting("sprites/$f/idle.png") ?: "sprites/$f/idle.png", 0.2f)
        walkA = SpriteSheet(Assets.firstExisting("sprites/$f/walk.png", "sprites/$f/walk+shot.png", "sprites/$f/idle.png") ?: "sprites/$f/walk.png", 0.12f)
        attackA = SpriteSheet(
            Assets.firstExisting("sprites/$f/attack.png", "sprites/$f/attack_1.png", "sprites/$f/shot.png", "sprites/$f/walk.png") ?: "sprites/$f/attack.png",
            0.09f, false
        )
        deadA = SpriteSheet(Assets.firstExisting("sprites/$f/dead.png", "sprites/$f/idle.png") ?: "sprites/$f/dead.png", 0.2f, false)
    }

    fun takeHit(dmg: Int, fromX: Float, fromY: Float, knock: Float = 1f): Int {
        if (isDead) return 0
        aggro = true
        if (shielded) {
            hurtTimer = 0.05f
            return 0
        }
        hp -= dmg
        hurtTimer = 0.14f
        if (!isBoss) {
            val dx = cx - fromX
            val dy = cy - fromY
            val l = max(1f, sqrt(dx * dx + dy * dy))
            kx = dx / l * 300f * knock
            ky = dy / l * 300f * knock
        }
        if (isColossalBoss && bossPhase == 4 && !phase4ShieldUsed) {
            phase4ShieldUsed = true
            shielded = true
        }
        if (hp <= 0) {
            hp = 0
            isDead = true
            deadTimer = 0f
            deadA.reset()
        }
        return dmg
    }

    private fun stepTo(dxn: Float, dyn: Float, mul: Float, dt: Float, world: Collider) {
        val len = sqrt(dxn * dxn + dyn * dyn)
        if (len < 0.001f) return
        val nx = dxn / len
        val ny = dyn / len
        val phaseMul = if (isColossalBoss) (1f + (bossPhase - 1) * 0.10f) else if (isBomberZombie) 1.18f else if (phase2) 1.25f else 1f
        val spd = speed * mul * phaseMul
        val hit = world.move(this, nx * spd * dt, ny * spd * dt)
        if (hit) {
            stuck += dt
            if (isBomberZombie && stuck > 0.22f) {
                wantBomberDetonate = true
                return
            }
            if (isSiegeZombie || isColossalBoss || era == EraType.ERA_3_ZOMBIE) {
                wantBreakWall = true
            }
            world.move(this, -ny * sideSign * spd * dt, nx * sideSign * spd * dt)
            if (stuck > 0.5f) {
                sideSign = -sideSign
                stuck = 0f
            }
        } else {
            stuck = max(0f, stuck - dt)
        }
        moving = true
        if (abs(nx) > 0.2f) facingLeft = nx < 0f
    }

    fun update(dt: Float, p: Player, world: Collider, proj: MutableList<Projectile>) {
        if (isDead) {
            deadA.update(dt)
            deadTimer += dt
            return
        }
        idleA.update(dt)
        walkA.update(dt)
        if (hurtTimer > 0f) hurtTimer -= dt
        if (stunTimer > 0f) {
            stunTimer -= dt
            return
        }
        if (cooldown > 0f) cooldown -= dt
        if (kx != 0f || ky != 0f) {
            world.move(this, kx * dt, ky * dt)
            val k = min(1f, 9f * dt)
            kx -= kx * k
            ky -= ky * k
            if (abs(kx) < 6f) kx = 0f
            if (abs(ky) < 6f) ky = 0f
        }
        hitbox.set(x + size * 0.22f, y + size * 0.18f, x + size * 0.78f, y + size * 0.95f)
        val dx = p.cx - cx
        val dy = p.cy - cy
        val dist = sqrt(dx * dx + dy * dy)
        moving = false
        if (p.isDead) return
        if (!aggro && dist < (if (isColossalBoss) 1100f else if (isBomberZombie) 820f else 580f)) aggro = true
        if (aggro && !isBoss && dist > 1300f) aggro = false

        // Night Bomber Zombie detonates if it gets right next to the player!
        if (isBomberZombie && aggro && dist < 72f) {
            wantBomberDetonate = true
            return
        }

        if (windup > 0f) {
            windup -= dt
            attackA.update(dt)
            if (windup <= 0f) finishAttack(p, proj, dx, dy, dist)
            return
        }
        if (!aggro) {
            val hx = homeX - x
            val hy = homeY - y
            if (hx * hx + hy * hy > 900f) stepTo(hx, hy, 0.6f, dt, world)
            return
        }
        facingLeft = dx < 0f
        if (isBoss) {
            volleyTimer -= dt
            if (volleyTimer <= 0f && dist < (if (isColossalBoss) 1100f else 800f)) {
                volley(proj, dx, dy)
                volleyTimer = if (isColossalBoss) max(1.35f, 2.9f - bossPhase * 0.28f) else if (phase2) 2.4f else 3.6f
            }
            if (isColossalBoss && bossPhase >= 3) {
                meteorTimer -= dt
                if (meteorTimer <= 0f && dist < 1100f) {
                    wantMeteorRain = true
                    meteorTimer = if (bossPhase == 5) 3.4f else 5.2f
                }
            }
            if (phase2 || (isColossalBoss && bossPhase >= 2)) {
                summonTimer -= dt
                if (summonTimer <= 0f) {
                    wantSummon = true
                    summonTimer = if (isColossalBoss) 8.5f else 11f
                }
            }
        }
        if (ranged) {
            shootTimer -= dt
            if (dist < 220f) stepTo(-dx, -dy, 1f, dt, world)
            else if (dist > 430f) stepTo(dx, dy, 1f, dt, world)
            else stepTo(-dy * sideSign, dx * sideSign, 0.5f, dt, world)
            if (shootTimer <= 0f && dist < 640f) {
                windup = 0.4f
                attackA.reset()
                shootTimer = 2.1f + (x.toInt() % 5) * 0.12f
            }
        } else {
            if (dist > reach * 0.85f) {
                stepTo(dx, dy, 1f, dt, world)
            } else if (cooldown <= 0f) {
                windup = if (isColossalBoss) 0.42f else if (isBoss) 0.5f else 0.42f
                cooldown = if (isColossalBoss) 0.95f else if (isBoss) 1.3f else 1.1f
                attackA.reset()
            }
        }
    }

    private fun finishAttack(p: Player, proj: MutableList<Projectile>, dx: Float, dy: Float, dist: Float) {
        if (ranged) {
            val l = max(1f, dist)
            val kind = if (role == EnemyRole.COMMANDO) Projectile.LASER else Projectile.ARROW
            proj.add(Projectile(cx, cy, dx / l * 360f, dy / l * 360f, damage, kind, false))
        } else {
            if (dist < reach * 1.4f) {
                val wasParry = p.parryTimer > 0f
                p.takeDamage(damage)
                if (wasParry) {
                    stunTimer = 1.6f
                    hurtTimer = 0.3f
                }
            }
        }
    }

    private fun volley(proj: MutableList<Projectile>, dx: Float, dy: Float) {
        val base = atan2(dy, dx)
        if (isColossalBoss) {
            val ph = bossPhase
            val dmg = (damage * 0.65f).toInt()
            if (ph >= 4) {
                for (i in 0 until 14) {
                    val a = base + i * (Math.PI.toFloat() * 2f / 14f)
                    proj.add(Projectile(cx, cy, cos(a) * 350f, sin(a) * 350f, dmg, Projectile.ORB, false))
                }
            } else {
                val cnt = 5 + ph * 2
                for (i in 0 until cnt) {
                    val a = base + (i - cnt / 2) * 0.22f
                    val kind = if (i % 2 == 0) Projectile.ORB else Projectile.FIREBALL
                    proj.add(Projectile(cx, cy, cos(a) * 370f, sin(a) * 370f, dmg, kind, false))
                }
            }
            return
        }
        val laser = era == EraType.ERA_5_CYBER
        val kind = if (laser) Projectile.LASER else Projectile.ORB
        val dmg = (damage * 0.5f).toInt()
        if (phase2 && era == EraType.ERA_6_VOID) {
            for (i in 0 until 12) {
                val a = base + i * (Math.PI.toFloat() * 2f / 12f)
                proj.add(Projectile(cx, cy, cos(a) * 300f, sin(a) * 300f, dmg, kind, false))
            }
            return
        }
        val n = if (phase2) 7 else 5
        for (i in 0 until n) {
            val a = base + (i - n / 2) * 0.27f
            proj.add(Projectile(cx, cy, cos(a) * 330f, sin(a) * 330f, dmg, kind, false))
        }
    }

    fun draw(c: Canvas, time: Float) {
        Fx.oval.set(x + size * 0.18f, y + size * 0.80f, x + size * 0.82f, y + size * 0.98f)
        c.drawOval(Fx.oval, Fx.shadow)
        if (isColossalBoss && !isDead) {
            Fx.slash.color = if (bossPhase == 5) Color.parseColor("#FF1744") else Color.parseColor("#AA00FF")
            Fx.slash.alpha = (140 + 80 * sin(time * 5f)).toInt().coerceIn(0, 255)
            Fx.slash.strokeWidth = 6f
            c.drawCircle(cx, cy, size * 0.52f + sin(time * 4f) * 6f, Fx.slash)
            Fx.slash.strokeWidth = 8f
        }
        val sheet = when {
            isDead -> deadA
            windup > 0f -> attackA
            moving -> walkA
            else -> idleA
        }
        val paint: Paint? = when {
            hurtTimer > 0f -> Fx.white
            windup > 0f -> Fx.red
            isBomberZombie && ((time * 9f).toInt() % 2 == 0) -> Fx.bomberOrange
            isColossalBoss && bossPhase == 5 -> Fx.purple
            else -> tintPaint
        }
        sheet.draw(c, x, y, size, size, facingLeft, paint)
        if (isDead) return
        if (isBomberZombie) {
            val bIcon = Assets.bmp("icons/bomb.png") ?: Assets.bmp("icons/block_tnt.png")
            if (bIcon != null) {
                val bob = sin(time * 10f) * 3f
                Fx.oval.set(cx - 14f, y - 16f + bob, cx + 14f, y + 12f + bob)
                c.drawBitmap(bIcon, null, Fx.oval, null)
            }
            WText.draw(c, "زومبي مفجر! 💣", cx, y - 20f, 15f, Color.parseColor("#FF7043"))
        }
        if (stunTimer > 0f) {
            WText.draw(c, "مصدوم! ⚡", cx, y - 10f, 18f, Color.parseColor("#FFD54F"))
        }
        if (shielded) {
            Fx.slash.color = if (isColossalBoss) Color.parseColor("#FF1744") else Color.CYAN
            Fx.slash.alpha = (160 + 85 * sin(time * 6f)).toInt().coerceIn(0, 255)
            Fx.slash.strokeWidth = 6f
            c.drawCircle(cx, cy - size * 0.08f, size * 0.56f, Fx.slash)
            Fx.slash.strokeWidth = 8f
        }
        if (!isBoss && hp < maxHp) {
            val bw = 54f
            val bx = x + size / 2f - bw / 2f
            val by = y + size * 0.08f
            Fx.bar.color = Color.argb(200, 0, 0, 0)
            c.drawRect(bx - 2f, by - 2f, bx + bw + 2f, by + 8f, Fx.bar)
            Fx.bar.color = if (hp > maxHp / 3) Color.rgb(230, 70, 60) else Color.rgb(255, 160, 40)
            c.drawRect(bx, by, bx + bw * hp / maxHp, by + 6f, Fx.bar)
        }
        if (holdsKey) {
            val k = Assets.bmp("icons/key.png")
            if (k != null) {
                val bob = sin(time * 4f) * 4f
                Fx.oval.set(x + size / 2f - 14f, y - 14f + bob, x + size / 2f + 14f, y + 14f + bob)
                c.drawBitmap(k, null, Fx.oval, null)
            }
        }
    }
}


/**
 * Co-Op Friend Knight character in the Open World (صديقك في طور اللعب الجماعي عبر الواي فاي / نقطة الاتصال):
 * Renders smooth 60 FPS interpolated movement, weapon swings, dashes, launchpad flights, boats,
 * tactical overhead pings, and supports Co-Op Revive when downed!
 */
class CoopPartner(var x: Float, var y: Float) {
    var targetX = x
    var targetY = y
    var aimX = 1f
    var aimY = 0f
    var facingLeft = false
    var moving = false
    var isAttacking = false
    var isDashing = false
    var hp = 220
    var maxHp = 220
    var weaponId = "sword_basic"
    var inBoat = false
    var airTimer = 0f
    var parryTimer = 0f
    var comboCount = 0
    var isDowned = false
    var nameAr = "صديقك الفارس 🤝"
    var pingText = ""
    var pingTimer = 0f
    private var swingTimer = 0f

    private val idleA = SpriteSheet("sprites/player/idle.png", 0.2f)
    private val walkA = SpriteSheet("sprites/player/walk.png", 0.11f)
    private val attackA = SpriteSheet("sprites/player/attack.png", 0.07f, false)

    val cx: Float get() = x + 48f
    val cy: Float get() = y + 60f

    fun onNetSync(
        nx: Float, ny: Float, ax: Float, ay: Float, fLeft: Boolean, mov: Boolean,
        atk: Boolean, dsh: Boolean, nhp: Int, nmhp: Int, wepId: String,
        boat: Boolean, air: Float, parry: Float, combo: Int, downed: Boolean
    ) {
        if (hypot((nx - x).toDouble(), (ny - y).toDouble()) > 420.0) {
            x = nx
            y = ny
        }
        targetX = nx
        targetY = ny
        aimX = ax
        aimY = ay
        facingLeft = fLeft
        moving = mov
        if (atk && !isAttacking) {
            attackA.reset()
            swingTimer = 0.26f
        }
        isAttacking = atk
        isDashing = dsh
        hp = nhp
        maxHp = max(1, nmhp)
        weaponId = wepId
        inBoat = boat
        airTimer = air
        parryTimer = parry
        comboCount = combo
        isDowned = downed
    }

    fun triggerPing(txt: String) {
        pingText = txt
        pingTimer = 4.5f
    }

    fun update(dt: Float) {
        val lerp = (dt * 14f).coerceIn(0.1f, 1f)
        x += (targetX - x) * lerp
        y += (targetY - y) * lerp
        if (pingTimer > 0f) pingTimer -= dt
        if (swingTimer > 0f) {
            swingTimer -= dt
            attackA.update(dt)
            if (swingTimer <= 0f) isAttacking = false
        } else if (moving) {
            walkA.update(dt)
        } else {
            idleA.update(dt)
        }
    }

    fun draw(c: Canvas, time: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        // Friendly Cyan-Green Co-Op Aura Ring
        p.style = Paint.Style.FILL
        p.color = if (isDowned) Color.argb(95, 255, 23, 68) else Color.argb(65, 0, 229, 255)
        c.drawOval(RectF(x + 20f, y + 66f, x + 76f, y + 90f), p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2.5f
        p.color = if (isDowned) Color.parseColor("#FF1744") else Color.parseColor("#00E5FF")
        c.drawOval(RectF(x + 20f, y + 66f, x + 76f, y + 90f), p)

        if (inBoat && airTimer <= 0f) {
            Assets.bmp("icons/boat.png")?.let {
                c.drawBitmap(it, null, RectF(x + 8f, y + 48f, x + 88f, y + 96f), null)
            }
        }
        val lift = if (airTimer > 0f) sin((airTimer / 0.76f).coerceIn(0f, 1f) * 3.14159f) * 78f else 0f
        val sheet = when {
            swingTimer > 0f -> attackA
            moving -> walkA
            else -> idleA
        }
        sheet.draw(c, x, y - lift, 96f, 96f, facingLeft)

        // Draw equipped weapon icon next to hand
        val wep = GameCatalogue.weaponById(weaponId) ?: GameCatalogue.WEAPON_BASIC
        if (swingTimer > 0f) {
            p.style = Paint.Style.STROKE
            p.strokeWidth = 5f
            p.color = wep.color
            val sx = cx + (if (facingLeft) -38f else 38f)
            c.drawCircle(sx, cy - 12f - lift, 34f, p)
        }

        if (parryTimer > 0f) {
            p.style = Paint.Style.STROKE
            p.strokeWidth = 4f
            p.color = Color.parseColor("#00E5FF")
            c.drawCircle(cx, cy - 10f - lift, 46f, p)
        }

        // Partner Nameplate & HP Bar above head
        val barW = 72f
        val by = y - 16f - lift
        val br = RectF(cx - barW / 2f, by, cx + barW / 2f, by + 8f)
        Ui.bar(c, br, hp.toFloat() / max(1, maxHp), if (isDowned) Color.parseColor("#FF1744") else Color.parseColor("#00E676"))
        val statusLbl = if (isDowned) "⚠️ $nameAr (سقط! اقترب لإنعاشه ❤️‍🩹)" else "$nameAr ($hp/$maxHp)"
        WText.draw(c, statusLbl, cx, by - 5f, 13.5f, if (isDowned) Color.parseColor("#FF5252") else Color.parseColor("#80DEEA"))

        if (pingTimer > 0f && pingText.isNotEmpty()) {
            val bob = sin(time * 7f) * 4f
            WText.draw(c, pingText, cx, by - 25f + bob, 16.5f, Color.parseColor("#FFD54F"))
        }
    }
}
