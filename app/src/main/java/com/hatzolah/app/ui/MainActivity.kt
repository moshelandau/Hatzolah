package com.hatzolah.app.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.provider.Telephony
import android.util.Log
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
import com.hatzolah.app.data.database.entity.CallLog
import com.hatzolah.app.data.repository.CallLogRepository
import com.hatzolah.app.service.DispatchNotificationListener
import com.hatzolah.app.service.SmsParser
import com.hatzolah.app.ui.auth.AuthScreen
import com.hatzolah.app.ui.navigation.AppNavigation
import com.hatzolah.app.ui.theme.HatzolahTheme
import com.hatzolah.app.util.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SmsImportState {
    data object IDLE : SmsImportState()
    data object IMPORTING : SmsImportState()
    data class DONE(val count: Int) : SmsImportState()
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var callLogRepository: CallLogRepository
    @Inject lateinit var smsParser: SmsParser

    private val importScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Tracks whether we need to show the "switch back" prompt after import */
    private val _smsImportState = mutableStateOf<SmsImportState>(SmsImportState.IDLE)

    private val defaultSmsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // After returning from the default SMS picker, check if we're now default
        if (isDefaultSmsApp()) {
            // We're default — run import then prompt to switch back
            _smsImportState.value = SmsImportState.IMPORTING
            importPastDispatchSms(onComplete = { count ->
                _smsImportState.value = SmsImportState.DONE(count)
            })
        } else {
            _smsImportState.value = SmsImportState.IDLE
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // Log denied permissions for troubleshooting
        val denied = results.filter { !it.value }.keys
        if (denied.isNotEmpty()) {
            android.util.Log.w("Hatzolah", "Denied permissions: $denied")
        }
    }

    private var hasShownDispatch = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestRequiredPermissions()

        // Show active dispatch only once on fresh app launch
        if (!hasShownDispatch) {
            showActiveDispatchIfAny()
            hasShownDispatch = true
        }

        // Import past dispatch SMS from inbox into call history
        importPastDispatchSms()

        setContent {
            HatzolahTheme {
                var isLoggedIn by remember { mutableStateOf(preferencesManager.isLoggedIn()) }
                var showNotifAccessPrompt by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    showNotifAccessPrompt = !isNotificationListenerEnabled()
                }

                val smsImportState by _smsImportState

                // SMS import dialogs
                when (val state = smsImportState) {
                    is SmsImportState.IMPORTING -> {
                        AlertDialog(
                            onDismissRequest = {},
                            title = { Text("Importing Calls") },
                            text = { Text("Scanning SMS inbox for past dispatch messages...") },
                            confirmButton = {}
                        )
                    }
                    is SmsImportState.DONE -> {
                        AlertDialog(
                            onDismissRequest = { _smsImportState.value = SmsImportState.IDLE },
                            title = { Text("Import Complete") },
                            text = {
                                Text(
                                    if (state.count > 0) "Imported ${state.count} dispatch calls.\n\nSwitch back to your regular SMS app now."
                                    else "No new dispatch messages found.\n\nSwitch back to your regular SMS app now."
                                )
                            },
                            confirmButton = {
                                Button(onClick = { requestRestoreDefaultSms() }) {
                                    Text("Switch SMS App Back")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { _smsImportState.value = SmsImportState.IDLE }) {
                                    Text("Later")
                                }
                            }
                        )
                    }
                    else -> {}
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

    private fun showActiveDispatchIfAny() {
        val active = try { preferencesManager.getActiveDispatch() } catch (_: Throwable) { null }
        if (active != null) {
            try {
                val intent = DispatchAlertActivity.createIntent(
                    this,
                    active.address,
                    active.callType,
                    active.rawMessage,
                    active.units,
                    active.age,
                    active.room,
                    active.cad
                )
                startActivity(intent)
            } catch (_: Throwable) {}
        }
    }

    /**
     * Called from AdminScreen to start the SMS import flow.
     * If we already have READ_SMS, imports directly.
     * Otherwise, prompts user to set as default SMS app first.
     */
    fun triggerSmsImport() {
        val hasReadSms = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasReadSms) {
            _smsImportState.value = SmsImportState.IMPORTING
            importPastDispatchSms(onComplete = { count ->
                _smsImportState.value = SmsImportState.DONE(count)
            })
        } else {
            requestBecomeDefaultSms()
        }
    }

    private fun isDefaultSmsApp(): Boolean {
        return try {
            Telephony.Sms.getDefaultSmsPackage(this) == packageName
        } catch (_: Throwable) { false }
    }

    private fun requestBecomeDefaultSms() {
        val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
            putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
        }
        defaultSmsLauncher.launch(intent)
    }

    private fun requestRestoreDefaultSms() {
        // Open the default SMS picker so the user can switch back
        val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
            // Don't pre-fill — let the user pick their real SMS app
            putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, "com.google.android.apps.messaging")
        }
        try { startActivity(intent) } catch (_: Throwable) {}
        _smsImportState.value = SmsImportState.IDLE
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val cn = ComponentName(this, DispatchNotificationListener::class.java)
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: ""
        return flat.contains(cn.flattenToString())
    }

    /**
     * Scans the device SMS inbox for past messages from the dispatch number
     * and imports any that aren't already in the call log database.
     */
    private fun importPastDispatchSms(onComplete: ((Int) -> Unit)? = null) {
        if (!preferencesManager.isLoggedIn()) { onComplete?.invoke(0); return }
        val dispatchNumber = preferencesManager.getDispatchNumber()
        if (dispatchNumber.isBlank()) { onComplete?.invoke(0); return }

        val hasReadSms = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasReadSms) { onComplete?.invoke(0); return }

        val memberId = preferencesManager.getLoggedInMemberId()
        val normalizedDispatch = dispatchNumber.replace(Regex("[^0-9]"), "").takeLast(10)

        importScope.launch {
            var imported = 0
            var cursor: Cursor? = null
            try {
                cursor = contentResolver.query(
                    Telephony.Sms.Inbox.CONTENT_URI,
                    arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
                    null, null,
                    "${Telephony.Sms.DATE} ASC"
                )
                if (cursor == null) { onComplete?.invoke(0); return@launch }

                while (cursor.moveToNext()) {
                    val address = cursor.getString(0) ?: continue
                    val body = cursor.getString(1) ?: continue
                    val date = cursor.getLong(2)

                    val normalizedSender = address.replace(Regex("[^0-9]"), "").takeLast(10)
                    if (normalizedSender != normalizedDispatch) continue

                    val parsed = smsParser.parseDispatchMessage(body) ?: continue

                    // Skip if already imported (dedup by raw message text)
                    if (callLogRepository.existsByRawMessage(body)) continue

                    callLogRepository.addCallLog(
                        CallLog(
                            memberId = memberId,
                            date = date,
                            dispatchAddress = parsed.address,
                            outcome = parsed.callType,
                            medicalNotes = body,
                            isDocumented = false
                        )
                    )
                    imported++
                }
                if (imported > 0) {
                    Log.i("Hatzolah", "Imported $imported past dispatch SMS into call history")
                }
            } catch (e: Throwable) {
                Log.w("Hatzolah", "SMS import failed", e)
            } finally {
                try { cursor?.close() } catch (_: Throwable) {}
            }
            val finalCount = imported
            runOnUiThread { onComplete?.invoke(finalCount) }
        }
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
