package com.hatzolah.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) onAuthSuccess()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Hatzolah Login") })
        }
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
                text = "Hatzolah Members App",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.isFirstTimeSetup) {
                // First-time setup — create admin account
                FirstTimeSetupContent(uiState, viewModel)
            } else if (!uiState.codeSent) {
                // Normal login — phone number entry
                PhoneEntryContent(uiState, viewModel)
            } else {
                // Verification code entry
                VerificationCodeContent(uiState, viewModel)
            }

            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
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
private fun PhoneEntryContent(uiState: AuthUiState, viewModel: AuthViewModel) {
    Text(
        text = "Enter your registered phone number to receive a verification code",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(32.dp))

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
        onClick = viewModel::sendVerificationCode,
        modifier = Modifier.fillMaxWidth(),
        enabled = uiState.phoneNumber.length >= 10 && !uiState.isLoading
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text("Send Verification Code")
        }
    }
}

@Composable
private fun VerificationCodeContent(uiState: AuthUiState, viewModel: AuthViewModel) {
    Text(
        text = "Code sent to ${uiState.phoneNumber}",
        style = MaterialTheme.typography.bodyMedium
    )

    // Show the code on screen for testing
    uiState.displayedCode?.let { code ->
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Your verification code:",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = code,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "(Shown here for testing — will be SMS only in production)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = uiState.verificationCode,
        onValueChange = viewModel::onCodeChanged,
        label = { Text("Verification Code") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = viewModel::verifyCode,
        modifier = Modifier.fillMaxWidth(),
        enabled = uiState.verificationCode.length == 6 && !uiState.isLoading
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text("Verify")
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    TextButton(onClick = viewModel::resetToPhoneEntry) {
        Text("Use a different number")
    }
}
