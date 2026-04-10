package com.hatzolah.app.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.hatzolah.app.service.DispatchNotificationListener
import com.hatzolah.app.ui.auth.AuthScreen
import com.hatzolah.app.ui.navigation.AppNavigation
import com.hatzolah.app.ui.theme.HatzolahTheme
import com.hatzolah.app.util.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var preferencesManager: PreferencesManager

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // Log denied permissions for troubleshooting
        val denied = results.filter { !it.value }.keys
        if (denied.isNotEmpty()) {
            android.util.Log.w("Hatzolah", "Denied permissions: $denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestRequiredPermissions()

        setContent {
            HatzolahTheme {
                var isLoggedIn by remember { mutableStateOf(preferencesManager.isLoggedIn()) }
                var showNotifAccessPrompt by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    showNotifAccessPrompt = !isNotificationListenerEnabled()
                }

                if (showNotifAccessPrompt && isLoggedIn) {
                    NotificationAccessPrompt(
                        onEnable = {
                            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            showNotifAccessPrompt = false
                        },
                        onSkip = { showNotifAccessPrompt = false }
                    )
                } else if (isLoggedIn) {
                    AppNavigation()
                } else {
                    AuthScreen(
                        onAuthSuccess = { isLoggedIn = true }
                    )
                }
            }
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val cn = ComponentName(this, DispatchNotificationListener::class.java)
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: ""
        return flat.contains(cn.flattenToString())
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CALL_PHONE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }

        // Check full-screen intent permission for Android 14+
        if (Build.VERSION.SDK_INT >= 34) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (!nm.canUseFullScreenIntent()) {
                startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT))
            }
        }
    }
}

@Composable
fun NotificationAccessPrompt(onEnable: () -> Unit, onSkip: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Enable Dispatch Monitoring",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "To receive dispatch alerts from incoming SMS, Hatzolah needs Notification Access. This lets the app read SMS notifications without needing restricted SMS permissions.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "On the next screen, find \"Hatzolah\" and toggle it ON.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onEnable,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enable Notification Access")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onSkip) {
            Text("Skip for now")
        }
    }
}
