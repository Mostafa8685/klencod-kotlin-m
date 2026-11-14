package com.clashangprpr.app.model

import android.graphics.Color

/**
 * 4x4 Crafting Table Recipe with an authentic 16-slot (4x4) blueprint pattern!
 * Grid codes in `pattern4x4` (length 16):
 * '.' = empty, 'W' = Wood, 'S' = Stone, 'I' = Iron, 'L' = Wool, 'P' = Gunpowder, 'G' = Gold/Gem, 'M' = Era Shard
 */
class CraftRecipe(
    val id: String,
    val titleAr: String,
    val descAr: String,
    val resultType: String,
    val iconAsset: String,
    val tint: Int? = null,
    val repeatable: Boolean = false,
    val wood: Int = 0,
    val stone: Int = 0,
    val iron: Int = 0,
    val gold: Int = 0,
    val mats: IntArray = IntArray(6),
    val wool: Int = 0,
    val gunpowder: Int = 0,
    val pattern4x4: String = "WWWW............",
    val category: Int = 0
)

/** Resolved cost of a recipe (upgrades get more expensive with every level up to +10). */
class Cost(
    val wood: Int,
    val stone: Int,
    val iron: Int,
    val gold: Int,
    val mats: IntArray,
    val wool: Int = 0,
    val gunpowder: Int = 0
)

object CraftingRegistry {
    const val MAX_UPG = 10

