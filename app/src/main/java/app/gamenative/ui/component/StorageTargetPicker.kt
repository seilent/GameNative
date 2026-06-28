package app.gamenative.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.gamenative.PrefManager
import app.gamenative.service.storage.StorageManager
import app.gamenative.service.storage.StorageTarget
import app.gamenative.ui.theme.GlassBorder
import app.gamenative.ui.theme.GlassFill
import app.gamenative.ui.theme.GlassFillStrong
import app.gamenative.ui.theme.LocalAccentContainer
import app.gamenative.ui.theme.LocalAccentContainerBright
import app.gamenative.ui.theme.LocalAccentMuted
import app.gamenative.ui.theme.LocalGameAccent
import app.gamenative.ui.theme.PluviaTheme
import app.gamenative.utils.StorageUtils

@Composable
fun StorageTargetTabs(
    selectedTargetId: String,
    onSelectTarget: (StorageTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val accent = LocalGameAccent.current
    val accentContainer = LocalAccentContainer.current
    val accentContainerBright = LocalAccentContainerBright.current
    val accentMuted = LocalAccentMuted.current
    val targets = remember { StorageManager.allTargets(context) }
    val selectedTarget = targets.firstOrNull { it.id == selectedTargetId } ?: targets.first()
    var defaultTargetId by remember { mutableStateOf(StorageManager.defaultInstallTarget(context).id) }
    val free = StorageManager.freeBytes(selectedTarget)
    val total = StorageManager.totalBytes(selectedTarget)
    val isDefault = defaultTargetId == selectedTarget.id

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            targets.forEach { target ->
                val isSelected = target.id == selectedTargetId
                val chipShape = RoundedCornerShape(20.dp)
                var focused by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(chipShape)
                        .then(
                            when {
                                isSelected && focused -> Modifier
                                    .background(accentContainerBright, chipShape)
                                    .border(2.dp, accent, chipShape)
                                isSelected -> Modifier
                                    .background(accentContainer, chipShape)
                                    .border(1.dp, accent.copy(alpha = 0.40f), chipShape)
                                focused -> Modifier
                                    .background(GlassFill, chipShape)
                                    .border(2.dp, accent, chipShape)
                                else -> Modifier
                                    .background(GlassFill, chipShape)
                                    .border(1.dp, GlassBorder, chipShape)
                            }
                        )
                        .alpha(if (target.isMounted) 1f else 0.4f)
                        .onFocusChanged { focused = it.isFocused }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = target.isMounted,
                        ) { onSelectTarget(target) }
                        .padding(horizontal = 16.dp)
                        .semantics {
                            contentDescription = "${target.label}${if (isSelected) ", selected" else ""}"
                            role = Role.Tab
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isSelected || focused) Color.White else PluviaTheme.colors.textMuted,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = target.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected || focused) Color.White else PluviaTheme.colors.textMuted,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        val starShape = RoundedCornerShape(8.dp)
        var starFocused by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(starShape)
                .then(
                    if (starFocused) Modifier.border(2.dp, accent, starShape)
                    else Modifier
                )
                .onFocusChanged { starFocused = it.isFocused }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = selectedTarget.isMounted && !isDefault,
                ) {
                    PrefManager.defaultStorageTargetId = selectedTarget.id
                    defaultTargetId = selectedTarget.id
                }
                .semantics {
                    contentDescription = if (isDefault) "Default storage target" else "Set as default storage target"
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isDefault) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isDefault) accent else PluviaTheme.colors.textMuted,
            )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LinearProgressIndicator(
                progress = { if (total > 0) (total - free).toFloat() / total else 0f },
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = accentMuted,
                trackColor = PluviaTheme.colors.borderDefault,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${StorageUtils.formatBinarySize(total - free)} / ${StorageUtils.formatBinarySize(total)}",
                style = MaterialTheme.typography.labelSmall,
                color = PluviaTheme.colors.textMuted,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun StorageTargetDropdown(
    selectedTarget: StorageTarget,
    onTargetSelected: (StorageTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val targets = remember { StorageManager.allTargets(context) }
    val accent = LocalGameAccent.current

    if (targets.size <= 1) return

    var expanded by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val free = StorageManager.freeBytes(selectedTarget)
    val dropdownShape = RoundedCornerShape(12.dp)

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(dropdownShape)
                .then(
                    if (focused) Modifier.border(1.dp, accent, dropdownShape)
                    else Modifier.border(1.dp, GlassBorder, dropdownShape)
                )
                .background(GlassFill, dropdownShape)
                .onFocusChanged { focused = it.isFocused }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { expanded = true }
                .padding(horizontal = 12.dp)
                .semantics { contentDescription = "Install location: ${selectedTarget.label}" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Storage,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = accent,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${selectedTarget.label} (${StorageUtils.formatBinarySize(free)} free)",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = "Expand storage options",
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
            targets.filter { it.isMounted }.forEach { target ->
                val targetFree = StorageManager.freeBytes(target)
                val isSelected = target.id == selectedTarget.id
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "${target.label} (${StorageUtils.formatBinarySize(targetFree)} free)",
                            color = if (isSelected) accent else Color.White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        onTargetSelected(target)
                        expanded = false
                    },
                )
            }
        }
    }
}
