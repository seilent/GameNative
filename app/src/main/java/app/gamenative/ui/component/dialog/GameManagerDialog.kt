package app.gamenative.ui.component.dialog

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.gamenative.R
import app.gamenative.data.DepotInfo
import app.gamenative.service.SteamService
import app.gamenative.service.SteamService.Companion.INVALID_APP_ID
import app.gamenative.service.storage.StorageManager
import app.gamenative.service.storage.StorageTarget
import app.gamenative.ui.component.BlurredBackdrop
import app.gamenative.ui.component.GlassSurface
import app.gamenative.ui.component.LoadingScreen
import app.gamenative.ui.component.NoExtractOutlinedTextField
import app.gamenative.ui.component.StorageTargetDropdown
import app.gamenative.ui.component.topbar.BackButton
import app.gamenative.ui.data.GameDisplayInfo
import app.gamenative.ui.internal.fakeAppInfo
import app.gamenative.ui.theme.GlassBorder
import app.gamenative.ui.theme.GlassFill
import app.gamenative.ui.theme.GlassFillStrong
import app.gamenative.ui.theme.LocalGameAccent
import app.gamenative.ui.theme.LocalOnAccent
import app.gamenative.ui.theme.PluviaTheme
import app.gamenative.utils.SteamUtils
import app.gamenative.utils.StorageUtils
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.collections.orEmpty
import kotlinx.coroutines.launch