    val recipes = listOf(
        CraftRecipe(
            "craft_iron_golem", "صناعة الغولم الحديدي الحارس 🤖",
            "حارس آلي عملاق (650 صحة) يجوب قلعتك ومزرعتك ويفجر الزومبي الليلي قبل وصولهم لبيتك!",
            "IRON_GOLEM", "icons/iron_golem.png", null, true, 2, 4, 6, 40, IntArray(6), 0, 2,
            ".II.IIIIIIII.I.I", 2
        ),
        CraftRecipe(
            "craft_bow_fire", "قوس اللهب القناص 🏹",
            "قوس احترافي يطلق سهاماً نارية بعيدة المدى تخترق النوافذ القتالية وأسوار القلعة!",
            "WEAPON_BOW", "icons/weapon_bow.png", null, false, 6, 0, 2, 60, IntArray(6), 2, 0,
            ".WL..W..L.W..WL.", 1
        ),
        CraftRecipe(
            "craft_musket_powder", "بندقية البارود القناصة 💥",
            "بندقية قنص ثقيلة تستخدم البارود لإطلاق رصاصات خارقة بضرر 195 ومدى هائل!",
            "WEAPON_MUSKET", "icons/weapon_musket.png", null, false, 4, 2, 6, 120, IntArray(6), 0, 5,
            "IIIIPPPPWWWW....", 1
        ),
        CraftRecipe(
            "craft_hammer_thunder", "مطرقة الرعد العملاقة ⚡",
            "مطرقة حرب تطلق موجة صاعقة رعدية تشل وتدمر حشود الزومبي حولك في كل ضربة!",
            "WEAPON_HAMMER", "icons/weapon_hammer.png", null, false, 4, 6, 6, 110, IntArray(6), 0, 0,
            "IIIISSSS.W...W..", 1
        ),
        CraftRecipe(
            "craft_blk_obsidian", "بلوك حجر السبج المدرع (x6)",
            "جدار بركاني أسود لا ينكسر أبداً بأي انفجار أو زومبي مفجر حتى بسمك 1 بلوك!",
            "BLK_OBSIDIAN", "icons/block_obsidian.png", null, true, 0, 4, 2, 0, IntArray(6), 0, 1,
            "SSSSIIIISSSS....", 0
        ),
        CraftRecipe(
            "craft_blk_spikes", "فخ الأشواك الفولاذية (x6)",
            "يوضع حول أسوار قلعتك ومزارعك ليمزق الزومبي الذين يدوسون عليه بضرر مستمر!",
            "BLK_SPIKES", "icons/block_spikes.png", null, true, 1, 2, 2, 0, IntArray(6), 0, 0,
            "I.I.I.I.SSSS....", 0
        ),
        CraftRecipe(
            "craft_blk_wire", "سلك شائك دفاعي لإبطاء الزومبي (x6)",
            "يبطئ سرعة الزومبي والزومبي المفجر بنسبة 65% ليتمكن المدفع والغولم من سحقهم!",
            "BLK_WIRE", "icons/block_wire.png", null, true, 2, 0, 2, 0, IntArray(6), 0, 0,
            "I.I.W.W.I.I.....", 0
        ),
        CraftRecipe(
            "craft_blk_beacon", "منارة الشفاء البلورية (x2)",
            "توضع داخل قلعتك لتشفي الفارس والغولم الحديدي تلقائياً كل ثانية!",
            "BLK_BEACON", "icons/block_beacon.png", null, true, 2, 3, 2, 35, IntArray(6), 0, 0,
            ".GG.GIIGSSSS....", 0
        ),
        CraftRecipe(
            "craft_blk_royal", "أرضية رخام القلعة الملكية (x8)",
            "تمنحك +25% سرعة حركة إضافية عند المشي عليها داخل قلعتك أو بيتك!",
            "BLK_ROYAL", "icons/block_royal_floor.png", null, true, 0, 3, 0, 10, IntArray(6), 0, 0,
            "SSSSGGGGSSSS....", 0
        ),
        CraftRecipe(
            "craft_watering_can", "دلو الري المائي لتسريع الزراعة 💧",
            "يروي المزارع والأشجار في مساحة 3×3 فتنضج المحاصيل فوراً وتضاعف الحصاد!",
            "WATERING_CAN", "icons/watering_can.png", null, false, 1, 0, 3, 20, IntArray(6), 0, 0,
            "I..I.II.........", 3
        ),
        CraftRecipe(
            "craft_enchant_lifesteal", "ترصيع السلاح: جوهرة مصاص الدماء 🩸",
            "يمنح كل أسلحتك خاصية امتصاص الصحة (+18% شفاء فوري من كل ضربة في المعركة)!",
            "ENCH_LIFESTEAL", "icons/pot33.png", Color.parseColor("#FF1744"), true, 0, 2, 3, 95, IntArray(6), 0, 2,
            ".GG.GIIIG.GG....", 3
        ),
        CraftRecipe(
            "craft_enchant_lightning", "ترصيع السلاح: جوهرة الصاعقة المتسلسلة ⚡",
            "يجعل ضربات سلاحك تطلق برقاً متسلسلاً يضرب 3 من الزومبي المجاورين تلقائياً!",
            "ENCH_LIGHTNING", "icons/pot_vision.png", Color.parseColor("#00E5FF"), true, 0, 2, 3, 95, IntArray(6), 0, 2,
            "P..P.II..GG.....", 3
        ),
        CraftRecipe(
            "craft_helmet", "خوذة الفارس المدرعة (4 مستويات)",
            "تحمي الرأس وتمنح +35 صحة و+4% دفاع لكل مستوى (شبكة 4×4 بستايل ماين كرافت).",
            "HELMET_UPG", "icons/armor_helmet.png", null, true, 0, 2, 3, 40, IntArray(6), 0, 0,
            "IIIII..I........"
        ),
        CraftRecipe(
            "craft_chestplate", "درع الصدر الملكي (4 مستويات)",
            "درع الصدر الثقيل: يمنح +50 صحة و+6% حماية ضد الزومبي والزعيم لكل مستوى.",
            "CHEST_UPG", "icons/armor_chest.png", null, true, 0, 3, 5, 65, IntArray(6), 0, 0,
            "I..IIIIIIIIIIIII"
        ),
        CraftRecipe(
            "craft_leggings", "بنطال الدرع الحديدي (4 مستويات)",
            "يحمي الساقين ويمنح +40 صحة و+5% مقاومة لانفجارات الزومبي المفجر لكل مستوى.",
            "LEGS_UPG", "icons/armor_legs.png", null, true, 0, 2, 4, 50, IntArray(6), 0, 0,
            "IIIII..II..II..I"
        ),
        CraftRecipe(
            "craft_boots", "حذاء السرعة والقتال (4 مستويات)",
            "يمنح +25 صحة و+6% سرعة حركة دائمة لكل مستوى في العالم المفتوح.",
            "BOOTS_UPG", "icons/armor_boots.png", null, true, 0, 1, 2, 35, IntArray(6), 0, 0,
            "........I..II..I"
        ),
        CraftRecipe(
            "craft_gunpowder", "طحن وبرد البارود المتفجر (x10)",
            "يبرد الحديد والحجر مع الخشب على طاولة 4×4 لإنتاج 10 أكياس بارود لتعبئة المدافع!",
            "GUNPOWDER", "icons/gunpowder.png", null, true, 1, 2, 1, 0, IntArray(6), 0, 0,
            "S.I..W.S.I.W...."
        ),
        CraftRecipe(
            "craft_storage_chest", "شونة وصندوق تخزين كبير",
            "صندوق تخزين ضخم يوضع في بيتك أو قلعتك لتفريغ الشنطة وتخزين كافة مواردك وأسلحتك!",
            "STORAGE_CHEST", "icons/storage_chest.png", null, true, 6, 2, 1, 0, IntArray(6), 0, 0,
            "WWWWWIWWWIWWWWWW"
        ),
        CraftRecipe(
            "craft_house_bp", "مخطط بناء بيت ريفي كامل (4×4)",
            "يبني بيتاً متكاملاً بضغطة واحدة (جدران، أرضية، باب، وشعلتي إضاءة) في موقعك!",
            "HOUSE_BP", "icons/blueprint_house.png", null, true, 10, 4, 0, 0, IntArray(6), 0, 0,
            "WWWWTo..oWS..SWW"
        ),
        CraftRecipe(
            "craft_watchtower_bp", "مخطط ستركشر: برج القنص والمدفعية 🏰",
            "يبني برج مراقبة حجري (سمك 2) بنوافذ قنص فورتنايت من 4 جهات وأرضية رخام ومنارة شفاء!",
            "WATCHTOWER_BP", "icons/blueprint_watchtower.png", null, true, 4, 12, 2, 0, IntArray(6), 0, 0,
            "SSSSS..SSIIISSSS", 0
        ),
        CraftRecipe(
            "craft_farm_bp", "مخطط ستركشر: المزرعة الملكية المسوّرة 🌾",
            "يبني مزرعة متكاملة محاطة بسياج وبوابة حديدية و12 محصولاً متنوعاً ومروياً بالكامل!",
            "FARM_BP", "icons/blueprint_farm.png", null, true, 8, 2, 1, 0, IntArray(6), 0, 0,
            "WWWWWWWW.II.WWWW", 3
        ),
        CraftRecipe(
            "craft_fort_bp", "مخطط قلعة حجرية سمك 2 (ضد الزومبي المفجر)",
            "يبني حصناً حجرياً مزدوج الجدران (سمك 2 حجر) لا يستطيع الزومبي المفجر اختراقه أبداً!",
            "FORT_BP", "icons/blueprint_fort.png", null, true, 4, 16, 2, 0, IntArray(6), 0, 0,
            "SSSSSSSSSSSSSSWW"
        ),
        CraftRecipe(
            "craft_seeds", "تجهيز بذور زراعية وشتلات (x6)",
            "يستخرج 4 بذور محاصيل قمح + 2 شتلة شجر لزراعتها يدوياً في مزرعتك.",
            "SEEDS_PACK", "icons/seeds.png", null, true, 2, 0, 0, 0, IntArray(6), 0, 0,
            ".WW..WW........."
        ),
        CraftRecipe(
            "craft_blk_stone", "طوب حجري للتحصين المزدوج (x10)",
            "حجارة صلبة لا تنكسر بالسيف أو المدفع؛ ابنِ جداراً بسمك 2 حجر لصد الزومبي المفجر!",
            "BLK_STONE", "icons/block_stone_brick.png", null, true, 0, 2, 0, 0, IntArray(6), 0, 0,
            "SSSSSS.........."
        ),
        CraftRecipe(
            "craft_blk_wood", "ألواح خشبية للبناء السريع (x10)",
            "بلوكات جدران خشبية لبناء البيوت والغرف والحماية السريعة.",
            "BLK_WOOD", "icons/block_wood.png", null, true, 2, 0, 0, 0, IntArray(6), 0, 0,
            "WWWWWW.........."
        ),
        CraftRecipe(
            "craft_blk_window", "جدار نافذة قتالية - فورتنايت (x4)",
            "جدار حجري بفتحة قنص: يمنع الزومبي ومقذوفاتهم ويسمح لك بإطلاق السحر والسهام من خلاله!",
            "BLK_WINDOW", "icons/block_window.png", null, true, 1, 2, 0, 0, IntArray(6), 0, 0,
            "SSSSS..S.WW.SSSS"
        ),
        CraftRecipe(
            "craft_blk_launchpad", "منصة القفز الهوائي (x2)",
            "تقذفك عالياً فوق أسوار قلعتك وحشود الزومبي مع ضربة هبطة ساحقة!",
            "BLK_LAUNCHPAD", "icons/block_launchpad.png", null, true, 2, 0, 2, 15, IntArray(6), 0, 0,
            ".II.WWWW.GG....."
        ),
        CraftRecipe(
            "craft_blk_bridge", "جسر وأرضية خشبية (x8)",
            "توضع على الأرض أو فوق المياه العميقة لبناء الجسور والموانئ!",
            "BLK_BRIDGE", "icons/block_bridge.png", null, true, 2, 0, 0, 0, IntArray(6), 0, 0,
            "WWWWWWWW........"
        ),
        CraftRecipe(
            "craft_blk_irongate", "بوابة قلعة حديدية مصفحة (x2)",
            "بوابة حديدية لأسوار القلعة تمنع الزومبي وتصمد أمام انفجار الزومبي المفجر!",
            "BLK_IRONGATE", "icons/iron_gate.png", null, true, 0, 2, 3, 0, IntArray(6), 0, 0,
            "ISSIISSIISSI...."
        ),
        CraftRecipe(
            "craft_blk_door", "باب منزل خشبي (x2)",
            "باب يفتح ويغلق عند التفاعل معه ويمنع دخول الزومبي.",
            "BLK_DOOR", "tiles/door.png", null, true, 2, 0, 0, 0, IntArray(6), 0, 0,
            "WW..WW..WW..WW.."
        ),
        CraftRecipe(
            "craft_blk_fence", "سياج حماية المزارع (x8)",
            "سياج يحمي محاصيلك الزراعية ويسمح بمرور السهام والسحر.",
            "BLK_FENCE", "icons/block_fence.png", null, true, 2, 0, 0, 0, IntArray(6), 0, 0,
            "W.W.WWWW.W.W...."
        ),
        CraftRecipe(
            "craft_blk_torch", "مشاعل إضاءة (x6)",
            "توضع على أسوار القلعة وداخل البيوت والكهوف لطرد الظلام ليلاً.",
            "BLK_TORCH", "environment/torch.png", null, true, 1, 1, 0, 0, IntArray(6), 0, 0,
            ".S...W...W......"
        ),
        CraftRecipe(
            "craft_blk_tnt", "بلوك متفجرات TNT بالبارود (x3)",
            "يُصنع من البارود والحجر؛ ينفجر عند ضربه ويكسر درع الزعيم العملاق!",
            "BLK_TNT", "icons/block_tnt.png", null, true, 0, 2, 0, 0, IntArray(6), 0, 3,
            "P.P.SPS.P.P....."
        ),
        CraftRecipe(
            "craft_cannon", "مدفع أسوار القلعة الآلي",
            "يوضع على أسوار قلعتك الحجرية أو الأرض؛ املأه بالبارود ليقصف الزومبي تلقائياً!",
            "CANNON", "icons/pirate_cannon.png", null, true, 4, 2, 4, 25, IntArray(6), 0, 2,
            "IIII.PP.WWWWSSSS"
        ),
        CraftRecipe(
            "craft_bed", "سرير النوم الميداني",
            "يُصنع من الصوف والخشب. ضعه في بيتك للنوم حتى الفجر وحفظ تقدمك!",
            "BED", "icons/bed.png", null, true, 4, 0, 0, 0, IntArray(6), 3, 0,
            "LLL.WWWW........"
        ),
        CraftRecipe(
            "craft_furnace", "فرن الصهر والطبخ",
            "يوضع في العالم لطهي اللحوم والقمح وصهر الحديد والحجر إلى ذهب وزجاج.",
            "FURNACE", "icons/furnace.png", null, true, 0, 6, 3, 0, IntArray(6), 0, 0,
            "SSSSS..SSIIISSSS"
        ),
        CraftRecipe(
            "craft_boat", "مركب الإبحار السريع",
            "يتيح لك الإبحار فوق المياه العميقة والوصول للجزر وحصون القراصنة!",
            "BOAT", "icons/boat.png", null, false, 8, 0, 1, 20, IntArray(6), 2, 0,
            "L..LW..WWWWWWWWW"
        ),
        CraftRecipe(
            "craft_campfire", "مخيم السفر السريع",
            "يبني نار مخيم تفتح نقطة سفر سريع وتطرد وحوش الليل.",
            "CAMPFIRE", "environment/bonefire.png", null, true, 5, 3, 0, 0, IntArray(6), 0, 0,
            ".W..WWW.SSSS...."
        ),
        CraftRecipe(
            "craft_pickaxe", "معول تكسير الحجارة والتعدين",
            "الأداة الوحيدة القادرة على تكسير الصخور وجدران الحجر وبيوت القرية وتعدين الكهوف!",
            "PICKAXE", "ui/ico/axe.png", Color.parseColor("#FFB74D"), false, 3, 2, 2, 0, IntArray(6), 0, 0,
            "ISSI.W...W...W.."
        ),
        CraftRecipe(
            "craft_key", "مفتاح ذهبي",
            "يفتح صناديق الغنائم الذهبية في الحصون وطوابق الكهوف.",
            "GOLD_KEY", "icons/key_gold.png", null, true, 0, 0, 2, 45, IntArray(6), 0, 0,
            ".GG..I...I......"
        ),
        CraftRecipe(
            "craft_stew", "حساء الطاقة والقمح الساخن (x2)",
            "وجبة مطبوخة تملأ الشبع +75 وتستعيد +35 صحة.",
            "STEW", "icons/stew.png", null, true, 2, 0, 0, 15, IntArray(6), 0, 0,
            ".WW..GG........."
        ),
        CraftRecipe(
            "craft_speed_pot", "إكسير السرعة والرؤية (x2)",
            "يضاعف سرعة الحركة ويكشف ظلام الكهوف والليل.",
            "SPEED_POT", "icons/pot_speed.png", null, true, 0, 2, 1, 25, IntArray(6), 0, 0,
            ".I..S.S..G......"
        ),
        CraftRecipe(
            "craft_bomb", "قنبلتان بالبارود (x2)",
            "قنابل بارود يدوية تنفجر بمساحة 160 وتكسر درع الزعيم العملاق.",
            "BOMB", "icons/bomb.png", null, true, 0, 1, 1, 0, IntArray(6), 0, 2,
            ".I..PPP..S......"
        ),
        CraftRecipe(
            "upg_weapon", "صقل السلاح الأسطوري (الحداد +10)",
            "+14% ضرر لكل مستوى (حتى 10 مستويات لمواجهة الزعيم العملاق).",
            "UPG_WEAPON", "icons/sabre.png", null, true, 0, 0, 0, 0, IntArray(6), 0, 0,
            "MMMMIIII.GG....."
        ),
        CraftRecipe(
            "upg_armor", "تقوية الدرع الأسطوري (الحداد +10)",
            "+30 صحة و+2% حماية لكل مستوى (حتى 10 مستويات).",
            "UPG_ARMOR", "ui/ico/shield.png", Color.parseColor("#FFD54F"), true, 0, 0, 0, 0, IntArray(6), 0, 0,
            "IIIIIMMM.GG....."
        ),
        CraftRecipe(
            "craft_plasma", "مدفع البلازما الخارق",
            "يرفع ضرر السحر بنسبة هائلة ويجعله يخترق صفوف الزومبي.",
            "PLASMA", "icons/cannon.png", null, false, 0, 4, 5, 90,
            intArrayOf(0, 4, 3, 0, 0, 0), 0, 4, "MMMMIIIIPPPPSSSS"
        ),
        CraftRecipe(
            "craft_shield", "درع النانو المتطور",
            "+170 صحة قصوى ويقلل الضرر 44%.",
            "SHIELD", "ui/ico/shield.png", Color.parseColor("#4DD0E1"), false, 3, 0, 6, 120,
            intArrayOf(0, 0, 5, 3, 0, 0), 0, 0, "I..IIMMMIIII.II."
        ),
        CraftRecipe(
            "craft_godblade", "نصل الأبعاد الكوني",
            "سلاح متقدم: ضرر 175، ويضاعف قوة التعويذات السحرية.",
            "GODBLADE", "icons/blade.png", null, false, 5, 5, 6, 250,
            intArrayOf(0, 5, 5, 4, 4, 0), 0, 0, ".MM..MM..II..WW."
        ),
        CraftRecipe(
            "craft_armor_titan", "درع التيتانيوم الخالد (نهاية اللعبة)",
            "أقوى درع لمواجهة الزعيم العملاق: +290 صحة ويقلل الضرر 58%!",
            "ARMOR_TITAN", "ui/ico/shield.png", Color.parseColor("#FF1744"), false, 10, 12, 10, 420,
            intArrayOf(4, 4, 4, 4, 4, 4), 0, 0, "MMMMIIIISSSSMMMM"
        ),
        CraftRecipe(
            "craft_abyss_sword", "سيف قاهر الهاوية الخرافي (نهاية اللعبة)",
            "السلاح النهائي لختم اللعبة: ضرر 265 وسرعة هجوم خاطفة!",
            "ABYSS_SWORD", "icons/sword_gold.png", Color.parseColor("#FF1744"), false, 12, 12, 14, 600,
            intArrayOf(6, 6, 6, 6, 6, 6), 0, 0, ".MM..MM.IIII.WW."
        ),
        CraftRecipe(
            "craft_abyss_seal", "صياغة ختم الهاوية المركّب",
            "يصنع ختماً من أختام الهاوية الـ 7 اللازمة لاستدعاء الزعيم العملاق.",
            "ABYSS_SEAL", "icons/abyss_seal.png", null, true, 4, 4, 4, 200,
            intArrayOf(2, 2, 2, 2, 2, 2), 0, 0, "MMMMMGGMMMMM.II."
        )
    )
}
