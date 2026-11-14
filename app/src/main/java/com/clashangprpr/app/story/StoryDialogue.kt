package com.clashangprpr.app.story

import com.clashangprpr.app.model.EraType

data class DialogueLine(val speaker: String, val text: String, val portrait: String)

object Story {
    private const val HERO = "sprites/player/idle.png"
    private const val TIME = "environment/magic_circle.png"
    private const val MERCHANT = "sprites/merchant/idle.png"

    fun intro(era: EraType): List<DialogueLine> = when (era) {
        EraType.ERA_1_MEDIEVAL -> listOf(
            DialogueLine("صوت الزمن", "قبل ألف عام، حطّمتَ بيدك حجر كرونو في لحظة يأس. تناثرت شظاياه في ستة عصور.", TIME),
            DialogueLine("الفارس", "ذلك اليوم لا يفارقني. سأجمع الشظايا وأصلح ما كسرته.", HERO),
            DialogueLine("صوت الزمن", "العصر الأول: قلعة الفارس الأسود. حارسها يحمل المفتاح... ويحمل سراً.", TIME)
        )
        EraType.ERA_2_CAVES -> listOf(
            DialogueLine("صوت الزمن", "كهوف الظلام. لا نور هنا إلا ما تصنعه بنفسك. والفخاخ لا تنتظر.", TIME),
            DialogueLine("الفارس", "أسمع أنفاساً في العتمة. أبقِ عينيك على الكريستال.", HERO)
        )
        EraType.ERA_3_ZOMBIE -> listOf(
            DialogueLine("صوت الزمن", "مدينة ماتت قبل أوانها، وموتاها لا يرقدون.", TIME),
            DialogueLine("الفارس", "رائحة الخراب... اجمع الخشب والحجر، فسنحتاج سلاحاً أقوى.", HERO)
        )
        EraType.ERA_4_ISLAND -> listOf(
            DialogueLine("صوت الزمن", "جزيرة القراصنة. المفتاح مدفون تحت الرمال، ولن تحفر إلا بفأس.", TIME),
            DialogueLine("الفارس", "إذن أحتاج فأساً. الطاولة قرب المعسكر.", HERO)
        )
        EraType.ERA_5_CYBER -> listOf(
            DialogueLine("صوت الزمن", "القطاع صفر. ثلاثة مولدات تغذي درع القائد. عطّلها كلها لتكشف ضعفه.", TIME),
            DialogueLine("الفارس", "حديد وكهرباء... هذا العصر لا يشبه ما أعرفه.", HERO)
        )
        EraType.ERA_6_VOID -> listOf(
            DialogueLine("صوت الزمن", "الفراغ. هنا تنتهي كل الأزمنة، ولا يبقى إلا ما رفضتَ مواجهته.", TIME),
            DialogueLine("الفارس", "إذن هذه آخر شظية. لن أتراجع.", HERO)
        )
    }

    fun clear(era: EraType): List<DialogueLine> = when (era) {
        EraType.ERA_1_MEDIEVAL -> listOf(
            DialogueLine("الفارس الأسود", "لم أكن عدوك. كنت أحمي الحجر منك. خذ الشظية الأولى، لكنها ستكلفك أكثر مما تظن.", "sprites/kit2/idle.png"),
            DialogueLine("الفارس", "ماذا تقصد؟", HERO),
            DialogueLine("صوت الزمن", "الشظية الأولى عادت. بوابة الزمن تنتظرك.", TIME)
        )
        EraType.ERA_2_CAVES -> listOf(
            DialogueLine("الحارس الحجري", "أنا نسخة ممن حاولوا إصلاح الحجر قبلك... وفشلوا.", "sprites/kit1/idle.png"),
            DialogueLine("الفارس", "إذن لست الأول. لكنني سأكون الأخير.", HERO)
        )
        EraType.ERA_3_ZOMBIE -> listOf(
            DialogueLine("الجرثومة الأم", "الزمن المكسور يتعفن، وأنا ثمرته. أنت من زرعتني.", "sprites/zombie_titan/idle.png"),
            DialogueLine("الفارس", "ما كسرتُه سأصلحه. هذه المدينة ستنام أخيراً.", HERO)
        )
        EraType.ERA_4_ISLAND -> listOf(
            DialogueLine("القبطان", "كنتُ مثلك. كسرتُ الزمن لأبقي ذهبي. والآن لا شيء يشبع هذا الجوع.", "sprites/pirate_captain/idle.png"),
            DialogueLine("الفارس", "الكنز الحقيقي هو أن تُعيد ما أخذته.", HERO)
        )
        EraType.ERA_5_CYBER -> listOf(
            DialogueLine("القائد", "النظام لم يتمرد. كان يحاول حمايتك من نفسك.", "sprites/cyber_boss/idle.png"),
            DialogueLine("الفارس", "الحماية التي تسجن ليست حماية.", HERO)
        )
        EraType.ERA_6_VOID -> listOf(
            DialogueLine("سيد الفراغ", "أنا أنت... ذلك الذي رفض أن يقبل الخسارة.", "sprites/kit2/idle.png"),
            DialogueLine("الفارس", "ما كُسر لا يُصلح بالإنكار. سأقبل ما حدث.", HERO),
            DialogueLine("صوت الزمن", "تحررت الشظية الأخيرة. الزمن يستأنف مساره... وأنت أخيراً حرّ.", TIME)
        )
    }

    fun openWorldIntro(): List<DialogueLine> = listOf(
        DialogueLine("صوت الزمن", "اندمجت العصور الستة في عالم واحد عملاق! القرى، الكهوف العميقة، جزر القراصنة، والحصون تنتظرك.", TIME),
        DialogueLine("الفارس", "عالم شاسع بلا حدود... سأجمع الصوف والخشب، أصنع المراكب والمدافع، وأستكشف كل كهف وجزيرة!", HERO),
        DialogueLine("صوت الزمن", "تاجر مع القرويين أو خاطر بالسرقة، بدّل قدراتك السحرية من الحقيبة، واستعمل البوابات الزمنية للتنقل السريع!", TIME)
    )

    fun merchantGreeting(): List<DialogueLine> = listOf(
        DialogueLine("التاجر إلريك", "أهلاً أيها المسافر عبر العصور! لدي أسلحة جديدة، جرعات سرعة ورؤية، مفاتيح ذهبية، وخرائط كنوز.", MERCHANT)
    )

    fun keyHint(era: EraType): String = when (era) {
        EraType.ERA_4_ISLAND -> "البوابة مقفلة. اصنع فأساً من الطاولة ثم احفر الكنز المدفون."
        EraType.ERA_6_VOID -> "الحاجز لا ينفتح إلا بعد هزيمة كل أشباه الفارس."
        else -> "البوابة مقفلة. اهزم الحارس الذي يحمل المفتاح."
    }
}
