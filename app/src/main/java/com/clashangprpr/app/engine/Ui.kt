package com.clashangprpr.app.engine

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import kotlin.math.min

/**
 * Tiny immediate-mode UI toolkit styled with clean Minecraft beveled stone panels & buttons.
 */
object Ui {
    var u = 1f
    var w = 1280f
    var h = 720f

    private val fillP = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val textP = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
    private val iconP = Paint()
    private val tmp = RectF()
    private val filters = HashMap<Int, PorterDuffColorFilter>()

    private val layoutCache = object : LinkedHashMap<String, StaticLayout>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, StaticLayout>?): Boolean {
            return size > 90
        }
    }

    fun init(width: Float, height: Float) {
        w = width
        h = height
        u = min(width, height) / 720f
    }

    fun darken(col: Int, f: Float): Int {
        return Color.argb(
            Color.alpha(col),
            (Color.red(col) * f).toInt().coerceIn(0, 255),
            (Color.green(col) * f).toInt().coerceIn(0, 255),
            (Color.blue(col) * f).toInt().coerceIn(0, 255)
        )
    }

    fun brighten(col: Int, add: Int): Int {
        return Color.argb(
            Color.alpha(col),
            (Color.red(col) + add).coerceIn(0, 255),
            (Color.green(col) + add).coerceIn(0, 255),
            (Color.blue(col) + add).coerceIn(0, 255)
        )
    }

    fun withAlpha(col: Int, a: Int): Int = Color.argb(a.coerceIn(0, 255), Color.red(col), Color.green(col), Color.blue(col))

    private fun filterFor(col: Int): PorterDuffColorFilter {
        var f = filters[col]
        if (f == null) {
            f = PorterDuffColorFilter(col, PorterDuff.Mode.SRC_IN)
            filters[col] = f
        }
        return f
    }

    fun measure(s: String, size: Float, bold: Boolean = true): Float {
        textP.textSize = size * u
        textP.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        return textP.measureText(s)
    }

    /** Single line text with crisp Minecraft drop-shadow. */
    fun text(
        c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int,
        align: Paint.Align = Paint.Align.CENTER, bold: Boolean = true, shadow: Boolean = true
    ) {
        textP.textSize = size * u
        textP.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        textP.textAlign = align
        if (shadow) {
            textP.color = Color.argb(230, 12, 12, 16)
            c.drawText(s, x + 2.0f * u, y + 2.0f * u, textP)
        }
        textP.color = color
        c.drawText(s, x, y, textP)
    }

    /** Wrapped paragraph (RTL aware). */
    fun wrapped(
        c: Canvas, s: String, x: Float, y: Float, width: Float, size: Float, color: Int,
        bold: Boolean = false, maxLines: Int = 6, center: Boolean = false, cache: Boolean = true
    ) {
        val wi = width.toInt().coerceAtLeast(1)
        val key = "$s|$size|$wi|$color|$bold|$maxLines|$center"
        var lay = if (cache) layoutCache[key] else null
        if (lay == null) {
            val tp = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
            tp.textSize = size * u
            tp.color = color
            tp.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            lay = StaticLayout.Builder.obtain(s, 0, s.length, tp, wi)
                .setAlignment(if (center) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL)
                .setMaxLines(maxLines)
                .setEllipsize(TextUtils.TruncateAt.END)
                .setLineSpacing(0f, 1.12f)
                .build()
            if (cache) layoutCache[key] = lay
        }
        c.save()
        c.translate(x, y)
        lay!!.draw(c)
        c.restore()
    }

    /**
     * Minecraft-style beveled stone panel:
     * crisp outer dark frame, top/left light bevel, bottom/right dark bevel, and clean slate interior.
     */
    fun panel(c: Canvas, r: RectF, accent: Int, bg: Int = Color.argb(242, 34, 36, 44), radius: Float = 6f) {
        val b = 3f * u
        fillP.style = Paint.Style.FILL
        fillP.color = Color.argb(120, 0, 0, 0)
        c.drawRect(r.left + 4f * u, r.top + 5f * u, r.right + 4f * u, r.bottom + 5f * u, fillP)
        fillP.color = Color.rgb(14, 15, 20)
        c.drawRect(r.left - b, r.top - b, r.right + b, r.bottom + b, fillP)
        fillP.color = bg
        c.drawRect(r, fillP)
        fillP.color = brighten(accent, 45)
        c.drawRect(r.left, r.top, r.right, r.top + b, fillP)
        c.drawRect(r.left, r.top, r.left + b, r.bottom, fillP)
        fillP.color = darken(accent, 0.48f)
        c.drawRect(r.left, r.bottom - b, r.right, r.bottom, fillP)
        c.drawRect(r.right - b, r.top, r.right, r.bottom, fillP)
        strokeP.color = Color.argb(95, 0, 0, 0)
        strokeP.strokeWidth = 1.5f * u
        c.drawRect(r.left + b * 1.4f, r.top + b * 1.4f, r.right - b * 1.4f, r.bottom - b * 1.4f, strokeP)
    }

    /**
     * Classic Minecraft-style 3D beveled stone button with crisp top-left highlight & bottom-right shadow.
     */
    fun button(
        c: Canvas, r: RectF, label: String, color: Int, enabled: Boolean = true,
        size: Float = 20f, pressed: Boolean = false
    ) {
        val b = 2.6f * u
        val base = if (enabled) color else Color.rgb(74, 76, 84)
        val dy = if (pressed) 2f * u else 0f
        fillP.style = Paint.Style.FILL
        fillP.color = Color.rgb(12, 12, 16)
        c.drawRect(r.left - 2f * u, r.top + dy - 2f * u, r.right + 2f * u, r.bottom + dy + 3f * u, fillP)
        fillP.color = base
        c.drawRect(r.left, r.top + dy, r.right, r.bottom + dy, fillP)
        fillP.color = if (pressed) darken(base, 0.65f) else brighten(base, 58)
        c.drawRect(r.left, r.top + dy, r.right - b, r.top + dy + b, fillP)
        c.drawRect(r.left, r.top + dy, r.left + b, r.bottom + dy - b, fillP)
        fillP.color = if (pressed) brighten(base, 35) else darken(base, 0.45f)
        c.drawRect(r.left + b, r.bottom + dy - b * 1.3f, r.right, r.bottom + dy, fillP)
        c.drawRect(r.right - b, r.top + dy + b, r.right, r.bottom + dy, fillP)

        val txtCol = if (!enabled) Color.rgb(165, 168, 178) else if (pressed) Color.parseColor("#FFFFA0") else Color.WHITE
        text(c, label, r.centerX(), r.centerY() + dy + size * u * 0.35f, size, txtCol)
    }

    fun bar(c: Canvas, r: RectF, frac: Float, fill: Int, bg: Int = Color.argb(225, 14, 16, 22)) {
        fillP.style = Paint.Style.FILL
        fillP.color = bg
        c.drawRect(r, fillP)
        val f = frac.coerceIn(0f, 1f)
        if (f > 0.01f) {
            tmp.set(r.left + 2f * u, r.top + 2f * u, r.left + 2f * u + (r.width() - 4f * u) * f, r.bottom - 2f * u)
            if (tmp.width() > 0f) {
                fillP.color = fill
                c.drawRect(tmp, fillP)
                fillP.color = Color.argb(75, 255, 255, 255)
                c.drawRect(tmp.left, tmp.top, tmp.right, tmp.top + tmp.height() * 0.38f, fillP)
            }
        }
        strokeP.color = Color.rgb(20, 22, 28)
        strokeP.strokeWidth = 2f * u
        c.drawRect(r, strokeP)
    }

    fun icon(c: Canvas, path: String, cx: Float, cy: Float, size: Float, tint: Int? = null, alpha: Int = 255) {
        val b = Assets.bmp(path) ?: return
        val s = size * u
        iconP.alpha = alpha
        iconP.colorFilter = if (tint != null) filterFor(tint) else null
        tmp.set(cx - s / 2f, cy - s / 2f, cx + s / 2f, cy + s / 2f)
        c.drawBitmap(b, null, tmp, iconP)
    }

    fun iconTinted(c: Canvas, path: String, cx: Float, cy: Float, size: Float, mul: Int) {
        val b = Assets.tinted(path, mul) ?: return
        val s = size * u
        iconP.alpha = 255
        iconP.colorFilter = null
        tmp.set(cx - s / 2f, cy - s / 2f, cx + s / 2f, cy + s / 2f)
        c.drawBitmap(b, null, tmp, iconP)
    }

    fun fillRound(c: Canvas, r: RectF, color: Int, radius: Float = 6f) {
        fillP.style = Paint.Style.FILL
        fillP.color = color
        c.drawRoundRect(r, radius * u, radius * u, fillP)
    }

    fun strokeRound(c: Canvas, r: RectF, color: Int, width: Float = 2f, radius: Float = 6f) {
        strokeP.color = color
        strokeP.strokeWidth = width * u
        c.drawRoundRect(r, radius * u, radius * u, strokeP)
    }

    fun fillRect(c: Canvas, l: Float, t: Float, r: Float, b: Float, color: Int) {
        fillP.style = Paint.Style.FILL
        fillP.color = color
        c.drawRect(l, t, r, b, fillP)
    }

    fun circle(c: Canvas, cx: Float, cy: Float, r: Float, color: Int) {
        fillP.style = Paint.Style.FILL
        fillP.color = color
        c.drawCircle(cx, cy, r, fillP)
    }

    fun ring(c: Canvas, cx: Float, cy: Float, r: Float, color: Int, width: Float) {
        strokeP.color = color
        strokeP.strokeWidth = width
        c.drawCircle(cx, cy, r, strokeP)
    }

    fun pie(c: Canvas, cx: Float, cy: Float, r: Float, sweep: Float, color: Int) {
        fillP.style = Paint.Style.FILL
        fillP.color = color
        tmp.set(cx - r, cy - r, cx + r, cy + r)
        c.drawArc(tmp, -90f, sweep, true, fillP)
    }
}
