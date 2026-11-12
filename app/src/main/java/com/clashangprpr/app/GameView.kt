package com.clashangprpr.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.clashangprpr.app.engine.ActionButton
import com.clashangprpr.app.engine.Camera2D
import com.clashangprpr.app.engine.CollisionSystem
import com.clashangprpr.app.engine.SaveManager
import com.clashangprpr.app.engine.SoundEngine
import com.clashangprpr.app.engine.SpriteCache
import com.clashangprpr.app.engine.VirtualJoystick
import com.clashangprpr.app.model.BuriedTreasureMound
import com.clashangprpr.app.model.ChoppableTree
import com.clashangprpr.app.model.ChronomancerNPC
import com.clashangprpr.app.model.CoinDrop
import com.clashangprpr.app.model.DungeonTrap
import com.clashangprpr.app.model.Enemy
import com.clashangprpr.app.model.EnemyRole
import com.clashangprpr.app.model.EraType
import com.clashangprpr.app.model.ExplosionEffect
import com.clashangprpr.app.model.FloatingText
import com.clashangprpr.app.model.GameCatalogue
import com.clashangprpr.app.model.HubGate
import com.clashangprpr.app.model.ItemType
import com.clashangprpr.app.model.MerchantNPC
import com.clashangprpr.app.model.FarmPlot
import com.clashangprpr.app.model.LootVault
import com.clashangprpr.app.model.PlayerGrave
import com.clashangprpr.app.model.RareMiningNode
import com.clashangprpr.app.model.MiningRock
import com.clashangprpr.app.model.PlantedExplosive
import com.clashangprpr.app.model.PlayerBarricade
import com.clashangprpr.app.model.ParticleSystem
import com.clashangprpr.app.model.Player
import com.clashangprpr.app.model.PowerGeneratorNode
import com.clashangprpr.app.model.Projectile
import com.clashangprpr.app.model.TimePortal
import com.clashangprpr.app.model.TreasureChest
import com.clashangprpr.app.model.WoodCrate
import com.clashangprpr.app.model.WorldProp
import com.clashangprpr.app.story.DialogueLine
import com.clashangprpr.app.story.StoryManager
import com.clashangprpr.app.systems.PotionType
import com.clashangprpr.app.systems.ProgressionSystem
import com.clashangprpr.app.ui.GameDialogs
import com.clashangprpr.app.ui.GameHUD
import com.clashangprpr.app.ui.RpgTab
import com.clashangprpr.app.model.AlchemistNPC
import com.clashangprpr.app.model.BlacksmithNPC
import com.clashangprpr.app.model.QuestGiverNPC
import com.clashangprpr.app.model.SageNPC
import com.clashangprpr.app.world.EraLayout
import com.clashangprpr.app.world.OpenWorldChunkSystem
import com.clashangprpr.app.world.WorldEnvironmentSystem
import kotlin.math.sqrt
import kotlin.random.Random

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    private var gameThread: Thread? = null
    @Volatile private var isRunning = false
    @Volatile private var isSurfaceReady = false

    private var mapWidth = 2160f
    private var mapHeight = 1440f

    private var camera: Camera2D = Camera2D(1280f, 720f, mapWidth, mapHeight)
    private var joystick: VirtualJoystick = VirtualJoystick(180f, 540f, 120f, 50f)

    // Clean 4 Tactical Action Buttons (Arc around right thumb)
    private val attackBtn = ActionButton(1140f, 540f, 66f, "تنفيذ", Color.argb(220, 229, 57, 53))
    private val interactBtn = ActionButton(995f, 565f, 50f, "تفاعل", Color.argb(220, 30, 136, 229))
    private val dashBtn = ActionButton(1010f, 425f, 50f, "صد/اندفاع", Color.argb(220, 156, 39, 176))
    private val slowMoBtn = ActionButton(1145f, 385f, 50f, "تباطؤ/سحر", Color.argb(220, 0, 176, 255))

    private var currentEra: EraType = EraType.ERA_0_HUB
    private var completedErasMask: Int = 0
    private val hubGates = mutableListOf<HubGate>()
    private var chronomancer: ChronomancerNPC? = null

    private lateinit var player: Player
    private val prog = ProgressionSystem()
    private val worldEnv = WorldEnvironmentSystem()
    private val chunkSystem = OpenWorldChunkSystem()
    private var alchemistNpc: AlchemistNPC? = null
    private var blacksmithNpc: BlacksmithNPC? = null
    private var sageNpc: SageNPC? = null
    private val questGivers = mutableListOf<QuestGiverNPC>()
    private var openWorldRespawnTimer = 0f
    private var isAlchemistOpen = false

    private val enemies = mutableListOf<Enemy>()
    private val chests = mutableListOf<TreasureChest>()
    private val props = mutableListOf<WorldProp>()
    private val traps = mutableListOf<DungeonTrap>()
    private val coinDrops = mutableListOf<CoinDrop>()
    private val floatingTexts = mutableListOf<FloatingText>()
    private val projectiles = mutableListOf<Projectile>()
    private val explosions = mutableListOf<ExplosionEffect>()
    private val miningRocks = mutableListOf<MiningRock>()
    private val woodCrates = mutableListOf<WoodCrate>()
    private val choppableTrees = mutableListOf<ChoppableTree>()
    private val powerGenerators = mutableListOf<PowerGeneratorNode>()
    private val plantedExplosives = mutableListOf<PlantedExplosive>()
    private val barricades = mutableListOf<PlayerBarricade>()
    private val lootVaults = mutableListOf<LootVault>()
    private val farmPlots = mutableListOf<FarmPlot>()
    private val rareNodes = mutableListOf<RareMiningNode>()
    private var playerGrave: PlayerGrave? = null
    private var hungerDamageTimer = 0f
    private val particles = ParticleSystem(150)

    private val staticObstacles = mutableListOf<RectF>()
    private val activeObstacles = mutableListOf<RectF>()

    private var buriedMound: BuriedTreasureMound? = null
    private val extraBuriedMounds = mutableListOf<BuriedTreasureMound>()
    private var islandWaveTimer = 0f

    private var shakeTimer = 0f
    private var shakeMagnitude = 0f
    private var hitStopTimer = 0f

    private val hud = GameHUD()
    private val dialogs = GameDialogs()

    private var currentBgBitmap: Bitmap? = null
    private var darkTotemRgba: Bitmap? = null
    private var frameShopBmp: Bitmap? = null
    private var frameInventoryBmp: Bitmap? = null
    private var frameCharPanelBmp: Bitmap? = null
    private val iconCache = mutableMapOf<String, Bitmap>()

    private var merchant: MerchantNPC? = null
    private lateinit var portal: TimePortal

    private var craftTableX = 520f
    private var craftTableY = 380f
    private val craftTableW = 80f
    private val craftTableH = 80f
    private val craftTableRect = RectF()

    private var gateX = 1460f
    private var gateY = 720f
    private var isGateUnlocked = false

    private var isDialogueOpen = false
    private val dialogueQueue = mutableListOf<DialogueLine>()
    private var currentDialogue: DialogueLine? = null
    private var isShopOpen = false
    private var isCraftingOpen = false
    private var isRpgMenuOpen = false
    private var selectedEnding = 1

    private val craftTablePaint = Paint().apply {
        color = Color.parseColor("#3E2723")
        style = Paint.Style.FILL
    }
    private val craftLabelPaint = Paint().apply {
        color = Color.parseColor("#E1BEE7")
        textSize = 20f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val attackHitbox = RectF()
    private val totemRect = RectF()

    enum class GameState { TITLE, PLAYING, GAME_OVER, ENDING_CHOICE, VICTORY }
    private var state = GameState.TITLE
    private var hasSavedGame = false
    private var savedEra: EraType? = null

    init {
        holder.addCallback(this)
        isFocusable = true
        loadStaticBitmaps()
        hasSavedGame = SaveManager.hasSavedGame(context)
        savedEra = if (hasSavedGame) SaveManager.getSavedEra(context) else null
        initNewGame()
    }

    private fun loadStaticBitmaps() {
        chunkSystem.initAssets(context)
        darkTotemRgba = SpriteCache.getBitmap(context, "environment/dark_totem_rgba.png")
        frameShopBmp = SpriteCache.getBitmap(context, "ui/frame_shop.png")
        frameInventoryBmp = SpriteCache.getBitmap(context, "ui/frame_inventory.png")
        frameCharPanelBmp = SpriteCache.getBitmap(context, "ui/frame_character_panel.png")

        val iconPaths = listOf(
            "icons/sword_scimitar.png", "icons/sword_flame.png", "icons/item_10.png",
            "icons/potion_health.png", "icons/potion_2.png", "icons/potion_3.png", "icons/potion_mana.png",
            "icons/item_1.png", "icons/item_2.png", "icons/item_3.png", "icons/item_4.png",
            "icons/item_5.png", "icons/item_15.png", "icons/item_20.png", "icons/pirate_1.png", "icons/pirate_3.png", "icons/pirate_5.png", "icons/ammo_bullet.png"
        )
        for (p in iconPaths) {
            SpriteCache.getBitmap(context, p)?.let { iconCache[p] = it }
        }
    }

    private fun initNewGame() {
        currentEra = EraType.ERA_1_MEDIEVAL
        completedErasMask = 0
        player = Player(context, 260f, 700f)
        portal = TimePortal(1920f, 720f)
        setupEra(currentEra, showIntro = true)
    }

    private fun continueSavedGame() {
        player = Player(context, 1030f, 700f)
        completedErasMask = SaveManager.getCompletedErasMask(context)
        val loadedEra = SaveManager.loadPlayerState(context, player, prog)
        portal = TimePortal(-500f, -500f)
        setupEra(loadedEra, showIntro = false)
        state = GameState.PLAYING
    }

    private fun triggerScreenShake(duration: Float, magnitude: Float) {
        shakeTimer = duration
        shakeMagnitude = magnitude
    }

    private fun grantXp(amount: Int) {
        if (prog.addXp(amount)) {
            SoundEngine.playLevelUp()
            player.maxMana = 100f + prog.getBonusMaxMana()
            player.recalculateMaxHp(prog.getBonusMaxHp())
            player.hp = player.maxHp
            player.mana = player.maxMana
            floatingTexts.add(FloatingText("ارتقيت للمستوى ${prog.level}!", player.x - 40f, player.y - 50f, Color.parseColor("#E040FB"), isCrit = true))
        }
    }

    private fun setupEra(era: EraType, showIntro: Boolean = true) {
        currentEra = era
        mapWidth = era.mapWidth
        mapHeight = era.mapHeight
        camera.mapWidth = mapWidth
        camera.mapHeight = mapHeight
        worldEnv.onEraChanged(era)

        currentBgBitmap = SpriteCache.getBitmap(context, era.bgAsset)

        enemies.clear()
        chests.clear()
        props.clear()
        traps.clear()
        coinDrops.clear()
        floatingTexts.clear()
        projectiles.clear()
        explosions.clear()
        particles.clear()
        miningRocks.clear()
        woodCrates.clear()
        choppableTrees.clear()
        powerGenerators.clear()
        plantedExplosives.clear()
        barricades.clear()
        lootVaults.clear()
        farmPlots.clear()
        rareNodes.clear()
        hubGates.clear()
        chronomancer = null
        alchemistNpc = null
        blacksmithNpc = null
        sageNpc = null
        questGivers.clear()
        openWorldRespawnTimer = 0f
        staticObstacles.clear()
        buriedMound = null
        extraBuriedMounds.clear()
        islandWaveTimer = 0f

        val bp = EraLayout.getBlueprint(era)
        player.x = bp.playerStart.x
        player.y = bp.playerStart.y
        player.hp = player.maxHp
        player.mana = player.maxMana
        player.isDead = false

        portal.x = bp.portalPos.x
        portal.y = bp.portalPos.y
        portal.isActive = false
        isGateUnlocked = false
        gateX = bp.gatePos.x
        gateY = bp.gatePos.y

        craftTableX = bp.craftTablePos.x
        craftTableY = bp.craftTablePos.y
        craftTableRect.set(craftTableX, craftTableY, craftTableX + craftTableW, craftTableY + craftTableH)

        staticObstacles.addAll(bp.staticObstacles)
        staticObstacles.add(craftTableRect)

        merchant = MerchantNPC(context, bp.merchantPos.x, bp.merchantPos.y).also {
            staticObstacles.add(it.hitbox)
        }
        props.add(WorldProp(context, "environment/fire.png", 5, bp.firePos.x, bp.firePos.y, 64f, 64f))
        props.add(WorldProp(context, "environment/torch.png", 4, bp.merchantPos.x - 140f, bp.merchantPos.y - 80f, 32f, 64f))
        props.add(WorldProp(context, "environment/torch.png", 4, bp.craftTablePos.x + 180f, bp.craftTablePos.y - 80f, 32f, 64f))

        for ((pt, isIron) in bp.rockCoords) miningRocks.add(MiningRock(context, pt.x, pt.y, isIron))
        for ((pt, isBarrel) in bp.crateCoords) woodCrates.add(WoodCrate(context, pt.x, pt.y, isBarrel))
        for ((pt, isPine) in bp.treeCoords) choppableTrees.add(ChoppableTree(context, pt.x, pt.y, isPine))
        for ((pt, gold) in bp.chestCoords) chests.add(TreasureChest(context, pt.x, pt.y, gold))
        for ((pt, isSaw) in bp.trapCoords) traps.add(DungeonTrap(context, pt.x, pt.y, isSaw))

        totemRect.set(bp.gatePos.x - 40f, bp.gatePos.y + 120f, bp.gatePos.x + 80f, bp.gatePos.y + 240f)

        when (era) {
            EraType.ERA_0_HUB -> {
                chronomancer = ChronomancerNPC(context, 1030f, 520f).also {
                    staticObstacles.add(it.hitbox)
                }
                val gatesSpec = listOf(
                    Triple(420f, 320f, EraType.ERA_1_MEDIEVAL to "1: القلعة"),
                    Triple(1080f, 240f, EraType.ERA_2_CAVES to "2: الكهوف"),
                    Triple(1740f, 320f, EraType.ERA_3_ZOMBIE to "3: الزومبي"),
                    Triple(420f, 1120f, EraType.ERA_4_ISLAND to "4: الجزيرة"),
                    Triple(1080f, 1200f, EraType.ERA_5_CYBER to "5: السايبربنك"),
                    Triple(1740f, 1120f, EraType.ERA_6_VOID to "6: الفراغ")
                )
                for ((gx, gy, info) in gatesSpec) {
                    val target = info.first
                    val done = (completedErasMask and (1 shl target.ordinal)) != 0
                    hubGates.add(HubGate(gx, gy, target, info.second, done))
                }
            }
            EraType.ERA_1_MEDIEVAL -> {
                enemies.add(Enemy(context, 1020f, 520f, EnemyRole.SPEARMAN, era))
                enemies.add(Enemy(context, 1220f, 460f, EnemyRole.ASSASSIN, era))
                enemies.add(Enemy(context, 1380f, 360f, EnemyRole.ARCHER, era))
                enemies.add(Enemy(context, 1620f, 420f, EnemyRole.MAGE, era))
                enemies.add(Enemy(context, 1340f, 760f, EnemyRole.HEAVY_SHIELD, era, holdsKey = true))
                enemies.add(Enemy(context, 1780f, 740f, EnemyRole.BOSS, era))
            }
            EraType.ERA_2_OPEN_WORLD -> {
                chunkSystem.updateAroundPlayer(player.x, player.y)
                // Village Safe Zone (North-West: 950, 950)
                sageNpc = SageNPC(context, 820f, 880f).also { staticObstacles.add(it.hitbox) }
                blacksmithNpc = BlacksmithNPC(context, 1820f, 1180f).also { staticObstacles.add(it.hitbox) }
                // Far Alchemist Outpost (East-Center: 17200, 14000)
                alchemistNpc = AlchemistNPC(context, 17200f, 14000f).also { staticObstacles.add(it.hitbox) }

                // Farming Plots in the Safe Village!
                farmPlots.add(FarmPlot(1120f, 920f))
                farmPlots.add(FarmPlot(1230f, 920f))
                farmPlots.add(FarmPlot(1120f, 1040f))
                farmPlots.add(FarmPlot(1230f, 1040f))
                // 6 Buried Treasures across the Open World (need Pickaxe!)
                val moundSpots = listOf(
                    3500f to 3200f, 9200f to 6800f, 14500f to 22000f,
                    22000f to 24000f, 24000f to 11000f, 12000f to 16000f
                )
                for ((mx, my) in moundSpots) extraBuriedMounds.add(BuriedTreasureMound(mx, my))

                // Rare Mining Zone Nodes (Caves & Mountains: 4000..9000, 12000..18000)
                val rareSpots = listOf(
                    Triple(4200f, 13000f, 2), Triple(4600f, 13400f, 2), Triple(5100f, 14200f, 3),
                    Triple(5800f, 14800f, 2), Triple(6400f, 15200f, 3), Triple(7200f, 14600f, 3),
                    Triple(2600f, 2400f, 1), Triple(3100f, 2600f, 1), Triple(8200f, 16200f, 2)
                )
                for ((rx, ry, ot) in rareSpots) rareNodes.add(RareMiningNode(context, rx, ry, ot))

                questGivers.add(QuestGiverNPC(context, 1280f, 1340f, "عمدة القرية", "q_open_soldiers").also { staticObstacles.add(it.hitbox) })
                questGivers.add(QuestGiverNPC(context, 4200f, 4500f, "رحالة الغابة", "q_open_alchemist").also { staticObstacles.add(it.hitbox) })
                questGivers.add(QuestGiverNPC(context, 6200f, 21000f, "الناجي المقاتل", "q_prison").also { staticObstacles.add(it.hitbox) })

                // Loot Vaults across the 32768x32768 World
                lootVaults.add(LootVault(16500f, 11500f, 260, "خزينة العصابات"))
                lootVaults.add(LootVault(5100f, 24500f, 350, "خزينة السجن المحصن"))
                lootVaults.add(LootVault(27600f, 27600f, 550, "كنز المحفل السري"))

                // 6 Regional Bosses across the Open World (Each summons 10-soldier waves!)
                enemies.add(Enemy(context, 8500f, 4800f, EnemyRole.BOSS, EraType.ERA_1_MEDIEVAL))
                enemies.add(Enemy(context, 5500f, 15500f, EnemyRole.BOSS, EraType.ERA_2_CAVES))
                enemies.add(Enemy(context, 5200f, 25000f, EnemyRole.BOSS, EraType.ERA_3_ZOMBIE))
                enemies.add(Enemy(context, 18000f, 26000f, EnemyRole.BOSS, EraType.ERA_4_ISLAND))
                enemies.add(Enemy(context, 18500f, 14500f, EnemyRole.BOSS, EraType.ERA_5_CYBER))
                // The Grand Fortress Overlord holding the Clock Hand (North-East Fortress)
                enemies.add(Enemy(context, 28500f, 3200f, EnemyRole.BOSS, EraType.ERA_2_OPEN_WORLD))

                // Gang, Prison, and Secret Lodge Guards
                val guards = listOf(
                    Triple(16300f, 11400f, EnemyRole.GANG_MEMBER), Triple(16650f, 11400f, EnemyRole.GANG_BOSS),
                    Triple(4950f, 24400f, EnemyRole.PRISON_GUARD), Triple(5250f, 24400f, EnemyRole.PRISON_GUARD),
                    Triple(27400f, 27500f, EnemyRole.SECRET_ORDER_MAGE), Triple(27750f, 27500f, EnemyRole.SECRET_ORDER_MAGE),
                    Triple(28200f, 3400f, EnemyRole.HEAVY_SHIELD), Triple(28800f, 3400f, EnemyRole.HEAVY_SHIELD)
                )
                for ((gx, gy, gr) in guards) enemies.add(Enemy(context, gx, gy, gr, era))
            }
            EraType.ERA_2_CAVES -> {
                enemies.add(Enemy(context, 1020f, 520f, EnemyRole.GHOUL_FAST, era))
                enemies.add(Enemy(context, 1240f, 460f, EnemyRole.ASSASSIN, era))
                enemies.add(Enemy(context, 1460f, 360f, EnemyRole.MAGE, era))
                enemies.add(Enemy(context, 1640f, 660f, EnemyRole.HEAVY_SHIELD, era, holdsKey = true))
                enemies.add(Enemy(context, 1940f, 740f, EnemyRole.BOSS, era))
            }
            EraType.ERA_3_ZOMBIE -> {
                enemies.add(Enemy(context, 960f, 460f, EnemyRole.MELEE, era))
                enemies.add(Enemy(context, 1160f, 560f, EnemyRole.GHOUL_FAST, era))
                enemies.add(Enemy(context, 1360f, 460f, EnemyRole.ASSASSIN, era))
                enemies.add(Enemy(context, 1560f, 660f, EnemyRole.MELEE, era, holdsKey = true))
                enemies.add(Enemy(context, 1760f, 560f, EnemyRole.GHOUL_FAST, era))
                enemies.add(Enemy(context, 1980f, 760f, EnemyRole.BOSS, era))
            }
            EraType.ERA_4_ISLAND -> {
                buriedMound = BuriedTreasureMound(1640f, 620f)
                enemies.add(Enemy(context, 1040f, 560f, EnemyRole.ISLAND_TRIBAL, era))
                enemies.add(Enemy(context, 1260f, 500f, EnemyRole.ASSASSIN, era))
                enemies.add(Enemy(context, 1460f, 440f, EnemyRole.ARCHER, era))
                enemies.add(Enemy(context, 1660f, 700f, EnemyRole.SPEARMAN, era))
                enemies.add(Enemy(context, 1960f, 920f, EnemyRole.BOSS, era))
            }
            EraType.ERA_5_CYBER -> {
                powerGenerators.add(PowerGeneratorNode(680f, 760f, 1))
                powerGenerators.add(PowerGeneratorNode(1260f, 340f, 2))
                powerGenerators.add(PowerGeneratorNode(1260f, 1080f, 3))

                enemies.add(Enemy(context, 980f, 400f, EnemyRole.CYBER_SHOOTER, era))
                enemies.add(Enemy(context, 1200f, 960f, EnemyRole.CYBER_SHOOTER, era))
                enemies.add(Enemy(context, 1560f, 400f, EnemyRole.CYBER_SHOOTER, era))
                enemies.add(Enemy(context, 1780f, 920f, EnemyRole.HEAVY_SHIELD, era, holdsKey = true))
                enemies.add(Enemy(context, 1960f, 700f, EnemyRole.BOSS, era))
            }
            EraType.ERA_6_VOID -> {
                enemies.add(Enemy(context, 1180f, 460f, EnemyRole.VOID_CLONE, era))
                enemies.add(Enemy(context, 1450f, 760f, EnemyRole.VOID_CLONE, era))
                enemies.add(Enemy(context, 1740f, 560f, EnemyRole.MAGE, era))
                enemies.add(Enemy(context, 1920f, 720f, EnemyRole.BOSS, era))
            }
        }

        rebuildActiveObstacles()
        camera.update(player.x, player.y)
        if (currentEra == EraType.ERA_2_OPEN_WORLD) chunkSystem.updateAroundPlayer(player.x, player.y)

        if (showIntro) {
            showDialogue(StoryManager.getEraIntroDialogue(era))
        }
    }

    private fun rebuildActiveObstacles() {
        activeObstacles.clear()
        activeObstacles.addAll(staticObstacles)
        for (i in miningRocks.indices) if (!miningRocks[i].isDestroyed) activeObstacles.add(miningRocks[i].hitbox)
        for (i in rareNodes.indices) if (!rareNodes[i].isDestroyed) activeObstacles.add(rareNodes[i].hitbox)
        for (i in woodCrates.indices) if (!woodCrates[i].isDestroyed) activeObstacles.add(woodCrates[i].hitbox)
        for (i in choppableTrees.indices) if (!choppableTrees[i].isDestroyed) activeObstacles.add(choppableTrees[i].hitbox)
        for (i in powerGenerators.indices) if (!powerGenerators[i].isDestroyed) activeObstacles.add(powerGenerators[i].hitbox)
        for (i in barricades.indices) if (!barricades[i].isDestroyed) activeObstacles.add(barricades[i].hitbox)
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
        isSurfaceReady = true
        startLoop()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        if (w > 0 && h > 0) {
            val wf = w.toFloat()
            val hf = h.toFloat()
            camera.viewWidth = wf
            camera.viewHeight = hf

            val scale = (hf / 720f).coerceIn(0.72f, 1.35f)
            joystick.setCenter(155f * scale, hf - 145f * scale)

            attackBtn.radius = 64f * scale
            attackBtn.x = wf - 110f * scale
            attackBtn.y = hf - 135f * scale

            interactBtn.radius = 50f * scale
            interactBtn.x = wf - 245f * scale
            interactBtn.y = hf - 125f * scale

            dashBtn.radius = 50f * scale
            dashBtn.x = wf - 235f * scale
            dashBtn.y = hf - 255f * scale

            slowMoBtn.radius = 48f * scale
            slowMoBtn.x = wf - 110f * scale
            slowMoBtn.y = hf - 285f * scale
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isSurfaceReady = false
        stopLoop()
    }

    fun resume() {
        if (isSurfaceReady) startLoop()
    }

    fun pause() {
        if (state == GameState.PLAYING) {
            SaveManager.saveGame(context, currentEra, player, completedErasMask, prog)
            hasSavedGame = true
            savedEra = currentEra
        }
        stopLoop()
    }

    private fun startLoop() {
        if (isRunning) return
        isRunning = true
        gameThread = Thread(this, "KnightWarGameLoop").apply { start() }
    }

    private fun stopLoop() {
        if (!isRunning) return
        isRunning = false
        var retry = true
        while (retry) {
            try {
                gameThread?.join(500)
                retry = false
            } catch (ignored: InterruptedException) {}
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

    private fun triggerBombExplosion(bx: Float, by: Float, damage: Int) {
        val aoeRadius = 185f
        explosions.add(ExplosionEffect(bx, by, aoeRadius))
        particles.emitBurst(bx, by, 26, Color.parseColor("#FF6D00"), speed = 340f, size = 9f, life = 0.45f)
        SoundEngine.playExplosion()
        triggerScreenShake(0.28f, 11f)
        hitStopTimer = 0.045f
        for (i in enemies.indices) {
            val enemy = enemies[i]
            if (enemy.isDead) continue
            val ex = enemy.x + enemy.width / 2f
            val ey = enemy.y + enemy.height / 2f
            if (distToPoint(bx, by, ex, ey) <= aoeRadius) {
                if (enemy.takeDamage(damage)) {
                    enemy.applyKnockback(bx, by, 420f)
                    floatingTexts.add(FloatingText("-$damage", enemy.x + 20f, enemy.y, Color.parseColor("#FF6D00"), isCrit = true))
                    handleEnemyDeath(enemy)
                } else if (enemy.isShielded) {
                    floatingTexts.add(FloatingText("درع الطاقة مفعل!", enemy.x - 40f, enemy.y, Color.CYAN))
                }
            }
        }
    }

    private fun update(deltaTime: Float) {
        if (state != GameState.PLAYING || isDialogueOpen || isShopOpen || isCraftingOpen || isRpgMenuOpen || isAlchemistOpen) return

        dashBtn.cooldownSeconds = player.dashCooldown.coerceAtLeast(0f)
        dashBtn.cooldownRatio = (player.dashCooldown / 0.85f).coerceIn(0f, 1f)
        slowMoBtn.cooldownSeconds = player.slowMoCooldown.coerceAtLeast(0f)
        slowMoBtn.cooldownRatio = (player.slowMoCooldown / 8.0f).coerceIn(0f, 1f)

        if (hitStopTimer > 0f) {
            hitStopTimer -= deltaTime
            particles.update(deltaTime)
            return
        }

        if (shakeTimer > 0) shakeTimer -= deltaTime

        if (player.lastParryTriggered) {
            player.lastParryTriggered = false
            SoundEngine.playLevelUp()
            triggerScreenShake(0.15f, 6f)
            particles.emitBurst(player.x + player.width / 2, player.y + player.height / 2, 18, Color.parseColor("#FFD700"), speed = 260f)
            floatingTexts.add(FloatingText("صد مثالي! PARRY (2.5x)", player.x - 40f, player.y - 45f, Color.parseColor("#FFD700"), isCrit = true))
        }
        prog.updateBuffs(deltaTime)
        worldEnv.update(deltaTime, currentEra, width.toFloat(), height.toFloat())
        player.update(deltaTime, joystick.deltaX, joystick.deltaY, mapWidth, mapHeight, activeObstacles, prog.getSpeedMultiplier())
        camera.update(player.x, player.y)
        if (currentEra == EraType.ERA_2_OPEN_WORLD) chunkSystem.updateAroundPlayer(player.x, player.y)
        particles.update(deltaTime)

        merchant?.update(deltaTime)
        chronomancer?.update(deltaTime)
        alchemistNpc?.update(deltaTime)
        blacksmithNpc?.update(deltaTime)
        sageNpc?.update(deltaTime)
        for (i in questGivers.indices) questGivers[i].update(deltaTime)
        for (i in hubGates.indices) hubGates[i].update(deltaTime)
        portal.update(deltaTime)
        for (i in farmPlots.indices) farmPlots[i].update(deltaTime)
        val activeGrave = playerGrave
        if (activeGrave != null) {
            val g = activeGrave
            if (!g.update(deltaTime)) {
                playerGrave = null
                floatingTexts.add(FloatingText("اختفى قبرك وضاعت أغراضك!", player.x - 40f, player.y - 40f, Color.RED))
            } else if (g.isPlayerNear(player.x + player.width / 2, player.y + player.height / 2)) {
                player.coins += g.coins
                player.wood += g.wood
                player.stone += g.stone
                player.iron += g.iron
                prog.crystal += g.crystal
                prog.mythril += g.mythril
                g.isCollected = true
                playerGrave = null
                SoundEngine.playCoin()
                floatingTexts.add(FloatingText("استعدت أغراضك من القبر!", player.x - 50f, player.y - 40f, Color.GREEN, isCrit = true))
            }
        }
        if (prog.hunger <= 0f) {
            hungerDamageTimer += deltaTime
            if (hungerDamageTimer >= 2.0f) {
                hungerDamageTimer = 0f
                player.takeDamage(5)
                floatingTexts.add(FloatingText("تتضور جوعاً! كُل طعاماً!", player.x, player.y - 30f, Color.RED))
            }
        }
        buriedMound?.update(deltaTime)
        for (i in extraBuriedMounds.indices) extraBuriedMounds[i].update(deltaTime)

        for (i in props.indices) props[i].update(deltaTime)
        for (i in traps.indices) traps[i].update(deltaTime, player)
        for (i in powerGenerators.indices) powerGenerators[i].update(deltaTime)

        if (currentEra == EraType.ERA_5_CYBER) {
            var generatorsLeft = 0
            for (i in powerGenerators.indices) if (!powerGenerators[i].isDestroyed) generatorsLeft++
            if (generatorsLeft == 0) {
                for (i in enemies.indices) {
                    val b = enemies[i]
                    if (b.isBoss && b.isShielded) {
                        b.isShielded = false
                        triggerScreenShake(0.5f, 15f)
                        floatingTexts.add(FloatingText("تم إسقاط الدرع النووي! القائد السايبورغ مكشوف!", player.x - 100f, player.y - 45f, Color.CYAN))
                    }
                }
            }
        }

        if (currentEra == EraType.ERA_2_OPEN_WORLD) {
            openWorldRespawnTimer += deltaTime
            if (openWorldRespawnTimer >= 11.0f) {
                openWorldRespawnTimer = 0f
                var aliveCount = 0
                for (i in enemies.indices) if (!enemies[i].isDead) aliveCount++
                if (aliveCount < 24) {
                    val sx = (player.x + (if (Random.nextBoolean()) 620f else -620f)).coerceIn(200f, mapWidth - 200f)
                    val sy = (player.y + (if (Random.nextBoolean()) 480f else -480f)).coerceIn(200f, mapHeight - 200f)
                    if (!CollisionSystem.isPointBlocked(sx, sy, activeObstacles, mapWidth, mapHeight)) {
                        val roles = listOf(EnemyRole.SPEARMAN, EnemyRole.ASSASSIN, EnemyRole.ARCHER, EnemyRole.GHOUL_FAST)
                        enemies.add(Enemy(context, sx, sy, roles[Random.nextInt(roles.size)], currentEra))
                    }
                }
            }
        }

        if (currentEra == EraType.ERA_4_ISLAND && !player.hasOceanGem) {
            islandWaveTimer += deltaTime
            if (islandWaveTimer >= 9.0f) {
                islandWaveTimer = 0f
                var activeMinions = 0
                for (i in enemies.indices) if (!enemies[i].isDead && !enemies[i].isBoss) activeMinions++
                if (activeMinions < 8) {
                    val spawnX = (player.x + (if (Random.nextBoolean()) 550f else -550f)).coerceIn(140f, mapWidth - 180f)
                    val spawnY = (player.y + (if (Random.nextBoolean()) 350f else -350f)).coerceIn(140f, mapHeight - 180f)
                    if (!CollisionSystem.isPointBlocked(spawnX, spawnY, activeObstacles, mapWidth, mapHeight)) {
                        val role = if (Random.nextBoolean()) EnemyRole.ISLAND_TRIBAL else EnemyRole.ARCHER
                        enemies.add(Enemy(context, spawnX, spawnY, role, currentEra))
                        floatingTexts.add(FloatingText("تحذير: هجوم قراصنة جديد!", player.x, player.y - 45f, Color.parseColor("#FF5252")))
                    }
                }
            }
        }

        val itCoins = coinDrops.iterator()
        while (itCoins.hasNext()) {
            val c = itCoins.next()
            c.update(deltaTime)
            if (c.isPlayerNear(player.x + player.width / 2, player.y + player.height / 2)) {
                player.coins += c.value
                SoundEngine.playCoin()
                particles.emitBurst(c.x, c.y, 8, Color.parseColor("#FFD700"), speed = 150f, size = 5f, life = 0.3f)
                floatingTexts.add(FloatingText("+${c.value} ذهب", player.x, player.y - 20f, Color.parseColor("#FFD700")))
                itCoins.remove()
            }
        }

        val itFloat = floatingTexts.iterator()
        while (itFloat.hasNext()) {
            if (!itFloat.next().update(deltaTime)) itFloat.remove()
        }

        val itC4 = plantedExplosives.iterator()
        while (itC4.hasNext()) {
            val c4 = itC4.next()
            var proxTrigger = false
            for (i in enemies.indices) {
                val e = enemies[i]
                if (!e.isDead && distToPoint(c4.x, c4.y, e.x + e.width / 2, e.y + e.height / 2) < 75f) {
                    proxTrigger = true
                    break
                }
            }
            if (proxTrigger || c4.update(deltaTime)) {
                c4.isDetonated = true
                triggerBombExplosion(c4.x, c4.y, c4.damage)
                itC4.remove()
            }
        }

        val itExp = explosions.iterator()
        while (itExp.hasNext()) {
            if (!itExp.next().update(deltaTime)) itExp.remove()
        }

        val effectiveDelta = if (player.isSlowMoActive) deltaTime * 0.30f else deltaTime
        val itProj = projectiles.iterator()
        while (itProj.hasNext()) {
            val proj = itProj.next()
            val timedOutBomb = proj.update(effectiveDelta)

            if (timedOutBomb) {
                triggerBombExplosion(proj.x, proj.y, proj.damage)
                itProj.remove()
                continue
            }

            if (CollisionSystem.isPointBlocked(proj.x, proj.y, staticObstacles, mapWidth, mapHeight)) {
                if (proj.isBomb) triggerBombExplosion(proj.x, proj.y, proj.damage)
                else particles.emitBurst(proj.x, proj.y, 6, Color.LTGRAY, speed = 120f, size = 5f, life = 0.25f)
                itProj.remove()
                continue
            }

            if (proj.isEnemy) {
                if (player.hitbox.contains(proj.x, proj.y)) {
                    if (player.takeDamage(proj.damage)) {
                        SoundEngine.playHit()
                        particles.emitBurst(player.x + player.width / 2, player.y + player.height / 2, 10, Color.RED, speed = 200f)
                        triggerScreenShake(0.2f, 8f)
                        floatingTexts.add(FloatingText("-${proj.damage}", player.x + 20f, player.y, Color.parseColor("#E91E63")))
                    }
                    itProj.remove()
                }
            } else {
                var hit = false
                for (i in enemies.indices) {
                    val enemy = enemies[i]
                    if (!enemy.isDead && enemy.hitbox.contains(proj.x, proj.y)) {
                        if (proj.isBomb) {
                            triggerBombExplosion(proj.x, proj.y, proj.damage)
                        } else {
                            if (enemy.takeDamage(proj.damage)) {
                                SoundEngine.playHit()
                                enemy.applyKnockback(proj.x, proj.y, 260f)
                                particles.emitBurst(proj.x, proj.y, 12, Color.CYAN, speed = 240f)
                                triggerScreenShake(0.15f, 6f)
                                floatingTexts.add(FloatingText("-${proj.damage}", enemy.x + 20f, enemy.y, Color.parseColor("#FF5722")))
                                handleEnemyDeath(enemy)
                            } else if (enemy.isShielded) {
                                floatingTexts.add(FloatingText("درع الطاقة مفعل! دمر المولدات!", enemy.x - 80f, enemy.y, Color.CYAN))
                            }
                        }
                        hit = true
                        break
                    }
                }
                if (hit) itProj.remove()
            }
        }

        var bossDead = false
        var activeCount = 0
        val newSummons = mutableListOf<Enemy>()
        for (i in enemies.indices) {
            val enemy = enemies[i]
            enemy.update(deltaTime, player, projectiles, activeObstacles)
            if (enemy.isBoss && enemy.wantsToSummonWave && !enemy.isDead) {
                enemy.wantsToSummonWave = false
                floatingTexts.add(FloatingText("الزعيم استدعى 10 جنود!", enemy.x, enemy.y - 45f, Color.RED, isCrit = true))
                for (s in 0 until 10) {
                    val ang = (s / 10.0) * Math.PI * 2.0
                    val sx = (enemy.x + kotlin.math.cos(ang).toFloat() * 190f).coerceIn(100f, mapWidth - 150f)
                    val sy = (enemy.y + kotlin.math.sin(ang).toFloat() * 190f).coerceIn(100f, mapHeight - 150f)
                    val r = if (s % 3 == 0) EnemyRole.ARCHER else if (s % 2 == 0) EnemyRole.SPEARMAN else EnemyRole.GANG_MEMBER
                    newSummons.add(Enemy(context, sx, sy, r, enemy.era))
                }
            }
            if (enemy.isDead) {
                if (enemy.isBoss) bossDead = true
            } else {
                activeCount++
            }
        }
        if (newSummons.isNotEmpty()) enemies.addAll(newSummons)

        if (currentEra != EraType.ERA_0_HUB && !portal.isActive) {
            val unlockPortal = when (currentEra) {
                EraType.ERA_2_OPEN_WORLD -> bossDead && prog.hasClockHand
                EraType.ERA_6_VOID, EraType.ERA_5_CYBER -> bossDead
                EraType.ERA_4_ISLAND -> player.hasOceanGem && bossDead
                else -> activeCount == 0
            }
            if (unlockPortal) portal.isActive = true
        }

        if (player.isAttacking) {
            attackHitbox.set(
                if (player.facingLeft) player.x - 50f else player.x + player.width - 10f,
                player.y + 8f,
                if (player.facingLeft) player.x + 10f else player.x + player.width + 50f,
                player.y + player.height - 8f
            )
            val swingId = player.attackSwingId
            val baseDmg = (player.equippedWeapon.damage * prog.getDamageMultiplier()).toInt()
            val pxCenter = player.x + player.width / 2f
            val pyCenter = player.y + player.height / 2f

            for (i in enemies.indices) {
                val enemy = enemies[i]
                val hitCheck = if (player.isWhirlwindActive) {
                    distToPoint(pxCenter, pyCenter, enemy.x + enemy.width / 2f, enemy.y + enemy.height / 2f) < 155f
                } else {
                    RectF.intersects(attackHitbox, enemy.hitbox)
                }
                if (!enemy.isDead && enemy.lastHitSwingId != swingId && hitCheck) {
                    enemy.lastHitSwingId = swingId
                    val isCrit = Random.nextFloat() < prog.getCritChance()
                    val comboMult = if (player.isWhirlwindActive) 1.5f else 1.0f
                    val counterMult = if (player.counterBuffTimer > 0f) { player.counterBuffTimer = 0f; 2.5f } else 1.0f
                    val dmg = ((if (isCrit) baseDmg * 1.5f else baseDmg.toFloat()) * comboMult * counterMult).toInt()
                    if (enemy.takeDamage(dmg)) {
                        enemy.applyKnockback(pxCenter, pyCenter, if (isCrit) 440f else 310f)
                        val ex = enemy.x + enemy.width / 2f
                        val ey = enemy.y + enemy.height / 2f
                        if (isCrit) {
                            SoundEngine.playCrit()
                            hitStopTimer = 0.052f
                            triggerScreenShake(0.18f, 8f)
                            particles.emitBurst(ex, ey, 18, Color.parseColor("#FFD700"), speed = 280f, size = 8f)
                            floatingTexts.add(FloatingText("حرج! -$dmg", enemy.x + 10f, enemy.y - 10f, Color.parseColor("#FFD700"), isCrit = true))
                        } else {
                            SoundEngine.playHit()
                            hitStopTimer = 0.032f
                            triggerScreenShake(0.11f, 5f)
                            particles.emitBurst(ex, ey, 11, Color.parseColor("#FF5252"), speed = 210f, size = 6f)
                            floatingTexts.add(FloatingText("-$dmg", enemy.x + 20f, enemy.y, Color.parseColor("#FF1744")))
                        }
                        handleEnemyDeath(enemy)
                    } else if (enemy.isShielded) {
                        floatingTexts.add(FloatingText("الدرع النووي مفعل! دمر المولدات!", enemy.x - 70f, enemy.y, Color.CYAN))
                    }
                }
            }

            var obstacleDestroyed = false
            for (i in powerGenerators.indices) {
                val gen = powerGenerators[i]
                if (!gen.isDestroyed && RectF.intersects(attackHitbox, gen.hitbox) && gen.hit(swingId)) {
                    SoundEngine.playHit()
                    particles.emitBurst(gen.x + gen.width / 2, gen.y + gen.height / 2, 12, Color.CYAN, speed = 220f)
                    triggerScreenShake(0.2f, 8f)
                    floatingTexts.add(FloatingText("-1 طاقة المولد!", gen.x, gen.y - 20f, Color.CYAN))
                    if (gen.isDestroyed) {
                        obstacleDestroyed = true
                        SoundEngine.playExplosion()
                        triggerScreenShake(0.4f, 12f)
                        coinDrops.add(CoinDrop(gen.x + gen.width / 2, gen.y + gen.height / 2, 40))
                        floatingTexts.add(FloatingText("تم تدمير المولد ${gen.index}!", gen.x - 40f, gen.y - 30f, Color.YELLOW))
                    }
                }
            }

            for (i in miningRocks.indices) {
                val rock = miningRocks[i]
                if (!rock.isDestroyed && RectF.intersects(attackHitbox, rock.hitbox)) {
                    if (!player.hasPickaxe) {
                        floatingTexts.add(FloatingText("تحتاج لفأس التعدين لكسر الحجر!", rock.x - 40f, rock.y - 20f, Color.YELLOW))
                        continue
                    }
                    if (rock.hit(swingId)) {
                        SoundEngine.playHit()
                        particles.emitBurst(rock.x + 38f, rock.y + 38f, 8, Color.LTGRAY, speed = 160f)
                        triggerScreenShake(0.1f, 4f)
                        if (rock.isDestroyed) obstacleDestroyed = true
                        if (rock.isIron) {
                            player.iron += 1
                            floatingTexts.add(FloatingText("+1 خام حديد", rock.x, rock.y - 20f, Color.parseColor("#FFD54F")))
                        } else {
                            player.stone += 1
                            floatingTexts.add(FloatingText("+1 حجر", rock.x, rock.y - 20f, Color.parseColor("#B0BEC5")))
                        }
                        onResourceGathered()
                    }
                }
            }

            for (i in rareNodes.indices) {
                val rnode = rareNodes[i]
                if (!rnode.isDestroyed && RectF.intersects(attackHitbox, rnode.hitbox)) {
                    if (!player.hasPickaxe) {
                        floatingTexts.add(FloatingText("اصنع فأس التعدين من الطاولة أولاً!", rnode.x - 40f, rnode.y - 20f, Color.YELLOW))
                        continue
                    }
                    if (rnode.hit(swingId)) {
                        SoundEngine.playHit()
                        if (rnode.isDestroyed) obstacleDestroyed = true
                        when (rnode.oreType) {
                            3 -> { prog.mythril += 1; floatingTexts.add(FloatingText("+1 ميثريل نادر!", rnode.x, rnode.y - 20f, Color.parseColor("#E040FB"))) }
                            2 -> { prog.crystal += 1; floatingTexts.add(FloatingText("+1 كريستال!", rnode.x, rnode.y - 20f, Color.CYAN)) }
                            1 -> { player.iron += 2; floatingTexts.add(FloatingText("+2 حديد!", rnode.x, rnode.y - 20f, Color.parseColor("#FFD54F"))) }
                            else -> { player.stone += 2; floatingTexts.add(FloatingText("+2 حجر!", rnode.x, rnode.y - 20f, Color.LTGRAY)) }
                        }
                        onResourceGathered()
                    }
                }
            }

            for (i in woodCrates.indices) {
                val crate = woodCrates[i]
                if (!crate.isDestroyed && RectF.intersects(attackHitbox, crate.hitbox) && crate.hit(swingId)) {
                    SoundEngine.playHit()
                    particles.emitBurst(crate.x + 32f, crate.y + 32f, 8, Color.parseColor("#8D6E63"), speed = 160f)
                    triggerScreenShake(0.1f, 4f)
                    if (crate.isDestroyed) obstacleDestroyed = true
                    player.wood += 2
                    floatingTexts.add(FloatingText("+2 خشب", crate.x, crate.y - 20f, Color.parseColor("#8D6E63")))
                    onResourceGathered()
                }
            }

            for (i in choppableTrees.indices) {
                val tree = choppableTrees[i]
                if (!tree.isDestroyed && RectF.intersects(attackHitbox, tree.hitbox) && tree.hit(swingId)) {
                    SoundEngine.playHit()
                    particles.emitBurst(tree.x + 40f, tree.y + 50f, 8, Color.parseColor("#66BB6A"), speed = 150f)
                    triggerScreenShake(0.08f, 3f)
                    player.wood += 1
                    floatingTexts.add(FloatingText("قطع خشب +1", tree.x, tree.y - 15f, Color.parseColor("#8D6E63")))
                    if (tree.isDestroyed) {
                        obstacleDestroyed = true
                        player.wood += 2
                        floatingTexts.add(FloatingText("+2 خشب إضافي!", tree.x, tree.y - 30f, Color.parseColor("#A1887F")))
                    }
                    onResourceGathered()
                }
            }

            if (obstacleDestroyed) rebuildActiveObstacles()
        }

        // Dynamic Interact button
        val px = player.x + player.width / 2
        val py = player.y + player.height / 2
        val distToTable = distToPoint(px, py, craftTableX + 40f, craftTableY + 40f)
        val distToGate = distToPoint(px, py, gateX, gateY)
        val nearMound = (buriedMound != null && buriedMound!!.isPlayerNear(px, py) && !buriedMound!!.isDug) || extraBuriedMounds.any { !it.isDug && it.isPlayerNear(px, py) }
        val nearHubGate = hubGates.firstOrNull { it.isPlayerNear(px, py) }
        val nearChrono = chronomancer != null && chronomancer!!.isPlayerNear(px, py)
        val nearAlchemist = alchemistNpc != null && alchemistNpc!!.isPlayerNear(px, py)
        val nearBlacksmith = blacksmithNpc != null && blacksmithNpc!!.isPlayerNear(px, py)
        val nearSage = sageNpc != null && sageNpc!!.isPlayerNear(px, py)
        val nearQuestGiver = questGivers.firstOrNull { it.isPlayerNear(px, py) }
        val nearVault = lootVaults.firstOrNull { !it.isLooted && it.isPlayerNear(px, py) }
        val nearFarm = farmPlots.firstOrNull { it.isPlayerNear(px, py) }
        val nearGen = if (currentEra == EraType.ERA_5_CYBER) {
            powerGenerators.firstOrNull { !it.isDestroyed && distToPoint(px, py, it.x + it.width / 2, it.y + it.height / 2) < 115f }
        } else null
        var nearChest = false
        for (i in chests.indices) {
            if (chests[i].isPlayerNear(px, py) && !chests[i].isOpened) {
                nearChest = true
                break
            }
        }

        when {
            nearHubGate != null -> {
                interactBtn.label = "دخول"
                interactBtn.colorPrimary = Color.argb(220, 0, 176, 255)
            }
            nearChrono || nearSage || nearQuestGiver != null -> {
                interactBtn.label = "حوار"
                interactBtn.colorPrimary = Color.argb(220, 0, 229, 255)
            }
            nearFarm != null -> {
                interactBtn.label = when (nearFarm.stage) { 0 -> "زراعة"; 2 -> "حصاد"; else -> "ينمو" }
                interactBtn.colorPrimary = Color.argb(220, 67, 160, 71)
            }
            nearVault != null -> {
                interactBtn.label = "سرقة"
                interactBtn.colorPrimary = Color.argb(220, 255, 215, 0)
            }
            nearAlchemist -> {
                interactBtn.label = "كيمياء"
                interactBtn.colorPrimary = Color.argb(220, 224, 64, 251)
            }
            nearBlacksmith -> {
                interactBtn.label = "حداد"
                interactBtn.colorPrimary = Color.argb(220, 255, 138, 101)
            }
            nearGen != null -> {
                interactBtn.label = "اختراق"
                interactBtn.colorPrimary = Color.argb(220, 0, 229, 255)
            }
            distToTable < 110f -> {
                interactBtn.label = "صناعة"
                interactBtn.colorPrimary = Color.argb(220, 123, 31, 162)
            }
            nearMound -> {
                interactBtn.label = if (player.hasPickaxe) "حفر" else "فأس!"
                interactBtn.colorPrimary = Color.argb(220, 255, 152, 0)
            }
            merchant != null && merchant!!.isPlayerNear(px, py) -> {
                interactBtn.label = "المتجر"
                interactBtn.colorPrimary = Color.argb(220, 255, 152, 0)
            }
            distToGate < 130f && !isGateUnlocked -> {
                interactBtn.label = if (player.hasBossKey) "فتح" else "مقفل"
                interactBtn.colorPrimary = Color.argb(220, 211, 47, 47)
            }
            nearChest -> {
                interactBtn.label = "فتح"
                interactBtn.colorPrimary = Color.argb(220, 251, 192, 45)
            }
            portal.isActive && portal.isPlayerInside(px, py) -> {
                interactBtn.label = "عبور"
                interactBtn.colorPrimary = Color.argb(220, 0, 229, 255)
            }
            else -> {
                interactBtn.label = "تفاعل"
                interactBtn.colorPrimary = Color.argb(220, 30, 136, 229)
            }
        }

        if (player.hp <= 0 || player.isDead) {
            if (currentEra == EraType.ERA_2_OPEN_WORLD) {
                // Drop 60-second Grave with dropped resources if past Sage free rescues
                if (prog.sageRescueCount >= 2) {
                    playerGrave = PlayerGrave(
                        player.x + player.width / 2f, player.y + player.height / 2f, 60f,
                        player.coins / 2, player.wood / 2, player.stone / 2, player.iron / 2, prog.crystal / 2, prog.mythril / 2
                    )
                    player.coins -= player.coins / 2
                    player.wood -= player.wood / 2
                    player.stone -= player.stone / 2
                    player.iron -= player.iron / 2
                    prog.crystal -= prog.crystal / 2
                    prog.mythril -= prog.mythril / 2
                    floatingTexts.add(FloatingText("سقطت! لديك 60 ثانية لاستعادة قبرك!", 950f, 900f, Color.RED, isCrit = true))
                } else {
                    prog.sageRescueCount++
                    showDialogue(StoryManager.getSageRescueDialogue(prog.sageRescueCount))
                }
                player.isDead = false
                player.hp = player.maxHp
                player.mana = player.maxMana
                prog.hunger = 80f
                player.x = 950f
                player.y = 950f
                player.invincibilityTimer = 2.5f
                camera.update(player.x, player.y)
                SaveManager.saveGame(context, currentEra, player, completedErasMask, prog)
            } else {
                state = GameState.GAME_OVER
            }
        }
    }

    private fun onResourceGathered() {
        prog.advanceQuest("q_miner", 1)?.let { q ->
            player.coins += q.rewardGold
            grantXp(q.rewardXp)
            floatingTexts.add(FloatingText("مهمة مكتملة: ${q.titleAr}!", player.x - 50f, player.y - 45f, Color.parseColor("#81C784")))
        }
    }

    private fun handleEnemyDeath(enemy: Enemy) {
        if (!enemy.isDead) return
        particles.emitBurst(enemy.x + enemy.width / 2, enemy.y + enemy.height / 2, 18, Color.parseColor("#B71C1C"), speed = 250f, size = 7f, life = 0.45f)
        val goldAmount = if (enemy.isBoss) 120 else 22
        coinDrops.add(CoinDrop(enemy.x + enemy.width / 2, enemy.y + enemy.height / 2, goldAmount))

        grantXp(if (enemy.isBoss) 150 else 28)
        prog.unlockAchievement("ach_first_kill")?.let {
            floatingTexts.add(FloatingText("إنجاز: ${it.titleAr}!", player.x - 40f, player.y - 70f, Color.YELLOW))
        }
        prog.advanceQuest("q_first_blood", 1)?.let { q ->
            player.coins += q.rewardGold
            grantXp(q.rewardXp)
            floatingTexts.add(FloatingText("مهمة مكتملة: ${q.titleAr}!", player.x - 50f, player.y - 45f, Color.parseColor("#81C784")))
        }

        if (enemy.holdsKey && currentEra != EraType.ERA_4_ISLAND) {
            player.hasBossKey = true
            floatingTexts.add(FloatingText("عثرت على مفتاح قاعة الزعيم!", player.x - 40f, player.y - 40f, Color.YELLOW))
        }

        if (currentEra == EraType.ERA_2_OPEN_WORLD && !enemy.isBoss) {
            prog.advanceQuest("q_open_soldiers", 1)?.let { q ->
                player.coins += q.rewardGold
                grantXp(q.rewardXp)
                floatingTexts.add(FloatingText("مهمة مكتملة: ${q.titleAr}!", player.x - 50f, player.y - 45f, Color.parseColor("#81C784")))
            }
        }
        if (enemy.isBoss) {
            if (currentEra == EraType.ERA_2_OPEN_WORLD) {
                prog.hasClockHand = true
                prog.advanceQuest("q_open_clockhand", 1)
                floatingTexts.add(FloatingText("حصلت على عقرب الساعة!", player.x - 50f, player.y - 75f, Color.parseColor("#FFD700"), isCrit = true))
            }
            hitStopTimer = 0.08f
            triggerScreenShake(0.5f, 15f)
            showDialogue(StoryManager.getEraClearDialogue(currentEra))
            SaveManager.saveGame(context, currentEra, player, completedErasMask, prog)
        }
    }

    private fun handleInteract() {
        val px = player.x + player.width / 2
        val py = player.y + player.height / 2

        for (i in hubGates.indices) {
            val gate = hubGates[i]
            if (gate.isPlayerNear(px, py)) {
                SoundEngine.playSlowMo()
                setupEra(gate.targetEra, showIntro = true)
                SaveManager.saveGame(context, gate.targetEra, player, completedErasMask, prog)
                return
            }
        }

        if (chronomancer != null && chronomancer!!.isPlayerNear(px, py)) {
            showDialogue(StoryManager.getHubIntroDialogue())
            return
        }
        if (alchemistNpc != null && alchemistNpc!!.isPlayerNear(px, py)) {
            isAlchemistOpen = true
            return
        }
        if (blacksmithNpc != null && blacksmithNpc!!.isPlayerNear(px, py)) {
            dialogs.activeTab = RpgTab.INVENTORY
            isRpgMenuOpen = true
            return
        }
        if (sageNpc != null && sageNpc!!.isPlayerNear(px, py)) {
            showDialogue(StoryManager.getEraIntroDialogue(EraType.ERA_2_OPEN_WORLD))
            return
        }
        for (fp in farmPlots) {
            if (fp.isPlayerNear(px, py)) {
                when (fp.stage) {
                    0 -> if (prog.seeds > 0) {
                        prog.seeds--
                        fp.stage = 1
                        fp.growthTimer = 22f
                        SoundEngine.playUiClick()
                        floatingTexts.add(FloatingText("زرعت بذرة! انتظر 22 ثانية", fp.x, fp.y - 20f, Color.GREEN))
                    } else {
                        floatingTexts.add(FloatingText("لا توجد بذور! اشترِ من التاجر", fp.x, fp.y - 20f, Color.YELLOW))
                    }
                    2 -> {
                        fp.stage = 0
                        prog.food += 2
                        prog.seeds += 2
                        grantXp(25)
                        SoundEngine.playCoin()
                        prog.advanceQuest("q_farmer", 1)?.let { q -> player.coins += q.rewardGold; grantXp(q.rewardXp) }
                        floatingTexts.add(FloatingText("+2 طعام +2 بذور!", fp.x, fp.y - 20f, Color.parseColor("#FFD700")))
                    }
                    else -> floatingTexts.add(FloatingText("المحصول لا يزال ينمو...", fp.x, fp.y - 20f, Color.LTGRAY))
                }
                return
            }
        }
        for (v in lootVaults) {
            if (!v.isLooted && v.isPlayerNear(px, py)) {
                v.isLooted = true
                player.coins += v.goldAmount
                grantXp(90)
                SoundEngine.playCoin()
                particles.emitBurst(v.x + 36f, v.y + 32f, 20, Color.parseColor("#FFD700"), speed = 240f)
                floatingTexts.add(FloatingText("سرقت ${v.goldAmount} ذهب!", px - 50f, py - 40f, Color.parseColor("#FFD700"), isCrit = true))
                if (!player.isInvisible) {
                    floatingTexts.add(FloatingText("رآك الحراس! اهرب!", px - 40f, py - 70f, Color.RED))
                    for (e in enemies) {
                        if (!e.isDead && distToPoint(px, py, e.x, e.y) < 1400f) e.alertedTimer = 14f
                    }
                }
                SaveManager.saveGame(context, currentEra, player, completedErasMask, prog)
                return
            }
        }
        for (qg in questGivers) {
            if (qg.isPlayerNear(px, py)) {
                val q = prog.quests.firstOrNull { it.id == qg.questId }
                val msg = if (q != null && q.isCompleted) "أحسنت في إتمام المهمة!" else "مهمة: ${q?.descAr ?: "استكشف العالم واهزم الوحوش!"}"
                showDialogue(listOf(DialogueLine(qg.npcName, msg, "Quest updated in your Quest Log!")))
                return
            }
        }

        // Era 5 Hacking option for Power Generators
        if (currentEra == EraType.ERA_5_CYBER) {
            for (i in powerGenerators.indices) {
                val gen = powerGenerators[i]
                if (!gen.isDestroyed && distToPoint(px, py, gen.x + gen.width / 2, gen.y + gen.height / 2) < 115f) {
                    gen.isDestroyed = true
                    gen.hp = 0
                    rebuildActiveObstacles()
                    SoundEngine.playHack()
                    player.coins += 60
                    grantXp(45)
                    prog.unlockAchievement("ach_hacker")
                    particles.emitBurst(gen.x + gen.width / 2, gen.y + gen.height / 2, 16, Color.CYAN, speed = 220f)
                    floatingTexts.add(FloatingText("تم اختراق المولد ${gen.index}! (+60 ذهب)", gen.x - 40f, gen.y - 30f, Color.CYAN))
                    return
                }
            }
        }

        if (distToPoint(px, py, craftTableX + 40f, craftTableY + 40f) < 110f) {
            isCraftingOpen = true
            return
        }

        for (em in extraBuriedMounds) {
            if (!em.isDug && em.isPlayerNear(px, py)) {
                if (player.hasPickaxe) {
                    em.isDug = true
                    player.coins += 180
                    prog.crystal += 2
                    prog.mythril += 1
                    grantXp(80)
                    SoundEngine.playCoin()
                    floatingTexts.add(FloatingText("كنز مدفون! +180 ذهب +2 كريستال +1 ميثريل", px - 90f, py - 40f, Color.parseColor("#FFD700"), isCrit = true))
                } else {
                    floatingTexts.add(FloatingText("تحتاج لفأس التعدين لحفر الكنز!", px - 80f, py - 40f, Color.YELLOW))
                }
                return
            }
        }
        if (buriedMound != null && buriedMound!!.isPlayerNear(px, py) && !buriedMound!!.isDug) {
            if (player.hasPickaxe) {
                buriedMound!!.isDug = true
                player.hasBossKey = true
                SoundEngine.playCoin()
                triggerScreenShake(0.2f, 8f)
                floatingTexts.add(FloatingText("حفرت الرمال واستخرجت المفتاح الذهبي!", px - 80f, py - 40f, Color.CYAN))
            } else {
                floatingTexts.add(FloatingText("أنت بحاجة إلى فأس التنقيب!", px - 90f, py - 40f, Color.YELLOW))
            }
            return
        }

        if (merchant != null && merchant!!.isPlayerNear(px, py)) {
            isShopOpen = true
            return
        }

        if (distToPoint(px, py, gateX, gateY) < 130f && !isGateUnlocked) {
            if (player.hasBossKey) {
                isGateUnlocked = true
                SoundEngine.playCoin()
                floatingTexts.add(FloatingText("تم فتح بوابة قاعة الزعيم!", px - 60f, py - 40f, Color.CYAN))
            } else {
                floatingTexts.add(FloatingText("البوابة مقفلة بالمفتاح!", px - 80f, py - 40f, Color.RED))
            }
            return
        }

        for (i in chests.indices) {
            val ch = chests[i]
            if (ch.isPlayerNear(px, py) && !ch.isOpened) {
                val gold = ch.open()
                player.coins += gold
                player.stone += 2
                player.wood += 2
                SoundEngine.playCoin()
                particles.emitBurst(ch.x + 32f, ch.y + 32f, 14, Color.parseColor("#FFD700"), speed = 200f)
                if (currentEra == EraType.ERA_4_ISLAND && !player.hasOceanGem) {
                    player.hasOceanGem = true
                    triggerScreenShake(0.4f, 12f)
                    floatingTexts.add(FloatingText("استعدت جوهرة المحيط! اقضِ على القبطان!", px - 100f, py - 40f, Color.parseColor("#00E5FF")))
                } else {
                    floatingTexts.add(FloatingText("+$gold ذهب وموارد نادرة!", px - 40f, py - 40f, Color.parseColor("#FFD700")))
                }
                SaveManager.saveGame(context, currentEra, player, completedErasMask, prog)
                return
            }
        }

        if (portal.isActive && portal.isPlayerInside(px, py)) {
            advanceEra()
        }
    }

    private fun distToPoint(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2))
    }

    private fun advanceEra() {
        if (currentEra != EraType.ERA_0_HUB) {
            completedErasMask = completedErasMask or (1 shl currentEra.ordinal)
            for (q in prog.onEraCompleted(currentEra)) {
                player.coins += q.rewardGold
                grantXp(q.rewardXp)
            }
        }
        val allSixBitmask = (1 shl EraType.ERA_1_MEDIEVAL.ordinal) or
            (1 shl EraType.ERA_2_CAVES.ordinal) or
            (1 shl EraType.ERA_3_ZOMBIE.ordinal) or
            (1 shl EraType.ERA_4_ISLAND.ordinal) or
            (1 shl EraType.ERA_5_CYBER.ordinal) or
            (1 shl EraType.ERA_6_VOID.ordinal)

        if ((completedErasMask and allSixBitmask) == allSixBitmask) {
            state = GameState.ENDING_CHOICE
        } else if (currentEra == EraType.ERA_1_MEDIEVAL && (completedErasMask and (1 shl EraType.ERA_2_OPEN_WORLD.ordinal)) == 0) {
            setupEra(EraType.ERA_2_OPEN_WORLD, showIntro = true)
            SaveManager.saveGame(context, EraType.ERA_2_OPEN_WORLD, player, completedErasMask, prog)
        } else {
            setupEra(EraType.ERA_0_HUB, showIntro = false)
            SaveManager.saveGame(context, EraType.ERA_0_HUB, player, completedErasMask, prog)
            floatingTexts.add(FloatingText("تمت استعادة الشظية! عدت إلى مدينة الزمن", player.x - 90f, player.y - 40f, Color.parseColor("#FFD700")))
        }
    }

    private fun render(canvas: Canvas) {
        val screenW = width.toFloat().coerceAtLeast(1f)
        val screenH = height.toFloat().coerceAtLeast(1f)
        when (state) {
            GameState.TITLE -> dialogs.drawTitleScreen(canvas, screenW, screenH, hasSavedGame, savedEra)
            GameState.PLAYING -> drawGameWorld(canvas, screenW, screenH)
            GameState.GAME_OVER -> dialogs.drawGameOverScreen(canvas, screenW, screenH)
            GameState.ENDING_CHOICE -> dialogs.drawEndingChoiceScreen(canvas, screenW, screenH)
            GameState.VICTORY -> dialogs.drawVictoryScreen(canvas, screenW, screenH, selectedEnding)
        }
    }

    private fun drawGameWorld(canvas: Canvas, screenW: Float, screenH: Float) {
        canvas.drawColor(currentEra.bgColor)

        canvas.save()
        var offsetX = -camera.x
        var offsetY = -camera.y
        if (shakeTimer > 0f) {
            offsetX += (Random.nextFloat() * 2f - 1f) * shakeMagnitude
            offsetY += (Random.nextFloat() * 2f - 1f) * shakeMagnitude
        }
        canvas.translate(offsetX, offsetY)

        if (currentEra == EraType.ERA_2_OPEN_WORLD) {
            chunkSystem.drawVisibleChunks(canvas, camera.x, camera.y, screenW, screenH)
        } else {
            currentBgBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        }

        for (i in traps.indices) traps[i].draw(canvas)
        for (i in barricades.indices) barricades[i].draw(canvas)
        for (i in lootVaults.indices) lootVaults[i].draw(canvas)
        for (i in plantedExplosives.indices) plantedExplosives[i].draw(canvas)

        canvas.drawRoundRect(craftTableRect, 12f, 12f, craftTablePaint)
        canvas.drawText("طاولة الصناعة", craftTableX + craftTableW / 2, craftTableY - 10f, craftLabelPaint)

        buriedMound?.draw(canvas)
        for (i in extraBuriedMounds.indices) extraBuriedMounds[i].draw(canvas)
        for (i in powerGenerators.indices) powerGenerators[i].draw(canvas)

        if (currentEra != EraType.ERA_0_HUB) darkTotemRgba?.let { canvas.drawBitmap(it, null, totemRect, null) }

        for (i in props.indices) props[i].draw(canvas)
        for (i in choppableTrees.indices) choppableTrees[i].draw(canvas)
        for (i in farmPlots.indices) farmPlots[i].draw(canvas)
        for (i in miningRocks.indices) miningRocks[i].draw(canvas)
        for (i in rareNodes.indices) rareNodes[i].draw(canvas)
        playerGrave?.draw(canvas)
        for (i in woodCrates.indices) woodCrates[i].draw(canvas)
        for (i in chests.indices) chests[i].draw(canvas)

        merchant?.draw(canvas)
        chronomancer?.draw(canvas)
        alchemistNpc?.draw(canvas)
        blacksmithNpc?.draw(canvas)
        sageNpc?.draw(canvas)
        for (i in questGivers.indices) questGivers[i].draw(canvas)
        for (i in hubGates.indices) hubGates[i].draw(canvas)
        if (currentEra != EraType.ERA_0_HUB) portal.draw(canvas)

        for (i in projectiles.indices) projectiles[i].draw(canvas)
        for (i in explosions.indices) explosions[i].draw(canvas)
        for (i in coinDrops.indices) coinDrops[i].draw(canvas)
        for (i in enemies.indices) enemies[i].draw(canvas)
        player.draw(canvas)
        particles.draw(canvas)
        for (i in floatingTexts.indices) floatingTexts[i].draw(canvas)

        canvas.restore()

        // Atmosphere: Rain, Day/Night, Cave Dynamic Torchlight
        val pScreenX = (player.x + player.width / 2f) - camera.x
        val pScreenY = (player.y + player.height / 2f) - camera.y
        worldEnv.drawAtmosphereOverlay(canvas, currentEra, screenW, screenH, pScreenX, pScreenY)

        var activeEnemiesCount = 0
        for (i in enemies.indices) if (!enemies[i].isDead) activeEnemiesCount++
        var genLeft = 0
        for (i in powerGenerators.indices) if (!powerGenerators[i].isDestroyed) genLeft++
        val completedCount = Integer.bitCount(completedErasMask)

        val zoneName = if (currentEra == EraType.ERA_2_OPEN_WORLD) chunkSystem.getZoneName(player.x, player.y) else ""
        val compassHint = if (currentEra == EraType.ERA_2_OPEN_WORLD) {
            val g = playerGrave
            val targetX = when { g != null -> g.x; prog.hasClockHand -> 950f; else -> 28500f }
            val targetY = when { g != null -> g.y; prog.hasClockHand -> 700f; else -> 3200f }
            val dx = targetX - player.x
            val dy = targetY - player.y
            val distM = (sqrt(dx * dx + dy * dy) / 32f).toInt()
            val dir = (if (dy < -200f) "شمال " else if (dy > 200f) "جنوب " else "") + (if (dx > 200f) "شرق" else if (dx < -200f) "غرب" else "")
            val goalLabel = when { playerGrave != null -> "قبرك (${playerGrave!!.timeLeft.toInt()}s)"; prog.hasClockHand -> "البوابة"; else -> "الحصن" }
            "$goalLabel: $dir (${distM}m)"
        } else ""
        hud.draw(canvas, screenW, screenH, player, prog, currentEra, iconCache, activeEnemiesCount, portal.isActive, genLeft, completedCount, prog.hasClockHand, zoneName, compassHint)
        worldEnv.drawMinimap(canvas, screenW, currentEra, player, enemies, merchant, hubGates, portal.x, portal.y, portal.isActive)

        if (!isDialogueOpen && !isShopOpen && !isCraftingOpen && !isRpgMenuOpen && !isAlchemistOpen) {
            joystick.draw(canvas)
            attackBtn.draw(canvas)
            dashBtn.draw(canvas)
            slowMoBtn.draw(canvas)
            interactBtn.draw(canvas)
        }

        if (isDialogueOpen) dialogs.drawDialogue(canvas, screenW, screenH, currentDialogue)
        if (isShopOpen) dialogs.drawShop(canvas, screenW, screenH, player, frameShopBmp, iconCache)
        if (isCraftingOpen) dialogs.drawCrafting(canvas, screenW, screenH, player, prog, frameInventoryBmp, iconCache)
        if (isRpgMenuOpen) dialogs.drawRpgMenu(canvas, screenW, screenH, player, prog, worldEnv, iconCache)
        if (isAlchemistOpen) dialogs.drawAlchemistShop(canvas, screenW, screenH, player, prog, iconCache)
    }

    private fun handleRpgMenuTouch(tx: Float, ty: Float) {
        if (dialogs.closeSheetRect.contains(tx, ty)) {
            SoundEngine.playUiClick()
            isRpgMenuOpen = false
            return
        }
        for ((rect, tab) in dialogs.tabRects) {
            if (rect.contains(tx, ty)) {
                SoundEngine.playUiClick()
                dialogs.activeTab = tab
                return
            }
        }
        when (dialogs.activeTab) {
            RpgTab.SKILLS -> {
                for ((rect, skillId) in dialogs.skillBtnRects) {
                    if (rect.contains(tx, ty)) {
                        if (prog.upgradeSkill(skillId)) {
                            SoundEngine.playLevelUp()
                            player.maxMana = 100f + prog.getBonusMaxMana()
                            player.recalculateMaxHp(prog.getBonusMaxHp())
                            SaveManager.saveGame(context, currentEra, player, completedErasMask, prog)
                        }
                        return
                    }
                }
            }
            RpgTab.INVENTORY -> {
                if (dialogs.upgradeWeaponRect.contains(tx, ty)) {
                    val costGold = 45 + prog.weaponUpgradeLevel * 30
                    val costIron = 2 + prog.weaponUpgradeLevel
                    if (prog.weaponUpgradeLevel < 5 && player.coins >= costGold && player.iron >= costIron) {
                        player.coins -= costGold
                        player.iron -= costIron
                        prog.weaponUpgradeLevel++
                        SoundEngine.playLevelUp()
                        floatingTexts.add(FloatingText("تم تطوير السلاح إلى +${prog.weaponUpgradeLevel}!", player.x - 40f, player.y - 30f, Color.CYAN))
                        SaveManager.saveGame(context, currentEra, player, completedErasMask, prog)
                    }
                    return
                }
                for ((rect, pot) in dialogs.potionUseRects) {
                    if (rect.contains(tx, ty)) {
                        when (pot) {
                            PotionType.SPEED -> if (prog.speedPotions > 0) {
                                prog.speedPotions--
                                prog.speedBuffTimer = 12f
                                SoundEngine.playPotion()
                                floatingTexts.add(FloatingText("سرعة +40% مفعلة!", player.x, player.y - 30f, Color.CYAN))
                            }
                            PotionType.STRENGTH -> if (prog.strengthPotions > 0) {
                                prog.strengthPotions--
                                prog.strengthBuffTimer = 12f
                                SoundEngine.playPotion()
                                floatingTexts.add(FloatingText("قوة +40% مفعلة!", player.x, player.y - 30f, Color.RED))
                            }
                            PotionType.MANA -> if (prog.manaPotions > 0) {
                                prog.manaPotions--
                                player.mana = player.maxMana
                                SoundEngine.playPotion()
                                floatingTexts.add(FloatingText("امتلأت طاقة السحر!", player.x, player.y - 30f, Color.BLUE))
                            }
                            else -> {}
                        }
                        return
                    }
                }
            }
            RpgTab.SETTINGS -> {
                if (dialogs.toggleMuteRect.contains(tx, ty)) {
                    SoundEngine.isMuted = !SoundEngine.isMuted
                    SoundEngine.playUiClick()
                    return
                }
                if (dialogs.toggleMinimapRect.contains(tx, ty)) {
                    worldEnv.showMinimap = !worldEnv.showMinimap
                    SoundEngine.playUiClick()
                    return
                }
                if (dialogs.saveQuitRect.contains(tx, ty)) {
                    SaveManager.saveGame(context, currentEra, player, completedErasMask, prog)
                    hasSavedGame = true
                    savedEra = currentEra
                    isRpgMenuOpen = false
                    state = GameState.TITLE
                    return
                }
            }
            else -> {}
        }
    }


    private fun handleAlchemistTouch(tx: Float, ty: Float) {
        if (dialogs.closeSheetRect.contains(tx, ty)) {
            isAlchemistOpen = false
            return
        }
        for (i in dialogs.alchemistItemRects.indices) {
            val (rect, ptype) = dialogs.alchemistItemRects[i]
            if (rect.contains(tx, ty)) {
                when (i) {
                    0 -> if (player.coins >= 45) {
                        player.coins -= 45; prog.invisPotions++; SoundEngine.playCoin()
                        prog.advanceQuest("q_open_alchemist", 1)?.let { q -> player.coins += q.rewardGold; grantXp(q.rewardXp) }
                        floatingTexts.add(FloatingText("+1 مشروب الإخفاء!", player.x, player.y - 30f, Color.parseColor("#E040FB")))
                    } else floatingTexts.add(FloatingText("الذهب غير كافٍ!", player.x, player.y - 30f, Color.RED))
                    1 -> if (player.wood >= 10) {
                        player.wood -= 10; player.iron += 2; SoundEngine.playCoin()
                        floatingTexts.add(FloatingText("حولت 10 خشب إلى +2 حديد!", player.x, player.y - 30f, Color.YELLOW))
                    } else floatingTexts.add(FloatingText("تحتاج 10 خشب!", player.x, player.y - 30f, Color.RED))
                    2 -> if (player.wood >= 15) {
                        player.wood -= 15; prog.crystal += 1; SoundEngine.playCoin()
                        floatingTexts.add(FloatingText("حولت 15 خشب إلى +1 كريستال!", player.x, player.y - 30f, Color.CYAN))
                    } else floatingTexts.add(FloatingText("تحتاج 15 خشب!", player.x, player.y - 30f, Color.RED))
                    else -> if (player.wood >= 20) {
                        player.wood -= 20; prog.mythril += 1; SoundEngine.playCoin()
                        floatingTexts.add(FloatingText("حولت 20 خشب إلى +1 ميثريل!", player.x, player.y - 30f, Color.parseColor("#E040FB")))
                    } else floatingTexts.add(FloatingText("تحتاج 20 خشب!", player.x, player.y - 30f, Color.RED))
                }
                SaveManager.saveGame(context, currentEra, player, completedErasMask, prog)
                return
            }
        }
    }

    private fun handleShopTouch(tx: Float, ty: Float) {
        if (dialogs.closeSheetRect.contains(tx, ty)) {
            isShopOpen = false
            return
        }
        if (dialogs.scrollUpRect.contains(tx, ty)) {
            dialogs.shopScrollOffset = (dialogs.shopScrollOffset - 1).coerceAtLeast(0)
            SoundEngine.playUiClick()
            return
        }
        if (dialogs.scrollDownRect.contains(tx, ty)) {
            dialogs.shopScrollOffset++
            SoundEngine.playUiClick()
            return
        }

        for ((rect, item) in dialogs.shopItemRects) {
            if (rect.contains(tx, ty)) {
                val isOwned = when (item.id) {
                    "sword_pirate" -> player.equippedWeapon.id == "sword_pirate" || player.equippedWeapon.damage >= 55
                    "sword_fire" -> player.equippedWeapon.id == "sword_fire" || player.equippedWeapon.damage >= 75
                    "spear_chrono" -> player.equippedWeapon.id == "spear_chrono" || player.equippedWeapon.damage >= 95
                    "armor_royal" -> player.hasRoyalArmor || player.hasNanoArmor
                    else -> false
                }
                if (isOwned) {
                    floatingTexts.add(FloatingText("هذا العنصر مملوك بالفعل!", player.x, player.y - 30f, Color.CYAN))
                    return
                }
                if (player.coins >= item.price) {
                    player.coins -= item.price
                    SoundEngine.playCoin()
                    when (item.type) {
                        ItemType.AMMO -> {
                            prog.ammo += 20
                            floatingTexts.add(FloatingText("+20 طلقة للمسدس!", player.x, player.y - 30f, Color.CYAN))
                        }
                        ItemType.FOOD -> {
                            prog.food += 2
                            prog.seeds += 3
                            floatingTexts.add(FloatingText("+2 طعام +3 بذور!", player.x, player.y - 30f, Color.GREEN))
                        }
                        ItemType.POTION -> {
                            player.potions += 2
                            floatingTexts.add(FloatingText("+2 جرعات شفاء!", player.x, player.y - 30f, Color.GREEN))
                        }
                        ItemType.WEAPON -> {
                            if (item.id == "sword_pirate") { player.equippedWeapon = GameCatalogue.WEAPON_PIRATE; floatingTexts.add(FloatingText("تم تجهيز سيف القبطان!", player.x, player.y - 30f, Color.YELLOW)) } else if (item.id == "sword_fire") {
                                player.equippedWeapon = GameCatalogue.WEAPON_FIRE
                                floatingTexts.add(FloatingText("تم تجهيز نصل النار!", player.x, player.y - 30f, Color.parseColor("#FF5722")))
                            } else if (item.id == "spear_chrono") {
                                player.equippedWeapon = GameCatalogue.WEAPON_CHRONO
                                floatingTexts.add(FloatingText("تم تجهيز رمح كرونو!", player.x, player.y - 30f, Color.parseColor("#00E5FF")))
                            }
                        }
                        ItemType.ARMOR -> {
                            player.hasRoyalArmor = true
                            player.recalculateMaxHp(prog.getBonusMaxHp())
                            player.hp = (player.hp + 70).coerceAtMost(player.maxHp)
                            floatingTexts.add(FloatingText("درع ملكي! (صحة ${player.maxHp})", player.x, player.y - 30f, Color.YELLOW))
                        }
                    }
                    SaveManager.saveGame(context, currentEra, player, completedErasMask, prog)
                } else {
                    floatingTexts.add(FloatingText("الذهب غير كافٍ!", player.x, player.y - 30f, Color.RED))
                }
                return
            }
        }
    }

    private fun handleCraftTouch(tx: Float, ty: Float) {
        if (dialogs.closeSheetRect.contains(tx, ty)) {
            isCraftingOpen = false
            return
        }
        if (dialogs.scrollUpRect.contains(tx, ty)) {
            dialogs.craftScrollOffset = (dialogs.craftScrollOffset - 1).coerceAtLeast(0)
            SoundEngine.playUiClick()
            return
        }
        if (dialogs.scrollDownRect.contains(tx, ty)) {
            dialogs.craftScrollOffset++
            SoundEngine.playUiClick()
            return
        }

        for ((rect, recipe) in dialogs.craftItemRects) {
            if (rect.contains(tx, ty)) {
                val isOwned = when (recipe.resultType) {
                    "GODBLADE" -> player.hasGodBlade
                    "PLASMA" -> player.hasPlasmaCannon
                    "PICKAXE" -> player.hasPickaxe
                    "SHIELD" -> player.hasNanoArmor
                    else -> false
                }
                if (isOwned) {
                    floatingTexts.add(FloatingText("تمت صناعته مسبقاً!", player.x, player.y - 30f, Color.CYAN))
                    return
                }
                if (player.wood >= recipe.woodCost && player.stone >= recipe.stoneCost && player.iron >= recipe.ironCost && prog.crystal >= recipe.crystalCost && prog.mythril >= recipe.mythrilCost) {
                    player.wood -= recipe.woodCost
                    player.stone -= recipe.stoneCost
                    player.iron -= recipe.ironCost
                    prog.crystal -= recipe.crystalCost
                    prog.mythril -= recipe.mythrilCost
                    SoundEngine.playCoin()
                    prog.unlockAchievement("ach_craft")

                    when (recipe.resultType) {
                        "GODBLADE" -> {
                            player.hasGodBlade = true
                            player.equippedWeapon = GameCatalogue.WEAPON_GODBLADE
                            triggerScreenShake(0.5f, 16f)
                            floatingTexts.add(FloatingText("تم تجهيز نصل الأبعاد (160 ضرر)!", player.x - 40f, player.y - 30f, Color.parseColor("#E040FB")))
                        }
                        "PLASMA" -> {
                            player.hasPlasmaCannon = true
                            if (player.equippedWeapon.damage < GameCatalogue.WEAPON_PLASMA.damage) {
                                player.equippedWeapon = GameCatalogue.WEAPON_PLASMA
                            }
                            floatingTexts.add(FloatingText("تم تجهيز مدفع البلازما (120 ضرر)!", player.x, player.y - 30f, Color.parseColor("#76FF03")))
                        }
                        "PICKAXE" -> {
                            player.hasPickaxe = true
                            floatingTexts.add(FloatingText("صنعت فأس التنقيب!", player.x, player.y - 30f, Color.parseColor("#FFD54F")))
                        }
                        "AMMO" -> { prog.ammo += 15; floatingTexts.add(FloatingText("+15 طلقة مسدس!", player.x, player.y - 30f, Color.CYAN)) }
                        "BOMB" -> {
                            player.bombs += 2
                            floatingTexts.add(FloatingText("+2 قنابل متفجرة!", player.x, player.y - 30f, Color.parseColor("#FF6D00")))
                        }
                        "SHIELD" -> {
                            player.hasNanoArmor = true
                            player.recalculateMaxHp(prog.getBonusMaxHp())
                            player.hp = (player.hp + 140).coerceAtMost(player.maxHp)
                            floatingTexts.add(FloatingText("صنع درع النانو! (صحة ${player.maxHp})", player.x, player.y - 30f, Color.YELLOW))
                        }
                    }
                    SaveManager.saveGame(context, currentEra, player, completedErasMask, prog)
                } else {
                    floatingTexts.add(FloatingText("الموارد غير كافية!", player.x, player.y - 30f, Color.RED))
                }
                return
            }
        }
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
                    if (hasSavedGame && dialogs.continueBtnRect.contains(touchX, touchY)) {
                        continueSavedGame()
                    } else if (dialogs.newGameBtnRect.contains(touchX, touchY) || !hasSavedGame) {
                        SaveManager.clearSave(context)
                        hasSavedGame = false
                        savedEra = null
                        initNewGame()
                        state = GameState.PLAYING
                    }
                }
                return true
            }
            GameState.GAME_OVER -> {
                if (action == MotionEvent.ACTION_DOWN) {
                    if (SaveManager.hasSavedGame(context)) continueSavedGame()
                    else {
                        initNewGame()
                        state = GameState.PLAYING
                    }
                }
                return true
            }
            GameState.ENDING_CHOICE -> {
                if (action == MotionEvent.ACTION_DOWN) {
                    if (dialogs.endingFixRect.contains(touchX, touchY)) {
                        selectedEnding = 1
                        SaveManager.clearSave(context)
                        hasSavedGame = false
                        savedEra = null
                        state = GameState.VICTORY
                    } else if (dialogs.endingAcceptRect.contains(touchX, touchY)) {
                        selectedEnding = 2
                        SaveManager.clearSave(context)
                        hasSavedGame = false
                        savedEra = null
                        state = GameState.VICTORY
                    }
                }
                return true
            }
            GameState.VICTORY -> {
                if (action == MotionEvent.ACTION_DOWN) {
                    initNewGame()
                    state = GameState.PLAYING
                }
                return true
            }
            GameState.PLAYING -> {
                if (isAlchemistOpen) {
                    if (action == MotionEvent.ACTION_DOWN) handleAlchemistTouch(touchX, touchY)
                    return true
                }
                if (isRpgMenuOpen) {
                    if (action == MotionEvent.ACTION_DOWN) handleRpgMenuTouch(touchX, touchY)
                    return true
                }
                if (isShopOpen) {
                    if (action == MotionEvent.ACTION_DOWN) handleShopTouch(touchX, touchY)
                    return true
                }
                if (isCraftingOpen) {
                    if (action == MotionEvent.ACTION_DOWN) handleCraftTouch(touchX, touchY)
                    return true
                }
                if (isDialogueOpen) {
                    if (action == MotionEvent.ACTION_DOWN) nextDialogue()
                    return true
                }

                when (action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                        for (slotIdx in hud.hotbarSlotRects.indices) {
                            if (hud.hotbarSlotRects[slotIdx].contains(touchX, touchY)) {
                                SoundEngine.playUiClick()
                                when (slotIdx) {
                                    4 -> if (player.usePotion()) {
                                        SoundEngine.playPotion()
                                        floatingTexts.add(FloatingText("+85 صحة!", player.x, player.y - 30f, Color.GREEN))
                                    }
                                    5 -> if (prog.invisPotions > 0 && player.activateInvisibility()) {
                                        prog.invisPotions--
                                        SoundEngine.playSlowMo()
                                        floatingTexts.add(FloatingText("تم تفعيل الإخفاء (12 ثانية)!", player.x - 40f, player.y - 30f, Color.parseColor("#E040FB")))
                                    } else if (prog.invisPotions <= 0) {
                                        floatingTexts.add(FloatingText("اشترِ مشروب الإخفاء من تاجر الكيمياء!", player.x - 60f, player.y - 30f, Color.LTGRAY))
                                    }
                                    6 -> if (prog.food > 0) {
                                        prog.food--
                                        prog.hunger = (prog.hunger + 40f).coerceAtMost(prog.maxHunger)
                                        player.hp = (player.hp + 20).coerceAtMost(player.maxHp)
                                        SoundEngine.playPotion()
                                        floatingTexts.add(FloatingText("أكلت وجبة (+40 جوع +20 صحة)", player.x, player.y - 30f, Color.GREEN))
                                    } else {
                                        floatingTexts.add(FloatingText("لا يوجد طعام! ازرع واحصد أو اشترِ!", player.x - 40f, player.y - 30f, Color.LTGRAY))
                                    }
                                    else -> {
                                        hud.selectedHotbarSlot = slotIdx
                                        attackBtn.label = when (slotIdx) {
                                            1 -> "إطلاق"
                                            2 -> "زرع C4"
                                            3 -> "بناء"
                                            else -> "ضربة"
                                        }
                                    }
                                }
                                return true
                            }
                        }
                        if (hud.menuBtnRect.contains(touchX, touchY)) {
                            SoundEngine.playUiClick()
                            isRpgMenuOpen = true
                            return true
                        }
                        if (hud.muteBtnRect.contains(touchX, touchY)) {
                            SoundEngine.isMuted = !SoundEngine.isMuted
                            return true
                        }
                        if (joystick.onTouchDown(pointerId, touchX, touchY)) return true
                        if (attackBtn.onTouchDown(pointerId, touchX, touchY)) {
                            when (hud.selectedHotbarSlot) {
                                1 -> {
                                    val bullet = player.shootAimedGun(enemies, joystick.deltaX, joystick.deltaY, prog.ammo, prog.getDamageMultiplier())
                                    if (bullet != null) {
                                        prog.ammo--
                                        projectiles.add(bullet)
                                        SoundEngine.playMagic()
                                        triggerScreenShake(0.06f, 3f)
                                    } else {
                                        floatingTexts.add(FloatingText("نفدت طلقات المسدس!", player.x - 30f, player.y - 25f, Color.RED))
                                    }
                                }
                                2 -> {
                                    if (player.bombs > 0) {
                                        player.bombs--
                                        plantedExplosives.add(PlantedExplosive(player.x + player.width / 2f, player.y + player.height / 2f))
                                        SoundEngine.playUiClick()
                                        floatingTexts.add(FloatingText("تم زرع متفجر C4!", player.x, player.y - 30f, Color.parseColor("#FF6D00")))
                                    } else {
                                        floatingTexts.add(FloatingText("لا توجد متفجرات!", player.x, player.y - 30f, Color.LTGRAY))
                                    }
                                }
                                3 -> {
                                    if (player.wood >= 2 && player.stone >= 2) {
                                        player.wood -= 2; player.stone -= 2
                                        val bx = player.x + (if (player.facingLeft) -90f else player.width + 15f)
                                        val by = player.y + 10f
                                        barricades.add(PlayerBarricade(bx, by))
                                        rebuildActiveObstacles()
                                        SoundEngine.playHit()
                                        floatingTexts.add(FloatingText("تم بناء متراس!", bx, by - 20f, Color.YELLOW))
                                    } else {
                                        floatingTexts.add(FloatingText("تحتاج 2 خشب + 2 حجر!", player.x, player.y - 30f, Color.LTGRAY))
                                    }
                                }
                                else -> {
                                    if (player.attack()) {
                                        SoundEngine.playSlash()
                                        if (player.isWhirlwindActive) {
                                            particles.emitBurst(player.x + player.width / 2, player.y + player.height / 2, 14, Color.parseColor("#00E5FF"), speed = 260f)
                                        }
                                    } else if (player.stamina < 14f) {
                                        floatingTexts.add(FloatingText("اللياقة منخفضة!", player.x, player.y - 25f, Color.YELLOW))
                                    }
                                }
                            }
                            return true
                        }
                        if (dashBtn.onTouchDown(pointerId, touchX, touchY)) {
                            if (player.dash()) {
                                particles.emitBurst(player.x + player.width / 2, player.y + player.height - 10f, 10, Color.LTGRAY, speed = 130f, size = 6f, life = 0.3f)
                                triggerScreenShake(0.08f, 3f)
                                floatingTexts.add(FloatingText("مراوغة!", player.x, player.y - 25f, Color.parseColor("#BA68C8")))
                            }
                            return true
                        }
                        if (slowMoBtn.onTouchDown(pointerId, touchX, touchY)) {
                            if (player.activateSlowMo()) {
                                SoundEngine.playSlowMo()
                                triggerScreenShake(0.25f, 9f)
                                floatingTexts.add(FloatingText("تجميد الزمن!", player.x - 30f, player.y - 30f, Color.parseColor("#FFD700")))
                            } else {
                                val proj = player.castMagic(prog.getDamageMultiplier())
                                if (proj != null) {
                                    projectiles.add(proj)
                                    SoundEngine.playMagic()
                                } else {
                                    floatingTexts.add(FloatingText("طاقة السحر لا تكفي!", player.x, player.y - 25f, Color.LTGRAY))
                                }
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
                            joystick.onTouchMove(event.getPointerId(i), event.getX(i), event.getY(i))
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                        joystick.onTouchUp(pointerId)
                        attackBtn.onTouchUp(pointerId)
                        dashBtn.onTouchUp(pointerId)
                        slowMoBtn.onTouchUp(pointerId)
                        interactBtn.onTouchUp(pointerId)
                    }
                }
                return true
            }
        }
    }
}
