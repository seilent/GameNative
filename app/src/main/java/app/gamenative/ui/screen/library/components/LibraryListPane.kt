package app.gamenative.ui.screen.library.components

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.gamenative.PrefManager
import app.gamenative.data.GameSource
import app.gamenative.data.LibraryItem
import app.gamenative.service.DownloadService
import app.gamenative.service.amazon.AmazonService
import app.gamenative.service.epic.EpicService
import app.gamenative.service.gog.GOGService
import app.gamenative.ui.enums.AppFilter
import app.gamenative.ui.component.Scrollbar
import app.gamenative.ui.data.LibraryState
import app.gamenative.ui.data.LibraryDecorations
import app.gamenative.ui.data.statsFor
import app.gamenative.ui.enums.PaneType
import app.gamenative.ui.internal.fakeAppInfo
import app.gamenative.ui.theme.GlassBorder
import app.gamenative.ui.theme.LocalGameAccent
import app.gamenative.ui.theme.PluviaTheme
import app.gamenative.ui.util.AdaptivePadding
import app.gamenative.ui.util.WindowWidthClass
import app.gamenative.ui.util.rememberWindowWidthClass
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import timber.log.Timber

/**
 * Calculates the installed games count based on the current filter state.
 *
 * @param state The current library state containing filters and visibility settings
 * @return The number of installed games, respecting current filters and source visibility
 */
