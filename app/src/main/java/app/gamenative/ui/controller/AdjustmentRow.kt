package app.gamenative.ui.controller

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.gamenative.ui.theme.LocalGameAccent

@Composable
fun AdjustmentRow(
    label: String,
    valueText: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    lockToAdjust: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
) {
    val accent = LocalGameAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)
    var isLocked by remember { mutableStateOf(false) }

    val background = if (isFocused) {
        Brush.horizontalGradient(listOf(accent.copy(alpha = 0.12f), accent.copy(alpha = 0.04f)))
    } else {
        Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.04f)))
    }

    val borderColor = when {
        !isFocused -> Color.Transparent
        lockToAdjust && isLocked -> accent
        else -> accent.copy(alpha = 0.7f)
    }
    val borderWidth = if (isFocused) 2.dp else 0.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(shape)
            .background(background, shape)
            .border(borderWidth, borderColor, shape)
            .onFocusChanged {
                if (!it.isFocused) isLocked = false
            }
            .onPreviewKeyEvent { keyEvent ->
                if (!enabled || !isFocused) return@onPreviewKeyEvent false
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    val keyCode = keyEvent.nativeKeyEvent.keyCode
                    if (lockToAdjust) {
                        when {
                            keyCode == KeyEvent.KEYCODE_BUTTON_A ||
                                keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                                keyCode == KeyEvent.KEYCODE_ENTER -> {
                                isLocked = !isLocked
                                true
                            }

                            isLocked && (keyCode == KeyEvent.KEYCODE_BUTTON_B ||
                                keyCode == KeyEvent.KEYCODE_BACK) -> {
                                isLocked = false
                                true
                            }

                            isLocked && keyCode == KeyEvent.KEYCODE_DPAD_LEFT -> {
                                onDecrease()
                                true
                            }

                            isLocked && keyCode == KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                onIncrease()
                                true
                            }

                            else -> false
                        }
                    } else {
                        when (keyCode) {
                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                onDecrease()
                                true
                            }

                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                onIncrease()
                                true
                            }

                            else -> false
                        }
                    }
                } else {
                    val keyCode = keyEvent.nativeKeyEvent.keyCode
                    if (!lockToAdjust && (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)) {
                        true
                    } else {
                        false
                    }
                }
            }
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (leading != null) {
                    leading()
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isFocused) accent else Color.White.copy(alpha = 0.8f),
                )
                if (lockToAdjust && isLocked) {
                    Text(
                        text = "\u25CF",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                    )
                }
            }
        }
    }
}
