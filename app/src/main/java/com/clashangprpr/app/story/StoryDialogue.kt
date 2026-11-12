package com.clashangprpr.app.story

import com.clashangprpr.app.model.EraType

data class DialogueLine(
    val speaker: String,
    val textAr: String,
    val textEn: String
)

object StoryManager {
    fun getHubIntroDialogue(): List<DialogueLine> = listOf(
        DialogueLine(
            "حارس الساعة (Chrono Keeper)",
            "أهلاً بعودتك إلى مدينة الزمن... بعد استعادة عقرب الساعة، أصبحت كل بوابات العصور متاحة لإكمال حجر كرونو!",
            "Welcome to the Time Sanctuary! Use the portals to reclaim all remaining shards."
        )
    )

    fun getSageRescueDialogue(rescueCount: Int): List<DialogueLine> = listOf(
        DialogueLine(
            "الحكيم (المنقذ)",
            "ها قد أنقذتك هذه المرة (الإنقاذ رقم $rescueCount) ونقلتك إلى الملاذ الآمن... لكن الله أعلم هل سأقدر على إنقاذك في المرة القادمة أم لا!",
            "I saved you this time (#$rescueCount)... Only God knows if I can rescue you next time!"
        ),
        DialogueLine(
            "الحكيم (المنقذ)",
            "زعيم الحصن وجنوده أقوياء جداً! طوّر سلاحك عند الحداد، نفّذ مهام القرية، واشترِ مشروب الإخفاء من تاجر الكيمياء البعيد!",
            "Upgrade at the Blacksmith, finish village quests, and buy Invisibility from the far Alchemist!"
        )
    )

    fun getEraIntroDialogue(era: EraType): List<DialogueLine> {
        return when (era) {
            EraType.ERA_0_HUB -> getHubIntroDialogue()
            EraType.ERA_1_MEDIEVAL -> listOf(
                DialogueLine(
                    "حارس الساعة (Chrono Keeper)",
                    "هذا هو الجزء الزمني الأول: قلعة القرون الوسطى. اهزم الفارس الأسود لتعبر إلى الزمن الثاني (القارة المفتوحة)!",
                    "Era 1: Medieval Fortress. Defeat the Dark Paladin to unlock the Open World!"
                )
            )
            EraType.ERA_2_OPEN_WORLD -> listOf(
                DialogueLine(
                    "الحكيم (The Sage)",
                    "مرحباً بك في الزمن الثاني: القارة المفتوحة الضخمة! هنا سرق زعيم الحصن عقرب الساعة وأحاط نفسه بجيش مرعب من الجنود.",
                    "Welcome to Era 2 Open World! The Fortress Overlord stole the Clock Hand."
                ),
                DialogueLine(
                    "الحكيم (The Sage)",
                    "استكشف القرى، نفّذ مهام الناس، طوّر سلاحك عند الحداد، وابحث عن تاجر الكيمياء البعيد في الجنوب الغربي لشراء مشروب الإخفاء!",
                    "Explore villages, complete quests, visit the Blacksmith, and find the far Alchemist in the south-west!"
                )
            )
            EraType.ERA_2_CAVES -> listOf(
                DialogueLine(
                    "صدى الحجر (Stone Echo)",
                    "سراديب الموت... الفخاخ تتحرك مع نبض الشظية. احذر من حارس الكهف الحجري!",
                    "Ancient Caverns... Watch out for traps and the Stone Golem!"
                )
            )
            EraType.ERA_3_ZOMBIE -> listOf(
                DialogueLine(
                    "الفارس السائر (Chrono Knight)",
                    "مدينة مدمرة... التهمت الشظية من حاول السيطرة عليها وحولتهم إلى زومبي!",
                    "The Ruined City... The shard corrupted all survivors here."
                )
            )
            EraType.ERA_4_ISLAND -> listOf(
                DialogueLine(
                    "رسالة في زجاجة",
                    "جزيرة القراصنة! اصنع فأس التنقيب، احفر الرمال للعثور على المفتاح، واستعد جوهرة المحيط!",
                    "Pirate Island! Craft a Pickaxe, dig the buried key, and claim the Ocean Gem!"
                )
            )
            EraType.ERA_5_CYBER -> listOf(
                DialogueLine(
                    "نظام الإنذار الآلي",
                    "قطاع السايبربنك: القائد الميكا محمي بدرع نووي. دمر أو اخترق المولدات الثلاثة لإسقاط درعه!",
                    "Cyber Sector Zero: Destroy or Hack the 3 EMP Generators to drop the Boss shield!"
                )
            )
            EraType.ERA_6_VOID -> listOf(
                DialogueLine(
                    "سيد الفراغ الأبدي (Void Overlord)",
                    "أخيراً وصلت... أنا انعكاس مستقبلك لو فشلت في ترميم الحجر! لن أسمح لك بإعادته!",
                    "At last... I am your future self if you fail! You cannot mend the Stone!"
                )
            )
        }
    }

    fun getEraClearDialogue(era: EraType): List<DialogueLine> {
        return when (era) {
            EraType.ERA_0_HUB -> emptyList()
            EraType.ERA_1_MEDIEVAL -> listOf(
                DialogueLine(
                    "الفارس الأسود (المهزوم)",
                    "لقد فعلتها... البوابة ستنقلك الآن إلى الزمن الثاني: القارة المفتوحة الضخمة!",
                    "You did it... The portal now leads to Era 2: The Vast Open World!"
                )
            )
            EraType.ERA_2_OPEN_WORLD -> listOf(
                DialogueLine(
                    "الحكيم (The Sage)",
                    "مذهل! هزمت زعيم الحصن واستعدت عقرب الساعة! الآن يمكنك العودة لمدينة الزمن والانتقال بين العصور!",
                    "Incredible! You reclaimed the Clock Hand! Return to the Time Sanctuary!"
                )
            )
            EraType.ERA_2_CAVES -> listOf(
                DialogueLine(
                    "حارس الساعة (Chrono Keeper)",
                    "استعدت شظية الكهوف! البوابة جاهزة لإعادتك إلى مدينة الزمن.",
                    "Cavern shard reclaimed! Return through the portal to the Sanctuary."
                )
            )
            EraType.ERA_3_ZOMBIE -> listOf(
                DialogueLine(
                    "الفارس السائر (Chrono Knight)",
                    "تطهرت المدينة! شظية الزمن أصبحت في قبضتنا.",
                    "The city is cleared! The Chrono shard is ours."
                )
            )
            EraType.ERA_4_ISLAND -> listOf(
                DialogueLine(
                    "روح البحر",
                    "جوهرة المحيط والشظية ملكك الآن! عد إلى البوابة لمواصلة رحلتك.",
                    "The Ocean Gem and shard are yours! Enter the portal."
                )
            )
            EraType.ERA_5_CYBER -> listOf(
                DialogueLine(
                    "نواة الذكاء الاصطناعي",
                    "تعطل الدرع النووي وسقط القائد! الشظية جاهزة للعودة إلى الملاذ.",
                    "Shield offline, Commander defeated! Return to the Sanctuary."
                )
            )
            EraType.ERA_6_VOID -> listOf(
                DialogueLine(
                    "حارس الساعة (Chrono Keeper)",
                    "هزمت انعكاسك المظلم وأعدت الشظية الأخيرة! حان وقت اختيار مصير الزمن!",
                    "You defeated your dark reflection! Choose the fate of Time!"
                )
            )
        }
    }
}
