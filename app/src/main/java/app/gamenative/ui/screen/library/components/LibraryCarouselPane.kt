package app.gamenative.ui.screen.library.components

import android.view.KeyEvent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.gamenative.R
import app.gamenative.data.GameCompatibilityStatus
import app.gamenative.data.LibraryItem
import app.gamenative.ui.component.CompatibilityBadge
import app.gamenative.ui.component.GameStatsRow
import app.gamenative.ui.component.GlassSurface
import app.gamenative.ui.data.GameCardStats
import app.gamenative.ui.data.LibraryDecorations
import app.gamenative.ui.data.LibraryState
import app.gamenative.ui.data.statsFor
import app.gamenative.ui.enums.PaneType
import app.gamenative.ui.theme.GlassBorder
import app.gamenative.ui.theme.GlassFill
import app.gamenative.ui.theme.LocalGameAccent
import app.gamenative.ui.theme.PluviaTheme
import app.gamenative.ui.util.ListItemImage
import app.gamenative.utils.StorageUtils
import app.gamenative.utils.SteamGridDB
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val CARD_ASPECT = 2f / 3f
private const val CARD_HEIGHT_FRACTION = 0.66f
private const val MAX_CARD_WIDTH_FRACTION = 0.34f
private const val MAX_ROTATION_Y = 20f
private const val MIN_SCALE = 0.82f
private const val MIN_ALPHA = 0.55f
private const val CAMERA_DISTANCE_FACTOR = 12f
private const val MOUSE_WHEEL_MULTIPLIER = 80f
private const val MOUSE_DRAG_SLOP = 8f

