package com.clashangprpr.app.model

import android.graphics.Color

data class Weapon(
    val id: String,
    val nameAr: String,
    val damage: Int,
    val reach: Float,
    val cooldown: Float,
    val iconAsset: String,
    val color: Int
)

data class Armor(
    val id: String,
    val nameAr: String,
    val hpBonus: Int,
    val reduction: Float
)

/** Placeable Minecraft & Fortnite-style block definitions for the Open World grid. */
enum class BlockType(
    val code: Int,
    val nameAr: String,
    val descAr: String,
    val tileAsset: String,
    val iconAsset: String,
    val solid: Boolean,
    val bridge: Boolean = false
) {
    WOOD_WALL(1, "بلوك ألواح خشبية", "جدار خشبي سريع للبناء القتالي والمنازل.", "tiles/block_wood.png", "icons/block_wood.png", true),
    STONE_BRICK(2, "بلوك طوب حجري", "لا ينكسر بالسيف أو المدفع (يتكسر بالمعول فقط). سمك 2 حجر يصد الزومبي المفجر!", "tiles/block_stone_brick.png", "icons/block_stone_brick.png", true),
    WOOD_BRIDGE(3, "أرضية / جسر خشبي", "أرضية خشبية يمكن بناؤها فوق الأرض أو فوق الماء كجسر!", "tiles/block_bridge.png", "icons/block_bridge.png", false, true),
    DOOR_CLOSED(4, "باب منزل خشبي", "باب يفتح ويغلق بالضغط عليه، ويمنع دخول الزومبي.", "tiles/door.png", "tiles/door.png", false),
    FENCE(6, "سياج دفاعي", "سياج خشبي يحمي مزرعتك ويسمح بمرور السهام والسحر.", "tiles/block_fence.png", "icons/block_fence.png", true),
    TORCH(7, "شعلة إضاءة", "تضيء المكان حولها في الليل وداخل الكهوف.", "environment/candlestick.png", "environment/torch.png", false),
    GLASS_BLOCK(8, "بلوك زجاج بلوري", "جدار زجاجي شفاف للنوافذ والقلاع الحديثة.", "tiles/block_glass.png", "icons/block_glass.png", true),
    TNT_BLOCK(9, "بلوك متفجرات TNT", "ينفجر بقوة هائلة عند ضربه ويكسر درع الزعيم العملاق!", "tiles/block_tnt.png", "icons/block_tnt.png", true),
    FIRING_WINDOW(10, "جدار نافذة قتالية (فورتنايت)", "جدار حجري بفتحة قنص: يمنع الزومبي ومقذوفاتهم ويسمح بمرور سحرك وضرباتك!", "tiles/block_window.png", "icons/block_window.png", true),
    LAUNCH_PAD(11, "منصة القفز الهوائي", "تقذفك في الهواء للتحليق فوق الجدران والزومبي مع ضربة هوائية ساحقة!", "tiles/block_launchpad.png", "icons/block_launchpad.png", false),
    IRON_GATE(12, "بوابة قلعة حديدية مصفحة", "بوابة حديدية تفتح للفارس تلقائياً ولا يستطيع الزومبي المفجر كسرها!", "tiles/iron_gate.png", "icons/iron_gate.png", false),
    OBSIDIAN_WALL(13, "بلوك حجر السبج المدرع", "أقوى جدار في اللعبة: مضاد 100% للزومبي المفجر والانفجارات حتى بسمك 1 بلوك!", "tiles/block_obsidian.png", "icons/block_obsidian.png", true),
    ROYAL_FLOOR(14, "أرضية رخام القلعة الملكية", "أرضية فاخرة تمنحك +25% سرعة حركة أثناء السير عليها وتمنع نمو الأعشاب.", "tiles/block_royal_floor.png", "icons/block_royal_floor.png", false, true),
    SPIKE_TRAP(15, "فخ الأشواك الفولاذية", "فخ أرضي يمزق الزومبي الذين يمرون فوقه بضرر مستمر ونزيف!", "tiles/block_spikes.png", "icons/block_spikes.png", false),
    BARRICADE_WIRE(16, "سلك شائك دفاعي", "يبطئ حركة الزومبي والوحوش بنسبة 65% حول أسوار قلعتك ومزارعك!", "tiles/block_wire.png", "icons/block_wire.png", false),
    HEALING_BEACON(17, "منارة الشفاء البلورية", "بلورة مقدسة تشفي الفارس والغولم الحديدي تلقائياً كل ثانية في محيطها!", "tiles/block_beacon.png", "icons/block_beacon.png", false);

    companion object {
        fun byCode(c: Int): BlockType? = values().firstOrNull { it.code == c }
    }
}

