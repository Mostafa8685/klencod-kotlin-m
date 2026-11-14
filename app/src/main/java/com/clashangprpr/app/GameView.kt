package com.clashangprpr.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.clashangprpr.app.engine.ActionButton
import com.clashangprpr.app.engine.Assets
import com.clashangprpr.app.engine.Camera2D
import com.clashangprpr.app.engine.SaveStore
import com.clashangprpr.app.engine.Sound
import com.clashangprpr.app.engine.SpriteSheet
import com.clashangprpr.app.engine.Ui
import com.clashangprpr.app.engine.LanMultiplayer
import com.clashangprpr.app.engine.VirtualJoystick
import com.clashangprpr.app.model.*
import com.clashangprpr.app.story.DialogueLine
import com.clashangprpr.app.story.Story
import com.clashangprpr.app.world.Collider
import com.clashangprpr.app.world.OpenWorld
import com.clashangprpr.app.world.Pt
import com.clashangprpr.app.world.TileMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    private enum class State { TITLE, PLAYING, GAME_OVER, VICTORY }
    private enum class Modal { NONE, SHOP, CRAFT, PAUSE, DIALOGUE, BAG, VILLAGER, TRAVEL, FURNACE, HUD_EDIT, BOSS_ALTAR, STORAGE, WORLD_MAP, MULTIPLAYER }
    private enum class Target {
        NONE, SHOP, CRAFT, GATE, CHEST, MOUND, PORTAL, COOK, SLEEP,
        FURNACE, CAMPFIRE, CAVE_ENTER, CAVE_EXIT, CAVE_NEXT, TOTEM, VILLAGER, BOAT_DOCK, BOSS_ALTAR, STORAGE_CHEST, CANNON_REFILL, FARM_HARVEST, STRUCTURE_NODE, REVIVE_PARTNER
    }

    private class Touch(val type: Int, val id: Int, val x: Float, val y: Float)
    private class Hit(val rect: RectF, val id: String)

    // ---------------------------------------------------------------- boot
    private val boot: Boolean = run {
        Assets.init(context)
        SaveStore.init(context)
        Sound.init(context)
        Sound.sfxOn = SaveStore.bool("sfx", true)
        Sound.musicOn = SaveStore.bool("music", true)
        LanMultiplayer.startDiscoveryListener()
        true
    }

    private var thread: Thread? = null
    @Volatile private var running = false
    private val queue = ConcurrentLinkedQueue<Touch>()
    @Volatile private var surfW = 0
    @Volatile private var surfH = 0
    private var curW = 0
    private var curH = 0

    private var state = State.TITLE
    private var modal = Modal.NONE
    private var player = Player(300f, 880f)
    private var era = EraType.ERA_1_MEDIEVAL
    private var world = TileMap(EraType.ERA_1_MEDIEVAL, 1077L)
    private var camera = Camera2D(world.width, world.height)
    private var zoom = 1f
    private var time = 0f
    private var titleT = 0f
    private var endT = 0f
    private var fade = 1f
    private var shake = 0f
    private var hitStop = 0f
    private var deathTimer = 0f
    private var hurtFlash = 0f
    private var toastText = ""
    private var toastTimer = 0f
    private var bannerTimer = 0f
    private var waveTimer = 6f
    private val rnd = java.util.Random(99)

    // world lists
    private val enemies = ArrayList<Enemy>()
    private val projectiles = ArrayList<Projectile>()
    private val explosions = ArrayList<ExplosionFx>()
    private val floats = ArrayList<FloatingText>()
    private val coins = ArrayList<CoinDrop>()
    private val chests = ArrayList<TreasureChest>()
    private val crates = ArrayList<WoodCrate>()
    private val rocks = ArrayList<MiningRock>()
    private val trees = ArrayList<ChoppableTree>()
    private val traps = ArrayList<DungeonTrap>()
    private val props = ArrayList<WorldProp>()
    private val gens = ArrayList<PowerGenerator>()
    private val particles = ParticleSystem()
    private var merchant = MerchantNPC(0f, 0f)
    private var table = CraftTable(0f, 0f)
    private var portal = TimePortal(0f, 0f)
    private var mound: BuriedMound? = null
    private var boss: Enemy? = null
    private var bossDown = false
    private var greeted = false
    private var target = Target.NONE
    private var targetChest: TreasureChest? = null
    private var targetMound: BuriedMound? = null
    private var targetCave: CaveEntrance? = null
    private var targetCamp: CampfireNode? = null
    private var targetTotem: CaveTotem? = null
    private var targetVillager: VillagerNPC? = null
    private val atkRect = RectF()

    // open world + cave transition + Minecraft-style building
    private var ow: OpenWorld? = null
    private var inCave = false
    private var caveMap: TileMap? = null
    private var caveFloor = 1
    private var activeCave: CaveEntrance? = null
    private var surfaceX = 0f
    private var surfaceY = 0f
    private var caveExitSurface = CaveExitNode(0f, 0f, false, 1)
    private var caveExitNext: CaveExitNode? = null

    private val beds = ArrayList<Bed>()
    private val furnaces = ArrayList<FurnaceObj>()
    private val campfires = ArrayList<CampfireNode>()
    private val caveEntrances = ArrayList<CaveEntrance>()
    private val cocoons = ArrayList<CaveCocoon>()
    private val totems = ArrayList<CaveTotem>()
    private val cannons = ArrayList<CannonTurret>()
    private val banners = ArrayList<FortressBanner>()
    private val docks = ArrayList<BoatDock>()
    private val mounds = ArrayList<BuriedMound>()
    private val sheeps = ArrayList<Sheep>()
    private val villagers = ArrayList<VillagerNPC>()
    private val owPortals = ArrayList<TimePortal>()
    private val quests = QuestBoard.createDefaultQuests()

    private var dayTime = 0f
    private var dayNo = 1
    private var nightFlag = false
    private var owSpawnTimer = 5f
    private var owSaveTimer = 0f
    private var owExploreTimer = 0f
    private var owRegrowTimer = 0f
    private var lowQuality = false
    private var nightPaint: Paint? = null
    private val shopBought = HashMap<String, Int>()
    private val dayLength = 600f
    private var hunger = 80f
    private var sinceHurt = 10f
    private var regenTimer = 0f
    private var starveTimer = 0f
    private var hungerWarned = false
    private var eyeTimer = 4f
    private var heartTimer = 0f
    private var stalker: Enemy? = null
    private class Eyes(val x: Float, val y: Float, var life: Float, val seed: Float)
    private val eyes = ArrayList<Eyes>()

    private val eatBtn = ActionButton("icons/apple.png", "أكل", Color.parseColor("#FF8F00"), null, "eat").apply { visible = false }
    private val bagBtn = ActionButton("icons/bag.png", "حقيبة", Color.parseColor("#8D6E63"), null, "bag").apply { visible = true }
    private val buildBtn = ActionButton("icons/block_wood.png", "بناء/كسر", Color.parseColor("#FFA000"), null, "build").apply { visible = false }
    private val turboWallBtn = ActionButton("icons/block_stone_brick.png", "جدار فوري", Color.parseColor("#29B6F6"), null, "turbowall").apply { visible = false }
    private val editBlockBtn = ActionButton("icons/block_window.png", "تعديل/نافذة", Color.parseColor("#FFCA28"), null, "editblk").apply { visible = false }
    private val parryBtn = ActionButton("ui/ico/shield.png", "صد عاكس", Color.parseColor("#00E5FF"), Color.WHITE, "parry").apply { visible = true }
    private val grappleBtn = ActionButton("icons/grapple.png", "خطاف", Color.parseColor("#26A69A"), null, "grapple").apply { visible = true }
    private val spellSwitchBtn = ActionButton("icons/pot10.png", "تبديل سحر", Color.parseColor("#7E57C2"), null, "spellsw").apply { visible = true }
    private val hotbarRects = Array(9) { RectF() }
    private var gridSelIdx = 0
    private val weaponSwitchBtn = ActionButton("icons/dagger_silver.png", "تبديل سلاح", Color.parseColor("#FFB300"), null, "wepsw").apply { visible = true }
    private val farmPlantBtn = ActionButton("icons/sapling.png", "زراعة/مدفع", Color.parseColor("#66BB6A"), null, "farmbtn").apply { visible = false }
    private var craftCatTab = 0
    private var bossAltar = ColossalBossAltar(0f, 0f)
    private var colossalBoss: Enemy? = null
    private val meteorZones = ArrayList<MeteorStrikeZone>()
    private val storageChests = ArrayList<StorageChest>()
    private val golems = ArrayList<IronGolemGuard>()
    /** 0 = Sunny ☀️, 1 = Heavy Rain (auto-irrigates crops) 🌧️, 2 = Thunderstorm ⚡, 3 = Blood Moon Fog 🩸 */
    private var weatherType = 0
    private var weatherTimer = 75f
    private var trapTickTimer = 0f
    private val structureNodes = ArrayList<AncientStructureNode>()
    private var targetStructure: AncientStructureNode? = null
    /** Category filter in Minecraft Bag Grid: 0=All, 1=Blocks/Structures, 2=Weapons/Gear, 3=Farming/Powder, 4=Food/Potions */
    private var bagFilterCat = 0
    /** Active Compass Waypoint set from the Interactive World Map */
    private var waypointX = -1f
    private var waypointY = -1f
    private var waypointTitle = ""
    private var lightningFlash = 0f
    // Co-Op LAN / Hotspot Multiplayer state
    private val coopPartner = CoopPartner(0f, 0f)
    private var netSyncTimer = 0f
    private var mpInputCode = ""
    private var mpWorldChoiceNew = false
    private var playerDownedTimer = 0f

    private class AmbientMote(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var size: Float,
        var color: Int,
        var phase: Float
    )
    private val ambientMotes = ArrayList<AmbientMote>()
    private var targetCannon: CannonTurret? = null
    private var targetHarvestTx = -1
    private var targetHarvestTy = -1
    private var furnaceSmeltCount = 0
    private var hudSelectedKey = "attack"
    private var hudDraggingKey: String? = null
    private val craftGrid4x4 = CharArray(16) { '.' }
    private var activeBrush4x4: Char = 'W'

    private val biomeNames = arrayOf(
        "مملكة القلعة والقرى", "أودية الكهوف المظلمة", "أطلال مدينة الزومبي",
        "بحر وجزر القراصنة", "القطاع صفر (سايبر)", "صدع الفراغ الكوني"
    )
    private val collider: Collider
        get() = if (inCave && caveMap != null) caveMap!! else (ow ?: world)

    // ui
    private val joystick = VirtualJoystick()
    private val attackBtn = ActionButton("icons/item_1.png", "هجوم", Color.parseColor("#FF7043"), null, "attack")
    private val dashBtn = ActionButton("ui/ico/arr_r.png", "اندفاع", Color.parseColor("#26C6DA"), Color.WHITE, "dash")
    private val magicBtn = ActionButton("ui/ico/cross.png", "سحر", Color.parseColor("#AB47BC"), Color.WHITE, "magic")
    private val slowBtn = ActionButton("ui/ico/refresh.png", "إبطاء", Color.parseColor("#5C6BC0"), Color.WHITE, "slow")
    private val bombBtn = ActionButton("icons/bomb.png", "قنبلة", Color.parseColor("#EF5350"), null, "bomb")
    private val potionBtn = ActionButton("icons/pot1.png", "علاج", Color.parseColor("#66BB6A"), null, "potion")
    private val interactBtn = ActionButton("ui/ico/excl.png", "تفاعل", Color.parseColor("#FFCA28"), Color.WHITE, "interact")

    private val allHudButtons: List<ActionButton>
        get() = listOf(
            attackBtn, weaponSwitchBtn, farmPlantBtn, buildBtn, turboWallBtn, editBlockBtn, parryBtn, grappleBtn,
            dashBtn, magicBtn, spellSwitchBtn, slowBtn, bombBtn, potionBtn, interactBtn, eatBtn, bagBtn
        )
    private var pauseCx = 0f
    private var pauseCy = 0f
    private var pauseR = 0f
    private var vignette: Paint? = null
    private var darkPaint: Paint? = null
    private val screenPaint = Paint()

    // modal state
    private val hits = ArrayList<Hit>()
    private var shopTab = 0
    private var bagTab = 0
    private var scrollY = 0f
    private var selectedId = ""
    private var listRect = RectF()
    private var maxScroll = 0f
    private var mPointer = -1
    private var mDownX = 0f
    private var mDownY = 0f
    private var mLastY = 0f
    private var mMoved = false
    private var dialogue: List<DialogueLine> = emptyList()
    private var dialogueIdx = 0
    private var dialogueTime = 0f
    private var dialogueDone: (() -> Unit)? = null
    private val tmpR = RectF()

    init {
        holder.addCallback(this)
        isFocusable = true
        world.build(Pt(300f, 880f), emptyList())
        lowQuality = SaveStore.bool("lowq", false)
        particles.maxCount = if (lowQuality) 90 else 380
        Sound.playMusic(1)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        applySizing()
    }

    private fun applySizing() {
        if (width <= 0 || height <= 0) return
        if (lowQuality) holder.setFixedSize((width * 0.62f).toInt(), (height * 0.62f).toInt())
        else holder.setSizeFromLayout()
    }

    private fun toggleQuality() {
        lowQuality = !lowQuality
        SaveStore.putAll(mapOf("lowq" to lowQuality), false)
        particles.maxCount = if (lowQuality) 90 else 380
        post { applySizing() }
    }

    // ---------------------------------------------------------------- lifecycle
    override fun surfaceCreated(h: SurfaceHolder) {
        startLoop()
    }

    override fun surfaceChanged(h: SurfaceHolder, format: Int, w: Int, hh: Int) {
        surfW = w
        surfH = hh
    }

    override fun surfaceDestroyed(h: SurfaceHolder) {
        stopLoop()
    }

    private fun startLoop() {
        if (thread != null) return
        running = true
        val t = Thread(this)
        thread = t
        t.start()
    }

    private fun stopLoop() {
        running = false
        try {
            thread?.join(700)
        } catch (e: InterruptedException) {
        }
        thread = null
    }

    fun pause() {
        stopLoop()
        if (state == State.PLAYING) {
            saveProgress()
            if (modal == Modal.NONE) modal = Modal.PAUSE
        }
        Sound.pauseAll()
    }

    fun resume() {
        Sound.resumeAll()
        if (holder.surface != null && holder.surface.isValid) startLoop()
    }

    override fun run() {
        var last = System.nanoTime()
        while (running) {
            val now = System.nanoTime()
            var dt = (now - last) / 1_000_000_000f
            last = now
            if (dt > 0.05f) dt = 0.05f
            if (surfW > 0 && surfH > 0 && (surfW != curW || surfH != curH)) {
                curW = surfW
                curH = surfH
                layoutUi()
            }
            if (curW > 0) {
                try {
                    processInput()
                    update(dt)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                var c: Canvas? = null
                try {
                    c = holder.lockCanvas()
                    if (c != null) render(c)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    if (c != null) {
                        try {
                            holder.unlockCanvasAndPost(c)
                        } catch (e: Exception) {
                        }
                    }
                }
            }
            val spent = (System.nanoTime() - now) / 1_000_000L
            val frameMs = if (lowQuality) 33L else 16L
            if (spent < frameMs) {
                try {
                    Thread.sleep(frameMs - spent)
                } catch (e: InterruptedException) {
                }
            }
        }
    }

    // ---------------------------------------------------------------- layout
    private fun layoutUi() {
        val w = curW.toFloat()
        val h = curH.toFloat()
        Ui.init(w, h)
        val u = Ui.u
        zoom = h / 720f
        camera.viewW = w / zoom
        camera.viewH = h / zoom
        fun place(b: ActionButton, dx: Float, dy: Float, r: Float) {
            b.placeDefaultOrCustom(w, h, u, dx, dy, r)
        }
        place(attackBtn, 110f, 118f, 58f)
        place(weaponSwitchBtn, 195f, 198f, 32f)
        place(farmPlantBtn, 325f, 365f, 33f)
        place(turboWallBtn, 255f, 185f, 40f)
        place(editBlockBtn, 195f, 282f, 36f)
        place(buildBtn, 318f, 195f, 38f)
        place(parryBtn, 232f, 82f, 36f)
        place(dashBtn, 332f, 82f, 35f)
        place(grappleBtn, 305f, 285f, 34f)
        place(magicBtn, 92f, 248f, 40f)
        place(spellSwitchBtn, 92f, 342f, 29f)
        place(slowBtn, 185f, 378f, 32f)
        place(bombBtn, 415f, 82f, 32f)
        place(potionBtn, 415f, 170f, 32f)
        place(eatBtn, 415f, 258f, 32f)
        place(bagBtn, 495f, 82f, 35f)
        place(interactBtn, 115f, 430f, 42f)

        // Layout 9-slot Minecraft Hotbar at bottom-center
        val slotS = 48f * u
        val gap = 4f * u
        val totalW = 9 * slotS + 8 * gap
        val startX = w / 2f - totalW / 2f - 40f * u
        val topY = h - slotS - 12f * u
        for (i in 0 until 9) {
            val sx = startX + i * (slotS + gap)
            hotbarRects[i].set(sx, topY, sx + slotS, topY + slotS)
        }

        joystick.layout(w, h, u)
        pauseCx = w - 38f * u
        pauseCy = 38f * u
        pauseR = 26f * u
        val diag = sqrt(w * w + h * h) / 2f
        val vg = Paint()
        vg.shader = RadialGradient(
            w / 2f, h / 2f, diag,
            intArrayOf(0x00000000, 0x00000000, Color.argb(255, 200, 0, 0)),
            floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP
        )
        vignette = vg
        val np = Paint()
        np.shader = RadialGradient(
            0f, 0f, 520f * u,
            intArrayOf(0x00000000, 0x00000000, Color.argb(190, 6, 8, 30), Color.argb(255, 6, 8, 30)),
            floatArrayOf(0f, 0.28f, 0.7f, 1f), Shader.TileMode.CLAMP
        )
        nightPaint = np
        buildDark()
        camera.snapTo(player.cx, player.cy)
    }

    private fun buildDark() {
        val d = if (inCave) 0.85f else era.darkness
        if (d <= 0f || curW <= 0) {
            darkPaint = null
            return
        }
        val r = 470f * Ui.u
        val a = (d * 255f).toInt().coerceIn(0, 255)
        val dp = Paint()
        dp.shader = RadialGradient(
            0f, 0f, r,
            intArrayOf(0x00000000, 0x00000000, Color.argb((a * 0.7f).toInt(), 4, 4, 14), Color.argb(a, 4, 4, 14)),
            floatArrayOf(0f, 0.30f, 0.75f, 1f), Shader.TileMode.CLAMP
        )
        darkPaint = dp
    }

    // ---------------------------------------------------------------- input
    override fun onTouchEvent(e: MotionEvent): Boolean {
        val sx = if (width > 0 && surfW > 0) surfW.toFloat() / width else 1f
        val sy = if (height > 0 && surfH > 0) surfH.toFloat() / height else 1f
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = e.actionIndex
                queue.add(Touch(0, e.getPointerId(i), e.getX(i) * sx, e.getY(i) * sy))
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until e.pointerCount) queue.add(Touch(1, e.getPointerId(i), e.getX(i) * sx, e.getY(i) * sy))
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val i = e.actionIndex
                queue.add(Touch(2, e.getPointerId(i), e.getX(i) * sx, e.getY(i) * sy))
            }
            MotionEvent.ACTION_CANCEL -> queue.add(Touch(3, -1, 0f, 0f))
        }
        return true
    }

    private fun processInput() {
        while (true) {
            val t = queue.poll() ?: break
            handleTouch(t)
        }
    }

    private fun handleTouch(t: Touch) {
        when (state) {
            State.TITLE -> if (t.type == 2) tapTitle(t.x, t.y)
            State.GAME_OVER, State.VICTORY -> if (t.type == 2) tapEnd(t.x, t.y)
            State.PLAYING -> {
                if (modal != Modal.NONE) {
                    modalTouch(t)
                    return
                }
                when (t.type) {
                    0 -> touchDown(t)
                    1 -> joystick.onMove(t.id, t.x, t.y)
                    2 -> releaseAll(t.id)
                    3 -> cancelAll()
                }
            }
        }
    }

    private fun releaseAll(id: Int) {
        joystick.onUp(id)
        for (b in allHudButtons) b.onUp(id)
    }

    private fun cancelAll() {
        joystick.cancel()
        for (b in allHudButtons) b.cancel()
        mPointer = -1
    }

    private fun touchDown(t: Touch) {
        val dx = t.x - pauseCx
        val dy = t.y - pauseCy
        if (sqrt(dx * dx + dy * dy) <= pauseR * 1.3f) {
            Sound.play("click")
            cancelAll()
            modal = Modal.PAUSE
            return
        }
        // Check if tapping the Top-Right Minimap in Open World -> open full Interactive World & Structure Map!
        if (ow != null && !inCave) {
            val u = Ui.u
            val mw = 190f * u
            val mx = curW - 14f * u - mw
            val my = 76f * u
            if (t.x >= mx - 8f * u && t.x <= mx + mw + 8f * u && t.y >= my - 8f * u && t.y <= my + mw + 28f * u) {
                Sound.play("click")
                cancelAll()
                scrollY = 0f
                modal = Modal.WORLD_MAP
                return
            }
        }
        // Check Minecraft Hotbar slots first (when in Open World)
        if (ow != null && !inCave) {
            for (i in 0 until 9) {
                if (hotbarRects[i].contains(t.x, t.y)) {
                    player.hotbarIdx = i
                    Sound.play("click", 0.6f)
                    toast("الشريط السريع: ${hotbarSlotTitle(i)}")
                    return
                }
            }
        }
        if (attackBtn.onDown(t.id, t.x, t.y)) return
        if (farmPlantBtn.onDown(t.id, t.x, t.y)) {
            doQuickFarmOrCannonAction()
            return
        }
        if (weaponSwitchBtn.onDown(t.id, t.x, t.y)) {
            val nw = player.switchNextWeapon()
            player.hotbarIdx = 0
            Sound.play("click")
            toast("بدّلت السلاح إلى: ${nw.nameAr} (ضرر ${nw.damage}) ⚔️")
            return
        }
        if (turboWallBtn.onDown(t.id, t.x, t.y)) {
            doFortniteTurboWall()
            return
        }
        if (editBlockBtn.onDown(t.id, t.x, t.y)) {
            doFortniteWallEdit()
            return
        }
        if (parryBtn.onDown(t.id, t.x, t.y)) {
            doSkillParry()
            return
        }
        if (grappleBtn.onDown(t.id, t.x, t.y)) {
            doSkillGrapple()
            return
        }
        if (buildBtn.onDown(t.id, t.x, t.y)) {
            useHotbarActionAtAim()
            return
        }
        if (dashBtn.onDown(t.id, t.x, t.y)) {
            if (player.dash()) {
                Sound.play("dash")
                particles.burst(player.cx, player.cy, 10, Color.parseColor("#26C6DA"), 160f, 5f, 0.4f)
            } else Sound.play("deny", 0.4f)
            return
        }
        if (magicBtn.onDown(t.id, t.x, t.y)) {
            if (player.castSpell(projectiles)) {
                Sound.play("magic")
                particles.burst(player.cx, player.cy, 10, player.currentSpell.color, 150f, 5f, 0.45f)
            } else Sound.play("deny", 0.5f)
            return
        }
        if (spellSwitchBtn.onDown(t.id, t.x, t.y)) {
            player.nextSpell()
            Sound.play("click")
            toast("القدرة السحرية: ${player.currentSpell.nameAr}")
            return
        }
        if (slowBtn.onDown(t.id, t.x, t.y)) {
            if (player.slowmo()) {
                Sound.play("slowmo")
                toast("الزمن يتباطأ!")
            } else Sound.play("deny", 0.5f)
            return
        }
        if (bombBtn.onDown(t.id, t.x, t.y)) {
            val b = player.throwBomb()
            if (b != null) {
                projectiles.add(b)
                Sound.play("swing")
            } else Sound.play("deny", 0.5f)
            return
        }
        if (potionBtn.onDown(t.id, t.x, t.y)) {
            if (player.usePotion()) {
                Sound.play("potion")
                addFloat("+85", player.cx, player.y, Color.parseColor("#69F0AE"), 30f)
                particles.burst(player.cx, player.cy, 14, Color.parseColor("#69F0AE"), 120f, 6f, 0.7f)
            } else Sound.play("deny", 0.5f)
            return
        }
        if (eatBtn.onDown(t.id, t.x, t.y)) {
            eatBest()
            return
        }
        if (bagBtn.onDown(t.id, t.x, t.y)) {
            openBag()
            return
        }
        if (interactBtn.onDown(t.id, t.x, t.y)) {
            doInteract()
            return
        }
        // If player taps directly in the middle of the world while holding a building block or pickaxe, place/break at tapped tile!
        if (ow != null && !inCave && player.hotbarIdx >= 1 && t.x > curW * 0.28f && t.x < curW * 0.72f && t.y > curH * 0.16f && t.y < curH * 0.80f) {
            val wx = camera.x + t.x / zoom
            val wy = camera.y + t.y / zoom
            if (dist(wx, wy, player.cx, player.cy) <= 270f) {
                val tx = floor(wx / OpenWorld.TS).toInt()
                val ty = floor(wy / OpenWorld.TS).toInt()
                useHotbarActionAtTile(tx, ty)
                return
            }
        }
        joystick.onDown(t.id, t.x, t.y)
    }

    // ---------------------------------------------------------------- Minecraft Hotbar & Block Building Helpers
    private fun hotbarSlotTitle(idx: Int): String = when (idx) {
        0 -> player.weapon.nameAr + (if (player.weaponEnchant == 1) " [مصاص دماء 🩸]" else if (player.weaponEnchant == 2) " [صاعقة ⚡]" else "")
        1 -> "معول التكسير والتعدين"
        2 -> "بلوك ألواح خشبية (${player.blkWood})"
        3 -> "بلوك طوب حجري (${player.blkStone})"
        4 -> "جدار نافذة قتالية - فورتنايت (${player.blkWindow})"
        5 -> "فخ الأشواك الفولاذية (${player.blkSpikes})"
        6 -> "حجر السبج المدرع المضاد للانفجار (${player.blkObsidian})"
        7 -> "سلك شائك لإبطاء الزومبي (${player.blkWire})"
        8 -> "بلوك متفجرات TNT (${player.blkTnt})"
        else -> ""
    }

    private fun hotbarSlotIcon(idx: Int): String = when (idx) {
        0 -> player.weapon.iconAsset
        1 -> "ui/ico/axe.png"
        2 -> "icons/block_wood.png"
        3 -> "icons/block_stone_brick.png"
        4 -> "icons/block_window.png"
        5 -> "icons/block_spikes.png"
        6 -> "icons/block_obsidian.png"
        7 -> "icons/block_wire.png"
        8 -> "icons/block_tnt.png"
        else -> "icons/item_1.png"
    }

    private fun hotbarSlotCount(idx: Int): Int = when (idx) {
        2 -> player.blkWood
        3 -> player.blkStone
        4 -> player.blkWindow
        5 -> player.blkSpikes
        6 -> player.blkObsidian
        7 -> player.blkWire
        8 -> player.blkTnt
        else -> -1
    }

    /**
     * Fortnite-style 1-Tap Turbo Wall: instantly builds a 3-tile protective barrier in front of the player
     * (or a bridge if facing deep water) without needing to switch away from your weapon!
     */
    /**
     * 1-Tap Direct World Action for Farming, Tree Planting, and Castle Wall Cannons:
     * - Harvests mature crop if facing one
     * - Refills nearby cannon with Gunpowder if low on ammo
     * - Mounts a Cannon on top of a Stone Wall if facing a wall and player has a cannon
     * - Otherwise plants a Crop Seed or Tree Sapling on the ground in front of the player!
     */
    private fun doQuickFarmOrCannonAction() {
        val w = ow ?: return
        if (inCave || player.isDead) return
        val (tx, ty) = aimTileCoords()
        if (w.isCropReadyAt(tx, ty)) {
            harvestCropTile(w, tx, ty)
            return
        }
        // Check if near a friendly cannon needing gunpowder
        for (cn in cannons) {
            if (cn.friendly && !cn.isDestroyed && cn.ammo < cn.maxAmmo && dist(cn.x + 38f, cn.y + 38f, player.cx, player.cy) < 165f && player.gunpowder > 0) {
                val put = min(cn.maxAmmo - cn.ammo, min(10, player.gunpowder))
                player.gunpowder -= put
                cn.ammo += put
                Sound.play("buy")
                particles.burst(cn.x + 38f, cn.y + 38f, 10, Color.parseColor("#FFAB40"), 150f, 5f, 0.4f)
                addFloat("+$put بارود للمدفع 💥", cn.x + 38f, cn.y - 12f, GOLD, 22f)
                toast("تمت تعبئة المدفع بالبارود (${cn.ammo}/${cn.maxAmmo})!")
                advanceQuest("q_powder", 1)
                return
            }
        }
        // Check if facing a growing crop/sapling and player has the Watering Can -> irrigate 3x3!
        if (player.hasWateringCan && (w.hasAnyCropAt(tx, ty) || w.plantedSaplings.containsKey(ty * OpenWorld.N + tx))) {
            val watered = w.waterCropsAround(tx, ty)
            if (watered > 0) {
                LanMultiplayer.send("CROP|WATER|$tx|$ty|0")
                Sound.play("potion", 0.85f)
                particles.burst(tx * OpenWorld.TS + 32f, ty * OpenWorld.TS + 32f, 12, Color.parseColor("#29B6F6"), 160f, 5f, 0.45f)
                addFloat("ريّ مائي سريع! 💧x$watered", tx * OpenWorld.TS + 32f, ty * OpenWorld.TS - 12f, Color.parseColor("#29B6F6"), 21f)
                toast("رويت $watered من المحاصيل والأشجار بدلو الماء فنضجت أسرع!")
                return
            }
        }
        // Check if facing a Stone Wall and player owns a Cannon to mount on top of the wall!
        if (w.isStoneWallTile(tx, ty) && player.cannonsItem > 0) {
            player.cannonsItem--
            cannons.add(CannonTurret(tx * OpenWorld.TS - 6f, ty * OpenWorld.TS - 10f, true, true))
            Sound.play("buy")
            toast("ثبّتّ المدفع فوق سور القلعة الحجري! سيقصف الزومبي بالبارود تلقائياً 💥")
            return
        }
        // Deploy Iron Golem Guard if player has one and taps near base!
        if (player.golemsItem > 0 && player.cropSeeds <= 0 && player.treeSaplings <= 0) {
            player.golemsItem--
            golems.add(IronGolemGuard(tx * OpenWorld.TS + 32f, ty * OpenWorld.TS + 32f))
            Sound.play("boss", 0.7f)
            toast("نشرت الغولم الحديدي الحارس 🤖! سيحرس قلعتك ومزارعك ويسحق الزومبي المفجر!")
            return
        }
        // Plant Crop Seed or Tree Sapling on the aimed floor tile!
        if (player.cropSeeds > 0 && w.plantCropAt(tx, ty)) {
            player.cropSeeds--
            LanMultiplayer.send("CROP|PLANT|$tx|$ty|${w.cropVarietyAt(tx, ty)}")
            Sound.play("chop")
            addFloat("زرعت بذور قمح 🌾", tx * OpenWorld.TS + 32f, ty * OpenWorld.TS, Color.parseColor("#FFD54F"), 20f)
            advanceQuest("q_farm", 1)
            return
        }
        if (player.treeSaplings > 0 && w.plantSaplingAt(tx, ty)) {
            player.treeSaplings--
            LanMultiplayer.send("CROP|SAPLING|$tx|$ty|0")
            Sound.play("chop")
            addFloat("زرعت شتلة شجر 🌱", tx * OpenWorld.TS + 32f, ty * OpenWorld.TS, Color.parseColor("#69F0AE"), 20f)
            advanceQuest("q_farm", 1)
            return
        }
        toast("وجّه لأرض فارغة لزراعة البذور/الشتلات، أو لسور حجري لتثبيت مدفع وتعبئته بالبارود!")
        Sound.play("deny", 0.4f)
    }

    private fun doFortniteTurboWall() {
        val w = ow ?: return
        if (inCave || player.isDead) return
        val (cxTile, cyTile) = aimTileCoords()
        val perpX = if (abs(player.aimX) > abs(player.aimY)) 0 else 1
        val perpY = if (perpX == 0) 1 else 0
        var placed = 0
        for (offset in -1..1) {
            val tx = (cxTile + perpX * offset).coerceIn(3, OpenWorld.N - 4)
            val ty = (cyTile + perpY * offset).coerceIn(3, OpenWorld.N - 4)
            val isWater = w.kindAt(ty * OpenWorld.N + tx) == OpenWorld.DEEP
            val blk = if (isWater) {
                BlockType.WOOD_BRIDGE
            } else if (player.blkStone > 0 || player.stone > 0) {
                BlockType.STONE_BRICK
            } else {
                BlockType.WOOD_WALL
            }
            if (player.blockCountFor(blk) <= 0) {
                if (blk == BlockType.STONE_BRICK && player.stone >= 1) {
                    player.stone -= 1
                    player.blkStone += 4
                } else if ((blk == BlockType.WOOD_WALL || blk == BlockType.WOOD_BRIDGE) && player.wood >= 1) {
                    player.wood -= 1
                    if (blk == BlockType.WOOD_BRIDGE) player.blkBridge += 4 else player.blkWood += 4
                }
            }
            if (player.blockCountFor(blk) > 0 && w.placeBlock(tx, ty, blk, player.cx, player.cy)) {
                player.addBlockCount(blk, -1)
                LanMultiplayer.send("BLK|$tx|$ty|${blk.code}")
                placed++
                val px = tx * OpenWorld.TS + 32f
                val py = ty * OpenWorld.TS + 32f
                particles.burst(px, py, 5, Color.parseColor("#29B6F6"), 140f, 4f, 0.32f)
            }
        }
        if (placed > 0) {
            Sound.play("chop", 0.9f)
            player.addCombo(1)
            advanceQuest("q_build", placed)
            addFloat("بناء فوري! 🧱x$placed", player.cx, player.y - 16f, Color.parseColor("#29B6F6"), 22f)
        } else {
            Sound.play("deny", 0.4f)
            toast("المربع مشغول أو نفدت بلوكات البناء السريع!")
        }
    }

    /**
     * Fortnite-style Wall Edit & Launch Pad:
     * If facing a wall, edits it into a Firing Window -> Door -> Solid Wall.
     * If facing empty floor and player has a Launch Pad, places a Launch Pad right in front!
     */
    private fun doFortniteWallEdit() {
        val w = ow ?: return
        if (inCave || player.isDead) return
        val (tx, ty) = aimTileCoords()
        val px = tx * OpenWorld.TS + 32f
        val py = ty * OpenWorld.TS + 32f
        val edited = w.editBlockAt(tx, ty)
        if (edited != 0) {
            LanMultiplayer.send("EDIT|$tx|$ty")
            Sound.play("gate", 0.85f)
            player.addCombo(2)
            advanceQuest("q_build", 1)
            advanceQuest("q_parry", 1)
            val label = when (edited) {
                BlockType.FIRING_WINDOW.code -> "تعديل: نافذة قنص قتالية! 🪟"
                BlockType.DOOR_CLOSED.code, 5 -> "تعديل: ممر باب سريع! 🚪"
                else -> "تعديل: إغلاق الجدار الدفاعي! 🧱"
            }
            particles.burst(px, py, 8, Color.parseColor("#FFCA28"), 150f, 5f, 0.35f)
            addFloat(label, px, py - 18f, Color.parseColor("#FFCA28"), 21f)
            return
        }
        // If empty floor, place a Launch Pad and leap!
        if (player.blkLaunchPad > 0 && w.placeBlock(tx, ty, BlockType.LAUNCH_PAD, player.cx, player.cy)) {
            player.blkLaunchPad--
            Sound.play("magic", 0.9f)
            particles.burst(px, py, 10, Color.parseColor("#00E5FF"), 160f, 5f, 0.45f)
            addFloat("منصة قفز هوائي! 🚀", px, py - 18f, Color.parseColor("#00E5FF"), 22f)
            advanceQuest("q_build", 1)
            return
        }
        toast("وجّه نحو جدار لتعديله لنافذة قتالية (أو أرض فارغة لوضع منصة قفز)")
        Sound.play("deny", 0.4f)
    }

    private fun doSkillParry() {
        if (player.activateParry()) {
            Sound.play("slowmo", 0.8f)
            particles.burst(player.cx, player.cy, 10, Color.parseColor("#00E5FF"), 150f, 5f, 0.35f)
            addFloat("درع الصد العاكس! 🛡️", player.cx, player.y - 16f, Color.parseColor("#00E5FF"), 22f)
        } else {
            Sound.play("deny", 0.4f)
        }
    }

    private fun doSkillGrapple() {
        if (player.grappleCd > 0f || player.isDead) {
            Sound.play("deny", 0.4f)
            return
        }
        var distStep = 340f
        while (distStep > 64f) {
            val gx = player.cx + player.aimX * distStep
            val gy = player.cy + player.aimY * distStep
            if (!collider.blocked(gx - 16f, gy - 16f, gx + 16f, gy + 16f)) {
                if (player.startGrapple(gx, gy)) {
                    Sound.play("dash", 0.95f)
                    particles.burst(gx, gy, 12, Color.parseColor("#00E5FF"), 180f, 5f, 0.45f)
                    // Check if any enemy is near the grapple point to stun & damage them
                    for (en in enemies) {
                        if (!en.isDead && dist(en.cx, en.cy, gx, gy) < 110f) {
                            en.stunTimer = 1.5f
                            damageEnemy(en, (65 * player.comboDmgMul()).toInt(), player.cx, player.cy, 0.5f, true)
                        }
                    }
                    return
                }
            }
            distStep -= 36f
        }
        Sound.play("deny", 0.4f)
    }

    private fun aimTileCoords(): Pair<Int, Int> {
        val tx = floor((player.cx + player.aimX * 68f) / OpenWorld.TS).toInt().coerceIn(2, OpenWorld.N - 3)
        val ty = floor((player.cy + player.aimY * 68f) / OpenWorld.TS).toInt().coerceIn(2, OpenWorld.N - 3)
        return Pair(tx, ty)
    }

    private fun useHotbarActionAtAim() {
        val (tx, ty) = aimTileCoords()
        useHotbarActionAtTile(tx, ty)
    }

    private fun useHotbarActionAtTile(tx: Int, ty: Int) {
        val w = ow ?: return
        if (inCave) return
        val px = tx * OpenWorld.TS + 32f
        val py = ty * OpenWorld.TS + 32f
        val existingCode = w.blockCodeAtTile(tx, ty)

        // Tapping a door toggles it open/closed unless holding Pickaxe
        if ((existingCode == BlockType.DOOR_CLOSED.code || existingCode == 5) && player.hotbarIdx != 1) {
            if (w.toggleDoorAt(tx, ty)) {
                LanMultiplayer.send("DOOR|$tx|$ty")
                Sound.play("gate", 0.7f)
                return
            }
        }

        // Check if harvesting a mature crop on this tile!
        if (w.isCropReadyAt(tx, ty)) {
            harvestCropTile(w, tx, ty)
            return
        }

        // Slot 1 = Pickaxe / Block breaker (ONLY tool that can break Stone, Village House Walls, and Stone Bricks!)
        if (player.hotbarIdx == 1) {
            if (existingCode != 0) {
                val removed = w.removeBlockAt(tx, ty)
                if (removed != null) {
                    LanMultiplayer.send("BLK|$tx|$ty|0")
                    if (removed == BlockType.TNT_BLOCK) {
                        explode(px, py)
                        LanMultiplayer.send("EXPLODE|${px.toInt()}|${py.toInt()}")
                    } else {
                        player.addBlockCount(removed, 1)
                        Sound.play("mine", 0.85f)
                        particles.burst(px, py, 8, Color.parseColor("#FFE082"), 150f, 5f, 0.4f)
                        addFloat("+1 ${removed.nameAr}", px, py - 18f, Color.parseColor("#FFE082"), 20f)
                    }
                    return
                }
            }
            val i = ty * OpenWorld.N + tx
            if (w.breakable(i)) {
                hitOpenTile(w, i, true)
                return
            }
            toast("وجّه المعول نحو جدار بيت أو حجر أو شجرة لتكسيرها")
            return
        }

        // Slots 2..8 = Place selected Minecraft block!
        val blk = player.selectedBlockType()
        if (blk != null) {
            // If a block is already there: stone blocks require Pickaxe (slot 1)!
            if (existingCode != 0) {
                if (existingCode == BlockType.STONE_BRICK.code || existingCode == BlockType.FIRING_WINDOW.code) {
                    toast("الحجر صلب جداً! اختر المعول (الخانة 2) لتكسير الحجارة ⛏️")
                    Sound.play("deny", 0.5f)
                    return
                }
                val removed = w.removeBlockAt(tx, ty)
                if (removed != null) {
                    if (removed == BlockType.TNT_BLOCK) {
                        explode(px, py)
                    } else {
                        player.addBlockCount(removed, 1)
                        Sound.play("chop", 0.8f)
                        particles.burst(px, py, 6, Color.LTGRAY, 130f, 4f, 0.35f)
                    }
                    return
                }
            }
            val have = player.blockCountFor(blk)
            if (have <= 0) {
                // Auto-craft from raw wood/stone if available!
                if (blk == BlockType.WOOD_WALL && player.wood >= 1) {
                    player.wood -= 1
                    player.blkWood += 3
                } else if (blk == BlockType.STONE_BRICK && player.stone >= 1) {
                    player.stone -= 1
                    player.blkStone += 3
                } else if (blk == BlockType.WOOD_BRIDGE && player.wood >= 1) {
                    player.wood -= 1
                    player.blkBridge += 3
                } else {
                    toast("نفد ${blk.nameAr}! اصنع المزيد من الحقيبة أو طاولة الصناعة")
                    Sound.play("deny", 0.5f)
                    return
                }
            }
            var placedCnt = 0
            if (player.buildBrushMode == 1 && blk.solid) {
                // 5-Block Line Wall Builder!
                val perpX = if (abs(player.aimX) > abs(player.aimY)) 0 else 1
                val perpY = if (perpX == 0) 1 else 0
                for (off in -2..2) {
                    if (player.blockCountFor(blk) <= 0) break
                    if (w.placeBlock(tx + perpX * off, ty + perpY * off, blk, player.cx, player.cy)) {
                        player.addBlockCount(blk, -1)
                        placedCnt++
                    }
                }
            } else if (player.buildBrushMode == 2 && blk.solid) {
                // 5x5 Hollow Room Box Builder!
                for (dy in -2..2) for (dx in -2..2) {
                    if (dx != -2 && dx != 2 && dy != -2 && dy != 2) continue
                    if (dy == 2 && dx == 0) continue // doorway opening
                    if (player.blockCountFor(blk) <= 0) break
                    if (w.placeBlock(tx + dx, ty + dy, blk, player.cx, player.cy)) {
                        player.addBlockCount(blk, -1)
                        placedCnt++
                    }
                }
            } else {
                if (w.placeBlock(tx, ty, blk, player.cx, player.cy)) {
                    player.addBlockCount(blk, -1)
                    placedCnt = 1
                }
            }
            if (placedCnt > 0) {
                if (player.buildBrushMode == 1 && blk.solid) {
                    val perpX = if (abs(player.aimX) > abs(player.aimY)) 0 else 1
                    val perpY = if (perpX == 0) 1 else 0
                    for (off in -2..2) {
                        val btx = tx + perpX * off
                        val bty = ty + perpY * off
                        if (w.blockCodeAtTile(btx, bty) == blk.code) LanMultiplayer.send("BLK|$btx|$bty|${blk.code}")
                    }
                } else if (player.buildBrushMode == 2 && blk.solid) {
                    for (dy in -2..2) for (dx in -2..2) {
                        val btx = tx + dx
                        val bty = ty + dy
                        if (w.blockCodeAtTile(btx, bty) == blk.code) LanMultiplayer.send("BLK|$btx|$bty|${blk.code}")
                    }
                } else {
                    LanMultiplayer.send("BLK|$tx|$ty|${blk.code}")
                }
                Sound.play("chop", 0.85f)
                particles.burst(px, py, 8, Color.parseColor("#FFD54F"), 135f, 4.5f, 0.35f)
                if (placedCnt > 1) addFloat("بناء هندسي x$placedCnt 🧱", px, py - 18f, Color.parseColor("#69F0AE"), 21f)
                advanceQuest("q_build", placedCnt)
            } else {
                Sound.play("deny", 0.4f)
            }
        }
    }

    private fun toast(s: String) {
        toastText = s
        toastTimer = 2.6f
    }

    private fun addFloat(s: String, x: Float, y: Float, color: Int, size: Float = 26f) {
        floats.add(FloatingText(s, x, y, color, size))
    }

    private fun advanceQuest(id: String, amount: Int = 1) {
        val q = quests.firstOrNull { it.id == id } ?: return
        if (q.claimed) return
        val wasDone = q.isComplete
        q.progress = min(q.targetCount, q.progress + amount)
        if (!wasDone && q.isComplete) {
            Sound.play("era_clear", 0.8f)
            toast("اكتملت مهمة: ${q.titleAr}! افتح الحقيبة لاستلام المكافأة")
        }
    }

    // ---------------------------------------------------------------- game flow
    private fun dist(ax: Float, ay: Float, bx: Float, by: Float): Float {
        val dx = ax - bx
        val dy = ay - by
        return sqrt(dx * dx + dy * dy)
    }

    private fun bossName(e: EraType): String = when (e) {
        EraType.ERA_1_MEDIEVAL -> "الفارس الأسود"
        EraType.ERA_2_CAVES -> "الحارس الحجري"
        EraType.ERA_3_ZOMBIE -> "الجرثومة الأم"
        EraType.ERA_4_ISLAND -> "قبطان القراصنة"
        EraType.ERA_5_CYBER -> "قائد النظام"
        EraType.ERA_6_VOID -> "سيد الفراغ"
    }

    private fun minionRole(e: EraType): EnemyRole = when (e) {
        EraType.ERA_1_MEDIEVAL -> EnemyRole.MELEE
        EraType.ERA_2_CAVES -> EnemyRole.GHOUL
        EraType.ERA_3_ZOMBIE -> EnemyRole.GHOUL
        EraType.ERA_4_ISLAND -> EnemyRole.TRIBAL
        EraType.ERA_5_CYBER -> EnemyRole.COMMANDO
        EraType.ERA_6_VOID -> EnemyRole.CLONE
    }

    private fun backToTitle() {
        ow = null
        inCave = false
        caveMap = null
        player = Player(300f, 880f)
        era = EraType.ERA_1_MEDIEVAL
        world = TileMap(era, 1077L)
        world.build(Pt(300f, 880f), emptyList())
        camera = Camera2D(world.width, world.height)
        camera.viewW = curW / zoom
        camera.viewH = curH / zoom
        enemies.clear()
        projectiles.clear()
        cancelAll()
        modal = Modal.NONE
        state = State.TITLE
        buildDark()
        Sound.playMusic(1)
    }

    private fun startNewGame() {
        SaveStore.clear()
        player = Player(300f, 880f)
        state = State.PLAYING
        startEra(EraType.ERA_1_MEDIEVAL)
    }

    private fun continueGame() {
        val p = Player(300f, 880f)
        val idx = p.load()
        player = p
        state = State.PLAYING
        if (SaveStore.str("mode", "") == "open") {
            startOpen(true)
            return
        }
        startEra(EraType.values()[idx.coerceIn(0, EraType.values().size - 1)])
    }

    private fun retryFromCheckpoint() {
        val idx = player.load()
        state = State.PLAYING
        startEra(EraType.values()[idx.coerceIn(0, EraType.values().size - 1)])
    }

    private fun addEnemy(role: EnemyRole, x: Float, y: Float, key: Boolean = false): Enemy {
        val e = Enemy(x, y, role, era, key)
        enemies.add(e)
        return e
    }

    private fun startEra(e: EraType) {
        era = e
        inCave = false
        caveMap = null
        world = TileMap(e, 1000L + e.ordinal * 77L)
        enemies.clear()
        projectiles.clear()
        explosions.clear()
        floats.clear()
        coins.clear()
        chests.clear()
        crates.clear()
        rocks.clear()
        trees.clear()
        traps.clear()
        props.clear()
        gens.clear()
        cocoons.clear()
        totems.clear()
        cannons.clear()
        particles.list.clear()
        mound = null
        boss = null
        bossDown = false
        greeted = false
        waveTimer = 8f
        ow = null
        shopBought.clear()
        cancelAll()
        val pts = ArrayList<Pt>()
        fun reg(x: Float, y: Float, ox: Float = 32f, oy: Float = 40f) {
            pts.add(Pt(x + ox, y + oy))
        }
        val sx = 300f
        val sy = 880f

        merchant = MerchantNPC(470f, 760f)
        reg(470f, 760f, 48f, 70f)
        world.setDynamic(518f, 840f, true)
        table = CraftTable(620f, 970f)
        reg(620f, 970f)
        world.setDynamic(652f, 1002f, true)
        props.add(WorldProp("environment/fire.png", 392f, 808f, 56f, 56f))
        props.add(WorldProp("environment/torch.png", 350f, 730f, 36f, 36f, 0.15f))
        props.add(WorldProp("environment/torch.png", 580f, 720f, 36f, 36f, 0.15f))

        chests.add(TreasureChest(250f, 440f, 25))
        chests.add(TreasureChest(1500f, 1560f, 35))
        chests.add(TreasureChest(1560f, 520f, 50))
        for (c in chests) {
            reg(c.x, c.y)
            world.setDynamic(c.x + 32f, c.y + 32f, true)
        }
        val crateSpots = listOf(Pt(860f, 470f), Pt(1290f, 340f), Pt(1300f, 1100f), Pt(560f, 1250f))
        for ((i, s) in crateSpots.withIndex()) {
            crates.add(WoodCrate(s.x, s.y, i % 2 == 1))
            reg(s.x, s.y)
            world.setDynamic(s.x + 32f, s.y + 32f, true)
        }
        val rockSpots = listOf(Triple(950f, 320f, false), Triple(1180f, 1560f, true), Triple(700f, 1560f, false), Triple(1480f, 300f, true))
        for (r in rockSpots) {
            rocks.add(MiningRock(r.first, r.second, r.third))
            reg(r.first, r.second)
            world.setDynamic(r.first + 32f, r.second + 32f, true)
        }
        val treeSpots = listOf(Pt(520f, 1380f), Pt(820f, 1480f), Pt(1220f, 1280f), Pt(1560f, 1400f), Pt(430f, 520f))
        for ((i, s) in treeSpots.withIndex()) {
            trees.add(ChoppableTree(s.x, s.y, i % 2 == 0))
            reg(s.x, s.y)
            world.setDynamic(s.x + 32f, s.y + 44f, true)
        }

        if (e.ordinal >= 1) {
            traps.add(DungeonTrap(1000f, 780f, true))
            traps.add(DungeonTrap(1280f, 1180f, true))
            traps.add(DungeonTrap(1130f, 1000f, false))
            traps.add(DungeonTrap(1380f, 600f, false))
            traps.add(DungeonTrap(1450f, 1340f, false))
            for (t in traps) reg(t.baseX, t.baseY, 32f, 32f)
        }

        when (e) {
            EraType.ERA_1_MEDIEVAL -> {
                addEnemy(EnemyRole.SPEARMAN, 1000f, 500f)
                addEnemy(EnemyRole.SPEARMAN, 1250f, 1300f)
                addEnemy(EnemyRole.ARCHER, 1400f, 720f)
                addEnemy(EnemyRole.ARCHER, 900f, 1450f)
                addEnemy(EnemyRole.MELEE, 1560f, 900f)
                addEnemy(EnemyRole.HEAVY, 1450f, 1080f, true)
            }
            EraType.ERA_2_CAVES -> {
                addEnemy(EnemyRole.GHOUL, 1000f, 500f)
                addEnemy(EnemyRole.GHOUL, 1100f, 1400f)
                addEnemy(EnemyRole.SPEARMAN, 1300f, 800f)
                addEnemy(EnemyRole.ARCHER, 1450f, 420f)
                addEnemy(EnemyRole.HEAVY, 1500f, 1120f, true)
            }
            EraType.ERA_3_ZOMBIE -> {
                addEnemy(EnemyRole.MELEE, 1000f, 520f)
                addEnemy(EnemyRole.MELEE, 1200f, 1350f)
                addEnemy(EnemyRole.GHOUL, 1350f, 760f)
                addEnemy(EnemyRole.GHOUL, 900f, 1450f)
                addEnemy(EnemyRole.GHOUL, 1560f, 1000f)
                addEnemy(EnemyRole.HEAVY, 1480f, 1180f, true)
            }
            EraType.ERA_4_ISLAND -> {
                addEnemy(EnemyRole.TRIBAL, 1000f, 560f)
                addEnemy(EnemyRole.TRIBAL, 1250f, 1250f)
                addEnemy(EnemyRole.TRIBAL, 1500f, 800f)
                addEnemy(EnemyRole.ARCHER, 1350f, 480f)
                addEnemy(EnemyRole.ARCHER, 900f, 1420f)
                addEnemy(EnemyRole.SPEARMAN, 1550f, 1150f)
                val m = BuriedMound(1500f, 1300f)
                mound = m
                reg(m.x, m.y)
            }
            EraType.ERA_5_CYBER -> {
                addEnemy(EnemyRole.COMMANDO, 1000f, 520f)
                addEnemy(EnemyRole.COMMANDO, 1250f, 1330f)
                addEnemy(EnemyRole.COMMANDO, 1450f, 760f)
                addEnemy(EnemyRole.COMMANDO, 900f, 1450f)
                addEnemy(EnemyRole.HEAVY, 1500f, 1100f, true)
                gens.add(PowerGenerator(700f, 520f, 1))
                gens.add(PowerGenerator(1300f, 1480f, 2))
                gens.add(PowerGenerator(1500f, 420f, 3))
                for (g in gens) {
                    reg(g.x, g.y, 42f, 42f)
                    world.setDynamic(g.x + 42f, g.y + 42f, true)
                }
            }
            EraType.ERA_6_VOID -> {
                addEnemy(EnemyRole.CLONE, 1000f, 620f)
                addEnemy(EnemyRole.CLONE, 1250f, 1300f)
                addEnemy(EnemyRole.COMMANDO, 1400f, 820f)
                addEnemy(EnemyRole.COMMANDO, 1000f, 1400f)
            }
        }
        for (en in enemies) reg(en.x, en.y, 40f, 60f)

        val b = addEnemy(EnemyRole.BOSS, 2060f, 880f)
        b.shielded = (e == EraType.ERA_5_CYBER)
        boss = b
        reg(b.x, b.y, b.size / 2f, b.size * 0.7f)
        val right1 = addEnemy(minionRole(e), 2000f, 520f)
        val right2 = addEnemy(minionRole(e), 2000f, 1330f)
        reg(right1.x, right1.y, 40f, 60f)
        reg(right2.x, right2.y, 40f, 60f)
        val rc = TreasureChest(2250f, 420f, 60)
        chests.add(rc)
        reg(rc.x, rc.y)
        world.setDynamic(rc.x + 32f, rc.y + 32f, true)
        if (e == EraType.ERA_4_ISLAND) {
            val gem = TreasureChest(2250f, 1420f, 0, true)
            chests.add(gem)
            reg(gem.x, gem.y)
            world.setDynamic(gem.x + 32f, gem.y + 32f, true)
        }
        portal = TimePortal(2230f, 1120f)
        reg(portal.x, portal.y, 70f, 70f)

        world.build(Pt(sx + 48f, sy + 60f), pts)

        player.x = sx
        player.y = sy
        player.resetState()
        camera = Camera2D(world.width, world.height)
        camera.viewW = curW / zoom
        camera.viewH = curH / zoom
        camera.snapTo(player.cx, player.cy)
        buildDark()
        fade = 1f
        bannerTimer = 4.2f
        hitStop = 0f
        deathTimer = 0f
        Sound.playMusic(e.track)
        player.save(e.ordinal)
        openDialogue(Story.intro(e)) { }
    }

    private fun completeEra() {
        Sound.play("portal")
        player.save(min(era.ordinal + 1, EraType.values().size - 1))
        val n = era.next()
        if (n == null) {
            SaveStore.putAll(mapOf("ow_unlocked" to true), false)
            state = State.VICTORY
            endT = 0f
            cancelAll()
            Sound.play("victory")
        } else {
            startEra(n)
        }
    }

    // ---------------------------------------------------------------- dialogue
    private fun openDialogue(lines: List<DialogueLine>, done: () -> Unit) {
        if (lines.isEmpty()) {
            done()
            return
        }
        dialogue = lines
        dialogueIdx = 0
        dialogueTime = 0f
        dialogueDone = done
        modal = Modal.DIALOGUE
        cancelAll()
    }

    private fun dialogueChars(): Int = (dialogueTime * 42f).toInt()

    private fun advanceDialogue() {
        if (dialogueIdx >= dialogue.size) {
            modal = Modal.NONE
            return
        }
        val full = dialogue[dialogueIdx].text.length
        if (dialogueChars() < full) {
            dialogueTime = full / 42f + 0.05f
            return
        }
        Sound.play("click", 0.5f)
        dialogueIdx++
        dialogueTime = 0f
        if (dialogueIdx >= dialogue.size) {
            modal = Modal.NONE
            val d = dialogueDone
            dialogueDone = null
            d?.invoke()
        }
    }

    // ---------------------------------------------------------------- update
    private fun update(dt0: Float) {
        time += dt0
        if (fade > 0f) fade = max(0f, fade - dt0 * 1.1f)
        if (toastTimer > 0f) toastTimer -= dt0
        if (hurtFlash > 0f) hurtFlash -= dt0
        if (shake > 0f) shake = max(0f, shake - dt0 * 30f)
        when (state) {
            State.TITLE -> {
                titleT += dt0
                return
            }
            State.GAME_OVER, State.VICTORY -> {
                endT += dt0
                return
            }
            State.PLAYING -> {
            }
        }
        if (bannerTimer > 0f) bannerTimer -= dt0
        if (modal == Modal.DIALOGUE) dialogueTime += dt0
        if (modal != Modal.NONE) return
        if (hitStop > 0f) {
            hitStop -= dt0
            return
        }
        if (ow != null) {
            updateOpen(dt0)
            return
        }
        val dt = dt0
        val edt = if (player.slowTimer > 0f) dt * 0.3f else dt
        val hpBefore = player.hp

        if (attackBtn.isPressed && !player.isDead) {
            if (player.attack()) Sound.play("swing", 0.7f)
        }
        player.update(dt, joystick.dx, joystick.dy, world)
        camera.update(player.cx, player.cy, dt)
        if (player.isDead) {
            deathTimer += dt
            if (deathTimer > 1.8f) {
                state = State.GAME_OVER
                endT = 0f
                cancelAll()
                Sound.play("gameover")
                return
            }
        }
        resolveMelee()

        var i = 0
        while (i < enemies.size) {
            val en = enemies[i]
            en.update(edt, player, world, projectiles)
            if (en.wantSummon) {
                en.wantSummon = false
                summonMinions(en)
            }
            i++
        }
        separateEnemies()
        val eit = enemies.iterator()
        while (eit.hasNext()) {
            val en = eit.next()
            if (en.isDead && en.deadTimer > 2.5f) eit.remove()
        }

        var pi = projectiles.size - 1
        while (pi >= 0) {
            val pr = projectiles[pi]
            pr.update(edt)
            if (!pr.dead) updateProjectile(pr)
            if (pr.dead) {
                if (pr.kind == Projectile.BOMB || pr.kind == Projectile.CANNONBALL) explode(pr.x, pr.y)
                projectiles.removeAt(pi)
            }
            pi--
        }
        var xi = explosions.size - 1
        while (xi >= 0) {
            explosions[xi].update(dt)
            if (explosions[xi].done) explosions.removeAt(xi)
            xi--
        }
        particles.update(dt)
        var fi = floats.size - 1
        while (fi >= 0) {
            floats[fi].update(dt)
            if (floats[fi].dead) floats.removeAt(fi)
            fi--
        }
        var ci = coins.size - 1
        while (ci >= 0) {
            val c = coins[ci]
            c.update(dt, player)
            if (c.collected) {
                player.coins += c.value
                addFloat("+${c.value}", player.cx, player.y - 10f, Color.parseColor("#FFD740"), 24f)
                Sound.play("coin", 0.7f)
                particles.burst(c.x, c.y, 6, Color.parseColor("#FFD740"), 110f, 5f, 0.4f)
                coins.removeAt(ci)
            }
            ci--
        }

        merchant.update(dt)
        for (p in props) p.update(dt)
        for (t in traps) t.update(dt, player)
        for (r in rocks) r.update(dt)
        for (c in crates) c.update(dt)
        for (t in trees) t.update(dt)
        for (g in gens) g.update(dt)
        mound?.update(dt)
        portal.update(dt)
        portal.isActive = bossDown && (era != EraType.ERA_4_ISLAND || player.hasOceanGem)

        if (era == EraType.ERA_4_ISLAND && !player.hasOceanGem && !bossDown) {
            waveTimer -= dt
            if (waveTimer <= 0f) {
                waveTimer = 15f
                spawnWave()
            }
        }
        if (era == EraType.ERA_6_VOID && !world.gateOpen) {
            val gateX = TileMap.GATE_COL * TileMap.TS
            var left = 0
            for (en in enemies) if (!en.isDead && en !== boss && en.x < gateX) left++
            if (left == 0) openGate()
        }
        if (player.hp < hpBefore) onPlayerHurt(hpBefore - player.hp)
        updateTarget()
        updateButtons()
    }

    private fun onPlayerHurt(d: Int) {
        Sound.play("hurt")
        shake = max(shake, 9f)
        hurtFlash = 0.4f
        addFloat("-$d", player.cx, player.y - 6f, Color.parseColor("#FF5252"), 28f)
        particles.burst(player.cx, player.cy, 10, Color.RED, 170f, 6f, 0.5f)
    }

    private fun separateEnemies() {
        for (i in 0 until enemies.size) {
            val a = enemies[i]
            if (a.isDead) continue
            for (j in i + 1 until enemies.size) {
                val b = enemies[j]
                if (b.isDead) continue
                val dx = b.cx - a.cx
                val dy = b.cy - a.cy
                val d2 = dx * dx + dy * dy
                val minD = 46f
                if (d2 < minD * minD && d2 > 0.01f) {
                    val d = sqrt(d2)
                    val push = (minD - d) * 0.5f
                    collider.move(a, -dx / d * push, -dy / d * push)
                    collider.move(b, dx / d * push, dy / d * push)
                }
            }
        }
    }

    private fun updateProjectile(pr: Projectile) {
        val w = ow
        val hitWall = if (w != null && !inCave) w.isSolidForProjectile(pr.x, pr.y, pr.friendly) else collider.isSolidAt(pr.x, pr.y)
        if (hitWall) {
            if (w != null && !inCave) {
                val tx = floor(pr.x / OpenWorld.TS).toInt()
                val ty = floor(pr.y / OpenWorld.TS).toInt()
                if (w.blockCodeAtTile(tx, ty) == BlockType.TNT_BLOCK.code) {
                    w.removeBlockAt(tx, ty)
                    explode(tx * OpenWorld.TS + 32f, ty * OpenWorld.TS + 32f)
                } else if (!pr.friendly && w.blockCodeAtTile(tx, ty) != 0) {
                    w.damagePlayerBlockAt(tx, ty, 1)
                }
            }
            pr.dead = true
            particles.burst(pr.x, pr.y, 5, Color.LTGRAY, 90f, 4f, 0.3f)
            return
        }
        if (pr.friendly) {
            for (en in enemies) {
                if (en.isDead || pr.hitSet.contains(en)) continue
                if (en.hitbox.contains(pr.x, pr.y)) {
                    if (pr.kind == Projectile.BOMB || pr.kind == Projectile.CANNONBALL) {
                        pr.dead = true
                        return
                    }
                    if (pr.parried && en.isColossalBoss && en.shielded) {
                        en.shielded = false
                        Sound.play("explode")
                        shake = 14f
                        toast("حطمت درع الزعيم العملاق بعكس قذيفته بالصد المثالي!")
                    }
                    damageEnemy(en, pr.damage, pr.x - pr.vx * 0.05f, pr.y - pr.vy * 0.05f, 0.8f, pr.parried)
                    if (pr.pierce) pr.hitSet.add(en) else {
                        pr.dead = true
                        return
                    }
                }
            }
            for (g in gens) {
                if (!g.isDestroyed && !pr.hitSet.contains(g) && g.hitbox.contains(pr.x, pr.y)) {
                    pr.hitSet.add(g)
                    hitGen(g)
                    if (!pr.pierce) {
                        pr.dead = true
                        return
                    }
                }
            }
            for (cc in cocoons) {
                if (!cc.isDestroyed && !pr.hitSet.contains(cc) && cc.hitbox.contains(pr.x, pr.y)) {
                    pr.hitSet.add(cc)
                    hitCocoon(cc)
                    if (!pr.pierce) {
                        pr.dead = true
                        return
                    }
                }
            }
        } else {
            if (player.hitbox.contains(pr.x, pr.y)) {
                if (player.parryTimer > 0f) {
                    pr.reflect()
                    player.parryTimer = 0f
                    player.parryCd = 0.4f
                    player.dashCd = 0f
                    player.mana = min(player.maxMana.toFloat(), player.mana + 25f)
                    player.addCombo(3)
                    advanceQuest("q_parry", 1)
                    Sound.play("era_clear", 0.8f)
                    particles.burst(pr.x, pr.y, 14, Color.parseColor("#00E5FF"), 220f, 6f, 0.45f)
                    addFloat("صد مثالي وعكس! ⚡x3", player.cx, player.y - 20f, Color.parseColor("#00E5FF"), 26f)
                } else {
                    player.takeDamage(pr.damage)
                    pr.dead = true
                }
            }
        }
    }

    private fun fireRangedOrHeavyWeaponEffect() {
        val wid = player.weapon.id
        val base = (player.weapon.damage * (1f + 0.14f * player.weaponLvl) * player.comboDmgMul()).toInt()
        if (wid == "bow_fire") {
            val pr = Projectile(
                player.cx + player.aimX * 34f, player.cy - 12f + player.aimY * 34f,
                player.aimX * 660f, player.aimY * 660f,
                base, Projectile.ARROW, true
            )
            projectiles.add(pr)
            Sound.play("magic", 0.7f)
            particles.burst(player.cx + player.aimX * 32f, player.cy - 10f + player.aimY * 32f, 6, Color.parseColor("#FF9800"), 130f, 4f, 0.25f)
        } else if (wid == "musket_powder") {
            if (player.gunpowder > 0) {
                player.gunpowder--
                val pr = Projectile(
                    player.cx + player.aimX * 38f, player.cy - 12f + player.aimY * 38f,
                    player.aimX * 880f, player.aimY * 880f,
                    (base * 1.35f).toInt(), Projectile.LASER, true
                )
                projectiles.add(pr)
                Sound.play("explode", 0.65f)
                shake = max(shake, 6f)
                particles.burst(player.cx + player.aimX * 40f, player.cy - 10f + player.aimY * 40f, 12, Color.parseColor("#FFD54F"), 210f, 5f, 0.3f)
            } else {
                addFloat("نفد البارود للبندقية!", player.cx, player.y - 20f, Color.parseColor("#EF9A9A"), 18f)
            }
        } else if (wid == "hammer_thunder") {
            Sound.play("explode", 0.55f)
            shake = max(shake, 8f)
            particles.burst(player.cx, player.cy, 18, Color.parseColor("#00E5FF"), 240f, 6f, 0.45f)
            for (en in enemies) {
                if (!en.isDead && dist(en.cx, en.cy, player.cx, player.cy) < 165f) {
                    en.stunTimer = max(en.stunTimer, 1.1f)
                    damageEnemy(en, (base * 0.85f).toInt(), player.cx, player.cy, 1.5f, true)
                }
            }
        }
    }

    private fun resolveMelee() {
        if (!player.inHitWindow) return
        if (!player.swingHit.contains("ranged_fired")) {
            player.swingHit.add("ranged_fired")
            fireRangedOrHeavyWeaponEffect()
        }
        player.attackRect(atkRect)
        val base = (player.weapon.damage * (1f + 0.14f * player.weaponLvl) * player.comboDmgMul()).toInt()
        for (en in enemies) {
            if (en.isDead || player.swingHit.contains(en)) continue
            if (RectF.intersects(atkRect, en.hitbox)) {
                player.swingHit.add(en)
                val crit = rnd.nextFloat() < 0.14f
                damageEnemy(en, if (crit) (base * 1.6f).toInt() else base, player.cx, player.cy, 1f, crit)
            }
        }
        for (r in rocks) if (!r.isDestroyed && !player.swingHit.contains(r) && RectF.intersects(atkRect, r.hitbox)) {
            player.swingHit.add(r)
            hitRock(r)
        }
        for (c in crates) if (!c.isDestroyed && !player.swingHit.contains(c) && RectF.intersects(atkRect, c.hitbox)) {
            player.swingHit.add(c)
            hitCrate(c)
        }
        for (t in trees) if (!t.isDestroyed && !player.swingHit.contains(t) && RectF.intersects(atkRect, t.hitbox)) {
            player.swingHit.add(t)
            hitTree(t)
        }
        for (g in gens) if (!g.isDestroyed && !player.swingHit.contains(g) && RectF.intersects(atkRect, g.hitbox)) {
            player.swingHit.add(g)
            hitGen(g)
        }
        for (cc in cocoons) if (!cc.isDestroyed && !player.swingHit.contains(cc) && RectF.intersects(atkRect, cc.hitbox)) {
            player.swingHit.add(cc)
            hitCocoon(cc)
        }
        for (cn in cannons) if (!cn.friendly && !cn.isDestroyed && !player.swingHit.contains(cn) && RectF.intersects(atkRect, cn.hitbox)) {
            player.swingHit.add(cn)
            hitCannon(cn)
        }
        for (sh in sheeps) if (!sh.isDead && !player.swingHit.contains(sh) && RectF.intersects(atkRect, sh.hitbox)) {
            player.swingHit.add(sh)
            hitSheep(sh)
        }
        val w = ow
        if (w != null && !inCave) meleeTiles(w)
    }

    private fun hitSheep(sh: Sheep) {
        if (!sh.hit(player.cx, player.cy)) return
        Sound.play("hit", 0.7f)
        particles.burst(sh.cx, sh.cy, 8, Color.WHITE, 140f, 5f, 0.45f)
        if (sh.woolReady) {
            sh.woolReady = false
            player.wool += 1
            addFloat("+1 صوف", sh.cx, sh.y - 10f, Color.WHITE, 22f)
            advanceQuest("q_wool", 1)
        }
        if (sh.isDead) {
            player.wool += 1
            player.rawMeat += 2
            addFloat("+1 صوف  +2 لحم", sh.cx, sh.y - 28f, Color.parseColor("#FFAB91"), 22f)
            advanceQuest("q_wool", 1)
        }
    }

    private fun hitCocoon(cc: CaveCocoon) {
        if (!cc.hit()) return
        Sound.play("chop", 0.9f)
        particles.burst(cc.x + 40f, cc.y + 40f, 10, Color.parseColor("#CE93D8"), 160f, 6f, 0.45f)
        if (cc.isDestroyed) {
            val b = cc.biome.coerceIn(0, 5)
            player.mats[b] += 2
            player.gunpowder += 4
            coins.add(CoinDrop(cc.x + 40f, cc.y + 40f, 25))
            addFloat("+2 ${Mats.names[b]}  +4 بارود!", cc.x + 40f, cc.y - 10f, Color.parseColor("#EA80FC"), 23f)
            advanceQuest("q_cave", 1)
        }
    }

    private fun hitCannon(cn: CannonTurret) {
        if (!cn.hit()) return
        Sound.play("mine", 0.9f)
        particles.burst(cn.x + 38f, cn.y + 38f, 8, Color.parseColor("#FF8A65"), 170f, 5f, 0.4f)
        if (cn.isDestroyed) {
            Sound.play("explode", 0.8f)
            player.iron += 2
            coins.add(CoinDrop(cn.x + 38f, cn.y + 38f, 35))
            addFloat("+2 حديد  +35 ذهب", cn.x + 38f, cn.y - 12f, GOLD, 23f)
            advanceQuest("q_pirate", 1)
        }
    }

    private fun damageEnemy(en: Enemy, dmg: Int, fx: Float, fy: Float, knock: Float, crit: Boolean = false) {
        val finalDmg = if (en.isColossalBoss && player.abyssalSeals < 7) {
            val missing = 7 - player.abyssalSeals
            max(1, (dmg * (1f - missing * 0.11f)).toInt())
        } else dmg
        player.addCombo(1)
        val dealt = en.takeHit(finalDmg, fx, fy, knock)
        if (dealt == 0) {
            addFloat("محصّن", en.cx, en.y, Color.CYAN, 22f)
            Sound.play("deny", 0.5f)
            return
        }
        val col = if (crit) Color.parseColor("#FFD740") else Color.WHITE
        addFloat(if (crit) "$dealt!" else "$dealt", en.cx, en.y, col, if (crit) 34f else 26f)
        particles.burst(en.cx, en.cy, if (crit) 14 else 8, col, 220f, 6f, 0.4f)
        // Elemental Weapon Enchantments (Lifesteal & Chain Lightning)
        if (player.weaponEnchant == 1 && player.hp < player.maxHp) {
            val heal = max(2, (dealt * 0.16f).toInt())
            player.hp = min(player.maxHp, player.hp + heal)
            addFloat("+$heal 🩸", player.cx, player.y - 18f, Color.parseColor("#FF5252"), 18f)
        } else if (player.weaponEnchant == 2 && !crit) {
            var chained = 0
            for (other in enemies) {
                if (other !== en && !other.isDead && dist(other.cx, other.cy, en.cx, en.cy) < 175f) {
                    val cDmg = max(12, dealt / 2)
                    other.takeHit(cDmg, en.cx, en.cy, 0.4f)
                    particles.burst(other.cx, other.cy, 6, Color.parseColor("#00E5FF"), 160f, 4f, 0.3f)
                    addFloat("⚡$cDmg", other.cx, other.y - 12f, Color.parseColor("#00E5FF"), 20f)
                    if (other.isDead) onEnemyDeath(other)
                    chained++
                    if (chained >= 2) break
                }
            }
        }
        Sound.play("hit", 0.8f)
        hitStop = if (crit) 0.07f else 0.035f
        shake = max(shake, if (crit) 6f else 3f)
        if (en.isDead) onEnemyDeath(en)
    }

    private fun onEnemyDeath(en: Enemy) {
        val n = if (en.isBoss) 6 else 1
        for (k in 0 until n) coins.add(CoinDrop(en.cx, en.cy, max(1, en.reward / n)))
        particles.burst(en.cx, en.cy, 18, Color.parseColor("#B0BEC5"), 200f, 7f, 0.7f)
        if (ow != null) {
            advanceQuest("q_zombie", 1)
            if (en.era == EraType.ERA_4_ISLAND || en.role == EnemyRole.TRIBAL) advanceQuest("q_pirate", 1)
        }
        if (en === stalker) {
            stalker = null
            player.mats[5] += 2
            addFloat("+2 ${Mats.names[5]}", en.cx, en.y - 40f, Color.parseColor("#D070FF"), 24f)
        }
        if (ow != null && !en.isBoss && rnd.nextFloat() < 0.45f) {
            player.rawMeat++
            addFloat("+1 لحم نيء", en.cx, en.y - 4f, Color.parseColor("#EF9A9A"), 20f)
        }
        if (ow != null && rnd.nextFloat() < 0.12f) {
            player.treasureMaps++
            addFloat("+1 خريطة كنز!", en.cx, en.y - 36f, Color.parseColor("#80DEEA"), 22f)
        }
        if (en.role == EnemyRole.HEAVY || en.isBoss) {
            val mi = en.era.ordinal
            val mn = if (en.isBoss) 3 + rnd.nextInt(2) else 1
            player.mats[mi] += mn
            addFloat("+$mn ${Mats.names[mi]}", en.cx, en.y - 22f, Color.parseColor("#FFD740"), 22f)
            if (en.isBoss && ow != null) {
                player.goldKeys++
                addFloat("+1 مفتاح ذهبي!", en.cx, en.y - 48f, GOLD, 25f)
            }
        }
        if (en.holdsKey) {
            player.hasBossKey = true
            Sound.play("gate")
            toast("حصلت على مفتاح البوابة! اذهب إليها")
        }
        if (en === boss && ow == null) {
            bossDown = true
            shake = 14f
            Sound.play("era_clear")
            openDialogue(Story.clear(era)) { toast("بوابة الزمن أصبحت مفتوحة") }
        } else if (en === boss && inCave) {
            bossDown = true
            shake = 12f
            Sound.play("era_clear")
            if (player.unlockSeal(0)) {
                advanceQuest("q_seals", 1)
                toast("هزمت زعيم الكهف وحصلت على ختم الهاوية الأول (ختم الأعماق)!")
            } else {
                toast("هزمت زعيم الكهف! افتح الصناديق الذهبية أو اخرج للسطح")
            }
            advanceQuest("q_cave", 2)
        }
        if (ow != null && !en.isBoss && isNight()) {
            player.zombiesKilled++
            if (player.zombiesKilled >= 15 && player.unlockSeal(1)) {
                advanceQuest("q_seals", 1)
                Sound.play("era_clear")
                toast("صمدت في حرب الزومبي الليلية وحصلت على ختم القمر الدموي! (${player.abyssalSeals}/7)")
            }
        }
        if (en.isColossalBoss) {
            colossalBoss = null
            player.colossalBossDefeated = true
            player.coins += 5000
            shake = 22f
            Sound.play("victory")
            saveProgress()
            openDialogue(
                listOf(
                    DialogueLine("إمبراطور الهاوية", "sprites/zombie_titan/idle.png", "مستحيل... لقد جمعت أختام العصور وحطمت درع الهاوية الخالد!"),
                    DialogueLine("الفارس", "sprites/player/idle.png", "انتهت حرب الزومبي وتحرر العالم المفتوح للأبد! لقد ختمت الرحلة الكبرى!")
                )
            ) {
                toast("مبارك ختم اللعبة! هزمت الزعيم الأسطوري العملاق وحصلت على +5000 ذهب!")
            }
        }
    }

    private fun summonMinions(b: Enemy) {
        var alive = 0
        for (en in enemies) if (!en.isDead && en !== b) alive++
        if (alive >= 7) return
        for (k in 0 until 2) {
            val sx = b.x + (if (k == 0) -110f else 110f)
            val sy = b.y + 40f
            val m = Enemy(sx, sy, minionRole(era), era)
            if (collider.blocked(m.x + m.fOffX, m.y + m.fOffY, m.x + m.fOffX + m.fW, m.y + m.fOffY + m.fH)) continue
            m.aggro = true
            enemies.add(m)
            particles.burst(m.cx, m.cy, 12, Color.parseColor("#E040FB"), 150f, 6f, 0.6f)
        }
        Sound.play("boss", 0.8f)
        addFloat("استدعاء!", b.cx, b.y - 10f, Color.parseColor("#E040FB"), 28f)
    }

    private fun spawnWave() {
        var alive = 0
        for (en in enemies) if (!en.isDead && en !== boss) alive++
        if (alive >= 8) return
        val gateX = TileMap.GATE_COL * TileMap.TS
        var made = 0
        var tries = 0
        while (made < 2 && tries < 30) {
            tries++
            val a = rnd.nextFloat() * 6.2832f
            val r = 480f + rnd.nextFloat() * 160f
            val m = Enemy(
                player.cx + cos(a) * r - 42f,
                player.cy + sin(a) * r - 60f,
                EnemyRole.TRIBAL, era
            )
            if ((m.x < gateX) != (player.x < gateX)) continue
            if (collider.blocked(m.x + m.fOffX, m.y + m.fOffY, m.x + m.fOffX + m.fW, m.y + m.fOffY + m.fH)) continue
            m.aggro = true
            enemies.add(m)
            made++
        }
        if (made > 0) toast("موجة جديدة من القبائل!")
    }

    private fun explode(x: Float, y: Float) {
        explosions.add(ExplosionFx(x, y, 160f))
        Sound.play("explode")
        shake = max(shake, 13f)
        particles.burst(x, y, 26, Color.parseColor("#FF9800"), 340f, 8f, 0.65f)
        for (en in enemies) {
            if (en.isDead) continue
            if (dist(en.cx, en.cy, x, y) < 170f + en.size * 0.35f) {
                if (en.isColossalBoss && en.shielded) {
                    en.shielded = false
                    toast("انفجار TNT حطم درع الزعيم العملاق! هاجمه الآن!")
                }
                damageEnemy(en, 185, x, y, 1.8f, true)
            }
        }
        for (r in rocks) if (!r.isDestroyed && dist(r.x + 32f, r.y + 32f, x, y) < 160f) {
            hitRock(r)
            if (!r.isDestroyed) hitRock(r)
        }
        for (c in crates) if (!c.isDestroyed && dist(c.x + 32f, c.y + 32f, x, y) < 160f) {
            hitCrate(c)
            if (!c.isDestroyed) hitCrate(c)
        }
        for (t in trees) if (!t.isDestroyed && dist(t.x + 32f, t.y + 40f, x, y) < 160f) hitTree(t)
        for (g in gens) if (!g.isDestroyed && dist(g.x + 42f, g.y + 42f, x, y) < 165f) hitGen(g)
        for (cc in cocoons) if (!cc.isDestroyed && dist(cc.x + 40f, cc.y + 40f, x, y) < 165f) hitCocoon(cc)
        for (cn in cannons) if (!cn.friendly && !cn.isDestroyed && dist(cn.x + 38f, cn.y + 38f, x, y) < 165f) hitCannon(cn)
        val w = ow
        if (w != null && !inCave) openExplode(w, x, y)
    }

    private fun hitRock(r: MiningRock) {
        if (!r.hit()) return
        Sound.play("mine", 0.8f)
        particles.burst(r.x + 32f, r.y + 32f, 6, Color.parseColor("#B0BEC5"), 150f, 5f, 0.4f)
        val amount = if (r.isDestroyed) 2 else 1
        if (r.isIron) player.iron += amount else player.stone += amount
        addFloat("+$amount ${if (r.isIron) "حديد" else "حجر"}", r.x + 32f, r.y - 6f, if (r.isIron) Color.parseColor("#80D8FF") else Color.parseColor("#CFD8DC"), 22f)
        if (r.isDestroyed) {
            val tm = if (inCave) caveMap else world
            tm?.setDynamic(r.x + 32f, r.y + 32f, false)
            if (inCave) {
                player.gunpowder += 2
                addFloat("+2 بارود خام 💥", r.x + 32f, r.y - 46f, Color.parseColor("#FFAB40"), 20f)
            }
            if (r.isIron) {
                val bi = if (inCave) (activeCave?.biome ?: 1) else era.ordinal
                player.mats[bi.coerceIn(0, 5)] += 1
                addFloat("+1 ${Mats.names[bi.coerceIn(0, 5)]}", r.x + 32f, r.y - 28f, Color.parseColor("#FFD740"), 22f)
            }
        }
    }

    private fun hitCrate(c: WoodCrate) {
        if (!c.hit()) return
        Sound.play("chop", 0.8f)
        particles.burst(c.x + 32f, c.y + 32f, 6, Color.parseColor("#A1887F"), 150f, 5f, 0.4f)
        if (c.isDestroyed) {
            val tm = if (inCave) caveMap else world
            tm?.setDynamic(c.x + 32f, c.y + 32f, false)
            player.wood += 1
            addFloat("+1 خشب", c.x + 32f, c.y - 6f, Color.parseColor("#FFCC80"), 22f)
            if (rnd.nextFloat() < 0.5f) coins.add(CoinDrop(c.x + 32f, c.y + 32f, 4 + rnd.nextInt(6)))
        }
    }

    private fun hitTree(t: ChoppableTree) {
        if (!t.hit()) return
        Sound.play("chop", 0.8f)
        particles.burst(t.x + 32f, t.y + 30f, 6, Color.parseColor("#66BB6A"), 140f, 5f, 0.4f)
        val amount = if (t.isDestroyed) 2 else 1
        player.wood += amount
        addFloat("+$amount خشب", t.x + 32f, t.y - 30f, Color.parseColor("#FFCC80"), 22f)
        if (t.isDestroyed) world.setDynamic(t.x + 32f, t.y + 44f, false)
    }

    private fun hitGen(g: PowerGenerator) {
        if (!g.hit()) return
        Sound.play("mine", 0.9f)
        particles.burst(g.x + 42f, g.y + 42f, 10, Color.parseColor("#00E5FF"), 200f, 6f, 0.5f)
        if (g.isDestroyed) {
            world.setDynamic(g.x + 42f, g.y + 42f, false)
            Sound.play("explode", 0.8f)
            var left = 0
            for (o in gens) if (!o.isDestroyed) left++
            if (left == 0) {
                boss?.shielded = false
                toast("انهار درع القائد! هاجمه الآن")
            } else toast("تبقّى $left مولد")
        }
    }

    // ---------------------------------------------------------------- interaction
    private fun openGate() {
        if (world.gateOpen) return
        world.gateOpen = true
        Sound.play("gate")
        shake = 10f
        toast("انفتحت البوابة!")
    }

    private fun updateTarget() {
        var best = Target.NONE
        var bestD = 1e9f
        targetChest = null
        fun cand(t: Target, d: Float, lim: Float) {
            if (d < lim && d < bestD) {
                best = t
                bestD = d
            }
        }
        val px = player.cx
        val py = player.cy
        cand(Target.SHOP, dist(px, py, merchant.x + 48f, merchant.y + 60f), 130f)
        cand(Target.CRAFT, dist(px, py, table.x + 32f, table.y + 32f), 112f)
        if (bossAltar.isNear(px, py)) cand(Target.BOSS_ALTAR, dist(px, py, bossAltar.x + 80f, bossAltar.y + 80f), 150f)
        if (!world.gateOpen && era != EraType.ERA_6_VOID) cand(Target.GATE, dist(px, py, world.gateCenterX, world.gateCenterY), 150f)
        for (c in chests) {
            if (c.isNear(px, py)) {
                val d = dist(px, py, c.x + 32f, c.y + 32f)
                if (d < bestD) {
                    cand(Target.CHEST, d, 100f)
                    if (best == Target.CHEST) targetChest = c
                }
            }
        }
        val m = mound
        if (m != null && m.isNear(px, py)) cand(Target.MOUND, dist(px, py, m.x + 32f, m.y + 32f), 95f)
        if (portal.isNear(px, py)) cand(Target.PORTAL, dist(px, py, portal.x + 70f, portal.y + 70f), 110f)
        target = best
        interactBtn.visible = best != Target.NONE && !player.isDead
        styleInteractButton(best)
    }

    private fun styleInteractButton(best: Target) {
        when (best) {
            Target.SHOP -> { interactBtn.iconPath = "ui/ico/cart.png"; interactBtn.label = "المتجر"; interactBtn.tintIcon = Color.WHITE }
            Target.CRAFT -> { interactBtn.iconPath = "ui/ico/axe.png"; interactBtn.label = "صناعة"; interactBtn.tintIcon = Color.WHITE }
            Target.GATE -> { interactBtn.iconPath = "icons/key.png"; interactBtn.label = "البوابة"; interactBtn.tintIcon = null }
            Target.CHEST -> { interactBtn.iconPath = "icons/chest_gold.png"; interactBtn.label = "افتح"; interactBtn.tintIcon = null }
            Target.MOUND -> { interactBtn.iconPath = "ui/ico/axe.png"; interactBtn.label = "احفر"; interactBtn.tintIcon = Color.WHITE }
            Target.PORTAL -> { interactBtn.iconPath = "ui/ico/enter.png"; interactBtn.label = "بوابة"; interactBtn.tintIcon = Color.WHITE }
            Target.COOK -> { interactBtn.iconPath = "icons/cooked_meat.png"; interactBtn.label = "طبخ"; interactBtn.tintIcon = null }
            Target.FURNACE -> { interactBtn.iconPath = "icons/furnace.png"; interactBtn.label = "الفرن"; interactBtn.tintIcon = null }
            Target.SLEEP -> { interactBtn.iconPath = "icons/bed.png"; interactBtn.label = "نوم"; interactBtn.tintIcon = null }
            Target.CAMPFIRE -> { interactBtn.iconPath = "environment/bonefire.png"; interactBtn.label = "مخيم/سفر"; interactBtn.tintIcon = null }
            Target.CAVE_ENTER -> { interactBtn.iconPath = "ui/ico/enter.png"; interactBtn.label = "دخول كهف"; interactBtn.tintIcon = Color.WHITE }
            Target.CAVE_EXIT -> { interactBtn.iconPath = "ui/ico/exit.png"; interactBtn.label = "خروج للسطح"; interactBtn.tintIcon = Color.WHITE }
            Target.CAVE_NEXT -> { interactBtn.iconPath = "ui/ico/enter.png"; interactBtn.label = "طابق أعمق"; interactBtn.tintIcon = Color.WHITE }
            Target.TOTEM -> { interactBtn.iconPath = "ui/ico/skull.png"; interactBtn.label = "تحدي الكهف"; interactBtn.tintIcon = Color.WHITE }
            Target.VILLAGER -> { interactBtn.iconPath = "icons/goblet.png"; interactBtn.label = "تجارة/سرقة"; interactBtn.tintIcon = null }
            Target.BOAT_DOCK -> { interactBtn.iconPath = "icons/boat.png"; interactBtn.label = "إبحار"; interactBtn.tintIcon = null }
            Target.BOSS_ALTAR -> { interactBtn.iconPath = "icons/abyss_seal.png"; interactBtn.label = "مذبح الزعيم"; interactBtn.tintIcon = null }
            Target.STORAGE_CHEST -> { interactBtn.iconPath = "icons/storage_chest.png"; interactBtn.label = "الشونة/تخزين"; interactBtn.tintIcon = null }
            Target.CANNON_REFILL -> { interactBtn.iconPath = "icons/gunpowder.png"; interactBtn.label = "تعبئة بارود"; interactBtn.tintIcon = null }
            Target.FARM_HARVEST -> { interactBtn.iconPath = "icons/wheat.png"; interactBtn.label = "حصاد الزرع"; interactBtn.tintIcon = null }
            Target.STRUCTURE_NODE -> { interactBtn.iconPath = targetStructure?.iconAsset ?: "icons/structure_tower.png"; interactBtn.label = "استكشاف 🏛️"; interactBtn.tintIcon = null }
            Target.REVIVE_PARTNER -> { interactBtn.iconPath = "icons/pot33.png"; interactBtn.label = "إنعاش الصديق ❤️‍🩹"; interactBtn.tintIcon = null }
            Target.NONE -> {}
        }
    }

    private fun doInteract() {
        when (target) {
            Target.SHOP -> {
                Sound.play("click")
                if (!greeted) {
                    greeted = true
                    openDialogue(Story.merchantGreeting()) { openShop() }
                } else openShop()
            }
            Target.CRAFT -> openCraft()
            Target.GATE -> {
                if (player.hasBossKey) openGate() else {
                    toast(Story.keyHint(era))
                    Sound.play("deny")
                }
            }
            Target.CHEST -> {
                val c = targetChest
                if (c != null) openChest(c)
            }
            Target.MOUND -> {
                val m = if (ow != null) targetMound else mound
                if (m != null) {
                    if (!player.hasPickaxe) {
                        toast("تحتاج فأس التنقيب! اصنعه من طاولة الصناعة")
                        Sound.play("deny")
                    } else {
                        m.isDug = true
                        Sound.play("mine")
                        particles.burst(m.x + 32f, m.y + 32f, 18, Color.parseColor("#FFD54F"), 210f, 6f, 0.6f)
                        if (ow != null) {
                            val reward = 65 + rnd.nextInt(55)
                            player.coins += reward
                            player.goldKeys++
                            addFloat("+$reward ذهب  +1 مفتاح ذهبي!", m.x + 32f, m.y - 10f, GOLD, 24f)
                            if (player.unlockSeal(5)) {
                                advanceQuest("q_seals", 1)
                                toast("استخرجت الكنز المدفون وحصلت على ختم الكنز الأثري! (${player.abyssalSeals}/7)")
                            } else {
                                toast("استخرجت كنزاً مدفوناً ومفتاحاً ذهبياً!")
                            }
                            advanceQuest("q_treasure", 1)
                        } else {
                            player.hasBossKey = true
                            toast("وجدت مفتاح البوابة تحت الرمال!")
                        }
                    }
                }
            }
            Target.PORTAL -> {
                if (ow != null) openTravelModal() else completeEra()
            }
            Target.COOK -> cookAll(false)
            Target.FURNACE -> {
                Sound.play("click")
                cancelAll()
                modal = Modal.FURNACE
            }
            Target.SLEEP -> trySleep()
            Target.CAMPFIRE -> {
                val cp = targetCamp
                if (cp != null) {
                    if (!cp.isLit) {
                        if (player.wood >= 2) {
                            player.wood -= 2
                            cp.isLit = true
                            Sound.play("magic")
                            toast("أشعلت ${cp.nameAr}! تم فتح السفر السريع إليه")
                        } else {
                            toast("تحتاج 2 خشب لإشعال المخيم")
                            Sound.play("deny")
                        }
                    } else {
                        openTravelModal()
                    }
                }
            }
            Target.CAVE_ENTER -> {
                val ce = targetCave
                if (ce != null) enterCave(ce, 1)
            }
            Target.CAVE_EXIT -> exitCaveToSurface()
            Target.CAVE_NEXT -> {
                val ce = activeCave
                if (ce != null) enterCave(ce, caveFloor + 1)
            }
            Target.TOTEM -> {
                val tt = targetTotem
                if (tt != null && !tt.activated) {
                    tt.activated = true
                    Sound.play("boss")
                    shake = 12f
                    player.goldKeys++
                    val b = tt.biome.coerceIn(0, 5)
                    player.mats[b] += 2
                    addFloat("+1 مفتاح ذهبي  +2 ${Mats.names[b]}", tt.x + 44f, tt.y - 10f, GOLD, 24f)
                    for (k in 0 until 3) {
                        val ang = k * 2.1f
                        val en = Enemy(tt.x + cos(ang) * 140f, tt.y + sin(ang) * 140f, EnemyRole.HEAVY, EraType.values()[b])
                        en.aggro = true
                        enemies.add(en)
                    }
                    toast("تفعّل مذبح التحدي! اهزم الحراس النخبة")
                    advanceQuest("q_cave", 1)
                }
            }
            Target.VILLAGER -> {
                if (targetVillager != null) {
                    Sound.play("click")
                    cancelAll()
                    modal = Modal.VILLAGER
                }
            }
            Target.BOAT_DOCK -> {
                if (!player.hasBoat) {
                    player.hasBoat = true
                    Sound.play("era_clear", 0.7f)
                    toast("حصلت على مركب القرية! يمكنك الآن الإبحار في المياه العميقة")
                } else {
                    toast("مركبك جاهز! اتجه للمياه للإبحار تلقائياً")
                }
            }
            Target.BOSS_ALTAR -> {
                Sound.play("click")
                cancelAll()
                scrollY = 0f
                modal = Modal.BOSS_ALTAR
            }
            Target.STORAGE_CHEST -> {
                Sound.play("click")
                cancelAll()
                scrollY = 0f
                modal = Modal.STORAGE
            }
            Target.CANNON_REFILL -> {
                val cn = targetCannon
                if (cn != null) {
                    val need = cn.maxAmmo - cn.ammo
                    if (need <= 0) {
                        toast("المدفع ممتلئ بالبارود بالكامل (${cn.ammo}/${cn.maxAmmo})!")
                    } else if (player.gunpowder <= 0) {
                        toast("ليس معك بارود! اصنعه في الكرافتينج 4×4 أو اشتره من القرويين أو اجمعه من الكهوف")
                        Sound.play("deny")
                    } else {
                        val put = min(need, min(10, player.gunpowder))
                        player.gunpowder -= put
                        cn.ammo += put
                        Sound.play("buy")
                        particles.burst(cn.x + 38f, cn.y + 38f, 10, Color.parseColor("#FFAB40"), 150f, 5f, 0.4f)
                        addFloat("+$put بارود للمدفع 💥", cn.x + 38f, cn.y - 12f, GOLD, 22f)
                        toast("تمت تعبئة المدفع بالبارود (${cn.ammo}/${cn.maxAmmo})!")
                        advanceQuest("q_powder", 1)
                    }
                }
            }
            Target.FARM_HARVEST -> {
                val w = ow
                if (w != null && targetHarvestTx >= 0 && targetHarvestTy >= 0) {
                    harvestCropTile(w, targetHarvestTx, targetHarvestTy)
                }
            }
            Target.STRUCTURE_NODE -> {
                val sn = targetStructure
                if (sn != null && !sn.claimed) {
                    claimStructureNode(sn)
                }
            }
            Target.REVIVE_PARTNER -> {
                if (LanMultiplayer.isConnected && coopPartner.isDowned) {
                    coopPartner.isDowned = false
                    coopPartner.hp = (coopPartner.maxHp * 0.65f).toInt()
                    LanMultiplayer.send("REVIVE")
                    Sound.play("era_clear")
                    particles.burst(coopPartner.cx, coopPartner.cy, 22, Color.parseColor("#69F0AE"), 220f, 6f, 0.55f)
                    addFloat("تم إنعاش صديقك! ❤️‍🩹🤝", coopPartner.cx, coopPartner.y - 20f, Color.parseColor("#69F0AE"), 24f)
                    toast("أنعشت صديقك وعاد للقتال معك!")
                }
            }
            Target.NONE -> {}
        }
    }

    private fun openChest(c: TreasureChest) {
        if (c.requiresGoldKey && player.goldKeys <= 0) {
            toast("صندوق ذهبي مقفل! تحتاج مفتاحاً ذهبياً (من المتجر أو الصناعة أو الزعماء)")
            Sound.play("deny")
            return
        }
        if (c.requiresGoldKey) {
            player.goldKeys--
            advanceQuest("q_treasure", 1)
        }
        val g = c.open()
        Sound.play("coin")
        particles.burst(c.x + 32f, c.y + 20f, 16, Color.parseColor("#FFD740"), 190f, 6f, 0.6f)
        if (c.isGem) {
            player.hasOceanGem = true
            toast("حصلت على جوهرة المحيط!")
        } else {
            player.coins += g
            addFloat("+$g ذهب", c.x + 32f, c.y - 6f, Color.parseColor("#FFD740"), 30f)
            if (c.requiresGoldKey) {
                val bi = if (inCave) (activeCave?.biome ?: 1) else (ow?.biomeAtPx(c.x, c.y) ?: era.ordinal)
                player.mats[bi.coerceIn(0, 5)] += 2
                player.potions += 1
                addFloat("+2 ${Mats.names[bi.coerceIn(0, 5)]}  +1 جرعة", c.x + 32f, c.y - 34f, Color.parseColor("#69F0AE"), 22f)
                if (!inCave && player.unlockSeal(2)) {
                    advanceQuest("q_seals", 1)
                    toast("فتحت صندوق الحصن الذهبي وحصلت على ختم قبطان البحار! (${player.abyssalSeals}/7)")
                }
            }
            if (inCave) advanceQuest("q_cave", 1)
        }
    }

    private fun updateButtons() {
        attackBtn.iconPath = player.weapon.iconAsset
        attackBtn.accent = player.weapon.color
        weaponSwitchBtn.visible = !player.isDead
        weaponSwitchBtn.iconPath = player.weapon.iconAsset
        weaponSwitchBtn.badge = "${player.owned.count { GameCatalogue.weaponById(it) != null }}"
        dashBtn.cooldown = player.cooldownFrac(player.dashCd, Player.DASH_CD)

        // Update Fortnite Turbo Wall, Edit Window, Parry, Grapple & Build buttons
        val inOwSurface = ow != null && !inCave && !player.isDead
        turboWallBtn.visible = inOwSurface
        turboWallBtn.badge = (player.blkStone + player.blkWood).toString()
        farmPlantBtn.visible = inOwSurface
        farmPlantBtn.iconPath = when {
            player.cannonsItem > 0 -> "icons/pirate_cannon.png"
            player.cropSeeds > 0 -> "icons/seeds.png"
            player.treeSaplings > 0 -> "icons/sapling.png"
            else -> "icons/gunpowder.png"
        }
        farmPlantBtn.badge = (player.cropSeeds + player.treeSaplings + player.cannonsItem).toString()
        editBlockBtn.visible = inOwSurface
        editBlockBtn.badge = if (player.blkLaunchPad > 0) "🚀${player.blkLaunchPad}" else null
        parryBtn.visible = !player.isDead
        parryBtn.cooldown = player.cooldownFrac(player.parryCd, Player.PARRY_CD)
        grappleBtn.visible = !player.isDead
        grappleBtn.cooldown = player.cooldownFrac(player.grappleCd, Player.GRAPPLE_CD)

        buildBtn.visible = inOwSurface && player.hotbarIdx >= 1
        if (buildBtn.visible) {
            buildBtn.iconPath = hotbarSlotIcon(player.hotbarIdx)
            val cnt = hotbarSlotCount(player.hotbarIdx)
            buildBtn.badge = if (cnt >= 0) cnt.toString() else null
            buildBtn.label = if (player.hotbarIdx == 1) "كسر ⛏️" else "بناء 🧱"
            buildBtn.accent = if (player.hotbarIdx == 1) Color.parseColor("#FFB74D") else Color.parseColor("#8BC34A")
        }

        val sp = player.currentSpell
        magicBtn.iconPath = sp.iconAsset
        magicBtn.accent = sp.color
        magicBtn.label = sp.nameAr.substringBefore(" ")
        magicBtn.cooldown = player.cooldownFrac(player.magicCd, sp.cooldown)
        magicBtn.enabled = player.mana >= sp.manaCost
        slowBtn.cooldown = if (player.slowTimer > 0f) 0f else player.cooldownFrac(player.slowCd, Player.SLOW_CD)
        slowBtn.enabled = player.mana >= Player.SLOW_COST || player.slowTimer > 0f
        bombBtn.badge = player.bombs.toString()
        bombBtn.enabled = player.bombs > 0
        bombBtn.cooldown = player.cooldownFrac(player.bombCd, Player.BOMB_CD)
        potionBtn.badge = player.potions.toString()
        potionBtn.enabled = player.potions > 0 && player.hp < player.maxHp
        eatBtn.visible = ow != null && !player.isDead
        eatBtn.badge = foodCount().toString()
        eatBtn.enabled = foodCount() > 0 && hunger < 98f
        eatBtn.iconPath = when {
            player.stew > 0 -> "icons/stew.png"
            player.cookedMeat > 0 -> "icons/cooked_meat.png"
            player.apples > 0 -> "icons/apple.png"
            else -> "icons/raw_meat.png"
        }
        val readyQuests = quests.count { it.isComplete && !it.claimed }
        bagBtn.badge = if (readyQuests > 0) "!$readyQuests" else null
    }

    // ---------------------------------------------------------------- modal logic
    private fun openShop() {
        modal = Modal.SHOP
        shopTab = 0
        scrollY = 0f
        selectedId = ""
        mPointer = -1
        cancelAll()
    }

    private fun loadRecipePatternInto4x4(r: CraftRecipe) {
        val pat = r.pattern4x4.padEnd(16, '.')
        for (i in 0 until 16) craftGrid4x4[i] = pat[i]
    }

    private fun openCraft() {
        modal = Modal.CRAFT
        scrollY = 0f
        val first = CraftingRegistry.recipes.firstOrNull()
        if (first != null) {
            selectedId = first.id
            loadRecipePatternInto4x4(first)
        } else {
            selectedId = ""
        }
        mPointer = -1
        cancelAll()
        Sound.play("click")
    }

    private fun openBag() {
        modal = Modal.BAG
        bagTab = 0
        scrollY = 0f
        selectedId = ""
        mPointer = -1
        cancelAll()
        Sound.play("click")
    }

    private fun openTravelModal() {
        if (inCave) {
            toast("اخرج لسطح العالم المفتوح أولاً للسفر السريع")
            return
        }
        modal = Modal.TRAVEL
        scrollY = 0f
        mPointer = -1
        cancelAll()
        Sound.play("click")
    }

    private fun modalTouch(t: Touch) {
        if (modal == Modal.HUD_EDIT) {
            hudEditTouch(t)
            return
        }
        when (t.type) {
            0 -> if (mPointer == -1) {
                mPointer = t.id
                mDownX = t.x
                mDownY = t.y
                mLastY = t.y
                mMoved = false
            }
            1 -> if (t.id == mPointer) {
                if (!mMoved && abs(t.y - mDownY) > 14f * Ui.u) mMoved = true
                if (mMoved && (modal == Modal.SHOP || modal == Modal.CRAFT || (modal == Modal.BAG && bagTab != 0) || modal == Modal.TRAVEL || modal == Modal.FURNACE || modal == Modal.VILLAGER || modal == Modal.BOSS_ALTAR || modal == Modal.STORAGE || modal == Modal.WORLD_MAP)) {
                    scrollY = (scrollY - (t.y - mLastY)).coerceIn(0f, maxScroll)
                }
                mLastY = t.y
            }
            2 -> if (t.id == mPointer) {
                mPointer = -1
                if (!mMoved) tapModal(t.x, t.y)
            }
            3 -> mPointer = -1
        }
    }

    private fun hitAt(x: Float, y: Float): String? {
        var i = hits.size - 1
        while (i >= 0) {
            if (hits[i].rect.contains(x, y)) return hits[i].id
            i--
        }
        return null
    }

    private fun tapModal(x: Float, y: Float) {
        when (modal) {
            Modal.DIALOGUE -> advanceDialogue()
            Modal.PAUSE -> tapPause(hitAt(x, y))
            Modal.SHOP -> tapList(hitAt(x, y), x, y, true)
            Modal.CRAFT -> tapCraft4x4Modal(hitAt(x, y))
            Modal.BAG -> tapBag(hitAt(x, y))
            Modal.VILLAGER -> tapVillagerModal(hitAt(x, y))
            Modal.TRAVEL -> tapTravelModal(hitAt(x, y))
            Modal.FURNACE -> tapFurnaceModal(hitAt(x, y))
            Modal.BOSS_ALTAR -> tapBossAltarModal(hitAt(x, y))
            Modal.STORAGE -> tapStorageModal(hitAt(x, y))
            Modal.WORLD_MAP -> tapWorldMapModal(hitAt(x, y), x, y)
            Modal.MULTIPLAYER -> tapMultiplayerModal(hitAt(x, y))
            Modal.HUD_EDIT -> {}
            Modal.NONE -> {}
        }
    }

    private fun tapPause(id: String?) {
        when (id) {
            "resume" -> {
                Sound.play("click")
                modal = Modal.NONE
            }
            "bag" -> {
                openBag()
            }
            "multiplayer" -> {
                Sound.play("click")
                modal = Modal.MULTIPLAYER
            }
            "hud_edit" -> {
                Sound.play("click")
                modal = Modal.HUD_EDIT
            }
            "boss_altar" -> {
                Sound.play("click")
                scrollY = 0f
                modal = Modal.BOSS_ALTAR
            }
            "sfx" -> {
                Sound.sfxOn = !Sound.sfxOn
                SaveStore.putAll(mapOf("sfx" to Sound.sfxOn), false)
                Sound.play("click")
            }
            "music" -> {
                Sound.setMusic(!Sound.musicOn)
                SaveStore.putAll(mapOf("music" to Sound.musicOn), false)
                Sound.play("click")
            }
            "quality" -> {
                Sound.play("click")
                toggleQuality()
            }
            "title" -> {
                Sound.play("click")
                saveProgress()
                backToTitle()
            }
        }
    }

    private fun tapList(id: String?, x: Float, y: Float, shop: Boolean) {
        if (id == null) return
        if (id == "close") {
            Sound.play("click")
            saveProgress()
            modal = Modal.NONE
            return
        }
        if (id.startsWith("tab:")) {
            shopTab = id.substring(4).toInt()
            scrollY = 0f
            selectedId = ""
            Sound.play("click")
            return
        }
        if (!listRect.contains(x, y)) return
        val isBuy = id.startsWith("buy:")
        val key = id.substring(4)
        if (selectedId != key) {
            selectedId = key
            Sound.play("click")
            return
        }
        if (!isBuy) return
        if (shop) {
            val item = GameCatalogue.shopItems.firstOrNull { it.id == key } ?: return
            buyItem(item)
        } else {
            val r = CraftingRegistry.recipes.firstOrNull { it.id == key } ?: return
            craftItem(r)
        }
    }

    private fun upgCount(id: String): Int = if (id == "up_hp") player.upgHp else player.upgMana

    private fun tier(): Int = if (ow != null) 5 else era.ordinal

    private fun priceOf(item: ShopItem): Int {
        var p = item.price.toFloat() * (1f + 0.15f * tier())
        if (item.type == ItemType.UPGRADE) p *= (1 + upgCount(item.id)).toFloat()
        return p.toInt()
    }

    private fun stockLimit(item: ShopItem): Int = when (item.id) {
        "potion_heal" -> 5
        "bombs" -> 4
        else -> 0
    }

    private fun stockLeft(item: ShopItem): Int {
        val lim = stockLimit(item)
        return if (lim == 0) 99 else lim - (shopBought[item.id] ?: 0)
    }

    private fun shopState(item: ShopItem): Int {
        val done = when (item.type) {
            ItemType.WEAPON, ItemType.ARMOR -> player.owned.contains(item.id)
            ItemType.UPGRADE -> upgCount(item.id) >= item.maxCount
            ItemType.CONSUMABLE -> false
        }
        if (done) return 1
        if (stockLeft(item) <= 0) return 3
        return if (player.coins >= priceOf(item)) 0 else 2
    }

    private fun applyArmor() {
        val old = player.maxHp
        player.armorId = when {
            player.owned.contains("armor_titan") -> "armor_titan"
            player.owned.contains("armor_nano") -> "armor_nano"
            player.owned.contains("armor_royal") -> "armor_royal"
            else -> ""
        }
        player.recomputeStats()
        player.hp = min(player.maxHp, player.hp + max(0, player.maxHp - old))
    }

    private fun buyItem(item: ShopItem) {
        val st = shopState(item)
        if (st == 1) {
            toast("اشتريته بالفعل")
            Sound.play("deny")
            return
        }
        if (st == 3) {
            toast("نفد المخزون اليوم")
            Sound.play("deny")
            return
        }
        if (st == 2) {
            toast("ذهبك لا يكفي")
            Sound.play("deny")
            return
        }
        player.coins -= priceOf(item)
        shopBought[item.id] = (shopBought[item.id] ?: 0) + 1
        when (item.id) {
            "dagger_silver", "sword_fire", "bow_fire", "sword_pirate", "spear_chrono", "hammer_thunder", "musket_powder" -> {
                player.owned.add(item.id)
                player.weapon = GameCatalogue.weaponById(item.id) ?: player.weapon
            }
            "golem_pack" -> {
                player.golemsItem += 1
                if (ow != null && !inCave) {
                    player.golemsItem--
                    golems.add(IronGolemGuard(player.cx + 48f, player.cy + 20f))
                }
            }
            "watering_can" -> {
                player.hasWateringCan = true
            }
            "armor_royal" -> {
                player.owned.add(item.id)
                applyArmor()
            }
            "gunpowder_pack" -> {
                player.gunpowder += 10
                advanceQuest("q_powder", 1)
            }
            "seeds_pack" -> {
                player.cropSeeds += 4
                player.treeSaplings += 2
            }
            "potion_heal" -> player.potions += 2
            "pot_speed" -> player.speedPots += 1
            "stew_pack" -> player.stew += 2
            "bombs" -> player.bombs += 2
            "key_gold" -> player.goldKeys += 1
            "treasure_map" -> {
                player.treasureMaps += 1
                spawnRandomTreasureMound()
            }
            "up_hp" -> {
                player.upgHp++
                player.recomputeStats()
                player.hp = min(player.maxHp, player.hp + 30)
            }
            "up_mana" -> {
                player.upgMana++
                player.recomputeStats()
            }
        }
        Sound.play("buy")
        toast("تم الشراء: ${item.titleAr}")
        selectedId = ""
        saveProgress()
    }

    private fun costOf(r: CraftRecipe): Cost {
        if (r.resultType == "UPG_WEAPON" || r.resultType == "UPG_ARMOR") {
            val lvl = if (r.resultType == "UPG_WEAPON") player.weaponLvl else player.armorLvl
            val m = IntArray(6)
            m[min(lvl, 4)] = 2 + lvl / 2
            val scale = if (r.resultType == "UPG_WEAPON") 1f else 0.9f
            val gold = ((60 * (lvl + 1) + 20 * lvl * lvl) * scale).toInt()
            return Cost(0, 3 + 2 * lvl, 2 + 2 * lvl, gold, m, 0, 0)
        }
        return Cost(r.wood, r.stone, r.iron, r.gold, r.mats, r.wool, r.gunpowder)
    }

    private fun craftState(r: CraftRecipe): Int {
        val done = when (r.resultType) {
            "PICKAXE" -> player.hasPickaxe
            "WATERING_CAN" -> player.hasWateringCan
            "WEAPON_BOW" -> player.owned.contains("bow_fire")
            "WEAPON_MUSKET" -> player.owned.contains("musket_powder")
            "WEAPON_HAMMER" -> player.owned.contains("hammer_thunder")
            "ENCH_LIFESTEAL" -> player.weaponEnchant == 1
            "ENCH_LIGHTNING" -> player.weaponEnchant == 2
            "BOAT" -> player.hasBoat
            "PLASMA" -> player.hasPlasma
            "SHIELD" -> player.owned.contains("armor_nano")
            "GODBLADE" -> player.owned.contains("godblade")
            "ARMOR_TITAN" -> player.owned.contains("armor_titan")
            "ABYSS_SWORD" -> player.owned.contains("abyss_breaker")
            "ABYSS_SEAL" -> player.abyssalSeals >= 7
            "HELMET_UPG" -> player.helmetTier >= 4
            "CHEST_UPG" -> player.chestTier >= 4
            "LEGS_UPG" -> player.legsTier >= 4
            "BOOTS_UPG" -> player.bootsTier >= 4
            "UPG_WEAPON" -> player.weaponLvl >= CraftingRegistry.MAX_UPG
            "UPG_ARMOR" -> player.armorLvl >= CraftingRegistry.MAX_UPG
            else -> false
        }
        if (done) return 1
        val c = costOf(r)
        var ok = player.wood >= c.wood && player.stone >= c.stone && player.iron >= c.iron &&
            player.coins >= c.gold && player.wool >= c.wool && player.gunpowder >= c.gunpowder
        for (i in 0 until 6) if (player.mats[i] < c.mats[i]) ok = false
        return if (ok) 0 else 2
    }

    private fun craftItem(r: CraftRecipe) {
        val st = craftState(r)
        if (st == 1) {
            toast("مملوك أو وصلت للحد الأقصى")
            Sound.play("deny")
            return
        }
        if (st == 2) {
            toast("الموارد أو الصوف أو الذهب غير كافية")
            Sound.play("deny")
            return
        }
        val c = costOf(r)
        player.wood -= c.wood
        player.stone -= c.stone
        player.iron -= c.iron
        player.coins -= c.gold
        player.wool -= c.wool
        player.gunpowder -= c.gunpowder
        for (i in 0 until 6) player.mats[i] -= c.mats[i]
        when (r.resultType) {
            "GUNPOWDER" -> {
                player.gunpowder += 10
                advanceQuest("q_powder", 1)
            }
            "STORAGE_CHEST" -> player.storageChestsItem += 1
            "HOUSE_BP" -> player.houseBlueprints += 1
            "FORT_BP" -> player.fortBlueprints += 1
            "SEEDS_PACK" -> {
                player.cropSeeds += 4
                player.treeSaplings += 2
            }
            "IRON_GOLEM" -> {
                if (ow != null && !inCave) {
                    golems.add(IronGolemGuard(player.cx + 54f, player.cy + 20f))
                    toast("صنعت ونشرت الغولم الحديدي الحارس 🤖 لحماية قلعتك!")
                } else {
                    player.golemsItem += 1
                }
            }
            "WEAPON_BOW" -> {
                player.owned.add("bow_fire")
                player.weapon = GameCatalogue.WEAPON_BOW
            }
            "WEAPON_MUSKET" -> {
                player.owned.add("musket_powder")
                player.weapon = GameCatalogue.WEAPON_MUSKET
            }
            "WEAPON_HAMMER" -> {
                player.owned.add("hammer_thunder")
                player.weapon = GameCatalogue.WEAPON_HAMMER
            }
            "WATERING_CAN" -> player.hasWateringCan = true
            "ENCH_LIFESTEAL" -> player.weaponEnchant = 1
            "ENCH_LIGHTNING" -> player.weaponEnchant = 2
            "BLK_OBSIDIAN" -> player.blkObsidian += 6
            "BLK_ROYAL" -> player.blkRoyalFloor += 8
            "BLK_SPIKES" -> player.blkSpikes += 6
            "BLK_WIRE" -> player.blkWire += 6
            "BLK_BEACON" -> player.blkBeacon += 2
            "WATCHTOWER_BP" -> player.towerBlueprints += 1
            "FARM_BP" -> player.farmBlueprints += 1
            "BLK_WOOD" -> player.blkWood += 10
            "BLK_STONE" -> player.blkStone += 10
            "BLK_WINDOW" -> player.blkWindow += 4
            "BLK_LAUNCHPAD" -> player.blkLaunchPad += 2
            "BLK_BRIDGE" -> player.blkBridge += 8
            "BLK_IRONGATE" -> player.blkIronGate += 2
            "BLK_DOOR" -> player.blkDoor += 2
            "BLK_FENCE" -> player.blkFence += 6
            "BLK_TORCH" -> player.blkTorch += 6
            "BLK_TNT" -> player.blkTnt += 2
            "ARMOR_TITAN" -> {
                player.owned.add("armor_titan")
                applyArmor()
            }
            "ABYSS_SWORD" -> {
                player.owned.add("abyss_breaker")
                player.equipBest()
                if (player.unlockSeal(6)) advanceQuest("q_seals", 1)
            }
            "ABYSS_SEAL" -> {
                for (si in 0..6) {
                    if (player.unlockSeal(si)) {
                        advanceQuest("q_seals", 1)
                        break
                    }
                }
            }
            "BED" -> player.bedsItem++
            "FURNACE" -> player.furnacesItem++
            "BOAT" -> player.hasBoat = true
            "CANNON" -> player.cannonsItem++
            "CAMPFIRE" -> player.campfiresItem++
            "PICKAXE" -> player.hasPickaxe = true
            "GOLD_KEY" -> player.goldKeys++
            "STEW" -> {
                player.stew += 2
                advanceQuest("q_cook", 2)
            }
            "SPEED_POT" -> player.speedPots += 2
            "BOMB" -> player.bombs += 2
            "PLASMA" -> player.hasPlasma = true
            "SHIELD" -> {
                player.owned.add("armor_nano")
                applyArmor()
            }
            "GODBLADE" -> {
                player.owned.add("godblade")
                player.equipBest()
            }
            "HELMET_UPG" -> { player.helmetTier++; player.recomputeStats(); player.hp = min(player.maxHp, player.hp + 35) }
            "CHEST_UPG" -> { player.chestTier++; player.recomputeStats(); player.hp = min(player.maxHp, player.hp + 50) }
            "LEGS_UPG" -> { player.legsTier++; player.recomputeStats(); player.hp = min(player.maxHp, player.hp + 40) }
            "BOOTS_UPG" -> { player.bootsTier++; player.recomputeStats(); player.hp = min(player.maxHp, player.hp + 25) }
            "UPG_WEAPON" -> player.weaponLvl++
            "UPG_ARMOR" -> {
                player.armorLvl++
                player.recomputeStats()
                player.hp = min(player.maxHp, player.hp + 25)
            }
        }
        Sound.play("buy")
        toast("تم الصنع: ${r.titleAr}")
        selectedId = ""
        saveProgress()
    }

    private var titleTaps = 0

    private fun tapTitle(x: Float, y: Float) {
        if (hitAt(x, y) == null && y < curH * 0.32f) {
            titleTaps++
            if (titleTaps >= 5) {
                titleTaps = 0
                SaveStore.putAll(mapOf("ow_unlocked" to true), false)
                toast("تم فتح العالم المفتوح للتجربة")
                Sound.play("era_clear")
            }
            return
        }
        when (hitAt(x, y)) {
            "new" -> {
                Sound.play("click")
                startNewGame()
            }
            "cont" -> {
                Sound.play("click")
                continueGame()
            }
            "sfx" -> {
                Sound.sfxOn = !Sound.sfxOn
                SaveStore.putAll(mapOf("sfx" to Sound.sfxOn), false)
                Sound.play("click")
            }
            "music" -> {
                Sound.setMusic(!Sound.musicOn)
                SaveStore.putAll(mapOf("music" to Sound.musicOn), false)
            }
            "quality" -> {
                Sound.play("click")
                toggleQuality()
            }
            "open" -> {
                Sound.play("click")
                val p = Player(300f, 880f)
                p.load()
                player = p
                startOpen(SaveStore.str("mode", "") == "open")
            }
            "multiplayer_title" -> {
                Sound.play("click")
                val p = Player(300f, 880f)
                p.load()
                player = p
                startOpen(SaveStore.str("mode", "") == "open")
                modal = Modal.MULTIPLAYER
            }
            "hud_edit_title" -> {
                Sound.play("click")
                val p = Player(300f, 880f)
                p.load()
                player = p
                startOpen(SaveStore.str("mode", "") == "open")
                modal = Modal.HUD_EDIT
            }
        }
    }

    private fun tapEnd(x: Float, y: Float) {
        if (endT < 0.8f) return
        when (hitAt(x, y)) {
            "retry" -> {
                Sound.play("click")
                retryFromCheckpoint()
            }
            "open" -> {
                Sound.play("click")
                startOpen(false)
            }
            "title" -> {
                Sound.play("click")
                backToTitle()
            }
        }
    }

    // ---------------------------------------------------------------- rendering
    private val GOLD = Color.parseColor("#FFD54F")

    private fun render(c: Canvas) {
        c.drawColor(Color.BLACK)
        hits.clear()
        when (state) {
            State.TITLE -> drawTitle(c)
            State.PLAYING -> {
                if (ow != null) {
                    drawOpenWorld(c)
                    drawOpenHud(c)
                } else {
                    drawWorld(c)
                    drawHud(c)
                }
                when (modal) {
                    Modal.SHOP -> drawList(c, true)
                    Modal.CRAFT -> drawCraft4x4Modal(c)
                    Modal.PAUSE -> drawPause(c)
                    Modal.DIALOGUE -> drawDialogue(c)
                    Modal.BAG -> drawBagModal(c)
                    Modal.VILLAGER -> drawVillagerModal(c)
                    Modal.TRAVEL -> drawTravelModal(c)
                    Modal.FURNACE -> drawFurnaceModal(c)
                    Modal.HUD_EDIT -> drawHudEditModal(c)
                    Modal.BOSS_ALTAR -> drawBossAltarModal(c)
                    Modal.STORAGE -> drawStorageModal(c)
                    Modal.WORLD_MAP -> drawWorldMapModal(c)
                    Modal.MULTIPLAYER -> drawMultiplayerModal(c)
                    Modal.NONE -> {}
                }
            }
            State.GAME_OVER -> {
                drawWorld(c)
                drawGameOver(c)
            }
            State.VICTORY -> drawVictory(c)
        }
        drawToast(c)
        if (fade > 0f) {
            screenPaint.color = Color.argb((fade * 255f).toInt().coerceIn(0, 255), 0, 0, 0)
            c.drawRect(0f, 0f, curW.toFloat(), curH.toFloat(), screenPaint)
        }
    }

    private fun drawWorld(c: Canvas) {
        val sh = shake
        val ox = if (sh > 0f) (rnd.nextFloat() - 0.5f) * sh * 2f else 0f
        val oy = if (sh > 0f) (rnd.nextFloat() - 0.5f) * sh * 2f else 0f
        c.save()
        c.scale(zoom, zoom)
        c.translate(-camera.x + ox, -camera.y + oy)
        val vx = camera.x - 12f
        val vy = camera.y - 12f
        val vw = camera.viewW + 24f
        val vh = camera.viewH + 24f
        world.draw(c, vx, vy, vw, vh, time)
        world.drawDecor(c, vx, vy, vw, vh)
        for (t in traps) t.draw(c)
        for (p in props) p.draw(c)
        mound?.draw(c)
        for (r in rocks) r.draw(c, time)
        for (cr in crates) cr.draw(c)
        for (g in gens) g.draw(c)
        table.draw(c, time)
        for (ch in chests) ch.draw(c, time)
        merchant.draw(c)
        portal.draw(c)
        for (t in trees) t.draw(c)
        for (co in coins) co.draw(c)
        for (en in enemies) if (en.isDead) en.draw(c, time)
        val py = player.y
        for (en in enemies) if (!en.isDead && en.y < py) en.draw(c, time)
        player.draw(c, time)
        for (en in enemies) if (!en.isDead && en.y >= py) en.draw(c, time)
        for (pr in projectiles) pr.draw(c)
        for (ex in explosions) ex.draw(c)
        particles.draw(c)
        for (f in floats) f.draw(c)
        c.restore()

        val dp = darkPaint
        if (dp != null && player.visionBuffTimer <= 0f) {
            val sx = (player.cx - camera.x) * zoom
            val sy = (player.cy - camera.y) * zoom
            c.save()
            c.translate(sx, sy)
            c.drawRect(-curW * 2f, -curH * 2f, curW * 2f, curH * 2f, dp)
            c.restore()
        }
        val vg = vignette
        if (vg != null && state == State.PLAYING) {
            val low = player.hp.toFloat() / max(1, player.maxHp)
            var a = 0f
            if (hurtFlash > 0f) a = hurtFlash / 0.4f * 190f
            if (low < 0.3f) a = max(a, (0.3f - low) / 0.3f * (80f + 40f * sin(time * 6f)))
            if (a > 1f) {
                vg.alpha = a.toInt().coerceIn(0, 255)
                c.drawRect(0f, 0f, curW.toFloat(), curH.toFloat(), vg)
            }
        }
    }

    private fun hpColor(f: Float): Int = when {
        f > 0.5f -> Color.parseColor("#43A047")
        f > 0.25f -> Color.parseColor("#FB8C00")
        else -> Color.parseColor("#E53935")
    }

    private fun drawHud(c: Canvas) {
        val u = Ui.u
        val w = curW.toFloat()
        val h = curH.toFloat()

        drawStatusPanel(c)

        val cw = min(560f * u, w * 0.42f)
        tmpR.set(w / 2f - cw / 2f, 10f * u, w / 2f + cw / 2f, 40f * u)
        Ui.panel(c, tmpR, Color.parseColor("#6D4C41"), Color.argb(190, 12, 16, 30), 10f)
        Ui.text(c, era.title, tmpR.centerX(), tmpR.centerY() + 6f * u, 16f, GOLD)
        tmpR.set(w / 2f - cw / 2f, 46f * u, w / 2f + cw / 2f, 92f * u)
        Ui.panel(c, tmpR, Color.parseColor("#455A64"), Color.argb(180, 10, 14, 26), 10f)
        Ui.icon(c, "ui/ico/excl.png", tmpR.right - 20f * u, tmpR.centerY(), 22f, Color.parseColor("#FFCA28"))
        Ui.wrapped(c, objectiveText(), tmpR.left + 12f * u, tmpR.top + 7f * u, cw - 54f * u, 15f, Color.WHITE, false, 2)

        val b = boss
        if (b != null && !b.isDead && b.aggro) {
            val bwid = min(520f * u, w * 0.44f)
            tmpR.set(w / 2f - bwid / 2f, 98f * u, w / 2f + bwid / 2f, 134f * u)
            Ui.panel(c, tmpR, Color.parseColor("#B71C1C"), Color.argb(200, 20, 8, 10), 10f)
            Ui.text(c, bossName(era) + if (b.shielded) "  (محصّن)" else "", tmpR.centerX(), tmpR.top + 17f * u, 15f, Color.WHITE)
            val br = RectF(tmpR.left + 12f * u, tmpR.top + 21f * u, tmpR.right - 12f * u, tmpR.bottom - 7f * u)
            Ui.bar(c, br, b.hp.toFloat() / max(1, b.maxHp), if (b.shielded) Color.parseColor("#00BCD4") else Color.parseColor("#E53935"))
        }

        val mw = 190f * u
        val mh = mw * TileMap.ROWS / TileMap.COLS
        val mx = w - 14f * u - mw
        val my = 76f * u
        tmpR.set(mx - 4f * u, my - 4f * u, mx + mw + 4f * u, my + mh + 4f * u)
        Ui.panel(c, tmpR, Color.parseColor("#455A64"), Color.argb(215, 6, 8, 16), 8f)
        val mmR = RectF(mx, my, mx + mw, my + mh)
        c.drawBitmap(world.minimap, null, mmR, null)
        val fx = mw / world.width
        val fy = mh / world.height
        for (en in enemies) {
            if (en.isDead) continue
            Ui.circle(c, mx + en.cx * fx, my + en.cy * fy, if (en.isBoss) 5f * u else 2.8f * u, if (en.isBoss) Color.parseColor("#FF1744") else Color.parseColor("#FF8A80"))
        }
        Ui.circle(c, mx + (merchant.x + 48f) * fx, my + (merchant.y + 60f) * fy, 3.4f * u, Color.parseColor("#FFD54F"))
        if (portal.isActive) Ui.circle(c, mx + (portal.x + 70f) * fx, my + (portal.y + 70f) * fy, 4.5f * u, Color.CYAN)
        Ui.circle(c, mx + player.cx * fx, my + player.cy * fy, 4f * u, Color.WHITE)
        Ui.strokeRound(c, RectF(mx + camera.x * fx, my + camera.y * fy, mx + (camera.x + camera.viewW) * fx, my + (camera.y + camera.viewH) * fy), Color.argb(150, 255, 255, 255), 1.2f, 2f)

        drawControls(c)

        if (bannerTimer > 0f && modal != Modal.DIALOGUE) {
            val a = min(1f, min(bannerTimer / 0.9f, (4.2f - bannerTimer) / 0.6f)).coerceIn(0f, 1f)
            Ui.fillRect(c, 0f, h * 0.27f, w, h * 0.27f + 170f * u, Color.argb((150 * a).toInt(), 0, 0, 0))
            Ui.text(c, era.title, w / 2f, h * 0.27f + 62f * u, 38f, Color.argb((255 * a).toInt(), 255, 213, 79))
            Ui.text(c, era.subtitle, w / 2f, h * 0.27f + 98f * u, 22f, Color.argb((230 * a).toInt(), 160, 220, 255))
            Ui.wrapped(c, era.description, w / 2f - 340f * u, h * 0.27f + 112f * u, 680f * u, 17f, Color.argb((235 * a).toInt(), 235, 235, 235), false, 2, true, false)
        }
    }

    private fun drawStatusPanel(c: Canvas) {
        val u = Ui.u
        val px = 14f * u
        val py = 14f * u
        val pw = 400f * u
        val anyMats = player.mats.any { it > 0 }
        val ph = (if (anyMats) 168f else 138f) * u
        tmpR.set(px, py, px + pw, py + ph)
        Ui.panel(c, tmpR, Color.parseColor("#B8860B"), Color.argb(205, 12, 16, 30), 12f)
        tmpR.set(px + 10f * u, py + 12f * u, px + 68f * u, py + 70f * u)
        Ui.fillRound(c, tmpR, Color.argb(210, 24, 30, 52), 10f)
        Ui.strokeRound(c, tmpR, player.weapon.color, 2.5f, 10f)
        Ui.icon(c, player.weapon.iconAsset, tmpR.centerX(), tmpR.centerY(), 42f)
        val armor = GameCatalogue.armorById(player.armorId)
        if (armor != null) Ui.icon(c, "ui/ico/shield.png", px + 58f * u, py + 62f * u, 20f, Color.parseColor("#4DD0E1"))

        val bx = px + 80f * u
        val bw = pw - 94f * u
        Ui.icon(c, "ui/ico/heart.png", bx + 10f * u, py + 26f * u, 24f, Color.parseColor("#FF5252"))
        tmpR.set(bx + 28f * u, py + 14f * u, bx + bw, py + 38f * u)
        val hf = player.hp.toFloat() / max(1, player.maxHp)
        Ui.bar(c, tmpR, hf, hpColor(hf))
        Ui.text(c, "${player.hp} / ${player.maxHp}", tmpR.centerX(), tmpR.centerY() + 5.5f * u, 15f, Color.WHITE)
        Ui.icon(c, "icons/pot10.png", bx + 10f * u, py + 54f * u, 26f)
        tmpR.set(bx + 28f * u, py + 44f * u, bx + bw, py + 62f * u)
        Ui.bar(c, tmpR, player.mana / max(1, player.maxMana), Color.parseColor("#2979FF"))
        Ui.text(c, "${player.mana.toInt()} • ${player.currentSpell.nameAr}", tmpR.centerX(), tmpR.centerY() + 5f * u, 12.5f, Color.WHITE)

        Ui.icon(c, "icons/coin.png", px + 24f * u, py + 90f * u, 26f)
        Ui.text(c, "${player.coins}", px + 42f * u, py + 97f * u, 19f, GOLD, Paint.Align.LEFT)
        Ui.icon(c, "icons/gunpowder.png", px + 112f * u, py + 90f * u, 24f)
        Ui.text(c, "${player.gunpowder}", px + 128f * u, py + 97f * u, 17f, Color.parseColor("#FFAB40"), Paint.Align.LEFT)
        Ui.icon(c, "icons/armor_chest.png", px + 186f * u, py + 90f * u, 22f)
        Ui.text(c, "${(player.armorReduction() * 100).toInt()}%", px + 202f * u, py + 97f * u, 16f, Color.parseColor("#80DEEA"), Paint.Align.LEFT)
        var sx = px + pw - 22f * u
        if (player.hasBoat) {
            Ui.icon(c, "icons/boat.png", sx, py + 88f * u, 26f)
            sx -= 32f * u
        }
        if (player.goldKeys > 0) {
            Ui.icon(c, "icons/key_gold.png", sx - 8f * u, py + 88f * u, 24f)
            Ui.text(c, "${player.goldKeys}", sx + 6f * u, py + 94f * u, 14f, GOLD, Paint.Align.LEFT)
            sx -= 42f * u
        }
        if (player.hasPickaxe) {
            Ui.icon(c, "ui/ico/axe.png", sx, py + 88f * u, 24f, Color.parseColor("#FFB74D"))
            sx -= 32f * u
        }
        if (player.hasBossKey) Ui.icon(c, "icons/key.png", sx, py + 88f * u, 28f)

        val rx = px + 14f * u
        val ry = py + 120f * u
        val step = 62f * u
        Ui.icon(c, "icons/wood.png", rx + 8f * u, ry, 21f)
        Ui.text(c, "${player.wood}", rx + 21f * u, ry + 6f * u, 15f, Color.WHITE, Paint.Align.LEFT)
        Ui.icon(c, "icons/stone.png", rx + step + 8f * u, ry, 21f)
        Ui.text(c, "${player.stone}", rx + step + 21f * u, ry + 6f * u, 15f, Color.WHITE, Paint.Align.LEFT)
        Ui.icon(c, "icons/iron.png", rx + step * 2f + 8f * u, ry, 21f)
        Ui.text(c, "${player.iron}", rx + step * 2f + 21f * u, ry + 6f * u, 15f, Color.WHITE, Paint.Align.LEFT)
        Ui.icon(c, "icons/wool.png", rx + step * 3f + 8f * u, ry, 21f)
        Ui.text(c, "${player.wool}", rx + step * 3f + 21f * u, ry + 6f * u, 15f, Color.WHITE, Paint.Align.LEFT)
        Ui.icon(c, "icons/bomb.png", rx + step * 4f + 8f * u, ry, 21f)
        Ui.text(c, "${player.bombs}", rx + step * 4f + 21f * u, ry + 6f * u, 15f, Color.WHITE, Paint.Align.LEFT)
        Ui.icon(c, "icons/pot1.png", rx + step * 5f + 8f * u, ry, 21f)
        Ui.text(c, "${player.potions}", rx + step * 5f + 21f * u, ry + 6f * u, 15f, Color.WHITE, Paint.Align.LEFT)

        if (anyMats) {
            var mx2 = px + 22f * u
            for (i in 0 until 6) {
                if (player.mats[i] <= 0) continue
                Ui.iconTinted(c, Mats.paths[i], mx2, py + 150f * u, 24f, Mats.mul[i])
                Ui.text(c, "${player.mats[i]}", mx2 + 15f * u, py + 156f * u, 15f, Color.WHITE, Paint.Align.LEFT)
                mx2 += 58f * u
            }
        }
    }

    private fun drawControls(c: Canvas) {
        val u = Ui.u
        joystick.draw(c)
        attackBtn.draw(c)
        weaponSwitchBtn.draw(c)
        farmPlantBtn.draw(c)
        turboWallBtn.draw(c)
        editBlockBtn.draw(c)
        parryBtn.draw(c)
        grappleBtn.draw(c)
        buildBtn.draw(c)
        dashBtn.draw(c)
        magicBtn.draw(c)
        spellSwitchBtn.draw(c)
        slowBtn.draw(c)
        bombBtn.draw(c)
        potionBtn.draw(c)
        interactBtn.draw(c)
        eatBtn.draw(c)
        bagBtn.draw(c)
        if (ow != null && !inCave) drawMinecraftHotbar(c)
        Ui.circle(c, pauseCx, pauseCy, pauseR, Color.argb(200, 14, 18, 32))
        Ui.ring(c, pauseCx, pauseCy, pauseR - 1.5f * u, Color.parseColor("#B0BEC5"), 3f * u)
        Ui.icon(c, "ui/ico/pause.png", pauseCx, pauseCy, 26f, Color.WHITE)
    }

    private fun drawMinecraftHotbar(c: Canvas) {
        val u = Ui.u
        val first = hotbarRects[0]
        val last = hotbarRects[8]
        val barBg = RectF(first.left - 6f * u, first.top - 6f * u, last.right + 6f * u, last.bottom + 6f * u)
        Ui.fillRound(c, barBg, Color.argb(220, 26, 28, 34), 8f)
        Ui.strokeRound(c, barBg, Color.rgb(90, 94, 108), 2.5f, 8f)

        val slotBmp = Assets.bmp("ui/mc_slot.png")
        val selBmp = Assets.bmp("ui/mc_slot_sel.png")
        for (i in 0 until 9) {
            val r = hotbarRects[i]
            val sel = player.hotbarIdx == i
            val bm = if (sel) selBmp else slotBmp
            if (bm != null) {
                c.drawBitmap(bm, null, r, null)
            } else {
                Ui.fillRound(c, r, if (sel) Color.rgb(160, 145, 95) else Color.rgb(110, 110, 118), 4f)
            }
            val icon = hotbarSlotIcon(i)
            val tint = if (i == 1) Color.parseColor("#FFB74D") else null
            Ui.icon(c, icon, r.centerX(), r.centerY() - 2f * u, 30f, tint)
            val cnt = hotbarSlotCount(i)
            if (cnt >= 0) {
                Ui.text(c, "$cnt", r.right - 5f * u, r.bottom - 5f * u, 12.5f, if (cnt > 0) Color.WHITE else Color.parseColor("#EF9A9A"), Paint.Align.RIGHT)
            }
            Ui.text(c, "${i + 1}", r.left + 5f * u, r.top + 13f * u, 10f, Color.rgb(210, 210, 220), Paint.Align.LEFT)
        }
        Ui.text(c, hotbarSlotTitle(player.hotbarIdx), barBg.centerX(), barBg.top - 8f * u, 14.5f, GOLD)
    }

    private fun objectiveText(): String {
        val b = boss
        if (bossDown && !portal.isActive && era == EraType.ERA_4_ISLAND) return "جد جوهرة المحيط في الصندوق خلف البوابة"
        if (bossDown) return "ادخل بوابة الزمن لتكمل رحلتك"
        if (world.gateOpen) {
            if (era == EraType.ERA_5_CYBER && b != null && b.shielded) return "عطّل المولدات لتكسر درع القائد"
            return "اهزم ${bossName(era)}"
        }
        return when (era) {
            EraType.ERA_4_ISLAND -> when {
                !player.hasBossKey && !player.hasPickaxe -> "اصنع فأساً من طاولة الصناعة (3 خشب، 2 حجر، 2 حديد)"
                !player.hasBossKey -> "احفر الكنز المدفون لتجد المفتاح"
                else -> "افتح البوابة المقفلة"
            }
            EraType.ERA_6_VOID -> {
                var left = 0
                val gateX = TileMap.GATE_COL * TileMap.TS
                for (en in enemies) if (!en.isDead && en !== b && en.x < gateX) left++
                "اهزم أشباه الفارس لفتح الحاجز ($left)"
            }
            EraType.ERA_5_CYBER -> if (!player.hasBossKey) "اهزم حامل المفتاح وعطّل المولدات" else "افتح البوابة المقفلة"
            else -> if (!player.hasBossKey) "اهزم الحارس حامل المفتاح (رمز المفتاح فوقه)" else "افتح البوابة المقفلة"
        }
    }

    private fun drawToast(c: Canvas) {
        if (toastTimer <= 0f || toastText.isEmpty()) return
        val u = Ui.u
        val a = min(1f, toastTimer / 0.4f)
        val tw = min(Ui.measure(toastText, 19f) + 50f * u, curW * 0.8f)
        val cx = curW / 2f
        val cy = curH - 78f * u
        tmpR.set(cx - tw / 2f, cy - 22f * u, cx + tw / 2f, cy + 22f * u)
        Ui.fillRound(c, tmpR, Color.argb((220 * a).toInt(), 14, 18, 34), 20f)
        Ui.strokeRound(c, tmpR, Color.argb((255 * a).toInt(), 255, 213, 79), 2f, 20f)
        Ui.text(c, toastText, cx, cy + 7f * u, 18f, Color.argb((255 * a).toInt(), 255, 255, 255))
    }

    // ---- modal frames
    private fun modalRect(): RectF {
        val w = curW.toFloat()
        val h = curH.toFloat()
        return RectF(w * 0.06f, h * 0.05f, w * 0.94f, h * 0.93f)
    }

    private fun dim(c: Canvas) {
        Ui.fillRect(c, 0f, 0f, curW.toFloat(), curH.toFloat(), Color.argb(175, 0, 0, 0))
    }

    private fun closeButton(c: Canvas, mr: RectF) {
        val u = Ui.u
        val r = RectF(mr.left + 14f * u, mr.top + 12f * u, mr.left + 62f * u, mr.top + 60f * u)
        Ui.button(c, r, "", Color.parseColor("#C62828"), true, 18f)
        Ui.icon(c, "ui/ico/x.png", r.centerX(), r.centerY(), 26f, Color.WHITE)
        hits.add(Hit(RectF(r.left - 6f * u, r.top - 6f * u, r.right + 6f * u, r.bottom + 6f * u), "close"))
    }

    private fun drawList(c: Canvas, shop: Boolean) {
        val u = Ui.u
        dim(c)
        val mr = modalRect()
        Ui.panel(c, mr, GOLD, Color.argb(246, 18, 22, 40), 16f)
        closeButton(c, mr)
        val chip = RectF(mr.left + 74f * u, mr.top + 14f * u, mr.left + 74f * u + 150f * u, mr.top + 58f * u)
        Ui.fillRound(c, chip, Color.argb(220, 8, 10, 20), 12f)
        Ui.strokeRound(c, chip, GOLD, 2f, 12f)
        Ui.icon(c, "icons/coin.png", chip.left + 24f * u, chip.centerY(), 28f)
        Ui.text(c, "${player.coins}", chip.left + 44f * u, chip.centerY() + 8f * u, 22f, GOLD, Paint.Align.LEFT)
        Ui.text(c, if (shop) "متجر إلريك" else "طاولة الصناعة والبلوكات", mr.right - 24f * u, mr.top + 46f * u, 30f, GOLD, Paint.Align.RIGHT)

        var listTop = mr.top + 78f * u
        if (shop) {
            val names = listOf("أسلحة", "دروع", "مستهلكات", "ترقيات")
            var tx = mr.right - 20f * u
            for (i in names.indices) {
                val r = RectF(tx - 150f * u, listTop, tx, listTop + 42f * u)
                val sel = i == shopTab
                Ui.button(c, r, names[i], if (sel) Color.parseColor("#F9A825") else Color.parseColor("#37474F"), true, 18f, sel)
                hits.add(Hit(RectF(r), "tab:$i"))
                tx -= 160f * u
            }
            listTop += 54f * u
        }
        listRect = RectF(mr.left + 16f * u, listTop, mr.right - 16f * u, mr.bottom - 16f * u)

        val rowH = (if (shop) 98f else 150f) * u
        val gap = 10f * u
        val shopItems: List<ShopItem> = if (shop) {
            val type = listOf(ItemType.WEAPON, ItemType.ARMOR, ItemType.CONSUMABLE, ItemType.UPGRADE)[shopTab]
            GameCatalogue.shopItems.filter { it.type == type }
        } else emptyList()
        val count = if (shop) shopItems.size else CraftingRegistry.recipes.size
        val contentH = count * (rowH + gap)
        maxScroll = max(0f, contentH - listRect.height())
        scrollY = scrollY.coerceIn(0f, maxScroll)

        c.save()
        c.clipRect(listRect)
        c.translate(0f, -scrollY)
        for (i in 0 until count) {
            val top = listRect.top + i * (rowH + gap)
            val card = RectF(listRect.left, top, listRect.right, top + rowH)
            if (shop) drawShopCard(c, shopItems[i], card) else drawCraftCard(c, CraftingRegistry.recipes[i], card)
        }
        c.restore()
        if (maxScroll > 0f) {
            val th = listRect.height() * listRect.height() / (listRect.height() + maxScroll)
            val ty = listRect.top + (listRect.height() - th) * (scrollY / maxScroll)
            tmpR.set(listRect.right - 6f * u, ty, listRect.right - 2f * u, ty + th)
            Ui.fillRound(c, tmpR, Color.argb(170, 255, 213, 79), 3f)
        }
    }

    private fun cardBase(c: Canvas, card: RectF, selected: Boolean, accent: Int) {
        val u = Ui.u
        Ui.fillRound(c, card, if (selected) Color.argb(235, 40, 50, 84) else Color.argb(225, 28, 34, 58), 12f)
        Ui.strokeRound(c, card, if (selected) GOLD else Color.argb(160, 90, 100, 140), if (selected) 3f else 1.5f, 12f)
        val box = RectF(card.right - 84f * u, card.top + 12f * u, card.right - 12f * u, card.top + 84f * u)
        Ui.fillRound(c, box, Color.argb(235, 10, 12, 24), 10f)
        Ui.strokeRound(c, box, accent, 2.5f, 10f)
    }

    private fun addCardHits(card: RectF, btn: RectF, key: String) {
        val sr = RectF(card.left, card.top - scrollY, card.right, card.bottom - scrollY)
        val br = RectF(btn.left, btn.top - scrollY, btn.right, btn.bottom - scrollY)
        if (!RectF.intersects(sr, listRect)) return
        hits.add(Hit(sr, "sel:$key"))
        hits.add(Hit(br, "buy:$key"))
    }

    private fun drawShopCard(c: Canvas, item: ShopItem, card: RectF) {
        val u = Ui.u
        val selected = selectedId == item.id
        val accent = when (item.type) {
            ItemType.WEAPON -> Color.parseColor("#FF7043")
            ItemType.ARMOR -> Color.parseColor("#4DD0E1")
            ItemType.CONSUMABLE -> Color.parseColor("#66BB6A")
            ItemType.UPGRADE -> Color.parseColor("#AB47BC")
        }
        cardBase(c, card, selected, accent)
        val boxLeft = card.right - 84f * u
        Ui.icon(c, item.iconAsset, boxLeft + 36f * u, card.top + 48f * u, 54f, item.tint)
        Ui.text(c, item.titleAr, boxLeft - 14f * u, card.top + 34f * u, 21f, Color.WHITE, Paint.Align.RIGHT)
        var desc = item.descAr
        if (item.type == ItemType.UPGRADE) desc += "  (${upgCount(item.id)}/${item.maxCount})"
        if (item.id == "potion_heal") desc += "  عندك: ${player.potions}"
        if (item.id == "bombs") desc += "  عندك: ${player.bombs}"
        if (item.id == "key_gold") desc += "  عندك: ${player.goldKeys}"
        if (stockLimit(item) > 0) desc += "  المتبقي: ${max(0, stockLeft(item))}"
        val btn = RectF(card.left + 14f * u, card.centerY() - 26f * u, card.left + 164f * u, card.centerY() + 26f * u)
        Ui.wrapped(c, desc, btn.right + 14f * u, card.top + 44f * u, boxLeft - 14f * u - (btn.right + 14f * u), 15f, Color.rgb(200, 208, 230), false, 2)
        val st = shopState(item)
        val price = priceOf(item)
        when {
            st == 1 -> {
                val label = if (item.type == ItemType.WEAPON && player.weapon.id == item.id) "مجهّز ✓" else "مملوك ✓"
                Ui.button(c, btn, label, Color.parseColor("#5D4037"), false, 18f)
            }
            st == 3 -> Ui.button(c, btn, "نفد اليوم", Color.parseColor("#455A64"), false, 18f)
            selected && st == 0 -> Ui.button(c, btn, "تأكيد الشراء", Color.parseColor("#2E7D32"), true, 17f)
            st == 0 -> {
                Ui.button(c, btn, "    $price", Color.parseColor("#F9A825"), true, 20f)
                Ui.icon(c, "icons/coin.png", btn.left + 28f * u, btn.centerY(), 26f)
            }
            else -> {
                Ui.button(c, btn, "    $price", Color.parseColor("#B71C1C"), false, 20f)
                Ui.icon(c, "icons/coin.png", btn.left + 28f * u, btn.centerY(), 26f, null, 150)
            }
        }
        addCardHits(card, btn, item.id)
    }

    private fun drawCraftCard(c: Canvas, r: CraftRecipe, card: RectF) {
        val u = Ui.u
        val selected = selectedId == r.id
        cardBase(c, card, selected, Color.parseColor("#FFB74D"))
        val boxLeft = card.right - 84f * u
        Ui.icon(c, r.iconAsset, boxLeft + 36f * u, card.top + 48f * u, 54f, r.tint)
        var title = r.titleAr
        if (r.resultType == "UPG_WEAPON") title += "  (${player.weaponLvl}/${CraftingRegistry.MAX_UPG})"
        if (r.resultType == "UPG_ARMOR") title += "  (${player.armorLvl}/${CraftingRegistry.MAX_UPG})"
        Ui.text(c, title, boxLeft - 14f * u, card.top + 32f * u, 21f, Color.WHITE, Paint.Align.RIGHT)
        val btn = RectF(card.left + 14f * u, card.centerY() - 26f * u, card.left + 164f * u, card.centerY() + 26f * u)
        Ui.wrapped(c, r.descAr, btn.right + 14f * u, card.top + 42f * u, boxLeft - 14f * u - (btn.right + 14f * u), 15f, Color.rgb(200, 208, 230), false, 2)
        val cost = costOf(r)
        val chipW = 96f * u
        var slot = 0
        fun chip(path: String, mul: Int, have: Int, need: Int) {
            if (need <= 0) return
            val row = slot / 4
            val col = slot % 4
            val cx = boxLeft - 14f * u - col * chipW
            val cy = card.bottom - 60f * u + row * 32f * u
            val ok = have >= need
            if (mul == 0) Ui.icon(c, path, cx - 12f * u, cy, 24f) else Ui.iconTinted(c, path, cx - 12f * u, cy, 24f, mul)
            Ui.text(c, "$have/$need", cx - 28f * u, cy + 6f * u, 16f, if (ok) Color.parseColor("#A5D6A7") else Color.parseColor("#EF9A9A"), Paint.Align.RIGHT)
            slot++
        }
        chip("icons/wood.png", 0, player.wood, cost.wood)
        chip("icons/stone.png", 0, player.stone, cost.stone)
        chip("icons/iron.png", 0, player.iron, cost.iron)
        chip("icons/wool.png", 0, player.wool, cost.wool)
        chip("icons/coin.png", 0, player.coins, cost.gold)
        for (i in 0 until 6) chip(Mats.paths[i], Mats.mul[i], player.mats[i], cost.mats[i])
        val st = craftState(r)
        when {
            st == 1 -> Ui.button(c, btn, "مملوك ✓", Color.parseColor("#5D4037"), false, 17f)
            selected && st == 0 -> Ui.button(c, btn, "تأكيد الصنع", Color.parseColor("#2E7D32"), true, 17f)
            st == 0 -> Ui.button(c, btn, "اصنع", Color.parseColor("#F9A825"), true, 20f)
            else -> Ui.button(c, btn, "ناقص", Color.parseColor("#B71C1C"), false, 18f)
        }
        addCardHits(card, btn, r.id)
    }

    // ---------------------------------------------------------------- Minecraft-Style Grid Inventory Modal (الحقيبة الشبكية بستايل ماين كرافت)
    private data class McSlotItem(
        val id: String,
        val title: String,
        val desc: String,
        val icon: String,
        val count: Int,
        val actionLabel: String,
        val canAct: Boolean,
        val hotbarTarget: Int = -1
    )

    private fun buildMcInventorySlots(): List<McSlotItem> {
        val list = ArrayList<McSlotItem>()
        list.add(McSlotItem("blk_wood", "بلوك ألواح خشبية", "جدار خشبي لبناء الحماية السريعة والمنازل (الخانة 3 بالشريط السريع).", "icons/block_wood.png", player.blkWood, "اختر للبناء", true, 2))
        list.add(McSlotItem("blk_stone", "بلوك طوب حجري", "جدار حجري مضاد لحصار الزومبي وقذائف الزعيم (الخانة 4).", "icons/block_stone_brick.png", player.blkStone, "اختر للبناء", true, 3))
        list.add(McSlotItem("blk_window", "جدار نافذة قتالية (فورتنايت)", "يمنع الزومبي ومقذوفاتهم ويسمح بمرور سحرك وضرباتك (الخانة 5).", "icons/block_window.png", player.blkWindow, "اختر للبناء", true, 4))
        list.add(McSlotItem("blk_bridge", "جسر وأرضية خشبية", "يبنى فوق الأرض أو فوق الماء كجسر (الخانة 6).", "icons/block_bridge.png", player.blkBridge, "اختر للبناء", true, 5))
        list.add(McSlotItem("blk_launchpad", "منصة القفز الهوائي", "تقذفك في الهواء فوق الأسوار والزومبي مع ضربة هبطة ساحقة (الخانة 7).", "icons/block_launchpad.png", player.blkLaunchPad, "اختر للبناء", true, 6))
        list.add(McSlotItem("blk_irongate", "بوابة قلعة حديدية مصفحة", "بوابة حديدية لا يكسرها الزومبي المفجر وتمر منها أنت فقط!", "icons/iron_gate.png", player.blkIronGate, "ضع البوابة هنا", player.blkIronGate > 0 && ow != null && !inCave))
        list.add(McSlotItem("blk_spikes", "فخ الأشواك الفولاذية", "يمزق الزومبي الذين يدوسون عليه حول قلعتك (الخانة 6 بالشريط السريع).", "icons/block_spikes.png", player.blkSpikes, "اختر للبناء", true, 5))
        list.add(McSlotItem("blk_obsidian", "حجر السبج المدرع", "لا ينكسر أبداً بأي انفجار أو زومبي مفجر حتى بسمك 1 (الخانة 7).", "icons/block_obsidian.png", player.blkObsidian, "اختر للبناء", true, 6))
        list.add(McSlotItem("blk_wire", "سلك شائك دفاعي", "يبطئ الزومبي بنسبة 65% حول أسوارك ومزارعك (الخانة 8).", "icons/block_wire.png", player.blkWire, "اختر للبناء", true, 7))
        list.add(McSlotItem("blk_beacon", "منارة الشفاء البلورية", "تشفي الفارس والغولم الحديدي تلقائياً في محيطها!", "icons/block_beacon.png", player.blkBeacon, "ضع المنارة هنا ✨", player.blkBeacon > 0 && ow != null && !inCave))
        list.add(McSlotItem("blk_royal", "أرضية رخام القلعة الملكية", "تمنحك +25% سرعة حركة عند السير عليها!", "icons/block_royal_floor.png", player.blkRoyalFloor, "ضع الأرضية 👑", player.blkRoyalFloor > 0 && ow != null && !inCave))
        list.add(McSlotItem("place_golem", "الغولم الحديدي الحارس 🤖", "حارس آلي مدرع يحمي قلعتك ومزارعك ويسحق الزومبي المفجر!", "icons/iron_golem.png", player.golemsItem, "انشر الغولم 🤖", player.golemsItem > 0 && ow != null && !inCave))
        list.add(McSlotItem("use_wcan", "دلو الري المائي 💧", "يروي المحاصيل والأشجار في مساحة 3×3 لتنضج فوراً!", "icons/watering_can.png", if (player.hasWateringCan) 1 else 0, "اروِ المزرعة 💧", player.hasWateringCan && ow != null && !inCave))
        list.add(McSlotItem("switch_brush", "نمط هندسة البناء: ${when(player.buildBrushMode){1->"سور خطي (5)";2->"غرفة كاملة (5×5)";else->"بلوك فردي (1)"}}", "بدّل بين وضع بلوك واحد، أو سور خطي (5 بلوكات)، أو غرفة مربعة 5×5 بضغطة واحدة!", "icons/blueprint_fort.png", player.buildBrushMode + 1, "تبديل نمط البناء 📐", true))
        list.add(McSlotItem("blk_door", "باب منزل خشبي", "باب يفتح ويغلق بالضغط عليه ويمنع الوحوش.", "tiles/door.png", player.blkDoor, "ضع الباب هنا", player.blkDoor > 0 && ow != null && !inCave))
        list.add(McSlotItem("blk_tnt", "بلوك متفجرات TNT", "ينفجر بقوة هائلة ويكسر درع الزعيم العملاق (الخانة 9).", "icons/block_tnt.png", player.blkTnt, "اختر للبناء", true, 8))
        list.add(McSlotItem("abyss_seals", "أختام الهاوية السبعة", "اجمع 7 أختام لإضعاف واستدعاء الزعيم الأسطوري العملاق (الجاهزية: ${player.readinessPercent()}%).", "icons/abyss_seal.png", player.abyssalSeals, "مسار الزعيم 💀", true))
        list.add(McSlotItem("conv_wood", "خشب خام", "اضغط لتحويل 1 خشب خام إلى 4 بلوكات ألواح خشبية للبناء!", "icons/wood.png", player.wood, "تحويل لبلوكات", player.wood >= 1))
        list.add(McSlotItem("conv_stone", "حجر خام", "اضغط لتحويل 1 حجر خام إلى 4 بلوكات طوب حجري للبناء!", "icons/stone.png", player.stone, "تحويل لبلوكات", player.stone >= 1))
        list.add(McSlotItem("plant_sapling", "شتلة شجر (زراعة يدوية)", "الشجر لا ينمو وحده! ازرع الشتلة بيدك لتنمو شجرة كاملة تعطيك خشباً وتفاحاً.", "icons/sapling.png", player.treeSaplings, "ازرع شتلة 🌱", player.treeSaplings > 0 && ow != null && !inCave))
        list.add(McSlotItem("plant_seeds", "بذور محاصيل القمح", "ازرعها في مزرعتك واحصدها بعد نضوجها للحصول على القمح والخبز!", "icons/seeds.png", player.cropSeeds, "ازرع بذرة 🌾", player.cropSeeds > 0 && ow != null && !inCave))
        list.add(McSlotItem("eat_wheat", "قمح وخبز الحصاد", "محصول زراعي طازج يملأ الشبع +55 ويستعيد +20 صحة!", "icons/wheat.png", player.wheat, "تناول الخبز", player.wheat > 0))
        list.add(McSlotItem("use_powder", "بارود المدافع والمتفجرات", "يُصنع في الكرافتينج 4×4 أو يجمع من الكهوف لتعبئة وتشغيل المدافع!", "icons/gunpowder.png", player.gunpowder, "تعبئة أقرب مدفع", player.gunpowder > 0 && ow != null))
        list.add(McSlotItem("place_storage", "شونة وصندوق تخزين", "ضعه في بيتك لتفريغ الشنطة وتخزين كل مواردك بأمان!", "icons/storage_chest.png", player.storageChestsItem, "ضع الشونة هنا 📦", player.storageChestsItem > 0 && ow != null && !inCave))
        list.add(McSlotItem("use_house_bp", "مخطط بيت ريفي جاهز", "يبني بيتاً كاملاً (جدران، باب، مشاعل) في موقعك بضغطة واحدة!", "icons/blueprint_house.png", player.houseBlueprints, "بناء البيت فوراً 🏠", player.houseBlueprints > 0 && ow != null && !inCave))
        list.add(McSlotItem("use_fort_bp", "مخطط قلعة سمك 2 حجر", "يبني حصناً حجرياً بسمك 2 حجر مضاداً للزومبي المفجر الليلي!", "icons/blueprint_fort.png", player.fortBlueprints, "بناء القلعة 🛡️", player.fortBlueprints > 0 && ow != null && !inCave))
        list.add(McSlotItem("use_tower_bp", "مخطط ستركشر: برج القنص والمدفعية 🏰", "يبني برج مراقبة حجري سمك 2 بنوافذ قنص فورتنايت ومنارة شفاء!", "icons/blueprint_watchtower.png", player.towerBlueprints, "بناء البرج فوراً 🏰", player.towerBlueprints > 0 && ow != null && !inCave))
        list.add(McSlotItem("use_farm_bp", "مخطط ستركشر: المزرعة الملكية 🌾", "يبني مزرعة مسوّرة ببوابة حديدية و12 محصولاً متنوعاً ومروياً!", "icons/blueprint_farm.png", player.farmBlueprints, "بناء المزرعة 🌾", player.farmBlueprints > 0 && ow != null && !inCave))
        list.add(McSlotItem("raw_iron", "خام الحديد", "يستخدم في الصناعة 4×4 للبارود أو يصهر في الفرن إلى ذهب وصلب.", "icons/iron.png", player.iron, "صهر بالفرن", false))
        list.add(McSlotItem("raw_wool", "صوف الخراف", "يجمع من الخراف لصناعة الأسرة والمراكب.", "icons/wool.png", player.wool, "صناعة سرير", player.wool >= 3 && player.wood >= 4))
        list.add(McSlotItem("eat_stew", "حساء الطاقة الساخن", "وجبة غنية تملأ الشبع +75 وتستعيد +35 صحة فوراً.", "icons/stew.png", player.stew, "تناول الآن", player.stew > 0))
        list.add(McSlotItem("eat_cooked", "لحم مشوي", "يملأ الشبع +45 ويستعيد +12 صحة.", "icons/cooked_meat.png", player.cookedMeat, "تناول الآن", player.cookedMeat > 0))
        list.add(McSlotItem("eat_apple", "تفاح طازج", "يملأ الشبع +16 ويستعيد +5 صحة.", "icons/apple.png", player.apples, "تناول الآن", player.apples > 0))
        list.add(McSlotItem("eat_raw", "لحم نيء", "اطبخه في الفرن مجاناً لتحويله إلى لحم مشوي وحساء!", "icons/raw_meat.png", player.rawMeat, "تناول نيئاً", player.rawMeat > 0))
        list.add(McSlotItem("use_heal", "جرعة الشفاء", "تستعيد +85 صحة فوراً.", "icons/pot1.png", player.potions, "اشرب الجرعة", player.potions > 0 && player.hp < player.maxHp))
        list.add(McSlotItem("use_speed", "إكسير السرعة والرؤية", "يمنح سرعة مضاعفة ورؤية ليلية في الكهوف لمدة 45 ثانية.", "icons/pot_speed.png", player.speedPots, "اشرب الإكسير", player.speedPots > 0))
        list.add(McSlotItem("place_bed", "سرير النوم الميداني", "ضعه في موقعك الحالي للنوم حتى الفجر وحفظ تقدمك.", "icons/bed.png", player.bedsItem, "ضع السرير هنا", player.bedsItem > 0 && ow != null && !inCave))
        list.add(McSlotItem("place_furnace", "فرن الصهر والطبخ", "ضعه في العالم لفتح واجهة الصهر والطبخ في أي مكان!", "icons/furnace.png", player.furnacesItem, "ضع الفرن هنا", player.furnacesItem > 0 && ow != null && !inCave))
        list.add(McSlotItem("place_cannon", "مدفع الحراسة الآلي", "ضعه لحماية قاعدتك؛ يطلق قذائف على الوحوش تلقائياً.", "icons/pirate_cannon.png", player.cannonsItem, "ضع المدفع هنا", player.cannonsItem > 0 && ow != null))
        list.add(McSlotItem("place_camp", "مخيم السفر السريع", "ضعه لإنشاء نقطة سفر سريع دائمة وإضاءة الليل.", "environment/bonefire.png", player.campfiresItem, "ضع المخيم هنا", player.campfiresItem > 0 && ow != null && !inCave))
        list.add(McSlotItem("use_map", "خريطة كنز مدفون", "استخدمها لتحديد موقع كنز مدفون جديد على البوصلة.", "icons/map.png", player.treasureMaps, "كشف موقع الكنز", player.treasureMaps > 0 && ow != null))
        list.add(McSlotItem("gold_keys", "مفتاح الصناديق الذهبي", "يفتح صناديق الحصون والكهوف الذهبية المقفلة.", "icons/key_gold.png", player.goldKeys, "مفتاح جاهز", false))
        for (w in GameCatalogue.weapons) {
            if (player.owned.contains(w.id)) {
                val eq = player.weapon.id == w.id
                list.add(McSlotItem("eq:${w.id}", w.nameAr, "ضرر السلاح: ${w.damage} (مستوى الصقل +${player.weaponLvl}).", w.iconAsset, 1, if (eq) "مجهّز ✓" else "تجهيز السلاح", !eq, 0))
            }
        }
        return list
    }

    private fun drawBagModal(c: Canvas) {
        val u = Ui.u
        dim(c)
        val mr = modalRect()
        // Minecraft-styled stone grey & gold border frame
        Ui.panel(c, mr, Color.rgb(190, 190, 198), Color.argb(248, 38, 40, 48), 14f)
        closeButton(c, mr)
        Ui.text(c, "حقيبة الفارس وصندوق البناء (Minecraft Style)", mr.right - 24f * u, mr.top + 44f * u, 25f, GOLD, Paint.Align.RIGHT)

        val tabs = listOf("شبكة الحقيبة (Grid)", "الصناعة والبلوكات", "السحر والمهام")
        var tx = mr.right - 20f * u
        val tabY = mr.top + 68f * u
        for (i in tabs.indices) {
            val r = RectF(tx - 190f * u, tabY, tx, tabY + 40f * u)
            val sel = i == bagTab
            Ui.button(c, r, tabs[i], if (sel) Color.parseColor("#F9A825") else Color.parseColor("#455A64"), true, 16.5f, sel)
            hits.add(Hit(RectF(r), "btab:$i"))
            tx -= 200f * u
        }

        val contentTop = tabY + 50f * u
        if (bagTab == 0) {
            // Left: Minecraft Character Preview + Equipment Slots + Bag Load + Quick Actions
            val charBox = RectF(mr.left + 18f * u, contentTop, mr.left + 265f * u, mr.bottom - 18f * u)
            Ui.fillRound(c, charBox, Color.rgb(24, 26, 32), 8f)
            Ui.strokeRound(c, charBox, Color.rgb(130, 134, 148), 2.5f, 8f)
            heroSheet.update(0.016f)
            heroSheet.draw(c, charBox.centerX() - 48f * u, charBox.top + 8f * u, 96f * u, 96f * u)

            // 4 Minecraft Armor Slots (Helmet, Chestplate, Leggings, Boots) + Weapon Slot around character!
            val armorSlotIcons = arrayOf("icons/armor_helmet.png", "icons/armor_chest.png", "icons/armor_legs.png", "icons/armor_boots.png")
            val armorTiers = intArrayOf(player.helmetTier, player.chestTier, player.legsTier, player.bootsTier)
            val armorIds = arrayOf("craft_helmet", "craft_chestplate", "craft_leggings", "craft_boots")
            for (ai in 0 until 4) {
                val ay = charBox.top + (8f + ai * 26f) * u
                val ar = RectF(charBox.left + 8f * u, ay, charBox.left + 32f * u, ay + 24f * u)
                val tierCol = when (armorTiers[ai]) {
                    4 -> Color.parseColor("#FF1744")
                    3 -> Color.parseColor("#00E5FF")
                    2 -> Color.parseColor("#FFD54F")
                    1 -> Color.parseColor("#B0BEC5")
                    else -> Color.rgb(75, 78, 88)
                }
                Ui.fillRound(c, ar, Color.rgb(42, 44, 54), 4f)
                Ui.strokeRound(c, ar, tierCol, 1.8f, 4f)
                Ui.icon(c, armorSlotIcons[ai], ar.centerX(), ar.centerY(), 18f)
                Ui.text(c, "+${armorTiers[ai]}", ar.right + 12f * u, ar.bottom - 5f * u, 11f, tierCol)
                hits.add(Hit(ar, "bag_arm_slot:${armorIds[ai]}"))
            }
            val eqWepR = RectF(charBox.right - 56f * u, charBox.top + 14f * u, charBox.right - 10f * u, charBox.top + 60f * u)
            Ui.fillRound(c, eqWepR, Color.rgb(55, 58, 70), 6f)
            Ui.strokeRound(c, eqWepR, player.weapon.color, 2f, 6f)
            Ui.icon(c, player.weapon.iconAsset, eqWepR.centerX(), eqWepR.centerY(), 32f)
            hits.add(Hit(eqWepR, "bag_sw_wep"))

            Ui.text(c, "${player.weapon.nameAr} (+${player.weaponLvl})", charBox.centerX(), charBox.top + 118f * u, 14.5f, GOLD)
            val armName = GameCatalogue.armorById(player.armorId)?.nameAr ?: "درع الفارس الأساسي"
            Ui.text(c, "$armName (+${player.armorLvl})", charBox.centerX(), charBox.top + 138f * u, 13f, Color.parseColor("#80DEEA"))
            Ui.text(c, "الصحة: ${player.hp}/${player.maxHp} • البارود: ${player.gunpowder}", charBox.centerX(), charBox.top + 158f * u, 13f, Color.parseColor("#69F0AE"))
            Ui.text(c, "الشنطة: ${player.bagUsed()}/${player.bagMax} • الذهب: ${player.coins}", charBox.centerX(), charBox.top + 178f * u, 13f, Color.parseColor("#FFD54F"))

            // Quick Action Buttons in Character Panel: Switch Weapon, Empty Bag to Storage Silo, 4x4 Craft, Furnace
            var by = charBox.bottom - 184f * u
            val rSwWep = RectF(charBox.left + 12f * u, by, charBox.right - 12f * u, by + 38f * u)
            Ui.button(c, rSwWep, "تبديل السلاح السريع ⚔️", Color.parseColor("#0277BD"), true, 14.5f)
            hits.add(Hit(rSwWep, "bag_sw_wep"))
            by += 44f * u
            val rEmpty = RectF(charBox.left + 12f * u, by, charBox.right - 12f * u, by + 38f * u)
            Ui.button(c, rEmpty, "تفريغ الشنطة في الشونة 📦", Color.parseColor("#2E7D32"), true, 14.5f)
            hits.add(Hit(rEmpty, "open_storage_ui"))
            by += 44f * u
            val btnCraft = RectF(charBox.left + 12f * u, by, charBox.right - 12f * u, by + 38f * u)
            Ui.button(c, btnCraft, "كرافتينج تيبل (4×4) ⚒️", Color.parseColor("#F57C00"), true, 14.5f)
            hits.add(Hit(btnCraft, "open_craft4x4_ui"))
            by += 44f * u
            val btnFurn = RectF(charBox.left + 12f * u, by, charBox.right - 12f * u, by + 38f * u)
            Ui.button(c, btnFurn, "فرن الصهر والطبخ 🔥", Color.parseColor("#D84315"), true, 14.5f)
            hits.add(Hit(btnFurn, "open_furnace_ui"))

            // Right: Category Filter Strip + 7x4 Grid of Beveled Minecraft Slots + Bottom Selected Item Bar
            val gridLeft = charBox.right + 16f * u
            val gridRight = mr.right - 18f * u
            val fLabels = arrayOf("الكل", "بلوكات وستركشرات 🧱", "أسلحة ⚔️", "زراعة وبارود 🌾", "طعام وجرعات 🍖")
            var fxChip = gridRight
            val chipY = contentTop - 4f * u
            for (fi in fLabels.indices) {
                val fw = if (fi == 0) 64f * u else if (fi == 2) 86f * u else 132f * u
                val fr = RectF(fxChip - fw, chipY, fxChip, chipY + 28f * u)
                val isF = bagFilterCat == fi
                Ui.button(c, fr, fLabels[fi], if (isF) Color.parseColor("#FFB300") else Color.parseColor("#37474F"), true, 12f, isF)
                hits.add(Hit(fr, "bag_fcat:$fi"))
                fxChip -= (fw + 6f * u)
            }
            val mapQuickBtn = RectF(gridLeft, chipY, fxChip - 4f * u, chipY + 28f * u)
            if (mapQuickBtn.width() > 75f * u) {
                Ui.button(c, mapQuickBtn, "خريطة الستركشرات 🗺️", Color.parseColor("#00838F"), true, 12f)
                hits.add(Hit(mapQuickBtn, "open_world_map_ui"))
            }

            val cols = 7
            val rows = 4
            val gap = 7f * u
            val gridStartTop = contentTop + 30f * u
            val slotW = min(64f * u, (gridRight - gridLeft - (cols - 1) * gap) / cols)
            val allSlots = buildMcInventorySlots()
            val slots = when (bagFilterCat) {
                1 -> allSlots.filter { it.id.startsWith("blk_") || it.id.endsWith("_bp") || it.id == "switch_brush" || it.id == "conv_wood" || it.id == "conv_stone" || it.id == "place_storage" }
                2 -> allSlots.filter { it.id.startsWith("eq:") || it.id == "abyss_seals" || it.id == "gold_keys" }
                3 -> allSlots.filter { it.id.contains("seed") || it.id.contains("sapling") || it.id.contains("powder") || it.id.contains("wcan") || it.id.contains("golem") || it.id.contains("cannon") || it.id.contains("wheat") }
                4 -> allSlots.filter { it.id.startsWith("eat_") || it.id.startsWith("use_heal") || it.id.startsWith("use_speed") || it.id.startsWith("place_bed") || it.id.startsWith("place_furnace") }
                else -> allSlots
            }
            if (gridSelIdx >= slots.size) gridSelIdx = 0
            val slotBmp = Assets.bmp("ui/mc_slot.png")
            val selBmp = Assets.bmp("ui/mc_slot_sel.png")

            for (idx in 0 until min(cols * rows, slots.size)) {
                val r = idx / cols
                val col = idx % cols
                val sx = gridLeft + col * (slotW + gap)
                val sy = gridStartTop + r * (slotW + gap)
                val sr = RectF(sx, sy, sx + slotW, sy + slotW)
                val isSel = idx == gridSelIdx
                val bm = if (isSel) selBmp else slotBmp
                if (bm != null) c.drawBitmap(bm, null, sr, null)
                else Ui.fillRound(c, sr, if (isSel) Color.rgb(170, 150, 90) else Color.rgb(110, 112, 120), 6f)

                val it = slots[idx]
                Ui.icon(c, it.icon, sr.centerX(), sr.centerY() - 3f * u, 38f)
                Ui.text(
                    c, "${it.count}", sr.right - 6f * u, sr.bottom - 6f * u, 14f,
                    if (it.count > 0) Color.WHITE else Color.parseColor("#EF9A9A"), Paint.Align.RIGHT
                )
                hits.add(Hit(sr, "gslot:$idx"))
            }

            // Selected Item Inspector Bar at bottom-right
            val selItem = slots.getOrNull(gridSelIdx)
            if (selItem != null) {
                val inspTop = gridStartTop + rows * (slotW + gap) + 6f * u
                val inspR = RectF(gridLeft, inspTop, gridRight, mr.bottom - 18f * u)
                Ui.fillRound(c, inspR, Color.rgb(26, 28, 36), 10f)
                Ui.strokeRound(c, inspR, GOLD, 2f, 10f)
                Ui.icon(c, selItem.icon, inspR.right - 44f * u, inspR.centerY(), 46f)
                Ui.text(c, "${selItem.title} (العدد: ${selItem.count})", inspR.right - 82f * u, inspR.top + 30f * u, 19f, GOLD, Paint.Align.RIGHT)
                Ui.wrapped(c, selItem.desc, inspR.left + 170f * u, inspR.top + 38f * u, inspR.width() - 260f * u, 14f, Color.WHITE, false, 2)
                val actBtn = RectF(inspR.left + 10f * u, inspR.centerY() - 24f * u, inspR.left + 148f * u, inspR.centerY() + 24f * u)
                Ui.button(c, actBtn, selItem.actionLabel, Color.parseColor("#2E7D32"), selItem.canAct, 15.5f)
                if (selItem.canAct) hits.add(Hit(actBtn, "gact:${selItem.id}"))
                val dropBtn = RectF(inspR.left + 154f * u, inspR.centerY() - 24f * u, inspR.left + 258f * u, inspR.centerY() + 24f * u)
                val canDrop = selItem.count > 0 && !selItem.id.startsWith("eq:") && selItem.id != "abyss_seals"
                Ui.button(c, dropBtn, "تفريغ/رمي 🗑️", Color.parseColor("#C62828"), canDrop, 14f)
                if (canDrop) hits.add(Hit(dropBtn, "gdrop:${selItem.id}"))
            }
            return
        }

        if (bagTab == 1) {
            drawCraft4x4Modal(c)
            return
        }
        if (bagTab == 99) {
            listRect = RectF(mr.left + 16f * u, contentTop, mr.right - 16f * u, mr.bottom - 16f * u)
            val rowH = 146f * u
            val gap = 10f * u
            val count = CraftingRegistry.recipes.size
            val contentH = count * (rowH + gap)
            maxScroll = max(0f, contentH - listRect.height())
            scrollY = scrollY.coerceIn(0f, maxScroll)
            c.save()
            c.clipRect(listRect)
            c.translate(0f, -scrollY)
            for (i in 0 until count) {
                val top = listRect.top + i * (rowH + gap)
                val card = RectF(listRect.left, top, listRect.right, top + rowH)
                drawCraftCard(c, CraftingRegistry.recipes[i], card)
            }
            c.restore()
            return
        }

        // Tab 2: Spells & Quests
        listRect = RectF(mr.left + 16f * u, contentTop, mr.right - 16f * u, mr.bottom - 16f * u)
        data class BagEntry(val id: String, val title: String, val desc: String, val icon: String, val btnLabel: String, val enabled: Boolean, val accent: Int)
        val entries = ArrayList<BagEntry>()
        for ((idx, sp) in SpellType.values().withIndex()) {
            val active = player.spellIdx == idx
            entries.add(BagEntry("spell:$idx", "تعويذة: ${sp.nameAr}", "${sp.descAr} (طاقة: ${sp.manaCost.toInt()})", sp.iconAsset, if (active) "مفعّلة ✓" else "تفعيل", !active, sp.color))
        }
        for (q in quests) {
            val status = when {
                q.claimed -> "تم استلام المكافأة ✓"
                q.isComplete -> "جاهزة للاستلام! (+${q.rewardGold} ذهب)"
                else -> "التقدم: ${q.progress} / ${q.targetCount}  (المكافأة: ${q.rewardGold} ذهب)"
            }
            entries.add(BagEntry("quest:${q.id}", "مهمة: ${q.titleAr}", "${q.descAr} — $status", q.iconAsset, if (q.isComplete && !q.claimed) "استلم" else "${q.progress}/${q.targetCount}", q.isComplete && !q.claimed, Color.parseColor("#AB47BC")))
        }

        val rowH = 92f * u
        val gap = 10f * u
        val contentH = entries.size * (rowH + gap)
        maxScroll = max(0f, contentH - listRect.height())
        scrollY = scrollY.coerceIn(0f, maxScroll)

        c.save()
        c.clipRect(listRect)
        c.translate(0f, -scrollY)
        for ((i, e) in entries.withIndex()) {
            val top = listRect.top + i * (rowH + gap)
            val card = RectF(listRect.left, top, listRect.right, top + rowH)
            cardBase(c, card, false, e.accent)
            val boxLeft = card.right - 84f * u
            Ui.icon(c, e.icon, boxLeft + 36f * u, card.top + 46f * u, 48f)
            Ui.text(c, e.title, boxLeft - 14f * u, card.top + 32f * u, 20f, Color.WHITE, Paint.Align.RIGHT)
            val btn = RectF(card.left + 14f * u, card.centerY() - 24f * u, card.left + 154f * u, card.centerY() + 24f * u)
            Ui.wrapped(c, e.desc, btn.right + 14f * u, card.top + 40f * u, boxLeft - 14f * u - (btn.right + 14f * u), 14.5f, Color.rgb(200, 208, 230), false, 2)
            Ui.button(c, btn, e.btnLabel, e.accent, e.enabled, 17f)
            val br = RectF(btn.left, btn.top - scrollY, btn.right, btn.bottom - scrollY)
            if (RectF.intersects(br, listRect) && e.enabled) hits.add(Hit(br, e.id))
        }
        c.restore()
    }

    private fun tapBag(id: String?) {
        if (id == null) return
        if (id == "close") {
            Sound.play("click")
            modal = Modal.NONE
            return
        }
        if (id.startsWith("bag_fcat:")) {
            bagFilterCat = id.substring(9).toIntOrNull() ?: 0
            gridSelIdx = 0
            Sound.play("click")
            return
        }
        if (id == "open_world_map_ui") {
            Sound.play("click")
            scrollY = 0f
            modal = Modal.WORLD_MAP
            return
        }
        if (id == "open_furnace_ui") {
            Sound.play("click")
            scrollY = 0f
            modal = Modal.FURNACE
            return
        }
        if (id == "open_storage_ui") {
            Sound.play("click")
            scrollY = 0f
            modal = Modal.STORAGE
            return
        }
        if (id == "open_craft4x4_ui") {
            openCraft()
            return
        }
        if (id == "bag_sw_wep") {
            val nw = player.switchNextWeapon()
            Sound.play("click")
            toast("جهّزت السلاح: ${nw.nameAr} (ضرر ${nw.damage})")
            return
        }
        if (id.startsWith("bag_arm_slot:")) {
            val rid = id.substring(13)
            openCraft()
            selectedId = rid
            CraftingRegistry.recipes.firstOrNull { it.id == rid }?.let { loadRecipePatternInto4x4(it) }
            return
        }
        if (bagTab == 1) {
            tapCraft4x4Modal(id)
            return
        }
        if (id.startsWith("gdrop:")) {
            val did = id.substring(6)
            dropOrStoreItemById(did)
            return
        }
        if (id.startsWith("btab:")) {
            bagTab = id.substring(5).toInt()
            scrollY = 0f
            selectedId = ""
            Sound.play("click")
            return
        }
        if (bagTab == 1 && (id.startsWith("sel:") || id.startsWith("buy:"))) {
            val isBuy = id.startsWith("buy:")
            val key = id.substring(4)
            if (selectedId != key) {
                selectedId = key
                Sound.play("click")
                return
            }
            if (isBuy) {
                val r = CraftingRegistry.recipes.firstOrNull { it.id == key } ?: return
                craftItem(r)
            }
            return
        }
        if (id.startsWith("gslot:")) {
            gridSelIdx = id.substring(6).toIntOrNull() ?: 0
            Sound.play("click", 0.6f)
            return
        }
        val actId = if (id.startsWith("gact:")) id.substring(5) else id
        when {
            actId == "blk_wood" -> { player.hotbarIdx = 2; modal = Modal.NONE; toast("اخترت بلوك ألواح خشبية للبناء") }
            actId == "blk_stone" -> { player.hotbarIdx = 3; modal = Modal.NONE; toast("اخترت بلوك طوب حجري للبناء") }
            actId == "blk_window" -> { player.hotbarIdx = 4; modal = Modal.NONE; toast("اخترت جدار النافذة القتالية (فورتنايت)") }
            actId == "blk_bridge" -> { player.hotbarIdx = 5; modal = Modal.NONE; toast("اخترت الجسر والأرضية الخشبية للبناء") }
            actId == "blk_launchpad" -> { player.hotbarIdx = 6; modal = Modal.NONE; toast("اخترت منصة القفز الهوائي") }
            actId == "blk_irongate" && player.blkIronGate > 0 -> {
                val w = ow
                if (w != null && !inCave) {
                    val (tx, ty) = aimTileCoords()
                    if (w.placeBlock(tx, ty, BlockType.IRON_GATE, player.cx, player.cy)) {
                        player.blkIronGate--
                        Sound.play("gate")
                        toast("ركّبت بوابة القلعة الحديدية المصفحة! 🛡️")
                        modal = Modal.NONE
                    }
                }
            }
            actId == "blk_spikes" -> { player.hotbarIdx = 5; modal = Modal.NONE; toast("اخترت فخ الأشواك الفولاذية للبناء") }
            actId == "blk_obsidian" -> { player.hotbarIdx = 6; modal = Modal.NONE; toast("اخترت حجر السبج المدرع المضاد للانفجارات") }
            actId == "blk_wire" -> { player.hotbarIdx = 7; modal = Modal.NONE; toast("اخترت السلك الشائك لإبطاء الزومبي") }
            actId == "blk_beacon" && player.blkBeacon > 0 -> {
                val w = ow
                if (w != null && !inCave) {
                    val (tx, ty) = aimTileCoords()
                    if (w.placeBlock(tx, ty, BlockType.HEALING_BEACON, player.cx, player.cy)) {
                        player.blkBeacon--
                        Sound.play("magic")
                        toast("نصبت منارة الشفاء البلورية! ستشفيك وتشفي الحراس تلقائياً ✨")
                        modal = Modal.NONE
                    }
                }
            }
            actId == "blk_royal" && player.blkRoyalFloor > 0 -> {
                val w = ow
                if (w != null && !inCave) {
                    val (tx, ty) = aimTileCoords()
                    if (w.placeBlock(tx, ty, BlockType.ROYAL_FLOOR, player.cx, player.cy)) {
                        player.blkRoyalFloor--
                        Sound.play("chop")
                        toast("فرشت أرضية الرخام الملكية (+25% سرعة حركة) 👑")
                        modal = Modal.NONE
                    }
                }
            }
            actId == "place_golem" && player.golemsItem > 0 -> {
                player.golemsItem--
                golems.add(IronGolemGuard(player.cx + 48f, player.cy + 16f))
                Sound.play("boss", 0.75f)
                toast("نشرت الغولم الحديدي الحارس 🤖 لحماية قلعتك ومزارعك!")
                modal = Modal.NONE
            }
            actId == "use_wcan" && player.hasWateringCan -> {
                val w = ow
                if (w != null && !inCave) {
                    val (tx, ty) = aimTileCoords()
                    val n = w.waterCropsAround(tx, ty)
                    Sound.play("potion")
                    toast(if (n > 0) "رويت $n من المحاصيل والأشجار بدلو الماء فنضجت فوراً! 💧" else "اقترب من المحاصيل أو الشتلات لريّها بدلو الماء!")
                    modal = Modal.NONE
                }
            }
            actId == "switch_brush" -> {
                player.buildBrushMode = (player.buildBrushMode + 1) % 3
                val mName = when (player.buildBrushMode) {
                    1 -> "سور خطي (5 بلوكات بضغطة)"
                    2 -> "غرفة مربعة كاملة (5×5 بضغطة)"
                    else -> "بلوك فردي (1×1)"
                }
                Sound.play("click")
                toast("نمط هندسة البناء الآن: $mName 📐")
            }
            actId == "blk_door" && player.blkDoor > 0 -> {
                val w = ow
                if (w != null && !inCave) {
                    val (tx, ty) = aimTileCoords()
                    if (w.placeBlock(tx, ty, BlockType.DOOR_CLOSED, player.cx, player.cy)) {
                        player.blkDoor--
                        Sound.play("gate")
                        toast("ركّبت باب المنزل الخشبي! 🚪")
                        modal = Modal.NONE
                    }
                }
            }
            actId == "blk_tnt" -> { player.hotbarIdx = 8; modal = Modal.NONE; toast("اخترت بلوك متفجرات TNT") }
            actId == "abyss_seals" -> { scrollY = 0f; modal = Modal.BOSS_ALTAR }
            actId == "conv_wood" && player.wood >= 1 -> {
                player.wood -= 1
                player.blkWood += 4
                Sound.play("chop")
                toast("حولت 1 خشب خام إلى +4 بلوكات ألواح خشبية!")
            }
            actId == "conv_stone" && player.stone >= 1 -> {
                player.stone -= 1
                player.blkStone += 4
                Sound.play("mine")
                toast("حولت 1 حجر خام إلى +4 بلوكات طوب حجري!")
            }
            actId == "raw_wool" && player.wool >= 3 && player.wood >= 4 -> {
                player.wool -= 3
                player.wood -= 4
                player.bedsItem += 1
                Sound.play("buy")
                toast("صنعت سرير نوم ميداني من الصوف والخشب!")
            }
            actId == "eat_stew" && player.stew > 0 -> {
                player.stew--
                hunger = min(100f, hunger + 75f)
                player.hp = min(player.maxHp, player.hp + 35)
                Sound.play("potion")
                toast("تناولت حساء الطاقة الساخن (+75 شبع و+35 صحة)")
            }
            actId == "eat_cooked" && player.cookedMeat > 0 -> {
                player.cookedMeat--
                hunger = min(100f, hunger + 45f)
                player.hp = min(player.maxHp, player.hp + 12)
                Sound.play("potion")
                toast("تناولت لحماً مشوياً (+45 شبع)")
            }
            actId == "eat_apple" && player.apples > 0 -> {
                player.apples--
                hunger = min(100f, hunger + 16f)
                player.hp = min(player.maxHp, player.hp + 5)
                Sound.play("potion")
                toast("تناولت تفاحة (+16 شبع)")
            }
            actId == "eat_raw" && player.rawMeat > 0 -> {
                player.rawMeat--
                hunger = min(100f, hunger + 12f)
                Sound.play("potion")
                toast("تناولت لحماً نيئاً")
            }
            actId == "use_heal" -> {
                if (player.usePotion()) {
                    Sound.play("potion")
                    toast("استعدت +85 صحة")
                }
            }
            actId == "use_speed" -> {
                if (player.useSpeedPotion()) {
                    Sound.play("magic")
                    toast("تفعّل إكسير السرعة والرؤية الليلية!")
                    modal = Modal.NONE
                }
            }
            actId == "place_bed" && player.bedsItem > 0 -> {
                player.bedsItem--
                beds.add(Bed(player.cx - 48f, player.cy - 20f))
                Sound.play("buy")
                toast("وضعت سرير النوم في العالم!")
                modal = Modal.NONE
            }
            actId == "place_furnace" && player.furnacesItem > 0 -> {
                player.furnacesItem--
                furnaces.add(FurnaceObj(player.cx - 34f, player.cy - 24f))
                Sound.play("buy")
                toast("وضعت فرن الصهر والطبخ!")
                modal = Modal.NONE
            }
            actId == "plant_sapling" && player.treeSaplings > 0 -> {
                val w = ow
                if (w != null && !inCave) {
                    val (tx, ty) = aimTileCoords()
                    if (w.plantSaplingAt(tx, ty)) {
                        player.treeSaplings--
                        Sound.play("chop")
                        toast("زرعت شتلة شجر بيدك! ستنمو إلى شجرة كاملة قريباً 🌱")
                        advanceQuest("q_farm", 1)
                        modal = Modal.NONE
                    } else {
                        toast("اختر أرضاً ترابية أو عشبية فارغة لزراعة الشتلة!")
                        Sound.play("deny")
                    }
                }
            }
            actId == "plant_seeds" && player.cropSeeds > 0 -> {
                val w = ow
                if (w != null && !inCave) {
                    val (tx, ty) = aimTileCoords()
                    if (w.plantCropAt(tx, ty)) {
                        player.cropSeeds--
                        Sound.play("chop")
                        toast("زرعت بذور القمح في الأرض! انتظر نضوجها ثم احصدها 🌾")
                        advanceQuest("q_farm", 1)
                        modal = Modal.NONE
                    } else {
                        toast("اختر أرضاً فارغة لزراعة البذور!")
                        Sound.play("deny")
                    }
                }
            }
            actId == "eat_wheat" && player.wheat > 0 -> {
                player.wheat--
                hunger = min(100f, hunger + 55f)
                player.hp = min(player.maxHp, player.hp + 20)
                Sound.play("potion")
                toast("تناولت خبز القمح الطازج (+55 شبع و+20 صحة)")
            }
            actId == "use_powder" && player.gunpowder > 0 -> {
                var filled = false
                for (cn in cannons) {
                    if (cn.friendly && !cn.isDestroyed && cn.ammo < cn.maxAmmo && dist(cn.x, cn.y, player.cx, player.cy) < 400f) {
                        val put = min(cn.maxAmmo - cn.ammo, min(10, player.gunpowder))
                        player.gunpowder -= put
                        cn.ammo += put
                        filled = true
                        Sound.play("buy")
                        toast("عبّأت المدفع بـ +$put بارود (${cn.ammo}/${cn.maxAmmo})!")
                        advanceQuest("q_powder", 1)
                        break
                    }
                }
                if (!filled) toast("اقترب من أحد مدافعك على السور لتعبئته بالبارود!")
            }
            actId == "place_storage" && player.storageChestsItem > 0 -> {
                player.storageChestsItem--
                storageChests.add(StorageChest(player.cx - 36f, player.cy - 24f, "شونة الفارس ${storageChests.size + 1}"))
                Sound.play("buy")
                toast("وضعت شونة وصندوق تخزين جديد! تفاعل معه لتفريغ الشنطة 📦")
                modal = Modal.NONE
            }
            actId == "use_house_bp" && player.houseBlueprints > 0 -> {
                val w = ow
                if (w != null && !inCave) {
                    player.houseBlueprints--
                    val tx = floor(player.cx / OpenWorld.TS).toInt()
                    val ty = floor(player.cy / OpenWorld.TS).toInt()
                    w.buildHouseBlueprintAt(tx, ty)
                    LanMultiplayer.send("BLUEPRINT|0|$tx|$ty")
                    Sound.play("era_clear")
                    toast("تم بناء البيت الكامل (جدران ونوافذ وباب ومشاعل) في موقعك! 🏠")
                    advanceQuest("q_build", 8)
                    modal = Modal.NONE
                }
            }
            actId == "use_fort_bp" && player.fortBlueprints > 0 -> {
                val w = ow
                if (w != null && !inCave) {
                    player.fortBlueprints--
                    val tx = floor(player.cx / OpenWorld.TS).toInt()
                    val ty = floor(player.cy / OpenWorld.TS).toInt()
                    w.buildFortress2ThickAt(tx, ty)
                    LanMultiplayer.send("BLUEPRINT|1|$tx|$ty")
                    Sound.play("era_clear")
                    toast("تم بناء القلعة الحجرية المزدوجة (سمك 2 حجر) المضادة للزومبي المفجر! 🛡️🧱")
                    advanceQuest("q_build", 15)
                    modal = Modal.NONE
                }
            }
            actId == "use_tower_bp" && player.towerBlueprints > 0 -> {
                val w = ow
                if (w != null && !inCave) {
                    player.towerBlueprints--
                    val tx = floor(player.cx / OpenWorld.TS).toInt()
                    val ty = floor(player.cy / OpenWorld.TS).toInt()
                    w.buildWatchtowerBlueprintAt(tx, ty)
                    LanMultiplayer.send("BLUEPRINT|2|$tx|$ty")
                    Sound.play("era_clear")
                    toast("بنيت ستركشر برج القنص والمدفعية (سمك 2 حجر + نوافذ قنص + منارة شفاء)! 🏰")
                    advanceQuest("q_build", 12)
                    modal = Modal.NONE
                }
            }
            actId == "use_farm_bp" && player.farmBlueprints > 0 -> {
                val w = ow
                if (w != null && !inCave) {
                    player.farmBlueprints--
                    val tx = floor(player.cx / OpenWorld.TS).toInt()
                    val ty = floor(player.cy / OpenWorld.TS).toInt()
                    w.buildFarmEstateBlueprintAt(tx, ty)
                    LanMultiplayer.send("BLUEPRINT|3|$tx|$ty")
                    Sound.play("era_clear")
                    toast("بنيت ستركشر المزرعة الملكية المسوّرة مع 12 محصولاً متنوعاً ومروياً! 🌾")
                    advanceQuest("q_farm", 6)
                    advanceQuest("q_build", 10)
                    modal = Modal.NONE
                }
            }
            actId == "place_cannon" && player.cannonsItem > 0 -> {
                player.cannonsItem--
                val w = ow
                val (tx, ty) = aimTileCoords()
                val onWall = w != null && w.isStoneWallTile(tx, ty)
                val cxPos = if (w != null && !inCave) tx * OpenWorld.TS - 6f else player.cx - 38f
                val cyPos = if (w != null && !inCave) ty * OpenWorld.TS - 10f else player.cy - 28f
                cannons.add(CannonTurret(cxPos, cyPos, true, onWall))
                Sound.play("buy")
                toast(if (onWall) "نصبت المدفع فوق سور القلعة الحجري! املأه بالبارود ليقصف الزومبي 💥" else "نصبت مدفع الحراسة الآلي! املأه بالبارود")
                modal = Modal.NONE
            }
            actId == "place_camp" && player.campfiresItem > 0 -> {
                player.campfiresItem--
                campfires.add(CampfireNode(player.cx - 32f, player.cy - 24f, "مخيم الفارس ${campfires.size + 1}", true))
                Sound.play("magic")
                toast("بنيت مخيم سفر سريع جديد!")
                modal = Modal.NONE
            }
            actId == "use_map" && player.treasureMaps > 0 -> {
                player.treasureMaps--
                spawnRandomTreasureMound()
                Sound.play("era_clear")
                toast("تم تحديد موقع كنز مدفون جديد على البوصلة!")
                modal = Modal.NONE
            }
            actId.startsWith("eq:") -> {
                val wid = actId.substring(3)
                val w = GameCatalogue.weaponById(wid)
                if (w != null) {
                    player.weapon = w
                    player.hotbarIdx = 0
                    Sound.play("click")
                    toast("تم تجهيز: ${w.nameAr}")
                }
            }
            actId.startsWith("spell:") -> {
                player.spellIdx = actId.substring(6).toInt()
                Sound.play("magic")
                toast("تم تفعيل تعويذة: ${player.currentSpell.nameAr}")
            }
            actId.startsWith("quest:") -> {
                val qid = actId.substring(6)
                val q = quests.firstOrNull { it.id == qid }
                if (q != null && q.isComplete && !q.claimed) {
                    q.claimed = true
                    player.coins += q.rewardGold
                    player.mats[q.rewardMatIdx] += q.rewardMatCount
                    Sound.play("victory")
                    toast("استلمت مكافأة المهمة: +${q.rewardGold} ذهب و+${q.rewardMatCount} ${Mats.names[q.rewardMatIdx]}!")
                    saveProgress()
                }
            }
        }
    }

    // ---------------------------------------------------------------- Drop / Empty Item Helper
    private fun dropOrStoreItemById(id: String) {
        var moved = false
        when (id) {
            "blk_wood" -> if (player.blkWood > 0) { val n = min(5, player.blkWood); player.blkWood -= n; player.stBlkWood += n; moved = true }
            "blk_stone" -> if (player.blkStone > 0) { val n = min(5, player.blkStone); player.blkStone -= n; player.stBlkStone += n; moved = true }
            "conv_wood" -> if (player.wood > 0) { val n = min(5, player.wood); player.wood -= n; player.stWood += n; moved = true }
            "conv_stone" -> if (player.stone > 0) { val n = min(5, player.stone); player.stone -= n; player.stStone += n; moved = true }
            "raw_iron" -> if (player.iron > 0) { val n = min(3, player.iron); player.iron -= n; player.stIron += n; moved = true }
            "raw_wool" -> if (player.wool > 0) { val n = min(3, player.wool); player.wool -= n; player.stWool += n; moved = true }
            "use_powder" -> if (player.gunpowder > 0) { val n = min(5, player.gunpowder); player.gunpowder -= n; player.stGunpowder += n; moved = true }
            "eat_wheat" -> if (player.wheat > 0) { val n = min(3, player.wheat); player.wheat -= n; player.stWheat += n; moved = true }
            "plant_seeds" -> if (player.cropSeeds > 0) { val n = min(3, player.cropSeeds); player.cropSeeds -= n; player.stSeeds += n; moved = true }
            "plant_sapling" -> if (player.treeSaplings > 0) { val n = min(3, player.treeSaplings); player.treeSaplings -= n; player.stSaplings += n; moved = true }
        }
        if (moved) {
            Sound.play("click")
            toast("نقلت الدفعة من الشنطة إلى الشونة لتخفيف الحمل! 📦")
            saveProgress()
        } else {
            val n = player.emptyBagToStorage()
            if (n > 0) {
                Sound.play("coin")
                toast("فرّغت $n عنصراً من الشنطة إلى الشونة!")
                saveProgress()
            }
        }
    }



    // ---------------------------------------------------------------- Co-Op LAN / Hotspot Multiplayer Sync & UI (طور اللعب الجماعي عبر الواي فاي ونقطة الاتصال)
    private fun sendFullWorldSyncToGuest() {
        val w = ow ?: return
        LanMultiplayer.send("WORLD_META|$dayNo|$dayTime|$weatherType|${w.seed}")
        val pblocks = w.savePlayerBlocks()
        if (pblocks.isNotEmpty()) LanMultiplayer.send("WORLD_PBLOCKS|$pblocks")
        val fdata = w.saveFarming()
        if (fdata.isNotEmpty()) LanMultiplayer.send("WORLD_FARM|$fdata")
        val mdata = w.saveMods()
        if (mdata.isNotEmpty()) LanMultiplayer.send("WORLD_MODS|$mdata")
    }

    private fun updateMultiplayerSync(w: OpenWorld, dt: Float) {
        coopPartner.update(dt)
        netSyncTimer += dt
        if (netSyncTimer >= 0.055f) {
            netSyncTimer = 0f
            val msg = StringBuilder("POS|")
                .append(player.x.toInt()).append('|')
                .append(player.y.toInt()).append('|')
                .append(((player.aimX * 100).toInt()) / 100f).append('|')
                .append(((player.aimY * 100).toInt()) / 100f).append('|')
                .append(if (player.facingLeft) 1 else 0).append('|')
                .append(if (player.moving) 1 else 0).append('|')
                .append(if (player.isAttacking) 1 else 0).append('|')
                .append(if (player.isDashing) 1 else 0).append('|')
                .append(player.hp).append('|')
                .append(player.maxHp).append('|')
                .append(player.weapon.id).append('|')
                .append(if (player.inBoat) 1 else 0).append('|')
                .append(((player.airTimer * 100).toInt()) / 100f).append('|')
                .append(((player.parryTimer * 100).toInt()) / 100f).append('|')
                .append(player.comboCount).append('|')
                .append(if (player.isDead) 1 else 0)
                .toString()
            LanMultiplayer.send(msg)
        }

        var processed = 0
        while (processed < 40) {
            val pkt = LanMultiplayer.incoming.poll() ?: break
            processed++
            if (pkt == "SYS_DISCONNECTED") {
                toast("انقطع الاتصال مع صديقك")
                break
            }
            val p = pkt.split("|")
            if (p.isEmpty()) continue
            when (p[0]) {
                "POS" -> if (p.size >= 17) {
                    coopPartner.onNetSync(
                        p[1].toFloatOrNull() ?: coopPartner.x,
                        p[2].toFloatOrNull() ?: coopPartner.y,
                        p[3].toFloatOrNull() ?: 1f,
                        p[4].toFloatOrNull() ?: 0f,
                        p[5] == "1",
                        p[6] == "1",
                        p[7] == "1",
                        p[8] == "1",
                        p[9].toIntOrNull() ?: 180,
                        p[10].toIntOrNull() ?: 180,
                        p[11],
                        p[12] == "1",
                        p[13].toFloatOrNull() ?: 0f,
                        p[14].toFloatOrNull() ?: 0f,
                        p[15].toIntOrNull() ?: 0,
                        p[16] == "1"
                    )
                }
                "WORLD_META" -> if (p.size >= 4) {
                    dayNo = p[1].toIntOrNull() ?: dayNo
                    dayTime = p[2].toFloatOrNull() ?: dayTime
                    weatherType = p[3].toIntOrNull() ?: weatherType
                    toast("تمت مزامنة عالم صديقك بنجاح! 🤝🌍")
                }
                "WORLD_PBLOCKS" -> if (p.size >= 2) {
                    w.loadPlayerBlocks(p[1])
                }
                "WORLD_FARM" -> if (p.size >= 2) {
                    w.loadFarming(p[1])
                }
                "WORLD_MODS" -> if (p.size >= 2) {
                    w.loadMods(p[1])
                }
                "BLK" -> if (p.size >= 4) {
                    val tx = p[1].toIntOrNull() ?: continue
                    val ty = p[2].toIntOrNull() ?: continue
                    val code = p[3].toIntOrNull() ?: continue
                    if (code == 0) {
                        w.removeBlockAt(tx, ty)
                    } else {
                        val bt = BlockType.byCode(code)
                        if (bt != null) w.placeBlock(tx, ty, bt, -9999f, -9999f)
                    }
                    particles.burst(tx * OpenWorld.TS + 32f, ty * OpenWorld.TS + 32f, 5, Color.parseColor("#00E5FF"), 110f, 4f, 0.3f)
                }
                "EDIT" -> if (p.size >= 3) {
                    val tx = p[1].toIntOrNull() ?: continue
                    val ty = p[2].toIntOrNull() ?: continue
                    w.editBlockAt(tx, ty)
                }
                "DOOR" -> if (p.size >= 3) {
                    val tx = p[1].toIntOrNull() ?: continue
                    val ty = p[2].toIntOrNull() ?: continue
                    w.toggleDoorAt(tx, ty)
                }
                "BLUEPRINT" -> if (p.size >= 4) {
                    val kind = p[1].toIntOrNull() ?: continue
                    val tx = p[2].toIntOrNull() ?: continue
                    val ty = p[3].toIntOrNull() ?: continue
                    when (kind) {
                        0 -> w.buildHouseBlueprintAt(tx, ty)
                        1 -> w.buildFortress2ThickAt(tx, ty)
                        2 -> w.buildWatchtowerBlueprintAt(tx, ty)
                        3 -> w.buildFarmEstateBlueprintAt(tx, ty)
                    }
                    toast("صديقك بنى ستركشر كامل في العالم! 🏗️")
                }
                "CROP" -> if (p.size >= 5) {
                    val act = p[1]
                    val tx = p[2].toIntOrNull() ?: continue
                    val ty = p[3].toIntOrNull() ?: continue
                    val vr = p[4].toIntOrNull() ?: 0
                    when (act) {
                        "PLANT" -> w.plantCropAt(tx, ty, vr)
                        "SAPLING" -> w.plantSaplingAt(tx, ty)
                        "HARVEST" -> w.harvestCropAt(tx, ty)
                        "WATER" -> w.waterCropsAround(tx, ty)
                    }
                }
                "EXPLODE" -> if (p.size >= 3) {
                    val ex = p[1].toFloatOrNull() ?: continue
                    val ey = p[2].toFloatOrNull() ?: continue
                    explode(ex, ey)
                }
                "GIFT" -> if (p.size >= 3) {
                    val kind = p[1]
                    val amt = p[2].toIntOrNull() ?: 10
                    when (kind) {
                        "MATS" -> {
                            player.wood += amt
                            player.stone += amt
                            toast("أرسل لك صديقك هدية: +$amt خشب و+$amt حجر! 🎁")
                        }
                        "POWDER" -> {
                            player.gunpowder += amt
                            toast("أرسل لك صديقك هدية: +$amt بارود مدافع! 💥🎁")
                        }
                        "GOLD" -> {
                            player.coins += amt
                            toast("أرسل لك صديقك هدية: +$amt ذهب! 🪙🎁")
                        }
                    }
                    Sound.play("coin")
                }
                "PING" -> if (p.size >= 4) {
                    val txt = p[1]
                    val px = p[2].toFloatOrNull() ?: coopPartner.cx
                    val py = p[3].toFloatOrNull() ?: coopPartner.cy
                    coopPartner.triggerPing(txt)
                    waypointX = px
                    waypointY = py
                    waypointTitle = "إشارة صديقك: $txt"
                    Sound.play("click")
                    toast("إشارة من صديقك: $txt 📍")
                }
                "REVIVE" -> {
                    if (player.isDead) {
                        player.isDead = false
                        player.hp = (player.maxHp * 0.65f).toInt()
                        playerDownedTimer = 0f
                        deathTimer = 0f
                        Sound.play("era_clear")
                        particles.burst(player.cx, player.cy, 22, Color.parseColor("#69F0AE"), 220f, 6f, 0.55f)
                        addFloat("أنعشك صديقك! ❤️‍🩹🤝", player.cx, player.y - 20f, Color.parseColor("#69F0AE"), 25f)
                        toast("أنعشك صديقك وعدت للقتال!")
                    }
                }
            }
        }
    }

    private fun drawMultiplayerModal(c: Canvas) {
        val u = Ui.u
        dim(c)
        LanMultiplayer.pruneRooms()
        val mr = modalRect()
        Ui.panel(c, mr, Color.parseColor("#00E5FF"), Color.argb(248, 16, 22, 34), 10f)
        closeButton(c, mr)
        Ui.text(c, "طور اللعب الجماعي التعاوني (واي فاي / نقطة اتصال Hotspot) 🌐🤝", mr.right - 24f * u, mr.top + 40f * u, 22f, GOLD, Paint.Align.RIGHT)

        val myIp = LanMultiplayer.getLocalIpAddress()
        val myCode = LanMultiplayer.getRoomCode()
        Ui.text(c, "كود غرفتك السريع: [$myCode]  •  عنوان جهازك: $myIp", mr.left + 78f * u, mr.top + 40f * u, 14.5f, Color.parseColor("#80DEEA"), Paint.Align.LEFT)

        // Status banner
        val stBox = RectF(mr.left + 18f * u, mr.top + 54f * u, mr.right - 18f * u, mr.top + 88f * u)
        val stCol = if (LanMultiplayer.isConnected) Color.parseColor("#00E676") else Color.parseColor("#FFB300")
        Ui.panel(c, stBox, stCol, Color.argb(220, 10, 16, 26), 6f)
        Ui.text(c, LanMultiplayer.statusAr, stBox.centerX(), stBox.centerY() + 5.5f * u, 14.5f, stCol)

        val contentTop = mr.top + 96f * u
        val midX = mr.centerX()

        // Right Column: HOST A GAME (استضافة عالمك أو فتح عالم جديد) + Co-Op Team Actions
        val hostBox = RectF(midX + 8f * u, contentTop, mr.right - 18f * u, mr.bottom - 16f * u)
        Ui.panel(c, hostBox, GOLD, Color.argb(235, 22, 28, 40), 8f)
        Ui.text(c, "1. استضافة غرفة لصديقك (أنت المضيف Host) 👑", hostBox.centerX(), hostBox.top + 26f * u, 16.5f, GOLD)
        Ui.text(c, "اختر نوع العالم الذي ستلعبان فيه معاً:", hostBox.right - 16f * u, hostBox.top + 50f * u, 13.5f, Color.rgb(210, 225, 245), Paint.Align.RIGHT)

        val wBtn1 = RectF(hostBox.left + 14f * u, hostBox.top + 60f * u, hostBox.centerX() - 6f * u, hostBox.top + 98f * u)
        val wBtn2 = RectF(hostBox.centerX() + 6f * u, hostBox.top + 60f * u, hostBox.right - 14f * u, hostBox.top + 98f * u)
        Ui.button(c, wBtn2, "عالمك الحالي (بمبانيك) 🏰", if (!mpWorldChoiceNew) Color.parseColor("#2E7D32") else Color.parseColor("#37474F"), true, 13f, !mpWorldChoiceNew)
        Ui.button(c, wBtn1, "عالم تعاوني جديد 🌱", if (mpWorldChoiceNew) Color.parseColor("#0277BD") else Color.parseColor("#37474F"), true, 13f, mpWorldChoiceNew)
        hits.add(Hit(wBtn2, "mp_wmode:cur"))
        hits.add(Hit(wBtn1, "mp_wmode:new"))

        val startHostBtn = RectF(hostBox.left + 14f * u, hostBox.top + 106f * u, hostBox.right - 14f * u, hostBox.top + 150f * u)
        val isHosting = LanMultiplayer.role == LanMultiplayer.Role.HOST
        Ui.button(
            c, startHostBtn,
            if (isHosting && LanMultiplayer.isConnected) "متصل كمضيف ✓ (مزامنة العالم الآن 🔄)"
            else if (isHosting) "جاري البث وانتظار صديقك... (كود: $myCode)"
            else "ابدأ استضافة الغرفة الآن وافتح البث 📡",
            if (isHosting) Color.parseColor("#00897B") else Color.parseColor("#F57C00"), true, 15f
        )
        hits.add(Hit(startHostBtn, "mp_start_host"))

        // Co-Op Team Actions (Teleport to Friend, Gift Resources, Quick Tactical Pings, Disconnect)
        Ui.text(c, "أدوات التعاون السريعة مع صديقك داخل العالم 🤝:", hostBox.centerX(), hostBox.top + 176f * u, 15f, Color.parseColor("#80DEEA"))

        var ty = hostBox.top + 188f * u
        val bTp = RectF(hostBox.left + 14f * u, ty, hostBox.centerX() - 6f * u, ty + 38f * u)
        val bSync = RectF(hostBox.centerX() + 6f * u, ty, hostBox.right - 14f * u, ty + 38f * u)
        Ui.button(c, bTp, "انتقال لموقع صديقك ⚡", Color.parseColor("#6A1B9A"), LanMultiplayer.isConnected, 13f)
        Ui.button(c, bSync, "إرسال موارد (+10 خشب/حجر) 🎁", Color.parseColor("#2E7D32"), LanMultiplayer.isConnected, 12.5f)
        if (LanMultiplayer.isConnected) {
            hits.add(Hit(bTp, "mp_tp_friend"))
            hits.add(Hit(bSync, "mp_gift:MATS"))
        }
        ty += 44f * u
        val bGiftP = RectF(hostBox.left + 14f * u, ty, hostBox.centerX() - 6f * u, ty + 38f * u)
        val bGiftG = RectF(hostBox.centerX() + 6f * u, ty, hostBox.right - 14f * u, ty + 38f * u)
        Ui.button(c, bGiftP, "إهداء +10 بارود مدافع 💥", Color.parseColor("#E65100"), LanMultiplayer.isConnected && player.gunpowder >= 10, 12.5f)
        Ui.button(c, bGiftG, "إهداء +100 ذهب 🪙", Color.parseColor("#F9A825"), LanMultiplayer.isConnected && player.coins >= 100, 13f)
        if (LanMultiplayer.isConnected) {
            hits.add(Hit(bGiftP, "mp_gift:POWDER"))
            hits.add(Hit(bGiftG, "mp_gift:GOLD"))
        }
        ty += 44f * u
        val p1 = RectF(hostBox.left + 14f * u, ty, hostBox.centerX() - 6f * u, ty + 36f * u)
        val p2 = RectF(hostBox.centerX() + 6f * u, ty, hostBox.right - 14f * u, ty + 36f * u)
        Ui.button(c, p1, "إشارة: ابنِ معي هنا 🧱📍", Color.parseColor("#0277BD"), LanMultiplayer.isConnected, 12.5f)
        Ui.button(c, p2, "إشارة: هجوم / ساعدني ⚔️🆘", Color.parseColor("#C62828"), LanMultiplayer.isConnected, 12.5f)
        if (LanMultiplayer.isConnected) {
            hits.add(Hit(p1, "mp_ping:ابنِ معي هنا 🧱"))
            hits.add(Hit(p2, "mp_ping:ساعدني هنا ⚔️"))
        }
        ty += 44f * u
        val bDisc = RectF(hostBox.left + 14f * u, ty, hostBox.right - 14f * u, ty + 36f * u)
        Ui.button(c, bDisc, "إيقاف / قطع الاتصال بالشبكة ❌", Color.parseColor("#455A64"), LanMultiplayer.role != LanMultiplayer.Role.NONE, 13f)
        hits.add(Hit(bDisc, "mp_disconnect"))

        // Left Column: JOIN A FRIEND (Auto-Discovered Wi-Fi/Hotspot Rooms + 1-Tap Hotspot + Room Code Keypad)
        val joinBox = RectF(mr.left + 18f * u, contentTop, midX - 8f * u, mr.bottom - 16f * u)
        Ui.panel(c, joinBox, Color.parseColor("#00E5FF"), Color.argb(235, 22, 28, 40), 8f)
        Ui.text(c, "2. الانضمام لغرفة صديقك (واي فاي أو Hotspot) 📲", joinBox.centerX(), joinBox.top + 26f * u, 16.5f, Color.parseColor("#00E5FF"))

        // 1-Tap Auto Hotspot / Discovered Room Connect button
        val autoJoinBtn = RectF(joinBox.left + 14f * u, joinBox.top + 38f * u, joinBox.right - 14f * u, joinBox.top + 80f * u)
        val roomFound = LanMultiplayer.discoveredRooms.firstOrNull()
        val autoLbl = if (roomFound != null) "انضمام فوري لغرفة: ${roomFound.hostName} (${roomFound.worldModeAr}) ⚡"
        else "انضمام تلقائي سريع لنقطة الاتصال (Hotspot) 📶"
        Ui.button(c, autoJoinBtn, autoLbl, Color.parseColor("#00897B"), true, 14f)
        hits.add(Hit(autoJoinBtn, "mp_join_auto"))

        // Room Code / IP Display + On-Screen Keypad
        val codeBox = RectF(joinBox.left + 14f * u, joinBox.top + 88f * u, joinBox.right - 134f * u, joinBox.top + 124f * u)
        Ui.panel(c, codeBox, GOLD, Color.rgb(12, 16, 24), 6f)
        val shownCode = if (mpInputCode.isEmpty()) "اكتب كود صديقك (مثل $myCode)" else "الكود/IP: $mpInputCode"
        Ui.text(c, shownCode, codeBox.centerX(), codeBox.centerY() + 5f * u, 13.5f, if (mpInputCode.isEmpty()) Color.LTGRAY else GOLD)

        val connectCodeBtn = RectF(codeBox.right + 8f * u, joinBox.top + 88f * u, joinBox.right - 14f * u, joinBox.top + 124f * u)
        Ui.button(c, connectCodeBtn, "اتصال 🔗", Color.parseColor("#1565C0"), true, 14f)
        hits.add(Hit(connectCodeBtn, "mp_join_code"))

        // Numeric Keypad for entering Room Code / IP without needing system keyboard!
        val keys = arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "⌫")
        val kw = (joinBox.width() - 46f * u) / 4f
        val kh = 36f * u
        var kx0 = joinBox.left + 14f * u
        var ky0 = joinBox.top + 132f * u
        for (ki in keys.indices) {
            val col = ki % 4
            val row = ki / 4
            val kr = RectF(kx0 + col * (kw + 6f * u), ky0 + row * (kh + 6f * u), kx0 + col * (kw + 6f * u) + kw, ky0 + row * (kh + 6f * u) + kh)
            Ui.button(c, kr, keys[ki], if (keys[ki] == "⌫") Color.parseColor("#B71C1C") else Color.parseColor("#37474F"), true, 15f)
            hits.add(Hit(kr, "mp_key:${keys[ki]}"))
        }

        // How-To Hint at bottom of Join Box
        val hintY = ky0 + 3 * (kh + 6f * u) + 14f * u
        Ui.text(c, "💡 طريقة اللعب: افتح نقطة اتصال (Hotspot) من هاتفك ليتصل", joinBox.centerX(), hintY, 12f, Color.rgb(190, 215, 235))
        Ui.text(c, "بها صديقك (أو اتصلا بنفس الواي فاي)، ثم اضغط (استضافة) واضغط", joinBox.centerX(), hintY + 18f * u, 12f, Color.rgb(190, 215, 235))
        Ui.text(c, "عند صديقك (انضمام تلقائي) لتبنيا وتقاتلا في نفس العالم!", joinBox.centerX(), hintY + 36f * u, 12f, Color.parseColor("#69F0AE"))
    }

    private fun tapMultiplayerModal(id: String?) {
        if (id == null) return
        if (id == "close") {
            Sound.play("click")
            modal = Modal.NONE
            return
        }
        if (id == "mp_wmode:cur") {
            mpWorldChoiceNew = false
            Sound.play("click")
            toast("سيتم استضافة عالمك الحالي بكل مبانيك وتقدمك 🏰")
            return
        }
        if (id == "mp_wmode:new") {
            mpWorldChoiceNew = true
            Sound.play("click")
            toast("سيتم فتح عالم تعاوني جديد مشترك من البداية 🌱")
            return
        }
        if (id.startsWith("mp_key:")) {
            val k = id.substring(7)
            if (k == "⌫") {
                if (mpInputCode.isNotEmpty()) mpInputCode = mpInputCode.dropLast(1)
            } else if (mpInputCode.length < 15) {
                mpInputCode += k
            }
            Sound.play("click", 0.5f)
            return
        }
        if (id == "mp_start_host") {
            Sound.play("era_clear", 0.8f)
            if (LanMultiplayer.isConnected && LanMultiplayer.role == LanMultiplayer.Role.HOST) {
                sendFullWorldSyncToGuest()
                toast("تم إرسال تحديث العالم الكامل لصديقك! 🔄")
                return
            }
            if (mpWorldChoiceNew) {
                startOpen(false)
                modal = Modal.MULTIPLAYER
            }
            coopPartner.x = player.x + 64f
            coopPartner.y = player.y
            coopPartner.targetX = coopPartner.x
            coopPartner.targetY = coopPartner.y
            LanMultiplayer.startHosting("عالم الفارس", mpWorldChoiceNew, { dayNo }) {
                sendFullWorldSyncToGuest()
                toast("انضم صديقك إلى العالم المفتوح! ابذلا قصارى جهدكما 🤝⚔️")
            }
            return
        }
        if (id == "mp_join_auto") {
            Sound.play("click")
            val room = LanMultiplayer.discoveredRooms.firstOrNull()
            val target = room?.ip ?: ""
            coopPartner.x = player.x + 64f
            coopPartner.y = player.y
            LanMultiplayer.connectToHost(target) {
                toast("اتصلت بعالم صديقك بنجاح! جاري مزامنة المباني والعالم 🌍🤝")
            }
            return
        }
        if (id == "mp_join_code") {
            Sound.play("click")
            coopPartner.x = player.x + 64f
            coopPartner.y = player.y
            LanMultiplayer.connectToHost(mpInputCode) {
                toast("اتصلت بعالم صديقك عبر الكود بنجاح! 🤝")
            }
            return
        }
        if (id == "mp_tp_friend" && LanMultiplayer.isConnected) {
            player.x = coopPartner.x + 42f
            player.y = coopPartner.y
            camera.snapTo(player.cx, player.cy)
            Sound.play("magic")
            toast("انتقلت فوراً إلى موقع صديقك! ⚡🤝")
            modal = Modal.NONE
            return
        }
        if (id.startsWith("mp_gift:") && LanMultiplayer.isConnected) {
            val gType = id.substring(8)
            when (gType) {
                "MATS" -> if (player.wood >= 10 && player.stone >= 10) {
                    player.wood -= 10
                    player.stone -= 10
                    LanMultiplayer.send("GIFT|MATS|10")
                    Sound.play("coin")
                    toast("أرسلت +10 خشب و+10 حجر لصديقك! 🎁")
                } else toast("تحتاج 10 خشب و10 حجر لإرسال الهدية")
                "POWDER" -> if (player.gunpowder >= 10) {
                    player.gunpowder -= 10
                    LanMultiplayer.send("GIFT|POWDER|10")
                    Sound.play("coin")
                    toast("أرسلت +10 بارود مدافع لصديقك! 💥🎁")
                }
                "GOLD" -> if (player.coins >= 100) {
                    player.coins -= 100
                    LanMultiplayer.send("GIFT|GOLD|100")
                    Sound.play("coin")
                    toast("أرسلت +100 ذهب لصديقك! 🪙🎁")
                }
            }
            return
        }
        if (id.startsWith("mp_ping:") && LanMultiplayer.isConnected) {
            val pTxt = id.substring(8)
            LanMultiplayer.send("PING|$pTxt|${player.cx.toInt()}|${player.cy.toInt()}")
            addFloat("أرسلت إشارة: $pTxt 📍", player.cx, player.y - 24f, GOLD, 22f)
            Sound.play("click")
            modal = Modal.NONE
            return
        }
        if (id == "mp_disconnect") {
            LanMultiplayer.stopSession()
            Sound.play("click")
            toast("تم إيقاف اتصال اللعب الجماعي")
            return
        }
    }

    // ---------------------------------------------------------------- V7 World Structures, Atmosphere & Interactive World Map
    private fun claimStructureNode(sn: AncientStructureNode) {
        sn.claimed = true
        Sound.play("era_clear")
        shake = 11f
        particles.burst(sn.x, sn.y, 24, sn.accentColor, 240f, 6.5f, 0.65f)
        when (sn.kind) {
            0 -> { // Arcane Wizard Tower
                player.upgMana = min(8, player.upgMana + 1)
                player.recomputeStats()
                player.mana = player.maxMana.toFloat()
                player.manaBerries += 4
                player.blkBeacon += 1
                addFloat("+30 طاقة سحر قصوى دائمة! 🔮 +1 منارة شفاء", sn.x, sn.y - 24f, sn.accentColor, 24f)
                toast("استكشفت ${sn.nameAr}! ارتفعت طاقتك السحرية القصوى وحصلت على منارة شفاء!")
            }
            1 -> { // Ancient Royal Ruins
                player.coins += 180
                player.goldKeys += 2
                player.blkRoyalFloor += 12
                addFloat("+180 ذهب 👑 +2 مفتاح ذهبي +12 رخام ملكي!", sn.x, sn.y - 24f, GOLD, 24f)
                toast("استكشفت ${sn.nameAr}! حصلت على كنز الملوك ومفاتيح ذهبية!")
            }
            2 -> { // Abandoned Gunpowder Mine
                player.gunpowder += 18
                player.iron += 8
                player.blkTnt += 4
                advanceQuest("q_powder", 2)
                addFloat("+18 بارود 💥 +8 حديد +4 بلوكات TNT!", sn.x, sn.y - 24f, Color.parseColor("#FF9800"), 24f)
                toast("استكشفت ${sn.nameAr}! غنمت مخزوناً ضخماً من البارود والحديد والمتفجرات!")
            }
            3 -> { // Sunken Pirate Shipwreck
                player.coins += 220
                player.treasureMaps += 2
                player.cannonsItem += 1
                spawnRandomTreasureMound()
                addFloat("+220 ذهب 🚢 +1 مدفع +2 خريطة كنز!", sn.x, sn.y - 24f, Color.parseColor("#4FC3F7"), 24f)
                toast("استكشفت ${sn.nameAr}! حصلت على مدفع قراصنة وذهب وخرائط كنوز!")
            }
            4 -> { // Cursed Blood Obelisk
                player.gunpowder += 12
                for (mi in 0..5) player.mats[mi] += 1
                if (player.unlockSeal(4)) advanceQuest("q_seals", 1)
                addFloat("+12 بارود 🩸 +شظايا العصور الستة!", sn.x, sn.y - 24f, Color.parseColor("#FF1744"), 24f)
                toast("فعّلت ${sn.nameAr}! حصلت على شظايا العصور الستة وبارود نقي!")
                // Spawn 3 elite guardians for an exciting battle!
                for (k in 0..2) {
                    val ang = k * 2.09f
                    val en = Enemy(sn.x + cos(ang) * 135f, sn.y + sin(ang) * 135f, EnemyRole.HEAVY, EraType.ERA_3_ZOMBIE)
                    en.aggro = true
                    enemies.add(en)
                }
            }
            else -> { // Claimable Stone Watchtower
                player.blkObsidian += 8
                player.blkWindow += 6
                player.golemsItem += 1
                golems.add(IronGolemGuard(sn.x + 44f, sn.y + 24f))
                addFloat("+1 غولم حديدي حارس 🤖 +8 حجر سبج!", sn.x, sn.y - 24f, Color.parseColor("#69F0AE"), 24f)
                toast("احتللت ${sn.nameAr}! انضم إليك غولم حديدي حارس لحماية القلعة!")
            }
        }
        saveProgress()
    }

    private fun updateAtmosphereMotes(w: OpenWorld, dt: Float) {
        if (lightningFlash > 0f) lightningFlash = max(0f, lightningFlash - dt * 2.4f)
        if (weatherType == 2 && rnd.nextFloat() < dt * 0.14f) {
            lightningFlash = 0.85f
        }
        val targetCount = if (lowQuality) 16 else 36
        val bi = w.biomeAtPx(player.cx, player.cy)
        val night = isNight()
        while (ambientMotes.size < targetCount) {
            val rx = camera.x + rnd.nextFloat() * camera.viewW
            val ry = camera.y + rnd.nextFloat() * camera.viewH
            val col = when {
                weatherType == 1 || weatherType == 2 -> Color.argb(185, 130, 205, 255)
                night -> Color.argb(215, 190, 255, 110) // Glowing Fireflies at night!
                bi == 1 -> Color.argb(190, 128, 222, 234) // Crystal spores
                bi == 2 -> Color.argb(190, 255, 112, 67)  // Zombie wasteland embers
                bi == 3 -> Color.argb(185, 255, 241, 118) // Tropical sun sparkles
                bi == 4 -> Color.argb(195, 0, 229, 255)   // Cyber neon motes
                bi == 5 -> Color.argb(195, 224, 64, 251)  // Void rift particles
                else -> Color.argb(190, 165, 214, 167)    // Forest drifting leaves
            }
            val vx = if (weatherType == 1 || weatherType == 2) -140f else (rnd.nextFloat() - 0.5f) * 32f
            val vy = if (weatherType == 1 || weatherType == 2) 520f else -14f - rnd.nextFloat() * 22f
            val sz = if (weatherType == 1 || weatherType == 2) 3.2f else (3.5f + rnd.nextFloat() * 3.5f)
            ambientMotes.add(AmbientMote(rx, ry, vx, vy, sz, col, rnd.nextFloat() * 6.28f))
        }
        val vw = camera.viewW
        val vh = camera.viewH
        for (m in ambientMotes) {
            m.phase += dt * 3.2f
            m.x += (m.vx + sin(m.phase) * 14f) * dt
            m.y += m.vy * dt
            if (m.x < camera.x - 40f) m.x += vw + 80f
            if (m.x > camera.x + vw + 40f) m.x -= vw + 80f
            if (m.y < camera.y - 40f) m.y += vh + 80f
            if (m.y > camera.y + vh + 40f) m.y -= vh + 80f
        }
    }

    private fun drawAtmosphereMotes(c: Canvas) {
        if (inCave) return
        val u = Ui.u
        val isRain = weatherType == 1 || weatherType == 2
        for (m in ambientMotes) {
            val sx = (m.x - camera.x) * zoom
            val sy = (m.y - camera.y) * zoom
            if (sx < -20f || sx > curW + 20f || sy < -20f || sy > curH + 20f) continue
            if (isRain) {
                screenPaint.color = Color.argb(165, 130, 210, 255)
                screenPaint.strokeWidth = 2.2f * u
                c.drawLine(sx, sy, sx - 6f * u, sy + 18f * u, screenPaint)
            } else {
                val pulse = (0.55f + 0.45f * sin(m.phase)).coerceIn(0.2f, 1f)
                val a = (Color.alpha(m.color) * pulse).toInt().coerceIn(30, 240)
                val col = Color.argb(a, Color.red(m.color), Color.green(m.color), Color.blue(m.color))
                Ui.circle(c, sx, sy, m.size * u * 0.7f, col)
            }
        }
    }

    /** Renders warm flickering light halos over Campfires, Furnaces, Portals, Structure Shrines & Torches at night! */
    private fun drawDynamicLightHalos(c: Canvas, dk: Float) {
        val w = ow ?: return
        if (inCave) return
        val flicker = 0.92f + 0.08f * sin(time * 9.5f)
        val alphaBase = (dk * 95f * flicker).toInt().coerceIn(18, 110)

        fun halo(wx: Float, wy: Float, radPx: Float, r: Int, g: Int, b: Int) {
            val sx = (wx - camera.x) * zoom
            val sy = (wy - camera.y) * zoom
            if (sx < -radPx || sx > curW + radPx || sy < -radPx || sy > curH + radPx) return
            Ui.circle(c, sx, sy, radPx * zoom, Color.argb(alphaBase, r, g, b))
            Ui.circle(c, sx, sy, radPx * 0.52f * zoom, Color.argb((alphaBase * 1.3f).toInt().coerceAtMost(140), r, g, b))
        }

        // Player warm lantern aura
        halo(player.cx, player.cy, 105f, 255, 200, 110)

        for (cp in campfires) if (cp.isLit) halo(cp.x + 32f, cp.y + 32f, 170f, 255, 165, 60)
        for (fn in furnaces) halo(fn.x + 34f, fn.y + 34f, 130f, 255, 140, 50)
        for (pt in owPortals) if (pt.isActive) halo(pt.x + 70f, pt.y + 70f, 165f, 0, 229, 255)
        for (sn in structureNodes) if (!sn.claimed) halo(sn.x, sn.y, 160f, Color.red(sn.accentColor), Color.green(sn.accentColor), Color.blue(sn.accentColor))

        // Nearby player-placed Torches & Healing Beacons
        val ptx = floor(player.cx / OpenWorld.TS).toInt()
        val pty = floor(player.cy / OpenWorld.TS).toInt()
        for (dy in -10..10) for (dx in -14..14) {
            val tx = ptx + dx
            val ty = pty + dy
            val code = w.blockCodeAtTile(tx, ty)
            if (code == BlockType.TORCH.code) {
                halo(tx * OpenWorld.TS + 32f, ty * OpenWorld.TS + 32f, 125f, 255, 190, 75)
            } else if (code == BlockType.HEALING_BEACON.code) {
                halo(tx * OpenWorld.TS + 32f, ty * OpenWorld.TS + 32f, 150f, 0, 229, 255)
            }
        }
    }

    // ---------------------------------------------------------------- Full-Screen Interactive World & Structure Map Modal (خريطة العالم ورادار الستركشرات)
    private fun drawWorldMapModal(c: Canvas) {
        val w = ow ?: return
        val u = Ui.u
        dim(c)
        val mr = modalRect()
        Ui.panel(c, mr, GOLD, Color.argb(248, 18, 22, 32), 10f)
        closeButton(c, mr)
        Ui.text(c, "خريطة العالم التفاعلية ورادار الستركشرات الكبرى (512×512)", mr.right - 24f * u, mr.top + 42f * u, 23f, GOLD, Paint.Align.RIGHT)

        val discoveredCnt = structureNodes.count { it.claimed }
        Ui.text(c, "الستركشرات المكتشفة: $discoveredCnt / ${structureNodes.size}  •  اضغط على أي مكان بالخريطة لتحديد البوصلة 📍", mr.left + 78f * u, mr.top + 42f * u, 14f, Color.parseColor("#80DEEA"), Paint.Align.LEFT)

        val contentTop = mr.top + 60f * u
        val mapSize = min(mr.height() - 82f * u, mr.width() * 0.46f)
        val mapLeft = mr.left + 20f * u
        val mapRect = RectF(mapLeft, contentTop, mapLeft + mapSize, contentTop + mapSize)
        Ui.panel(c, mapRect, Color.parseColor("#455A64"), Color.rgb(10, 14, 22), 8f)
        c.drawBitmap(w.minimap, null, mapRect, null)
        hits.add(Hit(mapRect, "wmap_canvas"))

        val fx = mapSize / w.width
        val fy = mapSize / w.height

        // Draw Sub-Villages, Caves, Fortresses, Structures, Boss Altar & Player on the big map
        for (sv in w.subVillageTiles) {
            Ui.circle(c, mapRect.left + sv.first * OpenWorld.TS * fx, mapRect.top + sv.second * OpenWorld.TS * fy, 5f * u, Color.parseColor("#69F0AE"))
        }
        for (ce in caveEntrances) {
            Ui.circle(c, mapRect.left + ce.x * fx, mapRect.top + ce.y * fy, 4.2f * u, Color.CYAN)
        }
        for (sn in structureNodes) {
            val sx = mapRect.left + sn.x * fx
            val sy = mapRect.top + sn.y * fy
            Ui.circle(c, sx, sy, if (sn.claimed) 4f * u else 6.2f * u, if (sn.claimed) Color.GRAY else sn.accentColor)
            Ui.icon(c, sn.iconAsset, sx, sy, 16f)
        }
        // Colossal Boss Altar
        Ui.circle(c, mapRect.left + bossAltar.x * fx, mapRect.top + bossAltar.y * fy, 6.5f * u, Color.parseColor("#FF1744"))
        // Central Village
        Ui.circle(c, mapRect.left + OpenWorld.CX * OpenWorld.TS * fx, mapRect.top + OpenWorld.CY * OpenWorld.TS * fy, 6.5f * u, GOLD)
        // Active Waypoint
        if (waypointX > 0f && waypointY > 0f) {
            val wx = mapRect.left + waypointX * fx
            val wy = mapRect.top + waypointY * fy
            Ui.circle(c, wx, wy, 8f * u, Color.parseColor("#00E5FF"))
        }
        // Player Position
        val pxMap = mapRect.left + player.cx * fx
        val pyMap = mapRect.top + player.cy * fy
        Ui.circle(c, pxMap, pyMap, 6f * u, Color.WHITE)
        Ui.text(c, "أنت", pxMap, pyMap - 8f * u, 11.5f, Color.WHITE)

        // Right Panel: Scrollable Radar of All 12 Major World Structures + Landmarks
        val listLeft = mapRect.right + 16f * u
        listRect = RectF(listLeft, contentTop, mr.right - 18f * u, mr.bottom - 18f * u)
        val rowH = 74f * u
        val gap = 8f * u
        val contentH = structureNodes.size * (rowH + gap)
        maxScroll = max(0f, contentH - listRect.height())
        scrollY = scrollY.coerceIn(0f, maxScroll)

        c.save()
        c.clipRect(listRect)
        c.translate(0f, -scrollY)
        for ((idx, sn) in structureNodes.withIndex()) {
            val top = listRect.top + idx * (rowH + gap)
            val card = RectF(listRect.left, top, listRect.right, top + rowH)
            val dMeters = (dist(player.cx, player.cy, sn.x, sn.y) / OpenWorld.TS).toInt()
            val dir = directionName(sn.x - player.cx, sn.y - player.cy)
            Ui.panel(c, card, if (sn.claimed) Color.GRAY else sn.accentColor, Color.argb(230, 26, 30, 42))
            Ui.icon(c, sn.iconAsset, card.right - 34f * u, card.centerY(), 38f)
            Ui.text(c, sn.nameAr + (if (sn.claimed) " (مكتشف ✓)" else ""), card.right - 66f * u, card.top + 25f * u, 16f, if (sn.claimed) Color.LTGRAY else GOLD, Paint.Align.RIGHT)
            Ui.text(c, "المسافة: $dMeters متر ($dir) • ${sn.descAr}", card.right - 66f * u, card.top + 48f * u, 11.5f, Color.rgb(210, 220, 235), Paint.Align.RIGHT)

            val wpBtn = RectF(card.left + 10f * u, card.centerY() - 20f * u, card.left + 132f * u, card.centerY() + 20f * u)
            val isWp = abs(waypointX - sn.x) < 30f && abs(waypointY - sn.y) < 30f
            Ui.button(c, wpBtn, if (isWp) "محدد بالبوصلة 📍" else "تحديد بالبوصلة 📍", if (isWp) Color.parseColor("#00838F") else Color.parseColor("#1565C0"), true, 13.5f)
            val wpScr = RectF(wpBtn.left, wpBtn.top - scrollY, wpBtn.right, wpBtn.bottom - scrollY)
            if (RectF.intersects(wpScr, listRect)) {
                hits.add(Hit(wpScr, "wmap_wp:$idx"))
            }
        }
        c.restore()
    }

    private fun tapWorldMapModal(id: String?, x: Float, y: Float) {
        val w = ow ?: return
        if (id == null) return
        if (id == "close") {
            Sound.play("click")
            modal = Modal.NONE
            return
        }
        if (id == "wmap_canvas") {
            val u = Ui.u
            val mr = modalRect()
            val contentTop = mr.top + 60f * u
            val mapSize = min(mr.height() - 82f * u, mr.width() * 0.46f)
            val mapLeft = mr.left + 20f * u
            val rx = ((x - mapLeft) / mapSize).coerceIn(0.02f, 0.98f)
            val ry = ((y - contentTop) / mapSize).coerceIn(0.02f, 0.98f)
            waypointX = rx * w.width
            waypointY = ry * w.height
            waypointTitle = "نقطة مخصصة"
            Sound.play("click")
            toast("تم وضع علامة البوصلة على الخريطة 📍!")
            return
        }
        if (id.startsWith("wmap_wp:")) {
            val idx = id.substring(8).toIntOrNull() ?: return
            val sn = structureNodes.getOrNull(idx) ?: return
            waypointX = sn.x
            waypointY = sn.y
            waypointTitle = sn.nameAr
            Sound.play("click")
            toast("تم توجيه البوصلة نحو: ${sn.nameAr} 📍!")
            modal = Modal.NONE
        }
    }

    // ---------------------------------------------------------------- Storage Chest & Silo Modal (الشونة وصندوق التخزين الكبير)
    private fun drawStorageModal(c: Canvas) {
        val u = Ui.u
        dim(c)
        val mr = modalRect()
        Ui.panel(c, mr, GOLD, Color.argb(248, 28, 30, 38), 6f)
        closeButton(c, mr)
        Ui.text(c, "الشونة وصندوق التخزين الكبير (تفريغ الشنطة)", mr.right - 24f * u, mr.top + 42f * u, 25f, GOLD, Paint.Align.RIGHT)
        Ui.text(c, "حمل الشنطة الحالي: ${player.bagUsed()} / ${player.bagMax}", mr.left + 84f * u, mr.top + 42f * u, 17f, Color.parseColor("#69F0AE"), Paint.Align.LEFT)

        val btnEmptyAll = RectF(mr.left + 20f * u, mr.top + 64f * u, mr.centerX() - 8f * u, mr.top + 110f * u)
        Ui.button(c, btnEmptyAll, "تفريغ الشنطة بالكامل في الشونة 📦⬇", Color.parseColor("#2E7D32"), true, 16.5f)
        hits.add(Hit(btnEmptyAll, "st_empty_all"))

        val btnWithdrawAll = RectF(mr.centerX() + 8f * u, mr.top + 64f * u, mr.right - 20f * u, mr.top + 110f * u)
        Ui.button(c, btnWithdrawAll, "سحب كل المخزون للشنطة 📤⬆", Color.parseColor("#0277BD"), true, 16.5f)
        hits.add(Hit(btnWithdrawAll, "st_withdraw_all"))

        listRect = RectF(mr.left + 18f * u, mr.top + 120f * u, mr.right - 18f * u, mr.bottom - 16f * u)
        data class StRow(val key: String, val name: String, val icon: String, val inBag: Int, val inSilo: Int)
        val rows = listOf(
            StRow("wood", "خشب خام", "icons/wood.png", player.wood, player.stWood),
            StRow("stone", "حجر خام (للتحصين)", "icons/stone.png", player.stone, player.stStone),
            StRow("iron", "حديد خام", "icons/iron.png", player.iron, player.stIron),
            StRow("powder", "بارود المدافع 💥", "icons/gunpowder.png", player.gunpowder, player.stGunpowder),
            StRow("wheat", "قمح ومحاصيل الحصاد 🌾", "icons/wheat.png", player.wheat, player.stWheat),
            StRow("seeds", "بذور المحاصيل الزراعية", "icons/seeds.png", player.cropSeeds, player.stSeeds),
            StRow("saplings", "شتلات الأشجار 🌱", "icons/sapling.png", player.treeSaplings, player.stSaplings),
            StRow("wool", "صوف الخراف", "icons/wool.png", player.wool, player.stWool),
            StRow("bwood", "بلوكات ألواح خشبية", "icons/block_wood.png", player.blkWood, player.stBlkWood),
            StRow("bstone", "بلوكات طوب حجري", "icons/block_stone_brick.png", player.blkStone, player.stBlkStone)
        )

        val rowH = 64f * u
        val gap = 8f * u
        val contentH = rows.size * (rowH + gap)
        maxScroll = max(0f, contentH - listRect.height())
        scrollY = scrollY.coerceIn(0f, maxScroll)

        c.save()
        c.clipRect(listRect)
        c.translate(0f, -scrollY)
        for ((i, r) in rows.withIndex()) {
            val top = listRect.top + i * (rowH + gap)
            val card = RectF(listRect.left, top, listRect.right, top + rowH)
            Ui.panel(c, card, Color.rgb(120, 124, 138), Color.argb(230, 36, 38, 48))
            Ui.icon(c, r.icon, card.right - 36f * u, card.centerY(), 36f)
            Ui.text(c, r.name, card.right - 68f * u, card.top + 26f * u, 18f, Color.WHITE, Paint.Align.RIGHT)
            Ui.text(c, "في الشنطة: ${r.inBag}   •   في الشونة: ${r.inSilo}", card.right - 68f * u, card.top + 49f * u, 14.5f, GOLD, Paint.Align.RIGHT)

            val bDep = RectF(card.left + 12f * u, card.centerY() - 20f * u, card.left + 142f * u, card.centerY() + 20f * u)
            Ui.button(c, bDep, "إيداع 5 ⬇", Color.parseColor("#2E7D32"), r.inBag > 0, 15f)
            val bDepScr = RectF(bDep.left, bDep.top - scrollY, bDep.right, bDep.bottom - scrollY)
            if (RectF.intersects(bDepScr, listRect) && r.inBag > 0) hits.add(Hit(bDepScr, "st_dep:${r.key}"))

            val bWit = RectF(card.left + 150f * u, card.centerY() - 20f * u, card.left + 280f * u, card.centerY() + 20f * u)
            Ui.button(c, bWit, "سحب 5 ⬆", Color.parseColor("#1565C0"), r.inSilo > 0, 15f)
            val bWitScr = RectF(bWit.left, bWit.top - scrollY, bWit.right, bWit.bottom - scrollY)
            if (RectF.intersects(bWitScr, listRect) && r.inSilo > 0) hits.add(Hit(bWitScr, "st_wit:${r.key}"))
        }
        c.restore()
    }

    private fun tapStorageModal(id: String?) {
        if (id == null) return
        if (id == "close") {
            Sound.play("click")
            modal = Modal.NONE
            return
        }
        if (id == "st_empty_all") {
            val moved = player.emptyBagToStorage()
            Sound.play("coin")
            toast("تم تفريغ $moved عنصراً من الشنطة إلى الشونة بنجاح! 📦")
            saveProgress()
            return
        }
        if (id == "st_withdraw_all") {
            val got = player.withdrawAllFromStorage()
            Sound.play("buy")
            toast("سحبت $got عنصراً من الشونة إلى شنطتك!")
            saveProgress()
            return
        }
        if (id.startsWith("st_dep:")) {
            val k = id.substring(7)
            when (k) {
                "wood" -> { val n = min(5, player.wood); player.wood -= n; player.stWood += n }
                "stone" -> { val n = min(5, player.stone); player.stone -= n; player.stStone += n }
                "iron" -> { val n = min(5, player.iron); player.iron -= n; player.stIron += n }
                "powder" -> { val n = min(5, player.gunpowder); player.gunpowder -= n; player.stGunpowder += n }
                "wheat" -> { val n = min(5, player.wheat); player.wheat -= n; player.stWheat += n }
                "seeds" -> { val n = min(5, player.cropSeeds); player.cropSeeds -= n; player.stSeeds += n }
                "saplings" -> { val n = min(5, player.treeSaplings); player.treeSaplings -= n; player.stSaplings += n }
                "wool" -> { val n = min(5, player.wool); player.wool -= n; player.stWool += n }
                "bwood" -> { val n = min(5, player.blkWood); player.blkWood -= n; player.stBlkWood += n }
                "bstone" -> { val n = min(5, player.blkStone); player.blkStone -= n; player.stBlkStone += n }
            }
            Sound.play("click")
            saveProgress()
        } else if (id.startsWith("st_wit:")) {
            val k = id.substring(7)
            when (k) {
                "wood" -> { val n = min(5, player.stWood); player.stWood -= n; player.wood += n }
                "stone" -> { val n = min(5, player.stStone); player.stStone -= n; player.stone += n }
                "iron" -> { val n = min(5, player.stIron); player.stIron -= n; player.iron += n }
                "powder" -> { val n = min(5, player.stGunpowder); player.stGunpowder -= n; player.gunpowder += n }
                "wheat" -> { val n = min(5, player.stWheat); player.stWheat -= n; player.wheat += n }
                "seeds" -> { val n = min(5, player.stSeeds); player.stSeeds -= n; player.cropSeeds += n }
                "saplings" -> { val n = min(5, player.stSaplings); player.stSaplings -= n; player.treeSaplings += n }
                "wool" -> { val n = min(5, player.stWool); player.stWool -= n; player.wool += n }
                "bwood" -> { val n = min(5, player.stBlkWood); player.stBlkWood -= n; player.blkWood += n }
                "bstone" -> { val n = min(5, player.stBlkStone); player.stBlkStone -= n; player.blkStone += n }
            }
            Sound.play("click")
            saveProgress()
        }
    }

    // ---------------------------------------------------------------- Professional 4x4 Crafting Table Modal (الكرافتينج تيبل الاحترافي 4×4)
    private fun iconFor4x4Char(ch: Char): String? = when (ch) {
        'W' -> "icons/wood.png"
        'S' -> "icons/stone.png"
        'I' -> "icons/iron.png"
        'P' -> "icons/gunpowder.png"
        'L' -> "icons/wool.png"
        'G' -> "icons/coin.png"
        'M' -> "environment/crystal_green.png"
        'T' -> "environment/torch.png"
        'o' -> "tiles/door.png"
        else -> null
    }

    private fun drawCraft4x4Modal(c: Canvas) {
        val u = Ui.u
        dim(c)
        val mr = modalRect()
        Ui.panel(c, mr, GOLD, Color.argb(248, 32, 34, 42), 6f)
        closeButton(c, mr)
        Ui.text(c, "طاولة الصناعة الاحترافية (شبكة كرافتينج 4×4)", mr.right - 24f * u, mr.top + 42f * u, 25f, GOLD, Paint.Align.RIGHT)

        // Resource summary strip on top-left
        val resTxt = "خشب:${player.wood} • حجر:${player.stone} • حديد:${player.iron} • بارود:${player.gunpowder} • صوف:${player.wool} • ذهب:${player.coins}"
        Ui.text(c, resTxt, mr.left + 78f * u, mr.top + 42f * u, 14.5f, Color.rgb(215, 225, 240), Paint.Align.LEFT)

        // Category Filter Tabs in 4x4 Crafting Table
        val catNames = arrayOf("الكل", "تشييد وفخاخ 🧱", "أسلحة ودروع ⚔️", "بارود وغولم 🤖", "زراعة وسحر ✨")
        var cxTab = mr.right - 20f * u
        val catY = mr.top + 54f * u
        for (ci in catNames.indices) {
            val cwTab = if (ci == 0) 86f * u else 148f * u
            val cr = RectF(cxTab - cwTab, catY, cxTab, catY + 32f * u)
            val isCat = craftCatTab == ci
            Ui.button(c, cr, catNames[ci], if (isCat) Color.parseColor("#F57C00") else Color.parseColor("#37474F"), true, 13.5f, isCat)
            hits.add(Hit(cr, "c4cat:$ci"))
            cxTab -= (cwTab + 8f * u)
        }

        val contentTop = mr.top + 92f * u
        val rightPanelLeft = mr.right - 375f * u

        // Right Panel: Interactive 4x4 Grid (16 Beveled Slots) + Material Brushes + Output Slot & Craft Button
        val gridBox = RectF(rightPanelLeft, contentTop, mr.right - 16f * u, mr.bottom - 16f * u)
        Ui.panel(c, gridBox, Color.rgb(150, 154, 168), Color.argb(240, 22, 24, 30))
        Ui.text(c, "شبكة التصميم 4×4 (16 خانة)", gridBox.centerX(), gridBox.top + 24f * u, 16.5f, GOLD)

        val slotBmp = Assets.bmp("ui/mc_slot.png")
        val selBmp = Assets.bmp("ui/mc_slot_sel.png")
        val cellS = 50f * u
        val cellGap = 5f * u
        val g4W = 4 * cellS + 3 * cellGap
        val g4X = gridBox.left + 16f * u
        val g4Y = gridBox.top + 36f * u

        for (r in 0 until 4) {
            for (col in 0 until 4) {
                val idx = r * 4 + col
                val sx = g4X + col * (cellS + cellGap)
                val sy = g4Y + r * (cellS + cellGap)
                val sr = RectF(sx, sy, sx + cellS, sy + cellS)
                if (slotBmp != null) c.drawBitmap(slotBmp, null, sr, null)
                else Ui.fillRound(c, sr, Color.rgb(100, 102, 112), 4f)
                val ic = iconFor4x4Char(craftGrid4x4[idx])
                if (ic != null) Ui.icon(c, ic, sr.centerX(), sr.centerY(), 32f)
                hits.add(Hit(sr, "c4cell:$idx"))
            }
        }

        // Output Slot & Craft Button on the right of the 4x4 Grid
        val activeRec = CraftingRegistry.recipes.firstOrNull { it.id == selectedId } ?: CraftingRegistry.recipes.first()
        val outX = g4X + g4W + 22f * u
        val outY = g4Y + 46f * u
        val outSlot = RectF(outX, outY, outX + 76f * u, outY + 76f * u)
        if (selBmp != null) c.drawBitmap(selBmp, null, outSlot, null)
        Ui.icon(c, activeRec.iconAsset, outSlot.centerX(), outSlot.centerY(), 46f, activeRec.tint)
        Ui.text(c, "الناتج ⬇", outSlot.centerX(), outSlot.top - 8f * u, 14f, GOLD)

        val st = craftState(activeRec)
        val craftBtn = RectF(outX - 10f * u, outSlot.bottom + 12f * u, gridBox.right - 10f * u, outSlot.bottom + 64f * u)
        val btnLbl = when (st) {
            1 -> "مملوك ✓"
            0 -> "اصنع الآن ⚒️"
            else -> "موارد ناقصة"
        }
        Ui.button(c, craftBtn, btnLbl, if (st == 0) Color.parseColor("#2E7D32") else Color.parseColor("#B71C1C"), st == 0, 15.5f)
        if (st == 0) hits.add(Hit(craftBtn, "c4do:${activeRec.id}"))

        // Material Brush Palette below the 4x4 grid so player can manually paint cells too!
        val palY = g4Y + 4 * (cellS + cellGap) + 10f * u
        val brushes = listOf(
            Pair('W', "icons/wood.png"),
            Pair('S', "icons/stone.png"),
            Pair('I', "icons/iron.png"),
            Pair('P', "icons/gunpowder.png"),
            Pair('L', "icons/wool.png"),
            Pair('.', "ui/ico/x.png")
        )
        var bx = g4X
        for ((ch, ic) in brushes) {
            val br = RectF(bx, palY, bx + 42f * u, palY + 42f * u)
            val isB = activeBrush4x4 == ch
            if (isB && selBmp != null) c.drawBitmap(selBmp, null, br, null)
            else if (slotBmp != null) c.drawBitmap(slotBmp, null, br, null)
            Ui.icon(c, ic, br.centerX(), br.centerY(), 26f)
            hits.add(Hit(br, "c4brush:$ch"))
            bx += 48f * u
        }

        // Selected recipe title & cost details at bottom of right panel
        val infoY = palY + 52f * u
        Ui.text(c, activeRec.titleAr, gridBox.centerX(), infoY + 14f * u, 16f, GOLD)
        Ui.wrapped(c, activeRec.descAr, gridBox.left + 12f * u, infoY + 22f * u, gridBox.width() - 24f * u, 13f, Color.WHITE, false, 2, true)

        // Left Panel: Scrollable 4x4 Blueprint & Recipe Book (Filtered by Category Tab)
        listRect = RectF(mr.left + 16f * u, contentTop, rightPanelLeft - 12f * u, mr.bottom - 16f * u)
        val recipes = if (craftCatTab == 0) CraftingRegistry.recipes else CraftingRegistry.recipes.filter { it.category == craftCatTab - 1 }
        val rowH = 92f * u
        val gap = 8f * u
        val contentH = recipes.size * (rowH + gap)
        maxScroll = max(0f, contentH - listRect.height())
        scrollY = scrollY.coerceIn(0f, maxScroll)

        c.save()
        c.clipRect(listRect)
        c.translate(0f, -scrollY)
        for ((i, r) in recipes.withIndex()) {
            val top = listRect.top + i * (rowH + gap)
            val card = RectF(listRect.left, top, listRect.right, top + rowH)
            val sel = r.id == activeRec.id
            val rst = craftState(r)
            Ui.panel(c, card, if (sel) GOLD else Color.rgb(100, 105, 120), if (sel) Color.argb(240, 44, 52, 78) else Color.argb(225, 28, 32, 44))
            Ui.icon(c, r.iconAsset, card.right - 38f * u, card.centerY(), 42f, r.tint)
            var tStr = r.titleAr
            if (r.resultType == "UPG_WEAPON") tStr += " (+${player.weaponLvl}/10)"
            if (r.resultType == "UPG_ARMOR") tStr += " (+${player.armorLvl}/10)"
            Ui.text(c, tStr, card.right - 72f * u, card.top + 28f * u, 16.5f, if (sel) GOLD else Color.WHITE, Paint.Align.RIGHT)

            val cost = costOf(r)
            val sb = StringBuilder("المطلوب: ")
            if (cost.wood > 0) sb.append("${cost.wood} خشب • ")
            if (cost.stone > 0) sb.append("${cost.stone} حجر • ")
            if (cost.iron > 0) sb.append("${cost.iron} حديد • ")
            if (cost.gunpowder > 0) sb.append("${cost.gunpowder} بارود • ")
            if (cost.wool > 0) sb.append("${cost.wool} صوف • ")
            if (cost.gold > 0) sb.append("${cost.gold} ذهب • ")
            for (mi in 0 until 6) if (cost.mats[mi] > 0) sb.append("${cost.mats[mi]} ${Mats.names[mi]} • ")
            Ui.text(c, sb.toString().removeSuffix(" • "), card.right - 72f * u, card.top + 54f * u, 13f, if (rst == 0) Color.parseColor("#A5D6A7") else Color.parseColor("#EF9A9A"), Paint.Align.RIGHT)
            Ui.text(c, "اضغط لعرض المخطط في شبكة 4×4 ➔", card.right - 72f * u, card.top + 76f * u, 12f, Color.rgb(180, 205, 235), Paint.Align.RIGHT)

            val btn = RectF(card.left + 10f * u, card.centerY() - 22f * u, card.left + 118f * u, card.centerY() + 22f * u)
            Ui.button(c, btn, if (rst == 1) "مملوك ✓" else if (rst == 0) "اصنع ⚒️" else "ناقص", if (rst == 0) Color.parseColor("#F57C00") else Color.parseColor("#455A64"), rst == 0, 15f)

            val cardScr = RectF(card.left, card.top - scrollY, card.right, card.bottom - scrollY)
            val btnScr = RectF(btn.left, btn.top - scrollY, btn.right, btn.bottom - scrollY)
            if (RectF.intersects(cardScr, listRect)) {
                hits.add(Hit(cardScr, "c4sel:${r.id}"))
                if (rst == 0) hits.add(Hit(btnScr, "c4do:${r.id}"))
            }
        }
        c.restore()
    }

    private fun tapCraft4x4Modal(id: String?) {
        if (id == null) return
        if (id == "close") {
            Sound.play("click")
            saveProgress()
            modal = Modal.NONE
            return
        }
        if (id.startsWith("btab:")) {
            bagTab = id.substring(5).toInt()
            scrollY = 0f
            Sound.play("click")
            return
        }
        if (id.startsWith("c4cat:")) {
            craftCatTab = id.substring(6).toIntOrNull() ?: 0
            scrollY = 0f
            Sound.play("click")
            return
        }
        if (id.startsWith("c4brush:")) {
            activeBrush4x4 = id[8]
            Sound.play("click", 0.6f)
            return
        }
        if (id.startsWith("c4cell:")) {
            val idx = id.substring(7).toIntOrNull() ?: return
            if (idx in 0 until 16) {
                craftGrid4x4[idx] = if (craftGrid4x4[idx] == activeBrush4x4) '.' else activeBrush4x4
                Sound.play("click", 0.5f)
                // Auto-match recipe by dominant material in the 4x4 grid!
                val wCnt = craftGrid4x4.count { it == 'W' }
                val sCnt = craftGrid4x4.count { it == 'S' }
                val iCnt = craftGrid4x4.count { it == 'I' }
                val pCnt = craftGrid4x4.count { it == 'P' }
                val matchedId = when {
                    pCnt >= 2 && sCnt >= 1 -> "craft_blk_tnt"
                    iCnt >= 2 && sCnt >= 2 && wCnt >= 1 -> "craft_gunpowder"
                    sCnt >= 12 -> "craft_fort_bp"
                    wCnt >= 8 && sCnt >= 2 -> "craft_house_bp"
                    wCnt >= 6 && iCnt >= 1 -> "craft_storage_chest"
                    sCnt >= 4 -> "craft_blk_stone"
                    wCnt >= 4 -> "craft_blk_wood"
                    else -> null
                }
                if (matchedId != null) selectedId = matchedId
            }
            return
        }
        if (id.startsWith("c4sel:")) {
            val rid = id.substring(6)
            val r = CraftingRegistry.recipes.firstOrNull { it.id == rid } ?: return
            selectedId = r.id
            loadRecipePatternInto4x4(r)
            Sound.play("click")
            return
        }
        if (id.startsWith("c4do:")) {
            val rid = id.substring(5)
            val r = CraftingRegistry.recipes.firstOrNull { it.id == rid } ?: return
            selectedId = r.id
            loadRecipePatternInto4x4(r)
            craftItem(r)
        }
    }

    // ---------------------------------------------------------------- Dedicated Minecraft-Style Furnace Modal (واجهة الفرن المستقلة)
    private fun drawFurnaceModal(c: Canvas) {
        val u = Ui.u
        dim(c)
        val mr = modalRect()
        Ui.panel(c, mr, Color.parseColor("#FF7043"), Color.argb(248, 34, 32, 38), 16f)
        closeButton(c, mr)
        Ui.text(c, "فرن الصهر والطهي (Minecraft Furnace)", mr.right - 24f * u, mr.top + 44f * u, 27f, Color.parseColor("#FFAB40"), Paint.Align.RIGHT)

        // Top visual diagram of Input + Flame + Output slots
        val diagY = mr.top + 66f * u
        val cx = mr.centerX()
        val slotBmp = Assets.bmp("ui/mc_slot.png")
        val inSlot = RectF(cx - 170f * u, diagY, cx - 110f * u, diagY + 60f * u)
        val fuelSlot = RectF(cx - 30f * u, diagY, cx + 30f * u, diagY + 60f * u)
        val outSlot = RectF(cx + 110f * u, diagY, cx + 170f * u, diagY + 60f * u)
        if (slotBmp != null) {
            c.drawBitmap(slotBmp, null, inSlot, null)
            c.drawBitmap(slotBmp, null, fuelSlot, null)
            c.drawBitmap(slotBmp, null, outSlot, null)
        }
        Ui.icon(c, "icons/raw_meat.png", inSlot.centerX(), inSlot.centerY(), 34f)
        Ui.icon(c, "environment/fire.png", fuelSlot.centerX(), fuelSlot.centerY(), 40f)
        Ui.icon(c, "icons/cooked_meat.png", outSlot.centerX(), outSlot.centerY(), 36f)
        Ui.text(c, "+", cx - 70f * u, diagY + 38f * u, 26f, GOLD)
        Ui.text(c, "➔", cx + 70f * u, diagY + 38f * u, 26f, GOLD)

        listRect = RectF(mr.left + 18f * u, diagY + 72f * u, mr.right - 18f * u, mr.bottom - 16f * u)
        data class SmeltEntry(val id: String, val title: String, val desc: String, val inIcon: String, val outIcon: String, val ok: Boolean)
        val recipes = listOf(
            SmeltEntry("f_meat", "شوي كل اللحم النيء (${player.rawMeat})", "يشوي كل اللحم النيء فوراً إلى لحم مشوي ساخن (+45 شبع).", "icons/raw_meat.png", "icons/cooked_meat.png", player.rawMeat > 0),
            SmeltEntry("f_stew", "طهي حساء الطاقة الملكي (x2)", "يتطلب (1 لحم نيء + 1 تفاحة) ➔ يعطي 2 حساء طاقة (+75 شبع و+35 صحة).", "icons/apple.png", "icons/stew.png", player.rawMeat >= 1 && player.apples >= 1),
            SmeltEntry("f_gold", "صهر الحديد والحجر إلى ذهب وصلب", "يتطلب (2 حديد + 2 حجر) ➔ يعطي +60 ذهب خالص + 1 صلب الحصن.", "icons/iron.png", "icons/coin.png", player.iron >= 2 && player.stone >= 2),
            SmeltEntry("f_glass", "صهر الصخور إلى زجاج بلوري (x6)", "يتطلب (2 حجر + 1 خشب) ➔ يعطي 6 بلوكات زجاج بلوري للبناء.", "icons/stone.png", "icons/block_glass.png", player.stone >= 2 && player.wood >= 1),
            SmeltEntry("f_tnt", "تقطير البارود ومتفجرات TNT (x3)", "يتطلب (2 حجر + 1 حديد + 15 ذهب) ➔ يعطي 3 بلوكات متفجرات TNT.", "icons/bomb.png", "icons/block_tnt.png", player.stone >= 2 && player.iron >= 1 && player.coins >= 15)
        )

        val rowH = 86f * u
        val gap = 10f * u
        val contentH = recipes.size * (rowH + gap)
        maxScroll = max(0f, contentH - listRect.height())
        scrollY = scrollY.coerceIn(0f, maxScroll)

        c.save()
        c.clipRect(listRect)
        c.translate(0f, -scrollY)
        for ((i, r) in recipes.withIndex()) {
            val top = listRect.top + i * (rowH + gap)
            val card = RectF(listRect.left, top, listRect.right, top + rowH)
            cardBase(c, card, false, Color.parseColor("#FF7043"))
            val boxLeft = card.right - 84f * u
            Ui.icon(c, r.outIcon, boxLeft + 36f * u, card.top + 43f * u, 46f)
            Ui.text(c, r.title, boxLeft - 14f * u, card.top + 32f * u, 19.5f, Color.WHITE, Paint.Align.RIGHT)
            Ui.text(c, r.desc, boxLeft - 14f * u, card.top + 60f * u, 14f, Color.rgb(210, 215, 230), Paint.Align.RIGHT)
            val btn = RectF(card.left + 14f * u, card.centerY() - 24f * u, card.left + 160f * u, card.centerY() + 24f * u)
            Ui.button(c, btn, if (r.ok) "اصهر / اطبخ 🔥" else "موارد ناقصة", Color.parseColor("#E65100"), r.ok, 17f)
            val br = RectF(btn.left, btn.top - scrollY, btn.right, btn.bottom - scrollY)
            if (RectF.intersects(br, listRect) && r.ok) hits.add(Hit(br, r.id))
        }
        c.restore()
    }

    private fun tapFurnaceModal(id: String?) {
        if (id == null) return
        if (id == "close") {
            Sound.play("click")
            modal = Modal.NONE
            return
        }
        when (id) {
            "f_meat" -> if (player.rawMeat > 0) {
                val n = player.rawMeat
                player.rawMeat = 0
                player.cookedMeat += n
                Sound.play("potion")
                toast("الفرن: شويت $n قطعة لحم مشوي!")
                advanceQuest("q_cook", n)
                saveProgress()
            }
            "f_stew" -> if (player.rawMeat >= 1 && player.apples >= 1) {
                player.rawMeat -= 1
                player.apples -= 1
                player.stew += 2
                Sound.play("potion")
                toast("الفرن: طهوت 2 حساء طاقة ملكي!")
                advanceQuest("q_cook", 2)
                saveProgress()
            }
            "f_gold" -> if (player.iron >= 2 && player.stone >= 2) {
                player.iron -= 2
                player.stone -= 2
                player.coins += 60
                player.mats[0] += 1
                Sound.play("coin")
                furnaceSmeltCount++
                if (furnaceSmeltCount >= 3 && player.unlockSeal(4)) {
                    advanceQuest("q_seals", 1)
                    toast("أتقنت الصهر في الفرن وحصلت على ختم سيد الصهر! (${player.abyssalSeals}/7)")
                } else {
                    toast("الفرن: صهرت الخامات إلى +60 ذهب و+1 صلب الحصن!")
                }
                advanceQuest("q_cook", 1)
                saveProgress()
            }
            "f_glass" -> if (player.stone >= 2 && player.wood >= 1) {
                player.stone -= 2
                player.wood -= 1
                player.blkGlass += 6
                Sound.play("mine")
                toast("الفرن: صنعت +6 بلوكات زجاج بلوري للبناء!")
                advanceQuest("q_cook", 1)
                saveProgress()
            }
            "f_tnt" -> if (player.stone >= 2 && player.iron >= 1 && player.coins >= 15) {
                player.stone -= 2
                player.iron -= 1
                player.coins -= 15
                player.blkTnt += 3
                Sound.play("buy")
                toast("الفرن: صنعت +3 بلوكات متفجرات TNT!")
                advanceQuest("q_cook", 1)
                saveProgress()
            }
        }
    }

    // ---------------------------------------------------------------- Minecraft-Style Villager Multi-Trade & Steal Modal
    private fun drawVillagerModal(c: Canvas) {
        val u = Ui.u
        val v = targetVillager ?: return
        dim(c)
        val mr = modalRect()
        Ui.panel(c, mr, v.role.color, Color.argb(248, 28, 32, 42), 16f)
        closeButton(c, mr)
        Ui.text(c, "${v.nameAr} • ${v.role.titleAr} (المستوى ${v.level}/5)", mr.right - 24f * u, mr.top + 44f * u, 25f, GOLD, Paint.Align.RIGHT)

        // Villager XP Bar + Steal Button on top bar
        val xpBar = RectF(mr.right - 340f * u, mr.top + 56f * u, mr.right - 24f * u, mr.top + 70f * u)
        Ui.bar(c, xpBar, v.xp.toFloat() / max(1, v.level * 2), Color.parseColor("#69F0AE"))

        val stealChance = if (player.owned.contains("dagger_silver")) 80 else 60
        val canSteal = v.stealCd <= 0f
        val rSteal = RectF(mr.left + 80f * u, mr.top + 14f * u, mr.left + 350f * u, mr.top + 58f * u)
        Ui.button(
            c, rSteal,
            if (canSteal) "سرقة الجيب ($stealChance%)" else "القروي حذر حالياً",
            Color.parseColor("#C62828"), canSteal, 16.5f
        )
        if (canSteal) hits.add(Hit(rSteal, "v_steal"))

        listRect = RectF(mr.left + 18f * u, mr.top + 82f * u, mr.right - 18f * u, mr.bottom - 16f * u)
        val bonus = v.level - 1
        data class TradeOffer(val id: String, val giveTitle: String, val getTitle: String, val giveIcon: String, val getIcon: String, val ok: Boolean)
        val offers = listOf(
            TradeOffer("t_special", v.tradeGiveText, "${v.tradeGetText} (+مكافأة المستوى $bonus)", "icons/wool.png", "icons/coin.png", true),
            TradeOffer("t_powder", "30 ذهب (أو 2 حديد)", "${10 + bonus * 2} بارود المدافع المتفجر 💥", "icons/coin.png", "icons/gunpowder.png", player.coins >= 30 || player.iron >= 2),
            TradeOffer("t_seeds", "15 ذهب", "${5 + bonus} بذور محاصيل + ${3 + bonus} شتلات شجر 🌱", "icons/coin.png", "icons/seeds.png", player.coins >= 15),
            TradeOffer("t_blocks", "3 خشب خام + 2 حجر", "10 بلوكات بناء مشكلة (خشب وطوب وجسور)", "icons/wood.png", "icons/block_wood.png", player.wood >= 3 && player.stone >= 2),
            TradeOffer("t_stew", "35 ذهب", "${2 + bonus} حساء الطاقة الساخن + ${2 + bonus} تفاح", "icons/coin.png", "icons/stew.png", player.coins >= 35),
            TradeOffer("t_key", "3 حديد + 30 ذهب", "1 مفتاح ذهبي + 2 قنبلة", "icons/iron.png", "icons/key_gold.png", player.iron >= 3 && player.coins >= 30),
            TradeOffer("t_blacksmith", "فتح ورشة الحدادة والصناعة", "صقل الأسلحة والدروع وصناعة البلوكات", "ui/ico/axe.png", "icons/sabre.png", true)
        )

        val rowH = 84f * u
        val gap = 10f * u
        val contentH = offers.size * (rowH + gap)
        maxScroll = max(0f, contentH - listRect.height())
        scrollY = scrollY.coerceIn(0f, maxScroll)

        c.save()
        c.clipRect(listRect)
        c.translate(0f, -scrollY)
        for ((i, o) in offers.withIndex()) {
            val top = listRect.top + i * (rowH + gap)
            val card = RectF(listRect.left, top, listRect.right, top + rowH)
            cardBase(c, card, false, v.role.color)
            val boxLeft = card.right - 84f * u
            Ui.icon(c, o.getIcon, boxLeft + 36f * u, card.top + 42f * u, 44f)
            Ui.text(c, "${o.giveTitle}   ➔   ${o.getTitle}", boxLeft - 14f * u, card.top + 36f * u, 19f, Color.WHITE, Paint.Align.RIGHT)
            Ui.text(c, "اضغط لإتمام المقايضة ورفع مستوى القروي", boxLeft - 14f * u, card.top + 62f * u, 13.5f, Color.rgb(190, 210, 225), Paint.Align.RIGHT)
            val btn = RectF(card.left + 14f * u, card.centerY() - 24f * u, card.left + 160f * u, card.centerY() + 24f * u)
            Ui.button(c, btn, if (o.id == "t_blacksmith") "الحدادة ⚒️" else "مقايضة 🤝", Color.parseColor("#2E7D32"), o.ok, 17f)
            val br = RectF(btn.left, btn.top - scrollY, btn.right, btn.bottom - scrollY)
            if (RectF.intersects(br, listRect) && o.ok) hits.add(Hit(br, o.id))
        }
        c.restore()
    }

    private fun tapVillagerModal(id: String?) {
        val v = targetVillager ?: return
        when (id) {
            "close" -> {
                Sound.play("click")
                modal = Modal.NONE
            }
            "t_blacksmith" -> openCraft()
            "t_special" -> {
                var ok = false
                val bonus = v.level - 1
                when (v.tradeId) {
                    "wool_gold" -> if (player.wool >= 2) { player.wool -= 2; player.coins += 45 + bonus * 8; ok = true }
                    "meat_stew" -> if (player.rawMeat >= 2) { player.rawMeat -= 2; player.stew += 2 + bonus; ok = true }
                    "iron_key" -> if (player.iron >= 3) { player.iron -= 3; player.goldKeys += 1; player.coins += bonus * 10; ok = true }
                    "wood_pot" -> if (player.wood >= 4) { player.wood -= 4; player.potions += 2 + bonus; ok = true }
                    "gold_mat" -> if (player.coins >= 55) { player.coins -= 55; val b = (ow?.biomeAtPx(v.x, v.y) ?: 0).coerceIn(0, 5); player.mats[b] += 2 + bonus; ok = true }
                    "stone_bomb" -> if (player.stone >= 4) { player.stone -= 4; player.bombs += 3 + bonus; ok = true }
                }
                if (ok) {
                    val reached5 = v.addTradeXp()
                    Sound.play("buy")
                    if (reached5 && player.unlockSeal(3)) {
                        advanceQuest("q_seals", 1)
                        toast("وصل القروي للمستوى 5 ومنحك ختم الحكيم الأعظم! (${player.abyssalSeals}/7)")
                    } else {
                        toast("تم التبادل بنجاح مع ${v.nameAr}! (مستوى القروي: ${v.level})")
                    }
                    advanceQuest("q_trade", 1)
                    saveProgress()
                } else {
                    Sound.play("deny")
                    toast("لا تملك الموارد المطلوبة لهذا العرض")
                }
            }
            "t_powder" -> if (player.coins >= 30 || player.iron >= 2) {
                val bonus = v.level - 1
                if (player.coins >= 30) player.coins -= 30 else player.iron -= 2
                player.gunpowder += 10 + bonus * 2
                v.addTradeXp()
                Sound.play("buy")
                toast("اشتريت بارود المدافع من القروي! املأ به مدافع قلعتك 💥")
                advanceQuest("q_powder", 1)
                advanceQuest("q_trade", 1)
                saveProgress()
            }
            "t_seeds" -> if (player.coins >= 15) {
                val bonus = v.level - 1
                player.coins -= 15
                player.cropSeeds += 5 + bonus
                player.treeSaplings += 3 + bonus
                v.addTradeXp()
                Sound.play("buy")
                toast("اشتريت بذور محاصيل وشتلات شجر للزراعة اليدوية! 🌱")
                advanceQuest("q_trade", 1)
                saveProgress()
            }
            "t_blocks" -> if (player.wood >= 3 && player.stone >= 2) {
                player.wood -= 3
                player.stone -= 2
                player.blkWood += 4
                player.blkStone += 4
                player.blkBridge += 2
                v.addTradeXp()
                Sound.play("buy")
                toast("حصلت على 10 بلوكات بناء من القروي!")
                advanceQuest("q_trade", 1)
                saveProgress()
            }
            "t_stew" -> if (player.coins >= 35) {
                val bonus = v.level - 1
                player.coins -= 35
                player.stew += 2 + bonus
                player.apples += 2 + bonus
                v.addTradeXp()
                Sound.play("buy")
                toast("اشتريت وجبات حساء وتفاح طازج!")
                advanceQuest("q_trade", 1)
                saveProgress()
            }
            "t_key" -> if (player.iron >= 3 && player.coins >= 30) {
                player.iron -= 3
                player.coins -= 30
                player.goldKeys += 1
                player.bombs += 2
                v.addTradeXp()
                Sound.play("buy")
                toast("قايضت الحديد بـ 1 مفتاح ذهبي و2 قنبلة!")
                advanceQuest("q_trade", 1)
                saveProgress()
            }
            "v_steal" -> {
                if (v.stealCd > 0f) return
                v.stealCd = 45f
                val chance = if (player.owned.contains("dagger_silver")) 0.80f else 0.60f
                if (rnd.nextFloat() < chance) {
                    val stolenGold = 35 + rnd.nextInt(45)
                    player.coins += stolenGold
                    if (rnd.nextFloat() < 0.35f) player.goldKeys++
                    Sound.play("coin")
                    toast("نجحت السرقة! حصلت على +$stolenGold ذهب بدون أن يشعر")
                    advanceQuest("q_trade", 1)
                    modal = Modal.NONE
                } else {
                    Sound.play("boss")
                    shake = 10f
                    toast("فشلت السرقة! حراس القرية يطاردونك!")
                    modal = Modal.NONE
                    for (k in 0..1) {
                        val gx = v.x + (if (k == 0) -90f else 90f)
                        val gy = v.y + 40f
                        val guard = Enemy(gx, gy, EnemyRole.SPEARMAN, EraType.ERA_1_MEDIEVAL)
                        guard.aggro = true
                        enemies.add(guard)
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- Fast Travel Modal (البوابات الزمنية والمخيمات)
    private fun drawTravelModal(c: Canvas) {
        val u = Ui.u
        dim(c)
        val mr = modalRect()
        Ui.panel(c, mr, Color.parseColor("#00E5FF"), Color.argb(246, 14, 20, 38), 16f)
        closeButton(c, mr)
        Ui.text(c, "شبكة البوابات الزمنية والمخيمات (سفر سريع)", mr.right - 24f * u, mr.top + 46f * u, 26f, Color.parseColor("#80DEEA"), Paint.Align.RIGHT)

        listRect = RectF(mr.left + 16f * u, mr.top + 74f * u, mr.right - 16f * u, mr.bottom - 16f * u)
        data class Dest(val name: String, val sub: String, val x: Float, val y: Float, val icon: String)
        val dests = ArrayList<Dest>()
        val w = ow ?: return
        dests.add(Dest("قرية الزمن المركزية", "المركز الآمن • المتجر والحداد والفرن", w.spawnX, w.spawnY, "environment/magic_circle.png"))
        for ((idx, v) in w.subVillageTiles.withIndex()) {
            dests.add(Dest("قرية ${biomeNames[idx]}", "بوابة القرية الفرعية والقرويين", v.first * OpenWorld.TS, (v.second + 2) * OpenWorld.TS, "environment/magic_circle2.png"))
        }
        for (cp in campfires) {
            if (cp.isLit) dests.add(Dest(cp.nameAr, "نقطة مخيم مضيئة", cp.x, cp.y + 48f, "environment/bonefire.png"))
        }

        val rowH = 86f * u
        val gap = 10f * u
        val contentH = dests.size * (rowH + gap)
        maxScroll = max(0f, contentH - listRect.height())
        scrollY = scrollY.coerceIn(0f, maxScroll)

        c.save()
        c.clipRect(listRect)
        c.translate(0f, -scrollY)
        for ((i, d) in dests.withIndex()) {
            val top = listRect.top + i * (rowH + gap)
            val card = RectF(listRect.left, top, listRect.right, top + rowH)
            cardBase(c, card, false, Color.parseColor("#00BCD4"))
            val boxLeft = card.right - 84f * u
            Ui.icon(c, d.icon, boxLeft + 36f * u, card.top + 43f * u, 46f)
            Ui.text(c, d.name, boxLeft - 14f * u, card.top + 32f * u, 20f, Color.WHITE, Paint.Align.RIGHT)
            Ui.text(c, d.sub, boxLeft - 14f * u, card.top + 60f * u, 15f, Color.rgb(180, 210, 230), Paint.Align.RIGHT)
            val btn = RectF(card.left + 14f * u, card.centerY() - 24f * u, card.left + 164f * u, card.centerY() + 24f * u)
            Ui.button(c, btn, "انتقل فوراً", Color.parseColor("#00838F"), true, 18f)
            val br = RectF(btn.left, btn.top - scrollY, btn.right, btn.bottom - scrollY)
            if (RectF.intersects(br, listRect)) hits.add(Hit(br, "tp:$i"))
        }
        c.restore()
    }

    private fun tapTravelModal(id: String?) {
        if (id == null) return
        if (id == "close") {
            Sound.play("click")
            modal = Modal.NONE
            return
        }
        if (id.startsWith("tp:")) {
            val idx = id.substring(3).toIntOrNull() ?: return
            val w = ow ?: return
            val coords = ArrayList<Pair<Float, Float>>()
            val names = ArrayList<String>()
            coords.add(Pair(w.spawnX, w.spawnY))
            names.add("قرية الزمن المركزية")
            for ((bi, v) in w.subVillageTiles.withIndex()) {
                coords.add(Pair(v.first * OpenWorld.TS, (v.second + 2) * OpenWorld.TS))
                names.add("قرية ${biomeNames[bi]}")
            }
            for (cp in campfires) {
                if (cp.isLit) {
                    coords.add(Pair(cp.x, cp.y + 48f))
                    names.add(cp.nameAr)
                }
            }
            if (idx in coords.indices) {
                player.x = coords[idx].first
                player.y = coords[idx].second
                camera.snapTo(player.cx, player.cy)
                w.warm(player.cx, player.cy)
                w.reveal(player.cx, player.cy, 20)
                fade = 1f
                modal = Modal.NONE
                Sound.play("portal")
                toast("انتقلت عبر الزمن إلى: ${names[idx]}")
            }
        }
    }

    private fun drawPause(c: Canvas) {
        val u = Ui.u
        dim(c)
        val w = curW.toFloat()
        val h = curH.toFloat()
        val pr = RectF(w / 2f - 250f * u, h / 2f - 310f * u, w / 2f + 250f * u, h / 2f + 310f * u)
        Ui.panel(c, pr, Color.rgb(175, 178, 188), Color.argb(248, 34, 36, 44), 6f)
        Ui.text(c, "قائمة اللعبة (Minecraft UI)", pr.centerX(), pr.top + 44f * u, 27f, GOLD)
        val bw = 410f * u
        var y = pr.top + 64f * u
        val items = listOf(
            Triple("resume", "العودة إلى اللعب ▶", Color.parseColor("#2E7D32")),
            Triple("multiplayer", "اللعب الجماعي مع صديقك (واي فاي / نقطة اتصال) 🌐🤝", Color.parseColor("#00897B")),
            Triple("hud_edit", "تخصيص أماكن وحجم الأزرار (HUD فورتنايت) ⚙️", Color.parseColor("#0277BD")),
            Triple("boss_altar", "مسار التختيم والزعيم العملاق (الأختام ${player.abyssalSeals}/7) 💀", Color.parseColor("#6A1B9A")),
            Triple("bag", "الحقيبة الشبكية وصندوق البناء 🎒", Color.parseColor("#8D6E63")),
            Triple("sfx", "المؤثرات: " + (if (Sound.sfxOn) "مفعّلة" else "مغلقة"), Color.parseColor("#455A64")),
            Triple("music", "الموسيقى: " + (if (Sound.musicOn) "مفعّلة" else "مغلقة"), Color.parseColor("#455A64")),
            Triple("quality", "الأداء: " + (if (lowQuality) "سريع (أجهزة ضعيفة)" else "جودة فائقة"), Color.parseColor("#00838F")),
            Triple("title", "حفظ والعودة للقائمة الرئيسية", Color.parseColor("#C62828"))
        )
        for (item in items) {
            val r = RectF(pr.centerX() - bw / 2f, y, pr.centerX() + bw / 2f, y + 43f * u)
            Ui.button(c, r, item.second, item.third, true, 16.5f)
            hits.add(Hit(r, item.first))
            y += 49f * u
        }
    }

    // ---------------------------------------------------------------- HUD Customization Editor (تخصيص أماكن الأزرار بستايل فورتنايت)
    private fun applyHudPreset(preset: Int) {
        val w = curW.toFloat()
        val h = curH.toFloat()
        val u = Ui.u
        for (b in allHudButtons) b.resetCustom()
        joystick.resetCustom()
        layoutUi()
        if (preset == 1) {
            // Fortnite Pro Combat + Build layout: ergonomic arc around attack button
            attackBtn.cx = w - 105f * u; attackBtn.cy = h - 115f * u; attackBtn.customScale = 1.15f
            turboWallBtn.cx = w - 215f * u; turboWallBtn.cy = h - 185f * u; turboWallBtn.customScale = 1.12f
            editBlockBtn.cx = w - 185f * u; editBlockBtn.cy = h - 285f * u; editBlockBtn.customScale = 1.08f
            parryBtn.cx = w - 228f * u; parryBtn.cy = h - 82f * u; parryBtn.customScale = 1.08f
            grappleBtn.cx = w - 300f * u; grappleBtn.cy = h - 265f * u; grappleBtn.customScale = 1.05f
            dashBtn.cx = w - 328f * u; dashBtn.cy = h - 82f * u
            magicBtn.cx = w - 88f * u; magicBtn.cy = h - 245f * u
            buildBtn.cx = w - 315f * u; buildBtn.cy = h - 175f * u
        } else if (preset == 2) {
            // 4-Finger Claw HUD: Build, Edit, Parry & Magic placed in top claw zones!
            turboWallBtn.cx = w - 115f * u; turboWallBtn.cy = 145f * u; turboWallBtn.customScale = 1.22f
            editBlockBtn.cx = w - 220f * u; editBlockBtn.cy = 145f * u; editBlockBtn.customScale = 1.18f
            buildBtn.cx = w - 320f * u; buildBtn.cy = 145f * u; buildBtn.customScale = 1.12f
            parryBtn.cx = 120f * u; parryBtn.cy = 225f * u; parryBtn.customScale = 1.18f
            grappleBtn.cx = 225f * u; grappleBtn.cy = 225f * u; grappleBtn.customScale = 1.15f
            attackBtn.cx = w - 115f * u; attackBtn.cy = h - 125f * u; attackBtn.customScale = 1.20f
            dashBtn.cx = w - 245f * u; dashBtn.cy = h - 105f * u; dashBtn.customScale = 1.10f
            magicBtn.cx = w - 105f * u; magicBtn.cy = h - 260f * u; magicBtn.customScale = 1.12f
        }
        for (b in allHudButtons) b.saveCustom(w, h)
        joystick.saveCustom(w, h)
    }

    private fun drawHudEditModal(c: Canvas) {
        val u = Ui.u
        val w = curW.toFloat()
        val h = curH.toFloat()
        Ui.fillRect(c, 0f, 0f, w, h, Color.argb(195, 10, 14, 24))
        // Draw alignment grid
        screenPaint.style = Paint.Style.STROKE
        screenPaint.strokeWidth = 1f
        screenPaint.color = Color.argb(45, 120, 180, 255)
        var gx = 0f
        while (gx < w) {
            c.drawLine(gx, 0f, gx, h, screenPaint)
            gx += 48f * u
        }
        var gy = 0f
        while (gy < h) {
            c.drawLine(0f, gy, w, gy, screenPaint)
            gy += 48f * u
        }
        screenPaint.style = Paint.Style.FILL

        // Draw all HUD buttons and joystick so user can see and drag any of them
        joystick.draw(c, hudSelectedKey == "joy")
        for (b in allHudButtons) {
            b.draw(c, true, hudSelectedKey == b.hudKey)
        }
        drawMinecraftHotbar(c)

        // Top control panel
        val topBar = RectF(16f * u, 10f * u, w - 16f * u, 118f * u)
        Ui.panel(c, topBar, GOLD, Color.argb(242, 26, 28, 36), 6f)
        val selBtn = allHudButtons.firstOrNull { it.hudKey == hudSelectedKey }
        val selName = if (hudSelectedKey == "joy") "عصا الحركة (Joystick)" else (selBtn?.label ?: "زر")
        val selScale = ((if (hudSelectedKey == "joy") joystick.customScale else (selBtn?.customScale ?: 1f)) * 100f).toInt()
        val selAlpha = ((if (hudSelectedKey == "joy") joystick.customAlpha else (selBtn?.customAlpha ?: 0.9f)) * 100f).toInt()
        Ui.text(
            c, "اسحب أي زر بإصبعك لتغيير مكانه! • المحدد: $selName (الحجم: $selScale% • الشفافية: $selAlpha%)",
            topBar.centerX(), topBar.top + 28f * u, 16.5f, GOLD
        )

        val rowY = topBar.top + 42f * u
        val bh = 32f * u
        fun topBtn(x0: Float, bw: Float, label: String, col: Int, id: String) {
            val r = RectF(x0, rowY, x0 + bw, rowY + bh)
            Ui.button(c, r, label, col, true, 14f)
            hits.add(Hit(r, id))
        }
        var bx = topBar.left + 12f * u
        topBtn(bx, 110f * u, "حفظ وخروج ✓", Color.parseColor("#2E7D32"), "hud_save"); bx += 118f * u
        topBtn(bx, 96f * u, "تكبير +", Color.parseColor("#1565C0"), "hud_s_up"); bx += 104f * u
        topBtn(bx, 96f * u, "تصغير -", Color.parseColor("#1565C0"), "hud_s_dn"); bx += 104f * u
        topBtn(bx, 104f * u, "شفافية +/-", Color.parseColor("#00838F"), "hud_alpha"); bx += 112f * u
        topBtn(bx, 140f * u, "وضع فورتنايت ⚡", Color.parseColor("#6A1B9A"), "hud_p_fn"); bx += 148f * u
        topBtn(bx, 140f * u, "محترف 4 أصابع 🖐️", Color.parseColor("#E65100"), "hud_p_claw"); bx += 148f * u
        topBtn(bx, 115f * u, "إعادة الضبط ↺", Color.parseColor("#C62828"), "hud_reset")

        Ui.text(
            c, "نصيحة: زر «جدار فوري» يبني 3 جدران حماية بضغطة واحدة، وزر «تعديل/نافذة» يفتح نافذة قنص في الجدار أو يضع منصة قفز!",
            topBar.centerX(), topBar.bottom - 10f * u, 13.5f, Color.rgb(200, 225, 255)
        )
    }

    private fun hudEditTouch(t: Touch) {
        val w = curW.toFloat()
        val h = curH.toFloat()
        when (t.type) {
            0 -> {
                val hit = hitAt(t.x, t.y)
                if (hit != null) {
                    Sound.play("click")
                    val selBtn = allHudButtons.firstOrNull { it.hudKey == hudSelectedKey }
                    when (hit) {
                        "hud_save" -> {
                            for (b in allHudButtons) b.saveCustom(w, h)
                            joystick.saveCustom(w, h)
                            toast("تم حفظ تصميم أزرار التحكم الاحترافي!")
                            modal = Modal.NONE
                        }
                        "hud_s_up" -> {
                            if (hudSelectedKey == "joy") {
                                joystick.customScale = (joystick.customScale + 0.10f).coerceIn(0.65f, 1.55f)
                                joystick.saveCustom(w, h)
                            } else if (selBtn != null) {
                                selBtn.customScale = (selBtn.customScale + 0.10f).coerceIn(0.65f, 1.55f)
                                selBtn.saveCustom(w, h)
                            }
                        }
                        "hud_s_dn" -> {
                            if (hudSelectedKey == "joy") {
                                joystick.customScale = (joystick.customScale - 0.10f).coerceIn(0.65f, 1.55f)
                                joystick.saveCustom(w, h)
                            } else if (selBtn != null) {
                                selBtn.customScale = (selBtn.customScale - 0.10f).coerceIn(0.65f, 1.55f)
                                selBtn.saveCustom(w, h)
                            }
                        }
                        "hud_alpha" -> {
                            if (hudSelectedKey == "joy") {
                                joystick.customAlpha = if (joystick.customAlpha <= 0.5f) 1.0f else (joystick.customAlpha - 0.18f).coerceAtLeast(0.4f)
                                joystick.saveCustom(w, h)
                            } else if (selBtn != null) {
                                selBtn.customAlpha = if (selBtn.customAlpha <= 0.5f) 1.0f else (selBtn.customAlpha - 0.18f).coerceAtLeast(0.4f)
                                selBtn.saveCustom(w, h)
                            }
                        }
                        "hud_p_fn" -> {
                            applyHudPreset(1)
                            toast("تم تفعيل وضع فورتنايت السريع (بناء + تعديل + قتال)!")
                        }
                        "hud_p_claw" -> {
                            applyHudPreset(2)
                            toast("تم تفعيل وضع محترفي 4 أصابع (Claw HUD)!")
                        }
                        "hud_reset" -> {
                            applyHudPreset(0)
                            toast("تمت استعادة أماكن الأزرار الافتراضية")
                        }
                    }
                    return
                }
                // Check if user touched a HUD button or joystick to drag it
                for (i in allHudButtons.indices.reversed()) {
                    val b = allHudButtons[i]
                    if (b.hitTest(t.x, t.y)) {
                        hudSelectedKey = b.hudKey
                        hudDraggingKey = b.hudKey
                        Sound.play("click", 0.5f)
                        return
                    }
                }
                if (joystick.hitTestHome(t.x, t.y)) {
                    hudSelectedKey = "joy"
                    hudDraggingKey = "joy"
                    Sound.play("click", 0.5f)
                }
            }
            1 -> {
                val drag = hudDraggingKey ?: return
                val ny = t.y.coerceIn(135f * Ui.u, h - 45f * Ui.u)
                val nx = t.x.coerceIn(45f * Ui.u, w - 45f * Ui.u)
                if (drag == "joy") {
                    joystick.homeX = nx
                    joystick.homeY = ny
                } else {
                    val b = allHudButtons.firstOrNull { it.hudKey == drag }
                    if (b != null) {
                        b.cx = nx
                        b.cy = ny
                    }
                }
            }
            2, 3 -> {
                if (hudDraggingKey != null) {
                    for (b in allHudButtons) if (b.hudKey == hudDraggingKey) b.saveCustom(w, h)
                    if (hudDraggingKey == "joy") joystick.saveCustom(w, h)
                    hudDraggingKey = null
                }
            }
        }
    }

    // ---------------------------------------------------------------- Colossal Boss Altar & Week-Long Progression Modal
    private fun drawBossAltarModal(c: Canvas) {
        val u = Ui.u
        dim(c)
        val mr = modalRect()
        Ui.panel(c, mr, Color.parseColor("#FF1744"), Color.argb(248, 24, 18, 34), 6f)
        closeButton(c, mr)
        Ui.text(c, "مذبح الزعيم الأسطوري العملاق ومسار التختيم", mr.right - 24f * u, mr.top + 42f * u, 25f, Color.parseColor("#FF5252"), Paint.Align.RIGHT)

        val ready = player.readinessPercent()
        val statsTxt = "أختام الهاوية: ${player.abyssalSeals}/7  •  الجاهزية القتالية: $ready%  •  قتلى الزومبي: ${player.zombiesKilled}  •  صقل السلاح: +${player.weaponLvl}/10"
        Ui.text(c, statsTxt, mr.centerX(), mr.top + 72f * u, 15.5f, GOLD)

        val barR = RectF(mr.left + 24f * u, mr.top + 82f * u, mr.right - 24f * u, mr.top + 98f * u)
        Ui.bar(c, barR, ready / 100f, if (ready >= 85) Color.parseColor("#69F0AE") else Color.parseColor("#FF7043"))

        val bottomBtnsTop = mr.bottom - 68f * u
        listRect = RectF(mr.left + 18f * u, mr.top + 108f * u, mr.right - 18f * u, bottomBtnsTop - 10f * u)

        data class SealStep(val bit: Int, val title: String, val desc: String)
        val sealSteps = listOf(
            SealStep(0, "1. ختم الأعماق السحيقة", "انزل إلى الطابق 3 في أي كهف مظلم واهزم زعيم الأعماق."),
            SealStep(1, "2. ختم القمر الدموي وحرب الزومبي", "اصمد في الليل واقضِ على 15 من زومبي الحصار الليلي (الحالي: ${player.zombiesKilled}/15)."),
            SealStep(2, "3. ختم قبطان البحار والحصون", "أبحر بالمركب واقتحم أحد الحصون أو جزر القراصنة وافتح صندوقه الذهبي المقفل."),
            SealStep(3, "4. ختم الحكيم الأعظم (التجارة)", "قايض مع أي قروي حتى يرتفع مستواه إلى المستوى 5 (Master)."),
            SealStep(4, "5. ختم سيد الصهر والفرن", "اصهر الحديد والحجر إلى ذهب وصلب في الفرن التفاعلي 3 مرات."),
            SealStep(5, "6. ختم الكنز الأثري المدفون", "اتبع بوصلة الكنز واحفر كنزاً مدفوناً في العالم المفتوح بالمعول."),
            SealStep(6, "7. ختم الصياغة الكونية والترسانة", "اصنع «سيف قاهر الهاوية الخرافي» أو «ختم الهاوية المركّب» من طاولة الصناعة.")
        )

        val rowH = 68f * u
        val gap = 8f * u
        val contentH = sealSteps.size * (rowH + gap)
        maxScroll = max(0f, contentH - listRect.height())
        scrollY = scrollY.coerceIn(0f, maxScroll)

        c.save()
        c.clipRect(listRect)
        c.translate(0f, -scrollY)
        for ((i, s) in sealSteps.withIndex()) {
            val unlocked = (player.sealMask and (1 shl s.bit)) != 0
            val top = listRect.top + i * (rowH + gap)
            val card = RectF(listRect.left, top, listRect.right, top + rowH)
            Ui.panel(c, card, if (unlocked) Color.parseColor("#69F0AE") else Color.rgb(110, 114, 128), Color.argb(230, 32, 34, 46))
            Ui.icon(c, "icons/abyss_seal.png", card.right - 36f * u, card.centerY(), 38f, if (unlocked) null else Color.GRAY)
            Ui.text(c, s.title + (if (unlocked) "  (مكتمل ✓)" else "  (قيد التجهيز)"), card.right - 68f * u, card.top + 26f * u, 17.5f, if (unlocked) Color.parseColor("#69F0AE") else Color.WHITE, Paint.Align.RIGHT)
            Ui.text(c, s.desc, card.right - 68f * u, card.top + 50f * u, 13.5f, Color.rgb(205, 210, 225), Paint.Align.RIGHT)
        }
        c.restore()

        val btnSummon = RectF(mr.left + 20f * u, bottomBtnsTop, mr.centerX() - 10f * u, mr.bottom - 14f * u)
        val bossAlive = colossalBoss != null && !colossalBoss!!.isDead
        Ui.button(
            c, btnSummon,
            if (bossAlive) "الزعيم العملاق في الساحة!" else "استدعاء الزعيم العملاق (25,000 HP) 💀",
            Color.parseColor("#C62828"), !bossAlive, 17f
        )
        if (!bossAlive) hits.add(Hit(btnSummon, "summon_colossal"))

        val btnTpAltar = RectF(mr.centerX() + 10f * u, bottomBtnsTop, mr.right - 20f * u, mr.bottom - 14f * u)
        Ui.button(c, btnTpAltar, "انتقال فوري لساحة المذبح ⚡", Color.parseColor("#00838F"), ow != null && !inCave, 17f)
        if (ow != null && !inCave) hits.add(Hit(btnTpAltar, "tp_altar"))
    }

    private fun tapBossAltarModal(id: String?) {
        when (id) {
            "close" -> {
                Sound.play("click")
                modal = Modal.NONE
            }
            "tp_altar" -> {
                val w = ow ?: return
                player.x = bossAltar.x + 32f
                player.y = bossAltar.y + 180f
                camera.snapTo(player.cx, player.cy)
                w.warm(player.cx, player.cy)
                w.reveal(player.cx, player.cy, 20)
                fade = 1f
                modal = Modal.NONE
                Sound.play("portal")
                toast("انتقلت إلى ساحة مذبح الزعيم العملاق شمال القرية!")
            }
            "summon_colossal" -> {
                val w = ow ?: return
                if (inCave) {
                    toast("اخرج إلى سطح العالم المفتوح أولاً لاستدعاء الزعيم العملاق!")
                    return
                }
                modal = Modal.NONE
                player.x = bossAltar.x + 32f
                player.y = bossAltar.y + 220f
                camera.snapTo(player.cx, player.cy)
                w.warm(player.cx, player.cy)
                val cb = Enemy(bossAltar.x - 28f, bossAltar.y - 160f, EnemyRole.BOSS, EraType.ERA_3_ZOMBIE, false, true)
                cb.aggro = true
                enemies.add(cb)
                colossalBoss = cb
                shake = 18f
                Sound.play("boss")
                Sound.playMusic(2)
                if (player.abyssalSeals < 7) {
                    toast("تحذير: استدعيت الزعيم العملاق قبل جمع الأختام الـ 7! درعه يقلل ضررك بشدة!")
                } else {
                    toast("استيقظ إمبراطور الهاوية والزومبي العملاق! استعد لمعركة الـ 5 مراحل!")
                }
            }
        }
    }

    private fun drawDialogue(c: Canvas) {
        val u = Ui.u
        val w = curW.toFloat()
        val h = curH.toFloat()
        if (dialogueIdx >= dialogue.size) return
        val line = dialogue[dialogueIdx]
        val pr = RectF(w * 0.06f, h - 236f * u, w * 0.94f, h - 20f * u)
        Ui.panel(c, pr, GOLD, Color.argb(240, 14, 18, 34), 16f)
        val box = RectF(pr.right - 150f * u, pr.top + 18f * u, pr.right - 18f * u, pr.top + 150f * u)
        Ui.fillRound(c, box, Color.argb(255, 8, 10, 22), 12f)
        Ui.strokeRound(c, box, GOLD, 2.5f, 12f)
        val bm = Assets.bmp(line.portrait)
        if (bm != null) {
            val sw = if (bm.width > bm.height * 3 / 2) bm.height else bm.width
            val src = android.graphics.Rect(0, 0, sw, bm.height)
            val dstR = RectF(box.left + 10f * u, box.top + 10f * u, box.right - 10f * u, box.bottom - 10f * u)
            c.drawBitmap(bm, src, dstR, null)
        }
        val nr = RectF(pr.right - 330f * u, pr.top - 20f * u, pr.right - 160f * u, pr.top + 14f * u)
        Ui.fillRound(c, nr, Color.parseColor("#F9A825"), 12f)
        Ui.text(c, line.speaker, nr.centerX(), nr.centerY() + 6f * u, 18f, Color.parseColor("#3E2723"), Paint.Align.CENTER, true, false)
        val shown = min(line.text.length, dialogueChars())
        val txt = line.text.substring(0, shown)
        Ui.wrapped(c, txt, pr.left + 28f * u, pr.top + 28f * u, pr.width() - 200f * u, 23f, Color.WHITE, false, 5, false, false)
        if (shown >= line.text.length) {
            val a = (160 + 90 * sin(time * 5f)).toInt().coerceIn(0, 255)
            Ui.text(c, "اضغط للمتابعة", pr.left + 28f * u, pr.bottom - 20f * u, 15f, Color.argb(a, 255, 213, 79), Paint.Align.LEFT)
        }
        hits.add(Hit(RectF(0f, 0f, w, h), "adv"))
    }

    // ---------------------------------------------------------------- open world + caves
    private fun saveProgress() {
        if (ow != null) saveOpen() else player.save(era.ordinal)
    }

    private fun saveOpen() {
        val w = ow ?: return
        player.save(5)
        val saveX = if (inCave) surfaceX else player.x
        val saveY = if (inCave) surfaceY else player.y
        SaveStore.putAll(
            mapOf<String, Any>(
                "mode" to "open", "ow_day" to dayNo, "ow_time" to dayTime,
                "ow_px" to saveX, "ow_py" to saveY,
                "ow_exp" to w.saveExplored(), "ow_mod" to w.saveMods(),
                "ow_pblocks" to w.savePlayerBlocks(),
                "ow_farm" to w.saveFarming(),
                "ow_unlocked" to true, "ow_hunger" to hunger
            )
        )
    }

    private fun isNight(): Boolean = sin(6.2832f * dayTime) < -0.15f

    private fun darknessNow(): Float {
        if (player.visionBuffTimer > 0f) return 0.05f
        if (inCave) return 0.82f
        return (0.30f - sin(6.2832f * dayTime) * 0.75f).coerceIn(0f, 0.88f)
    }

    private fun timeLabel(): String = when {
        inCave -> "داخل الكهف (طابق $caveFloor)"
        dayTime < 0.06f -> "فجر"
        dayTime < 0.38f -> "نهار"
        dayTime < 0.50f -> "غروب"
        else -> "ليل"
    }

    private fun spawnRandomTreasureMound() {
        val w = ow ?: return
        for (tries in 0 until 40) {
            val tx = 30 + rnd.nextInt(OpenWorld.N - 60)
            val ty = 30 + rnd.nextInt(OpenWorld.N - 60)
            val px = tx * OpenWorld.TS
            val py = ty * OpenWorld.TS
            if (!w.isWaterAt(px, py) && w.freeAt(px, py)) {
                mounds.add(BuriedMound(px, py))
                break
            }
        }
    }

    private fun enterCave(ce: CaveEntrance, floorNo: Int) {
        if (!inCave) {
            surfaceX = player.x
            surfaceY = player.y
        }
        inCave = true
        activeCave = ce
        caveFloor = floorNo.coerceIn(1, 3)
        val cEra = EraType.values()[ce.biome.coerceIn(0, 5)]
        val cm = TileMap(cEra, ce.seed + caveFloor * 131L)
        caveMap = cm

        enemies.clear()
        projectiles.clear()
        explosions.clear()
        coins.clear()
        chests.clear()
        crates.clear()
        rocks.clear()
        trees.clear()
        traps.clear()
        cocoons.clear()
        totems.clear()
        boss = null
        bossDown = false

        val sx = 4f * TileMap.TS
        val sy = 15f * TileMap.TS
        caveExitSurface = CaveExitNode(sx - 20f, sy - 90f, false, caveFloor)
        val nextX = 34f * TileMap.TS
        val nextY = 15f * TileMap.TS
        caveExitNext = if (caveFloor < 3) CaveExitNode(nextX, nextY, true, caveFloor) else null

        val pts = ArrayList<Pt>()
        pts.add(Pt(sx, sy))
        pts.add(Pt(nextX, nextY))

        for (i in 0 until (5 + caveFloor * 2)) {
            val rx = (7 + rnd.nextInt(26)) * TileMap.TS
            val ry = (5 + rnd.nextInt(20)) * TileMap.TS
            rocks.add(MiningRock(rx, ry, i % 2 == 0))
            pts.add(Pt(rx, ry))
        }
        for (i in 0 until (3 + caveFloor)) {
            val tx = (8 + rnd.nextInt(24)) * TileMap.TS
            val ty = (5 + rnd.nextInt(20)) * TileMap.TS
            traps.add(DungeonTrap(tx, ty, i % 2 == 0))
            pts.add(Pt(tx, ty))
        }
        for (i in 0 until (2 + caveFloor)) {
            val ccx = (10 + rnd.nextInt(20)) * TileMap.TS
            val ccy = (6 + rnd.nextInt(18)) * TileMap.TS
            cocoons.add(CaveCocoon(ccx, ccy, ce.biome))
            pts.add(Pt(ccx, ccy))
        }
        val ttx = 20f * TileMap.TS
        val tty = 8f * TileMap.TS
        totems.add(CaveTotem(ttx, tty, ce.biome))
        pts.add(Pt(ttx, tty))

        chests.add(TreasureChest(18f * TileMap.TS, 22f * TileMap.TS, 45 * caveFloor, false, false))
        chests.add(TreasureChest(32f * TileMap.TS, 8f * TileMap.TS, 85 * caveFloor, false, true))
        pts.add(Pt(18f * TileMap.TS, 22f * TileMap.TS))
        pts.add(Pt(32f * TileMap.TS, 8f * TileMap.TS))

        for (i in 0 until (4 + caveFloor * 2)) {
            val ex = (9 + rnd.nextInt(24)) * TileMap.TS
            val ey = (5 + rnd.nextInt(20)) * TileMap.TS
            val role = if (i % 3 == 0) EnemyRole.HEAVY else if (i % 2 == 0) EnemyRole.GHOUL else EnemyRole.SPEARMAN
            enemies.add(Enemy(ex, ey, role, cEra))
            pts.add(Pt(ex, ey))
        }
        if (caveFloor == 3) {
            val cb = Enemy(31f * TileMap.TS, 15f * TileMap.TS, EnemyRole.BOSS, cEra)
            boss = cb
            enemies.add(cb)
            pts.add(Pt(cb.x, cb.y))
        }

        cm.buildCave(caveFloor, Pt(sx, sy), pts)
        player.x = sx
        player.y = sy
        player.inBoat = false
        camera = Camera2D(cm.width, cm.height)
        camera.viewW = curW / zoom
        camera.viewH = curH / zoom
        camera.snapTo(player.cx, player.cy)
        buildDark()
        fade = 1f
        Sound.play("portal")
        Sound.playMusic(2)
        toast("دخلت ${ce.nameAr} — الطابق $caveFloor")
    }

    private fun exitCaveToSurface() {
        val w = ow ?: return
        inCave = false
        caveMap = null
        activeCave = null
        enemies.clear()
        projectiles.clear()
        rocks.clear()
        traps.clear()
        cocoons.clear()
        totems.clear()
        chests.clear()
        boss = null
        player.x = surfaceX
        player.y = surfaceY
        camera = Camera2D(w.width, w.height)
        camera.viewW = curW / zoom
        camera.viewH = curH / zoom
        camera.snapTo(player.cx, player.cy)
        w.warm(player.cx, player.cy)
        buildDark()
        fade = 1f
        Sound.play("portal")
        Sound.playMusic(if (isNight()) 2 else 1)
        toast("خرجت إلى سطح العالم المفتوح")
        populateOpenWorldLandmarks(w)
    }

    private fun populateOpenWorldLandmarks(w: OpenWorld) {
        chests.clear()
        banners.clear()
        structureNodes.clear()
        for (st in w.structureSites) {
            val sx = st.tx * OpenWorld.TS + 32f
            val sy = st.ty * OpenWorld.TS + 32f
            structureNodes.add(AncientStructureNode(sx, sy, st.kind, st.nameAr, st.descAr, st.iconAsset, st.color))
            chests.add(TreasureChest(sx + 68f, sy - 24f, 150 + st.kind * 25, false, st.kind % 2 == 0))
        }
        for ((bi, f) in w.fortressTiles.withIndex()) {
            val fx = f.first * OpenWorld.TS
            val fy = f.second * OpenWorld.TS
            banners.add(FortressBanner(fx - 36f, fy - 5f * OpenWorld.TS, "حصن ${biomeNames[bi]}"))
            chests.add(TreasureChest(fx - 32f, fy - 32f, 140 + bi * 30, false, true))
            chests.add(TreasureChest(fx + 64f, fy - 32f, 65 + bi * 15, false, false))
        }
        for (isl in w.islandCenters) {
            val ix = isl.first * OpenWorld.TS
            val iy = isl.second * OpenWorld.TS
            banners.add(FortressBanner(ix, iy - 64f, "جزيرة القراصنة"))
            chests.add(TreasureChest(ix + 48f, iy, 120, false, true))
        }
    }

    private fun startOpen(restore: Boolean) {
        val w = OpenWorld(20260)
        ow = w
        inCave = false
        caveMap = null
        activeCave = null
        state = State.PLAYING
        modal = Modal.NONE
        enemies.clear()
        projectiles.clear()
        explosions.clear()
        floats.clear()
        coins.clear()
        chests.clear()
        crates.clear()
        rocks.clear()
        trees.clear()
        traps.clear()
        props.clear()
        gens.clear()
        beds.clear()
        furnaces.clear()
        campfires.clear()
        caveEntrances.clear()
        cocoons.clear()
        totems.clear()
        cannons.clear()
        banners.clear()
        docks.clear()
        mounds.clear()
        sheeps.clear()
        villagers.clear()
        owPortals.clear()
        particles.list.clear()
        mound = null
        boss = null
        bossDown = false
        greeted = false
        cancelAll()
        shopBought.clear()

        val ts = OpenWorld.TS
        val cxp = OpenWorld.CX * ts
        val cyp = OpenWorld.CY * ts

        merchant = MerchantNPC(cxp - 5f * ts, cyp - 2f * ts)
        table = CraftTable(cxp + 4f * ts, cyp - 2f * ts)
        beds.add(Bed((OpenWorld.CX - 7) * ts, (OpenWorld.CY - 6) * ts))
        storageChests.clear()
        furnaces.add(FurnaceObj(cxp + 4f * ts, cyp + 2f * ts))
        storageChests.add(StorageChest(cxp + 6.5f * ts, cyp - 1.5f * ts, "شونة القرية وصندوق التخزين"))
        owPortals.add(TimePortal(cxp - 70f, cyp - 6f * ts, "بوابة الزمن المركزية").apply { isActive = true })
        bossAltar = ColossalBossAltar(cxp - 80f, (OpenWorld.CY - 34) * ts - 80f)
        colossalBoss = null
        meteorZones.clear()
        cannons.add(CannonTurret(cxp - 8f * ts, cyp + 6f * ts, true))
        cannons.add(CannonTurret(cxp + 7f * ts, cyp + 6f * ts, true))

        villagers.add(VillagerNPC(cxp + 2.5f * ts, cyp - 2.5f * ts, "الحداد براندون", VillagerRole.BLACKSMITH, "3 حديد ➔ مفتاح ذهبي", "مفتاح ذهبي", "iron_key"))
        villagers.add(VillagerNPC(cxp - 2.5f * ts, cyp + 2f * ts, "الحكيم أوريون", VillagerRole.ELDER, "55 ذهب ➔ 2 خام عصر", "خامات نادرة", "gold_mat"))
        villagers.add(VillagerNPC(cxp - 6f * ts, cyp + 3f * ts, "المزارع توماس", VillagerRole.FARMER, "2 صوف ➔ 45 ذهب", "45 ذهب", "wool_gold"))
        villagers.add(VillagerNPC(cxp + 6f * ts, cyp + 3f * ts, "الطاهية هيلين", VillagerRole.TRADER, "2 لحم نيء ➔ 2 حساء", "حساء الطاقة", "meat_stew"))

        val caveNames = arrayOf(
            "كهف القلعة المظلم", "كهف الكريستال العميق", "سرداب الزومبي",
            "مغارة القراصنة", "مختبر السايبر تحت الأرض", "هاوية الفراغ الكوني"
        )
        for (bi in 0 until 6) {
            val sv = w.subVillageTiles[bi]
            val vx = sv.first * ts
            val vy = sv.second * ts
            beds.add(Bed(vx - 5f * ts, vy - 5f * ts))
            furnaces.add(FurnaceObj(vx + 3f * ts, vy - 2f * ts))
            campfires.add(CampfireNode(vx, vy, "مخيم ${biomeNames[bi]}", true))
            owPortals.add(TimePortal(vx - 70f, vy + 4f * ts, "بوابة ${biomeNames[bi]}").apply { isActive = true })
            villagers.add(
                VillagerNPC(
                    vx - 2f * ts, vy + 1f * ts,
                    "تاجر ${biomeNames[bi].substringBefore(" ")}",
                    if (bi == 3) VillagerRole.PIRATE_SMUGGLER else VillagerRole.TRADER,
                    if (bi % 2 == 0) "4 خشب ➔ 2 جرعة شفاء" else "4 حجر ➔ 3 قنابل",
                    "مقايضة القرية",
                    if (bi % 2 == 0) "wood_pot" else "stone_bomb"
                )
            )

            val cv = w.caveTiles[bi]
            caveEntrances.add(CaveEntrance(cv.first * ts - 64f, cv.second * ts - 56f, bi, caveNames[bi], 5000L + bi * 99L))

            val ft = w.fortressTiles[bi]
            val fx = ft.first * ts
            val fy = ft.second * ts
            cannons.add(CannonTurret(fx - 4f * ts, fy + 5f * ts, false))
            cannons.add(CannonTurret(fx + 3f * ts, fy + 5f * ts, false))
        }

        for (isl in w.islandCenters) {
            docks.add(BoatDock((isl.first - 12) * ts, isl.second * ts))
            cannons.add(CannonTurret(isl.first * ts - 80f, isl.second * ts + 80f, false))
        }
        docks.add(BoatDock((OpenWorld.CX - 22) * ts, (OpenWorld.CY + 18) * ts))

        populateOpenWorldLandmarks(w)

        for (i in 0 until 28) {
            val ang = rnd.nextFloat() * 6.2832f
            val rad = (12f + rnd.nextFloat() * 140f) * ts
            val sx = cxp + cos(ang) * rad
            val sy = cyp + sin(ang) * rad
            if (sx in 400f..(w.width - 400f) && sy in 400f..(w.height - 400f) && !w.isWaterAt(sx, sy) && w.freeAt(sx, sy)) {
                sheeps.add(Sheep(sx, sy))
            }
        }

        repeat(6) { spawnRandomTreasureMound() }

        eyes.clear()
        stalker = null
        hunger = 85f
        sinceHurt = 10f
        hungerWarned = false
        props.add(WorldProp("environment/fire.png", cxp - 28f, cyp - 40f, 56f, 56f))
        props.add(WorldProp("environment/torch.png", cxp - 3f * ts, cyp - 3f * ts, 36f, 36f, 0.15f))
        props.add(WorldProp("environment/torch.png", cxp + 3f * ts, cyp - 3f * ts, 36f, 36f, 0.15f))
        player.resetState()
        player.x = w.spawnX
        player.y = w.spawnY
        surfaceX = player.x
        surfaceY = player.y
        dayNo = 1
        dayTime = 0.05f
        if (restore) {
            dayNo = SaveStore.int("ow_day", 1)
            dayTime = SaveStore.float("ow_time", 0.05f)
            hunger = SaveStore.float("ow_hunger", 85f)
            w.loadMods(SaveStore.str("ow_mod", ""))
            w.loadPlayerBlocks(SaveStore.str("ow_pblocks", ""))
            w.loadFarming(SaveStore.str("ow_farm", ""))
            w.loadExplored(SaveStore.str("ow_exp", ""))
            val sx = SaveStore.float("ow_px", w.spawnX)
            val sy = SaveStore.float("ow_py", w.spawnY)
            if (!w.blocked(sx + 33f, sy + 66f, sx + 63f, sy + 88f)) {
                player.x = sx
                player.y = sy
            }
        }
        camera = Camera2D(w.width, w.height)
        camera.viewW = curW / zoom
        camera.viewH = curH / zoom
        camera.snapTo(player.cx, player.cy)
        w.warm(player.cx, player.cy)
        w.reveal(player.cx, player.cy, 20)
        nightFlag = isNight()
        owSpawnTimer = 4f
        owSaveTimer = 0f
        owExploreTimer = 0f
        owRegrowTimer = 0f
        deathTimer = 0f
        fade = 1f
        bannerTimer = 0f
        Sound.playMusic(if (nightFlag) 2 else 1)
        saveOpen()
        if (!restore) openDialogue(Story.openWorldIntro()) { }
    }

    private fun respawnOpen() {
        val w = ow ?: return
        if (inCave) {
            exitCaveToSurface()
        }
        val lost = player.coins / 5
        player.coins -= lost
        player.resetState()
        player.x = w.spawnX
        player.y = w.spawnY
        enemies.clear()
        projectiles.clear()
        camera.snapTo(player.cx, player.cy)
        deathTimer = 0f
        fade = 1f
        hunger = max(hunger, 60f)
        eyes.clear()
        stalker = null
        toast("استيقظتَ في القرية المركزية... فقدتَ $lost ذهب")
    }

    private fun updateOpen(dt: Float) {
        val w = ow ?: return
        val edt = if (player.slowTimer > 0f) dt * 0.3f else dt
        val hpBefore = player.hp

        dayTime += dt / dayLength
        if (dayTime >= 1f) {
            dayTime -= 1f
            dayNo++
            shopBought.clear()
            toast("بداية اليوم $dayNo")
            w.regrow(dayNo, player.cx, player.cy)
            saveOpen()
        }
        val nowNight = isNight()
        if (nowNight != nightFlag) {
            nightFlag = nowNight
            if (!inCave) Sound.playMusic(if (nowNight) 2 else 1)
            toast(if (nowNight) "حلّ الليل... الزومبي والظلام يتحركون!" else "طلع الفجر")
            if (!nowNight) {
                val old = stalker
                if (old != null) enemies.remove(old)
                stalker = null
                eyes.clear()
            }
        }
        hunger = max(0f, hunger - dt * (if (nowNight) 0.22f else 0.16f))
        sinceHurt += dt
        if (hunger < 25f && !hungerWarned) {
            hungerWarned = true
            toast("أنت جائع... تناول طعاماً من زر الأكل أو الحقيبة")
        }
        if (hunger > 40f) hungerWarned = false

        if (nowNight && !inCave) {
            eyeTimer -= dt
            if (eyeTimer <= 0f) {
                eyeTimer = 3f + rnd.nextFloat() * 4f
                val maxE = if (lowQuality) 1 else 3
                if (eyes.size < maxE && !w.inVillage(player.cx, player.cy)) {
                    val a = rnd.nextFloat() * 6.2832f
                    val r = 380f + rnd.nextFloat() * 120f
                    eyes.add(Eyes(player.cx + cos(a) * r, player.cy + sin(a) * r, 9f, rnd.nextFloat() * 6f))
                }
            }
        }
        var ei = eyes.size - 1
        while (ei >= 0) {
            val ey = eyes[ei]
            ey.life -= dt
            if (ey.life <= 0f || !nowNight || inCave) {
                eyes.removeAt(ei)
            } else if (dist(ey.x, ey.y, player.cx, player.cy) < 250f) {
                Sound.play("whisper", 0.7f)
                particles.burst(ey.x, ey.y, 6, Color.parseColor("#B71C1C"), 90f, 4f, 0.5f)
                eyes.removeAt(ei)
            }
            ei--
        }

        if (attackBtn.isPressed && !player.isDead) {
            if (player.attack()) Sound.play("swing", 0.7f)
        }

        if (!inCave) {
            if (w.blockCodeAtPx(player.cx, player.cy) == BlockType.LAUNCH_PAD.code && player.airTimer <= 0f && player.launchPadCd <= 0f) {
                player.triggerLaunchPad()
                Sound.play("dash")
                particles.burst(player.cx, player.cy, 14, Color.parseColor("#00E5FF"), 210f, 6f, 0.5f)
                addFloat("تحليق هوائي! 🚀", player.cx, player.y - 20f, Color.parseColor("#00E5FF"), 24f)
            }
            w.allowDeepWater = player.hasBoat || player.airTimer > 0f
            w.allowDoorPass = true
            w.ignoreObstaclesForAir = player.airTimer > 0f
            player.update(dt, joystick.dx, joystick.dy, w)
            w.allowDeepWater = false
            w.allowDoorPass = false
            w.ignoreObstaclesForAir = false
            player.inBoat = player.hasBoat && player.airTimer <= 0f && w.isWaterAt(player.cx, player.cy)
            if (player.justLandedSlam) {
                player.justLandedSlam = false
                Sound.play("explode", 0.75f)
                shake = max(shake, 10f)
                particles.burst(player.cx, player.cy, 22, Color.parseColor("#00E5FF"), 260f, 7f, 0.55f)
                addFloat("ضربة هوائية ساحقة! 💥", player.cx, player.y - 24f, GOLD, 26f)
                for (en in enemies) {
                    if (!en.isDead && dist(en.cx, en.cy, player.cx, player.cy) < 155f) {
                        damageEnemy(en, (115 * player.comboDmgMul()).toInt(), player.cx, player.cy, 1.5f, true)
                    }
                }
            }
        } else {
            player.inBoat = false
            player.update(dt, joystick.dx, joystick.dy, collider)
        }

        camera.update(player.cx, player.cy, dt)
        if (LanMultiplayer.isConnected) {
            updateMultiplayerSync(w, dt)
        }
        if (player.isDead) {
            if (LanMultiplayer.isConnected) {
                // In Co-Op mode, enter a 12-second Downed State so your friend can revive you!
                playerDownedTimer += dt
                if (playerDownedTimer < 12f) {
                    if ((playerDownedTimer * 2f).toInt() != ((playerDownedTimer - dt) * 2f).toInt()) {
                        toast("سقطت في المعركة! صديقك يمكنه الاقتراب لإنعاشك (${(12f - playerDownedTimer).toInt()}ث) ❤️‍🩹")
                    }
                    updateTarget()
                    updateButtons()
                    return
                }
                playerDownedTimer = 0f
            }
            deathTimer += dt
            if (deathTimer > 2f) {
                respawnOpen()
                return
            }
        } else {
            playerDownedTimer = 0f
        }
        resolveMelee()

        if (!inCave) {
            spawnOpenEnemies(w, dt)
            for (sh in sheeps) sh.update(dt, w)
            for (vl in villagers) vl.update(dt, w)
            for (fn in furnaces) fn.update(dt)
            for (cp in campfires) cp.update(dt)
            for (pt in owPortals) pt.update(dt)
            for (m in mounds) m.update(dt)
            for (sn in structureNodes) sn.update(dt)
            updateAtmosphereMotes(w, dt)
            for (cn in cannons) {
                if (dist(cn.x, cn.y, player.cx, player.cy) < 1200f) {
                    if (cn.update(dt, player, enemies, projectiles)) Sound.play("explode", 0.5f)
                }
            }
        } else {
            for (t in traps) t.update(dt, player)
            for (r in rocks) r.update(dt)
            for (cc in cocoons) {
                if (cc.update(dt) && enemies.size < 14) {
                    val b = cc.biome.coerceIn(0, 5)
                    val en = Enemy(cc.x, cc.y + 40f, EnemyRole.GHOUL, EraType.values()[b])
                    en.aggro = true
                    enemies.add(en)
                }
            }
        }

        if (!inCave) {
            bossAltar.update(dt)
            var mi = meteorZones.size - 1
            while (mi >= 0) {
                val mz = meteorZones[mi]
                mz.update(edt)
                if (mz.readyToExplode) {
                    meteorZones.removeAt(mi)
                    explosions.add(ExplosionFx(mz.x, mz.y, mz.radius))
                    Sound.play("explode", 0.7f)
                    shake = max(shake, 8f)
                    particles.burst(mz.x, mz.y, 16, Color.parseColor("#FF1744"), 260f, 6f, 0.5f)
                    if (dist(player.cx, player.cy, mz.x, mz.y) < mz.radius) {
                        player.takeDamage(55)
                    }
                    val mtx = floor(mz.x / OpenWorld.TS).toInt()
                    val mty = floor(mz.y / OpenWorld.TS).toInt()
                    w.damagePlayerBlockAt(mtx, mty, 2)
                }
                mi--
            }
        }
        var i = 0
        while (i < enemies.size) {
            val en = enemies[i]
            en.update(edt, player, collider, projectiles)
            if (en.wantSummon) {
                en.wantSummon = false
                summonMinions(en)
            }
            if (en.wantMeteorRain && !inCave) {
                en.wantMeteorRain = false
                Sound.play("boss", 0.85f)
                toast("عاصفة نيازك الهاوية! اندفع أو ابنِ سقفاً وتحرك فوراً!")
                val count = if (en.bossPhase == 5) 5 else 3
                for (k in 0 until count) {
                    val ang = rnd.nextFloat() * 6.283f
                    val rad = if (k == 0) 35f else (110f + rnd.nextFloat() * 160f)
                    meteorZones.add(MeteorStrikeZone(player.cx + cos(ang) * rad, player.cy + sin(ang) * rad, 110f, 1.25f))
                }
            }
            if (en.wantBomberDetonate && !inCave && !en.isDead) {
                en.wantBomberDetonate = false
                en.hp = 0
                en.isDead = true
                bomberZombieExplode(w, en.cx, en.cy)
                i++
                continue
            }
            if (en.wantBreakWall && !inCave) {
                en.wantBreakWall = false
                val wtx = floor((en.cx + (if (en.facingLeft) -44f else 44f)) / OpenWorld.TS).toInt()
                val wty = floor(en.cy / OpenWorld.TS).toInt()
                val res = w.damagePlayerBlockAt(wtx, wty, if (en.isColossalBoss) 3 else 1, false)
                if (res == 2) {
                    Sound.play("chop", 0.7f)
                    particles.burst(wtx * OpenWorld.TS + 32f, wty * OpenWorld.TS + 32f, 8, Color.LTGRAY, 140f, 5f, 0.35f)
                } else if (res == -1) {
                    addFloat("جدار سمك 2 منيع! 🛡️", wtx * OpenWorld.TS + 32f, wty * OpenWorld.TS, Color.parseColor("#80DEEA"), 18f)
                }
            }
            i++
        }
        separateEnemies()
        val eit = enemies.iterator()
        while (eit.hasNext()) {
            val en = eit.next()
            val far = !inCave && dist(en.cx, en.cy, player.cx, player.cy) > 2100f
            if ((en.isDead && en.deadTimer > 2.5f) || (!en.isDead && far)) eit.remove()
        }

        var pi = projectiles.size - 1
        while (pi >= 0) {
            val pr = projectiles[pi]
            pr.update(edt)
            if (!pr.dead) updateProjectile(pr)
            if (pr.dead) {
                if (pr.kind == Projectile.BOMB || pr.kind == Projectile.CANNONBALL) explode(pr.x, pr.y)
                projectiles.removeAt(pi)
            }
            pi--
        }
        var xi = explosions.size - 1
        while (xi >= 0) {
            explosions[xi].update(dt)
            if (explosions[xi].done) explosions.removeAt(xi)
            xi--
        }
        particles.update(dt)
        var fi = floats.size - 1
        while (fi >= 0) {
            floats[fi].update(dt)
            if (floats[fi].dead) floats.removeAt(fi)
            fi--
        }
        var ci = coins.size - 1
        while (ci >= 0) {
            val co = coins[ci]
            co.update(dt, player)
            if (co.collected) {
                player.coins += co.value
                addFloat("+${co.value}", player.cx, player.y - 10f, Color.parseColor("#FFD740"), 24f)
                Sound.play("coin", 0.7f)
                coins.removeAt(ci)
            }
            ci--
        }
        merchant.update(dt)
        for (p in props) p.update(dt)

        if (!inCave) {
            w.updateFarmingAndSaplings(dt)
            owExploreTimer -= dt
            if (owExploreTimer <= 0f) {
                owExploreTimer = 0.6f
                w.reveal(player.cx, player.cy, 18)
            }
            owRegrowTimer += dt
            if (owRegrowTimer > 45f) {
                owRegrowTimer = 0f
                if (sheeps.count { !it.isDead } < 20) {
                    val sx = w.spawnX + (rnd.nextFloat() - 0.5f) * 3500f
                    val sy = w.spawnY + (rnd.nextFloat() - 0.5f) * 3500f
                    if (!w.isWaterAt(sx, sy) && w.freeAt(sx, sy)) sheeps.add(Sheep(sx, sy))
                }
            }
        }
        owSaveTimer += dt
        if (owSaveTimer > 25f) {
            owSaveTimer = 0f
            saveOpen()
        }
        if (player.hp < hpBefore) {
            onPlayerHurt(hpBefore - player.hp)
            sinceHurt = 0f
        }
        if (!player.isDead) {
            if (hunger > 50f && sinceHurt > 5f && player.hp < player.maxHp) {
                regenTimer += dt
                if (regenTimer >= 1.5f) {
                    regenTimer = 0f
                    player.hp = min(player.maxHp, player.hp + 2)
                }
            }
            if (hunger <= 0f) {
                starveTimer += dt
                if (starveTimer >= 2.5f) {
                    starveTimer = 0f
                    if (player.hp > 8) {
                        player.hp -= 2
                        addFloat("جوع!", player.cx, player.y - 10f, Color.parseColor("#FF8A65"), 22f)
                    }
                }
            }
        }
        updateTargetOpen()
        updateButtons()
    }

    private fun updateTargetOpen() {
        val px = player.cx
        val py = player.cy
        var best = Target.NONE
        var bestD = 1e9f
        targetChest = null
        targetMound = null
        targetCave = null
        targetCamp = null
        targetTotem = null
        targetVillager = null

        fun cand(t: Target, d: Float, lim: Float): Boolean {
            if (d < lim && d < bestD) {
                best = t
                bestD = d
                return true
            }
            return false
        }

        for (c in chests) {
            if (c.isNear(px, py)) {
                val d = dist(px, py, c.x + 32f, c.y + 32f)
                if (cand(Target.CHEST, d, 105f)) targetChest = c
            }
        }

        if (inCave) {
            if (caveExitSurface.isNear(px, py)) cand(Target.CAVE_EXIT, dist(px, py, caveExitSurface.x + 48f, caveExitSurface.y + 48f), 110f)
            val nxt = caveExitNext
            if (nxt != null && nxt.isNear(px, py)) cand(Target.CAVE_NEXT, dist(px, py, nxt.x + 48f, nxt.y + 48f), 110f)
            for (tt in totems) {
                if (tt.isNear(px, py)) {
                    val d = dist(px, py, tt.x + 44f, tt.y + 48f)
                    if (cand(Target.TOTEM, d, 110f)) targetTotem = tt
                }
            }
            target = best
            interactBtn.visible = best != Target.NONE && !player.isDead
            styleInteractButton(best)
            return
        }

        cand(Target.SHOP, dist(px, py, merchant.x + 48f, merchant.y + 60f), 130f)
        cand(Target.CRAFT, dist(px, py, table.x + 32f, table.y + 32f), 112f)
        cand(Target.COOK, dist(px, py, OpenWorld.CX * OpenWorld.TS, OpenWorld.CY * OpenWorld.TS - 12f), 105f)

        targetCannon = null
        targetHarvestTx = -1
        targetHarvestTy = -1
        val w = ow
        if (w != null) {
            val ptx = floor(px / OpenWorld.TS).toInt()
            val pty = floor(py / OpenWorld.TS).toInt()
            for (dy in -1..1) for (dx in -1..1) {
                val tx = ptx + dx
                val ty = pty + dy
                if (w.isCropReadyAt(tx, ty)) {
                    val d = dist(px, py, tx * OpenWorld.TS + 32f, ty * OpenWorld.TS + 32f)
                    if (cand(Target.FARM_HARVEST, d, 105f)) {
                        targetHarvestTx = tx
                        targetHarvestTy = ty
                    }
                }
            }
        }
        if (LanMultiplayer.isConnected && coopPartner.isDowned) {
            val dRev = dist(px, py, coopPartner.cx, coopPartner.cy)
            cand(Target.REVIVE_PARTNER, dRev, 135f)
        }
        targetStructure = null
        for (sn in structureNodes) {
            if (sn.isNear(px, py)) {
                val d = dist(px, py, sn.x, sn.y)
                if (cand(Target.STRUCTURE_NODE, d, 125f)) targetStructure = sn
            }
        }
        for (sc in storageChests) if (sc.isNear(px, py)) cand(Target.STORAGE_CHEST, dist(px, py, sc.x + 36f, sc.y + 36f), 115f)
        for (cn in cannons) {
            if (cn.isNear(px, py) && cn.ammo < cn.maxAmmo) {
                val d = dist(px, py, cn.x + 38f, cn.y + 38f)
                if (cand(Target.CANNON_REFILL, d, 115f)) targetCannon = cn
            }
        }
        for (b in beds) if (b.isNear(px, py)) cand(Target.SLEEP, dist(px, py, b.x + 48f, b.y + 32f), 105f)
        for (fn in furnaces) if (fn.isNear(px, py)) cand(Target.FURNACE, dist(px, py, fn.x + 34f, fn.y + 34f), 110f)
        for (cp in campfires) {
            if (cp.isNear(px, py)) {
                val d = dist(px, py, cp.x + 32f, cp.y + 32f)
                if (cand(Target.CAMPFIRE, d, 115f)) targetCamp = cp
            }
        }
        for (ce in caveEntrances) {
            if (ce.isNear(px, py)) {
                val d = dist(px, py, ce.x + 64f, ce.y + 70f)
                if (cand(Target.CAVE_ENTER, d, 125f)) targetCave = ce
            }
        }
        for (vl in villagers) {
            if (vl.isNear(px, py)) {
                val d = dist(px, py, vl.x + 44f, vl.y + 50f)
                if (cand(Target.VILLAGER, d, 120f)) targetVillager = vl
            }
        }
        for (pt in owPortals) {
            if (pt.isNear(px, py)) cand(Target.PORTAL, dist(px, py, pt.x + 70f, pt.y + 70f), 115f)
        }
        for (dk in docks) {
            if (dk.isNear(px, py)) cand(Target.BOAT_DOCK, dist(px, py, dk.x + 40f, dk.y + 40f), 120f)
        }
        for (m in mounds) {
            if (m.isNear(px, py)) {
                val d = dist(px, py, m.x + 32f, m.y + 32f)
                if (cand(Target.MOUND, d, 100f)) targetMound = m
            }
        }

        target = best
        interactBtn.visible = best != Target.NONE && !player.isDead
        styleInteractButton(best)
    }

    private fun foodCount(): Int = player.stew + player.wheat + player.cookedMeat + player.apples + player.rawMeat

    private fun eatBest() {
        if (ow == null) return
        if (hunger >= 98f) {
            toast("لست جائعاً")
            Sound.play("deny", 0.5f)
            return
        }
        if (player.stew > 0) {
            player.stew--
            hunger = min(100f, hunger + 75f)
            player.hp = min(player.maxHp, player.hp + 35)
            addFloat("+75 شبع  +35 صحة", player.cx, player.y - 8f, Color.parseColor("#FFD54F"), 24f)
        } else if (player.wheat > 0) {
            player.wheat--
            hunger = min(100f, hunger + 55f)
            player.hp = min(player.maxHp, player.hp + 20)
            addFloat("+55 شبع  +20 صحة (خبز القمح)", player.cx, player.y - 8f, Color.parseColor("#FFD54F"), 24f)
        } else if (player.cookedMeat > 0) {
            player.cookedMeat--
            hunger = min(100f, hunger + 45f)
            player.hp = min(player.maxHp, player.hp + 12)
            addFloat("+45 شبع", player.cx, player.y - 8f, Color.parseColor("#FFB74D"), 24f)
        } else if (player.apples > 0) {
            player.apples--
            hunger = min(100f, hunger + 16f)
            player.hp = min(player.maxHp, player.hp + 5)
            addFloat("+16 شبع", player.cx, player.y - 8f, Color.parseColor("#FFB74D"), 24f)
        } else if (player.rawMeat > 0) {
            player.rawMeat--
            hunger = min(100f, hunger + 12f)
            if (player.hp > 12) player.hp -= 4
            addFloat("لحم نيء! (اطبخه في الفرن أفضل)", player.cx, player.y - 8f, Color.parseColor("#EF9A9A"), 22f)
        } else {
            toast("لا تملك طعاماً")
            Sound.play("deny", 0.5f)
            return
        }
        Sound.play("potion", 0.8f)
    }

    private fun cookAll(freeFuel: Boolean) {
        val n = player.rawMeat
        if (n <= 0) {
            toast("ليس معك لحم نيء للطبخ")
            Sound.play("deny", 0.6f)
            return
        }
        val need = if (freeFuel) 0 else (n + 1) / 2
        if (player.wood < need) {
            toast("تحتاج $need خشب للنار (أو استخدم الفرن مجاناً)")
            Sound.play("deny", 0.6f)
            return
        }
        player.wood -= need
        player.rawMeat = 0
        player.cookedMeat += n
        Sound.play("potion")
        toast("طبخت $n قطعة لحم مشوي!")
        advanceQuest("q_cook", n)
        saveProgress()
    }

    private fun trySleep() {
        val w = ow ?: return
        if (dayTime < 0.42f) {
            toast("لا تستطيع النوم الآن... انتظر المساء أو الليل")
            Sound.play("deny", 0.6f)
            return
        }
        for (en in enemies) {
            if (!en.isDead && dist(en.cx, en.cy, player.cx, player.cy) < 650f) {
                toast("الأعداء قريبون! اقضِ عليهم قبل النوم")
                Sound.play("deny", 0.6f)
                return
            }
        }
        dayTime = 0.04f
        dayNo++
        shopBought.clear()
        w.regrow(dayNo, player.cx, player.cy)
        player.hp = player.maxHp
        player.mana = player.maxMana.toFloat()
        hunger = max(15f, hunger - 20f)
        enemies.clear()
        projectiles.clear()
        eyes.clear()
        stalker = null
        nightFlag = false
        Sound.playMusic(1)
        fade = 1f
        toast("نمتَ في السرير حتى الفجر... بداية اليوم $dayNo")
        Sound.play("potion")
        saveOpen()
    }

    private fun pickRole(b: Int, night: Boolean): EnemyRole {
        val r = rnd.nextFloat()
        return when (b) {
            0 -> if (night && r < 0.35f) EnemyRole.GHOUL else if (r < 0.4f) EnemyRole.MELEE else if (r < 0.75f) EnemyRole.SPEARMAN else EnemyRole.ARCHER
            1 -> if (r < 0.5f) EnemyRole.GHOUL else if (r < 0.8f) EnemyRole.SPEARMAN else EnemyRole.HEAVY
            2 -> if (r < 0.45f) EnemyRole.MELEE else if (r < 0.85f) EnemyRole.GHOUL else EnemyRole.HEAVY
            3 -> if (r < 0.55f) EnemyRole.TRIBAL else if (r < 0.80f) EnemyRole.ARCHER else EnemyRole.SPEARMAN
            4 -> if (r < 0.7f) EnemyRole.COMMANDO else EnemyRole.HEAVY
            else -> if (r < 0.6f) EnemyRole.CLONE else EnemyRole.COMMANDO
        }
    }

    private fun spawnOpenEnemies(w: OpenWorld, dt: Float) {
        owSpawnTimer -= dt
        if (owSpawnTimer > 0f) return
        val night = isNight()
        owSpawnTimer = if (night) 2.0f else 4.8f
        if (night && dayNo >= 2 && stalker == null && rnd.nextFloat() < 0.14f && !w.inVillage(player.cx, player.cy, 1.4f)) {
            for (t in 0 until 10) {
                val a = rnd.nextFloat() * 6.2832f
                val r = 900f + rnd.nextFloat() * 120f
                val ex = player.cx + cos(a) * r
                val ey = player.cy + sin(a) * r
                if (ex < 200f || ey < 200f || ex > w.width - 200f || ey > w.height - 200f) continue
                if (!w.freeAt(ex, ey)) continue
                val st = Enemy(ex - 46f, ey - 64f, EnemyRole.CLONE, EraType.ERA_6_VOID)
                if (w.blocked(st.x + st.fOffX, st.y + st.fOffY, st.x + st.fOffX + st.fW, st.y + st.fOffY + st.fH)) continue
                st.aggro = true
                enemies.add(st)
                stalker = st
                toast("شيء ما يطاردك في الظلام...")
                Sound.play("boss", 0.9f)
                return
            }
        }
        var alive = 0
        for (en in enemies) if (!en.isDead) alive++
        val cap = (if (lowQuality) 5 else 9) + (if (night) 4 else 0)
        if (alive >= cap) return
        for (t in 0 until 12) {
            val a = rnd.nextFloat() * 6.2832f
            val r = 680f + rnd.nextFloat() * 320f
            val ex = player.cx + cos(a) * r
            val ey = player.cy + sin(a) * r
            if (ex < 200f || ey < 200f || ex > w.width - 200f || ey > w.height - 200f) continue
            if (!night && w.inVillage(ex, ey, 1.15f)) continue
            if (!w.freeAt(ex, ey)) continue
            val b = w.biomeAtPx(ex, ey)
            val eraForSpawn = if (night) EraType.ERA_3_ZOMBIE else EraType.values()[b]
            val en = Enemy(ex - 42f, ey - 60f, pickRole(b, night), eraForSpawn)
            if (w.blocked(en.x + en.fOffX, en.y + en.fOffY, en.x + en.fOffX + en.fW, en.y + en.fOffY + en.fH)) continue
            en.aggro = night
            en.isSiegeZombie = night
            if (night && rnd.nextFloat() < 0.34f) {
                en.isBomberZombie = true
            }
            enemies.add(en)
            break
        }
    }

    private fun harvestCropTile(w: OpenWorld, tx: Int, ty: Int) {
        val variety = w.cropVarietyAt(tx, ty)
        if (!w.harvestCropAt(tx, ty)) return
        LanMultiplayer.send("CROP|HARVEST|$tx|$ty|$variety")
        val px = tx * OpenWorld.TS + 32f
        val py = ty * OpenWorld.TS + 32f
        player.wheat += 2
        player.cropSeeds += 2
        player.apples += 1
        Sound.play("coin", 0.8f)
        particles.burst(px, py, 10, Color.parseColor("#FFD54F"), 150f, 5f, 0.45f)
        addFloat("+2 قمح  +2 بذور  +1 تفاح 🌾", px, py - 18f, Color.parseColor("#FFD54F"), 21f)
        advanceQuest("q_farm", 1)
    }

    /**
     * Night Bomber Zombie explosion:
     * - Destroys Wood walls and 1-block-thick Stone walls,
     * - CANNOT break 2-block-thick Stone walls (سمك 2 حجر)!
     */
    private fun bomberZombieExplode(w: OpenWorld, x: Float, y: Float) {
        explosions.add(ExplosionFx(x, y, 155f))
        Sound.play("explode")
        shake = max(shake, 14f)
        particles.burst(x, y, 24, Color.parseColor("#FF5722"), 310f, 7f, 0.6f)
        if (dist(player.cx, player.cy, x, y) < 145f) {
            player.takeDamage(42)
        }
        val ts = OpenWorld.TS
        val c0 = ((x - 140f) / ts).toInt()
        val c1 = ((x + 140f) / ts).toInt()
        val r0 = ((y - 140f) / ts).toInt()
        val r1 = ((y + 140f) / ts).toInt()
        var blockedBy2Stone = false
        var brokeSingleWall = false
        for (r in r0..r1) for (cc in c0..c1) {
            if (cc < 0 || r < 0 || cc >= OpenWorld.N || r >= OpenWorld.N) continue
            if (dist(cc * ts + 32f, r * ts + 32f, x, y) > 135f) continue
            val res = w.damagePlayerBlockAt(cc, r, 3, true)
            if (res == -1) blockedBy2Stone = true
            if (res == 2) brokeSingleWall = true
        }
        if (blockedBy2Stone) {
            addFloat("صمد الجدار الحجري المزدوج (سمك 2)! 🛡️🧱", x, y - 24f, Color.parseColor("#69F0AE"), 22f)
            toast("انفجر الزومبي المفجر لكن جدارك الحجري (سمك 2) صمد بالكامل!")
        } else if (brokeSingleWall) {
            toast("الزومبي المفجر فجّر الحائط! ابنِ جداراً بسمك 2 حجر وضع مدافع بالبارود لحماية بيتك!")
        }
    }

    private fun meleeTiles(w: OpenWorld) {
        val ts = OpenWorld.TS
        val c0 = floor(atkRect.left / ts).toInt()
        val c1 = floor(atkRect.right / ts).toInt()
        val r0 = floor(atkRect.top / ts).toInt()
        val r1 = floor(atkRect.bottom / ts).toInt()
        val usingPickaxe = (player.hotbarIdx == 1)
        for (r in r0..r1) for (cc in c0..c1) {
            if (cc < 0 || r < 0 || cc >= OpenWorld.N || r >= OpenWorld.N) continue
            if (w.isCropReadyAt(cc, r)) {
                harvestCropTile(w, cc, r)
                continue
            }
            val i = r * OpenWorld.N + cc
            if (!w.breakable(i)) continue
            val key = i + 10000000
            if (player.swingHit.contains(key)) continue
            player.swingHit.add(key)
            // Stone rocks, village stone walls, and stone bricks CANNOT be broken with a Sword! Only Pickaxe!
            if (w.isStoneMaterialAt(i) && !usingPickaxe) {
                val px = cc * ts + 32f
                val py = r * ts + 32f
                particles.burst(px, py, 4, Color.LTGRAY, 90f, 3.5f, 0.25f)
                addFloat("الحجر يتكسر بالمعول فقط! ⛏️", px, py - 16f, Color.LTGRAY, 17f)
                continue
            }
            hitOpenTile(w, i, usingPickaxe)
        }
    }

    /**
     * Regular explosions (Cannons, Bombs, TNT) NEVER break Stone or Stone Brick walls!
     * They only clear wooden structures or trees.
     */
    private fun openExplode(w: OpenWorld, x: Float, y: Float) {
        val ts = OpenWorld.TS
        val c0 = ((x - 160f) / ts).toInt()
        val c1 = ((x + 160f) / ts).toInt()
        val r0 = ((y - 160f) / ts).toInt()
        val r1 = ((y + 160f) / ts).toInt()
        for (r in r0..r1) for (cc in c0..c1) {
            if (cc < 0 || r < 0 || cc >= OpenWorld.N || r >= OpenWorld.N) continue
            val i = r * OpenWorld.N + cc
            if (!w.breakable(i)) continue
            // Stone & Stone Walls are 100% immune to bombs and cannons!
            if (w.isStoneMaterialAt(i)) continue
            if (dist(cc * ts + 32f, r * ts + 32f, x, y) > 150f) continue
            hitOpenTile(w, i, false)
            hitOpenTile(w, i, false)
        }
    }

    private fun hitOpenTile(w: OpenWorld, i: Int, withPickaxe: Boolean = true) {
        val tx = i % OpenWorld.N
        val ty = i / OpenWorld.N
        val px = tx * OpenWorld.TS + 32f
        val py = ty * OpenWorld.TS + 32f

        // Check if hitting a player-placed Minecraft block first!
        val pb = w.blockCodeAtTile(tx, ty)
        if (pb != 0) {
            if ((pb == BlockType.STONE_BRICK.code || pb == BlockType.FIRING_WINDOW.code) && !withPickaxe) {
                addFloat("يحتاج المعول! ⛏️", px, py - 16f, Color.LTGRAY, 18f)
                return
            }
            val removed = w.removeBlockAt(tx, ty)
            if (removed != null) {
                if (removed == BlockType.TNT_BLOCK) {
                    explode(px, py)
                } else {
                    player.addBlockCount(removed, 1)
                    Sound.play("chop", 0.8f)
                    particles.burst(px, py, 8, Color.parseColor("#FFE082"), 150f, 5f, 0.4f)
                    addFloat("+1 ${removed.nameAr}", px, py - 20f, Color.parseColor("#FFE082"), 20f)
                }
            }
            return
        }

        val k = w.kindAt(i)
        if ((k == OpenWorld.ROCK || k == OpenWorld.ORE || k == OpenWorld.WALL) && !withPickaxe) {
            return
        }
        val b = w.biome[i].toInt()
        val res = w.damageTile(i, dayNo)
        if (res == 0) return
        val broke = res == 2
        val n = if (broke) 2 else 1
        if (k == OpenWorld.TREE) {
            Sound.play("chop", 0.8f)
            player.wood += n
            addFloat("+$n خشب", px, py - 24f, Color.parseColor("#FFCC80"), 22f)
            particles.burst(px, py, 6, Color.parseColor("#66BB6A"), 140f, 5f, 0.4f)
            if (broke) {
                player.treeSaplings += 2
                player.cropSeeds += 1
                addFloat("+2 شتلة شجر 🌱 +1 بذور", px, py - 46f, Color.parseColor("#A5D6A7"), 20f)
                if ((b == 0 || b == 3) && rnd.nextFloat() < 0.45f) {
                    player.apples++
                }
            }
        } else if (k == OpenWorld.WALL) {
            Sound.play("mine", 0.85f)
            player.stone += n
            if (broke) player.blkStone += 2
            addFloat(if (broke) "+2 حجر +2 طوب بناء" else "+1 حجر", px, py - 24f, Color.parseColor("#CFD8DC"), 21f)
            particles.burst(px, py, 7, Color.parseColor("#B0BEC5"), 150f, 5f, 0.4f)
        } else if (k == OpenWorld.ROCK) {
            Sound.play("mine", 0.8f)
            player.stone += n
            addFloat("+$n حجر", px, py - 24f, Color.parseColor("#CFD8DC"), 22f)
            if (broke) {
                if (rnd.nextFloat() < 0.45f) {
                    player.iron += 1
                    addFloat("+1 حديد", px, py - 46f, Color.parseColor("#80D8FF"), 22f)
                }
                if (rnd.nextFloat() < 0.35f) {
                    player.gunpowder += 2
                    addFloat("+2 بارود خام 💥", px, py - 64f, Color.parseColor("#FFAB40"), 20f)
                }
            }
            particles.burst(px, py, 6, Color.parseColor("#B0BEC5"), 150f, 5f, 0.4f)
        } else if (k == OpenWorld.ORE) {
            Sound.play("mine", 0.9f)
            player.mats[b] += n
            addFloat("+$n ${Mats.names[b]}", px, py - 24f, Color.parseColor("#FFD740"), 22f)
            if (broke) {
                player.iron += 1
                player.gunpowder += 2
            }
            particles.burst(px, py, 8, OpenWorld.ORE_MUL[b], 170f, 5f, 0.5f)
        }
    }

    private fun drawOpenWorld(c: Canvas) {
        val w = ow ?: return
        val sh = shake
        val ox = if (sh > 0f) (rnd.nextFloat() - 0.5f) * sh * 2f else 0f
        val oy = if (sh > 0f) (rnd.nextFloat() - 0.5f) * sh * 2f else 0f
        c.save()
        c.scale(zoom, zoom)
        c.translate(-camera.x + ox, -camera.y + oy)
        val vx = camera.x - 24f
        val vy = camera.y - 24f
        val vw = camera.viewW + 48f
        val vh = camera.viewH + 48f

        if (inCave && caveMap != null) {
            val cm = caveMap!!
            cm.draw(c, vx, vy, vw, vh, time)
            cm.drawDecor(c, vx, vy, vw, vh)
            caveExitSurface.draw(c, time)
            caveExitNext?.draw(c, time)
            for (t in traps) t.draw(c)
            for (r in rocks) r.draw(c, time)
            for (cc in cocoons) cc.draw(c, time)
            for (tt in totems) tt.draw(c, time)
            for (ch in chests) ch.draw(c, time)
        } else {
            w.draw(c, vx, vy, vw, vh)
            // Draw Minecraft-style tile placement / breaker highlight cursor in front of player
            if (player.hotbarIdx >= 1 && !player.isDead) {
                val (tx, ty) = aimTileCoords()
                val rx = tx * OpenWorld.TS
                val ry = ty * OpenWorld.TS
                tmpR.set(rx + 2f, ry + 2f, rx + OpenWorld.TS - 2f, ry + OpenWorld.TS - 2f)
                val blk = player.selectedBlockType()
                val canPlace = blk != null && w.canPlaceBlockAt(tx, ty, blk, player.cx, player.cy)
                val col = when {
                    player.hotbarIdx == 1 -> Color.argb(180, 255, 193, 7)
                    canPlace -> Color.argb(180, 105, 240, 174)
                    else -> Color.argb(160, 255, 82, 82)
                }
                Ui.fillRound(c, tmpR, Color.argb(45, Color.red(col), Color.green(col), Color.blue(col)), 4f)
                screenPaint.style = Paint.Style.STROKE
                screenPaint.strokeWidth = 3f
                screenPaint.color = col
                c.drawRect(tmpR, screenPaint)
                screenPaint.style = Paint.Style.FILL
                if (blk == BlockType.STONE_BRICK || blk == BlockType.FIRING_WINDOW) {
                    val adjStone = w.isStoneWallTile(tx - 1, ty) || w.isStoneWallTile(tx + 1, ty) ||
                        w.isStoneWallTile(tx, ty - 1) || w.isStoneWallTile(tx, ty + 1)
                    val wallHint = if (adjStone) "جدار سمك 2 حجر (مضاد للزومبي المفجر 🛡️)" else "جدار سمك 1 حجر"
                    WText.draw(c, wallHint, rx + 32f, ry - 6f, 13.5f, if (adjStone) Color.parseColor("#69F0AE") else Color.parseColor("#FFE082"))
                }
            }
            w.drawTrees(c, vx, vy, vw, vh)
            for (dk in docks) if (abs(dk.x - player.cx) < vw && abs(dk.y - player.cy) < vh) dk.draw(c, time)
            for (m in mounds) if (abs(m.x - player.cx) < vw && abs(m.y - player.cy) < vh) m.draw(c)
            for (b in beds) if (abs(b.x - player.cx) < vw && abs(b.y - player.cy) < vh) b.draw(c)
            for (fn in furnaces) if (abs(fn.x - player.cx) < vw && abs(fn.y - player.cy) < vh) fn.draw(c, time)
            for (sc in storageChests) if (abs(sc.x - player.cx) < vw && abs(sc.y - player.cy) < vh) sc.draw(c, time)
            for (cp in campfires) if (abs(cp.x - player.cx) < vw && abs(cp.y - player.cy) < vh) cp.draw(c, time)
            for (ce in caveEntrances) if (abs(ce.x - player.cx) < vw && abs(ce.y - player.cy) < vh) ce.draw(c, time)
            if (abs(bossAltar.x - player.cx) < vw + 160f && abs(bossAltar.y - player.cy) < vh + 160f) {
                bossAltar.draw(c, player.abyssalSeals, colossalBoss != null && !colossalBoss!!.isDead)
            }
            for (mz in meteorZones) mz.draw(c)
            for (pt in owPortals) if (abs(pt.x - player.cx) < vw && abs(pt.y - player.cy) < vh) pt.draw(c)
            for (sn in structureNodes) if (abs(sn.x - player.cx) < vw && abs(sn.y - player.cy) < vh) sn.draw(c, time)
            for (bn in banners) if (abs(bn.x - player.cx) < vw && abs(bn.y - player.cy) < vh) bn.draw(c, time)
            for (ch in chests) if (abs(ch.x - player.cx) < vw && abs(ch.y - player.cy) < vh) ch.draw(c, time)
            for (cn in cannons) if (abs(cn.x - player.cx) < vw && abs(cn.y - player.cy) < vh) cn.draw(c)
            for (p in props) p.draw(c)
            table.draw(c, time)
            merchant.draw(c)
            for (sh in sheeps) if (abs(sh.x - player.cx) < vw && abs(sh.y - player.cy) < vh) sh.draw(c)
            for (vl in villagers) if (abs(vl.x - player.cx) < vw && abs(vl.y - player.cy) < vh) vl.draw(c)
        }

        for (co in coins) co.draw(c)
        for (en in enemies) if (en.isDead) en.draw(c, time)
        val pyy = player.y
        for (en in enemies) if (!en.isDead && en.y < pyy) en.draw(c, time)
        if (LanMultiplayer.isConnected && !inCave && coopPartner.y < pyy) coopPartner.draw(c, time)
        player.draw(c, time)
        if (LanMultiplayer.isConnected && !inCave && coopPartner.y >= pyy) coopPartner.draw(c, time)
        for (en in enemies) if (!en.isDead && en.y >= pyy) en.draw(c, time)
        for (pr in projectiles) pr.draw(c)
        for (ex in explosions) ex.draw(c)
        particles.draw(c)
        for (f in floats) f.draw(c)
        c.restore()

        drawAtmosphereMotes(c)
        val np = nightPaint
        val dk = darknessNow()
        if (np != null && dk > 0.03f) {
            np.alpha = (dk * 255f).toInt().coerceIn(0, 255)
            val sx = (player.cx - camera.x) * zoom
            val sy = (player.cy - camera.y) * zoom
            c.save()
            c.translate(sx, sy)
            c.drawRect(-curW * 2f, -curH * 2f, curW * 2f, curH * 2f, np)
            c.restore()
            drawDynamicLightHalos(c, dk)
        }
        // Dawn / Sunset / Blood Moon Atmospheric Sky Tint + Lightning Flash
        if (!inCave) {
            if (dayTime in 0.20f..0.30f || dayTime in 0.72f..0.82f) {
                Ui.fillRect(c, 0f, 0f, curW.toFloat(), curH.toFloat(), Color.argb(34, 255, 140, 50))
            } else if (weatherType == 3 && isNight()) {
                Ui.fillRect(c, 0f, 0f, curW.toFloat(), curH.toFloat(), Color.argb(42, 220, 20, 50))
            }
            if (lightningFlash > 0f) {
                Ui.fillRect(c, 0f, 0f, curW.toFloat(), curH.toFloat(), Color.argb((lightningFlash * 170f).toInt().coerceIn(0, 180), 210, 245, 255))
            }
        }
        if (dk > 0.4f && !inCave) {
            val uu = Ui.u
            for (ey in eyes) {
                val ex = (ey.x - camera.x) * zoom
                val eyy = (ey.y - camera.y) * zoom
                if (ex < -20f || ex > curW + 20f || eyy < -20f || eyy > curH + 20f) continue
                if (sin(time * 2.2f + ey.seed) < -0.6f) continue
                val a = (210 * min(1f, ey.life / 2f)).toInt().coerceIn(0, 255)
                Ui.circle(c, ex - 7f * uu, eyy, 3.2f * uu, Color.argb(a, 255, 60, 40))
                Ui.circle(c, ex + 7f * uu, eyy, 3.2f * uu, Color.argb(a, 255, 60, 40))
            }
        }
        val vg = vignette
        if (vg != null) {
            val low = player.hp.toFloat() / max(1, player.maxHp)
            var a = 0f
            if (hurtFlash > 0f) a = hurtFlash / 0.4f * 190f
            if (low < 0.3f) a = max(a, (0.3f - low) / 0.3f * (80f + 40f * sin(time * 6f)))
            if (a > 1f) {
                vg.alpha = a.toInt().coerceIn(0, 255)
                c.drawRect(0f, 0f, curW.toFloat(), curH.toFloat(), vg)
            }
        }
    }

    private fun drawOpenHud(c: Canvas) {
        val w = ow ?: return
        val u = Ui.u
        val sw = curW.toFloat()
        drawStatusPanel(c)
        val phh = (if (player.mats.any { it > 0 }) 168f else 138f) * u
        val hy = 14f * u + phh + 8f * u
        tmpR.set(14f * u, hy, 414f * u, hy + 38f * u)
        Ui.panel(c, tmpR, Color.parseColor("#BF360C"), Color.argb(205, 12, 16, 30), 10f)
        Ui.icon(c, "icons/apple.png", 34f * u, hy + 19f * u, 22f)
        val hb = RectF(50f * u, hy + 10f * u, 180f * u, hy + 28f * u)
        Ui.bar(c, hb, hunger / 100f, if (hunger > 40f) Color.parseColor("#FB8C00") else Color.parseColor("#E53935"))
        var fx = 196f * u
        Ui.icon(c, "icons/apple.png", fx, hy + 19f * u, 20f)
        Ui.text(c, "${player.apples}", fx + 12f * u, hy + 25f * u, 14f, Color.WHITE, Paint.Align.LEFT)
        fx += 50f * u
        Ui.icon(c, "icons/raw_meat.png", fx, hy + 19f * u, 20f)
        Ui.text(c, "${player.rawMeat}", fx + 12f * u, hy + 25f * u, 14f, Color.WHITE, Paint.Align.LEFT)
        fx += 50f * u
        Ui.icon(c, "icons/cooked_meat.png", fx, hy + 19f * u, 20f)
        Ui.text(c, "${player.cookedMeat}", fx + 12f * u, hy + 25f * u, 14f, Color.WHITE, Paint.Align.LEFT)
        fx += 50f * u
        Ui.icon(c, "icons/stew.png", fx, hy + 19f * u, 20f)
        Ui.text(c, "${player.stew}", fx + 12f * u, hy + 25f * u, 14f, Color.WHITE, Paint.Align.LEFT)

        if (!inCave) {
            var nearestMound: BuriedMound? = null
            var bestMD = 1e9f
            for (m in mounds) if (!m.isDug) {
                val d = dist(player.cx, player.cy, m.x, m.y)
                if (d < bestMD) { bestMD = d; nearestMound = m }
            }
            if (nearestMound != null) {
                val cy2 = hy + 44f * u
                tmpR.set(14f * u, cy2, 320f * u, cy2 + 30f * u)
                Ui.panel(c, tmpR, Color.parseColor("#FFD54F"), Color.argb(190, 12, 16, 30), 8f)
                Ui.icon(c, "icons/compass.png", 34f * u, cy2 + 15f * u, 20f)
                val meters = (bestMD / OpenWorld.TS).toInt()
                val dir = directionName(nearestMound.x - player.cx, nearestMound.y - player.cy)
                Ui.text(c, "بوصلة الكنز: $meters م ($dir)", 176f * u, cy2 + 20f * u, 13.5f, GOLD)
            }
        }

        val cw = min(460f * u, sw * 0.40f)
        tmpR.set(sw / 2f - cw / 2f, 10f * u, sw / 2f + cw / 2f, 40f * u)
        Ui.panel(c, tmpR, Color.parseColor("#6D4C41"), Color.argb(190, 12, 16, 30), 10f)
        val wIcon = when (weatherType) { 1 -> "🌧️ مطر"; 2 -> "⚡ رعد"; 3 -> "🩸 قمر دموي"; else -> "☀️ صحو" }
        Ui.text(c, "اليوم $dayNo • ${timeLabel()} • $wIcon", tmpR.centerX(), tmpR.centerY() + 6f * u, 15f, GOLD)
        tmpR.set(sw / 2f - cw / 2f, 44f * u, sw / 2f + cw / 2f, 54f * u)
        Ui.bar(c, tmpR, dayTime, if (isNight()) Color.parseColor("#5C6BC0") else Color.parseColor("#FFCA28"))

        val inV = !inCave && w.inVillage(player.cx, player.cy)
        val bi = w.biomeAtPx(player.cx, player.cy)
        tmpR.set(sw / 2f - cw / 2f, 60f * u, sw / 2f + cw / 2f, 88f * u)
        Ui.panel(c, tmpR, Color.parseColor("#455A64"), Color.argb(180, 10, 14, 26), 10f)
        val locLabel = when {
            inCave -> "${activeCave?.nameAr ?: "كهف"} (طابق $caveFloor/3)"
            player.inBoat -> "الإبحار بالمركب • ${biomeNames[bi]}"
            inV -> "منطقة قرية آمنة (${biomeNames[bi]})"
            else -> biomeNames[bi]
        }
        Ui.text(c, locLabel, tmpR.centerX(), tmpR.centerY() + 5.5f * u, 15f, if (inV) Color.parseColor("#A5D6A7") else Color.WHITE)

        // Skill Combo & Speed Rank badge (Fortnite/DMC style)
        if (player.comboCount >= 2) {
            val combR = RectF(sw / 2f - 140f * u, 92f * u, sw / 2f + 140f * u, 122f * u)
            Ui.panel(c, combR, Color.parseColor("#00E5FF"), Color.argb(215, 16, 22, 36))
            Ui.text(
                c, "كومبو x${player.comboCount} • ${player.skillRank()} (+${((player.comboDmgMul() - 1f) * 100).toInt()}% ضرر)",
                combR.centerX(), combR.centerY() + 5.5f * u, 14f, Color.parseColor("#00E5FF")
            )
        }

        // Colossal Endgame Boss 5-Phase Health Bar
        val cb = colossalBoss
        if (cb != null && !cb.isDead) {
            val bwid = min(560f * u, sw * 0.46f)
            val topY = if (player.comboCount >= 2) 126f * u else 94f * u
            tmpR.set(sw / 2f - bwid / 2f, topY, sw / 2f + bwid / 2f, topY + 46f * u)
            Ui.panel(c, tmpR, Color.parseColor("#FF1744"), Color.argb(235, 28, 10, 18))
            val shTxt = if (cb.shielded) " [درع منيع: فجّره بـ TNT أو صد عاكس!]" else " (المرحلة ${cb.bossPhase}/5)"
            Ui.text(c, "إمبراطور الهاوية والزومبي العملاق$shTxt", tmpR.centerX(), tmpR.top + 18f * u, 14.5f, GOLD)
            val br = RectF(tmpR.left + 12f * u, tmpR.top + 24f * u, tmpR.right - 12f * u, tmpR.bottom - 6f * u)
            Ui.bar(c, br, cb.hp.toFloat() / max(1, cb.maxHp), if (cb.shielded) Color.parseColor("#00E5FF") else Color.parseColor("#FF1744"))
            Ui.text(c, "${cb.hp} / ${cb.maxHp}", br.centerX(), br.centerY() + 4.5f * u, 11.5f, Color.WHITE)
        }

        val mw = 190f * u
        val mx = sw - 14f * u - mw
        val my = 76f * u
        tmpR.set(mx - 4f * u, my - 4f * u, mx + mw + 4f * u, my + mw + 4f * u)
        Ui.panel(c, tmpR, Color.parseColor("#455A64"), Color.argb(215, 6, 8, 16), 8f)
        val mmR = RectF(mx, my, mx + mw, my + mw)
        if (inCave && caveMap != null) {
            c.drawBitmap(caveMap!!.minimap, null, mmR, null)
            val fx2 = mw / caveMap!!.width
            val fy2 = mw / caveMap!!.height
            for (en in enemies) if (!en.isDead) Ui.circle(c, mx + en.cx * fx2, my + en.cy * fy2, 3f * u, Color.RED)
            Ui.circle(c, mx + caveExitSurface.x * fx2, my + caveExitSurface.y * fy2, 4f * u, Color.GREEN)
            caveExitNext?.let { Ui.circle(c, mx + it.x * fx2, my + it.y * fy2, 4.5f * u, Color.MAGENTA) }
            Ui.circle(c, mx + player.cx * fx2, my + player.cy * fy2, 4f * u, Color.WHITE)
        } else {
            c.drawBitmap(w.minimap, null, mmR, null)
            val f = mw / w.width
            for (sv in w.subVillageTiles) Ui.circle(c, mx + sv.first * OpenWorld.TS * f, my + sv.second * OpenWorld.TS * f, 3.5f * u, Color.parseColor("#69F0AE"))
            for (ce in caveEntrances) Ui.circle(c, mx + ce.x * f, my + ce.y * f, 3.2f * u, Color.CYAN)
            for (ft in w.fortressTiles) Ui.circle(c, mx + ft.first * OpenWorld.TS * f, my + ft.second * OpenWorld.TS * f, 3.5f * u, Color.parseColor("#FF5252"))
            for (sn in structureNodes) if (!sn.claimed) Ui.circle(c, mx + sn.x * f, my + sn.y * f, 3.4f * u, sn.accentColor)
            if (waypointX > 0f && waypointY > 0f) {
                Ui.circle(c, mx + waypointX * f, my + waypointY * f, 5f * u, GOLD)
            }
            for (en in enemies) {
                if (en.isDead) continue
                if (dist(en.cx, en.cy, player.cx, player.cy) > 2000f) continue
                Ui.circle(c, mx + en.cx * f, my + en.cy * f, 2.5f * u, Color.parseColor("#FF8A80"))
            }
            Ui.circle(c, mx + OpenWorld.CX * OpenWorld.TS * f, my + OpenWorld.CY * OpenWorld.TS * f, 4.5f * u, Color.parseColor("#FFD54F"))
            if (LanMultiplayer.isConnected) {
                Ui.circle(c, mx + coopPartner.cx * f, my + coopPartner.cy * f, 4.8f * u, Color.parseColor("#00E5FF"))
            }
            Ui.circle(c, mx + player.cx * f, my + player.cy * f, 4f * u, Color.WHITE)
        }
        // If in Co-Op Multiplayer, show Partner Status & Distance Bar on left HUD!
        if (LanMultiplayer.isConnected) {
            val dPart = (dist(player.cx, player.cy, coopPartner.cx, coopPartner.cy) / OpenWorld.TS).toInt()
            val dirPart = directionName(coopPartner.cx - player.cx, coopPartner.cy - player.cy)
            val coopR = RectF(14f * u, hy + 78f * u, 360f * u, hy + 110f * u)
            Ui.panel(c, coopR, if (coopPartner.isDowned) Color.parseColor("#FF1744") else Color.parseColor("#00E676"), Color.argb(215, 10, 22, 32), 8f)
            val pTxt = if (coopPartner.isDowned) "⚠️ صديقك سقط! ($dPart م $dirPart) أنعشه!" else "🤝 ${coopPartner.nameAr}: ${coopPartner.hp}/${coopPartner.maxHp} ($dPart م $dirPart)"
            Ui.text(c, pTxt, coopR.centerX(), coopR.centerY() + 5f * u, 12.5f, Color.WHITE)
        }
        // Label under Minimap: Tap to open full Interactive World & Structure Map!
        val mapHintR = RectF(mx - 4f * u, my + mw + 4f * u, mx + mw + 4f * u, my + mw + 26f * u)
        Ui.panel(c, mapHintR, GOLD, Color.argb(220, 14, 20, 34), 6f)
        Ui.text(c, "اضغط للخريطة والستركشرات 🗺️", mapHintR.centerX(), mapHintR.centerY() + 4.5f * u, 11.5f, GOLD)

        // If player locked a Waypoint on a Structure/Landmark, show live Compass Bar under top bar!
        if (waypointX > 0f && waypointY > 0f && !inCave) {
            val dWp = dist(player.cx, player.cy, waypointX, waypointY)
            val mWp = (dWp / OpenWorld.TS).toInt()
            val dirWp = directionName(waypointX - player.cx, waypointY - player.cy)
            val wpBar = RectF(mx - 4f * u, my + mw + 30f * u, mx + mw + 4f * u, my + mw + 54f * u)
            Ui.panel(c, wpBar, Color.parseColor("#00E5FF"), Color.argb(215, 10, 24, 38), 6f)
            Ui.text(c, "📍 $waypointTitle ($mWp م $dirWp)", wpBar.centerX(), wpBar.centerY() + 4.5f * u, 11f, Color.parseColor("#80DEEA"))
        }
        drawControls(c)
    }

    private fun directionName(dx: Float, dy: Float): String {
        val a = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        return when {
            a in -22.5f..22.5f -> "شرق"
            a in 22.5f..67.5f -> "جنوب شرق"
            a in 67.5f..112.5f -> "جنوب"
            a in 112.5f..157.5f -> "جنوب غرب"
            a < -157.5f || a > 157.5f -> "غرب"
            a in -157.5f..-112.5f -> "شمال غرب"
            a in -112.5f..-67.5f -> "شمال"
            else -> "شمال شرق"
        }
    }

    // ---- title / end screens
    private val heroSheet = SpriteSheet("sprites/player/idle.png", 0.2f)

    private fun drawTitle(c: Canvas) {
        val u = Ui.u
        val w = curW.toFloat()
        val h = curH.toFloat()
        val camX = (world.width - camera.viewW) / 2f + sin(titleT * 0.12f) * 520f
        val camY = (world.height - camera.viewH) / 2f + sin(titleT * 0.09f + 1f) * 300f
        c.save()
        c.scale(zoom, zoom)
        c.translate(-camX, -camY)
        world.draw(c, camX, camY, camera.viewW, camera.viewH, titleT)
        world.drawDecor(c, camX, camY, camera.viewW, camera.viewH)
        c.restore()
        Ui.fillRect(c, 0f, 0f, w, h, Color.argb(150, 4, 6, 16))
        Ui.text(c, "حرب الفارس: ماين كرافت وحرب الزومبي", w / 2f, h * 0.17f, 52f, GOLD)
        Ui.text(c, "البقاء • البناء القتالي (فورتنايت) • الزعيم العملاق (5 مراحل)", w / 2f, h * 0.17f + 44f * u, 23f, Color.parseColor("#80DEEA"))
        Ui.text(c, "قوائم ماين كرافت • جدران ونوافذ قتالية فورية • صد عاكس وخطاف • تخصيص أماكن الأزرار", w / 2f, h * 0.17f + 74f * u, 15.5f, Color.rgb(225, 228, 238), Paint.Align.CENTER, false)
        heroSheet.update(0.016f)
        heroSheet.draw(c, w / 2f - 62f * u, h * 0.27f, 124f * u, 124f * u)
        val bw = 390f * u
        var y = h * 0.27f + 128f * u
        val ro = RectF(w / 2f - bw / 2f, y, w / 2f + bw / 2f, y + 46f * u)
        Ui.button(c, ro, "العالم المفتوح (بقاء وحرب الزومبي والزعيم العملاق) ⚔️🧱", Color.parseColor("#2E7D32"), true, 17.5f)
        hits.add(Hit(ro, "open"))
        y += 52f * u
        val rMp = RectF(w / 2f - bw / 2f, y, w / 2f + bw / 2f, y + 46f * u)
        Ui.button(c, rMp, "اللعب الجماعي مع صديقك (واي فاي / نقطة اتصال) 🌐🤝", Color.parseColor("#00897B"), true, 17.5f)
        hits.add(Hit(rMp, "multiplayer_title"))
        y += 52f * u
        val rHud = RectF(w / 2f - bw / 2f, y, w / 2f + bw / 2f, y + 44f * u)
        Ui.button(c, rHud, "تخصيص أماكن وحجم الأزرار (HUD فورتنايت) ⚙️", Color.parseColor("#0277BD"), true, 18f)
        hits.add(Hit(rHud, "hud_edit_title"))
        y += 56f * u
        if (SaveStore.hasSave()) {
            val r = RectF(w / 2f - bw / 2f, y, w / 2f + bw / 2f, y + 48f * u)
            Ui.button(c, r, "متابعة الحفظ الأخير ▶", Color.parseColor("#6A1B9A"), true, 19f)
            hits.add(Hit(r, "cont"))
            y += 56f * u
        }
        val r2 = RectF(w / 2f - bw / 2f, y, w / 2f + bw / 2f, y + 48f * u)
        Ui.button(c, r2, "طور القصة (العصور الستة)", Color.parseColor("#546E7A"), true, 18.5f)
        hits.add(Hit(r2, "new"))
        val s1 = RectF(w - 330f * u, h - 64f * u, w - 176f * u, h - 16f * u)
        Ui.button(c, s1, "المؤثرات: " + (if (Sound.sfxOn) "نعم" else "لا"), Color.parseColor("#37474F"), true, 15f)
        hits.add(Hit(s1, "sfx"))
        val s2 = RectF(w - 166f * u, h - 64f * u, w - 16f * u, h - 16f * u)
        Ui.button(c, s2, "الموسيقى: " + (if (Sound.musicOn) "نعم" else "لا"), Color.parseColor("#37474F"), true, 15f)
        hits.add(Hit(s2, "music"))
        val s3 = RectF(16f * u, h - 64f * u, 236f * u, h - 16f * u)
        Ui.button(c, s3, "الجودة: " + (if (lowQuality) "منخفضة" else "عالية"), Color.parseColor("#00838F"), true, 15f)
        hits.add(Hit(s3, "quality"))
    }

    private fun drawGameOver(c: Canvas) {
        val u = Ui.u
        val w = curW.toFloat()
        val h = curH.toFloat()
        Ui.fillRect(c, 0f, 0f, w, h, Color.argb(min(200, (endT * 300f).toInt()), 40, 0, 0))
        val pr = RectF(w / 2f - 260f * u, h / 2f - 170f * u, w / 2f + 260f * u, h / 2f + 170f * u)
        Ui.panel(c, pr, Color.parseColor("#B71C1C"), Color.argb(245, 22, 10, 14), 16f)
        Ui.text(c, "سقط الفارس", pr.centerX(), pr.top + 66f * u, 44f, Color.parseColor("#FF5252"))
        Ui.text(c, "الزمن لم ينتهِ بعد... انهض من جديد", pr.centerX(), pr.top + 104f * u, 18f, Color.rgb(230, 220, 224), Paint.Align.CENTER, false)
        val r1 = RectF(pr.centerX() - 160f * u, pr.top + 132f * u, pr.centerX() + 160f * u, pr.top + 192f * u)
        Ui.button(c, r1, "حاول مرة أخرى", Color.parseColor("#2E7D32"), endT > 0.8f, 21f)
        hits.add(Hit(r1, "retry"))
        val r2 = RectF(pr.centerX() - 160f * u, pr.top + 208f * u, pr.centerX() + 160f * u, pr.top + 268f * u)
        Ui.button(c, r2, "القائمة الرئيسية", Color.parseColor("#455A64"), endT > 0.8f, 21f)
        hits.add(Hit(r2, "title"))
    }

    private fun drawVictory(c: Canvas) {
        val u = Ui.u
        val w = curW.toFloat()
        val h = curH.toFloat()
        Ui.fillRect(c, 0f, 0f, w, h, Color.rgb(10, 8, 24))
        for (i in 0 until 60) {
            val sx = ((i * 97) % 100) / 100f * w
            val sy = (((i * 53) % 100) / 100f * h + endT * (10f + (i % 5) * 6f)) % h
            Ui.circle(c, sx, sy, (1.5f + (i % 3)) * u, Color.argb(200, 255, 240, 180))
        }
        Ui.text(c, "تحرر الزمن", w / 2f, h * 0.25f, 66f, GOLD)
        Ui.text(c, "جمعتَ الشظايا الست وأصلحتَ حجر كرونو", w / 2f, h * 0.25f + 50f * u, 24f, Color.parseColor("#80DEEA"))
        Ui.wrapped(
            c, "لم يكن العدو في أي عصر سوى صدى لقرارٍ قديم. قبلتَ ما حدث فاستأنف الزمن مساره... لكن الليل صار أطول، وشيءٌ ما في الظلام استيقظ.",
            w / 2f - 360f * u, h * 0.25f + 78f * u, 720f * u, 21f, Color.rgb(230, 232, 240), false, 3, true, false
        )
        Ui.icon(c, "icons/coin.png", w / 2f - 70f * u, h * 0.25f + 200f * u, 32f)
        Ui.text(c, "${player.coins}", w / 2f - 46f * u, h * 0.25f + 210f * u, 28f, GOLD, Paint.Align.LEFT)
        val ro = RectF(w / 2f - 190f * u, h * 0.25f + 244f * u, w / 2f + 190f * u, h * 0.25f + 304f * u)
        Ui.button(c, ro, "ابدأ العالم المفتوح", Color.parseColor("#6A1B9A"), endT > 0.8f, 22f)
        hits.add(Hit(ro, "open"))
        val r = RectF(w / 2f - 190f * u, h * 0.25f + 316f * u, w / 2f + 190f * u, h * 0.25f + 372f * u)
        Ui.button(c, r, "القائمة الرئيسية", Color.parseColor("#455A64"), endT > 0.8f, 20f)
        hits.add(Hit(r, "title"))
    }
}
