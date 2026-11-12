package com.clashangprpr.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.Layout
import android.text.TextPaint
import com.clashangprpr.app.engine.SoundEngine
import com.clashangprpr.app.engine.TextHelper
import com.clashangprpr.app.model.CraftRecipe
import com.clashangprpr.app.model.CraftingRegistry
import com.clashangprpr.app.model.EraType
import com.clashangprpr.app.model.GameCatalogue
import com.clashangprpr.app.model.Player
import com.clashangprpr.app.model.ShopItem
import com.clashangprpr.app.story.DialogueLine
import com.clashangprpr.app.systems.PotionType
import com.clashangprpr.app.systems.ProgressionSystem
import com.clashangprpr.app.world.WorldEnvironmentSystem

enum class RpgTab { SKILLS, QUESTS, INVENTORY, SETTINGS }

class GameDialogs {
    private val panelRect = RectF()
    private val iconDstRect = RectF()
    val closeSheetRect = RectF()
    val continueBtnRect = RectF()
    val newGameBtnRect = RectF()

    // Scroll buttons for Shop & Crafting
    val scrollUpRect = RectF()
    val scrollDownRect = RectF()
    var shopScrollOffset = 0
    var craftScrollOffset = 0

    // Ending choice rects
    val endingFixRect = RectF()
    val endingAcceptRect = RectF()

    // RPG Menu rects
    var activeTab: RpgTab = RpgTab.SKILLS
    val tabRects = mutableListOf<Pair<RectF, RpgTab>>()
    val skillBtnRects = mutableListOf<Pair<RectF, String>>()
    val upgradeWeaponRect = RectF()
    val potionUseRects = mutableListOf<Pair<RectF, PotionType>>()
    val toggleMuteRect = RectF()
    val toggleMinimapRect = RectF()
    val saveQuitRect = RectF()

    val shopItemRects = mutableListOf<Pair<RectF, ShopItem>>()
    val craftItemRects = mutableListOf<Pair<RectF, CraftRecipe>>()

