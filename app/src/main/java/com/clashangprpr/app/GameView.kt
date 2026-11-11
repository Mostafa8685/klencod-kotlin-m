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
import com.clashangprpr.app.model.Enemy
import com.clashangprpr.app.model.EraType
import com.clashangprpr.app.model.FloatingText
import com.clashangprpr.app.model.GameCatalogue
import com.clashangprpr.app.model.ItemType
import com.clashangprpr.app.model.MerchantNPC
import com.clashangprpr.app.model.Player
import com.clashangprpr.app.model.ShopItem
import com.clashangprpr.app.model.TimePortal
import com.clashangprpr.app.model.TreasureChest
import com.clashangprpr.app.model.WorldProp
import com.clashangprpr.app.story.DialogueLine
import com.clashangprpr.app.story.StoryManager

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    private var gameThread: Thread? = null
    @Volatile private var isRunning = false

    private val mapWidth = 2600f
    private val mapHeight = 1800f

    private var camera: Camera2D = Camera2D(1280f, 720f, mapWidth, mapHeight)
    private var joystick: VirtualJoystick = VirtualJoystick(200f, 540f, 130f, 55f)
    private var attackBtn: ActionButton = ActionButton(1120f, 550f, 80f, "ضربة", Color.parseColor("#E53935"))
    private var interactBtn: ActionButton = ActionButton(960f, 570f, 60f, "تفاعل", Color.parseColor("#1E88E5"))
    private var potionBtn: ActionButton = ActionButton(960f, 430f, 55f, "🧪 علاج", Color.parseColor("#43A047"))

    private var currentEra: EraType = EraType.ERA_1_MEDIEVAL
    private lateinit var player: Player
    private val enemies = mutableListOf<Enemy>()
    private val chests = mutableListOf<TreasureChest>()
    private val props = mutableListOf<WorldProp>()
    private val coinDrops = mutableListOf<CoinDrop>()
    private val floatingTexts = mutableListOf<FloatingText>()
    private var merchant: MerchantNPC? = null
    private lateinit var portal: TimePortal

    // Dialogue State
    private var isDialogueOpen = false
    private var dialogueQueue = mutableListOf<DialogueLine>()
    private var currentDialogue: DialogueLine? = null

    // Shop State
    private var isShopOpen = false
    private val shopItemRects = mutableListOf<Pair<RectF, ShopItem>>()
    private val shopCloseRect = RectF()

    // Game States
    enum class GameState { TITLE, PLAYING, GAME_OVER, VICTORY }
    private var state = GameState.TITLE

    private var tileBitmap: Bitmap? = null

    init {
        holder.addCallback(this)
        isFocusable = true
        initGame()
    }

    private fun initGame() {
        try {
            player = Player(context, 350f, 450f)
            portal = TimePortal(mapWidth - 300f, mapHeight / 2f)
            setupEra(currentEra)

            val tileAsset = when (currentEra) {
                EraType.ERA_1_MEDIEVAL -> "environment/medieval_tiles.png"
                EraType.ERA_2_CAVES -> "environment/cave_totem.png"
                EraType.ERA_3_ZOMBIE -> "environment/chest.png"
            }
            context.assets.open(tileAsset).use { input ->
                tileBitmap = BitmapFactory.decodeStream(input)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupEra(era: EraType) {
        try {
            currentEra = era
            enemies.clear()
            chests.clear()
            props.clear()
            coinDrops.clear()
            floatingTexts.clear()

            player.x = 350f
            player.y = 450f
            player.hp = player.maxHp
            player.isDead = false
            portal.isActive = false

            // 1. Merchant NPC in Castle Courtyard
            if (era == EraType.ERA_1_MEDIEVAL) {
                merchant = MerchantNPC(context, 550f, 320f)
                // Campfires near merchant
                props.add(WorldProp(context, "environment/fire.png", 5, 480f, 330f, 64f, 64f))
                props.add(WorldProp(context, "environment/torch.png", 4, 300f, 200f, 32f, 64f))
                props.add(WorldProp(context, "environment/torch.png", 4, 800f, 200f, 32f, 64f))
                props.add(WorldProp(context, "environment/gate.png", 6, 1200f, 400f, 160f, 100f))
            } else {
                merchant = null
            }

            // 2. Treasure Chests
            chests.add(TreasureChest(context, 200f, 300f, 45))
            chests.add(TreasureChest(context, 1100f, 850f, 55))
            chests.add(TreasureChest(context, mapWidth - 400f, 300f, 70))

            // 3. Boss and Enemies
            val boss = Enemy(context, mapWidth - 450f, mapHeight / 2f, isBoss = true, era = era)
            enemies.add(boss)

            val minionCount = if (era == EraType.ERA_3_ZOMBIE) 8 else 5
            for (i in 0 until minionCount) {
                val ex = 750f + (i * 240f) % (mapWidth - 900f)
                val ey = 400f + (i * 200f) % (mapHeight - 700f)
                enemies.add(Enemy(context, ex, ey, isBoss = false, era = era))
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
            joystick.setCenter(200f, h - 180f)
            attackBtn.x = w - 160f
            attackBtn.y = h - 160f
            interactBtn.x = w - 320f
            interactBtn.y = h - 140f
            potionBtn.x = w - 320f
            potionBtn.y = h - 280f
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
        if (state != GameState.PLAYING || isDialogueOpen || isShopOpen) return

        player.update(deltaTime, joystick.deltaX, joystick.deltaY, mapWidth, mapHeight)
        camera.update(player.x, player.y)

        merchant?.update(deltaTime)
        portal.update(deltaTime)

        for (prop in props) prop.update(deltaTime)

        // Coin drops collection
        val itCoins = coinDrops.iterator()
        while (itCoins.hasNext()) {
            val c = itCoins.next()
            c.update(deltaTime)
            if (c.isPlayerNear(player.x + player.width / 2, player.y + player.height / 2)) {
                player.coins += c.value
                floatingTexts.add(FloatingText("+${c.value} 🪙", player.x, player.y - 20f, Color.parseColor("#FFD700")))
                itCoins.remove()
            }
        }

        // Floating texts update
        val itFloat = floatingTexts.iterator()
        while (itFloat.hasNext()) {
            if (!itFloat.next().update(deltaTime)) itFloat.remove()
        }

        // Update enemies
        val activeEnemies = enemies.filter { !it.isDead }
        if (activeEnemies.isEmpty() && !portal.isActive) {
            portal.isActive = true
        }

        for (enemy in enemies) {
            enemy.update(deltaTime, player)
        }

        // Player attacks enemies
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
                    floatingTexts.add(FloatingText("-$dmg", enemy.x + 20f, enemy.y, Color.parseColor("#FF1744")))

                    if (enemy.isDead) {
                        val goldAmount = if (enemy.isBoss) 60 else 15
                        coinDrops.add(CoinDrop(enemy.x + enemy.width / 2, enemy.y + enemy.height / 2, goldAmount))

                        if (enemy.isBoss) {
                            showDialogue(StoryManager.getEraClearDialogue(currentEra))
                        }
                    }
                }
            }
        }

        // Dynamic Interact button label
        val px = player.x + player.width / 2
        val py = player.y + player.height / 2
        when {
            merchant != null && merchant!!.isPlayerNear(px, py) -> {
                interactBtn = ActionButton(interactBtn.x, interactBtn.y, interactBtn.radius, "🛒 متجر", Color.parseColor("#FF9800"))
            }
            chests.any { it.isPlayerNear(px, py) && !it.isOpened } -> {
                interactBtn = ActionButton(interactBtn.x, interactBtn.y, interactBtn.radius, "📦 فتح", Color.parseColor("#FBC02D"))
            }
            portal.isActive && portal.isPlayerInside(px, py) -> {
                interactBtn = ActionButton(interactBtn.x, interactBtn.y, interactBtn.radius, "🌀 عبور", Color.parseColor("#00E5FF"))
            }
            else -> {
                interactBtn = ActionButton(interactBtn.x, interactBtn.y, interactBtn.radius, "تفاعل", Color.parseColor("#1E88E5"))
            }
        }

        if (player.isDead) {
            state = GameState.GAME_OVER
        }
    }

    private fun handleInteract() {
        val px = player.x + player.width / 2
        val py = player.y + player.height / 2

        // Check Merchant
        if (merchant != null && merchant!!.isPlayerNear(px, py)) {
            isShopOpen = true
            return
        }

        // Check Chests
        for (ch in chests) {
            if (ch.isPlayerNear(px, py) && !ch.isOpened) {
                val gold = ch.open()
                player.coins += gold
                floatingTexts.add(FloatingText("+$gold 🪙 صندوق كنز!", px - 40f, py - 40f, Color.parseColor("#FFD700")))
                return
            }
        }

        // Check Portal
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
        canvas.drawColor(currentEra.bgColor)

        canvas.save()
        canvas.translate(-camera.x, -camera.y)

        // Draw Castle Stone Tiles / Ground
        val floorTilePaint = Paint().apply {
            color = Color.argb(45, 200, 200, 220)
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val stoneFill = Paint().apply {
            color = Color.argb(30, 100, 110, 130)
            style = Paint.Style.FILL
        }

        val tileSize = 128f
        var tx = 0f
        while (tx < mapWidth) {
            var ty = 0f
            while (ty < mapHeight) {
                canvas.drawRect(tx, ty, tx + tileSize, ty + tileSize, stoneFill)
                canvas.drawRect(tx, ty, tx + tileSize, ty + tileSize, floorTilePaint)
                ty += tileSize
            }
            tx += tileSize
        }

        // Draw Props (Campfires, Torches, Gate)
        for (prop in props) prop.draw(canvas)

        // Draw Chests
        for (ch in chests) ch.draw(canvas)

        // Draw Merchant
        merchant?.draw(canvas)

        // Draw Portal
        portal.draw(canvas)

        // Draw Coins Drops
        for (c in coinDrops) c.draw(canvas)

        // Draw Enemies
        for (enemy in enemies) enemy.draw(canvas)

        // Draw Player
        player.draw(canvas)

        // Draw Floating texts
        for (ft in floatingTexts) ft.draw(canvas)

        canvas.restore()

        // Screen UI HUD
        drawHUD(canvas)

        if (!isDialogueOpen && !isShopOpen) {
            joystick.draw(canvas)
            attackBtn.draw(canvas)
            interactBtn.draw(canvas)
            potionBtn.draw(canvas)
        }

        if (isDialogueOpen) drawDialogueBox(canvas)
        if (isShopOpen) drawShopOverlay(canvas)
    }

    private fun drawHUD(canvas: Canvas) {
        val p = Paint()
        val screenW = width.toFloat().coerceAtLeast(1f)

        // HP Bar Box
        p.color = Color.argb(200, 20, 20, 30)
        canvas.drawRoundRect(40f, 35f, 380f, 130f, 16f, 16f, p)
        p.color = Color.parseColor("#FFD700")
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3f
        canvas.drawRoundRect(40f, 35f, 380f, 130f, 16f, 16f, p)
        p.style = Paint.Style.FILL

        // Green HP Bar
        p.color = Color.parseColor("#2E7D32")
        val hpWidth = (320f * (player.hp.toFloat() / player.maxHp)).coerceAtLeast(0f)
        canvas.drawRoundRect(50f, 45f, 50f + hpWidth, 75f, 8f, 8f, p)

        p.color = Color.WHITE
        p.textSize = 24f
        p.isFakeBoldText = true
        canvas.drawText("فارس الزمن: ${player.hp}/${player.maxHp} HP", 60f, 68f, p)

        // Weapon & Coins Info
        p.textSize = 22f
        p.color = Color.parseColor("#FFD700")
        canvas.drawText("💰 الذهب: ${player.coins}", 55f, 115f, p)

        p.color = Color.parseColor("#80D8FF")
        canvas.drawText("🗡️ السلاح: ${player.equippedWeapon.nameAr}", 200f, 115f, p)

        // Era Title in Center
        p.textAlign = Paint.Align.CENTER
        p.textSize = 30f
        p.color = Color.parseColor("#FFD700")
        canvas.drawText(currentEra.title, screenW / 2f, 50f, p)
        p.textSize = 20f
        p.color = Color.LTGRAY
        canvas.drawText(currentEra.subtitle, screenW / 2f, 80f, p)

        // Enemies count
        val left = enemies.count { !it.isDead }
        p.textAlign = Paint.Align.RIGHT
        p.textSize = 24f
        p.color = Color.WHITE
        val statusText = if (left == 0) "البوابة الزمنية مفتوحة! 🌀" else "الوحوش: $left"
        canvas.drawText(statusText, screenW - 40f, 60f, p)
    }

    private fun drawShopOverlay(canvas: Canvas) {
        val screenW = width.toFloat().coerceAtLeast(1f)
        val screenH = height.toFloat().coerceAtLeast(1f)

        // Semi-transparent dim background
        canvas.drawColor(Color.argb(220, 10, 12, 18))

        val panelRect = RectF(100f, 60f, screenW - 100f, screenH - 60f)
        val p = Paint()

        // Shop panel
        p.color = Color.argb(245, 25, 28, 40)
        canvas.drawRoundRect(panelRect, 24f, 24f, p)
        p.color = Color.parseColor("#FFB300")
        p.style = Paint.Style.STROKE
        p.strokeWidth = 5f
        canvas.drawRoundRect(panelRect, 24f, 24f, p)

        // Header Title
        p.style = Paint.Style.FILL
        p.color = Color.parseColor("#FFD700")
        p.textSize = 38f
        p.isFakeBoldText = true
        p.textAlign = Paint.Align.CENTER
        canvas.drawText("🏪 متجر القلعة - التاجر إلريك", screenW / 2f, 120f, p)

        p.textSize = 26f
        p.color = Color.WHITE
        canvas.drawText("رصيدك الحالي: ${player.coins} 🪙 ذهب  |  الجرعات: ${player.potions} 🧪", screenW / 2f, 160f, p)

        // Draw Shop Items Cards
        shopItemRects.clear()
        val cardW = (panelRect.width() - 80f) / 2f
        val cardH = 130f
        val startX = panelRect.left + 30f
        val startY = 190f

        for (i in GameCatalogue.shopItems.indices) {
            val item = GameCatalogue.shopItems[i]
            val col = i % 2
            val row = i / 2

            val cx = startX + col * (cardW + 20f)
            val cy = startY + row * (cardH + 20f)
            val cardRect = RectF(cx, cy, cx + cardW, cy + cardH)
            shopItemRects.add(Pair(cardRect, item))

            // Card BG
            p.color = Color.argb(200, 38, 43, 62)
            canvas.drawRoundRect(cardRect, 14f, 14f, p)

            // Card Border
            val canAfford = player.coins >= item.price
            p.color = if (canAfford) Color.parseColor("#81C784") else Color.parseColor("#E57373")
            p.style = Paint.Style.STROKE
            p.strokeWidth = 2f
            canvas.drawRoundRect(cardRect, 14f, 14f, p)
            p.style = Paint.Style.FILL

            // Item Title
            p.textAlign = Paint.Align.LEFT
            p.textSize = 24f
            p.color = Color.WHITE
            canvas.drawText(item.titleAr, cx + 20f, cy + 38f, p)

            // Item Desc
            p.textSize = 18f
            p.color = Color.LTGRAY
            canvas.drawText(item.descAr, cx + 20f, cy + 70f, p)

            // Price Button
            p.textSize = 24f
            p.isFakeBoldText = true
            p.color = if (canAfford) Color.parseColor("#FFD700") else Color.GRAY
            val buyText = "شراء (${item.price} 🪙)"
            canvas.drawText(buyText, cx + 20f, cy + 110f, p)
        }

        // Close Button
        shopCloseRect.set(screenW / 2f - 140f, panelRect.bottom - 75f, screenW / 2f + 140f, panelRect.bottom - 20f)
        p.color = Color.parseColor("#E53935")
        canvas.drawRoundRect(shopCloseRect, 14f, 14f, p)
        p.color = Color.WHITE
        p.textAlign = Paint.Align.CENTER
        p.textSize = 28f
        p.isFakeBoldText = true
        canvas.drawText("إغلاق المتجر (Close)", screenW / 2f, panelRect.bottom - 38f, p)
    }

    private fun handleShopTouch(tx: Float, ty: Float) {
        if (shopCloseRect.contains(tx, ty)) {
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
                            floatingTexts.add(FloatingText("+2 جرعات علاج! 🧪", player.x, player.y - 30f, Color.GREEN))
                        }
                        ItemType.WEAPON -> {
                            if (item.id == "sword_fire") {
                                player.equippedWeapon = GameCatalogue.WEAPON_FIRE
                                floatingTexts.add(FloatingText("تجهيز سيف النار! 🔥", player.x, player.y - 30f, Color.parseColor("#FF5722")))
                            } else if (item.id == "spear_chrono") {
                                player.equippedWeapon = GameCatalogue.WEAPON_CHRONO
                                floatingTexts.add(FloatingText("تجهيز رمح كرونو! ⚡", player.x, player.y - 30f, Color.parseColor("#00E5FF")))
                            }
                        }
                        ItemType.ARMOR -> {
                            player.hasRoyalArmor = true
                            player.maxHp = 250
                            player.hp = (player.hp + 100).coerceAtMost(player.maxHp)
                            floatingTexts.add(FloatingText("ارتداء درع الفارس! 🛡️ (+100 HP)", player.x, player.y - 30f, Color.YELLOW))
                        }
                    }
                } else {
                    floatingTexts.add(FloatingText("الذهب غير كافٍ! ❌", player.x, player.y - 30f, Color.RED))
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
        p.textSize = 32f
        p.isFakeBoldText = true
        p.textAlign = Paint.Align.LEFT
        canvas.drawText(dialogue.speaker, 110f, screenH - 230f, p)

        p.color = Color.WHITE
        p.textSize = 26f
        p.isFakeBoldText = false
        canvas.drawText(dialogue.textAr, 110f, screenH - 175f, p)

        p.color = Color.LTGRAY
        p.textSize = 22f
        canvas.drawText(dialogue.textEn, 110f, screenH - 120f, p)

        p.color = Color.parseColor("#4FC3F7")
        p.textSize = 20f
        p.textAlign = Paint.Align.RIGHT
        canvas.drawText("اضغط للمتابعة (Tap to continue) ▶", screenW - 110f, screenH - 60f, p)
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
        canvas.drawText("حرب الفارس", screenW / 2f, screenH / 2f - 90f, p)

        p.color = Color.WHITE
        p.textSize = 38f
        canvas.drawText("صراع العصور والوحوش - Knight War", screenW / 2f, screenH / 2f - 20f, p)

        p.color = Color.LTGRAY
        p.textSize = 24f
        canvas.drawText("قلاع، أسلحة، تجار، صناديق كنوز، وعوالم من القرون الوسطى حتى الزومبي", screenW / 2f, screenH / 2f + 40f, p)

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
        canvas.drawText("تم القضاء على وحوش العصور والزعماء بنجاح!", screenW / 2f, screenH / 2f + 60f, p)

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
                        if (potionBtn.onTouchDown(pointerId, touchX, touchY)) {
                            if (player.usePotion()) {
                                floatingTexts.add(FloatingText("+70 HP شفاء! 🧪", player.x, player.y - 30f, Color.GREEN))
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
                        interactBtn.onTouchUp(pointerId)
                        potionBtn.onTouchUp(pointerId)
                    }
                }
                return true
            }
        }
    }
}
