package app.gamenative.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Glassmorphism surface tokens.
 *
 * These are composited OVER the blurred game-thumbnail backdrop, so a light translucent fill
 * reads as frosted glass without any (expensive) live backdrop blur.
 *
 * Light glass  -> panels that sit over the blurred backdrop (library, menus, dialogs).
 * Dark glass   -> panels that sit over the live game surface (in-game overlay), where there is
 *                 no thumbnail to blur, so we use a translucent dark base instead.
 *
 * Alpha reference: 0x0D≈5%, 0x14≈8%, 0x1F≈12%, 0xD9≈85%.
 */

// Frosted glass panels: a semi-opaque tinted fill (so it reads as glass over the blurred backdrop
// while keeping text legible), a white sheen, and a hairline border. These are the main tuning knobs.
val GlassFill = Color(0x99141420) // ~60% dark frost (default panels)
val GlassFillStrong = Color(0xC2141420) // ~76% for dense panels (menus / sheets)
val GlassSheen = Color(0x1FFFFFFF) // ~12% white top highlight for the glass sheen
val GlassBorder = Color(0x2EFFFFFF) // ~18% white hairline

// Dark glass (over the live game surface, in-game overlay)
val DarkGlassFill = Color(0xE60C0C12) // ~90% near-background
val DarkGlassBorder = Color(0x2EFFFFFF) // ~18% white hairline

/**
 * The accent color extracted from the currently focused game's artwork (clamped + cached),
 * or [PluviaPurple] when nothing is focused / extraction is low-confidence.
 *
 * Provided near the root and consumed by focus glows, selection states, and glass accents.
 */
val LocalGameAccent = compositionLocalOf { PluviaPurple }

/**
 * The backdrop URL of the currently focused game, provided near the root so panels can render
 * a clipped [BlurredBackdrop] behind frosted glass for a genuine depth effect.
 */
val LocalGameBackdrop = compositionLocalOf { "" }
