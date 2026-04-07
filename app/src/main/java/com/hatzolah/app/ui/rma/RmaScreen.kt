package com.hatzolah.app.ui.rma

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun RmaScreen(
    onDocumentCall: (Long) -> Unit,
    viewModel: RmaViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "RMA Hotline",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Quick-dial button
        Button(
            onClick = {
                if (uiState.rmaHotline.isNotBlank()) {
                    val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${uiState.rmaHotline}"))
                    context.startActivity(intent)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            ),
            enabled = uiState.rmaHotline.isNotBlank()
        ) {
            Icon(
                Icons.Default.Call,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Call RMA Hotline",
                style = MaterialTheme.typography.titleLarge
            )
        }

        if (uiState.rmaHotline.isBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "RMA hotline not configured. Set it in Settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Outcome selection
        Text(
            text = "Log Call Outcome",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        val outcomes = listOf("Approved", "Denied", "Transferred")
        outcomes.forEach { outcome ->
            OutlinedButton(
                onClick = { viewModel.logOutcome(outcome) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = if (uiState.selectedOutcome == outcome) {
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                } else {
                    ButtonDefaults.outlinedButtonColors()
                }
            ) {
                Text(outcome)
            }
        }

        if (uiState.outcomeSaved) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Outcome logged: ${uiState.selectedOutcome}")

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = {
                        uiState.activeCallLogId?.let { onDocumentCall(it) }
                    }) {
                        Text("Document this call")
                    }
                }
            }
        }
    }
}
