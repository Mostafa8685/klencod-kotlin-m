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
    HEALTH("potion_health", "جرعة شفاء", "تستعيد +85 من الصحة فوراً", "icons/potion_health.png", 30),
    SPEED("potion_speed", "جرعة السرعة", "تزيد سرعة الحركة 40% لمدة 12 ثانية", "icons/potion_2.png", 45),
    STRENGTH("potion_strength", "جرعة القوة", "تضاعف ضرر الهجوم 40% لمدة 12 ثانية", "icons/potion_3.png", 55),
    MANA("potion_mana", "جرعة المانا", "تملأ طاقة السحر بالكامل فوراً", "icons/potion_mana.png", 35)
}

class ProgressionSystem {
    var level: Int = 1
    var xp: Int = 0
    var xpToNext: Int = 150
    var skillPoints: Int = 1

    var weaponUpgradeLevel: Int = 0
    var difficultyTier: Int = 1 // 1=عادي, 2=صعب, 3=كابوس

    // Survival: Hunger, Food, Seeds, Rare Ores, Ammo
    var hunger: Float = 100f
    val maxHunger: Float = 100f
    var food: Int = 3
    var seeds: Int = 4
    var crystal: Int = 0
    var mythril: Int = 0
    var ammo: Int = 24

    // Buff timers
    var speedBuffTimer: Float = 0f
    var strengthBuffTimer: Float = 0f

    // Potions & Story Keys
    var speedPotions: Int = 1
    var strengthPotions: Int = 1
    var manaPotions: Int = 1
    var invisPotions: Int = 0
    var hasClockHand: Boolean = false
    var defeatedBossesMask: Int = 0
    var sageRescueCount: Int = 0

    val skills = listOf(
        SkillNode("might", "قوة السيف", "+12% ضرر لكل مستوى", 0, 10),
        SkillNode("vitality", "صلابة الفارس", "+35 صحة قصوى لكل مستوى", 0, 10),
        SkillNode("agility", "رشاقة الزمن", "+8% سرعة حركة لكل مستوى", 0, 8),
        SkillNode("crit", "ضربة قاضية", "+5% فرصة ضربة حرجة لكل مستوى", 0, 8),
        SkillNode("arcane", "طاقة كرونو", "+25 مانا قصوى لكل مستوى", 0, 8)
    )

    val quests = mutableListOf(
        Quest("q_first_blood", "التطهر الأول", "اهزم 10 أعداء في أي إقليم", 10, 0, 80, 120),
        Quest("q_miner", "منقّب المعادن", "استخرج 15 حجراً أو معدناً بالفأس", 15, 0, 100, 150),
        Quest("q_farmer", "مزارع القرية", "احصد 4 محاصيل من الحقل لإطعام نفسك", 4, 0, 90, 110),
        Quest("q_era_1", "شظية القلعة", "اهزم فارس القلعة الأول لدخول العالم المفتوح", 1, 0, 150, 200),
        Quest("q_open_soldiers", "تطهير المعسكرات", "اهزم 30 من جنود العصابات والوحوش", 30, 0, 250, 350),
        Quest("q_open_alchemist", "رحلة الكيمياء", "زر تاجر الكيمياء البعيد واشترِ مشروب الإخفاء", 1, 0, 120, 160),
        Quest("q_prison", "كسر القيود", "سرقة خزينة السجن المحصن وهزيمة الحراس", 1, 0, 300, 400),
        Quest("q_lodge", "أسرار المحفل", "اخترق المحفل السري الغامض", 1, 0, 400, 500),
        Quest("q_boss_cave", "سيد الكهوف", "اهزم زعيم إقليم الكهوف في العالم المفتوح", 1, 0, 350, 450),
        Quest("q_boss_pirate", "قبطان البحار", "اهزم زعيم إقليم القراصنة في العالم المفتوح", 1, 0, 400, 500),
        Quest("q_open_clockhand", "عقرب الساعة المفقود", "اهزم زعيم الحصن الأعظم لاستعادة عقرب الساعة", 1, 0, 600, 800),
        Quest("q_void", "حارس الساعة الحقيقي", "اهزم سيد الفراغ في البعد السابع", 1, 0, 1000, 1500)
    )

    val achievements = mutableListOf(
        Achievement("ach_first_kill", "أول انتصار"),
        Achievement("ach_craft", "صانع محترف"),
        Achievement("ach_level5", "فارس متمرس (المستوى 5)"),
        Achievement("ach_farmer", "مزارع البقاء"),
        Achievement("ach_hacker", "مخترق الأنظمة"),
        Achievement("ach_guardian", "حارس الزمن الأسطوري")
    )

    fun addXp(amount: Int): Boolean {
        xp += amount
        var leveledUp = false
        while (xp >= xpToNext) {
            xp -= xpToNext
            level++
            skillPoints++
            xpToNext = (xpToNext * 1.4f).toInt()
            leveledUp = true
            if (level >= 5) unlockAchievement("ach_level5")
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

    fun getBonusMaxHp(): Int = skills[1].level * 35
    fun getSpeedMultiplier(): Float {
        val agiLvl = skills[2].level
        val buff = if (speedBuffTimer > 0f) 0.40f else 0f
        val hungerPenalty = if (hunger <= 15f) 0.80f else 1.0f
        return (1.0f + (agiLvl * 0.08f) + buff) * hungerPenalty
    }
    fun getCritChance(): Float = 0.18f + (skills[3].level * 0.05f)
    fun getBonusMaxMana(): Float = skills[4].level * 25f

    fun updateBuffs(deltaTime: Float) {
        if (speedBuffTimer > 0f) speedBuffTimer = (speedBuffTimer - deltaTime).coerceAtLeast(0f)
        if (strengthBuffTimer > 0f) strengthBuffTimer = (strengthBuffTimer - deltaTime).coerceAtLeast(0f)
        // Hunger slowly drains over time
        hunger = (hunger - 0.55f * deltaTime).coerceAtLeast(0f)
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