enum class SpellType(
    val id: String,
    val nameAr: String,
    val descAr: String,
    val manaCost: Float,
    val cooldown: Float,
    val iconAsset: String,
    val color: Int
) {
    ARCANE_BOLT("bolt", "قذيفة الزمن", "تطلق كرة طاقة سريعة تخترق النوافذ القتالية.", 18f, 0.36f, "ui/ico/cross.png", Color.parseColor("#00E5FF")),
    FIRE_NOVA("nova", "عاصفة اللهب", "تطلق 8 كرات نارية في كل الاتجاهات تحرق حشود الزومبي.", 30f, 1.25f, "icons/pot17.png", Color.parseColor("#FF7043")),
    FROST_HEAL("heal", "نبض النور والدرع", "تستعيد +45 صحة وتمنح درع حماية سحري مؤقت.", 36f, 3.0f, "icons/pot_shield.png", Color.parseColor("#69F0AE")),
    VOID_LASER("laser", "شعاع الفراغ الخارق", "ليزر كوني خارق يخترق صفوف الزومبي بضرر هائل.", 26f, 0.65f, "icons/pot_vision.png", Color.parseColor("#E040FB"))
}

enum class ItemType { WEAPON, ARMOR, CONSUMABLE, UPGRADE }

data class ShopItem(
    val id: String,
    val titleAr: String,
    val descAr: String,
    val price: Int,
    val type: ItemType,
    val iconAsset: String,
    val tint: Int? = null,
    val maxCount: Int = 1
)

class Quest(
    val id: String,
    val titleAr: String,
    val descAr: String,
    val targetCount: Int,
    val rewardGold: Int,
    val rewardMatIdx: Int,
    val rewardMatCount: Int,
    val iconAsset: String
) {
    var progress: Int = 0
    var claimed: Boolean = false
    val isComplete: Boolean get() = progress >= targetCount
}

object QuestBoard {
    fun createDefaultQuests(): List<Quest> = listOf(
        Quest("q_build", "مهندس القلاع والبيوت", "ابنِ أو عدّل 15 جداراً أو نافذة قتالية أو بيتاً في العالم المفتوح.", 15, 160, 0, 3, "icons/block_stone_brick.png"),
        Quest("q_farm", "المزارع الخبير وزراعة الشجر", "ازرع أو احصد 8 من المحاصيل الزراعية أو شتلات الأشجار بيدك.", 8, 150, 0, 3, "icons/wheat.png"),
        Quest("q_powder", "تجهيز البارود والمدافع", "اصنع البارود في الكرافتينج 4×4 أو اجمعه واملأ مدافع قلعتك 3 مرات.", 3, 180, 2, 3, "icons/gunpowder.png"),
        Quest("q_zombie", "حرب الزومبي والزومبي المفجر", "اقضِ على 20 من الزومبي الليلي والزومبي المفجر لحماية مزارعك وبيتك.", 20, 220, 2, 4, "icons/monster_1.png"),
        Quest("q_parry", "محترف الصد والمهارة", "نفّذ 6 صدات مثالية (Parry) أو تعديلات نوافذ قتالية أثناء المعارك.", 6, 180, 4, 3, "ui/ico/shield.png"),
        Quest("q_wool", "جزّ الخراف وجمع الصوف", "اجمع 5 قطع صوف من الخراف لصناعة السرير والمراكب.", 5, 100, 0, 2, "icons/wool.png"),
        Quest("q_cave", "قاهر الكهوف والبارود الخام", "انزل للطابق الثالث في الكهوف واجمع البارود واهزم زعماء الأعماق.", 3, 250, 1, 4, "environment/crystal_green.png"),
        Quest("q_pirate", "غارة حصون وجزر القراصنة", "أبحر بالمركب إلى الجزر واهزم 8 من القراصنة أو حراس الحصون.", 8, 240, 3, 4, "icons/pirate_flag.png"),
        Quest("q_cook", "خبير الفرن والصهر", "اطبخ أو اصهر 6 مرات في الفرن التفاعلي.", 6, 130, 0, 3, "icons/furnace.png"),
        Quest("q_trade", "سيد التجارة والمقايضة", "تبادل مع القرويين لرفع مستواهم إلى المستوى 5 والحصول على ختم الحكيم.", 5, 200, 5, 3, "icons/goblet.png"),
        Quest("q_seals", "أختام الهاوية السبعة (التختيم)", "اجمع أختام الهاوية الـ 7 لتفعيل مذبح الزعيم الأسطوري العملاق.", 7, 1000, 5, 6, "icons/abyss_seal.png")
    )
}

