package app.gamenative.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.gamenative.ui.theme.DarkGlassBorder
import app.gamenative.ui.theme.DarkGlassFill
import app.gamenative.ui.theme.GlassBorder
import app.gamenative.ui.theme.GlassFill
import app.gamenative.ui.theme.LocalGameAccent

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    dark: Boolean = false,
    fill: Color = if (dark) DarkGlassFill else GlassFill,
    borderColor: Color = if (dark) DarkGlassBorder else GlassBorder,
    borderWidth: Dp = 1.dp,
    sheen: Boolean = true,
    accentTint: Color = if (dark) Color.Unspecified else LocalGameAccent.current,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(color = fill, shape = shape)
            .then(
                if (accentTint != Color.Unspecified) {
                    Modifier.background(color = accentTint.copy(alpha = 0.07f), shape = shape)
                } else {
                    Modifier
                },
            )
            .then(
                if (sheen) {
                    Modifier.background(
                        color = Color.White.copy(alpha = 0.05f),
                        shape = shape,
                    )
                } else {
                    Modifier
                },
            )
            .border(width = borderWidth, color = borderColor, shape = shape),
        content = content,
    )
}
