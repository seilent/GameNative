package app.gamenative.ui.component.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.gamenative.R
import app.gamenative.ui.component.GlassSurface
import app.gamenative.ui.component.settings.SettingsListDropdown
import app.gamenative.ui.theme.settingsTileColors
import app.gamenative.ui.theme.settingsTileColorsAlt
import com.alorma.compose.settings.ui.SettingsGroup
import app.gamenative.ui.component.settings.SettingsSwitchWithAction
import com.winlator.container.Container
import kotlin.math.roundToInt

@Composable
fun ControllerTabContent(state: ContainerConfigState, default: Boolean) {
    val config = state.config.value

    GlassSurface(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), shape = RoundedCornerShape(20.dp)) {
    Column {
    SettingsGroup() {
        if (!default) {
            SettingsSwitchWithAction(
                colors = settingsTileColorsAlt(),
                title = { Text(text = stringResource(R.string.use_sdl_api)) },
                state = config.sdlControllerAPI,
                onCheckedChange = { state.config.value = config.copy(sdlControllerAPI = it) },
            )
        }
        SettingsSwitchWithAction(
            colors = settingsTileColorsAlt(),
            title = { Text(text = stringResource(R.string.use_steam_input)) },
            state = config.useSteamInput,
            onCheckedChange = { state.config.value = config.copy(useSteamInput = it) },
        )
        SettingsSwitchWithAction(
            colors = settingsTileColorsAlt(),
            title = { Text(text = stringResource(R.string.enable_xinput_api)) },
            state = config.enableXInput,
            onCheckedChange = { state.config.value = config.copy(enableXInput = it) },
        )
        SettingsSwitchWithAction(
            colors = settingsTileColorsAlt(),
            title = { Text(text = stringResource(R.string.enable_directinput_api)) },
            state = config.enableDInput,
            onCheckedChange = { state.config.value = config.copy(enableDInput = it) },
        )
        SettingsListDropdown(
            colors = settingsTileColors(),
            title = { Text(text = stringResource(R.string.directinput_mapper_type)) },
            value = if (config.dinputMapperType == 1.toByte()) 0 else 1,
            items = listOf("Standard", "XInput Mapper"),
            onItemSelected = { index ->
                state.config.value = config.copy(dinputMapperType = if (index == 0) 1 else 2)
            },
        )
        SettingsSwitchWithAction(
            colors = settingsTileColorsAlt(),
            title = { Text(text = stringResource(R.string.shooter_mode_toggle)) },
            subtitle = { Text(text = stringResource(R.string.shooter_mode_toggle_description)) },
            state = config.shooterMode,
            onCheckedChange = { state.config.value = config.copy(shooterMode = it) },
        )
        SettingsAdjustmentRow(
            title = stringResource(R.string.stick_deadzone_compensation),
            valueText = "${(config.stickAntiDeadzone * 100).roundToInt()}%",
            value = config.stickAntiDeadzone,
            valueRange = 0f..0.95f,
            steps = 18,
            onValueChange = {
                val snapped = (it * 20).roundToInt().coerceIn(0, 19) * 0.05f
                state.config.value = config.copy(stickAntiDeadzone = snapped)
            },
            onDecrease = {
                val current = (config.stickAntiDeadzone * 20).roundToInt()
                val next = (current - 1).coerceIn(0, 19) * 0.05f
                state.config.value = config.copy(stickAntiDeadzone = next)
            },
            onIncrease = {
                val current = (config.stickAntiDeadzone * 20).roundToInt()
                val next = (current + 1).coerceIn(0, 19) * 0.05f
                state.config.value = config.copy(stickAntiDeadzone = next)
            },
            subtitle = stringResource(R.string.stick_deadzone_compensation_description),
        )
        SettingsListDropdown(
            colors = settingsTileColors(),
            title = { Text(text = stringResource(R.string.external_display_input)) },
            subtitle = { Text(text = stringResource(R.string.external_display_input_subtitle)) },
            value = state.externalDisplayModeIndex.value,
            items = state.externalDisplayModes,
            onItemSelected = { index ->
                state.externalDisplayModeIndex.value = index
                state.config.value = config.copy(
                    externalDisplayMode = when (index) {
                        1 -> Container.EXTERNAL_DISPLAY_MODE_TOUCHPAD
                        2 -> Container.EXTERNAL_DISPLAY_MODE_KEYBOARD
                        3 -> Container.EXTERNAL_DISPLAY_MODE_HYBRID
                        else -> Container.EXTERNAL_DISPLAY_MODE_OFF
                    },
                )
            },
        )
        SettingsSwitchWithAction(
            colors = settingsTileColorsAlt(),
            title = { Text(text = stringResource(R.string.external_display_swap)) },
            subtitle = { Text(text = stringResource(R.string.external_display_swap_subtitle)) },
            state = config.externalDisplaySwap,
            onCheckedChange = { state.config.value = config.copy(externalDisplaySwap = it) },
        )
    }
    }
    }
}
