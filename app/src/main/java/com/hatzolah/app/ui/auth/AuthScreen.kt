package com.hatzolah.app.ui.auth

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    LaunchedEffect(uiState.step) {
        if (uiState.step == AuthStep.AUTHENTICATED) onAuthSuccess()
    }

    // Runtime permission launcher. When the system dialog closes we read the
    // phone number (if granted) and hand it to the view model.
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val detected = if (granted) DevicePhoneUtil.getDevicePhoneNumber(context) else null
        viewModel.onPermissionResult(granted, detected)
    }

    // If permission is already granted when we arrive at NEED_PERMISSION,
    // skip the prompt entirely and move straight into DETECTING.
    LaunchedEffect(uiState.step) {
        if (uiState.step == AuthStep.NEED_PERMISSION &&
            DevicePhoneUtil.hasPermission(context)
        ) {
            viewModel.onDevicePhoneAlreadyAvailable(DevicePhoneUtil.getDevicePhoneNumber(context))
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.primary) { padding ->
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
                    when (uiState.step) {
                        AuthStep.CHECKING -> LoadingContent("Loading…")
                        AuthStep.FIRST_TIME_SETUP -> FirstTimeSetupContent(uiState, viewModel)
                        AuthStep.NEED_PERMISSION -> NeedPermissionContent(
                            onAllow = {
                                permissionLauncher.launch(DevicePhoneUtil.requiredPermission())
                            }
                        )
                        AuthStep.DETECTING -> DetectingContent()
                        AuthStep.CONFIRM_IDENTITY -> ConfirmIdentityContent(uiState, viewModel)
                        AuthStep.SIGN_IN_FAILED -> SignInFailedContent(
                            uiState = uiState,
                            onCallHelp = { phone ->
                                try {
                                    context.startActivity(
                                        Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                            .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                    )
                                } catch (_: Throwable) {}
                            },
                            onRetry = viewModel::retryAutoDetect
                        )
                        AuthStep.AUTHENTICATED -> LoadingContent("Signing you in…")
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
private fun LoadingContent(message: String) {
    CircularProgressIndicator()
    Spacer(modifier = Modifier.height(12.dp))
    Text(message, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun NeedPermissionContent(onAllow: () -> Unit) {
    Icon(
        imageVector = Icons.Default.LockOpen,
        contentDescription = null,
        modifier = Modifier.size(48.dp),
        tint = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Sign In With Your Phone",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = "To sign you in automatically, Hatzolah needs permission to read your phone number from this device. We only use it to match you against the member list — nothing is sent anywhere.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(20.dp))
    Button(onClick = onAllow, modifier = Modifier.fillMaxWidth()) {
        Text("Allow Access")
    }
}

@Composable
private fun SignInFailedContent(
    uiState: AuthUiState,
    onCallHelp: (String) -> Unit,
    onRetry: () -> Unit
) {
    Icon(
        imageVector = Icons.Default.ErrorOutline,
        contentDescription = null,
        modifier = Modifier.size(48.dp),
        tint = MaterialTheme.colorScheme.error
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "We couldn't sign you in",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    if (uiState.failureReason.isNotBlank()) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = uiState.failureReason,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Help contact card — KY-85 Moshe Landau (or whoever is mapped to KY-85).
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Need help? Contact",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Text(
                text = uiState.helpContactUnit,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (uiState.helpContactName.isNotBlank()) {
                Text(
                    text = uiState.helpContactName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (uiState.helpContactPhone.isNotBlank()) {
                Button(
                    onClick = { onCallHelp(uiState.helpContactPhone) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Call ${uiState.helpContactUnit}")
                }
            } else {
                Text(
                    text = "Reach out in person or on WhatsApp",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedButton(
        onClick = onRetry,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Try Again")
    }
}

@Composable
private fun DetectingContent() {
    Icon(
        imageVector = Icons.Default.SimCard,
        contentDescription = null,
        modifier = Modifier.size(48.dp),
        tint = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = "Retrieving your phone number…",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = "Reading your SIM to identify you.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(20.dp))
    CircularProgressIndicator()
}

@Composable
private fun ConfirmIdentityContent(
    uiState: AuthUiState,
    viewModel: AuthViewModel
) {
    val member = uiState.matchedMember

    // Big green check circle so the user sees something succeeded.
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(Color(0xFF2E7D32).copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(44.dp),
            tint = Color(0xFF2E7D32)
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = "We found your number",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(4.dp))

    // The detected phone number, formatted large.
    Text(
        text = formatPhoneNumber(uiState.detectedPhoneNumber ?: uiState.phoneNumber),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )

    Spacer(modifier = Modifier.height(16.dp))

    Divider()

    Spacer(modifier = Modifier.height(16.dp))

    // Matched member card
    if (member != null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Signed in as",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Editable unit number so the user can confirm or correct it.
        OutlinedTextField(
            value = uiState.unitNumber,
            onValueChange = viewModel::onUnitChanged,
            label = { Text("Unit Number") },
            placeholder = { Text("e.g. KY85") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            supportingText = {
                Text(
                    text = if (member.unitNumber.isBlank()) "Enter your unit number to continue"
                    else "Confirm your unit number or edit it below",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        )
    }

    Spacer(modifier = Modifier.height(20.dp))

    Button(
        onClick = viewModel::confirmAndSignIn,
        modifier = Modifier.fillMaxWidth(),
        enabled = !uiState.isLoading && uiState.unitNumber.isNotBlank()
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

    Spacer(modifier = Modifier.height(8.dp))

    TextButton(onClick = viewModel::rejectIdentity) {
        Text("This isn't me")
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
        fontWeight = FontWeight.Bold,
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
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(
        value = uiState.unitNumber,
        onValueChange = viewModel::onUnitChanged,
        label = { Text("Unit Number") },
        placeholder = { Text("e.g. KY85") },
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

/** Render a 10-digit phone number as (XXX) XXX-XXXX for the confirmation screen. */
private fun formatPhoneNumber(raw: String): String {
    val digits = raw.filter { it.isDigit() }.takeLast(10)
    return if (digits.length == 10) {
        "(${digits.substring(0, 3)}) ${digits.substring(3, 6)}-${digits.substring(6)}"
    } else raw
}
