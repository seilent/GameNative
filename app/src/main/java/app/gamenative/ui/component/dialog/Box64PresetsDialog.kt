package app.gamenative.ui.component.dialog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.gamenative.R
import app.gamenative.ui.component.NoExtractOutlinedTextField
import app.gamenative.ui.component.settings.SettingsEnvVars
import app.gamenative.ui.theme.GlassBorder
import app.gamenative.ui.theme.GlassFillStrong
import app.gamenative.ui.theme.LocalGameAccent
import app.gamenative.ui.theme.settingsTileColors
import com.winlator.box86_64.Box86_64Preset
import com.winlator.box86_64.Box86_64PresetManager
import com.winlator.core.StringUtils
import com.winlator.core.envvars.EnvVarInfo
import com.winlator.core.envvars.EnvVars
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Box64PresetsDialog(
    visible: Boolean = true,
    onDismissRequest: () -> Unit,
) {
    if (visible) {
        val context = LocalContext.current
        val prefix = "box64"
        val scrollState = rememberScrollState()

        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnClickOutside = false,
            ),
            content = {
                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                            title = { Text(text = stringResource(R.string.box64_presets)) },
                            actions = {
                                val accent = LocalGameAccent.current
                                val shape = RoundedCornerShape(8.dp)
                                var focused by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(shape)
                                        .then(if (focused) Modifier.border(2.dp, accent, shape) else Modifier)
                                        .onFocusChanged { focused = it.isFocused }
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                        ) { onDismissRequest() },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Default.Done, "Close Box64 Presets", tint = accent)
                                }
                            },
                        )
                    },
                ) { paddingValues ->
                    val getPresets: () -> ArrayList<Box86_64Preset> = { Box86_64PresetManager.getPresets(prefix, context) }
                    val getPreset: (String) -> Box86_64Preset = { id -> getPresets().first { it.id == id } }
                    var showPresets by rememberSaveable { mutableStateOf(false) }
                    var presetId by rememberSaveable { mutableStateOf(getPresets().first().id) }
                    var presetName by rememberSaveable { mutableStateOf(getPreset(presetId).name) }
                    var envVars by rememberSaveable {
                        mutableStateOf(
                            Box86_64PresetManager.getEnvVars(prefix, context, getPreset(presetId).id).toString(),
                        )
                    }
                    val isCustom: () -> Boolean = { getPreset(presetId).isCustom }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                    ) {
                        NoExtractOutlinedTextField(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            value = presetName,
                            enabled = isCustom(),
                            onValueChange = {
                                presetName = it.replace("|", "")
                                Box86_64PresetManager.editPreset(prefix, context, presetId, presetName, EnvVars(envVars))
                            },
                            label = { Text(stringResource(R.string.preset_name)) },
                            singleLine = true,
                            trailingIcon = {
                                val accent = LocalGameAccent.current
                                val iconShape = RoundedCornerShape(8.dp)
                                var iconFocused by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(iconShape)
                                        .then(if (iconFocused) Modifier.border(2.dp, accent, iconShape) else Modifier)
                                        .onFocusChanged { iconFocused = it.isFocused }
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                        ) { showPresets = true },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.AutoMirrored.Outlined.ViewList, contentDescription = "Preset list", tint = Color.White)
                                }
                                DropdownMenu(
                                    expanded = showPresets,
                                    onDismissRequest = { showPresets = false },
                                    containerColor = GlassFillStrong,
                                    shape = RoundedCornerShape(12.dp),
                                    tonalElevation = 0.dp,
                                    shadowElevation = 0.dp,
                                    border = BorderStroke(1.dp, GlassBorder),
                                    content = {
                                        for (preset in getPresets()) {
                                            DropdownMenuItem(
                                                text = { Text(preset.name) },
                                                onClick = {
                                                    presetId = preset.id
                                                    presetName = getPreset(presetId).name
                                                    envVars = Box86_64PresetManager.getEnvVars(
                                                        prefix,
                                                        context,
                                                        getPreset(presetId).id,
                                                    ).toString()
                                                    showPresets = false
                                                },
                                            )
                                        }
                                    },
                                )
                            },
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(stringResource(R.string.environment_variables))
                            Row {
                                val accent = LocalGameAccent.current
                                val btnShape = RoundedCornerShape(8.dp)
                                var dupFocused by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(btnShape)
                                        .then(if (dupFocused) Modifier.border(2.dp, accent, btnShape) else Modifier)
                                        .onFocusChanged { dupFocused = it.isFocused }
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                        ) {
                                            presetId = Box86_64PresetManager.duplicatePreset(prefix, context, presetId)
                                            presetName = getPreset(presetId).name
                                            envVars = Box86_64PresetManager.getEnvVars(prefix, context, getPreset(presetId).id).toString()
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "Duplicate preset", tint = accent)
                                }
                                var addFocused by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(btnShape)
                                        .then(if (addFocused) Modifier.border(2.dp, accent, btnShape) else Modifier)
                                        .onFocusChanged { addFocused = it.isFocused }
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                        ) {
                                            val defaultEnvVars = EnvVarInfo.KNOWN_BOX64_VARS.values.joinToString(" ") {
                                                "${it.identifier}=${it.possibleValues.first()}"
                                            }
                                            presetId = Box86_64PresetManager
                                                .editPreset(prefix, context, null, "Unnamed", EnvVars(defaultEnvVars))
                                            presetName = getPreset(presetId).name
                                            envVars = Box86_64PresetManager.getEnvVars(prefix, context, getPreset(presetId).id).toString()
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Outlined.AddCircle, contentDescription = "Create preset", tint = accent)
                                }
                                val deleteEnabled = isCustom()
                                var delFocused by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(btnShape)
                                        .then(if (delFocused) Modifier.border(2.dp, accent, btnShape) else Modifier)
                                        .onFocusChanged { delFocused = it.isFocused }
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            enabled = deleteEnabled,
                                        ) {
                                            val idToBeDeleted = presetId
                                            presetId = getPresets().first().id
                                            presetName = getPreset(presetId).name
                                            envVars = Box86_64PresetManager.getEnvVars(prefix, context, getPreset(presetId).id).toString()
                                            Box86_64PresetManager.removePreset(prefix, context, idToBeDeleted)
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete preset", tint = if (deleteEnabled) accent else accent.copy(alpha = 0.38f))
                                }
                            }
                        }
                        Column(modifier = Modifier.verticalScroll(scrollState)) {
                            var infoMsg by rememberSaveable { mutableStateOf("") }
                            MessageDialog(
                                visible = infoMsg.isNotEmpty(),
                                onDismissRequest = { infoMsg = "" },
                                message = infoMsg,
                                useHtmlInMsg = true,
                            )
                            SettingsEnvVars(
                                colors = settingsTileColors(),
                                enabled = isCustom(),
                                envVars = EnvVars(envVars),
                                onEnvVarsChange = {
                                    envVars = it.toString()
                                    Box86_64PresetManager.editPreset(prefix, context, presetId, presetName, it)
                                },
                                knownEnvVars = EnvVarInfo.KNOWN_BOX64_VARS,
                                envVarAction = { varName ->
                                    val accent = LocalGameAccent.current
                                    val infoShape = RoundedCornerShape(8.dp)
                                    var infoFocused by remember { mutableStateOf(false) }
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(infoShape)
                                            .then(if (infoFocused) Modifier.border(2.dp, accent, infoShape) else Modifier)
                                            .onFocusChanged { infoFocused = it.isFocused }
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                            ) {
                                                val resName = varName.replace(prefix.uppercase(), "box86_64_env_var_help_").lowercase()
                                                StringUtils.getString(context, resName)
                                                    ?.let { infoMsg = it }
                                                    ?: Timber.w("Could not find string resource of $resName")
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Outlined.Info, contentDescription = "Variable info", tint = accent)
                                    }
                                },
                            )
                        }
                    }
                }
            },
        )
    }
}
