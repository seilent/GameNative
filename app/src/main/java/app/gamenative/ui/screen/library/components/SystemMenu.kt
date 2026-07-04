package app.gamenative.ui.screen.library.components

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AirplaneTicket
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.gamenative.PluviaApp
import app.gamenative.PrefManager
import app.gamenative.R
import app.gamenative.data.SteamFriend
import app.gamenative.events.SteamEvent
import app.gamenative.service.SteamService
import app.gamenative.ui.component.BlurredBackdrop
import app.gamenative.ui.component.GlassSurface
import app.gamenative.ui.component.dialog.SupportersDialog
import app.gamenative.ui.controller.acquireControllerFocus
import app.gamenative.ui.screen.PluviaScreen
import app.gamenative.ui.theme.GlassFill
import app.gamenative.ui.theme.GlassFillStrong
import app.gamenative.ui.theme.GlassBorder
import app.gamenative.ui.theme.LocalGameAccent
import app.gamenative.ui.theme.LocalGameBackdrop
import app.gamenative.ui.theme.Motion
import app.gamenative.ui.theme.PluviaTheme
import app.gamenative.ui.util.SteamIconImage
import app.gamenative.ui.util.adaptivePanelWidth
import app.gamenative.utils.getAvatarURL
import `in`.dragonbra.javasteam.enums.EPersonaState
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * A single menu item in the System Menu
 */
