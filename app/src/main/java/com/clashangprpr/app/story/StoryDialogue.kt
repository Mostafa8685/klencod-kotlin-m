package com.clashangprpr.app.story

import com.clashangprpr.app.model.EraType

data class DialogueLine(
    val speaker: String,
    val textAr: String,
    val textEn: String
)

object StoryManager {
    fun getIntroDialogue(): List<DialogueLine> = listOf(
        DialogueLine(
            "فارس الزمن (حرب الفارس)",
            "لقد تصدع نسيج الكون عبر 6 عصور متتالية! انطلق في أضخم ملحمة زمنية!",
            "Travel across all 6 eras to seal the cosmic timeline!"
        ),
        DialogueLine(
            "الصوت القديم",
            "في العصر السادس الأخير (فراغ الأبعاد)، تنكسر قوانين الفيزياء؛ استخدم زر تباطؤ الزمن لتفادي الهجمات وقهر سيد الأبعاد!",
            "In Era 6, use Chrono Freeze to slow down time and defeat the Dimension Overlord!"
        )
    )

    fun getEraClearDialogue(era: EraType): List<DialogueLine> = when (era) {
        EraType.ERA_1_MEDIEVAL -> listOf(
            DialogueLine(
                "الفارس الأسود",
                "سقطت قلعتي... لكن الكهوف السفلى ستبتلعك حياً!",
                "The fortress falls... but the caverns await you!"
            ),
            DialogueLine(
                "فارس الزمن",
                "البوابة الزمنية فُتحت إلى دهاليز الكهوف المظلمة!",
                "Time Portal to Caverns is open!"
            )
        )
        EraType.ERA_2_CAVES -> listOf(
            DialogueLine(
                "حارس الكهف",
                "تكسرت صخوري... العصر القادم مليء بالزومبي المدمر...",
                "The zombie apocalypse awaits you ahead..."
            ),
            DialogueLine(
                "فارس الزمن",
                "الآن إلى شوارع نهاية العالم والزومبي!",
                "To the Zombie Ruined City!"
            )
        )
        EraType.ERA_3_ZOMBIE -> listOf(
            DialogueLine(
                "الزومبي الطافر",
                "غررااه... البوابة البحرية لجزيرة القراصنة فُتحت...",
                "The ocean portal to the Pirate Island is open..."
            ),
            DialogueLine(
                "فارس الزمن",
                "الآن إلى جزيرة القراصنة والكنز المدفون بالرمال!",
                "To the Pirate Island of the Lost Gem!"
            )
        )
        EraType.ERA_4_ISLAND -> listOf(
            DialogueLine(
                "قبطان القراصنة",
                "استعدت جوهرة المحيط... بوابتك تقود إلى المنطقة العسكرية للسايبربنك!",
                "The ocean gem is yours... but the Cyber Sector awaits!"
            ),
            DialogueLine(
                "فارس الزمن",
                "الآن إلى العصر الخامس: معركة السايبربنك ومولدات الطاقة!",
                "To Cyber Sector Zero!"
            )
        )
        EraType.ERA_5_CYBER -> listOf(
            DialogueLine(
                "القائد السايبورغ",
                "سقط درعي... لكن انهيار المولدات مزق نسيج الأبعاد وفتح بوابة الفراغ الكوني الأخير...",
                "The reactor collapse tore open the Cosmic Void..."
            ),
            DialogueLine(
                "فارس الزمن",
                "الآن إلى المعركة الأسطورية الكبرى: العصر السادس وسيد فراغ الأبعاد!",
                "To the Final Frontier: The Cosmic Void of Infinity!"
            )
        )
        EraType.ERA_6_VOID -> listOf(
            DialogueLine(
                "سيد فراغ الأبعاد",
                "مستحيل... تحكمت في الزمن وقهرت نسخ الأبعاد واستعدت حجر الكرونو الأبدي بالكامل...",
                "You mastered time and conquered the infinity..."
            ),
            DialogueLine(
                "فارس الزمن",
                "اكتملت جميع شظايا الزمن واستقرت العصور الستة للأبد! مبروك، لقد ختمت اللعبة بنصر أسطوري غير مسبوق!",
                "ALL 6 ERAS CONQUERED! YOU ARE THE ULTIMATE CHRONO MASTER!"
            )
        )
    }
}
