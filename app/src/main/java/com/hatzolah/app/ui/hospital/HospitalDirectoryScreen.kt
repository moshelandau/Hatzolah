package com.hatzolah.app.ui.hospital

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalDirectoryScreen(
    viewModel: HospitalDirectoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::onSearchChanged,
            label = { Text("Search hospitals...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            singleLine = true
        )

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.hospitals) { hospital ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = hospital.name,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Clickable address opens Google Maps
                        Text(
                            text = hospital.address,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                val uri = "geo:${hospital.latitude},${hospital.longitude}?q=${Uri.encode(hospital.address)}"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
                            }
                        )

                        if (hospital.erLocation.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            DetailRow("ER Location", hospital.erLocation)
                        }
                        if (hospital.accessCodes.isNotBlank()) {
                            DetailRow("Access Codes", hospital.accessCodes)
                        }
                        if (hospital.kosherRoomLocation.isNotBlank()) {
                            DetailRow("Kosher Room", hospital.kosherRoomLocation)
                        }
                        if (hospital.bedAvailability.isNotBlank()) {
                            DetailRow("Beds", hospital.bedAvailability)
                        }
                        if (hospital.mainHotline.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row {
                                Text("Main Hotline: ", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = hospital.mainHotline,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${hospital.mainHotline}"))
                                        context.startActivity(intent)
                                    }
                                )
                            }
                        }
                        if (hospital.obHotline.isNotBlank()) {
                            Row {
                                Text("OB Hotline: ", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = hospital.obHotline,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${hospital.obHotline}"))
                                        context.startActivity(intent)
                                    }
                                )
                            }
                        }
                        if (hospital.communicationSystem.isNotBlank()) {
                            DetailRow("Comm System", hospital.communicationSystem)
                        }
                        if (hospital.patientAssistanceNotes.isNotBlank()) {
                            DetailRow("Patient Assistance", hospital.patientAssistanceNotes)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.bodySmall)
    }
}
