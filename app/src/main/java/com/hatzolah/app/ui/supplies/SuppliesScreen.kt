package com.hatzolah.app.ui.supplies

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hatzolah.app.data.database.entity.SupplyRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuppliesScreen(
    viewModel: SuppliesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.US) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add item")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = "Supply Restock",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "${uiState.pendingCount} items pending restock",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TabRow(selectedTabIndex = uiState.tab) {
                Tab(
                    selected = uiState.tab == 0,
                    onClick = { viewModel.onTabChanged(0) },
                    text = { Text("Pending (${uiState.pending.size})") },
                    icon = { Icon(Icons.Default.Inventory, contentDescription = null) }
                )
                Tab(
                    selected = uiState.tab == 1,
                    onClick = { viewModel.onTabChanged(1) },
                    text = { Text("History") },
                    icon = { Icon(Icons.Default.History, contentDescription = null) }
                )
            }

            when (uiState.tab) {
                0 -> PendingList(
                    items = uiState.pending,
                    dateFormat = dateFormat,
                    onRestock = viewModel::markRestocked,
                    onDelete = viewModel::delete,
                    onMarkAllDone = { viewModel.markAllRestocked() }
                )
                1 -> RestockedList(
                    items = uiState.restocked,
                    dateFormat = dateFormat,
                    onUnrestock = viewModel::markPending,
                    onDelete = viewModel::delete
                )
            }
        }
    }

    if (showAddDialog) {
        AddSupplyDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { text ->
                viewModel.addFromMultiLine(text)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun PendingList(
    items: List<SupplyRequest>,
    dateFormat: SimpleDateFormat,
    onRestock: (SupplyRequest) -> Unit,
    onDelete: (SupplyRequest) -> Unit,
    onMarkAllDone: () -> Unit
) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "All caught up!",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "No supplies pending restock",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    Column {
        if (items.size > 1) {
            Button(
                onClick = onMarkAllDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.DoneAll, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mark All Restocked")
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(items, key = { it.id }) { item ->
                SupplyRow(
                    item = item,
                    dateFormat = dateFormat,
                    pending = true,
                    onToggle = { onRestock(item) },
                    onDelete = { onDelete(item) }
                )
            }
        }
    }
}

@Composable
private fun RestockedList(
    items: List<SupplyRequest>,
    dateFormat: SimpleDateFormat,
    onUnrestock: (SupplyRequest) -> Unit,
    onDelete: (SupplyRequest) -> Unit
) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No restock history yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(items, key = { it.id }) { item ->
            SupplyRow(
                item = item,
                dateFormat = dateFormat,
                pending = false,
                onToggle = { onUnrestock(item) },
                onDelete = { onDelete(item) }
            )
        }
    }
}

@Composable
private fun SupplyRow(
    item: SupplyRequest,
    dateFormat: SimpleDateFormat,
    pending: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = !pending,
                onCheckedChange = { onToggle() }
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.quantity > 1) {
                        Text(
                            text = "${item.quantity}×",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = item.itemName,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (pending) null else TextDecoration.LineThrough,
                        color = if (pending)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Added ${dateFormat.format(Date(item.createdAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!pending && item.restockedAt != null) {
                    Text(
                        text = "Restocked ${dateFormat.format(Date(item.restockedAt))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (item.notes.isNotBlank()) {
                    Text(
                        text = item.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AddSupplyDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onAdd(text) },
                enabled = text.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Add Supplies to Restock") },
        text = {
            Column {
                Text(
                    "One item per line. You can include quantity like \"2x gauze\":",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    placeholder = {
                        Text(
                            "2x gauze\n" +
                                    "1 IV kit\n" +
                                    "mask\n" +
                                    "3x saline"
                        )
                    }
                )
            }
        }
    )
}
