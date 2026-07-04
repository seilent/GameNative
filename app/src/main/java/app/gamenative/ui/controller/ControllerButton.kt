package app.gamenative.ui.controller

import android.view.KeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.nativeKeyCode

enum class ControllerButton {
    Up,
    Down,
    Left,
    Right,
    Confirm,
    Back,
    PrevTab,
    NextTab,
    PrevPage,
    NextPage,
    Menu,
}

fun Key.toControllerButton(): ControllerButton? =
    nativeKeyCode.keyCodeToControllerButton()

fun Int.keyCodeToControllerButton(): ControllerButton? = when (this) {
    KeyEvent.KEYCODE_DPAD_UP -> ControllerButton.Up
    KeyEvent.KEYCODE_DPAD_DOWN -> ControllerButton.Down
    KeyEvent.KEYCODE_DPAD_LEFT -> ControllerButton.Left
    KeyEvent.KEYCODE_DPAD_RIGHT -> ControllerButton.Right
    KeyEvent.KEYCODE_DPAD_CENTER,
    KeyEvent.KEYCODE_ENTER,
    KeyEvent.KEYCODE_NUMPAD_ENTER,
    KeyEvent.KEYCODE_BUTTON_A -> ControllerButton.Confirm
    KeyEvent.KEYCODE_BACK,
    KeyEvent.KEYCODE_BUTTON_B -> ControllerButton.Back
    KeyEvent.KEYCODE_BUTTON_L1 -> ControllerButton.PrevTab
    KeyEvent.KEYCODE_BUTTON_R1 -> ControllerButton.NextTab
    KeyEvent.KEYCODE_BUTTON_L2 -> ControllerButton.PrevPage
    KeyEvent.KEYCODE_BUTTON_R2 -> ControllerButton.NextPage
    KeyEvent.KEYCODE_MENU,
    KeyEvent.KEYCODE_BUTTON_START,
    KeyEvent.KEYCODE_BUTTON_MODE -> ControllerButton.Menu
    else -> null
}
