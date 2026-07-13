package app.gamenative.ui.screen.library.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Face4
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.gamenative.R
import app.gamenative.data.GameCompatibilityStatus
import app.gamenative.data.GameSource
import app.gamenative.data.LibraryItem
import app.gamenative.ui.component.CompatibilityBadge
import app.gamenative.ui.component.GameStatsRow
import app.gamenative.ui.data.GameCardStats
import app.gamenative.ui.enums.PaneType
import app.gamenative.ui.theme.GlassFill
import app.gamenative.ui.theme.LocalGameAccent
import app.gamenative.ui.theme.PluviaTheme
import app.gamenative.ui.util.ListItemImage
import app.gamenative.utils.CustomGameScanner
import app.gamenative.utils.SteamGridDB
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val gridImageUrlCache = ConcurrentHashMap<String, GridImageUrls>()

/**
 * Grid card for Hero/Capsule layout views.
 */
@Composable
internal fun GridViewCard(
    modifier: Modifier,
    appInfo: LibraryItem,
    onClick: () -> Unit,
    onFocus: () -> Unit,
    isFocused: Boolean,
    onFocusChanged: (Boolean) -> Unit,
    scale: Float,
    paneType: PaneType,
    imageRefreshCounter: Long,
    hideText: Boolean,
    imageAlpha: Float,
    onImageLoadFailed: () -> Unit,
    isDeleting: Boolean,
    compatibilityStatus: GameCompatibilityStatus?,
    gameStats: GameCardStats?,
    showFocusGlow: Boolean,
    context: Context,
) {
    val aspectRatio = if (paneType == PaneType.GRID_CAPSULE) 2f / 3f else 460f / 215f
    val isCapsule = paneType == PaneType.GRID_CAPSULE
    val topOverlayPadding = if (isCapsule) 8.dp else 4.dp
    val cardContentBottomPadding = if (isCapsule) 12.dp else 8.dp
    val topIconPadding = if (isCapsule) 10.dp else 8.dp
    val accentColor = if (isFocused || appInfo.isRecommended || appInfo.isShared) LocalGameAccent.current else Color.Transparent
    val overlayAlpha = if (isFocused) 1f else 0f
    val focusHaloModifier = if (isFocused && showFocusGlow) {
        Modifier.drawWithCache {
            val glowBrush = Brush.radialGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.3f),
                    Color.Transparent,
                ),
                radius = size.maxDimension * 0.7f,
            )
            val glowRadius = size.maxDimension * 0.6f
            onDrawBehind {
                drawCircle(
                    brush = glowBrush,
                    radius = glowRadius,
                    center = center,
                )
            }
        }
    } else {
        Modifier
    }
    val focusBorderBrush = SolidColor(accentColor)

    Box(
        modifier = modifier
            .padding(vertical = 4.dp)
            .scale(scale)
            .then(focusHaloModifier),
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isItemFocused by interactionSource.collectIsFocusedAsState()

        LaunchedEffect(isItemFocused) {
            onFocusChanged(isItemFocused)
            if (isItemFocused) onFocus()
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .clickable(
                    onClick = onClick,
                    interactionSource = interactionSource,
                    indication = null,
                ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent,
            ),
            border = when {
                isFocused -> BorderStroke(2.dp, focusBorderBrush)
                appInfo.isRecommended -> BorderStroke(
                    1.dp,
                    accentColor.copy(alpha = 0.4f),
                )
                else -> null
            },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clearAndSetSemantics { contentDescription = appInfo.name },
            ) {
                val sgdbScope = rememberCoroutineScope()
                var sgdbTick by remember(appInfo.appId) { mutableStateOf(0) }

                val cacheKey = remember(appInfo.appId, paneType, imageRefreshCounter, sgdbTick) {
                    "${appInfo.appId}:${paneType}:${imageRefreshCounter}:${sgdbTick}"
                }
                val imageUrls by produceState(
                    initialValue = gridImageUrlCache[cacheKey] ?: GridImageUrls("", ""),
                    key1 = cacheKey,
                ) {
                    if (gridImageUrlCache.containsKey(cacheKey)) return@produceState
                    val result = withContext(Dispatchers.IO) {
                        getGridImageUrl(context, appInfo, paneType)
                    }
                    gridImageUrlCache[cacheKey] = result
                    value = result
                }

                var currentImageUrl by remember(
                    imageUrls.primary,
                    imageUrls.fallback,
                    appInfo.appId,
                    imageRefreshCounter,
                    sgdbTick,
                ) {
                    mutableStateOf(imageUrls.primary)
                }

                val gridHeroZoom = if (!isCapsule && appInfo.gridHeroImageScale != 1f) {
                    Modifier.graphicsLayer {
                        scaleX = appInfo.gridHeroImageScale
                        scaleY = appInfo.gridHeroImageScale
                        transformOrigin = TransformOrigin.Center
                    }
                } else {
                    Modifier
                }

                ListItemImage(
                    modifier = Modifier.fillMaxSize(),
                    imageModifier = Modifier
                        .fillMaxSize()
                        .alpha(imageAlpha)
                        .then(gridHeroZoom),
                    contentScale = getGridContentScale(paneType),
                    image = { currentImageUrl },
                    size = null,
                    loading = {},
                    onFailure = {
                        if (imageUrls.fallback.isNotEmpty() && currentImageUrl == imageUrls.primary) {
                            currentImageUrl = imageUrls.fallback
                        } else {
                            onImageLoadFailed()
                            if (appInfo.name.isNotBlank() &&
                                !SteamGridDB.hasTriedSgdb(context, appInfo.appId)
                            ) {
                                sgdbScope.launch {
                                    val ok = SteamGridDB.fetchSgdbForApp(
                                        context, appInfo.appId, appInfo.name
                                    )
                                    if (ok) {
                                        gridImageUrlCache.keys.removeIf { k ->
                                            k.startsWith("${appInfo.appId}:")
                                        }
                                        sgdbTick++
                                    }
                                }
                            }
                        }
                    },
                )

                if (!hideText) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(GlassFill),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = appInfo.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PluviaTheme.colors.textMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }

                val badgeStatus = if (appInfo.isRecommended) {
                    GameCompatibilityStatus.RECOMMENDED
                } else {
                    compatibilityStatus
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .alpha(overlayAlpha)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .background(accentColor.copy(alpha = 0.25f))
                        .padding(horizontal = 10.dp, vertical = cardContentBottomPadding),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = appInfo.name,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            shadow = Shadow(
                                color = Color.Black,
                                offset = Offset(0f, 1f),
                                blurRadius = 3f,
                            ),
                        ),
                        color = Color.White,
                        maxLines = if (paneType == PaneType.GRID_CAPSULE) 2 else 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        GameStatsRow(
                            modifier = Modifier.weight(1f),
                            stats = gameStats,
                            tint = Color.White.copy(alpha = 0.7f),
                            onDark = true,
                        )
                        if (!appInfo.isRecommended) {
                            GameSourceIcon(
                                gameSource = appInfo.gameSource,
                                iconSize = 14,
                                alignmentBoxSize = 14,
                            )
                        }
                        if (isDeleting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = accentColor,
                                strokeWidth = 1.5.dp,
                            )
                        } else if (appInfo.isInstalled) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = stringResource(R.string.library_installed),
                                tint = PluviaTheme.colors.statusInstalled,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        if (appInfo.isShared) {
                            Icon(
                                imageVector = Icons.Filled.Face4,
                                contentDescription = stringResource(R.string.library_family_shared),
                                tint = accentColor,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }

                badgeStatus?.let { status ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .alpha(overlayAlpha)
                            .padding(top = topOverlayPadding, start = topOverlayPadding),
                    ) {
                        CompatibilityBadge(status = status)
                    }
                }
            }
        }
    }
}

