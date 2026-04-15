package com.hatzolah.app.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hatzolah.app.data.database.entity.Hospital

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    viewModel: AdminViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var editingMember by remember { mutableStateOf<com.hatzolah.app.data.database.entity.Member?>(null) }
    var showAddHospitalDialog by remember { mutableStateOf(false) }
    var editingHospital by remember { mutableStateOf<com.hatzolah.app.data.database.entity.Hospital?>(null) }
    var showAddUrgentCareDialog by remember { mutableStateOf(false) }
    var editingUrgentCare by remember { mutableStateOf<com.hatzolah.app.data.database.entity.Hospital?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab row for Settings / Members / Hospitals / Urgent Care
        ScrollableTabRow(selectedTabIndex = uiState.activeTab, edgePadding = 0.dp) {
            Tab(
                selected = uiState.activeTab == 0,
                onClick = { viewModel.onTabChanged(0) },
                text = { Text("Settings") },
                icon = { Icon(Icons.Default.Settings, contentDescription = null) }
            )
            Tab(
                selected = uiState.activeTab == 1,
                onClick = { viewModel.onTabChanged(1) },
                text = { Text("Members") },
                icon = { Icon(Icons.Default.People, contentDescription = null) }
            )
            Tab(
                selected = uiState.activeTab == 2,
                onClick = { viewModel.onTabChanged(2) },
                text = { Text("Hospitals") },
                icon = { Icon(Icons.Default.LocalHospital, contentDescription = null) }
            )
            Tab(
                selected = uiState.activeTab == 3,
                onClick = { viewModel.onTabChanged(3) },
                text = { Text("Urgent Care") },
                icon = { Icon(Icons.Default.MedicalServices, contentDescription = null) }
            )
        }

        when (uiState.activeTab) {
            0 -> SettingsTab(uiState, viewModel)
            1 -> MembersTab(
                uiState, viewModel,
                onAddClick = { showAddMemberDialog = true },
                onEditClick = { member -> editingMember = member }
            )
            2 -> HospitalsTab(
                uiState, viewModel,
                onAddClick = { showAddHospitalDialog = true },
                onEditClick = { hospital -> editingHospital = hospital }
            )
            3 -> UrgentCareTab(
                uiState, viewModel,
                onAddClick = { showAddUrgentCareDialog = true },
                onEditClick = { uc -> editingUrgentCare = uc }
            )
        }
    }

    if (showAddMemberDialog) {
        AddMemberDialog(
            onDismiss = { showAddMemberDialog = false },
            onAdd = { name, phone, whatsapp, email, unitNumber ->
                viewModel.addMember(name, phone, whatsapp, email, unitNumber)
                showAddMemberDialog = false
            }
        )
    }

    editingMember?.let { member ->
        EditMemberDialog(
            member = member,
            onDismiss = { editingMember = null },
            onSave = { updated ->
                viewModel.updateMember(updated)
                editingMember = null
            }
        )
    }

    if (showAddHospitalDialog) {
        AddHospitalDialog(
            onDismiss = { showAddHospitalDialog = false },
            onAdd = { name, address, erLocation, accessCodes, kosherRoom, patientAssistance,
                      lat, lng, mainHotline, obHotline, deptHotlines, commSystem, beds ->
                viewModel.addHospital(
                    name, address, erLocation, accessCodes, kosherRoom, patientAssistance,
                    lat, lng, mainHotline, obHotline, deptHotlines, commSystem, beds
                )
                showAddHospitalDialog = false
            }
        )
    }

    editingHospital?.let { hospital ->
        EditHospitalDialog(
            hospital = hospital,
            onDismiss = { editingHospital = null },
            onSave = { updated ->
                viewModel.updateHospital(updated)
                editingHospital = null
            }
        )
    }

    if (showAddUrgentCareDialog) {
        AddUrgentCareDialog(
            onDismiss = { showAddUrgentCareDialog = false },
            onAdd = { name, address, phone, contactPerson, notes ->
                viewModel.addUrgentCare(name, address, phone, contactPerson, notes)
                showAddUrgentCareDialog = false
            }
        )
    }

    editingUrgentCare?.let { uc ->
        EditUrgentCareDialog(
            urgentCare = uc,
            onDismiss = { editingUrgentCare = null },
            onSave = { updated ->
                viewModel.updateUrgentCare(updated)
                editingUrgentCare = null
            }
        )
    }
}

