package com.clashangprpr.app.systems

import com.clashangprpr.app.model.EraType

data class SkillNode(
    val id: String,
    val titleAr: String,
    val descAr: String,
    var level: Int = 0,
    val maxLevel: Int = 5
)

data class Quest(
    val id: String,
    val titleAr: String,
    val descAr: String,
    val targetCount: Int,
    var currentCount: Int = 0,
    val rewardGold: Int,
    val rewardXp: Int,
    var isCompleted: Boolean = false
)

data class Achievement(
    val id: String,
    val titleAr: String,
    var isUnlocked: Boolean = false
)

enum class PotionType(val id: String, val titleAr: String, val descAr: String, val iconAsset: String, val price: Int) {
    HEALTH("potion_health", "جرعة شفاء", "تستعيد +85 من الصحة فوراً", "icons/potion_health.png", 25),
    SPEED("potion_speed", "جرعة السرعة", "تزيد سرعة الحركة 40% لمدة 12 ثانية", "icons/potion_2.png", 35),
    STRENGTH("potion_strength", "جرعة القوة", "تضاعف ضرر الهجوم 40% لمدة 12 ثانية", "icons/potion_3.png", 45),
    MANA("potion_mana", "جرعة المانا", "تملأ طاقة السحر بالكامل فوراً", "icons/potion_mana.png", 30)
}

class ProgressionSystem {
    var level: Int = 1
    var xp: Int = 0
    var xpToNext: Int = 100
    var skillPoints: Int = 1

    var weaponUpgradeLevel: Int = 0

    // Buff timers from Alchemy potions
    var speedBuffTimer: Float = 0f
    var strengthBuffTimer: Float = 0f

    // Potion inventory counts
    var speedPotions: Int = 1
    var strengthPotions: Int = 1
    var manaPotions: Int = 1
    var invisPotions: Int = 0
    var hasClockHand: Boolean = false
    var sageRescueCount: Int = 0

    val skills = listOf(
        SkillNode("might", "قوة السيف", "+12% ضرر لكل مستوى", 0, 5),
        SkillNode("vitality", "صلابة الفارس", "+30 صحة قصوى لكل مستوى", 0, 5),
        SkillNode("agility", "رشاقة الزمن", "+8% سرعة حركة لكل مستوى", 0, 5),
        SkillNode("crit", "ضربة قاضية", "+5% فرصة ضربة حرجة لكل مستوى", 0, 5),
        SkillNode("arcane", "طاقة كرونو", "+20 مانا قصوى لكل مستوى", 0, 5)
    )

    val quests = mutableListOf(
        Quest("q_first_blood", "التطهر الأول", "اهزم 5 أعداء في أي عصر", 5, 0, 60, 80),
        Quest("q_miner", "جامع الموارد", "اجمع 6 حجارة أو أخشاب أو حديد", 6, 0, 50, 60),
        Quest("q_era_1", "شظية القلعة", "استعد الشظية الأولى من قلعة القرون الوسطى", 1, 0, 120, 150),
        Quest("q_open_soldiers", "تطهير المعسكرات", "اهزم 15 جندياً في القارة المفتوحة", 15, 0, 160, 200),
        Quest("q_open_alchemist", "رحلة الكيمياء", "زر تاجر الكيمياء البعيد واشترِ مشروب الإخفاء", 1, 0, 100, 120),
        Quest("q_open_clockhand", "عقرب الساعة المفقود", "اهزم زعيم الحصن في القارة المفتوحة واستعد عقرب الساعة", 1, 0, 350, 400),
        Quest("q_era_4", "كنز القراصنة", "أكمل عصر جزيرة القراصنة", 1, 0, 180, 220),
        Quest("q_void", "حارس الساعة الحقيقي", "اهزم سيد الفراغ في العصر السادس", 1, 0, 400, 500)
    )

    val achievements = mutableListOf(
        Achievement("ach_first_kill", "أول انتصار"),
        Achievement("ach_craft", "صانع محترف"),
        Achievement("ach_level3", "فارس متمرس (المستوى 3)"),
        Achievement("ach_hacker", "مخترق الأنظمة (العصر 5)"),
        Achievement("ach_guardian", "حارس الزمن الأسطوري")
    )

    fun addXp(amount: Int): Boolean {
        xp += amount
        var leveledUp = false
        while (xp >= xpToNext) {
            xp -= xpToNext
            level++
            skillPoints++
            xpToNext = (xpToNext * 1.35f).toInt()
            leveledUp = true
            if (level >= 3) unlockAchievement("ach_level3")
        }
        return leveledUp
    }

    fun upgradeSkill(skillId: String): Boolean {
        if (skillPoints <= 0) return false
        val node = skills.firstOrNull { it.id == skillId } ?: return false
        if (node.level >= node.maxLevel) return false
        node.level++
        skillPoints--
        return true
    }

    fun getDamageMultiplier(): Float {
        val mightLvl = skills[0].level
        val buff = if (strengthBuffTimer > 0f) 0.40f else 0f
        return 1.0f + (mightLvl * 0.12f) + (weaponUpgradeLevel * 0.15f) + buff
    }

    fun getBonusMaxHp(): Int = skills[1].level * 30
    fun getSpeedMultiplier(): Float {
        val agiLvl = skills[2].level
        val buff = if (speedBuffTimer > 0f) 0.40f else 0f
        return 1.0f + (agiLvl * 0.08f) + buff
    }
    fun getCritChance(): Float = 0.18f + (skills[3].level * 0.05f)
    fun getBonusMaxMana(): Float = skills[4].level * 20f

    fun updateBuffs(deltaTime: Float) {
        if (speedBuffTimer > 0f) speedBuffTimer = (speedBuffTimer - deltaTime).coerceAtLeast(0f)
        if (strengthBuffTimer > 0f) strengthBuffTimer = (strengthBuffTimer - deltaTime).coerceAtLeast(0f)
    }

    fun advanceQuest(questId: String, amount: Int = 1): Quest? {
        val q = quests.firstOrNull { it.id == questId } ?: return null
        if (q.isCompleted) return null
        q.currentCount = (q.currentCount + amount).coerceAtMost(q.targetCount)
        if (q.currentCount >= q.targetCount) {
            q.isCompleted = true
            return q
        }
        return null
    }

    fun onEraCompleted(era: EraType): List<Quest> {
        val completed = mutableListOf<Quest>()
        if (era == EraType.ERA_1_MEDIEVAL) advanceQuest("q_era_1")?.let { completed.add(it) }
        if (era == EraType.ERA_4_ISLAND) advanceQuest("q_era_4")?.let { completed.add(it) }
        if (era == EraType.ERA_6_VOID) {
            advanceQuest("q_void")?.let { completed.add(it) }
            unlockAchievement("ach_guardian")
        }
        return completed
    }

    fun unlockAchievement(id: String): Achievement? {
        val a = achievements.firstOrNull { it.id == id } ?: return null
        if (!a.isUnlocked) {
            a.isUnlocked = true
            return a
        }
        return null
    }
}
