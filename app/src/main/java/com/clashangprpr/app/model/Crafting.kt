package com.clashangprpr.app.model

data class CraftRecipe(
    val id: String,
    val titleAr: String,
    val descAr: String,
    val woodCost: Int,
    val stoneCost: Int,
    val ironCost: Int,
    val resultType: String
)

object CraftingRegistry {
    val recipes = listOf(
        CraftRecipe(
            "craft_bomb",
            "💣 قنبلة متفجرة (x2)",
            "تسبب ضرراً انفجارياً واسعاً بـ 90 لجميع الوحوش المحيطة",
            woodCost = 2,
            stoneCost = 1,
            ironCost = 1,
            resultType = "BOMB"
        ),
        CraftRecipe(
            "craft_sword",
            "⚔️ سيف فولاذي معزز (ضرر 60)",
            "سيف ثقيل مصنوع من خامات الحديد والخشب الصلب",
            woodCost = 3,
            stoneCost = 2,
            ironCost = 3,
            resultType = "SWORD"
        ),
        CraftRecipe(
            "craft_potion",
            "🧪 إكسير شفاء كلي (+90 HP)",
            "تركيبة كيميائية حجرية متطورة لاستعادة الصحة",
            woodCost = 1,
            stoneCost = 3,
            ironCost = 0,
            resultType = "POTION"
        ),
        CraftRecipe(
            "craft_shield",
            "🛡️ درع تيتانيوم متين (+120 أقصى صحة)",
            "درع دفاعي متين يرفع قدرة تحملك ضد الزعماء",
            woodCost = 2,
            stoneCost = 4,
            ironCost = 4,
            resultType = "SHIELD"
        )
    )
}
