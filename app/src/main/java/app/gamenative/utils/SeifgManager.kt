package app.gamenative.utils

import android.content.Context
import com.winlator.container.Container
import com.winlator.core.envvars.EnvVars
import java.util.Locale
import timber.log.Timber
import kotlin.jvm.JvmStatic

object SeifgManager {
    private const val TAG = "SeifgManager"

    const val EXTRA_ARMED = "lsfgEnabled"
    const val EXTRA_MULTIPLIER = "lsfgMultiplier"
    const val EXTRA_FLOW_SCALE = "lsfgFlowScale"
    const val EXTRA_PERFORMANCE_MODE = "lsfgPerformanceMode"
    const val EXTRA_BASE_FPS_CAP = "lsfgBaseFpsCap"

    const val SEIFG_BASE_FPS_CAP = 30

    const val HOST_SIDE_FRAMEGEN = true

    private const val ENV_DISABLE = "DISABLE_SEIFG"
    private const val ENV_CONFIG = "SEIFG_CONFIG"
    private const val ENV_PROCESS = "SEIFG_PROCESS"

    @JvmStatic
    fun isSupported(container: Container): Boolean =
        container.containerVariant.equals(Container.BIONIC, ignoreCase = true)

    @JvmStatic
    fun isArmed(container: Container): Boolean =
        isSupported(container) &&
            parseBool(container.getExtra(EXTRA_ARMED, "false"))

    fun multiplier(container: Container): Int {
        val raw = container.getExtra(EXTRA_MULTIPLIER, "2").toIntOrNull() ?: 2
        return if (raw == 0) 0 else raw.coerceIn(2, 4)
    }

    fun flowScale(container: Container): Float =
        container.getExtra(EXTRA_FLOW_SCALE, "0.30").toFloatOrNull()?.coerceIn(0.25f, 1.0f) ?: 0.30f

    fun performanceMode(container: Container): Boolean =
        parseBool(container.getExtra(EXTRA_PERFORMANCE_MODE, "true"))

    fun baseFpsCap(container: Container): Int =
        container.getExtra(EXTRA_BASE_FPS_CAP, SEIFG_BASE_FPS_CAP.toString())
            .toIntOrNull()?.coerceAtLeast(0) ?: SEIFG_BASE_FPS_CAP

    @JvmStatic
    fun applyLaunchEnv(container: Container, envVars: EnvVars): Boolean {
        envVars.remove(ENV_DISABLE)
        envVars.remove(ENV_CONFIG)
        envVars.remove(ENV_PROCESS)

        if (!isSupported(container)) return false

        val armed = parseBool(container.getExtra(EXTRA_ARMED, "false"))
        if (!armed) {
            Timber.tag(TAG).i("SEIFG disabled")
            return false
        }

        Timber.tag(TAG).i(
            "SEIFG armed: multiplier=%d, flowScale=%.2f, perf=%s",
            multiplier(container), flowScale(container),
            if (performanceMode(container)) "on" else "off"
        )

        applyRealFrameCap(container, envVars)
        return true
    }

    @JvmStatic
    fun applyRealFrameCap(container: Container, envVars: EnvVars) {
        val mult = multiplier(container)
        if (mult < 2) return
        val cap = baseFpsCap(container)
        if (cap <= 0) return
        envVars.put("DXVK_FRAME_RATE", cap.toString())
        envVars.put("VKD3D_FRAME_RATE", cap.toString())
        Timber.tag(TAG).d("Real-frame cap set to %d", cap)
    }

    private fun parseBool(value: String): Boolean =
        value.equals("true", ignoreCase = true) || value == "1"

    private fun formatFlowScale(value: Float): String =
        String.format(Locale.US, "%.2f", value.coerceIn(0.25f, 1.0f))
}
