package com.clashangprpr.app.model

data class CraftRecipe(
    val id: String,
    val titleAr: String,
    val descAr: String,
    val woodCost: Int,
    val stoneCost: Int,
    val ironCost: Int,
    val resultType: String,
    val iconAsset: String
)

object CraftingRegistry {
    val recipes = listOf(
        CraftRecipe(
            "craft_godblade",
            "نصل الأبعاد الكوني الأسطوري (ضرر 160)",
            "أقوى سلاح في التاريخ يطلق هالات أبعاد خارقة لقهر سيد الفراغ",
            woodCost = 5,
            stoneCost = 5,
            ironCost = 6,
            resultType = "GODBLADE",
            iconAsset = "icons/sword_flame.png"
        ),
        CraftRecipe(
            "craft_plasma",
            "مدفع البلازما السايبرنتيكي (ضرر 120)",
            "سلاح مستقبلي يطلق مقذوفات ليزرية خارقة لتدمير الدروع والآلات",
            woodCost = 4,
            stoneCost = 3,
            ironCost = 5,
            resultType = "PLASMA",
            iconAsset = "icons/item_10.png"
        ),
        CraftRecipe(
            "craft_pickaxe",
            "فأس التنقيب والحفر (أداة الكنز)",
            "أداة أساسية لحفر الرمال واستخراج المفتاح المدفون",
            woodCost = 3,
            stoneCost = 2,
            ironCost = 2,
            resultType = "PICKAXE",
            iconAsset = "icons/item_15.png"
        ),
        CraftRecipe(
            "craft_bomb",
            "قنبلة متفجرة (x2)",
            "تسبب ضرراً انفجارياً بـ 120 لجميع الوحوش المحيطة",
            woodCost = 2,
            stoneCost = 1,
            ironCost = 1,
            resultType = "BOMB",
            iconAsset = "icons/item_1.png"
        ),
        CraftRecipe(
            "craft_shield",
            "درع النانو المتطور (+150 صحة)",
            "درع سايبرنتيكي متقدم يرفع صحتك ويصد طلقات الرصاص",
            woodCost = 3,
            stoneCost = 4,
            ironCost = 5,
            resultType = "SHIELD",
            iconAsset = "icons/item_3.png"
        )
    )
}
