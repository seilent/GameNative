package app.gamenative.ui.screen.settings

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.gamenative.R
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import app.gamenative.data.GameSource
import app.gamenative.ui.component.GlassSurface
import app.gamenative.ui.screen.library.GameMigrationDialog
import app.gamenative.ui.theme.GlassFill
import app.gamenative.ui.theme.GlassFillStrong
import app.gamenative.ui.theme.LocalGameAccent
import app.gamenative.ui.theme.PluviaTheme
import app.gamenative.ui.util.SnackbarManager
import app.gamenative.utils.ContainerStorageManager
import app.gamenative.utils.StorageUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

@Stable
class ContainerStorageManagerUiState internal constructor(
    private val appContext: Context,
    private val scope: CoroutineScope,
) {
    var entries by mutableStateOf<List<ContainerStorageManager.Entry>>(emptyList())
        private set

    var volumeInfo by mutableStateOf<List<ContainerStorageManager.VolumeInfo>>(emptyList())
        private set

    var isLoadingExternalVolume by mutableStateOf(false)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var hasLoaded by mutableStateOf(false)
        private set

    var pendingRemoval by mutableStateOf<ContainerStorageManager.Entry?>(null)
        private set

    var pendingUninstall by mutableStateOf<ContainerStorageManager.Entry?>(null)
        private set

    var uninstallingContainerId by mutableStateOf<String?>(null)
        private set

    var movingEntryName by mutableStateOf<String?>(null)
        private set

    var moveProgress by mutableFloatStateOf(0f)
        private set

    var moveCurrentFile by mutableStateOf("")
        private set

    var moveMovedFiles by mutableIntStateOf(0)
        private set

    var moveTotalFiles by mutableIntStateOf(0)
        private set

    val isMoving: Boolean
        get() = movingEntryName != null

    val isUninstalling: Boolean
        get() = uninstallingContainerId != null

    fun ensureLoaded() {
        if (!hasLoaded && !isLoading) {
            refresh()
        }
    }

    fun refresh() {
        if (isLoading) return

        scope.launch {
            isLoading = true

            val internal = ContainerStorageManager.getInternalVolumeInfo(appContext)
            volumeInfo = listOfNotNull(internal)

            val externalConfigured = ContainerStorageManager.isExternalStorageConfigured()
            isLoadingExternalVolume = externalConfigured
            val externalJob = launch {
                try {
                    val external = ContainerStorageManager.getExternalVolumeInfo(appContext)
                    if (external != null) {
                        volumeInfo = volumeInfo + external
                    }
                } finally {
                    isLoadingExternalVolume = false
                }
            }

            try {
                entries = ContainerStorageManager.loadEntries(appContext)
                hasLoaded = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                hasLoaded = false
                Timber.e(e, "Failed to load storage inventory")
                SnackbarManager.show(
                    e.message ?: appContext.getString(R.string.container_storage_unknown_error),
                )
            }

            externalJob.join()
            isLoading = false
        }
    }

    fun requestRemove(entry: ContainerStorageManager.Entry) {
        if (isMoving || isUninstalling) return
        pendingRemoval = entry
    }

    fun dismissRemove() {
        pendingRemoval = null
    }

    fun confirmRemove() {
        val entry = pendingRemoval ?: return
        pendingRemoval = null
        val entryName = entry.displayName.ifBlank {
            appContext.getString(R.string.container_storage_unknown_container)
        }

        scope.launch {
            val removed = ContainerStorageManager.removeContainer(appContext, entry.containerId)
            if (removed) {
                SnackbarManager.show(
                    appContext.getString(R.string.container_storage_remove_success, entryName),
                )
                refresh()
            } else {
                SnackbarManager.show(appContext.getString(R.string.container_storage_remove_failed))
            }
        }
    }

    fun requestUninstall(entry: ContainerStorageManager.Entry) {
        if (isMoving || isUninstalling) return
        pendingUninstall = entry
    }

    fun dismissUninstall() {
        pendingUninstall = null
    }

    fun confirmUninstall() {
        val entry = pendingUninstall ?: return
        pendingUninstall = null
        val entryName = entry.displayName.ifBlank {
            appContext.getString(R.string.container_storage_unknown_container)
        }

        uninstallingContainerId = entry.containerId
        scope.launch {
            try {
                val result = ContainerStorageManager.uninstallGameAndContainer(appContext, entry)
                if (result.isSuccess) {
                    SnackbarManager.show(
                        appContext.getString(R.string.container_storage_uninstall_success, entryName),
                    )
                    refresh()
                } else {
                    SnackbarManager.show(
                        appContext.getString(
                            R.string.container_storage_uninstall_failed,
                            result.exceptionOrNull()?.message
                                ?: appContext.getString(R.string.container_storage_unknown_error),
                        ),
                    )
                }
            } finally {
                uninstallingContainerId = null
            }
        }
    }

    fun startMove(
        entry: ContainerStorageManager.Entry,
        target: ContainerStorageManager.MoveTarget,
    ) {
        if (isMoving || isUninstalling) return

        if (target == ContainerStorageManager.MoveTarget.EXTERNAL && !ContainerStorageManager.isExternalStorageConfigured()) {
            SnackbarManager.show(appContext.getString(R.string.container_storage_move_external_disabled))
            return
        }

        val entryName = entry.displayName.ifBlank {
            appContext.getString(R.string.container_storage_unknown_container)
        }

        movingEntryName = entryName
        moveProgress = 0f
        moveCurrentFile = entryName
        moveMovedFiles = 0
        moveTotalFiles = 1

        scope.launch {
            val result = try {
                ContainerStorageManager.moveGame(
                    context = appContext,
                    entry = entry,
                    target = target,
                    onProgressUpdate = { currentFile, fileProgress, movedFiles, totalFiles ->
                        moveCurrentFile = currentFile
                        moveProgress = fileProgress
                        moveMovedFiles = movedFiles
                        moveTotalFiles = totalFiles
                    },
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Result.failure(error)
            } finally {
                movingEntryName = null
            }

            if (result.isSuccess) {
                SnackbarManager.show(
                    appContext.getString(
                        R.string.container_storage_move_success,
                        entryName,
                        appContext.getString(
                            if (target == ContainerStorageManager.MoveTarget.EXTERNAL) {
                                R.string.container_storage_location_external
                            } else {
                                R.string.container_storage_location_internal
                            },
                        ),
                    ),
                )
                refresh()
            } else {
                SnackbarManager.show(
                    appContext.getString(
                        R.string.container_storage_move_failed,
                        entryName,
                        result.exceptionOrNull()?.message
                            ?: appContext.getString(R.string.container_storage_unknown_error),
                    ),
                )
            }
        }
    }
}

@Composable
fun rememberContainerStorageManagerUiState(): ContainerStorageManagerUiState {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    return remember(context, scope) {
        ContainerStorageManagerUiState(
            appContext = context,
            scope = scope,
        )
    }
}

@Composable
fun ContainerStorageManagerTransientUi(
    state: ContainerStorageManagerUiState,
) {
    state.pendingRemoval?.let { entry ->
        val entryName = entry.displayName.ifBlank {
            stringResource(R.string.container_storage_unknown_container)
        }
        AlertDialog(
            onDismissRequest = state::dismissRemove,
            title = { Text(stringResource(R.string.container_storage_remove_title)) },
            text = { Text(stringResource(R.string.container_storage_remove_message, entryName)) },
            confirmButton = {
                TextButton(onClick = state::confirmRemove) {
                    Text(
                        text = stringResource(R.string.container_storage_remove_button),
                        color = PluviaTheme.colors.accentDanger,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = state::dismissRemove) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    state.pendingUninstall?.let { entry ->
        val entryName = entry.displayName.ifBlank {
            stringResource(R.string.container_storage_unknown_container)
        }
        AlertDialog(
            onDismissRequest = state::dismissUninstall,
            title = {
                Text(
                    stringResource(
                        if (entry.hasContainer) {
                            R.string.container_storage_uninstall_title
                        } else {
                            R.string.container_storage_uninstall_game_only_title
                        },
                    ),
                )
            },
            text = {
                Text(
                    stringResource(
                        if (entry.hasContainer) {
                            R.string.container_storage_uninstall_message
                        } else {
                            R.string.container_storage_uninstall_game_only_message
                        },
                        entryName,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = state::confirmUninstall) {
                    Text(
                        text = stringResource(R.string.container_storage_uninstall_button),
                        color = PluviaTheme.colors.accentDanger,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = state::dismissUninstall) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (state.isMoving) {
        GameMigrationDialog(
            progress = state.moveProgress,
            currentFile = state.moveCurrentFile,
            movedFiles = state.moveMovedFiles,
            totalFiles = state.moveTotalFiles,
        )
    }
}

@Composable
fun ContainerStorageManagerContent(
    state: ContainerStorageManagerUiState,
    modifier: Modifier = Modifier,
    onDismissRequest: (() -> Unit)? = null,
    onOpenGame: ((GameSource, String, String, String) -> Unit)? = null,
    targetFilter: app.gamenative.service.storage.StorageTarget? = null,
    leadingContent: (@Composable () -> Unit)? = null,
) {
    LaunchedEffect(state) {
        state.ensureLoaded()
    }

    val filteredEntries = if (targetFilter != null) {
        val prefix = java.io.File(targetFilter.rootPath).absolutePath
        state.entries.filter { entry ->
            entry.installPath != null && java.io.File(entry.installPath).absolutePath.startsWith(prefix)
        }
    } else {
        state.entries
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        if (onDismissRequest != null) {
            item(key = "close_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    IconButton(
                        onClick = onDismissRequest,
                        enabled = !state.isMoving,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                        )
                    }
                }
            }
        }

        if (leadingContent != null) {
            item(key = "leading_content") {
                leadingContent()
            }
        }

        when {
            state.isLoading && filteredEntries.isEmpty() -> {
                item(key = "loading") {
                    Box(
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(top = 48.dp),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CircularProgressIndicator(color = LocalGameAccent.current)
                            Text(
                                text = stringResource(R.string.container_storage_loading),
                                style = MaterialTheme.typography.bodyMedium,
                                color = PluviaTheme.colors.textMuted,
                            )
                        }
                    }
                }
            }

            filteredEntries.isEmpty() -> {
                item(key = "empty") {
                    Box(
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(top = 48.dp),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = PluviaTheme.colors.textMuted,
                                modifier = Modifier.size(64.dp),
                            )
                            Text(
                                text = stringResource(R.string.container_storage_empty),
                                style = MaterialTheme.typography.bodyLarge,
                                color = PluviaTheme.colors.textMuted,
                            )
                        }
                    }
                }
            }

            else -> {
                items(filteredEntries, key = { it.containerId }) { entry ->
                    StorageEntryCard(
                        entry = entry,
                        actionsEnabled = !state.isMoving && !state.isUninstalling,
                        isUninstalling = entry.containerId == state.uninstallingContainerId,
                        onOpenGame = onOpenGame,
                        onMoveToExternal = {
                            state.startMove(entry, ContainerStorageManager.MoveTarget.EXTERNAL)
                        },
                        onMoveToInternal = {
                            state.startMove(entry, ContainerStorageManager.MoveTarget.INTERNAL)
                        },
                        onRemove = { state.requestRemove(entry) },
                        onUninstall = { state.requestUninstall(entry) },
                    )
                }
            }
        }
    }
}

@Composable
fun ContainerStorageManagerDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    state: ContainerStorageManagerUiState = rememberContainerStorageManagerUiState(),
) {
    if (!visible) return

    ContainerStorageManagerTransientUi(state)

    Dialog(
        onDismissRequest = {
            if (!state.isMoving) {
                onDismissRequest()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .statusBarsPadding(),
            contentAlignment = Alignment.Center,
        ) {
            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.96f)
                    .widthIn(max = 1100.dp),
                shape = RoundedCornerShape(24.dp),
                fill = GlassFillStrong,
            ) {
                ContainerStorageManagerContent(
                    state = state,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PluviaTheme.colors.surfacePanel)
                        .padding(20.dp),
                    onDismissRequest = onDismissRequest,
                )
            }
        }
    }
}

@Composable
private fun StorageEntryCard(
    entry: ContainerStorageManager.Entry,
    actionsEnabled: Boolean,
    isUninstalling: Boolean,
    onOpenGame: ((GameSource, String, String, String) -> Unit)?,
    onMoveToExternal: () -> Unit,
    onMoveToInternal: () -> Unit,
    onRemove: () -> Unit,
    onUninstall: () -> Unit,
) {
    val context = LocalContext.current
    val accent = LocalGameAccent.current
    val displayName = entry.displayName.ifBlank {
        stringResource(R.string.container_storage_unknown_container)
    }
    val storageLocation = ContainerStorageManager.getStorageLocation(context, entry)
    val canMoveToExternal = ContainerStorageManager.canMoveToExternal(context, entry)
    val canMoveToInternal = ContainerStorageManager.canMoveToInternal(context, entry)
    val canOpenGame = onOpenGame != null && entry.gameSource != null && !entry.appId.isNullOrBlank()
    val canRemoveOrphanContainer = entry.hasContainer && !entry.canUninstallGame

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GlassFill),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StorageArtworkButton(
                imageUrl = entry.iconUrl,
                contentDescription = displayName,
                enabled = canOpenGame,
                onClick = {
                    val gameSource = entry.gameSource
                    val appId = entry.appId
                    if (gameSource != null && !appId.isNullOrBlank()) {
                        onOpenGame?.invoke(gameSource, appId, displayName, entry.iconUrl)
                    }
                },
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (storageLocation != ContainerStorageManager.StorageLocation.UNKNOWN) {
                        MetadataChip(
                            text = storageLocationLabel(storageLocation),
                            containerColor = GlassFillStrong,
                            contentColor = PluviaTheme.colors.textMuted,
                        )
                    }
                    entry.combinedSizeBytes?.let {
                        Text(
                            text = StorageUtils.formatBinarySize(it),
                            style = MaterialTheme.typography.labelSmall,
                            color = PluviaTheme.colors.textMuted,
                        )
                    }
                    if (isUninstalling) {
                        Text(
                            text = stringResource(R.string.container_storage_uninstalling),
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            if (canMoveToExternal || canMoveToInternal) {
                val moveShape = RoundedCornerShape(8.dp)
                var moveFocused by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(moveShape)
                        .then(if (moveFocused) Modifier.border(2.dp, accent, moveShape) else Modifier)
                        .onFocusChanged { moveFocused = it.isFocused }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = actionsEnabled,
                        ) { if (canMoveToExternal) onMoveToExternal() else onMoveToInternal() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = if (canMoveToExternal) {
                            stringResource(R.string.container_storage_move_to_external_button)
                        } else {
                            stringResource(R.string.container_storage_move_to_internal_button)
                        },
                        tint = PluviaTheme.colors.textMuted,
                    )
                }
            }

            if (entry.canUninstallGame) {
                val delShape = RoundedCornerShape(8.dp)
                var delFocused by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(delShape)
                        .then(if (delFocused) Modifier.border(2.dp, accent, delShape) else Modifier)
                        .onFocusChanged { delFocused = it.isFocused }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = actionsEnabled,
                        ) { onUninstall() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isUninstalling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = accent,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = stringResource(R.string.container_storage_uninstall_button),
                            tint = PluviaTheme.colors.accentDanger,
                        )
                    }
                }
            } else if (canRemoveOrphanContainer) {
                val remShape = RoundedCornerShape(8.dp)
                var remFocused by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(remShape)
                        .then(if (remFocused) Modifier.border(2.dp, accent, remShape) else Modifier)
                        .onFocusChanged { remFocused = it.isFocused }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = actionsEnabled,
                        ) { onRemove() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = stringResource(R.string.container_storage_remove_button),
                        tint = PluviaTheme.colors.accentDanger,
                    )
                }
            }
        }
    }
}

