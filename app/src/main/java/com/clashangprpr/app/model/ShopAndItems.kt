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
    WEAPON, POTION, ARMOR, AMMO, FOOD
}

object GameCatalogue {
    val WEAPON_BASIC = Weapon("sword_basic", "سيف الفجر", "Sword of Dawn", 35, 0, Color.WHITE, "icons/sword_scimitar.png")
    val WEAPON_PIRATE = Weapon("sword_pirate", "سيف القبطان المسنن", "Pirate Cutlass", 55, 140, Color.parseColor("#FFB300"), "icons/pirate_1.png")
    val WEAPON_FIRE = Weapon("sword_fire", "نصل النار الملتهب", "Flaming Blade", 75, 260, Color.parseColor("#FF5722"), "icons/sword_flame.png")
    val WEAPON_CHRONO = Weapon("spear_chrono", "رمح كرونو الأسطوري", "Chrono Glaive", 105, 480, Color.parseColor("#00E5FF"), "icons/item_10.png")
    val WEAPON_PLASMA = Weapon("weapon_plasma", "بندقية البلازما", "Plasma Rifle", 135, 0, Color.parseColor("#76FF03"), "icons/pirate_5.png")
    val WEAPON_GODBLADE = Weapon("weapon_godblade", "نصل الأبعاد الكوني", "Omega God-Blade", 190, 0, Color.parseColor("#E040FB"), "icons/sword_flame.png")

    fun getWeaponById(id: String): Weapon {
        return when (id) {
            WEAPON_PIRATE.id -> WEAPON_PIRATE
            WEAPON_FIRE.id -> WEAPON_FIRE
            WEAPON_CHRONO.id -> WEAPON_CHRONO
            WEAPON_PLASMA.id -> WEAPON_PLASMA
            WEAPON_GODBLADE.id -> WEAPON_GODBLADE
            else -> WEAPON_BASIC
        }
    }

    val shopItems = listOf(
        ShopItem(
            "ammo_pack",
            "صندوق ذخيرة المسدس (+20 طلقة)",
            "طلقات حقيقية للمسدس منفصلة عن طاقة السحر",
            35,
            ItemType.AMMO,
            "icons/ammo_bullet.png"
        ),
        ShopItem(
            "food_ration",
            "وجبة المسافر (+2 طعام +3 بذور)",
            "تملأ الجوع وتوفر بذوراً للزراعة في القرية",
            30,
            ItemType.FOOD,
            "icons/pirate_3.png"
        ),
        ShopItem(
            "potion_heal",
            "جرعة شفاء كبرى (x2)",
            "تستعيد 85 من صحة الفارس فوراً",
            40,
            ItemType.POTION,
            "icons/potion_health.png"
        ),
        ShopItem(
            "sword_pirate",
            "سيف القبطان المسنن (ضرر 55)",
            "سلاح بحري قوي وسريع للمعارك المتوسطة",
            140,
            ItemType.WEAPON,
            "icons/pirate_1.png"
        ),
        ShopItem(
            "sword_fire",
            "نصل النار الملتهب (ضرر 75)",
            "سيف حارق مضاعف القوة مع هالة نارية",
            260,
            ItemType.WEAPON,
            "icons/sword_flame.png"
        ),
        ShopItem(
            "armor_royal",
            "درع الفارس الملكي (صحة 250)",
            "يرفع الصحة القصوى ويقلل الضرر 30%",
            320,
            ItemType.ARMOR,
            "icons/item_3.png"
        ),
        ShopItem(
            "spear_chrono",
            "رمح كرونو الأسطوري (ضرر 105)",
            "سلاح ثقيل لاختراق دروع الزعماء الكبار",
            480,
            ItemType.WEAPON,
            "icons/item_10.png"
        )
    )
}
