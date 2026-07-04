package app.gamenative.ui.controller

import android.view.KeyEvent
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.SoftwareKeyboardController

fun Modifier.onControllerButton(
    enabled: Boolean = true,
    throttleMs: Long = 0L,
    onButton: (ControllerButton) -> Boolean,
): Modifier = composed {
    if (!enabled) return@composed this

    val lastFireTimes = remember {
        mutableMapOf<ControllerButton, Long>()
    }

    onPreviewKeyEvent { keyEvent ->
        if (keyEvent.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
        val button = keyEvent.nativeKeyEvent.keyCode.keyCodeToControllerButton()
            ?: return@onPreviewKeyEvent false

        if (throttleMs > 0L) {
            val now = System.currentTimeMillis()
            val lastFire = lastFireTimes[button] ?: 0L
            if (now - lastFire < throttleMs) return@onPreviewKeyEvent true
            lastFireTimes[button] = now
        }

        onButton(button)
    }
}

fun Modifier.dpadFocusEscape(
    focusManager: FocusManager,
    keyboardController: SoftwareKeyboardController? = null,
    escapeUp: Boolean = true,
    escapeDown: Boolean = true,
    escapeLeft: Boolean = false,
    escapeRight: Boolean = false,
    escapeBack: Boolean = true,
): Modifier = onPreviewKeyEvent { keyEvent ->
    if (keyEvent.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
    when (keyEvent.nativeKeyEvent.keyCode) {
        KeyEvent.KEYCODE_DPAD_UP -> if (escapeUp) {
            focusManager.moveFocus(FocusDirection.Up)
            true
        } else false

        KeyEvent.KEYCODE_DPAD_DOWN -> if (escapeDown) {
            focusManager.moveFocus(FocusDirection.Down)
            true
        } else false

        KeyEvent.KEYCODE_DPAD_LEFT -> if (escapeLeft) {
            focusManager.moveFocus(FocusDirection.Left)
            true
        } else false

        KeyEvent.KEYCODE_DPAD_RIGHT -> if (escapeRight) {
            focusManager.moveFocus(FocusDirection.Right)
            true
        } else false

        KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BACK -> if (escapeBack) {
            keyboardController?.hide()
            focusManager.clearFocus()
            true
        } else false

        else -> false
    }
}
