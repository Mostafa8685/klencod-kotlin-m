package com.clashangprpr.app.engine

import android.content.Context
import android.content.SharedPreferences

/** Simple key/value save so progress survives app restarts. */
object SaveStore {
    private var prefs: SharedPreferences? = null

    fun init(c: Context) {
        prefs = c.applicationContext.getSharedPreferences("chrono_knight_save", Context.MODE_PRIVATE)
    }

    fun hasSave(): Boolean = prefs?.getBoolean("has", false) ?: false

    fun putAll(values: Map<String, Any>, markSave: Boolean = true) {
        val e = prefs?.edit() ?: return
        for ((k, v) in values) {
            when (v) {
                is Int -> e.putInt(k, v)
                is Boolean -> e.putBoolean(k, v)
                is String -> e.putString(k, v)
                is Float -> e.putFloat(k, v)
            }
        }
        if (markSave) e.putBoolean("has", true)
        e.apply()
    }

    fun float(k: String, d: Float): Float = prefs?.getFloat(k, d) ?: d
    fun int(k: String, d: Int): Int = prefs?.getInt(k, d) ?: d
    fun bool(k: String, d: Boolean): Boolean = prefs?.getBoolean(k, d) ?: d
    fun str(k: String, d: String): String = prefs?.getString(k, d) ?: d

    fun clear() {
        val soundSfx = bool("sfx", true)
        val soundMusic = bool("music", true)
        val lowq = bool("lowq", false)
        val unlocked = bool("ow_unlocked", false)
        prefs?.edit()?.clear()?.putBoolean("sfx", soundSfx)?.putBoolean("music", soundMusic)
            ?.putBoolean("lowq", lowq)?.putBoolean("ow_unlocked", unlocked)?.apply()
    }
}