@Composable
private fun SystemMenuItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    isDestructive: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1f,
        animationSpec = Motion.FocusScale,
        label = "menuItemScale",
    )

    val backgroundColor = when {
        isFocused -> LocalGameAccent.current.copy(alpha = 0.18f)
        else -> Color.Transparent
    }

    val contentColor = when {
        isDestructive && isFocused -> MaterialTheme.colorScheme.error
        isDestructive -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
        isFocused -> LocalGameAccent.current
        else -> Color.White
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .focusRequester(focusRequester)
            .selectable(
                selected = isFocused,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

/**
 * Status option item for the dropdown
 */
@Composable
private fun StatusOption(
    text: String,
    statusColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1f,
        animationSpec = Motion.FocusScale,
        label = "statusOptionScale",
    )

    val backgroundColor = when {
        isFocused -> LocalGameAccent.current.copy(alpha = 0.18f)
        isSelected -> GlassFill
        else -> Color.Transparent
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .selectable(
                selected = isFocused,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(statusColor, CircleShape),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isFocused) {
                LocalGameAccent.current
            } else {
                Color.White
            },
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = LocalGameAccent.current,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * Full-screen System Menu
 * Opens with START button, shows profile and system settings
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SystemMenu(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onNavigateRoute: (String) -> Unit,
    onDownloadsClick: () -> Unit = {},
    onLogout: () -> Unit,
    onGoOnline: () -> Unit,
    isOffline: Boolean = false,
    gogLoggedIn: Boolean,
    epicLoggedIn: Boolean,
    amazonLoggedIn: Boolean,
    onGogLoginClick: () -> Unit,
    onGogLogoutClick: () -> Unit,
    onEpicLoginClick: () -> Unit,
    onEpicLogoutClick: () -> Unit,
    onAmazonLoginClick: () -> Unit,
    onAmazonLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val firstItemFocusRequester = remember { FocusRequester() }
    val profileFocusRequester = remember { FocusRequester() }
    val inputModeManager = LocalInputModeManager.current

    var persona by remember { mutableStateOf<SteamFriend?>(null) }
    var selectedStatus by remember(persona) { mutableStateOf(persona?.state ?: EPersonaState.Online) }
    var showSupporters by remember { mutableStateOf(false) }
    var showStatusPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        persona = SteamService.instance?.localPersona?.value
        SteamService.userSteamId?.let {
            SteamService.requestUserPersona()
        }
    }

    DisposableEffect(true) {
        val onPersonaStateReceived: (SteamEvent.PersonaStateReceived) -> Unit = { event ->
            Timber.d("SystemMenu onPersonaStateReceived: ${event.persona.state}")
            persona = event.persona
            selectedStatus = event.persona.state
        }

        val onLoggedOut: (SteamEvent.LoggedOut) -> Unit = {
            persona = SteamFriend()
            showStatusPicker = false
        }

        PluviaApp.events.on<SteamEvent.PersonaStateReceived, Unit>(onPersonaStateReceived)
        PluviaApp.events.on<SteamEvent.LoggedOut, Unit>(onLoggedOut)

        onDispose {
            PluviaApp.events.off<SteamEvent.PersonaStateReceived, Unit>(onPersonaStateReceived)
            PluviaApp.events.off<SteamEvent.LoggedOut, Unit>(onLoggedOut)
        }
    }

    BackHandler(enabled = isOpen && showStatusPicker) {
        showStatusPicker = false
    }
    BackHandler(enabled = isOpen && !showStatusPicker) {
        onDismiss()
    }

    SupportersDialog(visible = showSupporters, onDismiss = { showSupporters = false })

    val colorOnline = PluviaTheme.colors.statusInstalled
    val colorAway = PluviaTheme.colors.statusAway
    val colorOffline = PluviaTheme.colors.statusOffline

    val getStatusColor: (EPersonaState) -> Color = { state ->
        when (state) {
            EPersonaState.Online -> colorOnline
            EPersonaState.Away -> colorAway
            EPersonaState.Invisible, EPersonaState.Offline -> colorOffline
            else -> colorOnline
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Backdrop
        AnimatedVisibility(
            visible = isOpen,
            enter = fadeIn(animationSpec = Motion.Fade),
            exit = fadeOut(animationSpec = Motion.Fade),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
        }

        // Menu panel - slides from right
        AnimatedVisibility(
            visible = isOpen,
            enter = slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = Motion.PanelSlide,
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = Motion.PanelSlide,
            ),
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            val panelShape = RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp)
            Box(
                modifier = Modifier
                    .width(adaptivePanelWidth(380.dp))
                    .fillMaxHeight()
                    .clip(panelShape)
                    .focusProperties { exit = { FocusRequester.Cancel } }
                    .focusGroup(),
            ) {
                val backdrop = LocalGameBackdrop.current
                if (backdrop.isNotBlank()) {
                    BlurredBackdrop(
                        imageModel = backdrop,
                        accentKey = null,
                        blurRadius = 28,
                        onAccent = {},
                        modifier = Modifier.matchParentSize(),
                    )
                }
                GlassSurface(
                    modifier = Modifier.matchParentSize(),
                    shape = panelShape,
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(24.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.system_menu_title),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = Color.White,
                        )
                        var dismissFocused by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .then(
                                    if (dismissFocused) Modifier.border(2.dp, LocalGameAccent.current, CircleShape)
                                    else Modifier
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onDismiss,
                                )
                                .onFocusChanged { dismissFocused = it.isFocused },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.options_panel_close),
                                tint = PluviaTheme.colors.textMuted,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Profile section
                    val profileInteractionSource = remember { MutableInteractionSource() }
                    val isProfileFocused by profileInteractionSource.collectIsFocusedAsState()
                    val profileScale by animateFloatAsState(
                        targetValue = if (isProfileFocused) 1.02f else 1f,
                        animationSpec = Motion.FocusScale,
                        label = "profileScale",
                    )

                    Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(profileScale)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isProfileFocused) {
                                        LocalGameAccent.current.copy(alpha = 0.18f)
                                    } else {
                                        GlassFill
                                    },
                                )
                                .then(
                                    if (isProfileFocused) {
                                        Modifier.border(
                                            2.dp,
                                            LocalGameAccent.current,
                                            RoundedCornerShape(16.dp),
                                        )
                                    } else {
                                        Modifier
                                    },
                                )
                                .focusRequester(profileFocusRequester)
                                .selectable(
                                    selected = isProfileFocused,
                                    interactionSource = profileInteractionSource,
                                    indication = null,
                                    onClick = { if (!isOffline && SteamService.isLoggedIn) showStatusPicker = !showStatusPicker },
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(GlassFill),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (persona?.avatarHash?.isNotEmpty() == true) {
                                    SteamIconImage(
                                        size = 48.dp,
                                        image = { persona?.avatarHash?.getAvatarURL() },
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = PluviaTheme.colors.textMuted,
                                    )
                                }
                            }

                            // Name and status
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = persona?.name ?: stringResource(R.string.default_user_name),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(getStatusColor(selectedStatus), CircleShape),
                                    )
                                    Text(
                                        text = when (selectedStatus) {
                                            EPersonaState.Online -> stringResource(R.string.status_online)
                                            EPersonaState.Away -> stringResource(R.string.status_away)
                                            EPersonaState.Invisible -> stringResource(R.string.status_invisible)
                                            else -> selectedStatus.name
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PluviaTheme.colors.textMuted,
                                    )
                                }
                            }

                            // Dropdown indicator (only when online)
                            if (!isOffline && SteamService.isLoggedIn) {
                                Icon(
                                    imageVector = if (showStatusPicker) {
                                        Icons.Default.KeyboardArrowUp
                                    } else {
                                        Icons.Default.KeyboardArrowDown
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = PluviaTheme.colors.textMuted,
                                )
                            }
                        }

                        // Status picker dropdown
                        androidx.compose.material3.DropdownMenu(
                            expanded = showStatusPicker,
                            onDismissRequest = { showStatusPicker = false },
                            containerColor = GlassFillStrong,
                            shape = RoundedCornerShape(12.dp),
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                            border = BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier
                                .width(280.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .focusGroup(),
                            ) {
                                Text(
                                    text = stringResource(R.string.status),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = PluviaTheme.colors.textMuted,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                )
                                listOf(
                                    Triple(EPersonaState.Online, stringResource(R.string.status_online), PluviaTheme.colors.statusInstalled),
                                    Triple(EPersonaState.Away, stringResource(R.string.status_away), PluviaTheme.colors.statusAway),
                                    Triple(EPersonaState.Invisible, stringResource(R.string.status_invisible), PluviaTheme.colors.statusOffline),
                                ).forEach { (state, label, color) ->
                                    StatusOption(
                                        text = label,
                                        statusColor = color,
                                        isSelected = selectedStatus == state,
                                        onClick = {
                                            selectedStatus = state
                                            showStatusPicker = false
                                            scope.launch {
                                                SteamService.setPersonaState(state)
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    HorizontalDivider(
                        color = GlassBorder.copy(alpha = 0.3f),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Menu items
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .focusGroup(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        SystemMenuItem(
                            text = stringResource(R.string.settings_text),
                            icon = Icons.Default.Settings,
                            onClick = {
                                onNavigateRoute(PluviaScreen.Settings.route)
                                onDismiss()
                            },
                            focusRequester = firstItemFocusRequester,
                        )

                        SystemMenuItem(
                            text = stringResource(R.string.app_downloads),
                            icon = Icons.Default.Download,
                            onClick = {
                                onDownloadsClick()
                                onDismiss()
                            },
                            focusRequester = firstItemFocusRequester,
                        )

                        SystemMenuItem(
                            text = stringResource(R.string.help_and_support),
                            icon = Icons.AutoMirrored.Filled.Help,
                            onClick = {
                                uriHandler.openUri("https://discord.gg/2hKv4VfZfE")
                            },
                        )

                        SystemMenuItem(
                            text = stringResource(R.string.hall_of_fame),
                            icon = Icons.AutoMirrored.Filled.StarHalf,
                            onClick = { showSupporters = true },
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        HorizontalDivider(
                            color = GlassBorder.copy(alpha = 0.3f),
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isOffline || !SteamService.isLoggedIn) {
                            val goOnlineLabelRes = if (!SteamService.isLoggedIn) {
                                R.string.steam_sign_in
                            } else {
                                R.string.steam_go_online
                            }
                            SystemMenuItem(
                                text = stringResource(goOnlineLabelRes),
                                icon = Icons.AutoMirrored.Filled.Login,
                                onClick = {
                                    onGoOnline()
                                    onDismiss()
                                },
                            )
                        } else {
                            SystemMenuItem(
                                text = stringResource(R.string.steam_go_offline),
                                icon = Icons.AutoMirrored.Filled.AirplaneTicket,
                                onClick = {
                                    onNavigateRoute(PluviaScreen.Home.route + "?offline=true") // TODO: test this
                                    onDismiss()
                                },
                            )

                            SystemMenuItem(
                                text = stringResource(R.string.steam_sign_out),
                                icon = Icons.AutoMirrored.Filled.Logout,
                                onClick = {
                                    onLogout()
                                    onDismiss()
                                },
                                isDestructive = true,
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        HorizontalDivider(
                            color = GlassBorder.copy(alpha = 0.3f),
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // GOG
                        SystemMenuItem(
                            text = stringResource(
                                if (gogLoggedIn) R.string.gog_settings_logout_title
                                else R.string.gog_settings_login_title,
                            ),
                            icon = if (gogLoggedIn) {
                                Icons.AutoMirrored.Filled.Logout
                            } else {
                                Icons.AutoMirrored.Filled.Login
                            },
                            onClick = {
                                if (gogLoggedIn) onGogLogoutClick() else onGogLoginClick()
                                onDismiss()
                            },
                            isDestructive = gogLoggedIn,
                        )

                        // Epic
                        SystemMenuItem(
                            text = stringResource(
                                if (epicLoggedIn) R.string.epic_settings_logout_title
                                else R.string.epic_settings_login_title,
                            ),
                            icon = if (epicLoggedIn) {
                                Icons.AutoMirrored.Filled.Logout
                            } else {
                                Icons.AutoMirrored.Filled.Login
                            },
                            onClick = {
                                if (epicLoggedIn) onEpicLogoutClick() else onEpicLoginClick()
                                onDismiss()
                            },
                            isDestructive = epicLoggedIn,
                        )

                        // Amazon
                        SystemMenuItem(
                            text = stringResource(
                                if (amazonLoggedIn) R.string.amazon_settings_logout_title
                                else R.string.amazon_settings_login_title,
                            ),
                            icon = if (amazonLoggedIn) {
                                Icons.AutoMirrored.Filled.Logout
                            } else {
                                Icons.AutoMirrored.Filled.Login
                            },
                            onClick = {
                                if (amazonLoggedIn) onAmazonLogoutClick() else onAmazonLoginClick()
                                onDismiss()
                            },
                            isDestructive = amazonLoggedIn,
                        )
                    }
                }
            }
            }
        }
    }

    // Request focus on first item when menu opens
    LaunchedEffect(isOpen) {
        if (isOpen) {
            acquireControllerFocus(inputModeManager, firstItemFocusRequester)
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    device = "spec:width=1920px,height=1080px,dpi=440,orientation=landscape",
)
@Composable
private fun Preview_SystemMenu() {
    val context = LocalContext.current
    PrefManager.init(context)
    PluviaTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Fake background content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Game Library",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White.copy(alpha = 0.5f),
                    )
                }

                SystemMenu(
                    isOpen = true,
                    onDismiss = { },
                    onNavigateRoute = { },
                    onLogout = { },
                    onGoOnline = { },
                    isOffline = false,
                    gogLoggedIn = false,
                    epicLoggedIn = false,
                    amazonLoggedIn = false,
                    onGogLoginClick = { },
                    onGogLogoutClick = { },
                    onEpicLoginClick = { },
                    onEpicLogoutClick = { },
                    onAmazonLoginClick = { },
                    onAmazonLogoutClick = { },
                )
            }
        }
    }
}