object GameCatalogue {
    val WEAPON_BASIC = Weapon("sword_basic", "سيف الفجر", 35, 84f, 0.28f, "icons/item_1.png", Color.WHITE)
    val WEAPON_DAGGER = Weapon("dagger_silver", "خنجر الفضة السريع", 54, 82f, 0.16f, "icons/dagger_silver.png", Color.parseColor("#B0BEC5"))
    val WEAPON_FIRE = Weapon("sword_fire", "نصل النار الملتهب", 68, 94f, 0.25f, "icons/item_2.png", Color.parseColor("#FF7043"))
    val WEAPON_PIRATE = Weapon("sword_pirate", "سيف القبطان الذهبي", 88, 100f, 0.24f, "icons/sword_gold.png", Color.parseColor("#FFD54F"))
    val WEAPON_CHRONO = Weapon("spear_chrono", "رمح كرونو الأسطوري", 112, 124f, 0.30f, "icons/item_5.png", Color.parseColor("#00E5FF"))
    val WEAPON_BOW = Weapon("bow_fire", "قوس اللهب القناص 🏹", 82, 96f, 0.26f, "icons/weapon_bow.png", Color.parseColor("#FF9800"))
    val WEAPON_HAMMER = Weapon("hammer_thunder", "مطرقة الرعد العملاقة ⚡", 145, 122f, 0.34f, "icons/weapon_hammer.png", Color.parseColor("#00E5FF"))
    val WEAPON_MUSKET = Weapon("musket_powder", "بندقية البارود القناصة 💥", 195, 105f, 0.38f, "icons/weapon_musket.png", Color.parseColor("#FFD54F"))
    val WEAPON_GOD = Weapon("godblade", "نصل الأبعاد الكوني", 175, 114f, 0.25f, "icons/blade.png", Color.parseColor("#E040FB"))
    val WEAPON_ABYSS = Weapon("abyss_breaker", "سيف قاهر الهاوية الخرافي", 265, 134f, 0.20f, "icons/sword_gold.png", Color.parseColor("#FF1744"))

    val weapons = listOf(WEAPON_BASIC, WEAPON_DAGGER, WEAPON_FIRE, WEAPON_BOW, WEAPON_PIRATE, WEAPON_CHRONO, WEAPON_HAMMER, WEAPON_MUSKET, WEAPON_GOD, WEAPON_ABYSS)

    val ARMOR_ROYAL = Armor("armor_royal", "درع الفارس الملكي", 110, 0.30f)
    val ARMOR_NANO = Armor("armor_nano", "درع النانو المتطور", 170, 0.44f)
    val ARMOR_TITAN = Armor("armor_titan", "درع التيتانيوم الخالد", 290, 0.58f)
    val armors = listOf(ARMOR_ROYAL, ARMOR_NANO, ARMOR_TITAN)

    fun weaponById(id: String): Weapon? = weapons.firstOrNull { it.id == id }
    fun armorById(id: String): Armor? = armors.firstOrNull { it.id == id }