private fun Modifier.carouselMouseScroll(listState: LazyListState): Modifier =
    pointerInput(listState) {
        coroutineScope {
            awaitPointerEventScope {
                var isDragging = false
                var lastDragX = 0f
                var pressStartX: Float? = null
                while (true) {
                    val event = awaitPointerEvent()
                    val mouse = event.changes.firstOrNull { it.type == PointerType.Mouse }
                    when (event.type) {
                        PointerEventType.Scroll -> {
                            val delta = mouse?.scrollDelta ?: continue
                            val dominant = if (abs(delta.x) > abs(delta.y)) delta.x else delta.y
                            if (dominant != 0f) {
                                launch { listState.scrollBy(dominant * MOUSE_WHEEL_MULTIPLIER) }
                            }
                        }
                        PointerEventType.Press -> {
                            if (mouse?.pressed == true) {
                                pressStartX = mouse.position.x
                                isDragging = false
                            }
                        }
                        PointerEventType.Move -> {
                            if (mouse != null) {
                                val x = mouse.position.x
                                if (isDragging) {
                                    val d = x - lastDragX
                                    lastDragX = x
                                    if (d != 0f) listState.dispatchRawDelta(-d)
                                } else {
                                    val start = pressStartX
                                    if (start != null && abs(x - start) > MOUSE_DRAG_SLOP) {
                                        isDragging = true
                                        lastDragX = x
                                    }
                                }
                            }
                        }
                        PointerEventType.Release, PointerEventType.Exit -> {
                            isDragging = false
                            pressStartX = null
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun LibraryCarouselPane(
    state: LibraryState,
    decorations: LibraryDecorations,
    listState: LazyListState,
    onPageChange: (Int) -> Unit,
    onNavigate: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    firstCarouselItemFocusRequester: FocusRequester? = null,
    focusTargetListIndex: Int? = null,
    onFocusedIndexChanged: (Int) -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val pullToRefreshState = rememberPullToRefreshState()
    val density = LocalDensity.current

    var navTarget by remember { mutableIntStateOf(focusTargetListIndex ?: 0) }
    var syncingFromScroll by remember { mutableStateOf(false) }
    var didInitialScroll by remember { mutableStateOf(false) }

    val centeredIndex by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            if (info.visibleItemsInfo.isEmpty()) {
                navTarget
            } else {
                val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2f - center) }?.index ?: navTarget
            }
        }
    }

    LaunchedEffect(focusTargetListIndex) {
        if (focusTargetListIndex != null && focusTargetListIndex in state.appInfoList.indices && focusTargetListIndex != navTarget) {
            syncingFromScroll = false
            navTarget = focusTargetListIndex
        }
    }

    LaunchedEffect(navTarget) {
        if (syncingFromScroll) { syncingFromScroll = false; return@LaunchedEffect }
        if (state.appInfoList.isNotEmpty() && navTarget in state.appInfoList.indices) {
            if (didInitialScroll) {
                listState.animateScrollToItem(navTarget)
            } else {
                didInitialScroll = true
                listState.scrollToItem(navTarget)
            }
        }
    }

    LaunchedEffect(listState) {
        var wasScrolling = false
        snapshotFlow { listState.isScrollInProgress }.distinctUntilChanged().collect { scrolling ->
            if (scrolling) {
                wasScrolling = true
            } else if (wasScrolling) {
                wasScrolling = false
                if (centeredIndex != navTarget) {
                    syncingFromScroll = true
                    navTarget = centeredIndex
                }
            }
        }
    }

    LaunchedEffect(centeredIndex, didInitialScroll) {
        if (didInitialScroll && listState.layoutInfo.visibleItemsInfo.isNotEmpty()) {
            onFocusedIndexChanged(centeredIndex)
        }
    }

    LaunchedEffect(state.appInfoList.isNotEmpty(), firstCarouselItemFocusRequester) {
        if (firstCarouselItemFocusRequester != null && state.appInfoList.isNotEmpty()) {
            kotlinx.coroutines.delay(120)
            runCatching { firstCarouselItemFocusRequester.requestFocus() }
        }
    }

    LaunchedEffect(listState, state.appInfoList.size, state.totalAppsInFilter) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { lastVisible ->
                if (lastVisible >= state.appInfoList.lastIndex &&
                    state.appInfoList.size < state.totalAppsInFilter
                ) {
                    onPageChange(1)
                }
            }
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT -> {
                            syncingFromScroll = false
                            navTarget = (navTarget - 1).coerceAtLeast(0)
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT -> {
                            syncingFromScroll = false
                            navTarget = (navTarget + 1).coerceAtMost(state.appInfoList.lastIndex.coerceAtLeast(0))
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A -> {
                            if (centeredIndex in state.appInfoList.indices) {
                                onNavigate(state.appInfoList[centeredIndex].appId)
                            }
                            true
                        }
                        else -> false
                    }
                }
                .then(
                    if (firstCarouselItemFocusRequester != null)
                        Modifier.focusRequester(firstCarouselItemFocusRequester).focusable()
                    else Modifier
                ),
        ) {
            if (state.appInfoList.isEmpty()) {
                CarouselEmpty(isLoading = state.isLoading)
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val viewportWidth = maxWidth
                    val cardWidth = ((maxHeight * CARD_HEIGHT_FRACTION) * CARD_ASPECT)
                        .coerceAtMost(viewportWidth * MAX_CARD_WIDTH_FRACTION)
                    val cardHeight = cardWidth / CARD_ASPECT
                    val sidePadding = (viewportWidth - cardWidth) / 2
                    val cameraDistPx = with(density) { CAMERA_DISTANCE_FACTOR.dp.toPx() }

                    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

                    LazyRow(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .carouselMouseScroll(listState),
                        flingBehavior = flingBehavior,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        contentPadding = PaddingValues(horizontal = sidePadding),
                    ) {
                        items(
                            count = state.appInfoList.size,
                            key = { state.appInfoList[it].appId },
                        ) { index ->
                            val item = state.appInfoList[index]

                            CarouselCard(
                                item = item,
                                index = index,
                                listState = listState,
                                cardWidth = cardWidth,
                                cardHeight = cardHeight,
                                cameraDistPx = cameraDistPx,
                                isSelected = index == centeredIndex,
                                imageRefreshCounter = state.imageRefreshCounter,
                                decorations = decorations,
                                onClick = {
                                    syncingFromScroll = false
                                    navTarget = index
                                    onNavigate(item.appId)
                                },
                            )
                        }

                        if (state.appInfoList.size < state.totalAppsInFilter) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .width(cardWidth)
                                        .height(cardHeight)
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(color = LocalGameAccent.current)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CarouselCard(
    item: LibraryItem,
    index: Int,
    listState: LazyListState,
    cardWidth: Dp,
    cardHeight: Dp,
    cameraDistPx: Float,
    isSelected: Boolean,
    imageRefreshCounter: Long,
    decorations: LibraryDecorations,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val accent = LocalGameAccent.current
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight)
            .zIndex(if (isSelected) 10f else 0f)
            .graphicsLayer {
                val info = listState.layoutInfo
                val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                val itemInfo = info.visibleItemsInfo.firstOrNull { it.index == index }
                val itemCenter = if (itemInfo != null) {
                    itemInfo.offset + itemInfo.size / 2f
                } else {
                    viewportCenter
                }
                val distFromCenter = itemCenter - viewportCenter
                val halfViewport = ((info.viewportEndOffset - info.viewportStartOffset) / 2f).coerceAtLeast(1f)
                val normalized = (distFromCenter / halfViewport).coerceIn(-1.5f, 1.5f)
                val absNorm = abs(normalized)

                val s = 1f - (1f - MIN_SCALE) * absNorm.coerceAtMost(1f)
                scaleX = s
                scaleY = s
                alpha = 1f - (1f - MIN_ALPHA) * absNorm.coerceAtMost(1f)
                rotationY = -normalized * MAX_ROTATION_Y
                cameraDistance = cameraDistPx
            }
            .clip(shape)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) accent.copy(alpha = 0.7f) else GlassBorder,
                shape = shape,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        val sgdbScope = rememberCoroutineScope()
        var sgdbTick by remember(item.appId) { mutableStateOf(0) }

        val cacheKey = remember(item.appId, imageRefreshCounter, sgdbTick) {
            "${item.appId}:${PaneType.GRID_CAPSULE}:${imageRefreshCounter}:${sgdbTick}"
        }
        var imageUrl by remember(cacheKey) { mutableStateOf("") }
        var imageFailed by remember(cacheKey) { mutableStateOf(false) }

        LaunchedEffect(cacheKey) {
            val urls = withContext(Dispatchers.IO) {
                getGridImageUrl(context, item, PaneType.GRID_CAPSULE)
            }
            imageUrl = urls.primary
        }

        if (imageUrl.isNotEmpty() && !imageFailed) {
            ListItemImage(
                modifier = Modifier.fillMaxSize(),
                imageModifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                size = null,
                image = { imageUrl },
                onFailure = {
                    imageFailed = true
                    if (item.name.isNotBlank() &&
                        !SteamGridDB.hasTriedSgdb(context, item.appId)
                    ) {
                        sgdbScope.launch {
                            val ok = SteamGridDB.fetchSgdbForApp(
                                context, item.appId, item.name
                            )
                            if (ok) {
                                imageFailed = false
                                sgdbTick++
                            }
                        }
                    }
                },
                loading = {},
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(GlassFill),
                contentAlignment = Alignment.Center,
            ) {
                if (imageFailed) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PluviaTheme.colors.textMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }

        if (isSelected) {
            val compat = decorations.compatibilityMap[item.name]
            val stats = decorations.statsFor(item)
            CarouselInfoStrip(
                item = item,
                compatibilityStatus = compat,
                stats = stats,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CarouselInfoStrip(
    item: LibraryItem,
    compatibilityStatus: GameCompatibilityStatus?,
    stats: GameCardStats?,
    modifier: Modifier = Modifier,
) {
    val accent = LocalGameAccent.current
    GlassSurface(
        modifier = modifier,
        shape = RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp),
        sheen = false,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GameSourceIcon(gameSource = item.gameSource, iconSize = 12, alignmentBoxSize = 16)
                if (compatibilityStatus != null) {
                    CompatibilityBadge(status = compatibilityStatus, size = 16.dp)
                }
                if (item.isInstalled && item.sizeBytes > 0) {
                    Text(
                        text = StorageUtils.formatBinarySize(item.sizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = PluviaTheme.colors.textMuted,
                        maxLines = 1,
                    )
                }
            }
            if (stats != null) {
                GameStatsRow(stats = stats, tint = accent, onDark = true)
            }
        }
    }
}

@Composable
private fun CarouselEmpty(isLoading: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = LocalGameAccent.current)
        } else {
            GlassSurface(
                modifier = Modifier.padding(horizontal = 24.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    modifier = Modifier.padding(24.dp),
                    text = stringResource(R.string.library_no_items),
                    color = PluviaTheme.colors.textMuted,
                )
            }
        }
    }
}
