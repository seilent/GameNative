package app.gamenative.ui.component.dialog

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AirplaneTicket
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReplyAll
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.gamenative.R
import app.gamenative.service.SteamService
import app.gamenative.ui.screen.PluviaScreen
import app.gamenative.ui.theme.GlassBorder
import app.gamenative.ui.theme.GlassFillStrong
import app.gamenative.ui.theme.LocalAccentContainer
import app.gamenative.ui.theme.LocalGameAccent
import app.gamenative.ui.theme.PluviaTheme
import app.gamenative.ui.util.SteamIconImage
import app.gamenative.utils.getAvatarURL
import `in`.dragonbra.javasteam.enums.EPersonaState

@Composable
fun ProfileDialog(
    openDialog: Boolean,
    name: String,
    avatarHash: String,
    state: EPersonaState,
    onStatusChange: (EPersonaState) -> Unit,
    onNavigateRoute: (String) -> Unit,
    onLogout: () -> Unit,
    onDismiss: () -> Unit,
    onGoOnline: () -> Unit,
    isOffline: Boolean = false,
) {
    if (!openDialog) {
        return
    }

    var selectedItem by remember(state) { mutableStateOf(state) }
    var showSupporters by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    AlertDialog(
        containerColor = GlassFillStrong,
        onDismissRequest = onDismiss,
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val uriHandler = LocalUriHandler.current
                /* Icon, Name, and Status */
                ListItem(
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                    ),
                    leadingContent = {
                        SteamIconImage(
                            size = 48.dp,
                            image = { avatarHash.getAvatarURL() },
                        )
                    },
                    headlineContent = {
                        Text(text = name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    supportingContent = {
                        Text(text = state.name)
                    },
                )
                /* Online Status */
                Spacer(modifier = Modifier.height(16.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val status =
                        listOf(EPersonaState.Online, EPersonaState.Away, EPersonaState.Invisible)
                    status.forEachIndexed { index, state ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = status.size,
                            ),
                            onClick = {
                                selectedItem = state
                                onStatusChange(state)
                            },
                            selected = state == selectedItem,
                            label = {
                                Text(state.name)
                            },
                        )
                    }
                }

                /* Action Buttons - Scrollable */
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    val dialogBackgroundColor = GlassFillStrong
                    val showTopFade = remember {
                        derivedStateOf { scrollState.value > 0 }
                    }
                    val showBottomFade = remember {
                        derivedStateOf {
                            scrollState.value < scrollState.maxValue
                        }
                    }
                    
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1f)) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(scrollState)
                            ) {
                                AccentActionButton(
                                    icon = Icons.Default.Settings,
                                    label = stringResource(R.string.settings_text),
                                    onClick = { onNavigateRoute(PluviaScreen.Settings.route) },
                                )

                                AccentActionButton(
                                    icon = Icons.AutoMirrored.Filled.Help,
                                    label = stringResource(R.string.help_and_support),
                                    onClick = { uriHandler.openUri("https://discord.gg/2hKv4VfZfE") },
                                )

                                AccentActionButton(
                                    icon = Icons.AutoMirrored.Filled.StarHalf,
                                    label = stringResource(R.string.hall_of_fame),
                                    onClick = { showSupporters = true },
                                )

                                if(isOffline) {
                                    AccentActionButton(
                                        icon = Icons.AutoMirrored.Filled.Login,
                                        label = stringResource(R.string.go_online),
                                        onClick = onGoOnline,
                                    )
                                } else {
                                    AccentActionButton(
                                        icon = Icons.AutoMirrored.Filled.AirplaneTicket,
                                        label = stringResource(R.string.go_offline),
                                        onClick = { onNavigateRoute(PluviaScreen.Home.route + "?offline=true") },
                                    )
                                    AccentActionButton(
                                        icon = Icons.AutoMirrored.Filled.Logout,
                                        label = stringResource(R.string.log_out),
                                        onClick = onLogout,
                                    )
                                }
                            }
                            
                            // Top fade gradient
                            if (showTopFade.value) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .fillMaxWidth()
                                        .height(24.dp)
                                        .background(color = dialogBackgroundColor)
                                )
                            }
                            
                            // Bottom fade gradient
                            if (showBottomFade.value) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .height(24.dp)
                                        .background(color = dialogBackgroundColor)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.close), color = LocalGameAccent.current)
            }
        },
    )

    SupportersDialog(visible = showSupporters, onDismiss = { showSupporters = false })
}

@Composable
private fun AccentActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val accent = LocalGameAccent.current
    val accentContainer = LocalAccentContainer.current
    val shape = RoundedCornerShape(12.dp)
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(shape)
            .background(accentContainer, shape)
            .then(
                if (focused) Modifier.border(2.dp, accent, shape)
                else Modifier.border(1.dp, GlassBorder, shape)
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.size(8.dp))
            Text(text = label, color = Color.White, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_ProfileDialog() {
    PluviaTheme {
        ProfileDialog(
            openDialog = true,
            name = stringResource(R.string.app_name).repeat(4),
            avatarHash = "",
            state = EPersonaState.Online,
            onStatusChange = {},
            onNavigateRoute = {},
            onLogout = {},
            onDismiss = {},
            onGoOnline = {},
        )
    }
}