    val shopItems = listOf(
        ShopItem("dagger_silver", "خنجر الفضة السريع", "ضربات فائقة السرعة لبناء الكومبو (+فرصة سرقة أعلى) ضرر 54.", 85, ItemType.WEAPON, "icons/dagger_silver.png"),
        ShopItem("sword_fire", "نصل النار الملتهب", "سيف حارق لحشود الزومبي. ضرر 68 ومدى أطول.", 130, ItemType.WEAPON, "icons/item_2.png"),
        ShopItem("sword_pirate", "سيف القبطان الذهبي", "سيف القراصنة الملكي: ضرر 88 وسرعة عالية.", 220, ItemType.WEAPON, "icons/sword_gold.png"),
        ShopItem("bow_fire", "قوس اللهب القناص 🏹", "يطلق سهاماً نارية بعيدة المدى تخترق النوافذ القتالية بلا استهلاك مانا!", 190, ItemType.WEAPON, "icons/weapon_bow.png"),
        ShopItem("spear_chrono", "رمح كرونو الأسطوري", "مدى طويل للطعن خلف النوافذ القتالية: ضرر 112.", 340, ItemType.WEAPON, "icons/item_5.png"),
        ShopItem("hammer_thunder", "مطرقة الرعد العملاقة ⚡", "تضرب الأرض بصاعقة رعدية تشل وتصعق جميع الزومبي المحيطين بك!", 420, ItemType.WEAPON, "icons/weapon_hammer.png"),
        ShopItem("musket_powder", "بندقية البارود القناصة 💥", "تطلق رصاصات بارود خارقة للدروع بضرر 195 عبر النوافذ والأسوار!", 490, ItemType.WEAPON, "icons/weapon_musket.png"),
        ShopItem("golem_pack", "الغولم الحديدي الحارس 🤖", "حارس آلي مدرع يحرس قلعتك ومزارعك ويسحق الزومبي المفجر ليلاً!", 260, ItemType.CONSUMABLE, "icons/iron_golem.png"),
        ShopItem("watering_can", "دلو الري المائي السريع 💧", "يروي المحاصيل والأشجار في مساحة 3×3 لتنمو وتثمر فوراً!", 75, ItemType.CONSUMABLE, "icons/watering_can.png"),
        ShopItem("armor_royal", "درع الفارس الملكي", "+110 صحة قصوى ويقلل الضرر 30%.", 180, ItemType.ARMOR, "ui/ico/shield.png", Color.parseColor("#FFD54F")),
        ShopItem("gunpowder_pack", "برميل بارود المدافع (x10)", "بارود نقي لتعبئة وتشغيل المدافع على أسوار قلعتك ضد الزومبي.", 38, ItemType.CONSUMABLE, "icons/gunpowder.png"),
        ShopItem("seeds_pack", "حزمة بذور وشتلات شجر (x6)", "4 بذور محاصيل قمح + 2 شتلة شجر لزراعتها وحصادها بيدك.", 28, ItemType.CONSUMABLE, "icons/seeds.png"),
        ShopItem("potion_heal", "جرعتا شفاء", "تستعيد 85 صحة. المخزون يتجدد يومياً.", 35, ItemType.CONSUMABLE, "icons/pot1.png"),
        ShopItem("pot_speed", "إكسير السرعة والرؤية", "يمنح سرعة حركة مضاعفة ورؤية في ظلام الكهوف لمدة 45 ثانية.", 45, ItemType.CONSUMABLE, "icons/pot_speed.png"),
        ShopItem("stew_pack", "حساء الطاقة الساخن (x2)", "يشبع الجوع +75 ويستعيد +35 صحة فوراً.", 40, ItemType.CONSUMABLE, "icons/stew.png"),
        ShopItem("bombs", "قنبلتان متفجرتان", "انفجار يدمر الأخشاب ويكسر درع الزعيم العملاق.", 50, ItemType.CONSUMABLE, "icons/bomb.png"),
        ShopItem("key_gold", "مفتاح الصناديق الذهبي", "يفتح صناديق الحصون والكهوف الذهبية المقفلة.", 65, ItemType.CONSUMABLE, "icons/key_gold.png"),
        ShopItem("treasure_map", "خريطة كنز مفقود", "تكشف موقع كنز مدفون جديد على البوصلة.", 55, ItemType.CONSUMABLE, "icons/map.png"),
        ShopItem("up_hp", "قلب الحياة الأبدي", "+35 صحة قصوى دائمة للاستعداد للزعيم العملاق.", 140, ItemType.UPGRADE, "icons/pot33.png", null, 8),
        ShopItem("up_mana", "حجر الطاقة السحرية", "+30 طاقة سحر قصوى للقتال المهاري السريع.", 120, ItemType.UPGRADE, "icons/pot10.png", null, 8)
    )
}

/** One special material per era; needed for strong recipes, upgrades, and Abyssal Seals. */
object Mats {
    val names = arrayOf("صلب الحصن", "بلورة الكهف", "خردة الزومبي", "لؤلؤ القراصنة", "رقاقة سايبر", "شظية الفراغ")
    val paths = arrayOf(
        "icons/iron.png", "environment/crystal_green.png", "icons/stone.png",
        "environment/crystal_white.png", "environment/crystal_white.png", "environment/crystal_white.png"
    )
    val mul = intArrayOf(
        0xFFFFB74D.toInt(), 0xFFFFFFFF.toInt(), 0xFFC8A070.toInt(),
        0xFFFFC0E0.toInt(), 0xFF50E0FF.toInt(), 0xFFD070FF.toInt()
    )
}
