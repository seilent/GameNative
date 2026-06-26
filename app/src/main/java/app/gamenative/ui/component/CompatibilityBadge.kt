package app.gamenative.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.gamenative.R
import app.gamenative.data.GameCompatibilityStatus
import app.gamenative.ui.theme.PluviaTheme

/**
 * Badge displaying game compatibility status.
 *
 * Renders just the status icon tinted by its status color, with no background container.
 *
 * @param status The compatibility status to display
 * @param modifier Modifier for the badge
 * @param showLabel Unused, kept for API compat
 */
@Composable
fun CompatibilityBadge(
    status: GameCompatibilityStatus,
    modifier: Modifier = Modifier,
    showLabel: Boolean = false,
) {
    val badgeStyle = getBadgeStyle(status)

    Icon(
        imageVector = badgeStyle.icon,
        contentDescription = stringResource(badgeStyle.labelResId),
        tint = badgeStyle.iconTint,
        modifier = modifier.size(18.dp),
    )
}

/**
 * Style configuration for a compatibility badge.
 */
private data class BadgeStyle(
    val icon: ImageVector,
    val backgroundColor: Color,
    val iconTint: Color,
    val labelResId: Int,
)

/**
 * Gets the badge style for a given compatibility status.
 */
@Composable
private fun getBadgeStyle(status: GameCompatibilityStatus): BadgeStyle {
    val colors = PluviaTheme.colors
    return when (status) {
        GameCompatibilityStatus.COMPATIBLE -> BadgeStyle(
            icon = Icons.Rounded.Verified,
            backgroundColor = colors.compatibilityGoodBackground.copy(alpha = 0.9f),
            iconTint = colors.compatibilityGood,
            labelResId = R.string.library_compatible,
        )

        GameCompatibilityStatus.GPU_COMPATIBLE -> BadgeStyle(
            icon = Icons.Rounded.Verified,
            backgroundColor = colors.compatibilityGoodBackground.copy(alpha = 0.9f),
            iconTint = colors.compatibilityGood,
            labelResId = R.string.library_compatible,
        )

        GameCompatibilityStatus.UNKNOWN -> BadgeStyle(
            icon = Icons.Rounded.QuestionMark,
            backgroundColor = colors.compatibilityUnknownBackground.copy(alpha = 0.8f),
            iconTint = colors.compatibilityUnknown,
            labelResId = R.string.library_compatibility_unknown,
        )

        GameCompatibilityStatus.NOT_COMPATIBLE -> BadgeStyle(
            icon = Icons.Rounded.Close,
            backgroundColor = colors.compatibilityBadBackground.copy(alpha = 0.9f),
            iconTint = colors.compatibilityBad,
            labelResId = R.string.library_not_compatible,
        )

        GameCompatibilityStatus.RECOMMENDED -> BadgeStyle(
            icon = Icons.Rounded.Star,
            backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
            labelResId = R.string.recommended_badge,
        )
    }
}