data class InstallSizeInfo(
    val downloadSize: String,
    val installSize: String,
    val availableSpace: String,
    val installBytes: Long,
    val availableBytes: Long,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameManagerDialog(
    visible: Boolean,
    onGetDisplayInfo: @Composable (Context) -> GameDisplayInfo,
    onInstall: (List<Int>, StorageTarget?, String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val downloadableDepots = remember { mutableStateMapOf<Int, DepotInfo>() }
    val allDownloadableApps = remember { mutableStateListOf<Pair<Int, DepotInfo>>() }
    val selectedAppIds = remember { mutableStateMapOf<Int, Boolean>() }
    val enabledAppIds = remember { mutableStateMapOf<Int, Boolean>() }
    var selectedTarget by remember { mutableStateOf(StorageManager.defaultInstallTarget(context)) }

    val displayInfo = onGetDisplayInfo(context)
    val gameId = displayInfo.gameId

    val isBaseGameInstalled = remember(gameId) {
        SteamService.isAppInstalled(gameId)
    }
    val installedApp = remember(gameId, isBaseGameInstalled) {
        if (isBaseGameInstalled) SteamService.getInstalledApp(gameId) else null
    }
    val installedDlcIds = installedApp?.dlcDepots.orEmpty()

    val indirectDlcAppIds = remember(gameId) {
        SteamService.getDownloadableDlcAppsOf(gameId).orEmpty().map { it.id }
    }

    val mainAppDlcIdsWithoutProperDepotDlcIds = remember(gameId) {
        SteamService.getMainAppDlcIdsWithoutProperDepotDlcIds(gameId).toList()
    }

    val appBranches = remember(gameId) {
        SteamService.getAppInfoOf(gameId)?.branches.orEmpty()
    }
    val hasPrivateBranches = remember(appBranches) {
        appBranches.any { it.value.pwdRequired }
    }
    val publicBranches = remember(appBranches) {
        appBranches.filter { !it.value.pwdRequired }.keys.sorted()
    }
    var unlockedBranchNames by remember(gameId) { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(gameId) {
        unlockedBranchNames = SteamService.getSteamUnlockedBranches(gameId).map { it.branchName }
    }
    val availableBranches = remember(publicBranches, unlockedBranchNames) {
        (listOf("public") + publicBranches + unlockedBranchNames).distinct().sorted()
    }
    var selectedBranch by remember(gameId) {
        mutableStateOf(installedApp?.branch ?: "public")
    }
    val showVersionCard = availableBranches.size > 1 || hasPrivateBranches

    fun manifestFor(depot: DepotInfo) = depot.manifests[selectedBranch] ?: depot.manifests["public"]

    LaunchedEffect(visible) {
        scrollState.animateScrollTo(0)

        downloadableDepots.clear()
        allDownloadableApps.clear()

        val allPossibleDownloadableDepots = SteamService.getDownloadableDepots(gameId)
        downloadableDepots.putAll(allPossibleDownloadableDepots)

        val optionalDlcIds = allPossibleDownloadableDepots
            .filter { it.value.optionalDlcId == it.value.dlcAppId }
            .map { it.value.dlcAppId }

        downloadableDepots
            .toSortedMap()
            .filter { (_, depot) ->
                return@filter depot.dlcAppId != INVALID_APP_ID
            }.values
                .groupBy { it.dlcAppId }
                .mapValues { it.value.first() }
                .toMap()
            .forEach { (_, depotInfo) ->
                allDownloadableApps.add(Pair(depotInfo.dlcAppId, depotInfo))
                val installed = SteamService.isAppInstalled(depotInfo.dlcAppId)
                selectedAppIds[depotInfo.dlcAppId] =
                        installed ||
                        installedDlcIds.contains(depotInfo.dlcAppId) ||
                        ( !indirectDlcAppIds.contains(depotInfo.dlcAppId) && !optionalDlcIds.contains(depotInfo.dlcAppId) )

                enabledAppIds[depotInfo.dlcAppId] = !installedDlcIds.contains(depotInfo.dlcAppId) && !installed
            }

        allDownloadableApps.sortBy { it.first }

        val baseDepot = downloadableDepots.values.firstOrNull { it.dlcAppId == INVALID_APP_ID } ?: return@LaunchedEffect
        allDownloadableApps.add(0, Pair(gameId, baseDepot))
        selectedAppIds[gameId] = true
        enabledAppIds[gameId] = false
    }

    fun getDepotAppName(depotInfo: DepotInfo): String {
        if (depotInfo.dlcAppId == INVALID_APP_ID) {
            return displayInfo.name
        }

        val app = SteamService.getAppInfoOf(depotInfo.dlcAppId)
        if (app != null) {
            return app.name
        }

        return "DLC ${depotInfo.dlcAppId}"
    }

    fun getSizeInfo(dlcAppId: Int): Pair<String, String> {
        if (dlcAppId == INVALID_APP_ID || dlcAppId == gameId) {
            val depotsForBaseGame = downloadableDepots.filter { (_, depot) ->
                depot.dlcAppId == INVALID_APP_ID
            }

            val installBytes = depotsForBaseGame.values.sumOf {
                manifestFor(it)?.size ?: 0
            }
            val downloadBytes = depotsForBaseGame.values.sumOf {
                SteamUtils.getDownloadBytes(manifestFor(it))
            }

            return Pair(
                StorageUtils.formatBinarySize(downloadBytes),
                StorageUtils.formatBinarySize(installBytes)
            )
        }

        val depotsForDlc = downloadableDepots.filter { (_, depot) ->
            depot.dlcAppId == dlcAppId
        }

        val installBytes = depotsForDlc.values.sumOf {
            manifestFor(it)?.size ?: 0
        }
        val downloadBytes = depotsForDlc.values.sumOf {
            SteamUtils.getDownloadBytes(manifestFor(it))
        }

        return Pair(
            StorageUtils.formatBinarySize(downloadBytes),
            StorageUtils.formatBinarySize(installBytes)
        )
    }

    fun getInstallSizeInfo(): InstallSizeInfo {
        val availableBytes = StorageUtils.getAvailableSpaceForUncreatedPath(SteamService.getAppDirPath(gameId))

        val baseGameInstallBytes = if (!isBaseGameInstalled) {
            downloadableDepots
                .filter { (_, depot) ->
                    depot.dlcAppId == INVALID_APP_ID
                }.values.sumOf { manifestFor(it)?.size ?: 0 }
        } else {
            0L
        }

        val baseGameDownloadBytes = if (!isBaseGameInstalled) {
            downloadableDepots
                .filter { (_, depot) ->
                    depot.dlcAppId == INVALID_APP_ID
                }.values.sumOf {
                    SteamUtils.getDownloadBytes(manifestFor(it))
                }
        } else {
            0L
        }

        val selectedInstallBytes = downloadableDepots
            .filter { (_, depot) ->
                selectedAppIds[depot.dlcAppId] == true && enabledAppIds[depot.dlcAppId] == true
            }
            .values.sumOf { manifestFor(it)?.size ?: 0 }

        val selectedDownloadBytes = downloadableDepots
            .filter { (_, depot) ->
                selectedAppIds[depot.dlcAppId] == true && enabledAppIds[depot.dlcAppId] == true
            }
            .values.sumOf {
                SteamUtils.getDownloadBytes(manifestFor(it))
            }

        return InstallSizeInfo(
            downloadSize = StorageUtils.formatBinarySize(baseGameDownloadBytes + selectedDownloadBytes),
            installSize = StorageUtils.formatBinarySize(baseGameInstallBytes + selectedInstallBytes),
            availableSpace = StorageUtils.formatBinarySize(availableBytes),
            installBytes = baseGameInstallBytes + selectedInstallBytes,
            availableBytes = availableBytes
        )
    }

    val selectableAppIds by remember(enabledAppIds.toMap()) {
        derivedStateOf {
            enabledAppIds.filter { it.value }.keys.toList()
        }
    }

    val allSelectableSelected by remember(selectedAppIds.toMap(), selectableAppIds) {
        derivedStateOf {
            selectableAppIds.isNotEmpty() && selectableAppIds.all { selectedAppIds[it] == true }
        }
    }

    val installSizeInfo by remember(downloadableDepots.keys.toSet(), selectedAppIds.toMap(), enabledAppIds.toMap(), selectedBranch) {
        derivedStateOf { getInstallSizeInfo() }
    }

    fun installSizeDisplay() : String {
        return context.getString(
            R.string.steam_install_space,
            installSizeInfo.downloadSize,
            installSizeInfo.installSize,
            installSizeInfo.availableSpace
        )
    }

    fun installButtonEnabled() : Boolean {
        if (installSizeInfo.availableBytes < installSizeInfo.installBytes) {
            return false
        }

        if (isBaseGameInstalled) {
            val installed = installedDlcIds.toSet() - mainAppDlcIdsWithoutProperDepotDlcIds.toSet()
            val realSelectedAppIds = selectedAppIds.filter { it.value }.keys - installed
            return (realSelectedAppIds.size - 1) > 0
        }

        return selectedAppIds.filter { it.value }.isNotEmpty()
    }

    when {
        visible -> {
            Dialog(
                onDismissRequest = onDismissRequest,
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnClickOutside = false,
                ),
                content = {
                    Box(modifier = Modifier.fillMaxSize()) {
                    BlurredBackdrop(
                        imageModel = displayInfo.heroImageUrl,
                        accentKey = displayInfo.heroImageUrl,
                        blurRadius = 28,
                        onAccent = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                        ) {
                            if (displayInfo.heroImageUrl != null) {
                                CoilImage(
                                    modifier = Modifier.fillMaxSize(),
                                    imageModel = { displayInfo.heroImageUrl },
                                    imageOptions = ImageOptions(contentScale = ContentScale.Crop),
                                    loading = { LoadingScreen() },
                                    failure = {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Surface(
                                                modifier = Modifier.fillMaxSize(),
                                                color = LocalGameAccent.current
                                            ) { }
                                        }
                                    },
                                    previewPlaceholder = painterResource(R.drawable.testhero),
                                )
                            } else {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    color = LocalGameAccent.current
                                ) { }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color = GlassFillStrong)
                            )

                            Box(
                                modifier = Modifier
                                    .padding(20.dp)
                                    .background(
                                        color = Color.White.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            ) {
                                BackButton(onClick = onDismissRequest)
                            }

                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(20.dp)
                            ) {
                                Text(
                                    text = displayInfo.name,
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        shadow = Shadow(
                                            color = Color.Black.copy(alpha = 0.5f),
                                            offset = Offset(0f, 2f),
                                            blurRadius = 10f
                                        )
                                    ),
                                    color = Color.White
                                )

                                Text(
                                    text = "${displayInfo.developer} • ${
                                        remember(displayInfo.releaseDate) {
                                            if (displayInfo.releaseDate > 0) {
                                                SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(displayInfo.releaseDate * 1000))
                                            } else {
                                                ""
                                            }
                                        }
                                    }",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }

                        if (showVersionCard) {
                            InstallSectionCard(title = stringResource(R.string.game_version_section)) {
                                BranchSelector(
                                    selectedBranch = selectedBranch,
                                    availableBranches = availableBranches,
                                    onBranchSelected = { selectedBranch = it },
                                    showAccessCode = hasPrivateBranches,
                                    onCheckAccessCode = { code ->
                                        val result = SteamService.checkPrivateBranchPassword(gameId, code)
                                        if (result.isNotEmpty()) {
                                            unlockedBranchNames = SteamService.getSteamUnlockedBranches(gameId).map { it.branchName }
                                            result.keys.firstOrNull()?.let { selectedBranch = it }
                                            true
                                        } else {
                                            false
                                        }
                                    },
                                )
                            }
                        }

                        InstallSectionCard(
                            title = stringResource(R.string.game_content_section),
                            trailing = {
                                if (selectableAppIds.isNotEmpty()) {
                                    AccentTextAction(
                                        text = if (allSelectableSelected) {
                                            stringResource(R.string.deselect_all)
                                        } else {
                                            stringResource(R.string.select_all)
                                        },
                                        onClick = {
                                            val newState = !allSelectableSelected
                                            selectableAppIds.forEach { appId ->
                                                selectedAppIds[appId] = newState
                                            }
                                        },
                                    )
                                }
                            },
                        ) {
                            allDownloadableApps.forEach { (dlcAppId, depotInfo) ->
                                val checked = selectedAppIds[dlcAppId] ?: false
                                val enabled = enabledAppIds[dlcAppId] ?: false
                                val (downloadSize, installSize) = getSizeInfo(dlcAppId)

                                AccentCheckRow(
                                    name = getDepotAppName(depotInfo),
                                    subtitle = "$downloadSize download • $installSize install",
                                    checked = checked,
                                    enabled = enabled,
                                    onToggle = { selectedAppIds[dlcAppId] = !checked },
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            StorageTargetDropdown(
                                selectedTarget = selectedTarget,
                                onTargetSelected = { selectedTarget = it },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    modifier = Modifier.weight(1f),
                                    text = installSizeDisplay(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f),
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                AccentPill(
                                    text = stringResource(R.string.install),
                                    enabled = installButtonEnabled(),
                                    onClick = {
                                        onInstall(
                                            selectedAppIds
                                                .filter { selectedId -> selectedId.key in enabledAppIds.filter { enabledId -> enabledId.value } }
                                                .filter { selectedId -> selectedId.value }.keys.toList(),
                                            selectedTarget,
                                            selectedBranch,
                                        )
                                    },
                                )
                            }
                        }
                    }
                    }
                },
            )
        }
    }
}

