package com.clashangprpr.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.clashangprpr.app.engine.SoundEngine
import com.clashangprpr.app.model.EraType
import com.clashangprpr.app.model.Player
import com.clashangprpr.app.systems.ProgressionSystem

class GameHUD {
    private val panelRect = RectF(20f, 16f, 410f, 168f)
    private val iconRect = RectF()
    val muteBtnRect = RectF()
    val menuBtnRect = RectF()

    // Minecraft-style bottom Hotbar (6 slots)
    val hotbarSlotRects = Array(7) { RectF() }
    var selectedHotbarSlot: Int = 0 // 0=Sword, 1=Gun, 2=Bomb, 3=Heal, 4=Invis, 5=Speed/Mana

    private val bgPaint = Paint().apply {
        color = Color.argb(215, 14, 18, 28)
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint().apply {
        color = Color.parseColor("#FFB300")
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }
    private val innerBorderPaint = Paint().apply {
        color = Color.argb(90, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    private val barBgPaint = Paint().apply {
        color = Color.argb(220, 26, 30, 42)
        style = Paint.Style.FILL
    }
    private val hpPaint = Paint().apply {
        color = Color.parseColor("#E53935")
        style = Paint.Style.FILL
    }
    private val manaPaint = Paint().apply {
        color = Color.parseColor("#1E88E5")
        style = Paint.Style.FILL
    }
    private val staminaPaint = Paint().apply { color = Color.parseColor("#FFA000"); style = Paint.Style.FILL }
    private val xpPaint = Paint().apply {
        color = Color.parseColor("#8E24AA")
        style = Paint.Style.FILL
    }
    private val barTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 15f
        isFakeBoldText = true
    }
    private val goldPaint = Paint().apply {
        color = Color.parseColor("#FFD700")
        textSize = 17f
        isFakeBoldText = true
    }
    private val countPaint = Paint().apply {
        color = Color.parseColor("#E0E0E0")
        textSize = 16f
        isFakeBoldText = true
    }
    private val eraTitlePaint = Paint().apply {
        color = Color.parseColor("#FFD700")
        textSize = 21f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val eraSubPaint = Paint().apply {
        color = Color.LTGRAY
        textSize = 14f
        textAlign = Paint.Align.CENTER
    }
    private val statusBgPaint = Paint().apply {
        color = Color.argb(200, 14, 18, 28)
        style = Paint.Style.FILL
    }
    private val statusTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 17f
        textAlign = Paint.Align.RIGHT
        isFakeBoldText = true
    }
    private val btnTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 15f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val hotbarBgPaint = Paint().apply {
        color = Color.argb(215, 20, 24, 36)
        style = Paint.Style.FILL
    }
    private val hotbarSelectedPaint = Paint().apply {
        color = Color.parseColor("#FFD700")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val hotbarNormalPaint = Paint().apply {
        color = Color.argb(160, 140, 150, 175)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val hotbarLabelPaint = Paint().apply {
        color = Color.WHITE
        textSize = 12f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private fun drawCleanIconCount(
        canvas: Canvas,
        bmp: Bitmap?,
        fallback: String,
        valueText: String,
        x: Float,
        y: Float,
        paint: Paint
    ) {
        if (bmp != null) {
            iconRect.set(x, y - 18f, x + 22f, y + 4f)
            canvas.drawBitmap(bmp, null, iconRect, null)
            canvas.drawText(valueText, x + 26f, y - 1f, paint)
        } else {
            canvas.drawText("$fallback:$valueText", x, y - 1f, paint)
        }
    }

    fun draw(
        canvas: Canvas,
        screenW: Float,
        screenH: Float,
        player: Player,
        prog: ProgressionSystem,
        currentEra: EraType,
        iconCache: Map<String, Bitmap>,
        activeEnemyCount: Int,
        portalActive: Boolean,
        generatorsLeft: Int,
        completedCount: Int = 0,
        hasClockHand: Boolean = false,
        zoneName: String = "",
        compassHint: String = ""
    ) {
        // Clean, crisp HUD Card (no overlapping sprite-sheet frame!)
        canvas.drawRoundRect(panelRect, 14f, 14f, bgPaint)
        canvas.drawRoundRect(panelRect, 14f, 14f, borderPaint)

        val barLeft = 34f
        val barRight = 394f
        val barW = barRight - barLeft

        // 1. Health Bar
        canvas.drawRoundRect(barLeft, 26f, barRight, 47f, 6f, 6f, barBgPaint)
        val hpW = (barW * (player.hp.toFloat() / player.maxHp)).coerceIn(0f, barW)
        canvas.drawRoundRect(barLeft, 26f, barLeft + hpW, 47f, 6f, 6f, hpPaint)
        canvas.drawRoundRect(barLeft, 26f, barRight, 47f, 6f, 6f, innerBorderPaint)
        canvas.drawText("HP ${player.hp}/${player.maxHp}", barLeft + 10f, 42f, barTextPaint)

        // 2. Mana Bar
        canvas.drawRoundRect(barLeft, 53f, barRight, 71f, 5f, 5f, barBgPaint)
        val manaW = (barW * (player.mana / player.maxMana)).coerceIn(0f, barW)
        canvas.drawRoundRect(barLeft, 53f, barLeft + manaW, 71f, 5f, 5f, manaPaint)
        canvas.drawRoundRect(barLeft, 53f, barRight, 71f, 5f, 5f, innerBorderPaint)
        val statusTag = when {
            player.isInvisible -> " [مخفي]"
            player.isSlowMoActive -> " [تباطؤ]"
            else -> ""
        }
        canvas.drawText("MP ${player.mana.toInt()}/${player.maxMana.toInt()}$statusTag", barLeft + 10f, 67f, barTextPaint)

        // 3. Stamina Bar (Tactical Combat)
        canvas.drawRoundRect(barLeft, 76f, barRight, 91f, 5f, 5f, barBgPaint)
        val stamW = (barW * (player.stamina / player.maxStamina)).coerceIn(0f, barW)
        canvas.drawRoundRect(barLeft, 76f, barLeft + stamW, 91f, 5f, 5f, staminaPaint)
        canvas.drawRoundRect(barLeft, 76f, barRight, 91f, 5f, 5f, innerBorderPaint)
        val counterTag = if (player.counterBuffTimer > 0f) " [ضربة مضادة 2.5x!]" else ""
        canvas.drawText("لياقة ${player.stamina.toInt()}%$counterTag", barLeft + 10f, 89f, barTextPaint)

        // 4. XP & Level Bar
        canvas.drawRoundRect(barLeft, 95f, barRight, 110f, 5f, 5f, barBgPaint)
        val xpW = (barW * (prog.xp.toFloat() / prog.xpToNext.coerceAtLeast(1))).coerceIn(0f, barW)
        canvas.drawRoundRect(barLeft, 95f, barLeft + xpW, 110f, 5f, 5f, xpPaint)
        canvas.drawRoundRect(barLeft, 95f, barRight, 110f, 5f, 5f, innerBorderPaint)
        canvas.drawText("Lv.${prog.level}  (${prog.xp}/${prog.xpToNext} XP)", barLeft + 10f, 108f, barTextPaint)

        // 5. Clean Resource Row with Icons
        drawCleanIconCount(canvas, iconCache["icons/item_2.png"], "G", "${player.coins}", 34f, 134f, goldPaint)
        drawCleanIconCount(canvas, iconCache["icons/item_5.png"], "W", "${player.wood}", 120f, 134f, countPaint)
        drawCleanIconCount(canvas, iconCache["icons/item_4.png"], "S", "${player.stone}", 192f, 134f, countPaint)
        drawCleanIconCount(canvas, iconCache["icons/item_20.png"], "I", "${player.iron}", 264f, 134f, countPaint)

        val hungerPct = prog.hunger.toInt()
        val hungerPaint = if (hungerPct < 25) barTextPaint else countPaint
        canvas.drawText("جوع: $hungerPct% | كريستال: ${prog.crystal} | ميثريل: ${prog.mythril}", 34f, 158f, hungerPaint)

        // Era Title (Center Top)
        if (screenW >= 1050f) {
            canvas.drawText(currentEra.title, screenW / 2f + 20f, 34f, eraTitlePaint)
            val coords = if (zoneName.isNotEmpty()) " (${player.x.toInt()/32},${player.y.toInt()/32})" else ""
            val subLine = if (zoneName.isNotEmpty()) "$zoneName$coords | $compassHint" else currentEra.subtitle
            canvas.drawText(subLine, screenW / 2f + 20f, 56f, eraSubPaint)
        }

        // Objective Status (Top Right)
        val statusText = when {
            currentEra == EraType.ERA_0_HUB -> "الشظايا: $completedCount / 6"
            currentEra == EraType.ERA_2_OPEN_WORLD -> if (hasClockHand) "عقرب الساعة معك! عد للبوابة!" else "اهزم زعيم الحصن لتحصل على عقرب الساعة"
            portalActive -> "بوابة العبور مفتوحة!"
            currentEra == EraType.ERA_6_VOID -> "اقضِ على سيد الفراغ!"
            currentEra == EraType.ERA_5_CYBER -> if (generatorsLeft > 0) "المولدات: $generatorsLeft" else "اهزم القائد!"
            currentEra == EraType.ERA_4_ISLAND && !player.hasOceanGem -> "جد المفتاح والجوهرة!"
            else -> "الأعداء: $activeEnemyCount"
        }
        val textW = statusTextPaint.measureText(statusText)
        canvas.drawRoundRect(screenW - textW - 46f, 16f, screenW - 18f, 52f, 10f, 10f, statusBgPaint)
        canvas.drawText(statusText, screenW - 30f, 40f, statusTextPaint)

        // Mute & Menu Buttons
        muteBtnRect.set(screenW - 118f, 60f, screenW - 18f, 96f)
        canvas.drawRoundRect(muteBtnRect, 9f, 9f, statusBgPaint)
        canvas.drawRoundRect(muteBtnRect, 9f, 9f, borderPaint)
        canvas.drawText(if (SoundEngine.isMuted) "صوت: ✖" else "صوت: ✔", muteBtnRect.centerX(), muteBtnRect.centerY() + 5f, btnTextPaint)

        menuBtnRect.set(screenW - 242f, 60f, screenW - 126f, 96f)
        canvas.drawRoundRect(menuBtnRect, 9f, 9f, statusBgPaint)
        canvas.drawRoundRect(menuBtnRect, 9f, 9f, borderPaint)
        val ptsNote = if (prog.skillPoints > 0) " (${prog.skillPoints})" else ""
        canvas.drawText("القائمة$ptsNote", menuBtnRect.centerX(), menuBtnRect.centerY() + 5f, btnTextPaint)

        // Clean 7-slot Minecraft-style Bottom Hotbar
        val slotSize = 56f
        val gap = 7f
        val totalW = 7 * slotSize + 6 * gap
        val startX = (screenW - totalW) / 2f
        val startY = screenH - slotSize - 14f

        val slotData = listOf(
            Triple(player.equippedWeapon.iconAsset, "سيف", ""),
            Triple("icons/ammo_bullet.png", "مسدس", "${prog.ammo}"),
            Triple("icons/item_1.png", "C4/قنبلة", "${player.bombs}"),
            Triple("icons/item_5.png", "متراس", "${player.wood/2}"),
            Triple("icons/potion_health.png", "شفاء", "${player.potions}"),
            Triple("icons/potion_2.png", "إخفاء", "${prog.invisPotions}"),
            Triple("icons/pirate_3.png", "طعام", "${prog.food}")
        )

        for (i in 0 until 7) {
            val sx = startX + i * (slotSize + gap)
            val rect = hotbarSlotRects[i]
            rect.set(sx, startY, sx + slotSize, startY + slotSize)

            canvas.drawRoundRect(rect, 10f, 10f, hotbarBgPaint)
            canvas.drawRoundRect(rect, 10f, 10f, if (selectedHotbarSlot == i) hotbarSelectedPaint else hotbarNormalPaint)

            val (icoPath, label, count) = slotData[i]
            iconCache[icoPath]?.let { bmp ->
                iconRect.set(sx + 12f, startY + 6f, sx + slotSize - 12f, startY + slotSize - 20f)
                canvas.drawBitmap(bmp, null, iconRect, null)
            }
            canvas.drawText(label, rect.centerX(), startY + slotSize - 5f, hotbarLabelPaint)
            if (count.isNotEmpty()) {
                canvas.drawText(count, sx + slotSize - 14f, startY + 15f, hotbarLabelPaint)
            }
        }
    }
}
