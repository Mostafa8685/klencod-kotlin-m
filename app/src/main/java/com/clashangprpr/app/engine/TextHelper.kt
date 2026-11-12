package com.clashangprpr.app.engine

import android.graphics.Canvas
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint

object TextHelper {
    fun drawWrappedRTL(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        maxWidth: Int,
        paint: TextPaint,
        align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
    ) {
        if (text.isEmpty() || maxWidth <= 10) return
        val layout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, paint, maxWidth)
                .setAlignment(align)
                .setLineSpacing(4f, 1.0f)
                .setIncludePad(false)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(text, paint, maxWidth, align, 1.0f, 4f, false)
        }
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }
}
