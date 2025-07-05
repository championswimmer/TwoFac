package tech.arnav.twofac.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AccountDetailScreen(
    accountId: String,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = "Account Details",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Account: $accountId",
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = "OTP: 123456",
            style = MaterialTheme.typography.headlineSmall
        )

        Button(onClick = onNavigateBack) {
            Text("Back")
        }
    }
}