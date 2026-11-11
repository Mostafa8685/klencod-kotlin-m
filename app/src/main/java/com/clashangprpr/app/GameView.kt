package com.clashangprpr.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.clashangprpr.app.engine.ActionButton
import com.clashangprpr.app.engine.Camera2D
import com.clashangprpr.app.engine.VirtualJoystick
import com.clashangprpr.app.model.CoinDrop
import com.clashangprpr.app.model.CraftRecipe
import com.clashangprpr.app.model.CraftingRegistry
import com.clashangprpr.app.model.DungeonTrap
import com.clashangprpr.app.model.Enemy
import com.clashangprpr.app.model.EnemyRole
import com.clashangprpr.app.model.EraType
import com.clashangprpr.app.model.FloatingText
import com.clashangprpr.app.model.GameCatalogue
import com.clashangprpr.app.model.ItemType
import com.clashangprpr.app.model.MerchantNPC
import com.clashangprpr.app.model.MiningRock
import com.clashangprpr.app.model.Player
import com.clashangprpr.app.model.Projectile
import com.clashangprpr.app.model.ShopItem
import com.clashangprpr.app.model.TimePortal
import com.clashangprpr.app.model.TreasureChest
import com.clashangprpr.app.model.WoodCrate
import com.clashangprpr.app.model.WorldProp
import com.clashangprpr.app.story.DialogueLine
import com.clashangprpr.app.story.StoryManager
import kotlin.math.sqrt
import kotlin.random.Random

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    private var gameThread: Thread? = null
    @Volatile private var isRunning = false

    private val mapWidth = 2160f
    private val mapHeight = 1440f

    private var camera: Camera2D = Camera2D(1280f, 720f, mapWidth, mapHeight)
    private var joystick: VirtualJoystick = VirtualJoystick(180f, 540f, 120f, 50f)

    // Touch Action Buttons (Sleek semi-transparent)
    private var attackBtn: ActionButton = ActionButton(1140f, 560f, 70f, "ضربة", Color.argb(220, 229, 57, 53))
    private var dashBtn: ActionButton = ActionButton(1010f, 440f, 55f, "مراوغة", Color.argb(220, 156, 39, 176))
    private var magicBtn: ActionButton = ActionButton(1140f, 400f, 55f, "سحر", Color.argb(220, 0, 176, 255))
    private var bombBtn: ActionButton = ActionButton(890f, 370f, 50f, "قنبلة", Color.argb(220, 255, 109, 0))
    private var potionBtn: ActionButton = ActionButton(770f, 370f, 50f, "علاج", Color.argb(220, 67, 160, 71))
    private var interactBtn: ActionButton = ActionButton(1010f, 560f, 55f, "تفاعل", Color.argb(220, 30, 136, 229))

    private var currentEra: EraType = EraType.ERA_1_MEDIEVAL
    private lateinit var player: Player
    private val enemies = mutableListOf<Enemy>()
    private val chests = mutableListOf<TreasureChest>()
    private val props = mutableListOf<WorldProp>()
    private val traps = mutableListOf<DungeonTrap>()
    private val coinDrops = mutableListOf<CoinDrop>()
    private val floatingTexts = mutableListOf<FloatingText>()
    private val projectiles = mutableListOf<Projectile>()
    private val miningRocks = mutableListOf<MiningRock>()
    private val woodCrates = mutableListOf<WoodCrate>()

    // Screen Shake & Game Feel
    private var shakeTimer = 0f
    private var shakeMagnitude = 0f

    // Background Map Bitmaps
    private var bgMedieval: Bitmap? = null
    private var bgCave: Bitmap? = null
    private var bgZombie: Bitmap? = null
    private var darkTotemRgba: Bitmap? = null

    private var merchant: MerchantNPC? = null
    private lateinit var portal: TimePortal

    // Crafting Table
    private val craftTableX = 680f
    private val craftTableY = 320f
    private val craftTableW = 80f
    private val craftTableH = 80f

    // Locked Gate in Sanctuary
    private val gateX = 1450f
    private val gateY = 650f
    private var isGateUnlocked = false

    // Dialogue State
    private var isDialogueOpen = false
    private var dialogueQueue = mutableListOf<DialogueLine>()
    private var currentDialogue: DialogueLine? = null

    // Compact UI States
    private var isShopOpen = false
    private var isCraftingOpen = false
    private val shopItemRects = mutableListOf<Pair<RectF, ShopItem>>()
    private val craftItemRects = mutableListOf<Pair<RectF, CraftRecipe>>()
    private val closeSheetRect = RectF()

    enum class GameState { TITLE, PLAYING, GAME_OVER, VICTORY }
    private var state = GameState.TITLE

    init {
        holder.addCallback(this)
        isFocusable = true
        loadBitmaps()
        initGame()
    }

    private fun loadBitmaps() {
        try {
            context.assets.open("environment/bg_medieval_hd.png").use { bgMedieval = BitmapFactory.decodeStream(it) }
            context.assets.open("environment/bg_cave_hd.png").use { bgCave = BitmapFactory.decodeStream(it) }
            context.assets.open("environment/bg_zombie_hd.png").use { bgZombie = BitmapFactory.decodeStream(it) }
            context.assets.open("environment/dark_totem_rgba.png").use { darkTotemRgba = BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initGame() {
        try {
            player = Player(context, 350f, 450f)
            portal = TimePortal(1850f, 950f)
            setupEra(currentEra)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun triggerScreenShake(duration: Float, magnitude: Float) {
        shakeTimer = duration
        shakeMagnitude = magnitude
    }

    private fun setupEra(era: EraType) {
        try {
            currentEra = era
            enemies.clear()
            chests.clear()
            props.clear()
            traps.clear()
            coinDrops.clear()
            floatingTexts.clear()
            projectiles.clear()
            miningRocks.clear()
            woodCrates.clear()

            player.x = 350f
            player.y = 450f
            player.hp = player.maxHp
            player.mana = player.maxMana
            player.isDead = false
            portal.isActive = false
            isGateUnlocked = false

            // 1. Merchant & Fire Props
            merchant = MerchantNPC(context, 480f, 320f)
            props.add(WorldProp(context, "environment/fire.png", 5, 420f, 330f, 64f, 64f))
            props.add(WorldProp(context, "environment/torch.png", 4, 300f, 200f, 32f, 64f))
            props.add(WorldProp(context, "environment/torch.png", 4, 850f, 200f, 32f, 64f))

            // 2. Animated Traps
            traps.add(DungeonTrap(context, 1050f, 600f, isSaw = true))
            traps.add(DungeonTrap(context, 1250f, 600f, isSaw = false))
            traps.add(DungeonTrap(context, 1350f, 850f, isSaw = true))
            traps.add(DungeonTrap(context, 1600f, 750f, isSaw = false))

            // 3. Mining Rocks & Crates
            miningRocks.add(MiningRock(context, 950f, 250f, isIron = false))
            miningRocks.add(MiningRock(context, 1150f, 220f, isIron = true))
            miningRocks.add(MiningRock(context, 1500f, 260f, isIron = false))
            miningRocks.add(MiningRock(context, 1750f, 220f, isIron = true))

            woodCrates.add(WoodCrate(context, 850f, 450f, isBarrel = false))
            woodCrates.add(WoodCrate(context, 1300f, 350f, isBarrel = true))
            woodCrates.add(WoodCrate(context, 1650f, 450f, isBarrel = false))

            // 4. Chests
            chests.add(TreasureChest(context, 200f, 300f, 45))
            chests.add(TreasureChest(context, 1950f, 250f, 60))
            chests.add(TreasureChest(context, 1850f, 1200f, 80))

            // 5. Rich Roster of Enemies based on Era
            if (era == EraType.ERA_1_MEDIEVAL) {
                enemies.add(Enemy(context, 1050f, 500f, role = EnemyRole.SPEARMAN, era = era))
                enemies.add(Enemy(context, 1250f, 450f, role = EnemyRole.SPEARMAN, era = era))
                enemies.add(Enemy(context, 1400f, 350f, role = EnemyRole.ARCHER, era = era))
                enemies.add(Enemy(context, 1650f, 400f, role = EnemyRole.ARCHER, era = era))
                enemies.add(Enemy(context, 1350f, 750f, role = EnemyRole.HEAVY_SHIELD, era = era, holdsKey = true))
                enemies.add(Enemy(context, 1850f, 850f, role = EnemyRole.BOSS, era = era))
            } else if (era == EraType.ERA_2_CAVES) {
                enemies.add(Enemy(context, 1050f, 500f, role = EnemyRole.GHOUL_FAST, era = era))
                enemies.add(Enemy(context, 1250f, 450f, role = EnemyRole.SPEARMAN, era = era))
                enemies.add(Enemy(context, 1450f, 350f, role = EnemyRole.ARCHER, era = era))
                enemies.add(Enemy(context, 1650f, 650f, role = EnemyRole.HEAVY_SHIELD, era = era, holdsKey = true))
                enemies.add(Enemy(context, 1850f, 850f, role = EnemyRole.BOSS, era = era))
            } else {
                // Era 3: Zombie Outbreak
                enemies.add(Enemy(context, 950f, 450f, role = EnemyRole.MELEE, era = era))
                enemies.add(Enemy(context, 1150f, 550f, role = EnemyRole.GHOUL_FAST, era = era))
                enemies.add(Enemy(context, 1350f, 450f, role = EnemyRole.GHOUL_FAST, era = era))
                enemies.add(Enemy(context, 1550f, 650f, role = EnemyRole.MELEE, era = era, holdsKey = true))
                enemies.add(Enemy(context, 1750f, 550f, role = EnemyRole.GHOUL_FAST, era = era))
                enemies.add(Enemy(context, 1850f, 850f, role = EnemyRole.BOSS, era = era))
            }

            showDialogue(StoryManager.getIntroDialogue())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showDialogue(lines: List<DialogueLine>) {
        dialogueQueue.clear()
        dialogueQueue.addAll(lines)
        if (dialogueQueue.isNotEmpty()) {
            currentDialogue = dialogueQueue.removeAt(0)
            isDialogueOpen = true
        }
    }

    private fun nextDialogue() {
        if (dialogueQueue.isNotEmpty()) {
            currentDialogue = dialogueQueue.removeAt(0)
        } else {
            isDialogueOpen = false
            currentDialogue = null
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        startLoop()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        if (w > 0 && h > 0) {
            camera.viewWidth = w.toFloat()
            camera.viewHeight = h.toFloat()
            joystick.setCenter(180f, h - 160f)
            attackBtn.x = w - 140f
            attackBtn.y = h - 140f
            magicBtn.x = w - 140f
            magicBtn.y = h - 280f
            dashBtn.x = w - 260f
            dashBtn.y = h - 270f
            bombBtn.x = w - 380f
            bombBtn.y = h - 270f
            potionBtn.x = w - 490f
            potionBtn.y = h - 270f
            interactBtn.x = w - 260f
            interactBtn.y = h - 140f
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopLoop()
    }

    private fun startLoop() {
        if (isRunning) return
        isRunning = true
        gameThread = Thread(this, "KnightWarGameLoop").apply {
            start()
        }
    }

    fun pause() {
        stopLoop()
    }

    private fun stopLoop() {
        if (!isRunning) return
        isRunning = false
        var retry = true
        while (retry) {
            try {
                gameThread?.join(500)
                retry = false
            } catch (e: InterruptedException) {}
        }
        gameThread = null
    }

    override fun run() {
        var lastTime = System.nanoTime()
        while (isRunning) {
            val now = System.nanoTime()
            val deltaTime = ((now - lastTime) / 1_000_000_000.0).toFloat().coerceIn(0f, 0.05f)
            lastTime = now

            if (width > 0 && height > 0) {
                try {
                    update(deltaTime)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            var canvas: Canvas? = null
            try {
                if (holder.surface.isValid) {
                    canvas = holder.lockCanvas()
                    if (canvas != null) {
                        synchronized(holder) {
                            render(canvas)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            val elapsedMs = (System.nanoTime() - now) / 1_000_000
            val sleepMs = 16 - elapsedMs
            if (sleepMs > 0) {
                try {
                    Thread.sleep(sleepMs)
                } catch (ignored: Exception) {}
            }
        }
    }

    private fun update(deltaTime: Float) {
        if (state != GameState.PLAYING || isDialogueOpen || isShopOpen || isCraftingOpen) return

        if (shakeTimer > 0) shakeTimer -= deltaTime

        player.update(deltaTime, joystick.deltaX, joystick.deltaY, mapWidth, mapHeight)
        camera.update(player.x, player.y)

        merchant?.update(deltaTime)
        portal.update(deltaTime)

        for (prop in props) prop.update(deltaTime)
        for (trap in traps) trap.update(deltaTime, player)

        // Coin drops
        val itCoins = coinDrops.iterator()
        while (itCoins.hasNext()) {
            val c = itCoins.next()
            c.update(deltaTime)
            if (c.isPlayerNear(player.x + player.width / 2, player.y + player.height / 2)) {
                player.coins += c.value
                floatingTexts.add(FloatingText("+${c.value} ذهب", player.x, player.y - 20f, Color.parseColor("#FFD700")))
                itCoins.remove()
            }
        }

        // Floating texts
        val itFloat = floatingTexts.iterator()
        while (itFloat.hasNext()) {
            if (!itFloat.next().update(deltaTime)) itFloat.remove()
        }

        // Projectiles Update
        val itProj = projectiles.iterator()
        while (itProj.hasNext()) {
            val proj = itProj.next()
            proj.update(deltaTime)

            if (proj.isEnemy) {
                if (player.hitbox.contains(proj.x, proj.y)) {
                    if (player.takeDamage(proj.damage)) {
                        triggerScreenShake(0.2f, 8f)
                        floatingTexts.add(FloatingText("-${proj.damage} ضرر", player.x + 20f, player.y, Color.parseColor("#E91E63")))
                    }
                    proj.isDestroyed = true
                }
            } else {
                for (enemy in enemies) {
                    if (!enemy.isDead && enemy.hitbox.contains(proj.x, proj.y)) {
                        enemy.takeDamage(proj.damage)
                        triggerScreenShake(0.15f, 6f)
                        floatingTexts.add(FloatingText("-${proj.damage}", enemy.x + 20f, enemy.y, Color.parseColor("#FF5722")))
                        proj.isDestroyed = true
                        handleEnemyDeath(enemy)
                        break
                    }
                }
            }

            if (proj.isDestroyed) itProj.remove()
        }

        // Update enemies
        val activeEnemies = enemies.filter { !it.isDead }
        if (activeEnemies.isEmpty() && !portal.isActive) {
            portal.isActive = true
        }

        for (enemy in enemies) {
            enemy.update(deltaTime, player, projectiles)
        }

        // Player melee attack
        if (player.isAttacking) {
            val attackHitbox = RectF(
                if (player.facingLeft) player.x - 45f else player.x + player.width,
                player.y + 10f,
                if (player.facingLeft) player.x else player.x + player.width + 45f,
                player.y + player.height - 10f
            )

            for (enemy in enemies) {
                if (!enemy.isDead && RectF.intersects(attackHitbox, enemy.hitbox)) {
                    val dmg = player.equippedWeapon.damage
                    enemy.takeDamage(dmg)
                    triggerScreenShake(0.12f, 5f)
                    floatingTexts.add(FloatingText("-$dmg", enemy.x + 20f, enemy.y, Color.parseColor("#FF1744")))
                    handleEnemyDeath(enemy)
                }
            }

            for (rock in miningRocks) {
                if (!rock.isDestroyed && RectF.intersects(attackHitbox, rock.hitbox)) {
                    if (rock.hit()) {
                        triggerScreenShake(0.1f, 4f)
                        if (rock.isIron) {
                            player.iron += 1
                            floatingTexts.add(FloatingText("+1 خام حديد", rock.x, rock.y - 20f, Color.parseColor("#FFD54F")))
                        } else {
                            player.stone += 1
                            floatingTexts.add(FloatingText("+1 حجر", rock.x, rock.y - 20f, Color.parseColor("#B0BEC5")))
                        }
                    }
                }
            }

            for (crate in woodCrates) {
                if (!crate.isDestroyed && RectF.intersects(attackHitbox, crate.hitbox)) {
                    if (crate.hit()) {
                        triggerScreenShake(0.1f, 4f)
                        player.wood += 2
                        floatingTexts.add(FloatingText("+2 خشب", crate.x, crate.y - 20f, Color.parseColor("#8D6E63")))
                    }
                }
            }
        }

        // Dynamic Interact button
        val px = player.x + player.width / 2
        val py = player.y + player.height / 2
        val distToTable = sqrt((px - (craftTableX + 40f)) * (px - (craftTableX + 40f)) + (py - (craftTableY + 40f)) * (py - (craftTableY + 40f)))
        val distToGate = sqrt((px - gateX) * (px - gateX) + (py - gateY) * (py - gateY))

        when {
            distToTable < 110f -> {
                interactBtn = ActionButton(interactBtn.x, interactBtn.y, interactBtn.radius, "صناعة", Color.argb(220, 123, 31, 162))
            }
            merchant != null && merchant!!.isPlayerNear(px, py) -> {
                interactBtn = ActionButton(interactBtn.x, interactBtn.y, interactBtn.radius, "المتجر", Color.argb(220, 255, 152, 0))
            }
            distToGate < 130f && !isGateUnlocked -> {
                interactBtn = ActionButton(interactBtn.x, interactBtn.y, interactBtn.radius, if (player.hasBossKey) "فتح الباب" else "مقفل", Color.argb(220, 211, 47, 47))
            }
            chests.any { it.isPlayerNear(px, py) && !it.isOpened } -> {
                interactBtn = ActionButton(interactBtn.x, interactBtn.y, interactBtn.radius, "فتح", Color.argb(220, 251, 192, 45))
            }
            portal.isActive && portal.isPlayerInside(px, py) -> {
                interactBtn = ActionButton(interactBtn.x, interactBtn.y, interactBtn.radius, "عبور", Color.argb(220, 0, 229, 255))
            }
            else -> {
                interactBtn = ActionButton(interactBtn.x, interactBtn.y, interactBtn.radius, "تفاعل", Color.argb(220, 30, 136, 229))
            }
        }

        if (player.hp <= 0 || player.isDead) {
            state = GameState.GAME_OVER
        }
    }

    private fun handleEnemyDeath(enemy: Enemy) {
        if (enemy.isDead) {
            val goldAmount = if (enemy.isBoss) 95 else 20
            coinDrops.add(CoinDrop(enemy.x + enemy.width / 2, enemy.y + enemy.height / 2, goldAmount))

            if (enemy.holdsKey) {
                player.hasBossKey = true
                floatingTexts.add(FloatingText("عثرت على مفتاح قاعة الزعيم!", player.x - 40f, player.y - 40f, Color.YELLOW))
            }

            if (enemy.isBoss) {
                triggerScreenShake(0.5f, 15f)
                showDialogue(StoryManager.getEraClearDialogue(currentEra))
            }
        }
    }

    private fun handleInteract() {
        val px = player.x + player.width / 2
        val py = player.y + player.height / 2

        val distToTable = sqrt((px - (craftTableX + 40f)) * (px - (craftTableX + 40f)) + (py - (craftTableY + 40f)) * (py - (craftTableY + 40f)))
        if (distToTable < 110f) {
            isCraftingOpen = true
            return
        }

        if (merchant != null && merchant!!.isPlayerNear(px, py)) {
            isShopOpen = true
            return
        }

        val distToGate = sqrt((px - gateX) * (px - gateX) + (py - gateY) * (py - gateY))
        if (distToGate < 130f && !isGateUnlocked) {
            if (player.hasBossKey) {
                isGateUnlocked = true
                floatingTexts.add(FloatingText("تم فتح بوابة قاعة الزعيم!", px - 60f, py - 40f, Color.CYAN))
            } else {
                floatingTexts.add(FloatingText("البوابة مقفلة! اقضِ على الفارس المدرع لانتزاع المفتاح", px - 90f, py - 40f, Color.RED))
            }
            return
        }

        for (ch in chests) {
            if (ch.isPlayerNear(px, py) && !ch.isOpened) {
                val gold = ch.open()
                player.coins += gold
                player.stone += 2
                player.wood += 2
                floatingTexts.add(FloatingText("+$gold ذهب وموارد نادرة!", px - 40f, py - 40f, Color.parseColor("#FFD700")))
                return
            }
        }

        if (portal.isActive && portal.isPlayerInside(px, py)) {
            advanceEra()
        }
    }

    private fun advanceEra() {
        when (currentEra) {
            EraType.ERA_1_MEDIEVAL -> setupEra(EraType.ERA_2_CAVES)
            EraType.ERA_2_CAVES -> setupEra(EraType.ERA_3_ZOMBIE)
            EraType.ERA_3_ZOMBIE -> {
                state = GameState.VICTORY
            }
        }
    }

    private fun render(canvas: Canvas) {
        when (state) {
            GameState.TITLE -> drawTitleScreen(canvas)
            GameState.PLAYING -> drawGameWorld(canvas)
            GameState.GAME_OVER -> drawGameOverScreen(canvas)
            GameState.VICTORY -> drawVictoryScreen(canvas)
        }
    }

    private fun drawGameWorld(canvas: Canvas) {
        canvas.drawColor(Color.BLACK)

        canvas.save()

        // Apply Screen Shake
        var offsetX = -camera.x
        var offsetY = -camera.y
        if (shakeTimer > 0f) {
            offsetX += (Random.nextFloat() * 2f - 1f) * shakeMagnitude
            offsetY += (Random.nextFloat() * 2f - 1f) * shakeMagnitude
        }
        canvas.translate(offsetX, offsetY)

        // 1. One Single Pre-rendered Crisp Background Map Call (Zero grids, Zero lag!)
        val currentBg = when (currentEra) {
            EraType.ERA_1_MEDIEVAL -> bgMedieval
            EraType.ERA_2_CAVES -> bgCave
            EraType.ERA_3_ZOMBIE -> bgZombie
        }
        if (currentBg != null) {
            canvas.drawBitmap(currentBg, 0f, 0f, null)
        }

        // 2. Traps (Animated Rotating Saw & Fire Pit)
        for (tr in traps) tr.draw(canvas)

        // 3. Crafting Table
        val craftTablePaint = Paint().apply {
            color = Color.parseColor("#3E2723")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(craftTableX, craftTableY, craftTableX + craftTableW, craftTableY + craftTableH, 12f, 12f, craftTablePaint)
        val pTableLabel = Paint().apply {
            color = Color.parseColor("#E1BEE7")
            textSize = 20f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText("طاولة الصناعة", craftTableX + craftTableW / 2, craftTableY - 10f, pTableLabel)

        // 4. Dark Totem Altar with Pure Alpha Transparency
        darkTotemRgba?.let { tot ->
            canvas.drawBitmap(tot, null, RectF(1450f, 850f, 1580f, 980f), null)
        }

        // 5. Props (Campfires, Torches)
        for (prop in props) prop.draw(canvas)

        // 6. Resources (Real rock pillars and wooden crates)
        for (rock in miningRocks) rock.draw(canvas)
        for (crate in woodCrates) crate.draw(canvas)

        // 7. Chests, Merchant, Portal
        for (ch in chests) ch.draw(canvas)
        merchant?.draw(canvas)
        portal.draw(canvas)

        // 8. Projectiles, Coins, Enemies, Player
        for (p in projectiles) p.draw(canvas)
        for (c in coinDrops) c.draw(canvas)
        for (enemy in enemies) enemy.draw(canvas)
        player.draw(canvas)

        for (ft in floatingTexts) ft.draw(canvas)

        canvas.restore()

        // Clean Modern HUD
        drawHUD(canvas)

        if (!isDialogueOpen && !isShopOpen && !isCraftingOpen) {
            joystick.draw(canvas)
            attackBtn.draw(canvas)
            dashBtn.draw(canvas)
            magicBtn.draw(canvas)
            bombBtn.draw(canvas)
            potionBtn.draw(canvas)
            interactBtn.draw(canvas)
        }

        if (isDialogueOpen) drawDialogueBox(canvas)
        if (isShopOpen) drawCompactShop(canvas)
        if (isCraftingOpen) drawCompactCrafting(canvas)
    }

    private fun drawHUD(canvas: Canvas) {
        val p = Paint()
        val screenW = width.toFloat().coerceAtLeast(1f)

        // Sleek Semi-Transparent Stats Panel
        p.color = Color.argb(190, 15, 18, 25)
        canvas.drawRoundRect(35f, 25f, 440f, 160f, 16f, 16f, p)
        p.color = Color.parseColor("#FFD700")
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2.5f
        canvas.drawRoundRect(35f, 25f, 440f, 160f, 16f, 16f, p)
        p.style = Paint.Style.FILL

        // HP Bar
        p.color = Color.parseColor("#2E7D32")
        val hpWidth = (370f * (player.hp.toFloat() / player.maxHp)).coerceAtLeast(0f)
        canvas.drawRoundRect(45f, 35f, 45f + hpWidth, 60f, 6f, 6f, p)
        p.color = Color.WHITE
        p.textSize = 19f
        p.isFakeBoldText = true
        canvas.drawText("صحة: ${player.hp}/${player.maxHp}", 55f, 53f, p)

        // Mana Bar
        p.color = Color.parseColor("#0288D1")
        val manaWidth = (370f * (player.mana / player.maxMana)).coerceAtLeast(0f)
        canvas.drawRoundRect(45f, 65f, 45f + manaWidth, 85f, 6f, 6f, p)
        p.color = Color.WHITE
        p.textSize = 17f
        canvas.drawText("طاقة السحر: ${player.mana.toInt()}/${player.maxMana.toInt()}", 55f, 81f, p)

        // Gold & Weapon Info
        p.textSize = 19f
        p.color = Color.parseColor("#FFD700")
        canvas.drawText("الذهب: ${player.coins}", 50f, 115f, p)
        p.color = Color.parseColor("#80D8FF")
        canvas.drawText("السلاح: ${player.equippedWeapon.nameAr}", 180f, 115f, p)

        // Resources row
        p.textSize = 16f
        p.color = Color.parseColor("#FFE082")
        val keyStatus = if (player.hasBossKey) "المفتاح: متاح" else "المفتاح: مفقود"
        canvas.drawText("خشب: ${player.wood} | حجر: ${player.stone} | حديد: ${player.iron} | قنابل: ${player.bombs} | $keyStatus", 50f, 145f, p)

        // Era Title in Center
        p.textAlign = Paint.Align.CENTER
        p.textSize = 28f
        p.color = Color.parseColor("#FFD700")
        canvas.drawText(currentEra.title, screenW / 2f, 45f, p)
        p.textSize = 19f
        p.color = Color.LTGRAY
        canvas.drawText(currentEra.subtitle, screenW / 2f, 75f, p)

        // Enemies count
        val left = enemies.count { !it.isDead }
        p.textAlign = Paint.Align.RIGHT
        p.textSize = 21f
        p.color = Color.WHITE
        val statusText = if (left == 0) "بوابة الزمن مفتوحة!" else "الوحوش المتبقية: $left"
        canvas.drawText(statusText, screenW - 40f, 50f, p)
    }

    private fun drawCompactShop(canvas: Canvas) {
        val screenW = width.toFloat().coerceAtLeast(1f)
        val screenH = height.toFloat().coerceAtLeast(1f)

        val sheetH = 320f
        val panelRect = RectF(60f, screenH - sheetH - 20f, screenW - 60f, screenH - 20f)
        val p = Paint()

        p.color = Color.argb(245, 18, 22, 32)
        canvas.drawRoundRect(panelRect, 20f, 20f, p)
        p.color = Color.parseColor("#FFB300")
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3f
        canvas.drawRoundRect(panelRect, 20f, 20f, p)
        p.style = Paint.Style.FILL

        p.color = Color.parseColor("#FFD700")
        p.textSize = 28f
        p.isFakeBoldText = true
        p.textAlign = Paint.Align.LEFT
        canvas.drawText("متجر التاجر إلريك (رصيدك: ${player.coins} ذهب)", panelRect.left + 30f, panelRect.top + 45f, p)

        closeSheetRect.set(panelRect.right - 120f, panelRect.top + 15f, panelRect.right - 20f, panelRect.top + 55f)
        p.color = Color.parseColor("#E53935")
        canvas.drawRoundRect(closeSheetRect, 10f, 10f, p)
        p.color = Color.WHITE
        p.textAlign = Paint.Align.CENTER
        p.textSize = 20f
        canvas.drawText("إغلاق", closeSheetRect.centerX(), closeSheetRect.centerY() + 7f, p)

        shopItemRects.clear()
        val cardW = (panelRect.width() - 80f) / 2f
        val cardH = 105f
        val startX = panelRect.left + 30f
        val startY = panelRect.top + 75f

        for (i in GameCatalogue.shopItems.indices) {
            val item = GameCatalogue.shopItems[i]
            val col = i % 2
            val row = i / 2
            val cx = startX + col * (cardW + 20f)
            val cy = startY + row * (cardH + 15f)
            val cardRect = RectF(cx, cy, cx + cardW, cy + cardH)
            shopItemRects.add(Pair(cardRect, item))

            p.color = Color.argb(220, 30, 36, 52)
            canvas.drawRoundRect(cardRect, 12f, 12f, p)

            val canAfford = player.coins >= item.price
            p.color = if (canAfford) Color.parseColor("#66BB6A") else Color.parseColor("#EF5350")
            p.style = Paint.Style.STROKE
            p.strokeWidth = 2f
            canvas.drawRoundRect(cardRect, 12f, 12f, p)
            p.style = Paint.Style.FILL

            p.textAlign = Paint.Align.LEFT
            p.textSize = 21f
            p.color = Color.WHITE
            canvas.drawText(item.titleAr, cx + 15f, cy + 30f, p)

            p.textSize = 15f
            p.color = Color.LTGRAY
            canvas.drawText(item.descAr, cx + 15f, cy + 56f, p)

            p.textSize = 19f
            p.color = if (canAfford) Color.parseColor("#FFD700") else Color.GRAY
            canvas.drawText("شراء (${item.price} ذهب)", cx + 15f, cy + 90f, p)
        }
    }

    private fun drawCompactCrafting(canvas: Canvas) {
        val screenW = width.toFloat().coerceAtLeast(1f)
        val screenH = height.toFloat().coerceAtLeast(1f)

        val sheetH = 320f
        val panelRect = RectF(60f, screenH - sheetH - 20f, screenW - 60f, screenH - 20f)
        val p = Paint()

        p.color = Color.argb(245, 24, 18, 32)
        canvas.drawRoundRect(panelRect, 20f, 20f, p)
        p.color = Color.parseColor("#BA68C8")
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3f
        canvas.drawRoundRect(panelRect, 20f, 20f, p)
        p.style = Paint.Style.FILL

        p.color = Color.parseColor("#E1BEE7")
        p.textSize = 28f
        p.isFakeBoldText = true
        p.textAlign = Paint.Align.LEFT
        canvas.drawText("طاولة الصناعة (خشب: ${player.wood} | حجر: ${player.stone} | حديد: ${player.iron})", panelRect.left + 30f, panelRect.top + 45f, p)

        closeSheetRect.set(panelRect.right - 120f, panelRect.top + 15f, panelRect.right - 20f, panelRect.top + 55f)
        p.color = Color.parseColor("#E53935")
        canvas.drawRoundRect(closeSheetRect, 10f, 10f, p)
        p.color = Color.WHITE
        p.textAlign = Paint.Align.CENTER
        p.textSize = 20f
        canvas.drawText("إغلاق", closeSheetRect.centerX(), closeSheetRect.centerY() + 7f, p)

        craftItemRects.clear()
        val cardW = (panelRect.width() - 80f) / 2f
        val cardH = 105f
        val startX = panelRect.left + 30f
        val startY = panelRect.top + 75f

        for (i in CraftingRegistry.recipes.indices) {
            val recipe = CraftingRegistry.recipes[i]
            val col = i % 2
            val row = i / 2
            val cx = startX + col * (cardW + 20f)
            val cy = startY + row * (cardH + 15f)
            val cardRect = RectF(cx, cy, cx + cardW, cy + cardH)
            craftItemRects.add(Pair(cardRect, recipe))

            p.color = Color.argb(220, 38, 30, 52)
            canvas.drawRoundRect(cardRect, 12f, 12f, p)

            val canCraft = player.wood >= recipe.woodCost && player.stone >= recipe.stoneCost && player.iron >= recipe.ironCost
            p.color = if (canCraft) Color.parseColor("#AB47BC") else Color.parseColor("#EF5350")
            p.style = Paint.Style.STROKE
            p.strokeWidth = 2f
            canvas.drawRoundRect(cardRect, 12f, 12f, p)
            p.style = Paint.Style.FILL

            p.textAlign = Paint.Align.LEFT
            p.textSize = 21f
            p.color = Color.WHITE
            canvas.drawText(recipe.titleAr, cx + 15f, cy + 30f, p)

            p.textSize = 15f
            p.color = Color.LTGRAY
            canvas.drawText("التكلفة: ${recipe.woodCost} خشب، ${recipe.stoneCost} حجر، ${recipe.ironCost} حديد", cx + 15f, cy + 56f, p)

            p.textSize = 19f
            p.color = if (canCraft) Color.parseColor("#CE93D8") else Color.GRAY
            canvas.drawText(if (canCraft) "صنع الآن" else "الموارد لا تكفي", cx + 15f, cy + 90f, p)
        }
    }

    private fun handleShopTouch(tx: Float, ty: Float) {
        if (closeSheetRect.contains(tx, ty)) {
            isShopOpen = false
            return
        }

        for (pair in shopItemRects) {
            val rect = pair.first
            val item = pair.second
            if (rect.contains(tx, ty)) {
                if (player.coins >= item.price) {
                    player.coins -= item.price
                    when (item.type) {
                        ItemType.POTION -> {
                            player.potions += 2
                            floatingTexts.add(FloatingText("+2 جرعات شفاء!", player.x, player.y - 30f, Color.GREEN))
                        }
                        ItemType.WEAPON -> {
                            if (item.id == "sword_fire") {
                                player.equippedWeapon = GameCatalogue.WEAPON_FIRE
                                floatingTexts.add(FloatingText("سيف النار مجهز!", player.x, player.y - 30f, Color.parseColor("#FF5722")))
                            } else if (item.id == "spear_chrono") {
                                player.equippedWeapon = GameCatalogue.WEAPON_CHRONO
                                floatingTexts.add(FloatingText("رمح كرونو مجهز!", player.x, player.y - 30f, Color.parseColor("#00E5FF")))
                            }
                        }
                        ItemType.ARMOR -> {
                            player.hasRoyalArmor = true
                            player.maxHp = 250
                            player.hp = (player.hp + 100).coerceAtMost(player.maxHp)
                            floatingTexts.add(FloatingText("درع ملكي! (+100 صحة)", player.x, player.y - 30f, Color.YELLOW))
                        }
                    }
                } else {
                    floatingTexts.add(FloatingText("الذهب غير كافٍ!", player.x, player.y - 30f, Color.RED))
                }
                return
            }
        }
    }

    private fun handleCraftTouch(tx: Float, ty: Float) {
        if (closeSheetRect.contains(tx, ty)) {
            isCraftingOpen = false
            return
        }

        for (pair in craftItemRects) {
            val rect = pair.first
            val recipe = pair.second
            if (rect.contains(tx, ty)) {
                if (player.wood >= recipe.woodCost && player.stone >= recipe.stoneCost && player.iron >= recipe.ironCost) {
                    player.wood -= recipe.woodCost
                    player.stone -= recipe.stoneCost
                    player.iron -= recipe.ironCost

                    when (recipe.resultType) {
                        "BOMB" -> {
                            player.bombs += 2
                            floatingTexts.add(FloatingText("+2 قنابل متفجرة!", player.x, player.y - 30f, Color.parseColor("#FF6D00")))
                        }
                        "SWORD" -> {
                            player.equippedWeapon = GameCatalogue.WEAPON_FIRE
                            floatingTexts.add(FloatingText("صنع سيف فولاذي معزز!", player.x, player.y - 30f, Color.parseColor("#00E5FF")))
                        }
                        "POTION" -> {
                            player.potions += 2
                            floatingTexts.add(FloatingText("+2 إكسير شفاء!", player.x, player.y - 30f, Color.GREEN))
                        }
                        "SHIELD" -> {
                            player.hasRoyalArmor = true
                            player.maxHp = 280
                            player.hp = (player.hp + 120).coerceAtMost(player.maxHp)
                            floatingTexts.add(FloatingText("صنع درع تيتانيوم!", player.x, player.y - 30f, Color.YELLOW))
                        }
                    }
                } else {
                    floatingTexts.add(FloatingText("الموارد غير كافية!", player.x, player.y - 30f, Color.RED))
                }
                return
            }
        }
    }

    private fun drawDialogueBox(canvas: Canvas) {
        val dialogue = currentDialogue ?: return
        val screenW = width.toFloat().coerceAtLeast(1f)
        val screenH = height.toFloat().coerceAtLeast(1f)

        val boxRect = RectF(80f, screenH - 280f, screenW - 80f, screenH - 40f)
        val p = Paint()
        p.color = Color.argb(235, 15, 15, 25)
        canvas.drawRoundRect(boxRect, 20f, 20f, p)

        p.color = Color.parseColor("#FFD700")
        p.style = Paint.Style.STROKE
        p.strokeWidth = 4f
        canvas.drawRoundRect(boxRect, 20f, 20f, p)

        p.style = Paint.Style.FILL
        p.textSize = 30f
        p.isFakeBoldText = true
        p.textAlign = Paint.Align.LEFT
        canvas.drawText(dialogue.speaker, 110f, screenH - 230f, p)

        p.color = Color.WHITE
        p.textSize = 25f
        p.isFakeBoldText = false
        canvas.drawText(dialogue.textAr, 110f, screenH - 175f, p)

        p.color = Color.LTGRAY
        p.textSize = 21f
        canvas.drawText(dialogue.textEn, 110f, screenH - 120f, p)

        p.color = Color.parseColor("#4FC3F7")
        p.textSize = 19f
        p.textAlign = Paint.Align.RIGHT
        canvas.drawText("اضغط للمتابعة ▶", screenW - 110f, screenH - 60f, p)
    }

    private fun drawTitleScreen(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#0D1117"))
        val screenW = width.toFloat().coerceAtLeast(1f)
        val screenH = height.toFloat().coerceAtLeast(1f)

        val p = Paint().apply {
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        p.color = Color.parseColor("#FFD700")
        p.textSize = 68f
        canvas.drawText("حرب الفارس: عابر العصور", screenW / 2f, screenH / 2f - 90f, p)

        p.color = Color.WHITE
        p.textSize = 34f
        canvas.drawText("قتال، رماة سهام، مراوغة سريعة، وأفخاخ مميتة", screenW / 2f, screenH / 2f - 20f, p)

        p.color = Color.LTGRAY
        p.textSize = 24f
        canvas.drawText("صمم أسلحتك، اقضِ على الفرسان والزومبي وافتح بوابات الزمن!", screenW / 2f, screenH / 2f + 40f, p)

        p.color = Color.parseColor("#4CAF50")
        p.textSize = 36f
        canvas.drawText("▶ المس الشاشة للبدء (TAP TO START)", screenW / 2f, screenH / 2f + 140f, p)
    }

    private fun drawGameOverScreen(canvas: Canvas) {
        canvas.drawColor(Color.argb(220, 30, 0, 0))
        val screenW = width.toFloat().coerceAtLeast(1f)
        val screenH = height.toFloat().coerceAtLeast(1f)

        val p = Paint().apply {
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        p.color = Color.RED
        p.textSize = 72f
        canvas.drawText("سقط فارس الزمن (GAME OVER)", screenW / 2f, screenH / 2f - 40f, p)

        p.color = Color.WHITE
        p.textSize = 32f
        canvas.drawText("المس الشاشة لإعادة المحاولة", screenW / 2f, screenH / 2f + 50f, p)
    }

    private fun drawVictoryScreen(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#0A192F"))
        val screenW = width.toFloat().coerceAtLeast(1f)
        val screenH = height.toFloat().coerceAtLeast(1f)

        val p = Paint().apply {
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        p.color = Color.parseColor("#FFD700")
        p.textSize = 72f
        canvas.drawText("مبروك! ختمت اللعبة بالكامل!", screenW / 2f, screenH / 2f - 70f, p)

        p.color = Color.parseColor("#64FFDA")
        p.textSize = 38f
        canvas.drawText("YOU RESTORED THE TIMELINE!", screenW / 2f, screenH / 2f, p)

        p.color = Color.WHITE
        p.textSize = 26f
        canvas.drawText("تم استكشاف كافة العصور وقهر الزعماء بنجاح!", screenW / 2f, screenH / 2f + 60f, p)

        p.color = Color.YELLOW
        p.textSize = 30f
        canvas.drawText("المس الشاشة للعب مجدداً من البداية", screenW / 2f, screenH / 2f + 140f, p)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)
        val touchX = event.getX(pointerIndex)
        val touchY = event.getY(pointerIndex)

        when (state) {
            GameState.TITLE -> {
                if (action == MotionEvent.ACTION_DOWN) {
                    state = GameState.PLAYING
                }
                return true
            }
            GameState.GAME_OVER, GameState.VICTORY -> {
                if (action == MotionEvent.ACTION_DOWN) {
                    currentEra = EraType.ERA_1_MEDIEVAL
                    initGame()
                    state = GameState.PLAYING
                }
                return true
            }
            GameState.PLAYING -> {
                if (isShopOpen) {
                    if (action == MotionEvent.ACTION_DOWN) {
                        handleShopTouch(touchX, touchY)
                    }
                    return true
                }

                if (isCraftingOpen) {
                    if (action == MotionEvent.ACTION_DOWN) {
                        handleCraftTouch(touchX, touchY)
                    }
                    return true
                }

                if (isDialogueOpen) {
                    if (action == MotionEvent.ACTION_DOWN) {
                        nextDialogue()
                    }
                    return true
                }

                when (action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                        if (joystick.onTouchDown(pointerId, touchX, touchY)) return true
                        if (attackBtn.onTouchDown(pointerId, touchX, touchY)) {
                            player.attack()
                            return true
                        }
                        if (dashBtn.onTouchDown(pointerId, touchX, touchY)) {
                            if (player.dash()) {
                                triggerScreenShake(0.08f, 3f)
                                floatingTexts.add(FloatingText("مراوغة!", player.x, player.y - 25f, Color.parseColor("#BA68C8")))
                            }
                            return true
                        }
                        if (magicBtn.onTouchDown(pointerId, touchX, touchY)) {
                            val proj = player.castMagic()
                            if (proj != null) {
                                projectiles.add(proj)
                                triggerScreenShake(0.08f, 3f)
                                floatingTexts.add(FloatingText("سحر!", player.x, player.y - 25f, Color.parseColor("#00E5FF")))
                            } else {
                                floatingTexts.add(FloatingText("طاقة السحر لا تكفي!", player.x, player.y - 25f, Color.LTGRAY))
                            }
                            return true
                        }
                        if (bombBtn.onTouchDown(pointerId, touchX, touchY)) {
                            val bomb = player.throwBomb()
                            if (bomb != null) {
                                projectiles.add(bomb)
                                triggerScreenShake(0.12f, 5f)
                                floatingTexts.add(FloatingText("قنبلة!", player.x, player.y - 25f, Color.parseColor("#FF6D00")))
                            } else {
                                floatingTexts.add(FloatingText("لا توجد قنابل!", player.x, player.y - 25f, Color.LTGRAY))
                            }
                            return true
                        }
                        if (potionBtn.onTouchDown(pointerId, touchX, touchY)) {
                            if (player.usePotion()) {
                                floatingTexts.add(FloatingText("+85 صحة!", player.x, player.y - 30f, Color.GREEN))
                            }
                            return true
                        }
                        if (interactBtn.onTouchDown(pointerId, touchX, touchY)) {
                            handleInteract()
                            return true
                        }
                    }
                    MotionEvent.ACTION_MOVE -> {
                        for (i in 0 until event.pointerCount) {
                            val id = event.getPointerId(i)
                            joystick.onTouchMove(id, event.getX(i), event.getY(i))
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                        joystick.onTouchUp(pointerId)
                        attackBtn.onTouchUp(pointerId)
                        dashBtn.onTouchUp(pointerId)
                        magicBtn.onTouchUp(pointerId)
                        bombBtn.onTouchUp(pointerId)
                        potionBtn.onTouchUp(pointerId)
                        interactBtn.onTouchUp(pointerId)
                    }
                }
                return true
            }
        }
    }
}
