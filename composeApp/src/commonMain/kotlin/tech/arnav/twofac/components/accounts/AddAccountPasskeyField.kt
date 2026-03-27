package tech.arnav.twofac.components.accounts

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import twofac.composeapp.generated.resources.*
import tech.arnav.twofac.theme.TwoFacTheme

@Composable
fun AddAccountPasskeyField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(Res.string.label_passkey)) },
        placeholder = { Text(stringResource(Res.string.label_passkey_placeholder)) },
        modifier = modifier,
    )
}

@Preview
@Composable
private fun AddAccountPasskeyFieldPreview() {
    TwoFacTheme {
        AddAccountPasskeyField(
            value = "hunter2",
            onValueChange = {},
        )
    }
}
