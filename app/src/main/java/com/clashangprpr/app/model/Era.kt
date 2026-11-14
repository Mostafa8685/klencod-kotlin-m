package com.clashangprpr.app.model

enum class EraType(
    val title: String,
    val subtitle: String,
    val description: String,
    val bgColor: Int,
    val track: Int,
    val darkness: Float
) {
    ERA_1_MEDIEVAL(
        "العصر الأول: قلعة الفارس الأسود",
        "The Medieval Fortress",
        "قلعة محاصرة، وحارس يخفي سراً. اكسر الحصار واستعد الشظية الأولى.",
        0xFF1A1A24.toInt(), 1, 0f
    ),
    ERA_2_CAVES(
        "العصر الثاني: كهوف الظلام",
        "The Ancient Caverns",
        "الظلام هنا يسمع خطواتك. أشعل الطريق بالكريستال واحذر الفخاخ.",
        0xFF161E14.toInt(), 2, 0.86f
    ),
    ERA_3_ZOMBIE(
        "العصر الثالث: مدينة الزومبي",
        "The Ruined City",
        "مدينة ميتة تستيقظ ليلاً. اجمع الموارد وانجُ من الجحافل.",
        0xFF2B1414.toInt(), 3, 0f
    ),
    ERA_4_ISLAND(
        "العصر الرابع: جزيرة القراصنة",
        "The Pirate Island",
        "كنز مدفون وقبطان لا يرحم. اصنع الفأس وجد المفتاح تحت الرمال.",
        0xFF0D253A.toInt(), 4, 0f
    ),
    ERA_5_CYBER(
        "العصر الخامس: السايبربنك",
        "Cyber Sector Zero",
        "ثلاثة مولدات تغذي درع القائد. عطّلها كلها لتكشف ضعفه.",
        0xFF101524.toInt(), 5, 0.35f
    ),
    ERA_6_VOID(
        "العصر السادس: فراغ الأبعاد",
        "The Cosmic Void",
        "نهاية الزمن. هنا تواجه آخر ما تبقى من نفسك.",
        0xFF0A0814.toInt(), 6, 0.6f
    );

    val number: Int get() = ordinal + 1

    fun next(): EraType? {
        val all = values()
        return if (ordinal + 1 < all.size) all[ordinal + 1] else null
    }
}
