package com.clashangprpr.app.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LightingColorFilter
import android.graphics.Paint

/** Central bitmap cache so every image is decoded exactly once. */
object Assets {
    private lateinit var appCtx: Context
    private val cache = HashMap<String, Bitmap?>()

    fun init(c: Context) {
        appCtx = c.applicationContext
    }

    @Synchronized
    fun bmp(path: String): Bitmap? {
        if (cache.containsKey(path)) return cache[path]
        var b: Bitmap? = null
        try {
            appCtx.assets.open(path).use { b = BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            b = null
        }
        cache[path] = b
        return b
    }

    /** First path that exists, or null. */
    fun firstExisting(vararg paths: String): String? {
        for (p in paths) {
            if (bmp(p) != null) return p
        }
        return null
    }

    /** Copy of a bitmap with a multiply/add tint baked in (cached). */
    @Synchronized
    fun tinted(path: String, mul: Int, add: Int = 0): Bitmap? {
        val key = "$path|t|$mul|$add"
        if (cache.containsKey(key)) return cache[key]
        val src = bmp(path)
        var out: Bitmap? = null
        if (src != null) {
            out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
            val p = Paint()
            p.colorFilter = LightingColorFilter(mul, add)
            Canvas(out).drawBitmap(src, 0f, 0f, p)
        }
        cache[key] = out
        return out
    }

    /** Copy of a bitmap with an arbitrary colour filter baked in (cached by key). */
    @Synchronized
    fun filtered(path: String, key: String, filter: ColorFilter): Bitmap? {
        val k = "$path|f|$key"
        if (cache.containsKey(k)) return cache[k]
        val src = bmp(path)
        var out: Bitmap? = null
        if (src != null) {
            out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
            val p = Paint()
            p.colorFilter = filter
            Canvas(out).drawBitmap(src, 0f, 0f, p)
        }
        cache[k] = out
        return out
    }
}
