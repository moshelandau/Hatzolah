package com.hatzolah.app.ui.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hatzolah.app.util.DevicePhoneUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) onAuthSuccess()
    }

    // Permission launcher to request READ_PHONE_NUMBERS at runtime
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val detected = DevicePhoneUtil.getDevicePhoneNumber(context)
            viewModel.onDevicePhoneDetected(detected)
        } else {
            // User denied - fall back to manual entry
            viewModel.onDevicePhoneDetected(null)
        }
    }

    // On first launch (not first-time setup, not already authenticated, not already in manual mode),
    // try to auto-detect the device's phone number.
    LaunchedEffect(uiState.isFirstTimeSetup) {
        if (!uiState.isFirstTimeSetup && !uiState.isAuthenticated && !uiState.manualEntryMode && uiState.detectedPhoneNumber == null) {
            if (DevicePhoneUtil.hasPermission(context)) {
                val detected = DevicePhoneUtil.getDevicePhoneNumber(context)
                viewModel.onDevicePhoneDetected(detected)
            } else {
                permissionLauncher.launch(DevicePhoneUtil.requiredPermission())
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.primary
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Hatzolah",
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                text = "Members App",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    if (uiState.isFirstTimeSetup) {
                        FirstTimeSetupContent(uiState, viewModel)
                    } else if (uiState.manualEntryMode) {
                        ManualPhoneEntryContent(uiState, viewModel)
                    } else {
                        AutoDetectContent(uiState)
                    }

                    uiState.error?.let { error ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FirstTimeSetupContent(uiState: AuthUiState, viewModel: AuthViewModel) {
    Icon(
        imageVector = Icons.Default.AdminPanelSettings,
        contentDescription = null,
        modifier = Modifier.size(48.dp),
        tint = MaterialTheme.colorScheme.primary
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "First-Time Setup",
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = "No members registered yet. Create your admin account to get started.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(24.dp))

    OutlinedTextField(
        value = uiState.name,
        onValueChange = viewModel::onNameChanged,
        label = { Text("Your Name") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = uiState.phoneNumber,
        onValueChange = viewModel::onPhoneChanged,
        label = { Text("Phone Number") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(20.dp))

    Button(
        onClick = viewModel::setupAdmin,
        modifier = Modifier.fillMaxWidth(),
        enabled = uiState.name.isNotBlank() && uiState.phoneNumber.length >= 10 && !uiState.isLoading
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text("Create Admin Account & Enter App")
        }
    }
}

@Composable
private fun AutoDetectContent(uiState: AuthUiState) {
    Icon(
        imageVector = Icons.Default.PhoneAndroid,
        contentDescription = null,
        modifier = Modifier.size(48.dp),
        tint = MaterialTheme.colorScheme.primary
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Detecting your phone...",
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "The app is reading your phone number from your device settings to sign you in automatically.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(20.dp))

    if (uiState.isLoading) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ManualPhoneEntryContent(uiState: AuthUiState, viewModel: AuthViewModel) {
    Text(
        text = "Enter Your Phone Number",
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "We couldn't read your phone number automatically. Please enter it manually to sign in.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(24.dp))

    OutlinedTextField(
        value = uiState.phoneNumber,
        onValueChange = viewModel::onPhoneChanged,
        label = { Text("Phone Number") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = viewModel::loginWithManualPhone,
        modifier = Modifier.fillMaxWidth(),
        enabled = uiState.phoneNumber.length >= 10 && !uiState.isLoading
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text("Sign In")
        }
    }
}
