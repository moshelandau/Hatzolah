package com.hatzolah.app.ui.callhistory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CallHistoryScreen(
    onDocumentCall: (Long) -> Unit,
    viewModel: CallHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Your Call History",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Only your own past calls are shown",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiState.callLogs) { call ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = dateFormat.format(Date(call.date)),
                                style = MaterialTheme.typography.titleSmall
                            )
                            if (call.rmaApprovalStatus.isNotBlank()) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text(call.rmaApprovalStatus) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = when (call.rmaApprovalStatus.lowercase()) {
                                            "approved" -> MaterialTheme.colorScheme.primaryContainer
                                            "denied" -> MaterialTheme.colorScheme.errorContainer
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                                )
                            }
                        }

                        if (call.dispatchAddress.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row {
                                Icon(
                                    Icons.Default.LocationOn, null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = call.dispatchAddress, style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            if (call.milesTraveled > 0) {
                                Text(
                                    text = "${String.format("%.1f", call.milesTraveled)} mi",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (call.callDurationMinutes > 0) {
                                Text(
                                    text = "${call.callDurationMinutes} min",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        if (!call.isDocumented) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { onDocumentCall(call.id) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Document Call")
                            }
                        }
                    }
                }
            }
        }
    }
}