    private val panelBgPaint = Paint().apply {
        color = Color.argb(245, 16, 20, 30)
        style = Paint.Style.FILL
    }
    private val panelBorderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val cardBgPaint = Paint().apply {
        color = Color.argb(230, 28, 34, 50)
        style = Paint.Style.FILL
    }
    private val cardBorderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }
    private val closeBtnPaint = Paint().apply {
        color = Color.parseColor("#E53935")
        style = Paint.Style.FILL
    }
    private val scrollBtnPaint = Paint().apply {
        color = Color.parseColor("#37474F")
        style = Paint.Style.FILL
    }
    private val headerPaint = Paint().apply {
        textSize = 25f
        isFakeBoldText = true
        textAlign = Paint.Align.LEFT
    }
    private val closeTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 19f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val itemTitlePaint = Paint().apply {
        color = Color.WHITE
        textSize = 19f
        textAlign = Paint.Align.LEFT
        isFakeBoldText = true
    }
    private val itemDescPaint = Paint().apply {
        color = Color.LTGRAY
        textSize = 15f
        textAlign = Paint.Align.LEFT
    }
    private val itemPricePaint = Paint().apply {
        textSize = 18f
        textAlign = Paint.Align.LEFT
        isFakeBoldText = true
    }
    private val centerTitlePaint = Paint().apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val centerSubPaint = Paint().apply {
        textAlign = Paint.Align.CENTER
    }
    private val btnFillPaint = Paint().apply { style = Paint.Style.FILL }

    private val rtlTextPaint = TextPaint().apply {
        color = Color.WHITE
        textSize = 23f
        isAntiAlias = true
    }
    private val rtlSubPaint = TextPaint().apply {
        color = Color.LTGRAY
        textSize = 18f
        isAntiAlias = true
    }


    val alchemistItemRects = mutableListOf<Pair<RectF, PotionType>>()

    fun drawAlchemistShop(
        canvas: Canvas,
        screenW: Float,
        screenH: Float,
        player: Player,
        prog: ProgressionSystem,
        iconCache: Map<String, Bitmap>
    ) {
        val sheetH = 365f
        panelRect.set(50f, screenH - sheetH - 20f, screenW - 50f, screenH - 20f)
        canvas.drawRoundRect(panelRect, 20f, 20f, panelBgPaint)
        panelBorderPaint.color = Color.parseColor("#E040FB")
        canvas.drawRoundRect(panelRect, 20f, 20f, panelBorderPaint)

        headerPaint.color = Color.parseColor("#E040FB")
        canvas.drawText("متجر تاجر الكيمياء البعيد (ذهبك: ${player.coins})", panelRect.left + 35f, panelRect.top + 45f, headerPaint)

        closeSheetRect.set(panelRect.right - 120f, panelRect.top + 15f, panelRect.right - 20f, panelRect.top + 55f)
        canvas.drawRoundRect(closeSheetRect, 10f, 10f, closeBtnPaint)
        canvas.drawText("إغلاق", closeSheetRect.centerX(), closeSheetRect.centerY() + 7f, closeTextPaint)

        alchemistItemRects.clear()
        val items = listOf(
            Triple(PotionType.SPEED, "مشروب الإخفاء (12 ثانية)", 45),
            Triple(PotionType.STRENGTH, "جرعة القوة (+40% ضرر)", 45),
            Triple(PotionType.HEALTH, "جرعة شفاء كبرى (+85 صحة)", 25),
            Triple(PotionType.MANA, "جرعة المانا الكاملة", 30)
        )
        val cardW = (panelRect.width() - 80f) / 2f
        val cardH = 112f
        val startX = panelRect.left + 30f
        val startY = panelRect.top + 75f

        for (i in items.indices) {
            val (ptype, title, price) = items[i]
            val col = i % 2
            val row = i / 2
            val cx = startX + col * (cardW + 20f)
            val cy = startY + row * (cardH + 15f)
            val cardRect = RectF(cx, cy, cx + cardW, cy + cardH)
            // Tag SPEED slot 0 as invisibility potion purchase
            alchemistItemRects.add(Pair(cardRect, ptype))

            val canAfford = player.coins >= price
            canvas.drawRoundRect(cardRect, 12f, 12f, cardBgPaint)
            cardBorderPaint.color = if (canAfford) Color.parseColor("#E040FB") else Color.parseColor("#EF5350")
            canvas.drawRoundRect(cardRect, 12f, 12f, cardBorderPaint)

            iconCache[ptype.iconAsset]?.let { ico ->
                iconDstRect.set(cx + 12f, cy + 25f, cx + 64f, cy + 77f)
                canvas.drawBitmap(ico, null, iconDstRect, null)
            }
            canvas.drawText(title, cx + 72f, cy + 32f, itemTitlePaint)
            val desc = if (i == 0) "يخفيك عن أعين الوحوش لسرقة المعسكرات!" else ptype.descAr
            canvas.drawText(desc, cx + 72f, cy + 58f, itemDescPaint)
            itemPricePaint.color = if (canAfford) Color.parseColor("#FFD700") else Color.GRAY
            canvas.drawText("شراء ($price ذهب)", cx + 72f, cy + 92f, itemPricePaint)
        }
    }

    fun drawShop(
        canvas: Canvas,
        screenW: Float,
        screenH: Float,
        player: Player,
        frameShopBmp: Bitmap?,
        iconCache: Map<String, Bitmap>
    ) {
        val sheetH = 365f
        panelRect.set(50f, screenH - sheetH - 20f, screenW - 50f, screenH - 20f)

        if (frameShopBmp != null) {
            canvas.drawBitmap(frameShopBmp, null, panelRect, null)
        } else {
            canvas.drawRoundRect(panelRect, 20f, 20f, panelBgPaint)
            panelBorderPaint.color = Color.parseColor("#FFB300")
            canvas.drawRoundRect(panelRect, 20f, 20f, panelBorderPaint)
        }

        headerPaint.color = Color.parseColor("#FFD700")
        canvas.drawText("متجر التاجر إلريك (الذهب: ${player.coins})", panelRect.left + 35f, panelRect.top + 45f, headerPaint)

        closeSheetRect.set(panelRect.right - 120f, panelRect.top + 15f, panelRect.right - 20f, panelRect.top + 55f)
        canvas.drawRoundRect(closeSheetRect, 10f, 10f, closeBtnPaint)
        canvas.drawText("إغلاق", closeSheetRect.centerX(), closeSheetRect.centerY() + 7f, closeTextPaint)

        scrollUpRect.set(panelRect.right - 230f, panelRect.top + 15f, panelRect.right - 185f, panelRect.top + 55f)
        scrollDownRect.set(panelRect.right - 175f, panelRect.top + 15f, panelRect.right - 130f, panelRect.top + 55f)
        canvas.drawRoundRect(scrollUpRect, 8f, 8f, scrollBtnPaint)
        canvas.drawRoundRect(scrollDownRect, 8f, 8f, scrollBtnPaint)
        canvas.drawText("▲", scrollUpRect.centerX(), scrollUpRect.centerY() + 6f, closeTextPaint)
        canvas.drawText("▼", scrollDownRect.centerX(), scrollDownRect.centerY() + 6f, closeTextPaint)

        shopItemRects.clear()
        val cardW = (panelRect.width() - 80f) / 2f
        val cardH = 112f
        val startX = panelRect.left + 30f
        val startY = panelRect.top + 75f

        val maxRow = (GameCatalogue.shopItems.size - 1) / 2
        shopScrollOffset = shopScrollOffset.coerceIn(0, (maxRow - 1).coerceAtLeast(0))

        for (i in GameCatalogue.shopItems.indices) {
            val row = i / 2
            val visibleRow = row - shopScrollOffset
            if (visibleRow < 0 || visibleRow > 1) continue

            val item = GameCatalogue.shopItems[i]
            val col = i % 2
            val cx = startX + col * (cardW + 20f)
            val cy = startY + visibleRow * (cardH + 15f)
            val cardRect = RectF(cx, cy, cx + cardW, cy + cardH)
            shopItemRects.add(Pair(cardRect, item))

            val isOwned = when (item.id) {
                "sword_fire" -> player.equippedWeapon.id == "sword_fire" || player.equippedWeapon.damage >= 60
                "spear_chrono" -> player.equippedWeapon.id == "spear_chrono" || player.equippedWeapon.damage >= 95
                "armor_royal" -> player.hasRoyalArmor || player.hasNanoArmor
                else -> false
            }
            val canAfford = player.coins >= item.price && !isOwned

            canvas.drawRoundRect(cardRect, 12f, 12f, cardBgPaint)
            cardBorderPaint.color = when {
                isOwned -> Color.parseColor("#4FC3F7")
                canAfford -> Color.parseColor("#66BB6A")
                else -> Color.parseColor("#EF5350")
            }
            canvas.drawRoundRect(cardRect, 12f, 12f, cardBorderPaint)

            iconCache[item.iconAsset]?.let { ico ->
                iconDstRect.set(cx + 12f, cy + 25f, cx + 64f, cy + 77f)
                canvas.drawBitmap(ico, null, iconDstRect, null)
            }

            canvas.drawText(item.titleAr, cx + 72f, cy + 30f, itemTitlePaint)
            canvas.drawText(item.descAr, cx + 72f, cy + 56f, itemDescPaint)

            itemPricePaint.color = when {
                isOwned -> Color.parseColor("#4FC3F7")
                canAfford -> Color.parseColor("#FFD700")
                else -> Color.GRAY
            }
            val actionLabel = if (isOwned) "مملوك بالفعل" else "شراء (${item.price} ذهب)"
            canvas.drawText(actionLabel, cx + 72f, cy + 92f, itemPricePaint)
        }
    }

    fun drawCrafting(
        canvas: Canvas,
        screenW: Float,
        screenH: Float,
        player: Player,
        frameInventoryBmp: Bitmap?,
        iconCache: Map<String, Bitmap>
    ) {
        val sheetH = 385f
        panelRect.set(50f, screenH - sheetH - 20f, screenW - 50f, screenH - 20f)

        if (frameInventoryBmp != null) {
            canvas.drawBitmap(frameInventoryBmp, null, panelRect, null)
        } else {
            canvas.drawRoundRect(panelRect, 20f, 20f, panelBgPaint)
            panelBorderPaint.color = Color.parseColor("#BA68C8")
            canvas.drawRoundRect(panelRect, 20f, 20f, panelBorderPaint)
        }

        headerPaint.color = Color.parseColor("#E1BEE7")
        canvas.drawText("طاولة الصناعة (خشب: ${player.wood} | حجر: ${player.stone} | حديد: ${player.iron})", panelRect.left + 35f, panelRect.top + 45f, headerPaint)

        closeSheetRect.set(panelRect.right - 120f, panelRect.top + 15f, panelRect.right - 20f, panelRect.top + 55f)
        canvas.drawRoundRect(closeSheetRect, 10f, 10f, closeBtnPaint)
        canvas.drawText("إغلاق", closeSheetRect.centerX(), closeSheetRect.centerY() + 7f, closeTextPaint)

        scrollUpRect.set(panelRect.right - 230f, panelRect.top + 15f, panelRect.right - 185f, panelRect.top + 55f)
        scrollDownRect.set(panelRect.right - 175f, panelRect.top + 15f, panelRect.right - 130f, panelRect.top + 55f)
        canvas.drawRoundRect(scrollUpRect, 8f, 8f, scrollBtnPaint)
        canvas.drawRoundRect(scrollDownRect, 8f, 8f, scrollBtnPaint)
        canvas.drawText("▲", scrollUpRect.centerX(), scrollUpRect.centerY() + 6f, closeTextPaint)
        canvas.drawText("▼", scrollDownRect.centerX(), scrollDownRect.centerY() + 6f, closeTextPaint)

        craftItemRects.clear()
        val cardW = (panelRect.width() - 80f) / 2f
        val cardH = 110f
        val startX = panelRect.left + 30f
        val startY = panelRect.top + 75f

        val maxRow = (CraftingRegistry.recipes.size - 1) / 2
        craftScrollOffset = craftScrollOffset.coerceIn(0, (maxRow - 1).coerceAtLeast(0))

        for (i in CraftingRegistry.recipes.indices) {
            val row = i / 2
            val visibleRow = row - craftScrollOffset
            if (visibleRow < 0 || visibleRow > 1) continue

            val recipe = CraftingRegistry.recipes[i]
            val col = i % 2
            val cx = startX + col * (cardW + 20f)
            val cy = startY + visibleRow * (cardH + 15f)
            val cardRect = RectF(cx, cy, cx + cardW, cy + cardH)
            craftItemRects.add(Pair(cardRect, recipe))

            val isOwned = when (recipe.resultType) {
                "GODBLADE" -> player.hasGodBlade
                "PLASMA" -> player.hasPlasmaCannon
                "PICKAXE" -> player.hasPickaxe
                "SHIELD" -> player.hasNanoArmor
                else -> false
            }
            val canCraft = !isOwned && player.wood >= recipe.woodCost && player.stone >= recipe.stoneCost && player.iron >= recipe.ironCost

            canvas.drawRoundRect(cardRect, 12f, 12f, cardBgPaint)
            cardBorderPaint.color = when {
                isOwned -> Color.parseColor("#4FC3F7")
                canCraft -> Color.parseColor("#AB47BC")
                else -> Color.parseColor("#EF5350")
            }
            canvas.drawRoundRect(cardRect, 12f, 12f, cardBorderPaint)

            iconCache[recipe.iconAsset]?.let { ico ->
                iconDstRect.set(cx + 12f, cy + 24f, cx + 62f, cy + 74f)
                canvas.drawBitmap(ico, null, iconDstRect, null)
            }

            canvas.drawText(recipe.titleAr, cx + 70f, cy + 30f, itemTitlePaint)
            canvas.drawText("التكلفة: ${recipe.woodCost} خشب، ${recipe.stoneCost} حجر، ${recipe.ironCost} حديد", cx + 70f, cy + 56f, itemDescPaint)

            itemPricePaint.color = when {
                isOwned -> Color.parseColor("#4FC3F7")
                canCraft -> Color.parseColor("#CE93D8")
                else -> Color.GRAY
            }
            val statusLabel = when {
                isOwned -> "مصنوع ومجهز"
                canCraft -> "صنع الآن"
                else -> "الموارد لا تكفي"
            }
            canvas.drawText(statusLabel, cx + 70f, cy + 90f, itemPricePaint)
        }
    }

    fun drawRpgMenu(
        canvas: Canvas,
        screenW: Float,
        screenH: Float,
        player: Player,
        prog: ProgressionSystem,
        env: WorldEnvironmentSystem,
        iconCache: Map<String, Bitmap>
    ) {
        panelRect.set(70f, 50f, screenW - 70f, screenH - 50f)
        canvas.drawRoundRect(panelRect, 22f, 22f, panelBgPaint)
        panelBorderPaint.color = Color.parseColor("#FFD700")
        canvas.drawRoundRect(panelRect, 22f, 22f, panelBorderPaint)

        closeSheetRect.set(panelRect.right - 125f, panelRect.top + 18f, panelRect.right - 22f, panelRect.top + 58f)
        canvas.drawRoundRect(closeSheetRect, 10f, 10f, closeBtnPaint)
        canvas.drawText("استئناف", closeSheetRect.centerX(), closeSheetRect.centerY() + 7f, closeTextPaint)

        // Tabs
        tabRects.clear()
        val tabs = listOf(
            RpgTab.SKILLS to "المهارات (${prog.skillPoints})",
            RpgTab.QUESTS to "المهام والإنجازات",
            RpgTab.INVENTORY to "الترقية والجرعات",
            RpgTab.SETTINGS to "الإعدادات والحفظ"
        )
        var tx = panelRect.left + 26f
        val ty = panelRect.top + 18f
        val tw = 195f
        for ((tab, title) in tabs) {
            val r = RectF(tx, ty, tx + tw, ty + 44f)
            tabRects.add(Pair(r, tab))
            btnFillPaint.color = if (activeTab == tab) Color.parseColor("#FFB300") else Color.parseColor("#263238")
            canvas.drawRoundRect(r, 10f, 10f, btnFillPaint)
            closeTextPaint.color = if (activeTab == tab) Color.BLACK else Color.WHITE
            canvas.drawText(title, r.centerX(), r.centerY() + 6f, closeTextPaint)
            tx += tw + 12f
        }
        closeTextPaint.color = Color.WHITE

        val contentTop = panelRect.top + 82f
        val contentLeft = panelRect.left + 30f
        val contentW = panelRect.width() - 60f

        when (activeTab) {
            RpgTab.SKILLS -> {
                skillBtnRects.clear()
                headerPaint.color = Color.parseColor("#80D8FF")
                canvas.drawText("المستوى ${prog.level} | نقاط المهارة المتاحة: ${prog.skillPoints}", contentLeft, contentTop + 22f, headerPaint)
                var cy = contentTop + 45f
                val cardW = (contentW - 20f) / 2f
                for (i in prog.skills.indices) {
                    val sk = prog.skills[i]
                    val col = i % 2
                    val row = i / 2
                    val cx = contentLeft + col * (cardW + 20f)
                    val ry = cy + row * 95f
                    val r = RectF(cx, ry, cx + cardW, ry + 82f)
                    skillBtnRects.add(Pair(r, sk.id))
                    canvas.drawRoundRect(r, 12f, 12f, cardBgPaint)
                    cardBorderPaint.color = if (prog.skillPoints > 0 && sk.level < sk.maxLevel) Color.parseColor("#66BB6A") else Color.GRAY
                    canvas.drawRoundRect(r, 12f, 12f, cardBorderPaint)
                    canvas.drawText("${sk.titleAr} (${sk.level}/${sk.maxLevel})", cx + 18f, ry + 32f, itemTitlePaint)
                    canvas.drawText(sk.descAr, cx + 18f, ry + 60f, itemDescPaint)
                }
            }
            RpgTab.QUESTS -> {
                headerPaint.color = Color.parseColor("#FFD700")
                canvas.drawText("سجل المهام النشطة:", contentLeft, contentTop + 22f, headerPaint)
                var qy = contentTop + 42f
                for (q in prog.quests) {
                    val mark = if (q.isCompleted) "✔" else "•"
                    itemTitlePaint.color = if (q.isCompleted) Color.parseColor("#81C784") else Color.WHITE
                    canvas.drawText("$mark ${q.titleAr} (${q.currentCount}/${q.targetCount}) - مكافأة: ${q.rewardGold} ذهب", contentLeft, qy + 20f, itemTitlePaint)
                    qy += 38f
                }
                itemTitlePaint.color = Color.WHITE
                headerPaint.color = Color.parseColor("#CE93D8")
                canvas.drawText("الإنجازات:", contentLeft, qy + 34f, headerPaint)
                qy += 56f
                for (a in prog.achievements) {
                    itemDescPaint.color = if (a.isUnlocked) Color.parseColor("#FFD54F") else Color.GRAY
                    val st = if (a.isUnlocked) "مفتوح ★" else "مقفل"
                    canvas.drawText("${a.titleAr}: $st", contentLeft + 15f, qy, itemDescPaint)
                    qy += 28f
                }
                itemDescPaint.color = Color.LTGRAY
            }
            RpgTab.INVENTORY -> {
                headerPaint.color = Color.parseColor("#FFD700")
                canvas.drawText("السلاح الحالي: ${player.equippedWeapon.nameAr} (+${prog.weaponUpgradeLevel}) | الضرر الأساسي: ${player.equippedWeapon.damage}", contentLeft, contentTop + 22f, headerPaint)

                val upgCostGold = 45 + prog.weaponUpgradeLevel * 30
                val upgCostIron = 2 + prog.weaponUpgradeLevel
                upgradeWeaponRect.set(contentLeft, contentTop + 42f, contentLeft + 460f, contentTop + 105f)
                val canUpgrade = player.coins >= upgCostGold && player.iron >= upgCostIron && prog.weaponUpgradeLevel < 5
                btnFillPaint.color = if (canUpgrade) Color.parseColor("#2E7D32") else Color.parseColor("#37474F")
                canvas.drawRoundRect(upgradeWeaponRect, 12f, 12f, btnFillPaint)
                canvas.drawText("تطوير السلاح (+15% ضرر) | التكلفة: $upgCostGold ذهب، $upgCostIron حديد", upgradeWeaponRect.centerX(), upgradeWeaponRect.centerY() + 7f, closeTextPaint)

                headerPaint.color = Color.parseColor("#80D8FF")
                canvas.drawText("خزانة الجرعات الكيميائية (المس للشرب):", contentLeft, contentTop + 145f, headerPaint)
                potionUseRects.clear()
                val pots = listOf(PotionType.SPEED, PotionType.STRENGTH, PotionType.MANA)
                var px = contentLeft
                val py = contentTop + 165f
                for (pt in pots) {
                    val count = when (pt) {
                        PotionType.SPEED -> prog.speedPotions
                        PotionType.STRENGTH -> prog.strengthPotions
                        PotionType.MANA -> prog.manaPotions
                        else -> player.potions
                    }
                    val r = RectF(px, py, px + 290f, py + 95f)
                    potionUseRects.add(Pair(r, pt))
                    canvas.drawRoundRect(r, 12f, 12f, cardBgPaint)
                    cardBorderPaint.color = if (count > 0) Color.parseColor("#00E5FF") else Color.GRAY
                    canvas.drawRoundRect(r, 12f, 12f, cardBorderPaint)
                    iconCache[pt.iconAsset]?.let { ico ->
                        iconDstRect.set(px + 12f, py + 22f, px + 60f, py + 70f)
                        canvas.drawBitmap(ico, null, iconDstRect, null)
                    }
                    canvas.drawText("${pt.titleAr} (x$count)", px + 70f, py + 36f, itemTitlePaint)
                    canvas.drawText(pt.descAr, px + 70f, py + 68f, itemDescPaint)
                    px += 305f
                }
            }
            RpgTab.SETTINGS -> {
                toggleMuteRect.set(contentLeft, contentTop + 30f, contentLeft + 360f, contentTop + 95f)
                btnFillPaint.color = if (SoundEngine.isMuted) Color.parseColor("#C62828") else Color.parseColor("#2E7D32")
                canvas.drawRoundRect(toggleMuteRect, 12f, 12f, btnFillPaint)
                canvas.drawText(if (SoundEngine.isMuted) "الصوت: مغلق (اضغط للتشغيل)" else "الصوت: مفعل (اضغط للكتم)", toggleMuteRect.centerX(), toggleMuteRect.centerY() + 7f, closeTextPaint)

                toggleMinimapRect.set(contentLeft + 390f, contentTop + 30f, contentLeft + 750f, contentTop + 95f)
                btnFillPaint.color = if (env.showMinimap) Color.parseColor("#1565C0") else Color.parseColor("#37474F")
                canvas.drawRoundRect(toggleMinimapRect, 12f, 12f, btnFillPaint)
                canvas.drawText(if (env.showMinimap) "الخريطة المصغرة: ظاهرة" else "الخريطة المصغرة: مخفية", toggleMinimapRect.centerX(), toggleMinimapRect.centerY() + 7f, closeTextPaint)

                saveQuitRect.set(contentLeft, contentTop + 135f, contentLeft + 360f, contentTop + 200f)
                btnFillPaint.color = Color.parseColor("#6A1B9A")
                canvas.drawRoundRect(saveQuitRect, 12f, 12f, btnFillPaint)
                canvas.drawText("حفظ التقدم والعودة للقائمة الرئيسية", saveQuitRect.centerX(), saveQuitRect.centerY() + 7f, closeTextPaint)
            }
        }
    }

    fun drawDialogue(canvas: Canvas, screenW: Float, screenH: Float, dialogue: DialogueLine?) {
        if (dialogue == null) return
        panelRect.set(70f, screenH - 275f, screenW - 70f, screenH - 30f)
        canvas.drawRoundRect(panelRect, 20f, 20f, panelBgPaint)
        panelBorderPaint.color = Color.parseColor("#FFD700")
        canvas.drawRoundRect(panelRect, 20f, 20f, panelBorderPaint)

        headerPaint.color = Color.parseColor("#FFD700")
        canvas.drawText(dialogue.speaker, 100f, screenH - 228f, headerPaint)

        val maxTextW = (panelRect.width() - 60f).toInt().coerceAtLeast(200)
        TextHelper.drawWrappedRTL(canvas, dialogue.textAr, 100f, screenH - 208f, maxTextW, rtlTextPaint, Layout.Alignment.ALIGN_NORMAL)
        TextHelper.drawWrappedRTL(canvas, dialogue.textEn, 100f, screenH - 122f, maxTextW, rtlSubPaint, Layout.Alignment.ALIGN_NORMAL)

        itemPricePaint.color = Color.parseColor("#4FC3F7")
        canvas.drawText("اضغط للمتابعة ▶", screenW - 250f, screenH - 52f, itemPricePaint)
    }

    fun drawEndingChoiceScreen(canvas: Canvas, screenW: Float, screenH: Float) {
        canvas.drawColor(Color.argb(240, 10, 10, 22))
        centerTitlePaint.color = Color.parseColor("#FFD700")
        centerTitlePaint.textSize = 48f
        canvas.drawText("اكتملت شظايا حجر كرونو الستة!", screenW / 2f, screenH / 2f - 140f, centerTitlePaint)

        centerSubPaint.color = Color.WHITE
        centerSubPaint.textSize = 26f
        canvas.drawText("اختر مصير خط الزمن يا حارس الساعة:", screenW / 2f, screenH / 2f - 85f, centerSubPaint)

        endingFixRect.set(screenW / 2f - 360f, screenH / 2f - 20f, screenW / 2f + 360f, screenH / 2f + 65f)
        btnFillPaint.color = Color.parseColor("#0277BD")
        canvas.drawRoundRect(endingFixRect, 16f, 16f, btnFillPaint)
        centerTitlePaint.color = Color.WHITE
        centerTitlePaint.textSize = 26f
        canvas.drawText("1. إصلاح حجر كرونو واستعادة استقرار الزمن", endingFixRect.centerX(), endingFixRect.centerY() + 9f, centerTitlePaint)

        endingAcceptRect.set(screenW / 2f - 360f, screenH / 2f + 90f, screenW / 2f + 360f, screenH / 2f + 175f)
        btnFillPaint.color = Color.parseColor("#6A1B9A")
        canvas.drawRoundRect(endingAcceptRect, 16f, 16f, btnFillPaint)
        canvas.drawText("2. قبول الماضي وترك الزمن يسير بطريقته", endingAcceptRect.centerX(), endingAcceptRect.centerY() + 9f, centerTitlePaint)
    }

    fun drawTitleScreen(canvas: Canvas, screenW: Float, screenH: Float, hasSavedGame: Boolean, savedEra: EraType?) {
        canvas.drawColor(Color.parseColor("#0D1117"))

        centerTitlePaint.color = Color.parseColor("#FFD700")
        centerTitlePaint.textSize = 60f
        canvas.drawText("TIME GUARDIAN: حارس ساعة الزمن", screenW / 2f, screenH / 2f - 125f, centerTitlePaint)

        centerSubPaint.color = Color.WHITE
        centerSubPaint.textSize = 28f
        canvas.drawText("عالم مفتوح، 6 عصور متصلة، شجرة مهارات، ومهام", screenW / 2f, screenH / 2f - 60f, centerSubPaint)

        if (hasSavedGame && savedEra != null) {
            continueBtnRect.set(screenW / 2f - 280f, screenH / 2f + 5f, screenW / 2f + 280f, screenH / 2f + 85f)
            btnFillPaint.color = Color.parseColor("#2E7D32")
            canvas.drawRoundRect(continueBtnRect, 16f, 16f, btnFillPaint)
            panelBorderPaint.color = Color.parseColor("#81C784")
            canvas.drawRoundRect(continueBtnRect, 16f, 16f, panelBorderPaint)
            centerTitlePaint.color = Color.WHITE
            centerTitlePaint.textSize = 26f
            canvas.drawText("▶ متابعة (${savedEra.title})", continueBtnRect.centerX(), continueBtnRect.centerY() + 9f, centerTitlePaint)

            newGameBtnRect.set(screenW / 2f - 200f, screenH / 2f + 105f, screenW / 2f + 200f, screenH / 2f + 170f)
            btnFillPaint.color = Color.parseColor("#37474F")
            canvas.drawRoundRect(newGameBtnRect, 14f, 14f, btnFillPaint)
            centerTitlePaint.textSize = 23f
            canvas.drawText("بدء مغامرة جديدة", newGameBtnRect.centerX(), newGameBtnRect.centerY() + 8f, centerTitlePaint)
        } else {
            newGameBtnRect.set(screenW / 2f - 240f, screenH / 2f + 25f, screenW / 2f + 240f, screenH / 2f + 110f)
            btnFillPaint.color = Color.parseColor("#2E7D32")
            canvas.drawRoundRect(newGameBtnRect, 16f, 16f, btnFillPaint)
            centerTitlePaint.color = Color.WHITE
            centerTitlePaint.textSize = 32f
            canvas.drawText("▶ المس للبدء (TAP TO START)", newGameBtnRect.centerX(), newGameBtnRect.centerY() + 11f, centerTitlePaint)
        }
    }

    fun drawGameOverScreen(canvas: Canvas, screenW: Float, screenH: Float) {
        canvas.drawColor(Color.argb(225, 30, 0, 0))
        centerTitlePaint.color = Color.RED
        centerTitlePaint.textSize = 66f
        canvas.drawText("سقط حارس الزمن (GAME OVER)", screenW / 2f, screenH / 2f - 40f, centerTitlePaint)

        centerSubPaint.color = Color.WHITE
        centerSubPaint.textSize = 32f
        canvas.drawText("المس الشاشة للاستئناف من آخر نقطة حفظ", screenW / 2f, screenH / 2f + 50f, centerSubPaint)
    }

    fun drawVictoryScreen(canvas: Canvas, screenW: Float, screenH: Float, endingType: Int) {
        canvas.drawColor(Color.parseColor("#0A192F"))
        centerTitlePaint.color = Color.parseColor("#FFD700")
        centerTitlePaint.textSize = 58f
        val title = if (endingType == 1) "النهاية الأولى: استقرار الزمن الأبدي!" else "النهاية الثانية: السلام وقبول الماضي!"
        canvas.drawText(title, screenW / 2f, screenH / 2f - 70f, centerTitlePaint)

        centerSubPaint.color = Color.parseColor("#64FFDA")
        centerSubPaint.textSize = 34f
        canvas.drawText("TIME GUARDIAN – CONQUERED", screenW / 2f, screenH / 2f, centerSubPaint)

        centerSubPaint.color = Color.WHITE
        centerSubPaint.textSize = 24f
        val sub = if (endingType == 1) "تم ترميم حجر كرونو وعاد التوازن لجميع العصور الستة!" else "تركت الزمن يتدفق بحرية وتصالح حارس الساعة مع ذكرياته."
        canvas.drawText(sub, screenW / 2f, screenH / 2f + 55f, centerSubPaint)

        centerTitlePaint.color = Color.YELLOW
        centerTitlePaint.textSize = 28f
        canvas.drawText("المس الشاشة للعب مجدداً من البداية", screenW / 2f, screenH / 2f + 135f, centerTitlePaint)
    }
}
