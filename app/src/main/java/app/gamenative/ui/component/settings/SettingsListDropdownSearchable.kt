package app.gamenative.ui.component.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alorma.compose.settings.ui.base.internal.LocalSettingsGroupEnabled
import com.alorma.compose.settings.ui.base.internal.SettingsTileColors
import com.alorma.compose.settings.ui.base.internal.SettingsTileDefaults
import com.alorma.compose.settings.ui.base.internal.SettingsTileScaffold
import androidx.compose.material3.TextField
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.res.stringResource
import app.gamenative.R
import app.gamenative.ui.theme.GlassFillStrong
import app.gamenative.ui.theme.GlassBorder
import app.gamenative.ui.theme.LocalGameAccent
import app.gamenative.ui.theme.PluviaTheme
import androidx.compose.ui.graphics.Color

@Composable
fun SettingsListDropdownSearchable(
    modifier: Modifier = Modifier,
    enabled: Boolean = LocalSettingsGroupEnabled.current,
    value: Int,
    items: List<String>,
    itemMuted: List<Boolean>? = null,
    fallbackDisplay: String = "",
    onItemSelected: (Int) -> Unit,
    title: @Composable () -> Unit,
    subtitle: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    colors: SettingsTileColors = SettingsTileDefaults.colors(),
    tonalElevation: Dp = ListItemDefaults.Elevation,
    shadowElevation: Dp = ListItemDefaults.Elevation,
    action: @Composable (() -> Unit)? = null,
    searchable: Boolean = true,
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }

    // 🔥 NEW: search state
    var query by remember { mutableStateOf("") }

    // 🔥 NEW: filtered list
    val filteredItems = remember(query, items) {
        items.mapIndexed { index, text -> index to text }
            .filter { it.second.contains(query, ignoreCase = true) }
    }

    val selectedText =
        if (value >= 0 && value < items.size) items[value] else fallbackDisplay

    val accent = LocalGameAccent.current
    val focusManager = LocalFocusManager.current

    SettingsTileScaffold(
        modifier = Modifier
            .clickable(
                enabled = enabled,
                onClick = {
                    isDropdownExpanded = true
                    query = "" // reset search
                },
            )
            .then(modifier),
        enabled = enabled,
        title = title,
        subtitle = {
            if (subtitle != null) {
                Column {
                    ProvideTextStyle(value = LocalTextStyle.current.merge(TextStyle(fontStyle = FontStyle.Italic))) {
                        subtitle()
                    }
                    Text(
                        text = selectedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(fontWeight = FontWeight.Bold),
                    )
                }
            } else {
                Text(
                    text = selectedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(fontWeight = FontWeight.Bold),
                )
            }
        },
        icon = icon,
        colors = colors,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
    ) {
        DropdownMenu(
            expanded = isDropdownExpanded,
            onDismissRequest = { isDropdownExpanded = false },
            containerColor = GlassFillStrong,
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = BorderStroke(1.dp, GlassBorder),
        ) {

            if (searchable) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .onPreviewKeyEvent { e ->
                            if (e.type == KeyEventType.KeyDown) {
                                when (e.key) {
                                    Key.DirectionDown -> { focusManager.moveFocus(FocusDirection.Down); true }
                                    Key.DirectionUp -> { focusManager.moveFocus(FocusDirection.Up); true }
                                    else -> false
                                }
                            } else false
                        },
                    placeholder = { Text(text = stringResource(R.string.settings_interface_settingslistdropdownsearchable_searchlabel)) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = accent,
                        cursorColor = accent,
                        focusedLabelColor = accent,
                    ),
                )
            }

            filteredItems.forEach { (index, text) ->
                val isMuted = itemMuted?.getOrNull(index) == true
                val isSelected = index == value
                val textColor = when {
                    isSelected -> accent
                    isMuted -> PluviaTheme.colors.textMuted.copy(alpha = 0.6f)
                    else -> Color.White
                }
                DropdownMenuItem(
                    enabled = enabled,
                    text = { Text(text = text, color = textColor) },
                    onClick = {
                        onItemSelected(index)
                        isDropdownExpanded = false
                    },
                )
            }
        }
        Row {
            Icon(
                modifier = Modifier.align(Alignment.CenterVertically),
                imageVector = if (isDropdownExpanded) {
                    Icons.Filled.ArrowDropUp
                } else {
                    Icons.Filled.ArrowDropDown
                },
                contentDescription = "Dropdown arrow",
                tint = PluviaTheme.colors.textMuted,
            )
            if (action != null) {
                Spacer(modifier.width(16.dp))
                action()
            }
        }
    }
}
