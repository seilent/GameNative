package app.gamenative.ui.screen.library.components

import android.view.KeyEvent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import app.gamenative.ui.theme.GlassFill
import app.gamenative.ui.theme.LocalGameAccent
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.gamenative.BuildConfig
import app.gamenative.R
import app.gamenative.ui.enums.LibraryTab
import app.gamenative.ui.enums.LibraryTabItem
import app.gamenative.ui.theme.Motion
import app.gamenative.ui.theme.PluviaTheme
import app.gamenative.ui.util.WindowWidthClass
import app.gamenative.ui.util.rememberWindowWidthClass

/**
 * Tab bar for library navigation with sliding pill indicator.
 * Adapts to screen width
 */
@Composable
fun LibraryTabBar(
    tabs: List<LibraryTabItem>,
    currentItem: LibraryTabItem,
    tabCounts: Map<LibraryTab, Int>,
    onTabSelected: (LibraryTabItem) -> Unit,
    onOptionsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onAddGameClick: () -> Unit,
    onMenuClick: () -> Unit,
    onNavigateDownToGrid: () -> Unit,
    onPreviousTab: () -> Unit = {},
    onNextTab: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val widthClass = rememberWindowWidthClass()

    when (widthClass) {
        WindowWidthClass.COMPACT -> CompactLibraryTabBar(
            tabs = tabs,
            currentItem = currentItem,
            tabCounts = tabCounts,
            onTabSelected = onTabSelected,
            onOptionsClick = onOptionsClick,
            onSearchClick = onSearchClick,
            onAddGameClick = onAddGameClick,
            onMenuClick = onMenuClick,
            onNavigateDownToGrid = onNavigateDownToGrid,
            onPreviousTab = onPreviousTab,
            onNextTab = onNextTab,
            modifier = modifier,
        )

        else -> ExpandedLibraryTabBar(
            tabs = tabs,
            currentItem = currentItem,
            tabCounts = tabCounts,
            onTabSelected = onTabSelected,
            onOptionsClick = onOptionsClick,
            onSearchClick = onSearchClick,
            onAddGameClick = onAddGameClick,
            onMenuClick = onMenuClick,
            onNavigateDownToGrid = onNavigateDownToGrid,
            onPreviousTab = onPreviousTab,
            onNextTab = onNextTab,
            modifier = modifier,
        )
    }
}

/**
 * Compact tab bar for narrow screens.
 * Centered tabs with action buttons for Options, Search, Add Game, and Menu.
 */
@Composable
private fun CompactLibraryTabBar(
    tabs: List<LibraryTabItem>,
    currentItem: LibraryTabItem,
    tabCounts: Map<LibraryTab, Int>,
    onTabSelected: (LibraryTabItem) -> Unit,
    onOptionsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onAddGameClick: () -> Unit,
    onMenuClick: () -> Unit,
    onNavigateDownToGrid: () -> Unit,
    onPreviousTab: () -> Unit,
    onNextTab: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentIndex = tabs.indexOf(currentItem).coerceAtLeast(0)
    val scrollState = rememberScrollState()
    val tabPositions = remember { mutableStateMapOf<Int, Float>() }
    val tabWidths = remember { mutableStateMapOf<Int, Float>() }

    LaunchedEffect(currentItem) {
        val pos = tabPositions[currentIndex] ?: return@LaunchedEffect
        val width = tabWidths[currentIndex] ?: return@LaunchedEffect
        val targetCenter = (pos + width / 2).toInt()
        val viewportCenter = scrollState.viewportSize / 2
        scrollState.animateScrollTo((targetCenter - viewportCenter).coerceAtLeast(0))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 12.dp, start = 8.dp, end = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .focusGroup()
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                        when (keyEvent.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                onNavigateDownToGrid()
                                true
                            }
                            KeyEvent.KEYCODE_BUTTON_L1 -> {
                                onPreviousTab()
                                true
                            }
                            KeyEvent.KEYCODE_BUTTON_R1 -> {
                                onNextTab()
                                true
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            CompactIconButton(
                icon = Icons.Default.Tune,
                contentDescription = stringResource(R.string.options),
                onClick = onOptionsClick,
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(GlassFill)
                    .horizontalScroll(scrollState)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEachIndexed { index, item ->
                    val isSelected = item == currentItem
                    val tabInteractionSource = remember { MutableInteractionSource() }
                    val isTabFocused by tabInteractionSource.collectIsFocusedAsState()
                    Box(
                        modifier = Modifier
                            .onGloballyPositioned { coordinates ->
                                tabPositions[index] = coordinates.positionInParent().x
                                tabWidths[index] = coordinates.size.width.toFloat()
                            }
                            .then(
                                if (isTabFocused) {
                                    Modifier.border(
                                        BorderStroke(2.dp, LocalGameAccent.current),
                                        RoundedCornerShape(16.dp),
                                    )
                                } else {
                                    Modifier
                                },
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                when {
                                    isSelected -> LocalGameAccent.current
                                    isTabFocused -> LocalGameAccent.current.copy(alpha = 0.15f)
                                    else -> Color.Transparent
                                },
                            )
                            .selectable(
                                selected = isSelected,
                                interactionSource = tabInteractionSource,
                                indication = null,
                                onClick = { onTabSelected(item) },
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        val label = tabItemLabel(item, tabCounts)
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                isTabFocused -> LocalGameAccent.current
                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            },
                        )
                    }
                }
            }

            CompactIconButton(
                icon = Icons.Default.Search,
                contentDescription = stringResource(R.string.search),
                onClick = onSearchClick,
            )
            if (!BuildConfig.MODERN_ANDROID) {
                CompactIconButton(
                    icon = Icons.Default.Add,
                    contentDescription = stringResource(R.string.action_add_game),
                    onClick = onAddGameClick,
                )
            }
            CompactIconButton(
                icon = Icons.Default.Menu,
                contentDescription = stringResource(R.string.menu),
                onClick = onMenuClick,
            )
        }
    }
}

