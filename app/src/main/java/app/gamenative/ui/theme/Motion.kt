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

object Motion {
    val EaseGlass: Easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

    val EaseStandard: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

    const val DurationFast: Int = 180
    const val DurationBase: Int = 280
    const val DurationSlow: Int = 480

    val FocusScale: SpringSpec<Float> = spring(dampingRatio = 1f, stiffness = 800f)

    val AccentColor: SpringSpec<Color> = spring(dampingRatio = 1f, stiffness = 400f)

    val IndicatorDp: SpringSpec<Dp> = spring(dampingRatio = 1f, stiffness = 700f)

    val PanelSlide: FiniteAnimationSpec<IntOffset> = tween(durationMillis = 350, easing = EaseGlass)

    val Fade: FiniteAnimationSpec<Float> = tween(durationMillis = DurationBase, easing = EaseStandard)

    val BackdropCrossfade: FiniteAnimationSpec<Float> = tween(durationMillis = 360, easing = EaseGlass)
}
