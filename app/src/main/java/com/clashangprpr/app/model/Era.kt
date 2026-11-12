package com.clashangprpr.app.model

enum class EraType(
    val title: String,
    val subtitle: String,
    val description: String,
    val bgColor: Int,
    val bgAsset: String,
    val mapWidth: Float,
    val mapHeight: Float
) {
    ERA_0_HUB(
        "مدينة الزمن: ملاذ ساعة كرونو",
        "The Chrono Sanctuary Hub",
        "تحدث مع حارس الساعة واختر بوابة العصر التي تريد استعادة شظيتها!",
        0xFF141826.toInt(),
        "environment/bg_hub_hd.png",
        2160f,
        1440f
    ),
    ERA_1_MEDIEVAL(
        "الجزء الزمني الأول: قلعة القرون الوسطى",
        "Era 1: The Medieval Fortress",
        "تصدى لفرسان الظلام وقائدهم الفاسد لفتح بوابة العالم المفتوح!",
        0xFF1A1A24.toInt(),
        "environment/bg_medieval_hd.png",
        2160f,
        1440f
    ),
    ERA_2_OPEN_WORLD(
        "الزمن الثاني: القارة المفتوحة وعقرب الساعة",
        "Era 2: Vast Open World & The Clock Hand",
        "استكشف القارة الضخمة، نفّذ المهام، اشترِ مشروب الإخفاء، واهزم زعيم الحصن لاستعادة عقرب الساعة!",
        0xFF1B2B1B.toInt(),
        "environment/bg_medieval_hd.png",
        10240f,
        10240f
    ),
    ERA_2_CAVES(
        "العصر الثالث: سراديب وكهوف الموت",
        "The Ancient Caverns & Dungeons",
        "تفادى الأفخاخ الحارقة واقضِ على حارس الكهف العملاق!",
        0xFF161E14.toInt(),
        "environment/bg_cave_hd.png",
        2400f,
        1440f
    ),
    ERA_3_ZOMBIE(
        "العصر الرابع: نهاية العالم والزومبي",
        "The Ruined Zombie City",
        "واجه جحافل الزومبي والوحش الطافر لفتح مسار البوابة البحرية!",
        0xFF2B1414.toInt(),
        "environment/bg_zombie_hd.png",
        2400f,
        1440f
    ),
    ERA_4_ISLAND(
        "العصر الخامس: جزيرة القراصنة والكنز المدفون",
        "The Pirate Island & The Lost Ocean Gem",
        "اصنع فأس الحفر، استخرج المفتاح المدفون تحت الرمال، واجلب جوهرة المحيط!",
        0xFF0D253A.toInt(),
        "environment/bg_island_hd.png",
        2400f,
        1920f
    ),
    ERA_5_CYBER(
        "العصر السادس: حرب المستقبل والسايبربنك",
        "The Cyber-Sector Zero & Rogue Mecha Warfare",
        "عطل أو اخترق المولدات النووية الثلاثة لإسقاط درع القائد السايبورغ!",
        0xFF101524.toInt(),
        "environment/bg_cyber_hd.png",
        2400f,
        1440f
    ),
    ERA_6_VOID(
        "العصر السابع: فراغ الأبعاد ونهاية الزمن",
        "The Cosmic Void & Dimension Shatter",
        "استخدم قدرة تباطؤ الزمن، واقضِ على سيد الأبعاد لختم خط الزمن للأبد!",
        0xFF0A0814.toInt(),
        "environment/bg_void_hd.png",
        2400f,
        1440f
    )
}
