package com.hatzolah.app.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hatzolah.app.ui.theme.HatzolahTheme
import com.hatzolah.app.util.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Full-screen dispatch alert that takes over the entire screen,
 * shows on lock screen, and stays until the user dismisses it.
 * Persists across unfold/unlock/restart via PreferencesManager.
 */
@AndroidEntryPoint
class DispatchAlertActivity : ComponentActivity() {

    @Inject lateinit var preferencesManager: PreferencesManager

    companion object {
        const val EXTRA_ADDRESS = "dispatch_address"
        const val EXTRA_CALL_TYPE = "dispatch_call_type"
        const val EXTRA_RAW_MESSAGE = "dispatch_raw_message"
        const val EXTRA_UNITS = "dispatch_units"
        const val EXTRA_AGE = "dispatch_age"
        const val EXTRA_ROOM = "dispatch_room"

        // context is non-null by Kotlin type system
        fun createIntent(context: Context, address: String, callType: String, rawMessage: String, units: String = "", age: String = "", room: String = ""): Intent {
            return Intent(context, DispatchAlertActivity::class.java).apply {
                putExtra(EXTRA_ADDRESS, address)
                putExtra(EXTRA_CALL_TYPE, callType)
                putExtra(EXTRA_RAW_MESSAGE, rawMessage)
                putExtra(EXTRA_UNITS, units)
                putExtra(EXTRA_AGE, age)
                putExtra(EXTRA_ROOM, room)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show on lock screen and wake device
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            km.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        // Note: OS may override KEEP_SCREEN_ON on critically low battery
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (intent == null) { finish(); return }

        val address = intent.getStringExtra(EXTRA_ADDRESS) ?: ""
        val callType = intent.getStringExtra(EXTRA_CALL_TYPE) ?: ""
        val rawMessage = intent.getStringExtra(EXTRA_RAW_MESSAGE) ?: ""
        val units = intent.getStringExtra(EXTRA_UNITS) ?: ""
        val age = intent.getStringExtra(EXTRA_AGE) ?: ""
        val room = intent.getStringExtra(EXTRA_ROOM) ?: ""

        // Extract unit number from address (e.g. #011 from "3 Hamaspik Way #011")
        val unitNumber = extractUnitNumber(address)

        setContent {
            HatzolahTheme {
                DispatchAlertScreen(
                    address = address,
                    callType = callType,
                    unitNumber = unitNumber,
                    room = room,
                    rawMessage = rawMessage,
                    units = units,
                    age = age,
                    onNavigate = { navigateToAddress(address) },
                    onDismiss = {
                        // Clear persisted dispatch so it doesn't re-show on next launch
                        try { preferencesManager.clearActiveDispatch() } catch (_: Throwable) {}
                        // Also cancel the notification
                        try {
                            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                            nm.cancel(1001)
                        } catch (_: Throwable) {}
                        finish()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Re-create with new data
        recreate()
    }

    private fun navigateToAddress(address: String) {
        if (address.isBlank()) return
        // Strip unit/apt/room numbers - Google Maps can't route to them
        var clean = address.trim()
        clean = clean.replace(Regex("\\s*#\\d+[A-Za-z]?"), "")
        clean = clean.replace(Regex("(?i)\\s*(apt|unit|suite|ste|rm|room)\\.?\\s*[A-Za-z0-9-]+"), "")
        clean = clean.replace(Regex(",\\s*,"), ",").trim().trimEnd(',', ' ')
        val encoded = Uri.encode(clean)

        // google.navigation scheme launches turn-by-turn navigation in Google Maps
        val mapsIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("google.navigation:q=$encoded&mode=d")
        ).apply { setPackage("com.google.android.apps.maps") }

        // geo: scheme opens the pin on the map (used as fallback)
        val geoIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("geo:0,0?q=$encoded")
        )

        // Web fallback - Google Maps directions URL
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$encoded&travelmode=driving")
        )

        val intent = when {
            mapsIntent.resolveActivity(packageManager) != null -> mapsIntent
            geoIntent.resolveActivity(packageManager) != null -> geoIntent
            else -> webIntent
        }
        try {
            startActivity(intent)
        } catch (_: Throwable) {
            try { startActivity(webIntent) } catch (_: Throwable) {}
        }
    }

    private fun extractUnitNumber(address: String): String {
        // Match common unit/apt patterns: "Apt 3", "Unit 5B", "#12", "#011", "Room X"
        // Order matters: check Room and # before Floor to avoid false matches
        val patterns = listOf(
            Regex("(?i)(apt\\.?|unit|suite|ste\\.?|room|rm\\.?)\\s*([A-Za-z0-9-]+)"),
            Regex("#\\s*([A-Za-z0-9-]+)"),
            Regex("(?i)\\bfl(?:oor)?\\s*(\\d+)"),
        )
        for (pattern in patterns) {
            val match = pattern.find(address)
            if (match != null) return match.value.trim()
        }
        return ""
    }
}

private fun getSeverity(callType: String): String {
    val upper = callType.uppercase()
    val critical = listOf("CARDIAC", "ARREST", "UNCONSCIOUS", "UNRESPONSIVE", "NOT BREATHING",
        "CHOKING", "SHOOTING", "STABBING", "STROKE", "SEIZURE", "ANAPHYL", "CODE", "CPR", "DOA", "MCI")
    val moderate = listOf("DIFFICULTY BREATHING", "CHEST PAIN", "BLEED", "HEMORRHAG", "TRAUMA",
        "FALL", "FRACTURE", "VEHICLE", "MVA", "ACCIDENT", "OVERDOSE", "OB ", "LABOR", "DELIVERY",
        "DIABETIC", "ALLERGIC", "BURN", "LACERATION", "HEAD INJURY")
    if (critical.any { upper.contains(it) }) return "CRITICAL"
    if (moderate.any { upper.contains(it) }) return "MODERATE"
    return "MINOR"
}

@Composable
fun DispatchAlertScreen(
    address: String,
    callType: String,
    unitNumber: String,
    room: String = "",
    rawMessage: String,
    units: String = "",
    age: String = "",
    onNavigate: () -> Unit,
    onDismiss: () -> Unit
) {
    val severity = getSeverity(callType)
    val bgColor = when (severity) {
        "CRITICAL" -> Color(0xFFD32F2F) // Red
        "MODERATE" -> Color(0xFFE65100) // Deep orange
        else -> Color(0xFF1565C0) // Blue for minor
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dismiss button top-right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // DISPATCH header
            Text(
                text = "DISPATCH",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Call type / nature - BIG yellow badge
            if (callType.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEB3B)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = callType.uppercase(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Age info
            if (age.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Patient: $age",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ADDRESS - big and prominent
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = address,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        lineHeight = 36.sp
                    )

                    // Unit + Room - shown big so responder can see at a glance
                    if (unitNumber.isNotBlank() || room.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (unitNumber.isNotBlank()) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1565C0)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = "UNIT",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            text = unitNumber.uppercase(),
                                            fontSize = 42.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                            if (unitNumber.isNotBlank() && room.isNotBlank()) {
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            if (room.isNotBlank()) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF6A1B9A)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = "ROOM",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            text = room,
                                            fontSize = 42.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Units assigned - small reference
            if (units.isNotBlank()) {
                Text(
                    text = "Units: $units",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // NAVIGATE button - big
            Button(
                onClick = onNavigate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    Icons.Default.Navigation,
                    contentDescription = null,
                    tint = bgColor,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "NAVIGATE",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = bgColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dismiss button
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "DISMISS",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