@Composable
private fun InstallSectionCard(
    title: String,
    trailing: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title.uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.labelMedium,
                    color = PluviaTheme.colors.textMuted,
                    fontWeight = FontWeight.SemiBold,
                )
                trailing()
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun AccentCheckRow(
    name: String,
    subtitle: String?,
    checked: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    val accent = LocalGameAccent.current
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (focused) Modifier.border(1.dp, accent, shape) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
            ) { onToggle() }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AccentCheckBox(checked = checked, enabled = enabled)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = PluviaTheme.colors.textMuted,
                )
            }
        }
    }
}

@Composable
private fun AccentCheckBox(checked: Boolean, enabled: Boolean) {
    val accent = LocalGameAccent.current
    val shape = RoundedCornerShape(6.dp)
    val fill = if (checked) accent.copy(alpha = if (enabled) 1f else 0.4f) else Color.Transparent

    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(shape)
            .background(fill, shape)
            .then(if (checked) Modifier else Modifier.border(1.5.dp, GlassBorder, shape)),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = LocalOnAccent.current,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun AccentPill(
    text: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val accent = LocalGameAccent.current
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    val fill = if (enabled) accent else accent.copy(alpha = 0.3f)

    Box(
        modifier = modifier
            .clip(shape)
            .background(fill, shape)
            .then(if (focused) Modifier.border(2.dp, Color.White.copy(alpha = 0.7f), shape) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
            ) { onClick() }
            .padding(horizontal = 22.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = LocalOnAccent.current.copy(alpha = if (enabled) 1f else 0.7f),
        )
    }
}

@Composable
private fun AccentTextAction(text: String, onClick: () -> Unit) {
    val accent = LocalGameAccent.current
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)

    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = accent,
        modifier = Modifier
            .clip(shape)
            .then(if (focused) Modifier.border(1.dp, accent, shape) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun BranchSelector(
    selectedBranch: String,
    availableBranches: List<String>,
    onBranchSelected: (String) -> Unit,
    showAccessCode: Boolean,
    onCheckAccessCode: suspend (String) -> Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        BranchDropdown(
            selectedBranch = selectedBranch,
            availableBranches = availableBranches,
            onBranchSelected = onBranchSelected,
        )

        if (showAccessCode) {
            var accessCode by remember { mutableStateOf("") }
            var codeError by remember { mutableStateOf(false) }
            var codeSuccess by remember { mutableStateOf(false) }
            var checking by remember { mutableStateOf(false) }
            val coroutineScope = rememberCoroutineScope()

            Spacer(modifier = Modifier.height(12.dp))
            NoExtractOutlinedTextField(
                value = accessCode,
                onValueChange = {
                    accessCode = it
                    codeError = false
                    codeSuccess = false
                },
                label = { Text(stringResource(R.string.private_branch_password_hint)) },
                singleLine = true,
                isError = codeError,
                supportingText = when {
                    codeError -> ({ Text(stringResource(R.string.private_branch_password_invalid)) })
                    codeSuccess -> ({ Text(stringResource(R.string.private_branch_password_success)) })
                    else -> null
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(10.dp))
            AccentPill(
                text = stringResource(R.string.private_branch_password_check),
                enabled = accessCode.isNotBlank() && !checking,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    checking = true
                    codeError = false
                    codeSuccess = false
                    coroutineScope.launch {
                        val ok = onCheckAccessCode(accessCode)
                        codeSuccess = ok
                        codeError = !ok
                        checking = false
                    }
                },
            )
        }
    }
}

@Composable
private fun BranchDropdown(
    selectedBranch: String,
    availableBranches: List<String>,
    onBranchSelected: (String) -> Unit,
) {
    val accent = LocalGameAccent.current
    var expanded by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(shape)
                .then(if (focused) Modifier.border(1.dp, accent, shape) else Modifier.border(1.dp, GlassBorder, shape))
                .background(GlassFill, shape)
                .onFocusChanged { focused = it.isFocused }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { expanded = true }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.CallSplit,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = accent,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = selectedBranch,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = PluviaTheme.colors.textMuted,
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = GlassFillStrong,
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = BorderStroke(1.dp, GlassBorder),
        ) {
            availableBranches.forEach { branch ->
                val isSelected = branch == selectedBranch
                DropdownMenuItem(
                    text = {
                        Text(
                            text = branch,
                            color = if (isSelected) accent else Color.White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        onBranchSelected(branch)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
fun Preview_GameManagerDialog() {
    val fakeApp = fakeAppInfo(1)
    val displayInfo = GameDisplayInfo(
        name = fakeApp.name,
        developer = fakeApp.developer,
        releaseDate = fakeApp.releaseDate,
        heroImageUrl = fakeApp.getHeroUrl(),
        iconUrl = fakeApp.iconUrl,
        gameId = fakeApp.id,
        appId = "STEAM_${fakeApp.id}",
        installLocation = null,
        sizeOnDisk = null,
        sizeFromStore = null,
        lastPlayedText = null,
        playtimeText = null,
    )

    PluviaTheme {
        GameManagerDialog(
            visible = true,
            onGetDisplayInfo = {
                return@GameManagerDialog displayInfo
            },
            onInstall = { _, _, _ -> },
            onDismissRequest = {}
        )
    }
}
