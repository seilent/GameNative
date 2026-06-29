package app.gamenative.utils

import com.winlator.container.Container
import java.util.Locale

object SeifgQuickMenuHelper {
    data class Settings(
        val multiplier: Int,
        val flowScale: Float,
        val performanceMode: Boolean,
    )

    fun isAvailable(container: Container): Boolean =
        SeifgManager.isSupported(container) && SeifgManager.isArmed(container)

    fun readSettings(container: Container): Settings = Settings(
        multiplier = SeifgManager.multiplier(container),
        flowScale = SeifgManager.flowScale(container),
        performanceMode = SeifgManager.performanceMode(container),
    )

    fun sanitizeMultiplier(multiplier: Int): Int =
        if (multiplier < 2) 0 else multiplier.coerceIn(2, 4)

    fun sanitizeFlowScale(flowScale: Float): Float =
        flowScale.coerceIn(0.25f, 1.0f)

    fun applySettings(container: Container, settings: Settings) {
        val multiplier = sanitizeMultiplier(settings.multiplier)
        val flowScale = sanitizeFlowScale(settings.flowScale)

        container.putExtra(SeifgManager.EXTRA_MULTIPLIER, multiplier.toString())
        container.putExtra(SeifgManager.EXTRA_FLOW_SCALE, String.format(Locale.US, "%.2f", flowScale))
        container.putExtra(SeifgManager.EXTRA_PERFORMANCE_MODE, settings.performanceMode.toString())
        container.saveData()
    }
}
