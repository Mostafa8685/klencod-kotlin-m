package com.chronoknight.game

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
import com.chronoknight.game.engine.ActionButton
import com.chronoknight.game.engine.Camera2D
import com.chronoknight.game.engine.VirtualJoystick
import com.chronoknight.game.model.Enemy
import com.chronoknight.game.model.EraType
import com.chronoknight.game.model.Player
import com.chronoknight.game.model.TimePortal
import com.chronoknight.game.story.DialogueLine
import com.chronoknight.game.story.StoryManager

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    private var gameThread: Thread? = null
    private var isPlaying = false

    private val mapWidth = 2400f
    private val mapHeight = 1600f

    private var camera: Camera2D = Camera2D(1280f, 720f, mapWidth, mapHeight)
    private var joystick: VirtualJoystick = VirtualJoystick(180f, 540f, 130f, 55f)
    private var attackBtn: ActionButton = ActionButton(1120f, 550f, 75f, "ضربة", Color.parseColor("#E53935"))
    private var interactBtn: ActionButton = ActionButton(960f, 580f, 55f, "تفاعل", Color.parseColor("#1E88E5"))

    private var currentEra: EraType = EraType.ERA_1_MEDIEVAL
    private lateinit var player: Player
    private val enemies = mutableListOf<Enemy>()
    private lateinit var portal: TimePortal

    // Dialogue State
    private var isDialogueOpen = false
    private var dialogueQueue = mutableListOf<DialogueLine>()
    private var currentDialogue: DialogueLine? = null

    // Game States
    enum class GameState { TITLE, PLAYING, GAME_OVER, VICTORY }
    private var state = GameState.TITLE

    // Background tile cache
    private var bgTile: Bitmap? = null

    init {
        holder.addCallback(this)
        isFocusable = true
        initGame()
    }

    private fun initGame() {
        player = Player(context, 300f, 300f)
        portal = TimePortal(mapWidth - 300f, mapHeight / 2f)
        setupEra(currentEra)

        // Load background tile
        try {
            val tileAsset = when (currentEra) {
                EraType.ERA_1_MEDIEVAL -> "environment/medieval_tiles.png"
                EraType.ERA_2_CAVES -> "environment/cave_totem.png"
                EraType.ERA_3_ZOMBIE -> "environment/chest.png"
            }
            context.assets.open(tileAsset).use { input ->
                bgTile = BitmapFactory.decodeStream(input)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupEra(era: EraType) {
        currentEra = era
        enemies.clear()
        player.x = 250f
        player.y = 250f
        player.hp = player.maxHp
        player.isDead = false
        portal.isActive = false

        // Spawn Boss
        val boss = Enemy(context, mapWidth - 450f, mapHeight / 2f, isBoss = true, era = era)
        enemies.add(boss)

        // Spawn Minions
        val minionCount = if (era == EraType.ERA_3_ZOMBIE) 8 else 5
        for (i in 0 until minionCount) {
            val ex = 600f + (i * 220f) % (mapWidth - 800f)
            val ey = 300f + (i * 180f) % (mapHeight - 600f)
            enemies.add(Enemy(context, ex, ey, isBoss = false, era = era))
        }

        // Show dialogue
        showDialogue(StoryManager.getIntroDialogue())
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
        resume()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        camera.viewWidth = width.toFloat()
        camera.viewHeight = height.toFloat()
        joystick = VirtualJoystick(200f, height - 180f, 130f, 55f)
        attackBtn = ActionButton(width - 160f, height - 160f, 80f, "هجوم", Color.parseColor("#E53935"))
        interactBtn = ActionButton(width - 320f, height - 140f, 60f, "تقدم", Color.parseColor("#1E88E5"))
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        pause()
    }

    fun resume() {
        isPlaying = true
        gameThread = Thread(this)
        gameThread?.start()
    }

    fun pause() {
        isPlaying = false
        try {
            gameThread?.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    override fun run() {
        var lastTime = System.nanoTime()
        while (isPlaying) {
            val now = System.nanoTime()
            val deltaTime = ((now - lastTime) / 1_000_000_000.0).toFloat().coerceIn(0f, 0.05f)
            lastTime = now

            update(deltaTime)
            render()

            // 60 FPS limiter
            val sleepMs = 16 - ((System.nanoTime() - now) / 1_000_000)
            if (sleepMs > 0) {
                try {
                    Thread.sleep(sleepMs)
                } catch (ignored: Exception) {}
            }
        }
    }

    private fun update(deltaTime: Float) {
        if (state == GameState.TITLE || state == GameState.GAME_OVER || state == GameState.VICTORY) return
        if (isDialogueOpen) return

        player.update(deltaTime, joystick.deltaX, joystick.deltaY, mapWidth, mapHeight)
        camera.update(player.x, player.y)

        portal.update(deltaTime)

        // Check if all enemies dead -> activate portal
        val activeEnemies = enemies.filter { !it.isDead }
        if (activeEnemies.isEmpty() && !portal.isActive) {
            portal.isActive = true
        }

        // Update enemies
        for (enemy in enemies) {
            enemy.update(deltaTime, player)
        }

        // Player attack hits enemies
        if (player.isAttacking) {
            val attackHitbox = RectF(
                if (player.facingLeft) player.x - 40f else player.x + player.width,
                player.y + 10f,
                if (player.facingLeft) player.x else player.x + player.width + 40f,
                player.y + player.height - 10f
            )

            for (enemy in enemies) {
                if (!enemy.isDead && RectF.intersects(attackHitbox, enemy.hitbox)) {
                    enemy.takeDamage(40)
                    if (enemy.isBoss && enemy.isDead) {
                        // Boss defeated dialogue
                        showDialogue(StoryManager.getEraClearDialogue(currentEra))
                    }
                }
            }
        }

        // Check portal entry
        if (portal.isActive && portal.isPlayerInside(player.x + player.width / 2f, player.y + player.height / 2f)) {
            advanceEra()
        }

        // Game over
        if (player.isDead) {
            state = GameState.GAME_OVER
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

    private fun render() {
        if (!holder.surface.isValid) return
        val canvas = holder.lockCanvas() ?: return

        try {
            when (state) {
                GameState.TITLE -> drawTitleScreen(canvas)
                GameState.PLAYING -> drawGameWorld(canvas)
                GameState.GAME_OVER -> drawGameOverScreen(canvas)
                GameState.VICTORY -> drawVictoryScreen(canvas)
            }
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun drawGameWorld(canvas: Canvas) {
        // Clear background
        canvas.drawColor(currentEra.bgColor)

        canvas.save()
        canvas.translate(-camera.x, -camera.y)

        // Draw Map Grid / Tiles
        val gridPaint = Paint().apply {
            color = Color.argb(40, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val tileSize = 120f
        var x = 0f
        while (x < mapWidth) {
            canvas.drawLine(x, 0f, x, mapHeight, gridPaint)
            x += tileSize
        }
        var y = 0f
        while (y < mapHeight) {
            canvas.drawLine(0f, y, mapWidth, y, gridPaint)
            y += tileSize
        }

        // Draw Portal
        portal.draw(canvas)

        // Draw Enemies
        for (enemy in enemies) {
            enemy.draw(canvas)
        }

        // Draw Player
        player.draw(canvas)

        canvas.restore()

        // Draw HUD (Screen space)
        drawHUD(canvas)

        // Controls
        if (!isDialogueOpen) {
            joystick.draw(canvas)
            attackBtn.draw(canvas)
            interactBtn.draw(canvas)
        }

        // Dialogue Box
        if (isDialogueOpen) {
            drawDialogueBox(canvas)
        }
    }

    private fun drawHUD(canvas: Canvas) {
        val p = Paint()

        // Player HP Bar
        p.color = Color.argb(180, 0, 0, 0)
        canvas.drawRoundRect(40f, 40f, 360f, 90f, 15f, 15f, p)
        p.color = Color.parseColor("#4CAF50")
        val hpWidth = (300f * (player.hp.toFloat() / player.maxHp)).coerceAtLeast(0f)
        canvas.drawRoundRect(50f, 50f, 50f + hpWidth, 80f, 10f, 10f, p)

        p.color = Color.WHITE
        p.textSize = 28f
        p.isFakeBoldText = true
        canvas.drawText("فارس الزمن: ${player.hp}/${player.maxHp}", 60f, 75f, p)

        // Era Title in Center
        p.textAlign = Paint.Align.CENTER
        p.textSize = 32f
        p.color = Color.parseColor("#FFD700")
        canvas.drawText(currentEra.title, width / 2f, 55f, p)
        p.textSize = 22f
        p.color = Color.LTGRAY
        canvas.drawText(currentEra.subtitle, width / 2f, 90f, p)

        // Enemies remaining
        val left = enemies.count { !it.isDead }
        p.textAlign = Paint.Align.RIGHT
        p.textSize = 26f
        p.color = Color.WHITE
        val statusText = if (left == 0) "البوابة مفتوحة! اعبر للعهد التالي" else "الوحوش المتبقية: $left"
        canvas.drawText(statusText, width - 40f, 60f, p)
    }

    private fun drawDialogueBox(canvas: Canvas) {
        val dialogue = currentDialogue ?: return
        val boxRect = RectF(80f, height - 280f, width - 80f, height - 40f)

        val p = Paint()
        p.color = Color.argb(235, 15, 15, 25)
        canvas.drawRoundRect(boxRect, 20f, 20f, p)

        p.color = Color.parseColor("#FFD700")
        p.style = Paint.Style.STROKE
        p.strokeWidth = 4f
        canvas.drawRoundRect(boxRect, 20f, 20f, p)

        // Speaker Name
        p.style = Paint.Style.FILL
        p.textSize = 32f
        p.isFakeBoldText = true
        p.textAlign = Paint.Align.LEFT
        canvas.drawText(dialogue.speaker, 110f, height - 230f, p)

        // Arabic text
        p.color = Color.WHITE
        p.textSize = 26f
        p.isFakeBoldText = false
        canvas.drawText(dialogue.textAr, 110f, height - 175f, p)

        // English text
        p.color = Color.LTGRAY
        p.textSize = 22f
        canvas.drawText(dialogue.textEn, 110f, height - 120f, p)

        // Tap to continue prompt
        p.color = Color.parseColor("#4FC3F7")
        p.textSize = 20f
        p.textAlign = Paint.Align.RIGHT
        canvas.drawText("اضغط للمتابعة (Tap to continue) ▶", width - 110f, height - 60f, p)
    }

    private fun drawTitleScreen(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#0D1117"))
        val p = Paint().apply {
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        p.color = Color.parseColor("#FFD700")
        p.textSize = 68f
        canvas.drawText("CHRONO KNIGHT", width / 2f, height / 2f - 90f, p)

        p.color = Color.WHITE
        p.textSize = 38f
        canvas.drawText("فارس الزمن: صراع العصور والوحوش", width / 2f, height / 2f - 20f, p)

        p.color = Color.LTGRAY
        p.textSize = 24f
        canvas.drawText("من العصور الوسطى إلى الكهوف، وحتى نهاية العالم والزومبي", width / 2f, height / 2f + 40f, p)

        p.color = Color.parseColor("#4CAF50")
        p.textSize = 36f
        canvas.drawText("▶ المس الشاشة للبدء (TAP TO START)", width / 2f, height / 2f + 140f, p)
    }

    private fun drawGameOverScreen(canvas: Canvas) {
        canvas.drawColor(Color.argb(220, 30, 0, 0))
        val p = Paint().apply {
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        p.color = Color.RED
        p.textSize = 72f
        canvas.drawText("سقط فارس الزمن (GAME OVER)", width / 2f, height / 2f - 40f, p)

        p.color = Color.WHITE
        p.textSize = 32f
        canvas.drawText("المس الشاشة لإعادة المحاولة", width / 2f, height / 2f + 50f, p)
    }

    private fun drawVictoryScreen(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#0A192F"))
        val p = Paint().apply {
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        p.color = Color.parseColor("#FFD700")
        p.textSize = 72f
        canvas.drawText("مبروك! ختمت اللعبة بالكامل!", width / 2f, height / 2f - 70f, p)

        p.color = Color.parseColor("#64FFDA")
        p.textSize = 38f
        canvas.drawText("YOU RESTORED THE TIMELINE!", width / 2f, height / 2f, p)

        p.color = Color.WHITE
        p.textSize = 26f
        canvas.drawText("تم القضاء على وحوش العصور الوسطى والكهوف والزومبي بنجاح!", width / 2f, height / 2f + 60f, p)

        p.color = Color.YELLOW
        p.textSize = 30f
        canvas.drawText("المس الشاشة للعب مجدداً من البداية", width / 2f, height / 2f + 140f, p)
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
                        if (interactBtn.onTouchDown(pointerId, touchX, touchY)) {
                            if (portal.isActive && portal.isPlayerInside(player.x, player.y)) {
                                advanceEra()
                            }
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
                    }
                }
                return true
            }
        }
    }
}
