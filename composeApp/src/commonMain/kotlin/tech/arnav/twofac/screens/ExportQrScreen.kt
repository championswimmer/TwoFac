package tech.arnav.twofac.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import tech.arnav.twofac.viewmodels.AccountsViewModel
import twofac.composeapp.generated.resources.Res
import twofac.composeapp.generated.resources.account_detail_not_found
import twofac.composeapp.generated.resources.action_back
import twofac.composeapp.generated.resources.error_prefix
import twofac.composeapp.generated.resources.export_qr_hide
import twofac.composeapp.generated.resources.export_qr_reveal
import twofac.composeapp.generated.resources.export_qr_scan_instruction
import twofac.composeapp.generated.resources.export_qr_title
import twofac.composeapp.generated.resources.export_qr_vault_locked
import twofac.composeapp.generated.resources.export_qr_warning_message
import twofac.composeapp.generated.resources.export_qr_warning_title

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportQrScreen(
    accountId: String,
    onNavigateBack: () -> Unit,
    viewModel: AccountsViewModel = koinInject(),
) {
    val accounts by viewModel.accounts.collectAsState()
    val error by viewModel.error.collectAsState()

    val account = accounts.find { it.accountID == accountId }
    val isLibUnlocked = viewModel.twoFacLibUnlocked

    var otpAuthUri by remember { mutableStateOf<String?>(null) }
    var isRevealed by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(accountId, isRevealed, isLibUnlocked) {
        if (isRevealed && isLibUnlocked && otpAuthUri == null) {
            val uri = viewModel.getDecryptedUriForAccount(accountId)
            if (uri == null) {
                loadError = "Unable to load account URI."
                isRevealed = false
            } else {
                otpAuthUri = uri
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.export_qr_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(Res.string.action_back),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (account == null) {
                Text(
                    text = stringResource(Res.string.account_detail_not_found),
                    style = MaterialTheme.typography.bodyLarge,
                )
                return@Column
            }

            Text(
                text = account.accountLabel,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                        )
                        Text(
                            text = stringResource(Res.string.export_qr_warning_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    Text(
                        text = stringResource(Res.string.export_qr_warning_message),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            if (!isLibUnlocked) {
                Text(
                    text = stringResource(Res.string.export_qr_vault_locked),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }

            if (isRevealed && otpAuthUri != null) {
                val uri = otpAuthUri
                if (uri != null) {
                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        val painter = rememberQrCodePainter(data = uri)
                        Image(
                            painter = painter,
                            contentDescription = stringResource(Res.string.export_qr_title),
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Text(
                        text = stringResource(Res.string.export_qr_scan_instruction),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    Button(
                        onClick = {
                            isRevealed = false
                            otpAuthUri = null
                        },
                    ) {
                        Text(stringResource(Res.string.export_qr_hide))
                    }
                }
            } else {
                Button(
                    onClick = {
                        loadError = null
                        isRevealed = true
                    },
                    enabled = isLibUnlocked,
                ) {
                    Text(stringResource(Res.string.export_qr_reveal))
                }
            }

            (loadError ?: error)?.let { errorMessage ->
                Text(
                    text = stringResource(Res.string.error_prefix, errorMessage),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
    }
}
