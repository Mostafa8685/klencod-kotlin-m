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
    val WEAPON_PLASMA = Weapon("weapon_plasma", "مدفع البلازما السايبرنتيكي", "Plasma Blaster", 120, 0, Color.parseColor("#76FF03"), "icons/item_10.png")
    val WEAPON_GODBLADE = Weapon("weapon_godblade", "نصل الأبعاد الكوني", "Omega God-Blade", 160, 0, Color.parseColor("#E040FB"), "icons/sword_flame.png")

    fun getWeaponById(id: String): Weapon {
        return when (id) {
            WEAPON_FIRE.id -> WEAPON_FIRE
            WEAPON_CHRONO.id -> WEAPON_CHRONO
            WEAPON_PLASMA.id -> WEAPON_PLASMA
            WEAPON_GODBLADE.id -> WEAPON_GODBLADE
            else -> WEAPON_BASIC
        }
    }

    val shopItems = listOf(
        ShopItem(
            "potion_heal",
            "جرعة شفاء كبرى (x2)",
            "تستعيد 85 من صحة الفارس فوراً",
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
            "درع الفارس الملكي (صحة 250)",
            "يرفع الصحة القصوى إلى 250 ويقلل الضرر 30%",
            80,
            ItemType.ARMOR,
            "icons/item_3.png"
        ),
        ShopItem(
            "spear_chrono",
            "رمح كرونو الأسطوري (ضرر 95)",
            "أقوى سلاح متاح لدى التاجر لضرب الزعماء",
            120,
            ItemType.WEAPON,
            "icons/item_10.png"
        )
    )
}
