package com.clashangprpr.app.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.util.concurrent.ConcurrentHashMap

object SpriteCache {
    private val bitmapCache = ConcurrentHashMap<String, Bitmap>()

    fun getBitmap(context: Context, assetPath: String): Bitmap? {
        bitmapCache[assetPath]?.let { return it }
        return try {
            context.assets.open(assetPath).use { input ->
                val bmp = BitmapFactory.decodeStream(input)
                if (bmp != null) {
                    bitmapCache[assetPath] = bmp
                }
                bmp
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun clear() {
        bitmapCache.clear()
    }
}
