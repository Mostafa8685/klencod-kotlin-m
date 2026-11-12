package com.clashangprpr.app.model

data class CraftRecipe(
    val id: String,
    val titleAr: String,
    val descAr: String,
    val woodCost: Int,
    val stoneCost: Int,
    val ironCost: Int,
    val crystalCost: Int = 0,
    val mythrilCost: Int = 0,
    val resultType: String,
    val iconAsset: String
)

object CraftingRegistry {
    val recipes = listOf(
        CraftRecipe(
            "craft_pickaxe",
            "فأس التعدين والحفر (أساسي)",
            "مطلوب لتكسير الصخور والمعادن النادرة وحفر الكنوز",
            woodCost = 4,
            stoneCost = 3,
            ironCost = 1,
            resultType = "PICKAXE",
            iconAsset = "icons/item_15.png"
        ),
        CraftRecipe(
            "craft_ammo",
            "ذخيرة المسدس (+15 طلقة)",
            "تصنع طلقات للمسدس باستخدام الحديد والحجر",
            woodCost = 1,
            stoneCost = 2,
            ironCost = 3,
            resultType = "AMMO",
            iconAsset = "icons/ammo_bullet.png"
        ),
        CraftRecipe(
            "craft_bomb",
            "قنبلة متفجرة / C4 (x2)",
            "تسبب ضرراً انفجارياً مساحياً أو تُزرع كفخ",
            woodCost = 3,
            stoneCost = 2,
            ironCost = 2,
            resultType = "BOMB",
            iconAsset = "icons/item_1.png"
        ),
        CraftRecipe(
            "craft_shield",
            "درع النانو المتطور (صحة 320)",
            "درع متقدم يرفع صحتك ويصد 45% من الضرر",
            woodCost = 6,
            stoneCost = 8,
            ironCost = 10,
            crystalCost = 3,
            resultType = "SHIELD",
            iconAsset = "icons/item_3.png"
        ),
        CraftRecipe(
            "craft_plasma",
            "بندقية البلازما (ضرر 135)",
            "سلاح متطور يطلق طاقة ليزرية مدمرة",
            woodCost = 8,
            stoneCost = 8,
            ironCost = 12,
            crystalCost = 4,
            resultType = "PLASMA",
            iconAsset = "icons/pirate_5.png"
        ),
        CraftRecipe(
            "craft_godblade",
            "نصل الأبعاد الكوني (ضرر 190)",
            "أقوى سلاح أسطوري يحتاج لميثريل وكريستال نادر!",
            woodCost = 12,
            stoneCost = 12,
            ironCost = 16,
            crystalCost = 6,
            mythrilCost = 4,
            resultType = "GODBLADE",
            iconAsset = "icons/sword_flame.png"
        )
    )
}
