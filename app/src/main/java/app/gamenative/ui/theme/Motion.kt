package app.gamenative.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset

/**
 * Central motion language for the glassmorphism UI.
 *
 * Everything here is decelerate-only and critically damped: no bounce, no overshoot.
 * This replaces the scattered `spring(DampingRatioMediumBouncy, StiffnessMedium)` calls
 * that previously lived inline across ~14 files and gave the UI its "jarring" feel.
 *
 * Curve + durations mirror the MIU frontend recipe:
 *   ease  = cubic-bezier(0.22, 1, 0.36, 1)  (expo-out)
 *   fast  = 180ms (exits), base = 280ms (enters), slow = 480ms (backdrop crossfade)
 */
object Motion {
    /** MIU "expo-out": fast start, long gentle settle, zero overshoot. */
    val EaseGlass: Easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

    /** Standard Material emphasis curve, used for plain fades. */
    val EaseStandard: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

    const val DurationFast: Int = 180
    const val DurationBase: Int = 280
    const val DurationSlow: Int = 480

    /** Focus / press scale. Critically damped (dampingRatio = 1f) so it settles without a bounce. */
    val FocusScale: SpringSpec<Float> = spring(dampingRatio = 1f, stiffness = 800f)

    /** Per-game accent color glide (mirrors MIU's ~1s color transition). */
    val AccentColor: SpringSpec<Color> = spring(dampingRatio = 1f, stiffness = 400f)

    /** Animated Dp values (e.g. the tab-bar selection pill), no bounce. */
    val IndicatorDp: SpringSpec<Dp> = spring(dampingRatio = 1f, stiffness = 700f)

    /** Panel / sheet slide offset. */
    val PanelSlide: FiniteAnimationSpec<IntOffset> = tween(durationMillis = 350, easing = EaseGlass)

    /** Generic enter/exit fade. */
    val Fade: FiniteAnimationSpec<Float> = tween(durationMillis = DurationBase, easing = EaseStandard)

    /** Backdrop image crossfade when the focused game changes. */
    val BackdropCrossfade: FiniteAnimationSpec<Float> = tween(durationMillis = 360, easing = EaseGlass)
}
