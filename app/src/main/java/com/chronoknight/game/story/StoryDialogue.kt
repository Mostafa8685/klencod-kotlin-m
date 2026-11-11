package com.chronoknight.game.story

import com.chronoknight.game.model.EraType

data class DialogueLine(
    val speaker: String,
    val textAr: String,
    val textEn: String
)

object StoryManager {
    fun getIntroDialogue(): List<DialogueLine> = listOf(
        DialogueLine(
            "أرجوس (فارس الزمن)",
            "لقد تحطم 'حجر الكرونو' الأسطوري وتصدع نسيج الزمن عبر العصور!",
            "The Chrono Keystone is shattered! The timeline is collapsing across the eras!"
        ),
        DialogueLine(
            "الصوت القديم",
            "عليك عبور ثلاثة أزمنة: القلعة العتيقة، الكهوف المظلمة، والمدينة المدمرة بالزومبي، لهزيمة قادة الظلام وفتح بوابات العصور!",
            "You must conquer three eras: The Fortress, The Dark Caverns, and The Ruined City to seal the rift!"
        )
    )

    fun getEraClearDialogue(era: EraType): List<DialogueLine> = when (era) {
        EraType.ERA_1_MEDIEVAL -> listOf(
            DialogueLine(
                "الفارس الأسود (الزعيم)",
                "مستحيل... لقد سقطت قلعتي... لكن الكهوف السفلى ستبتلعك حياً!",
                "Impossible... my fortress falls... but the depths of the caverns will consume you!"
            ),
            DialogueLine(
                "أرجوس",
                "استعدت الشظية الأولى! البوابة الزمنية فُتحت إلى دهاليز الكهوف!",
                "First Keystone shard secured! The Time Portal to the Caverns is open!"
            )
        )
        EraType.ERA_2_CAVES -> listOf(
            DialogueLine(
                "حارس الكهف",
                "تكسرت صخوري... لكن العصر القادم مليء بوحوش لا تعرف الرحمة...",
                "My stones crumble... but the future era is overrun by ravenous beasts..."
            ),
            DialogueLine(
                "أرجوس",
                "الشظية الثانية بحوزتي! الآن إلى المعركة الأخيرة في العصر الحديث ضد الزومبي!",
                "Second shard acquired! Now to the final battlefield: the Zombie Apocalypse!"
            )
        )
        EraType.ERA_3_ZOMBIE -> listOf(
            DialogueLine(
                "الزومبي الطافر الأكبر",
                "غررااااه... لا يمكنك إيقاف النهاية...",
                "GRAAAHHH... you cannot halt the end of days..."
            ),
            DialogueLine(
                "أرجوس",
                "لقد اكتمل حجر الزمن! جميع العصور استقرت وانتهت اللعنة للأبد! مبروك، لقد ختمت اللعبة بنجاح!",
                "The Chrono Keystone is whole! The timeline is saved forever! CONGRATULATIONS, YOU BEAT THE GAME!"
            )
        )
    }
}