@Composable
private fun SettingsTab(uiState: AdminUiState, viewModel: AdminViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("App Configuration", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = uiState.dispatchNumber,
                    onValueChange = viewModel::onDispatchNumberChanged,
                    label = { Text("Dispatch SMS Number") },
                    placeholder = { Text("e.g. +1234567890") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = uiState.rmaHotline,
                    onValueChange = viewModel::onRmaHotlineChanged,
                    label = { Text("RMA Hotline Number") },
                    placeholder = { Text("e.g. +1234567890") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = viewModel::saveSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Settings")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Test Dispatch Simulator
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Test Dispatch Simulator", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Sends a fake dispatch alert to test the full flow (alert, sound, navigation, call log).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.sendTestDispatch() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send Test Dispatch Now")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.startAutoTest(60_000L) },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.autoTestRunning
                    ) {
                        Text("Auto 1min", style = MaterialTheme.typography.labelMedium)
                    }
                    Button(
                        onClick = { viewModel.startAutoTest(300_000L) },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.autoTestRunning
                    ) {
                        Text("Auto 5min", style = MaterialTheme.typography.labelMedium)
                    }
                    Button(
                        onClick = { viewModel.stopAutoTest() },
                        modifier = Modifier.weight(1f),
                        enabled = uiState.autoTestRunning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Stop", style = MaterialTheme.typography.labelMedium)
                    }
                }

                if (uiState.autoTestRunning) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Auto-test running. Next dispatch in ${uiState.autoTestIntervalLabel}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Quick Stats", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Registered Members: ${uiState.members.size}")
                Text("Hospitals in Directory: ${uiState.hospitals.size}")
                Text("Urgent Care Facilities: ${uiState.urgentCares.size}")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Import Call History from SMS
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Import Call History from SMS", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Temporarily set Hatzolah as default SMS app to read past dispatch messages, then switch back.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                val activity = LocalContext.current as? com.hatzolah.app.ui.MainActivity
                Button(
                    onClick = { activity?.triggerSmsImport() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = activity != null
                ) {
                    Icon(Icons.Default.Sms, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Dispatch SMS")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Crash log viewer
        val context = LocalContext.current
        var showCrashLog by remember { mutableStateOf(false) }
        var crashLogText by remember { mutableStateOf("") }

        OutlinedButton(
            onClick = {
                crashLogText = com.hatzolah.app.util.CrashLogger.readLog(context)
                showCrashLog = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.BugReport, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("View Crash Log")
        }

        if (showCrashLog) {
            AlertDialog(
                onDismissRequest = { showCrashLog = false },
                title = { Text("Crash Log") },
                text = {
                    Column {
                        Text(
                            text = crashLogText,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                                .verticalScroll(rememberScrollState())
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCrashLog = false }) { Text("Close") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        com.hatzolah.app.util.CrashLogger.clearLog(context)
                        crashLogText = "Log cleared."
                    }) { Text("Clear Log") }
                }
            )
        }
    }
}

@Composable
private fun MembersTab(
    uiState: AdminUiState,
    viewModel: AdminViewModel,
    onAddClick: () -> Unit,
    onEditClick: (com.hatzolah.app.data.database.entity.Member) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Members (${uiState.members.size})", style = MaterialTheme.typography.titleMedium)
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Member")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.members) { member ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = member.name, style = MaterialTheme.typography.titleSmall)
                                if (member.unitNumber.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = member.unitNumber,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(text = member.phoneNumber, style = MaterialTheme.typography.bodySmall)
                            if (member.whatsappContact.isNotBlank()) {
                                Text(text = "WhatsApp: ${member.whatsappContact}", style = MaterialTheme.typography.bodySmall)
                            }
                            if (member.email.isNotBlank()) {
                                Text(text = member.email, style = MaterialTheme.typography.bodySmall)
                            }
                            if (member.isVerified) {
                                Text(
                                    text = "Verified",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        IconButton(onClick = { onEditClick(member) }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { viewModel.deleteMember(member) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HospitalsTab(
    uiState: AdminUiState,
    viewModel: AdminViewModel,
    onAddClick: () -> Unit,
    onEditClick: (Hospital) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Hospitals (${uiState.hospitals.size})", style = MaterialTheme.typography.titleMedium)
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Hospital")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.hospitals) { hospital ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = hospital.name, style = MaterialTheme.typography.titleSmall)
                            Text(text = hospital.address, style = MaterialTheme.typography.bodySmall)
                            if (hospital.erLocation.isNotBlank()) {
                                Text(text = "ER: ${hospital.erLocation}", style = MaterialTheme.typography.bodySmall)
                            }
                            if (hospital.mainHotline.isNotBlank()) {
                                Text(text = "Hotline: ${hospital.mainHotline}", style = MaterialTheme.typography.bodySmall)
                            }
                            if (hospital.communicationSystem.isNotBlank()) {
                                Text(text = "Comm: ${hospital.communicationSystem}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        IconButton(onClick = { onEditClick(hospital) }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { viewModel.deleteHospital(hospital) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddMemberDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var unitNumber by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Member") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                OutlinedTextField(
                    value = unitNumber,
                    onValueChange = { unitNumber = it },
                    label = { Text("Unit Number (e.g. KY85)") },
                    placeholder = { Text("KY##") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = whatsapp,
                    onValueChange = { whatsapp = it },
                    label = { Text("WhatsApp (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(name, phone, whatsapp, email, unitNumber) },
                enabled = name.isNotBlank() && phone.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditMemberDialog(
    member: com.hatzolah.app.data.database.entity.Member,
    onDismiss: () -> Unit,
    onSave: (com.hatzolah.app.data.database.entity.Member) -> Unit
) {
    var name by remember { mutableStateOf(member.name) }
    var phone by remember { mutableStateOf(member.phoneNumber) }
    var whatsapp by remember { mutableStateOf(member.whatsappContact) }
    var email by remember { mutableStateOf(member.email) }
    var unitNumber by remember { mutableStateOf(member.unitNumber) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Member") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                OutlinedTextField(
                    value = unitNumber,
                    onValueChange = { unitNumber = it },
                    label = { Text("Unit Number (e.g. KY85)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = whatsapp,
                    onValueChange = { whatsapp = it },
                    label = { Text("WhatsApp (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        member.copy(
                            name = name,
                            phoneNumber = phone,
                            whatsappContact = whatsapp,
                            email = email,
                            unitNumber = unitNumber.trim().uppercase()
                        )
                    )
                },
                enabled = name.isNotBlank() && phone.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddHospitalDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String, String, String, Double, Double, String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var erLocation by remember { mutableStateOf("") }
    var accessCodes by remember { mutableStateOf("") }
    var kosherRoom by remember { mutableStateOf("") }
    var patientAssistance by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var mainHotline by remember { mutableStateOf("") }
    var obHotline by remember { mutableStateOf("") }
    var deptHotlines by remember { mutableStateOf("") }
    var commSystem by remember { mutableStateOf("") }
    var beds by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Hospital") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Hospital Name *") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address *") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = erLocation,
                    onValueChange = { erLocation = it },
                    label = { Text("ER Location") },
                    placeholder = { Text("e.g. Building B, Ground Floor") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = accessCodes,
                    onValueChange = { accessCodes = it },
                    label = { Text("Access Codes") },
                    placeholder = { Text("e.g. Door: 1234, Elevator: 5678") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = kosherRoom,
                    onValueChange = { kosherRoom = it },
                    label = { Text("Kosher Room Location") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = patientAssistance,
                    onValueChange = { patientAssistance = it },
                    label = { Text("Patient Assistance (beds/contacts)") },
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latitude,
                        onValueChange = { latitude = it },
                        label = { Text("Latitude") },
                        placeholder = { Text("e.g. 41.1234") },
                        supportingText = { Text("-90 to 90") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = longitude,
                        onValueChange = { longitude = it },
                        label = { Text("Longitude") },
                        placeholder = { Text("e.g. -74.0060") },
                        supportingText = { Text("-180 to 180") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
                OutlinedTextField(
                    value = mainHotline,
                    onValueChange = { mainHotline = it },
                    label = { Text("Main Notification Hotline") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                OutlinedTextField(
                    value = obHotline,
                    onValueChange = { obHotline = it },
                    label = { Text("OB Hotline") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                OutlinedTextField(
                    value = deptHotlines,
                    onValueChange = { deptHotlines = it },
                    label = { Text("Other Dept Hotlines") },
                    placeholder = { Text("e.g. ICU: 555-1234, Peds: 555-5678") }
                )
                OutlinedTextField(
                    value = commSystem,
                    onValueChange = { commSystem = it },
                    label = { Text("Communication System") },
                    placeholder = { Text("e.g. Tiger Connect, Phone") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = beds,
                    onValueChange = { beds = it },
                    label = { Text("Bikur Cholim Contact") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedLat = latitude.toDoubleOrNull() ?: 0.0
                    val parsedLng = longitude.toDoubleOrNull() ?: 0.0
                    // Clamp to valid range; out-of-range values reset to 0.0
                    val validLat = if (parsedLat in -90.0..90.0) parsedLat else 0.0
                    val validLng = if (parsedLng in -180.0..180.0) parsedLng else 0.0
                    onAdd(
                        name, address, erLocation, accessCodes, kosherRoom, patientAssistance,
                        validLat, validLng,
                        mainHotline, obHotline, deptHotlines, commSystem, beds
                    )
                },
                enabled = name.isNotBlank() && address.isNotBlank()
            ) {
                Text("Add Hospital")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditHospitalDialog(
    hospital: Hospital,
    onDismiss: () -> Unit,
    onSave: (Hospital) -> Unit
) {
    var name by remember { mutableStateOf(hospital.name) }
    var address by remember { mutableStateOf(hospital.address) }
    var erLocation by remember { mutableStateOf(hospital.erLocation) }
    var accessCodes by remember { mutableStateOf(hospital.accessCodes) }
    var kosherRoom by remember { mutableStateOf(hospital.kosherRoomLocation) }
    var patientAssistance by remember { mutableStateOf(hospital.patientAssistanceNotes) }
    var latitude by remember { mutableStateOf(if (hospital.latitude != 0.0) hospital.latitude.toString() else "") }
    var longitude by remember { mutableStateOf(if (hospital.longitude != 0.0) hospital.longitude.toString() else "") }
    var mainHotline by remember { mutableStateOf(hospital.mainHotline) }
    var obHotline by remember { mutableStateOf(hospital.obHotline) }
    var deptHotlines by remember { mutableStateOf(hospital.departmentHotlines) }
    var commSystem by remember { mutableStateOf(hospital.communicationSystem) }
    var beds by remember { mutableStateOf(hospital.bedAvailability) }
    var additionalNotes by remember { mutableStateOf(hospital.additionalNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Hospital") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Hospital Name *") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address *") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = erLocation,
                    onValueChange = { erLocation = it },
                    label = { Text("ER Location") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = accessCodes,
                    onValueChange = { accessCodes = it },
                    label = { Text("Access Codes") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = kosherRoom,
                    onValueChange = { kosherRoom = it },
                    label = { Text("Kosher Room Location") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = patientAssistance,
                    onValueChange = { patientAssistance = it },
                    label = { Text("Patient Assistance") },
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latitude,
                        onValueChange = { latitude = it },
                        label = { Text("Latitude") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = longitude,
                        onValueChange = { longitude = it },
                        label = { Text("Longitude") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
                OutlinedTextField(
                    value = mainHotline,
                    onValueChange = { mainHotline = it },
                    label = { Text("Main Hotline") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                OutlinedTextField(
                    value = obHotline,
                    onValueChange = { obHotline = it },
                    label = { Text("OB Hotline") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                OutlinedTextField(
                    value = deptHotlines,
                    onValueChange = { deptHotlines = it },
                    label = { Text("Other Dept Hotlines") }
                )
                OutlinedTextField(
                    value = commSystem,
                    onValueChange = { commSystem = it },
                    label = { Text("Communication System") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = beds,
                    onValueChange = { beds = it },
                    label = { Text("Bikur Cholim Contact") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = additionalNotes,
                    onValueChange = { additionalNotes = it },
                    label = { Text("Additional Notes") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedLat = latitude.toDoubleOrNull() ?: 0.0
                    val parsedLng = longitude.toDoubleOrNull() ?: 0.0
                    val validLat = if (parsedLat in -90.0..90.0) parsedLat else 0.0
                    val validLng = if (parsedLng in -180.0..180.0) parsedLng else 0.0
                    onSave(
                        hospital.copy(
                            name = name,
                            address = address,
                            erLocation = erLocation,
                            accessCodes = accessCodes,
                            kosherRoomLocation = kosherRoom,
                            patientAssistanceNotes = patientAssistance,
                            latitude = validLat,
                            longitude = validLng,
                            mainHotline = mainHotline,
                            obHotline = obHotline,
                            departmentHotlines = deptHotlines,
                            communicationSystem = commSystem,
                            bedAvailability = beds,
                            additionalNotes = additionalNotes
                        )
                    )
                },
                enabled = name.isNotBlank() && address.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun UrgentCareTab(
    uiState: AdminUiState,
    viewModel: AdminViewModel,
    onAddClick: () -> Unit,
    onEditClick: (Hospital) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Urgent Care (${uiState.urgentCares.size})", style = MaterialTheme.typography.titleMedium)
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.tertiary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Urgent Care")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.urgentCares) { uc ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = uc.name, style = MaterialTheme.typography.titleSmall)
                            if (uc.address.isNotBlank()) {
                                Text(text = uc.address, style = MaterialTheme.typography.bodySmall)
                            }
                            if (uc.mainHotline.isNotBlank()) {
                                Text(text = "Phone: ${uc.mainHotline}", style = MaterialTheme.typography.bodySmall)
                            }
                            if (uc.additionalNotes.isNotBlank()) {
                                Text(text = uc.additionalNotes, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        IconButton(onClick = { onEditClick(uc) }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { viewModel.deleteUrgentCare(uc) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddUrgentCareDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var contactPerson by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Urgent Care Facility") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Facility Name *") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                OutlinedTextField(
                    value = contactPerson,
                    onValueChange = { contactPerson = it },
                    label = { Text("Contact Person") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(name, address, phone, contactPerson, notes) },
                enabled = name.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditUrgentCareDialog(
    urgentCare: Hospital,
    onDismiss: () -> Unit,
    onSave: (Hospital) -> Unit
) {
    var name by remember { mutableStateOf(urgentCare.name) }
    var address by remember { mutableStateOf(urgentCare.address) }
    var phone by remember { mutableStateOf(urgentCare.mainHotline) }
    var contactPerson by remember { mutableStateOf(
        urgentCare.departmentHotlines
            .replace("{\"Contact\":\"", "")
            .replace("\"}", "")
            .let { if (it == urgentCare.departmentHotlines) "" else it }
    ) }
    var notes by remember { mutableStateOf(urgentCare.additionalNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Urgent Care Facility") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Facility Name *") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                OutlinedTextField(
                    value = contactPerson,
                    onValueChange = { contactPerson = it },
                    label = { Text("Contact Person") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        urgentCare.copy(
                            name = name,
                            address = address,
                            mainHotline = phone,
                            departmentHotlines = if (contactPerson.isNotBlank()) "{\"Contact\":\"$contactPerson\"}" else "",
                            additionalNotes = notes
                        )
                    )
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
