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
    data object PERMISSION_DENIED : SmsImportState()
    data class DONE(val imported: Int, val cleaned: Int) : SmsImportState()
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var callLogRepository: CallLogRepository
    @Inject lateinit var smsParser: SmsParser

    private val importScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Drives the SMS-import dialog UI (importing / done / permission-denied). */
    private val _smsImportState = mutableStateOf<SmsImportState>(SmsImportState.IDLE)

    private val smsImportPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            _smsImportState.value = SmsImportState.IMPORTING
            importPastDispatchSms(onComplete = { imported, cleaned ->
                _smsImportState.value = SmsImportState.DONE(imported, cleaned)
            })
        } else {
            _smsImportState.value = SmsImportState.PERMISSION_DENIED
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

        // Import past dispatch SMS from inbox into call history (silent; only
        // runs if READ_SMS was already granted — no UI side-effects here).
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
                        val msg = buildString {
                            if (state.imported > 0) {
                                append("Imported ${state.imported} dispatch call")
                                append(if (state.imported == 1) "." else "s.")
                            } else {
                                append("No new dispatch messages found.")
                            }
                            if (state.cleaned > 0) {
                                append("\n\nRemoved ${state.cleaned} incorrectly-imported non-dispatch message")
                                append(if (state.cleaned == 1) "." else "s.")
                            }
                        }
                        AlertDialog(
                            onDismissRequest = { _smsImportState.value = SmsImportState.IDLE },
                            title = { Text("Import Complete") },
                            text = { Text(msg) },
                            confirmButton = {
                                Button(onClick = { _smsImportState.value = SmsImportState.IDLE }) {
                                    Text("OK")
                                }
                            }
                        )
                    }
                    is SmsImportState.PERMISSION_DENIED -> {
                        AlertDialog(
                            onDismissRequest = { _smsImportState.value = SmsImportState.IDLE },
                            title = { Text("SMS Access Needed") },
                            text = {
                                Text(
                                    "Hatzolah needs SMS read permission to import past dispatch " +
                                    "calls from your inbox. You can grant it from Android " +
                                    "Settings \u2192 Apps \u2192 Hatzolah \u2192 Permissions \u2192 SMS."
                                )
                            },
                            confirmButton = {
                                Button(onClick = {
                                    _smsImportState.value = SmsImportState.IDLE
                                    try {
                                        startActivity(Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.fromParts("package", packageName, null)
                                        ))
                                    } catch (_: Throwable) {}
                                }) { Text("Open Settings") }
                            },
                            dismissButton = {
                                TextButton(onClick = { _smsImportState.value = SmsImportState.IDLE }) {
                                    Text("Cancel")
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
     * Requests the standard READ_SMS runtime permission — no need to make
     * Hatzolah the default SMS app. If the permission is already granted,
     * runs the import immediately; otherwise prompts the user.
     */
    fun triggerSmsImport() {
        val hasReadSms = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasReadSms) {
            _smsImportState.value = SmsImportState.IMPORTING
            importPastDispatchSms(onComplete = { imported, cleaned ->
                _smsImportState.value = SmsImportState.DONE(imported, cleaned)
            })
        } else {
            smsImportPermissionLauncher.launch(Manifest.permission.READ_SMS)
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val cn = ComponentName(this, DispatchNotificationListener::class.java)
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: ""
        return flat.contains(cn.flattenToString())
    }

    /**
     * Scans the device SMS inbox for past messages from the dispatch number
     * and imports any that aren't already in the call log database. Also
     * cleans up any previously-imported rows whose raw message does not
     * actually parse as a dispatch (e.g. shift changes, hospital-address
     * updates) — these used to slip through when the parser was lenient.
     *
     * onComplete is called with (importedCount, cleanedCount).
     */
    private fun importPastDispatchSms(onComplete: ((Int, Int) -> Unit)? = null) {
        if (!preferencesManager.isLoggedIn()) { onComplete?.invoke(0, 0); return }
        val dispatchNumber = preferencesManager.getDispatchNumber()
        if (dispatchNumber.isBlank()) { onComplete?.invoke(0, 0); return }

        val memberId = preferencesManager.getLoggedInMemberId()
        val normalizedDispatch = dispatchNumber.replace(Regex("[^0-9]"), "").takeLast(10)

        importScope.launch {
            // Cleanup pass: remove previously-imported rows that don't parse
            // as real dispatches. Only touches undocumented rows so we never
            // delete user-edited call entries.
            var cleaned = 0
            try {
                val undocumented = callLogRepository.getUndocumentedCallLogs()
                for (log in undocumented) {
                    val raw = log.medicalNotes
                    if (raw.isBlank()) continue
                    if (smsParser.parseDispatchMessage(raw, requireCallType = true) == null) {
                        callLogRepository.deleteCallLog(log)
                        cleaned++
                    }
                }
                if (cleaned > 0) {
                    Log.i("Hatzolah", "Removed $cleaned non-dispatch rows from call history")
                }
            } catch (e: Throwable) {
                Log.w("Hatzolah", "Call-log cleanup failed", e)
            }

            // Import pass: requires READ_SMS. If we don't have it, skip the
            // inbox scan but still report any cleanup that ran.
            val hasReadSms = ContextCompat.checkSelfPermission(
                this@MainActivity, Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED

            var imported = 0
            if (hasReadSms) {
                var cursor: Cursor? = null
                try {
                    cursor = contentResolver.query(
                        Telephony.Sms.Inbox.CONTENT_URI,
                        arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
                        null, null,
                        "${Telephony.Sms.DATE} ASC"
                    )
                    if (cursor != null) {
                        while (cursor.moveToNext()) {
                            val address = cursor.getString(0) ?: continue
                            val body = cursor.getString(1) ?: continue
                            val date = cursor.getLong(2)

                            val normalizedSender = address.replace(Regex("[^0-9]"), "").takeLast(10)
                            if (normalizedSender != normalizedDispatch) continue

                            val parsed = smsParser.parseDispatchMessage(body, requireCallType = true) ?: continue

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
                    }
                    if (imported > 0) {
                        Log.i("Hatzolah", "Imported $imported past dispatch SMS into call history")
                    }
                } catch (e: Throwable) {
                    Log.w("Hatzolah", "SMS import failed", e)
                } finally {
                    try { cursor?.close() } catch (_: Throwable) {}
                }
            }

            val finalImported = imported
            val finalCleaned = cleaned
            runOnUiThread { onComplete?.invoke(finalImported, finalCleaned) }
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CALL_PHONE,
            // Needed so the dispatch-SMS listener can match the sender even when
            // the dispatch number is saved as a contact (the SMS notification's
            // title then shows the contact name, not the raw phone number).
            Manifest.permission.READ_CONTACTS
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
