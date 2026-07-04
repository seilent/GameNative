package app.gamenative.ui.controller

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager

@Stable
class ControllerFocusState(
    val focusRequester: FocusRequester = FocusRequester(),
) {
    suspend fun requestFocus() {
        focusRequester.requestFocusWhenReady()
    }
}

@Composable
fun rememberControllerFocus(): ControllerFocusState = remember { ControllerFocusState() }

suspend fun FocusRequester.requestFocusWhenReady(maxFrames: Int = 30): Boolean {
    if (runCatching { requestFocus() }.isSuccess) return true
    repeat(maxFrames) {
        withFrameNanos { }
        if (runCatching { requestFocus() }.isSuccess) return true
    }
    return false
}

suspend fun acquireControllerFocus(
    inputModeManager: InputModeManager,
    requester: FocusRequester,
): Boolean {
    inputModeManager.requestInputMode(InputMode.Keyboard)
    return requester.requestFocusWhenReady()
}

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.controllerFocusAnchor(
    state: ControllerFocusState,
    restorer: Boolean = true,
): Modifier = focusRequester(state.focusRequester)
    .then(if (restorer) Modifier.focusRestorer() else Modifier)

@Composable
fun ControllerFocusState.RequestInitialFocus(active: Boolean) {
    if (active) {
        LaunchedEffect(Unit) {
            requestFocus()
        }
    }
}
