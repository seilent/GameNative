package app.gamenative.ui.util

import android.graphics.Bitmap
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import java.util.concurrent.ConcurrentHashMap

object GameAccent {
    private val cache = ConcurrentHashMap<String, Int>()

    @Suppress("serial")
    private val blurCache = object : LinkedHashMap<String, Bitmap>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>): Boolean = size > 12
    }

    fun cachedArgb(key: String): Int? = cache[key]

    fun put(key: String, argb: Int) {
        cache[key] = argb
    }

    fun cachedBlur(key: String): Bitmap? = synchronized(blurCache) { blurCache[key] }

    fun putBlur(key: String, bmp: Bitmap) { synchronized(blurCache) { blurCache[key] = bmp } }

    fun accentFromBitmap(bitmap: Bitmap, fallbackArgb: Int): Int {
        val palette = Palette.from(bitmap).generate()
        val swatch = palette.vibrantSwatch
            ?: palette.lightVibrantSwatch
            ?: palette.darkVibrantSwatch
            ?: palette.dominantSwatch
            ?: palette.mutedSwatch
            ?: return fallbackArgb
        return clampAccent(swatch.rgb)
    }

    private fun clampAccent(argb: Int): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(argb, hsl)
        hsl[1] = hsl[1].coerceIn(0.42f, 0.66f)
        hsl[2] = hsl[2].coerceIn(0.56f, 0.72f)
        var color = ColorUtils.HSLToColor(hsl)
        var guard = 0
        while (ColorUtils.calculateLuminance(color) < 0.22 && hsl[2] < 0.80f && guard < 6) {
            hsl[2] += 0.03f
            color = ColorUtils.HSLToColor(hsl)
            guard++
        }
        return color
    }

    /**
     * Dependency-free, all-API box blur (two separable passes ≈ Gaussian). Cheap on a small bitmap.
     * Alpha is forced opaque; only RGB is blurred.
     */
    fun boxBlur(src: Bitmap, radius: Int, passes: Int = 2): Bitmap {
        val w = src.width
        val h = src.height
        if (radius < 1 || w == 0 || h == 0) return src

        val buffer = IntArray(w * h)
        src.getPixels(buffer, 0, w, 0, 0, w, h)
        val scratch = IntArray(w * h)

        repeat(passes) {
            blurHorizontal(buffer, scratch, w, h, radius)
            blurVertical(scratch, buffer, w, h, radius)
        }

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(buffer, 0, w, 0, 0, w, h)
        return out
    }

    private fun blurHorizontal(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int) {
        val div = 2 * r + 1
        for (y in 0 until h) {
            val row = y * w
            var rs = 0
            var gs = 0
            var bs = 0
            for (k in -r..r) {
                val c = src[row + k.coerceIn(0, w - 1)]
                rs += (c shr 16) and 0xFF
                gs += (c shr 8) and 0xFF
                bs += c and 0xFF
            }
            for (x in 0 until w) {
                dst[row + x] = (0xFF shl 24) or ((rs / div) shl 16) or ((gs / div) shl 8) or (bs / div)
                val add = src[row + (x + r + 1).coerceIn(0, w - 1)]
                val sub = src[row + (x - r).coerceIn(0, w - 1)]
                rs += ((add shr 16) and 0xFF) - ((sub shr 16) and 0xFF)
                gs += ((add shr 8) and 0xFF) - ((sub shr 8) and 0xFF)
                bs += (add and 0xFF) - (sub and 0xFF)
            }
        }
    }

    private fun blurVertical(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int) {
        val div = 2 * r + 1
        for (x in 0 until w) {
            var rs = 0
            var gs = 0
            var bs = 0
            for (k in -r..r) {
                val c = src[k.coerceIn(0, h - 1) * w + x]
                rs += (c shr 16) and 0xFF
                gs += (c shr 8) and 0xFF
                bs += c and 0xFF
            }
            for (y in 0 until h) {
                dst[y * w + x] = (0xFF shl 24) or ((rs / div) shl 16) or ((gs / div) shl 8) or (bs / div)
                val add = src[(y + r + 1).coerceIn(0, h - 1) * w + x]
                val sub = src[(y - r).coerceIn(0, h - 1) * w + x]
                rs += ((add shr 16) and 0xFF) - ((sub shr 16) and 0xFF)
                gs += ((add shr 8) and 0xFF) - ((sub shr 8) and 0xFF)
                bs += (add and 0xFF) - (sub and 0xFF)
            }
        }
    }
}
