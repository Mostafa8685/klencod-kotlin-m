package com.chronoknight.game.model

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
        "واجه جحافل الزومبي والوحش الطافر لختم اللعبة وإنقاذ الزمن!",
        0xFF2B1414.toInt()
    )
}
