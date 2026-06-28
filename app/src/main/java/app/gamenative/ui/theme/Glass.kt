package app.gamenative.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val GlassFill = Color(0x99141420)
val GlassFillStrong = Color(0xC2141420)
val GlassSheen = Color(0x1FFFFFFF)
val GlassBorder = Color(0x2EFFFFFF)

val DarkGlassFill = Color(0xE60C0C12)
val DarkGlassBorder = Color(0x2EFFFFFF)

val LocalGameAccent = compositionLocalOf { PluviaPurple }
val LocalOnAccent = compositionLocalOf { Color.White }
val LocalAccentContainer = compositionLocalOf { Color.Transparent }
val LocalAccentContainerBright = compositionLocalOf { Color.Transparent }
val LocalAccentMuted = compositionLocalOf { Color.White.copy(alpha = 0.5f) }

val LocalGameBackdrop = compositionLocalOf { "" }
