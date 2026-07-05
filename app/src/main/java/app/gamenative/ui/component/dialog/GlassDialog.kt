package app.gamenative.ui.component.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.gamenative.ui.component.GlassSurface
import app.gamenative.ui.theme.GlassFillStrong
import app.gamenative.ui.theme.Motion

@Composable
fun GlassDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) {
    val transitionState = remember { MutableTransitionState(false) }
    transitionState.targetState = visible

    if (transitionState.currentState || transitionState.targetState) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = properties,
        ) {
            AnimatedVisibility(
                visibleState = transitionState,
                enter = fadeIn(tween(Motion.DurationBase, easing = Motion.EaseGlass)) +
                    scaleIn(
                        initialScale = 0.92f,
                        animationSpec = tween(Motion.DurationBase, easing = Motion.EaseGlass),
                    ),
                exit = fadeOut(tween(Motion.DurationFast, easing = Motion.EaseStandard)) +
                    scaleOut(
                        targetScale = 0.96f,
                        animationSpec = tween(Motion.DurationFast, easing = Motion.EaseStandard),
                    ),
            ) {
                content()
            }
        }
    }
}

@Composable
fun GlassAlertDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    properties: DialogProperties = DialogProperties(),
) {
    GlassDialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        GlassSurface(
            modifier = modifier
                .widthIn(max = 560.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            fill = GlassFillStrong,
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (icon != null) {
                    icon()
                    Spacer(Modifier.height(16.dp))
                }

                if (title != null) {
                    ProvideTextStyle(MaterialTheme.typography.headlineSmall) {
                        Box(Modifier.align(if (icon != null) Alignment.CenterHorizontally else Alignment.Start)) {
                            title()
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                if (text != null) {
                    ProvideTextStyle(
                        MaterialTheme.typography.bodyMedium.copy(
                            color = LocalContentColor.current.copy(alpha = 0.8f),
                        ),
                    ) {
                        Box(Modifier.fillMaxWidth()) {
                            text()
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    if (dismissButton != null) {
                        dismissButton()
                    }
                    confirmButton()
                }
            }
        }
    }
}
