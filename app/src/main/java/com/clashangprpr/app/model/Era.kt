package com.clashangprpr.app.model

enum class EraType(
    val title: String,
    val subtitle: String,
    val description: String,
    val bgColor: Int
) {
    ERA_1_MEDIEVAL(
        "العصر الأول: قلعة القرون الوسطى",
        "The Medieval Fortress",
        "تصدى لفرسان الظلام وقائدهم الفاسد لاستعادة شظية الحجر الأولى!",
        0xFF1A1A24.toInt()
    ),
    ERA_2_CAVES(
        "العصر الثاني: سراديب وكهوف الموت",
        "The Ancient Caverns & Dungeons",
        "تفادى الأفخاخ الحارقة واقضِ على حارس الكهف العملاق!",
        0xFF161E14.toInt()
    ),
    ERA_3_ZOMBIE(
        "العصر الثالث: نهاية العالم والزومبي",
        "The Ruined Zombie City",
        "واجه جحافل الزومبي والوحش الطافر لفتح مسار البوابة البحرية!",
        0xFF2B1414.toInt()
    ),
    ERA_4_ISLAND(
        "العصر الرابع: جزيرة القراصنة والكنز المدفون",
        "The Pirate Island & The Lost Ocean Gem",
        "اصنع فأس الحفر، استخرج المفتاح المدفون تحت الرمال، واجلب جوهرة المحيط!",
        0xFF0D253A.toInt()
    ),
    ERA_5_CYBER(
        "العصر الخامس: حرب المستقبل والسايبربنك",
        "The Cyber-Sector Zero & Rogue Mecha Warfare",
        "عطل المولدات النووية الثلاثة لإسقاط درع القائد السايبورغ وافتح الفراغ الكوني!",
        0xFF101524.toInt()
    ),
    ERA_6_VOID(
        "العصر السادس: فراغ الأبعاد ونهاية الزمن",
        "The Cosmic Void & Dimension Shatter",
        "استخدم قدرة تباطؤ الزمن، واقضِ على سيد الأبعاد والنسخ السرابية لختم خط الزمن للأبد!",
        0xFF0A0814.toInt()
    )
}
