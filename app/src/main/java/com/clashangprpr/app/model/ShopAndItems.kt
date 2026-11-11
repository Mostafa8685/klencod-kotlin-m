package com.clashangprpr.app.model

import android.graphics.Color

data class Weapon(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val damage: Int,
    val price: Int,
    val color: Int,
    val iconAsset: String
)

data class ShopItem(
    val id: String,
    val titleAr: String,
    val descAr: String,
    val price: Int,
    val type: ItemType,
    val iconAsset: String
)

enum class ItemType {
    WEAPON, POTION, ARMOR
}

object GameCatalogue {
    val WEAPON_BASIC = Weapon("sword_basic", "سيف الفجر", "Sword of Dawn", 35, 0, Color.WHITE, "icons/sword_scimitar.png")
    val WEAPON_FIRE = Weapon("sword_fire", "نصل النار الملتهب", "Flaming Blade", 60, 60, Color.parseColor("#FF5722"), "icons/sword_flame.png")
    val WEAPON_CHRONO = Weapon("spear_chrono", "رمح كرونو الأسطوري", "Chrono Glaive", 95, 120, Color.parseColor("#00E5FF"), "icons/item_10.png")

    val shopItems = listOf(
        ShopItem(
            "potion_heal",
            "جرعة شفاء كبرى (+80 صحة)",
            "تستعيد صحة الفارس فوراً في المعركة",
            25,
            ItemType.POTION,
            "icons/potion_health.png"
        ),
        ShopItem(
            "sword_fire",
            "نصل النار الملتهب (ضرر 60)",
            "سيف حارق مضاعف القوة مع هالة نارية",
            60,
            ItemType.WEAPON,
            "icons/sword_flame.png"
        ),
        ShopItem(
            "armor_royal",
            "درع الفارس الملكي (+100 صحة)",
            "يزيد نقاط صحتك القصوى ويقوي دفاعك",
            80,
            ItemType.ARMOR,
            "icons/item_3.png"
        ),
        ShopItem(
            "spear_chrono",
            "رمح كرونو الأسطوري (ضرر 95)",
            "أقوى سلاح لضرب وتدمير الزعماء بسرعة",
            120,
            ItemType.WEAPON,
            "icons/item_10.png"
        )
    )
}
