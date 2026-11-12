package com.clashangprpr.app.engine

import android.content.Context
import com.clashangprpr.app.model.EraType
import com.clashangprpr.app.model.GameCatalogue
import com.clashangprpr.app.model.Player
import com.clashangprpr.app.systems.ProgressionSystem

object SaveManager {
    private const val PREFS_NAME = "chrono_knight_save_v2"

    fun hasSavedGame(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("has_save", false)
    }

    fun getSavedEra(context: Context): EraType {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ordinal = prefs.getInt("era_ordinal", 0).coerceIn(0, EraType.values().size - 1)
        return EraType.values()[ordinal]
    }

    fun getCompletedErasMask(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt("completed_eras_mask", 0)
    }

    fun saveGame(
        context: Context,
        era: EraType,
        player: Player,
        completedErasMask: Int = getCompletedErasMask(context),
        prog: ProgressionSystem? = null
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ed = prefs.edit()
            .putBoolean("has_save", true)
            .putInt("era_ordinal", era.ordinal)
            .putInt("completed_eras_mask", completedErasMask)
            .putInt("hp", player.hp.coerceAtLeast(1))
            .putInt("max_hp", player.maxHp)
            .putFloat("mana", player.mana)
            .putInt("coins", player.coins)
            .putInt("wood", player.wood)
            .putInt("stone", player.stone)
            .putInt("iron", player.iron)
            .putInt("bombs", player.bombs)
            .putInt("potions", player.potions)
            .putString("weapon_id", player.equippedWeapon.id)
            .putBoolean("has_royal_armor", player.hasRoyalArmor)
            .putBoolean("has_nano_armor", player.hasNanoArmor)
            .putBoolean("has_pickaxe", player.hasPickaxe)
            .putBoolean("has_plasma", player.hasPlasmaCannon)
            .putBoolean("has_godblade", player.hasGodBlade)
            .putBoolean("sound_muted", SoundEngine.isMuted)
            .putFloat("sfx_vol", SoundEngine.sfxVolume)
            .putFloat("music_vol", SoundEngine.musicVolume)

        if (prog != null) {
            ed.putInt("lvl", prog.level)
            ed.putInt("xp", prog.xp)
            ed.putInt("xp_next", prog.xpToNext)
            ed.putInt("skill_pts", prog.skillPoints)
            ed.putInt("wep_upg", prog.weaponUpgradeLevel)
            ed.putInt("pot_spd", prog.speedPotions)
            ed.putInt("pot_str", prog.strengthPotions)
            ed.putInt("pot_mana", prog.manaPotions)
            ed.putInt("pot_invis", prog.invisPotions)
            ed.putBoolean("has_clock_hand", prog.hasClockHand)
            ed.putInt("sage_rescues", prog.sageRescueCount)
            for (sk in prog.skills) {
                ed.putInt("sk_${sk.id}", sk.level)
            }
            for (q in prog.quests) {
                ed.putInt("q_cnt_${q.id}", q.currentCount)
                ed.putBoolean("q_done_${q.id}", q.isCompleted)
            }
            for (a in prog.achievements) {
                ed.putBoolean("ach_${a.id}", a.isUnlocked)
            }
        }
        ed.apply()
    }

    fun loadPlayerState(context: Context, player: Player, prog: ProgressionSystem? = null): EraType {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ordinal = prefs.getInt("era_ordinal", 0).coerceIn(0, EraType.values().size - 1)
        player.coins = prefs.getInt("coins", 70)
        player.wood = prefs.getInt("wood", 6)
        player.stone = prefs.getInt("stone", 6)
        player.iron = prefs.getInt("iron", 5)
        player.bombs = prefs.getInt("bombs", 2)
        player.potions = prefs.getInt("potions", 3)
        player.hasRoyalArmor = prefs.getBoolean("has_royal_armor", false)
        player.hasNanoArmor = prefs.getBoolean("has_nano_armor", false)
        player.hasPickaxe = prefs.getBoolean("has_pickaxe", false)
        player.hasPlasmaCannon = prefs.getBoolean("has_plasma", false)
        player.hasGodBlade = prefs.getBoolean("has_godblade", false)
        player.equippedWeapon = GameCatalogue.getWeaponById(prefs.getString("weapon_id", "sword_basic") ?: "sword_basic")

        SoundEngine.isMuted = prefs.getBoolean("sound_muted", false)
        SoundEngine.sfxVolume = prefs.getFloat("sfx_vol", 1.0f)
        SoundEngine.musicVolume = prefs.getFloat("music_vol", 0.5f)

        if (prog != null) {
            prog.level = prefs.getInt("lvl", 1)
            prog.xp = prefs.getInt("xp", 0)
            prog.xpToNext = prefs.getInt("xp_next", 100)
            prog.skillPoints = prefs.getInt("skill_pts", 1)
            prog.weaponUpgradeLevel = prefs.getInt("wep_upg", 0)
            prog.speedPotions = prefs.getInt("pot_spd", 1)
            prog.strengthPotions = prefs.getInt("pot_str", 1)
            prog.manaPotions = prefs.getInt("pot_mana", 1)
            prog.invisPotions = prefs.getInt("pot_invis", 0)
            prog.hasClockHand = prefs.getBoolean("has_clock_hand", false)
            prog.sageRescueCount = prefs.getInt("sage_rescues", 0)
            for (sk in prog.skills) {
                sk.level = prefs.getInt("sk_${sk.id}", 0)
            }
            for (q in prog.quests) {
                q.currentCount = prefs.getInt("q_cnt_${q.id}", 0)
                q.isCompleted = prefs.getBoolean("q_done_${q.id}", false)
            }
            for (a in prog.achievements) {
                a.isUnlocked = prefs.getBoolean("ach_${a.id}", false)
            }
            player.maxMana = 100f + prog.getBonusMaxMana()
        }

        player.recalculateMaxHp(prog?.getBonusMaxHp() ?: 0)
        player.hp = prefs.getInt("hp", player.maxHp).coerceIn(1, player.maxHp)
        player.mana = prefs.getFloat("mana", player.maxMana).coerceIn(0f, player.maxMana)
        return EraType.values()[ordinal]
    }

    fun clearSave(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