private fun calculateInstalledCount(context: android.content.Context, state: LibraryState): Int {
    if (state.appInfoSortType.contains(AppFilter.INSTALLED)) {
        return state.totalAppsInFilter
    }

    val downloadDirectoryApps = DownloadService.getDownloadDirectoryApps()

    val steamCount = if (state.showSteamInLibrary) {
        downloadDirectoryApps.count()
    } else {
        0
    }

    val customGameCount = if (state.showCustomGamesInLibrary) {
        PrefManager.customGamesCount
    } else {
        0
    }

    val gogCount = if (state.showGOGInLibrary && GOGService.hasStoredCredentials(context)) {
        PrefManager.gogInstalledGamesCount
    } else {
        0
    }

    val epicCount = if (state.showEpicInLibrary && EpicService.hasStoredCredentials(context)) {
        PrefManager.epicInstalledGamesCount
    } else {
        0
    }

    val amazonCount = if (state.showAmazonInLibrary && AmazonService.hasStoredCredentials(context)) {
        PrefManager.amazonInstalledGamesCount
    } else {
        0
    }

    return steamCount + customGameCount + gogCount + epicCount + amazonCount
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibraryListPane(
    state: LibraryState,
    decorations: LibraryDecorations,
    listState: LazyGridState,
    currentLayout: PaneType,
    firstGridItemFocusRequester: FocusRequester? = null,
    focusTargetListIndex: Int? = null,
    onFocusedIndexChanged: (Int) -> Unit = {},
    onTouchPosition: (Offset) -> Unit = {},
    onPageChange: (Int) -> Unit,
    onNavigate: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val snackBarHost = remember { SnackbarHostState() }

    // Calculate installed count based on current filter state
    val installedCount = remember(
        state.appInfoSortType,
        state.showSteamInLibrary,
        state.showCustomGamesInLibrary,
        state.showGOGInLibrary,
        state.showEpicInLibrary,
        state.showAmazonInLibrary,
        state.totalAppsInFilter,
    ) {
        calculateInstalledCount(context, state)
    }

    val pullToRefreshState = rememberPullToRefreshState()
    val windowWidthClass = rememberWindowWidthClass()

    val columnType = remember(currentLayout, windowWidthClass) {
        when (currentLayout) {
            PaneType.GRID_HERO -> {
                val minSize = when (windowWidthClass) {
                    WindowWidthClass.COMPACT -> 160.dp
                    WindowWidthClass.MEDIUM -> 180.dp
                    WindowWidthClass.EXPANDED -> 200.dp
                }
                GridCells.Adaptive(minSize = minSize)
            }

            PaneType.GRID_CAPSULE -> {
                val minSize = when (windowWidthClass) {
                    WindowWidthClass.COMPACT -> 110.dp
                    WindowWidthClass.MEDIUM -> 130.dp
                    WindowWidthClass.EXPANDED -> 150.dp
                }
                GridCells.Adaptive(minSize = minSize)
            }

            else -> GridCells.Fixed(1)
        }
    }

    val horizontalPadding = AdaptivePadding.horizontal()
    val gridSpacing = AdaptivePadding.gridSpacing()

    LaunchedEffect(listState, state.appInfoList.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex >= state.appInfoList.lastIndex &&
                    state.appInfoList.size < state.totalAppsInFilter
                ) {
                    onPageChange(1)
                }
            }
    }

    var targetOfScroll by remember { mutableIntStateOf(-1) }
    LaunchedEffect(targetOfScroll) {
        if (targetOfScroll != -1) {
            listState.animateScrollToItem(targetOfScroll, -100)
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        snackbarHost = { SnackbarHost(snackBarHost) },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            if (state.appInfoList.isNotEmpty()) {
                Scrollbar(
                    listState = listState,
                    modifier = Modifier.fillMaxSize(),
                    totalItemsOverride = state.totalAppsInFilter,
                ) {
                    PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = onRefresh,
                        state = pullToRefreshState,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LazyVerticalGrid(
                            columns = columnType,
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent(PointerEventPass.Initial)
                                            event.changes.firstOrNull { it.pressed }?.let { onTouchPosition(it.position) }
                                        }
                                    }
                                },
                            horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                            contentPadding = PaddingValues(
                                top = 80.dp,
                                start = horizontalPadding,
                                end = horizontalPadding + 4.dp,
                                bottom = 72.dp,
                            ),
                        ) {
                            val appInfoList = state.appInfoList
                            val compatibilityMap = decorations.compatibilityMap
                            val imageRefreshCounter = state.imageRefreshCounter
                            val totalAppsInFilter = state.totalAppsInFilter
                            items(
                                count = appInfoList.size,
                                key = { listIndex -> appInfoList[listIndex].appId },
                                contentType = { "game" },
                            ) { listIndex ->
                                val item = appInfoList[listIndex]

                                Box {
                                    val appItemModifier = if (firstGridItemFocusRequester != null &&
                                        focusTargetListIndex != null &&
                                        listIndex == focusTargetListIndex
                                    ) {
                                        Modifier.focusRequester(firstGridItemFocusRequester)
                                    } else {
                                        Modifier
                                    }

                                    if (item.index > 0 && currentLayout == PaneType.LIST) {
                                        HorizontalDivider(color = GlassBorder)
                                    }
                                    AppItem(
                                        modifier = appItemModifier,
                                        appInfo = item,
                                        onClick = { onFocusedIndexChanged(item.index); onNavigate(item.appId) },
                                        paneType = currentLayout,
                                        onFocus = { targetOfScroll = item.index; onFocusedIndexChanged(item.index) },
                                        imageRefreshCounter = imageRefreshCounter,
                                        compatibilityStatus = compatibilityMap[item.name],
                                        gameStats = decorations.statsFor(item),
                                        enableFocusScale = false,
                                    )
                                }
                            }
                            if (appInfoList.size < totalAppsInFilter) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
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
}

/***********
 * PREVIEW *
 ***********/

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(device = "spec:width=1920px,height=1080px,dpi=440") // Odin2 Mini
@Composable
private fun Preview_LibraryListPane() {
    val context = LocalContext.current
    PrefManager.init(context)
    val state = remember {
        LibraryState(
            appInfoList = List(15) { idx ->
                val item = fakeAppInfo(idx)
                LibraryItem(
                    index = idx,
                    appId = "${GameSource.STEAM.name}_${item.id}",
                    name = item.name,
                    iconHash = item.iconHash,
                    isShared = idx % 2 == 0,
                )
            },
        )
    }
    PluviaTheme {
        Surface {
            LibraryListPane(
                listState = LazyGridState(2),
                state = state,
                decorations = LibraryDecorations(),
                currentLayout = PaneType.GRID_HERO,
                onPageChange = { },
                onNavigate = { },
                onRefresh = { },
            )
        }
    }
}
