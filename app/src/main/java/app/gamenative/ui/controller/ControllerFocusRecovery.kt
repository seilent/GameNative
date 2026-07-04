package app.gamenative.ui.controller

import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import app.gamenative.PluviaApp
import app.gamenative.events.AndroidEvent

@Composable
fun ControllerFocusRecovery(
    enabled: Boolean,
    hasFocus: () -> Boolean,
    onRecover: () -> Unit,
) {
    val latestEnabled by rememberUpdatedState(enabled)
    val latestHasFocus by rememberUpdatedState(hasFocus)
    val latestOnRecover by rememberUpdatedState(onRecover)

    DisposableEffect(Unit) {
        val listener: (AndroidEvent.KeyEvent) -> Boolean = { androidEvent ->
            val event = androidEvent.event
            if (event.action == KeyEvent.ACTION_DOWN && latestEnabled && !latestHasFocus()) {
                when (event.keyCode.keyCodeToControllerButton()) {
                    ControllerButton.Up,
                    ControllerButton.Down,
                    ControllerButton.Left,
                    ControllerButton.Right,
                    -> latestOnRecover()
                    else -> Unit
                }
            }
            false
        }
        PluviaApp.events.on<AndroidEvent.KeyEvent, Boolean>(listener)
        onDispose {
            PluviaApp.events.off<AndroidEvent.KeyEvent, Boolean>(listener)
        }
    }
}