/**
 * Simple icon button for compact tab bar.
 */
@Composable
private fun CompactIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = modifier
            .size(36.dp)
            .then(
                if (isFocused) {
                    Modifier.border(
                        BorderStroke(2.dp, LocalGameAccent.current),
                        CircleShape,
                    )
                } else {
                    Modifier
                },
            )
            .clip(CircleShape)
            .background(
                if (isFocused) {
                    LocalGameAccent.current.copy(alpha = 0.2f)
                } else {
                    GlassFill
                },
            )
            .selectable(
                selected = false,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isFocused) {
                LocalGameAccent.current
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            },
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Expanded tab bar for wide screens (landscape phone, tablet).
 */
@Composable
private fun ExpandedLibraryTabBar(
    tabs: List<LibraryTabItem>,
    currentItem: LibraryTabItem,
    tabCounts: Map<LibraryTab, Int>,
    onTabSelected: (LibraryTabItem) -> Unit,
    onOptionsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onAddGameClick: () -> Unit,
    onMenuClick: () -> Unit,
    onNavigateDownToGrid: () -> Unit,
    onPreviousTab: () -> Unit,
    onNextTab: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentIndex = tabs.indexOf(currentItem).coerceAtLeast(0)
    val scrollState = rememberScrollState()

    val tabPositions = remember { mutableStateMapOf<Int, Float>() }
    val tabWidths = remember { mutableStateMapOf<Int, Float>() }

    val density = LocalDensity.current

    val indicatorOffset by animateDpAsState(
        targetValue = with(density) { (tabPositions[currentIndex] ?: 0f).toDp() },
        animationSpec = Motion.IndicatorDp,
        label = "indicatorOffset",
    )

    val indicatorWidth by animateDpAsState(
        targetValue = with(density) { (tabWidths[currentIndex] ?: 80f).toDp() },
        animationSpec = Motion.IndicatorDp,
        label = "indicatorWidth",
    )

    LaunchedEffect(currentItem) {
        val pos = tabPositions[currentIndex] ?: return@LaunchedEffect
        val width = tabWidths[currentIndex] ?: return@LaunchedEffect
        val targetCenter = (pos + width / 2).toInt()
        val viewportCenter = scrollState.viewportSize / 2
        scrollState.animateScrollTo((targetCenter - viewportCenter).coerceAtLeast(0))
    }

    Box(
        modifier = modifier
            .padding(top = 8.dp, bottom = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .focusGroup()
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                        when (keyEvent.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                onNavigateDownToGrid()
                                true
                            }
                            KeyEvent.KEYCODE_BUTTON_L1 -> {
                                onPreviousTab()
                                true
                            }
                            KeyEvent.KEYCODE_BUTTON_R1 -> {
                                onNextTab()
                                true
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconActionButton(
                icon = Icons.Default.Tune,
                contentDescription = stringResource(R.string.options),
                onClick = onOptionsClick,
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(color = GlassFill)
                    .horizontalScroll(scrollState)
                    .padding(4.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(indicatorOffset.roundToPx(), 0) }
                        .width(indicatorWidth)
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(color = LocalGameAccent.current),
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    tabs.forEachIndexed { index, item ->
                        TabItem(
                            item = item,
                            tabCounts = tabCounts,
                            isSelected = item == currentItem,
                            onClick = { onTabSelected(item) },
                            onPositioned = { position, width ->
                                tabPositions[index] = position
                                tabWidths[index] = width
                            },
                        )
                    }
                }
            }

            IconActionButton(
                icon = Icons.Default.Search,
                contentDescription = stringResource(R.string.search),
                onClick = onSearchClick,
            )

            if (!BuildConfig.MODERN_ANDROID) {
                IconActionButton(
                    icon = Icons.Default.Add,
                    contentDescription = stringResource(R.string.action_add_game),
                    onClick = onAddGameClick,
                )
            }

            IconActionButton(
                icon = Icons.Default.Menu,
                contentDescription = stringResource(R.string.menu),
                onClick = onMenuClick,
            )
        }
    }
}

