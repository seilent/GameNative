package app.gamenative.utils

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

private const val DEFAULT_FPS_LIMITER_MAX_HZ = 60

internal fun detectMaxRefreshRateHz(context: Context, attachedView: View? = null): Int {
    val display = attachedView?.display
        ?: context.display
        ?: ContextCompat.getSystemService(context, DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY)

    val refreshRate = when {
        display == null -> DEFAULT_FPS_LIMITER_MAX_HZ.toFloat()
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
            val supportedMax = display.supportedModes.maxOfOrNull { it.refreshRate } ?: display.refreshRate
            if (supportedMax.isFinite() && supportedMax > 0f) supportedMax else display.refreshRate
        }
        else -> display.refreshRate
    }

    return refreshRate
        .takeIf { it.isFinite() && it > 0f }
        ?.roundToInt()
        ?.coerceAtLeast(5)
        ?: DEFAULT_FPS_LIMITER_MAX_HZ
}
