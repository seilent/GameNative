package app.gamenative.utils

import com.winlator.container.Container
import com.winlator.core.envvars.EnvVars
import timber.log.Timber
import kotlin.jvm.JvmStatic

object SeifgManager {
    private const val TAG = "SeifgManager"

    const val EXTRA_ARMED = "seifgEnabled"
    const val EXTRA_MULTIPLIER = "seifgMultiplier"
    const val EXTRA_QUALITY = "seifgQuality"
    const val EXTRA_BASE_FPS_CAP = "seifgBaseFpsCap"
    const val EXTRA_TARGET_FPS = "seifgTargetFps"

    const val SEIFG_BASE_FPS_CAP = 30
    const val SEIFG_TARGET_FPS = 60

    const val HOST_SIDE_FRAMEGEN = true

    private const val ENV_DISABLE = "DISABLE_SEIFG"
    private const val ENV_CONFIG = "SEIFG_CONFIG"
    private const val ENV_PROCESS = "SEIFG_PROCESS"

    @JvmStatic
    fun isSupported(container: Container): Boolean =
        container.containerVariant.equals(Container.BIONIC, ignoreCase = true) &&
            rendererSupportsFramegen(container)

    private fun rendererSupportsFramegen(container: Container): Boolean =
        container.graphicsDriver != "virgl" &&
            container.displayRenderer.equals(Container.DEFAULT_DISPLAY_RENDERER, ignoreCase = true)

    @JvmStatic
    fun isArmed(container: Container): Boolean =
        isSupported(container) &&
            parseBool(container.getExtra(EXTRA_ARMED, "false"))

    @JvmStatic
    fun isAvailable(container: Container): Boolean =
        isSupported(container) && isArmed(container)

    fun multiplier(container: Container): Int {
        val raw = container.getExtra(EXTRA_MULTIPLIER, "2").toIntOrNull() ?: 2
        return if (raw == 0) 0 else raw.coerceIn(2, 4)
    }

    fun quality(container: Container): Int =
        container.getExtra(EXTRA_QUALITY, "2").toIntOrNull()?.coerceIn(0, 4) ?: 2

    fun targetFps(container: Container): Int =
        container.getExtra(EXTRA_TARGET_FPS, SEIFG_TARGET_FPS.toString())
            .toIntOrNull()?.coerceIn(30, 240) ?: SEIFG_TARGET_FPS

    fun baseFpsCap(container: Container): Int {
        val m = multiplier(container).coerceAtLeast(2)
        return (targetFps(container) / m).coerceAtLeast(1)
    }

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
            "SEIFG armed: multiplier=%d, quality=%d",
            multiplier(container), quality(container)
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

}
