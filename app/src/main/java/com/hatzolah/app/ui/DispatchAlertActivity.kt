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

/**
 * Full-screen dispatch alert that takes over the entire screen,
 * shows on lock screen, and stays until the user dismisses it.
 */
class DispatchAlertActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ADDRESS = "dispatch_address"
        const val EXTRA_CALL_TYPE = "dispatch_call_type"
        const val EXTRA_RAW_MESSAGE = "dispatch_raw_message"
        const val EXTRA_UNITS = "dispatch_units"
        const val EXTRA_AGE = "dispatch_age"

        fun createIntent(context: Context, address: String, callType: String, rawMessage: String, units: String = "", age: String = ""): Intent {
            return Intent(context, DispatchAlertActivity::class.java).apply {
                putExtra(EXTRA_ADDRESS, address)
                putExtra(EXTRA_CALL_TYPE, callType)
                putExtra(EXTRA_RAW_MESSAGE, rawMessage)
                putExtra(EXTRA_UNITS, units)
                putExtra(EXTRA_AGE, age)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
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
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val address = intent.getStringExtra(EXTRA_ADDRESS) ?: ""
        val callType = intent.getStringExtra(EXTRA_CALL_TYPE) ?: ""
        val rawMessage = intent.getStringExtra(EXTRA_RAW_MESSAGE) ?: ""
        val units = intent.getStringExtra(EXTRA_UNITS) ?: ""
        val age = intent.getStringExtra(EXTRA_AGE) ?: ""

        // Parse unit number from address
        val unitNumber = extractUnitNumber(address)

        setContent {
            HatzolahTheme {
                DispatchAlertScreen(
                    address = address,
                    callType = callType,
                    unitNumber = unitNumber,
                    rawMessage = rawMessage,
                    units = units,
                    age = age,
                    onNavigate = { navigateToAddress(address) },
                    onDismiss = { finish() }
                )
            }
        }
    }

    private fun navigateToAddress(address: String) {
        val uri = "google.navigation:q=${address.trim().replace(" ", "+")}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
            setPackage("com.google.android.apps.maps")
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            // Fallback to browser
            val webUri = "https://www.google.com/maps/search/${Uri.encode(address)}"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUri)))
        }
    }

    private fun extractUnitNumber(address: String): String {
        // Match common unit/apt patterns: "Apt 3", "Unit 5B", "#12", "Apt. 4"
        val patterns = listOf(
            Regex("(?i)(apt\\.?|unit|suite|ste\\.?|#)\\s*([A-Za-z0-9-]+)"),
            Regex("(?i)\\bfl(?:oor)?\\s*(\\d+)"),
        )
        for (pattern in patterns) {
            val match = pattern.find(address)
            if (match != null) return match.value.trim()
        }
        return ""
    }
}

@Composable
fun DispatchAlertScreen(
    address: String,
    callType: String,
    unitNumber: String,
    rawMessage: String,
    units: String = "",
    age: String = "",
    onNavigate: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFD32F2F))
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

                    // Unit number - extra large
                    if (unitNumber.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1565C0)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = unitNumber.uppercase(),
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 28.dp, vertical = 14.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Units assigned
            if (units.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "UNITS: $units",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        textAlign = TextAlign.Center
                    )
                }
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
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "NAVIGATE",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F)
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
