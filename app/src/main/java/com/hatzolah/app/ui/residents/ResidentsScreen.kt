package com.hatzolah.app.ui.residents

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hatzolah.app.data.database.entity.Resident

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResidentsScreen(
    viewModel: ResidentsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showImportDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "KJ Phone Book",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "${uiState.totalCount} residents",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { showImportDialog = true }) {
                Icon(Icons.Default.Upload, contentDescription = "Import")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search bar - accepts Hebrew and English
        OutlinedTextField(
            value = uiState.query,
            onValueChange = viewModel::onQueryChanged,
            label = { Text("Search (English or עברית)") },
            placeholder = { Text("Name, phone, address...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (uiState.query.isNotBlank()) {
                    IconButton(onClick = { viewModel.onQueryChanged("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        uiState.importMessage?.let { msg ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = viewModel::clearImportMessage) {
                        Text("Dismiss")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // List
        if (uiState.residents.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.ContactPhone,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (uiState.totalCount == 0)
                            "No phone book data yet.\nTap the upload icon to import."
                        else
                            "No results for \"${uiState.query}\"",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(uiState.residents, key = { it.id }) { resident ->
                    ResidentRow(
                        resident = resident,
                        onClick = { viewModel.selectResident(resident) },
                        onCall = { callNumber(context, resident.phoneNumber) }
                    )
                }
                if (uiState.residents.size >= 200) {
                    item {
                        Text(
                            text = "Showing first 200 results. Refine your search.",
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Detail sheet
    uiState.selected?.let { details ->
        ResidentDetailDialog(
            details = details,
            onDismiss = viewModel::closeDetails,
            onCall = { phone -> callNumber(context, phone) },
            onNavigateToAddress = { addr -> navigateToAddress(context, addr) },
            onSelectRelated = { viewModel.selectResident(it) }
        )
    }

    if (showImportDialog) {
        ImportDialog(
            onDismiss = { showImportDialog = false },
            onImport = { csv ->
                viewModel.importCsv(csv)
                showImportDialog = false
            },
            onDeleteAll = {
                viewModel.deleteAll()
                showImportDialog = false
            }
        )
    }
}

@Composable
private fun ResidentRow(
    resident: Resident,
    onClick: () -> Unit,
    onCall: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // English name
                val enName = listOf(resident.firstNameEn, resident.lastNameEn)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                if (enName.isNotBlank()) {
                    Text(
                        text = enName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Hebrew name
                val heName = listOf(resident.firstNameHe, resident.lastNameHe)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                if (heName.isNotBlank()) {
                    Text(
                        text = heName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 16.sp
                    )
                }

                // Nickname
                val nick = listOf(resident.nicknameEn, resident.nicknameHe)
                    .filter { it.isNotBlank() }
                    .joinToString(" • ")
                if (nick.isNotBlank()) {
                    Text(
                        text = nick,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (resident.address.isNotBlank()) {
                    Text(
                        text = resident.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (resident.phoneNumber.isNotBlank()) {
                    Text(
                        text = resident.phoneNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (resident.phoneNumber.isNotBlank()) {
                IconButton(onClick = onCall) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = "Call",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun ResidentDetailDialog(
    details: ResidentDetails,
    onDismiss: () -> Unit,
    onCall: (String) -> Unit,
    onNavigateToAddress: (String) -> Unit,
    onSelectRelated: (Resident) -> Unit
) {
    val r = details.resident
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = {
            Column {
                val enName = listOf(r.firstNameEn, r.lastNameEn).filter { it.isNotBlank() }.joinToString(" ")
                val heName = listOf(r.firstNameHe, r.lastNameHe).filter { it.isNotBlank() }.joinToString(" ")
                if (enName.isNotBlank()) Text(enName, fontWeight = FontWeight.Bold)
                if (heName.isNotBlank()) Text(heName, fontSize = 18.sp)
                if (enName.isBlank() && heName.isBlank()) Text("Unknown Resident", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                if (r.phoneNumber.isNotBlank()) {
                    DetailRow(
                        label = "Phone",
                        value = r.phoneNumber,
                        onClick = { onCall(r.phoneNumber) },
                        icon = Icons.Default.Phone
                    )
                }
                if (r.address.isNotBlank()) {
                    DetailRow(
                        label = "Address",
                        value = r.address,
                        onClick = { onNavigateToAddress(r.address) },
                        icon = Icons.Default.Place
                    )
                }
                val nick = listOf(r.nicknameEn, r.nicknameHe).filter { it.isNotBlank() }.joinToString(" • ")
                if (nick.isNotBlank()) {
                    DetailRow(label = "Known as", value = nick)
                }
                if (r.notes.isNotBlank()) {
                    DetailRow(label = "Notes", value = r.notes)
                }

                // Family
                val hasFamily = details.father != null || details.fatherInLaw != null ||
                        details.sons.isNotEmpty() || details.sonsInLaw.isNotEmpty()
                if (hasFamily) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Family",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    details.father?.let {
                        FamilyRow("Father", it, onSelectRelated)
                    }
                    details.fatherInLaw?.let {
                        FamilyRow("Father-in-law (שווער)", it, onSelectRelated)
                    }
                    if (details.sons.isNotEmpty()) {
                        Text(
                            "Sons:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        details.sons.forEach { FamilyRow("", it, onSelectRelated) }
                    }
                    if (details.sonsInLaw.isNotEmpty()) {
                        Text(
                            "Sons-in-law (איידעם):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        details.sonsInLaw.forEach { FamilyRow("", it, onSelectRelated) }
                    }
                }
            }
        }
    )
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun FamilyRow(label: String, resident: Resident, onClick: (Resident) -> Unit) {
    val name = listOf(resident.firstNameEn, resident.lastNameEn).filter { it.isNotBlank() }.joinToString(" ")
    val heName = listOf(resident.firstNameHe, resident.lastNameHe).filter { it.isNotBlank() }.joinToString(" ")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable { onClick(resident) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            if (label.isNotBlank()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (name.isNotBlank()) Text(name, style = MaterialTheme.typography.bodyMedium)
            if (heName.isNotBlank()) Text(heName, style = MaterialTheme.typography.bodySmall)
            if (resident.phoneNumber.isNotBlank()) {
                Text(
                    resident.phoneNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ImportDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit,
    onDeleteAll: () -> Unit
) {
    var csv by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onImport(csv) },
                enabled = csv.isNotBlank()
            ) { Text("Import") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Import Phone Book") },
        text = {
            Column {
                Text(
                    "Paste CSV data (one resident per line, pipe-separated):",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "phone|address|firstEn|lastEn|firstHe|lastHe|nickEn|nickHe|notes",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = csv,
                    onValueChange = { csv = it },
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    placeholder = { Text("783-7252|99 Forest Rd #201|...") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onDeleteAll,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete All Residents")
                }
            }
        }
    )
}

private fun callNumber(context: android.content.Context, phone: String) {
    if (phone.isBlank()) return
    try {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
    } catch (_: Throwable) {}
}

private fun navigateToAddress(context: android.content.Context, address: String) {
    if (address.isBlank()) return
    try {
        val uri = "geo:0,0?q=${Uri.encode(address)}"
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
    } catch (_: Throwable) {
        try {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(address)}")
                )
            )
        } catch (_: Throwable) {}
    }
}
