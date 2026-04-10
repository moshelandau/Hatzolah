package com.hatzolah.app.ui.hospital

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.location.LocationServices

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalDirectoryScreen(
    viewModel: HospitalDirectoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var expandedId by remember { mutableStateOf(-1L) }

    // Get user's location for distance sorting
    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            try {
                val client = LocationServices.getFusedLocationProviderClient(context)
                client.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) viewModel.updateLocation(loc.latitude, loc.longitude)
                }
            } catch (_: Throwable) {}
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::onSearchChanged,
            placeholder = { Text("Search hospitals...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                Row {
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearchChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                    IconButton(onClick = { viewModel.toggleSort() }) {
                        Icon(
                            if (uiState.sortByDistance) Icons.Default.NearMe else Icons.Default.SortByAlpha,
                            contentDescription = "Sort",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )

        // Sort indicator
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${uiState.hospitals.size} hospitals",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (uiState.sortByDistance) "• Nearest first" else "• A-Z",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Hospital list
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(uiState.hospitals, key = { it.hospital.id }) { item ->
                HospitalCard(
                    item = item,
                    expanded = expandedId == item.hospital.id,
                    onToggle = {
                        expandedId = if (expandedId == item.hospital.id) -1L else item.hospital.id
                    },
                    onCall = { phone ->
                        try {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                        } catch (_: Throwable) {}
                    },
                    onNavigate = { address ->
                        val encoded = Uri.encode(address.trim())
                        try {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$encoded&mode=d"))
                                    .apply { setPackage("com.google.android.apps.maps") }
                            )
                        } catch (_: Throwable) {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$encoded")))
                            } catch (_: Throwable) {}
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun HospitalCard(
    item: HospitalWithDistance,
    expanded: Boolean,
    onToggle: () -> Unit,
    onCall: (String) -> Unit,
    onNavigate: (String) -> Unit
) {
    val h = item.hospital

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(16.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hospital icon
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.LocalHospital,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = h.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (h.address.isNotBlank()) {
                        Text(
                            text = h.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Distance badge
                item.distanceMiles?.let { dist ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = when {
                            dist < 10 -> Color(0xFF2E7D32).copy(alpha = 0.12f)
                            dist < 30 -> Color(0xFFE65100).copy(alpha = 0.12f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = if (dist < 1) "${String.format("%.1f", dist)} mi"
                            else "${dist.toInt()} mi",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                dist < 10 -> Color(0xFF2E7D32)
                                dist < 30 -> Color(0xFFE65100)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Quick info chips
            if (!expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (h.mainHotline.isNotBlank()) {
                        AssistChip(
                            onClick = { onCall(h.mainHotline) },
                            label = { Text(h.mainHotline, fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Phone, null, Modifier.size(14.dp)) },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                    if (h.erLocation.isNotBlank()) {
                        AssistChip(
                            onClick = onToggle,
                            label = { Text("ER", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Emergency, null, Modifier.size(14.dp)) },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }

            // Expanded details
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                if (h.erLocation.isNotBlank()) {
                    InfoRow(Icons.Default.Emergency, "ER Location", h.erLocation)
                }
                if (h.accessCodes.isNotBlank()) {
                    InfoRow(Icons.Default.Key, "Access Codes", h.accessCodes)
                }
                if (h.kosherRoomLocation.isNotBlank()) {
                    InfoRow(Icons.Default.Room, "Kosher Room", h.kosherRoomLocation)
                }
                if (h.patientAssistanceNotes.isNotBlank()) {
                    InfoRow(Icons.Default.Info, "Patient Assistance", h.patientAssistanceNotes)
                }
                if (h.additionalNotes.isNotBlank()) {
                    InfoRow(Icons.Default.Note, "Notes", h.additionalNotes)
                }

                // Phone numbers
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (h.mainHotline.isNotBlank()) {
                        Button(
                            onClick = { onCall(h.mainHotline) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Phone, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Main", fontSize = 13.sp)
                        }
                    }
                    if (h.obHotline.isNotBlank()) {
                        OutlinedButton(
                            onClick = { onCall(h.obHotline) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Phone, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("OB", fontSize = 13.sp)
                        }
                    }
                }

                if (h.address.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onNavigate(h.address) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Navigation, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Navigate")
                    }
                }

                if (h.departmentHotlines.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Other Lines",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = h.departmentHotlines
                            .replace("{", "").replace("}", "")
                            .replace("\"", "")
                            .replace(",", "\n")
                            .replace(":", ": "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            // Try to extract first phone number
                            val phone = Regex("\\d[\\d-]{6,}").find(h.departmentHotlines)?.value
                            if (phone != null) onCall(phone)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