/**
 * Status icons for grid view (installed, family share).
 */
@Composable
private fun GridStatusIcons(appInfo: LibraryItem) {
    val isInstalled = appInfo.isInstalled

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isInstalled) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = stringResource(R.string.library_installed),
                    tint = PluviaTheme.colors.statusInstalled,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
        if (appInfo.isShared) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Face4,
                    contentDescription = stringResource(R.string.library_family_shared),
                    tint = LocalGameAccent.current,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

internal data class GridImageUrls(val primary: String, val fallback: String = "")

private fun getGridContentScale(paneType: PaneType): ContentScale {
    return when (paneType) {
        PaneType.GRID_HERO, PaneType.GRID_CAPSULE -> ContentScale.Crop
        else -> ContentScale.Fit
    }
}

internal fun getGridImageUrl(
    context: Context,
    appInfo: LibraryItem,
    paneType: PaneType,
): GridImageUrls {
    fun findSteamGridDBImage(imageType: String): String? {
        if (appInfo.gameSource == GameSource.CUSTOM_GAME) {
            val gameFolderPath = CustomGameScanner.getFolderPathFromAppId(appInfo.appId)
            gameFolderPath?.let { path ->
                val folder = File(path)
                val imageFile = folder.listFiles()?.firstOrNull { file ->
                    file.name.startsWith("steamgriddb_$imageType") &&
                        (
                            file.name.endsWith(".png", ignoreCase = true) ||
                                file.name.endsWith(".jpg", ignoreCase = true) ||
                                file.name.endsWith(".webp", ignoreCase = true)
                            )
                }
                return imageFile?.let { android.net.Uri.fromFile(it).toString() }
            }
        }
        return null
    }

    fun cachedSgdb(imageType: String): String? =
        SteamGridDB.cachedSgdbImage(context, appInfo.appId, imageType)

    return when (appInfo.gameSource) {
        GameSource.CUSTOM_GAME -> {
            val primary = when (paneType) {
                PaneType.GRID_CAPSULE ->
                    CustomGameScanner.findCapsuleCoverForCustomGame(appInfo.appId)
                        ?: findSteamGridDBImage("grid_capsule")
                        ?: cachedSgdb("grid_capsule")
                        ?: appInfo.capsuleImageUrl
                PaneType.GRID_HERO ->
                    CustomGameScanner.findHeroCoverForCustomGame(appInfo.appId)
                        ?: findSteamGridDBImage("grid_hero")
                        ?: cachedSgdb("grid_hero")
                        ?: appInfo.headerImageUrl
                else -> {
                    val heroCover = CustomGameScanner.findHeroCoverForCustomGame(appInfo.appId)
                    val gameFolderPath = CustomGameScanner.getFolderPathFromAppId(appInfo.appId)
                    val heroUrl = gameFolderPath?.let { path ->
                        val folder = File(path)
                        val heroFile = folder.listFiles()?.firstOrNull { file ->
                            file.name.startsWith("steamgriddb_hero") &&
                                !file.name.contains("grid") &&
                                (
                                    file.name.endsWith(".png", ignoreCase = true) ||
                                        file.name.endsWith(".jpg", ignoreCase = true) ||
                                        file.name.endsWith(".webp", ignoreCase = true)
                                    )
                        }
                        heroFile?.let { android.net.Uri.fromFile(it).toString() }
                    }
                    heroCover ?: heroUrl ?: cachedSgdb("grid_hero") ?: appInfo.headerImageUrl
                }
            }
            GridImageUrls(primary = primary)
        }

        GameSource.GOG, GameSource.EPIC, GameSource.AMAZON -> {
            val sgdbType = when (paneType) {
                PaneType.GRID_CAPSULE -> "grid_capsule"
                else -> "grid_hero"
            }
            val sgdbUrl = cachedSgdb(sgdbType)
            val primary = when (paneType) {
                PaneType.GRID_CAPSULE ->
                    sgdbUrl ?: appInfo.capsuleImageUrl.ifEmpty { appInfo.iconHash }
                else ->
                    sgdbUrl ?: appInfo.headerImageUrl.ifEmpty {
                        appInfo.heroImageUrl.ifEmpty { appInfo.iconHash }
                    }
            }
            val fallback = when {
                sgdbUrl != null -> ""
                paneType == PaneType.GRID_CAPSULE ->
                    appInfo.iconHash.takeIf { it.isNotEmpty() && it != primary } ?: ""
                appInfo.heroImageUrl.isNotEmpty() && appInfo.heroImageUrl != primary ->
                    appInfo.heroImageUrl
                appInfo.iconHash.isNotEmpty() && appInfo.iconHash != primary ->
                    appInfo.iconHash
                else -> ""
            }
            GridImageUrls(primary = primary, fallback = fallback)
        }

        GameSource.STEAM -> {
            val sgdbType = when (paneType) {
                PaneType.GRID_CAPSULE -> "grid_capsule"
                else -> "grid_hero"
            }
            val sgdbUrl = cachedSgdb(sgdbType)
            when (paneType) {
                PaneType.GRID_CAPSULE ->
                    GridImageUrls(primary = sgdbUrl ?: appInfo.capsuleImageUrl)
                else ->
                    GridImageUrls(
                        primary = sgdbUrl ?: appInfo.headerImageUrl,
                        fallback = if (sgdbUrl != null) "" else appInfo.heroImageUrl,
                    )
            }
        }
    }
}