@Composable
private fun StorageArtworkButton(
    imageUrl: String,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val accentColor = LocalGameAccent.current

    Box(
        modifier = Modifier
            .size(44.dp)
            .background(
                color = if (isFocused) accentColor.copy(alpha = 0.18f) else GlassFill,
                shape = RoundedCornerShape(8.dp),
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) accentColor.copy(alpha = 0.7f) else PluviaTheme.colors.borderDefault,
                shape = RoundedCornerShape(8.dp),
            )
            .selectable(
                selected = isFocused,
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl.isNotBlank()) {
            CoilImage(
                imageModel = { imageUrl },
                imageOptions = ImageOptions(
                    contentScale = ContentScale.Crop,
                    contentDescription = contentDescription,
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp)),
            )
        } else {
            Icon(
                imageVector = Icons.Default.Storage,
                contentDescription = contentDescription,
                tint = if (enabled && isFocused) accentColor else PluviaTheme.colors.textMuted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun MetadataChip(
    text: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun storageLocationLabel(location: ContainerStorageManager.StorageLocation): String = when (location) {
    ContainerStorageManager.StorageLocation.INTERNAL -> stringResource(R.string.container_storage_location_internal)
    ContainerStorageManager.StorageLocation.EXTERNAL -> stringResource(R.string.container_storage_location_external)
    ContainerStorageManager.StorageLocation.UNKNOWN -> stringResource(R.string.container_storage_location_unknown)
}
