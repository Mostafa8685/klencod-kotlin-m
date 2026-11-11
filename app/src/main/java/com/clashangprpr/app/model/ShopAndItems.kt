package com.clashangprpr.app.model

import android.graphics.Color

data class Weapon(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val damage: Int,
    val price: Int,
    val color: Int
)

data class ShopItem(
    val id: String,
    val titleAr: String,
    val descAr: String,
    val price: Int,
    val type: ItemType
)

enum class ItemType {
    WEAPON, POTION, ARMOR
}

object GameCatalogue {
    val WEAPON_BASIC = Weapon("sword_basic", "سيف الفجر", "Sword of Dawn", 30, 0, Color.WHITE)
    val WEAPON_FIRE = Weapon("sword_fire", "نصل النار الملتهب", "Flaming Blade", 55, 60, Color.parseColor("#FF5722"))
    val WEAPON_CHRONO = Weapon("spear_chrono", "رمح كرونو الأسطوري", "Chrono Glaive", 85, 120, Color.parseColor("#00E5FF"))

    val shopItems = listOf(
        ShopItem(
            "potion_heal",
            "جرعة شفاء كبرى (+70 صحة)",
            "تستعيد صحة الفارس فوراً في المعركة",
            25,
            ItemType.POTION
        ),
        ShopItem(
            "sword_fire",
            "نصل النار الملتهب (ضرر 55)",
            "سيف حارق مضاعف القوة ضد وحوش العصور",
            60,
            ItemType.WEAPON
        ),
        ShopItem(
            "armor_royal",
            "درع الفارس الملكي (+100 أقصى صحة)",
            "يزيد نقاط صحتك القصوى ويقوي دفاعك",
            80,
            ItemType.ARMOR
        ),
        ShopItem(
            "spear_chrono",
            "رمح كرونو الأسطوري (ضرر 85)",
            "أقوى سلاح في اللعبة لضرب وتدمير الزعماء بسرعة",
            120,
            ItemType.WEAPON
        )
    )
}
