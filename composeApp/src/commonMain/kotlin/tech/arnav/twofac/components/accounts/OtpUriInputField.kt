package tech.arnav.twofac.components.accounts

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.tooling.preview.Preview
import tech.arnav.twofac.theme.TwoFacTheme

@Composable
fun OtpUriInputField(
    value: String,
    onValueChange: (String) -> Unit,
    isFocused: Boolean,
    onFocusChanged: (Boolean) -> Unit,
    onPasteShortcut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("2FA URI") },
        placeholder = { Text("otpauth://totp/...") },
        modifier = modifier
            .onFocusChanged { onFocusChanged(it.isFocused) }
            .onPreviewKeyEvent { keyEvent ->
                val isPasteShortcut =
                    keyEvent.type == KeyEventType.KeyDown &&
                        keyEvent.key == Key.V &&
                        (keyEvent.isMetaPressed || keyEvent.isCtrlPressed)
                if (isFocused && isPasteShortcut) {
                    onPasteShortcut()
                }
                false
            },
    )
}

@Preview
@Composable
private fun OtpUriInputFieldPreview() {
    TwoFacTheme {
        OtpUriInputField(
            value = "otpauth://totp/GitHub:user@example.com",
            onValueChange = {},
            isFocused = false,
            onFocusChanged = {},
            onPasteShortcut = {},
        )
    }
}