@Composable
private fun IconActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.15f else 1f,
        animationSpec = Motion.FocusScale,
        label = "iconButtonScale",
    )

    val alpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.7f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "iconButtonAlpha",
    )

    Box(
        modifier = modifier
            .scale(scale)
            .size(44.dp)
            .then(
                if (isFocused) {
                    Modifier.border(
                        BorderStroke(2.dp, LocalGameAccent.current),
                        CircleShape,
                    )
                } else {
                    Modifier
                },
            )
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = if (isFocused) {
                        listOf(
                            LocalGameAccent.current.copy(alpha = 0.4f),
                            LocalGameAccent.current.copy(alpha = 0.2f),
                        )
                    } else {
                        listOf(
                            GlassFill.copy(alpha = 0.4f),
                            GlassFill.copy(alpha = 0.2f),
                        )
                    },
                ),
            )
            .selectable(
                selected = isFocused,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .alpha(alpha),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isFocused) {
                LocalGameAccent.current
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            },
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun tabItemLabel(item: LibraryTabItem, tabCounts: Map<LibraryTab, Int>): String {
    return when (item) {
        is LibraryTabItem.Store -> {
            val count = tabCounts[item.tab]
            if (count != null && count > 0) {
                stringResource(R.string.library_tab_with_count, stringResource(item.tab.labelResId), count)
            } else {
                stringResource(item.tab.labelResId)
            }
        }
        is LibraryTabItem.Collection -> item.name
    }
}

@Composable
private fun TabItem(
    item: LibraryTabItem,
    tabCounts: Map<LibraryTab, Int>,
    isSelected: Boolean,
    onClick: () -> Unit,
    onPositioned: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val textAlpha by animateFloatAsState(
        targetValue = when {
            isSelected -> 1f
            isFocused -> 0.9f
            else -> 0.6f
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "textAlpha",
    )

    val label = tabItemLabel(item, tabCounts)

    Box(
        modifier = modifier
            .then(
                if (isFocused) {
                    Modifier.border(
                        BorderStroke(2.dp, LocalGameAccent.current),
                        RoundedCornerShape(20.dp),
                    )
                } else {
                    Modifier
                },
            )
            .clip(RoundedCornerShape(20.dp))
            .onGloballyPositioned { coordinates ->
                onPositioned(
                    coordinates.positionInParent().x,
                    coordinates.size.width.toFloat(),
                )
            }
            .selectable(
                selected = isSelected,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = when {
                isSelected -> MaterialTheme.colorScheme.onPrimary
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha)
            },
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A)
@Composable
private fun Preview_LibraryTabBar() {
    PluviaTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            LibraryTabBar(
                tabs = listOf(
                    LibraryTabItem.Store(LibraryTab.ALL),
                    LibraryTabItem.Store(LibraryTab.INSTALLED),
                    LibraryTabItem.Store(LibraryTab.STEAM),
                    LibraryTabItem.Store(LibraryTab.GOG),
                    LibraryTabItem.Store(LibraryTab.EPIC),
                ),
                currentItem = LibraryTabItem.Store(LibraryTab.ALL),
                tabCounts = mapOf(
                    LibraryTab.ALL to 42,
                    LibraryTab.STEAM to 30,
                    LibraryTab.GOG to 8,
                    LibraryTab.EPIC to 4,
                    LibraryTab.LOCAL to 3,
                ),
                onTabSelected = {},
                onOptionsClick = {},
                onSearchClick = {},
                onAddGameClick = {},
                onMenuClick = {},
                onNavigateDownToGrid = {},
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A)
@Composable
private fun Preview_LibraryTabBar_Steam() {
    PluviaTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            LibraryTabBar(
                tabs = listOf(
                    LibraryTabItem.Store(LibraryTab.ALL),
                    LibraryTabItem.Store(LibraryTab.INSTALLED),
                    LibraryTabItem.Collection("fav", "Favorites"),
                    LibraryTabItem.Collection("jrpg", "jrpg"),
                ),
                currentItem = LibraryTabItem.Store(LibraryTab.ALL),
                tabCounts = mapOf(
                    LibraryTab.ALL to 42,
                    LibraryTab.STEAM to 30,
                ),
                onTabSelected = {},
                onOptionsClick = {},
                onSearchClick = {},
                onAddGameClick = {},
                onMenuClick = {},
                onNavigateDownToGrid = {},
            )
        }
    }
}
