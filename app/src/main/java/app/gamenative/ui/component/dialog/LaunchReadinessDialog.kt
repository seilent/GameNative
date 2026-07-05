package app.gamenative.ui.component.dialog

import android.content.res.Configuration
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import app.gamenative.ui.theme.LocalGameAccent
import app.gamenative.ui.theme.PluviaTheme

@Composable
fun LaunchReadinessDialog(
    isLoading: Boolean,
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    GlassAlertDialog(
        visible = true,
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isLoading,
            dismissOnClickOutside = !isLoading,
        ),
        title = { Text(text = title) },
        text = {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp), color = LocalGameAccent.current)
            } else {
                Text(text = message)
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isLoading) { Text(text = confirmText, color = LocalGameAccent.current) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text(text = dismissText) }
        },
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_LaunchReadinessDialog() {
    PluviaTheme {
        LaunchReadinessDialog(
            isLoading = false,
            title = "Title",
            message = "Message body goes here.",
            confirmText = "Confirm",
            dismissText = "Not now",
            onConfirm = {},
            onDismiss = {},
        )
    }
}
