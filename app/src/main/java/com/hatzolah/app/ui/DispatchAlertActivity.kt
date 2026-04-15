package com.hatzolah.app.ui

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.hatzolah.app.data.database.entity.Member
import com.hatzolah.app.data.repository.FirebaseResponderRepository
import com.hatzolah.app.data.repository.MemberRepository
import com.hatzolah.app.service.UnitClassifier
import com.hatzolah.app.ui.theme.HatzolahTheme
import com.hatzolah.app.util.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Full-screen dispatch alert that takes over the entire screen,
 * shows on lock screen, and stays until the user dismisses it.
 * Persists across unfold/unlock/restart via PreferencesManager.
 */
@AndroidEntryPoint
class DispatchAlertActivity : ComponentActivity() {

    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var memberRepository: MemberRepository
    @Inject lateinit var firebaseResponderRepository: FirebaseResponderRepository

    companion object {
        const val EXTRA_ADDRESS = "dispatch_address"
        const val EXTRA_CALL_TYPE = "dispatch_call_type"
        const val EXTRA_RAW_MESSAGE = "dispatch_raw_message"
        const val EXTRA_UNITS = "dispatch_units"
        const val EXTRA_AGE = "dispatch_age"
        const val EXTRA_ROOM = "dispatch_room"
        const val EXTRA_CAD = "dispatch_cad"

        fun createIntent(
            context: Context,
            address: String,
            callType: String,
            rawMessage: String,
            units: String = "",
            age: String = "",
            room: String = "",
            cad: String = ""
        ): Intent {
            return Intent(context, DispatchAlertActivity::class.java).apply {
                putExtra(EXTRA_ADDRESS, address)
                putExtra(EXTRA_CALL_TYPE, callType)
                putExtra(EXTRA_RAW_MESSAGE, rawMessage)
                putExtra(EXTRA_UNITS, units)
                putExtra(EXTRA_AGE, age)
                putExtra(EXTRA_ROOM, room)
                putExtra(EXTRA_CAD, cad)
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
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (intent == null) { finish(); return }

        val address = intent.getStringExtra(EXTRA_ADDRESS) ?: ""
        val callType = intent.getStringExtra(EXTRA_CALL_TYPE) ?: ""
        val rawMessage = intent.getStringExtra(EXTRA_RAW_MESSAGE) ?: ""
        val units = intent.getStringExtra(EXTRA_UNITS) ?: ""
        val age = intent.getStringExtra(EXTRA_AGE) ?: ""
        val room = intent.getStringExtra(EXTRA_ROOM) ?: ""
        val cad = intent.getStringExtra(EXTRA_CAD) ?: ""

        // Extract unit number from address (e.g. #011 from "3 Hamaspik Way #011")
        val unitNumber = extractUnitNumber(address)

        // Join the Firebase call room and register this responder
        val myMemberId = preferencesManager.getLoggedInMemberId()
        if (cad.isNotBlank() && myMemberId > 0) {
            lifecycleScope.launch {
                try {
                    val me = memberRepository.getMemberById(myMemberId)
                    if (me != null && me.unitNumber.isNotBlank()) {
                        firebaseResponderRepository.ensureAuth()
                        firebaseResponderRepository.joinCall(
                            cad = cad,
                            memberId = me.id,
                            unitNumber = me.unitNumber,
                            name = me.name,
                            phone = me.phoneNumber,
                            status = preferencesManager.getResponderStatus().ifBlank { "BLUE" }
                        )
                    }
                } catch (_: Throwable) {}
            }
        }

        setContent {
            HatzolahTheme {
                DispatchAlertScreen(
                    address = address,
                    callType = callType,
                    unitNumber = unitNumber,
                    room = room,
                    rawMessage = rawMessage,
                    unitsRaw = units,
                    age = age,
                    cad = cad,
                    memberRepository = memberRepository,
                    firebaseResponderRepository = firebaseResponderRepository,
                    myMemberId = myMemberId,
                    onNavigate = { navigateToAddress(address) },
                    onCallMember = { phone -> dialNumber(phone) },
                    onOpenApp = {
                        try {
                            val mainIntent = Intent(this@DispatchAlertActivity, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            }
                            startActivity(mainIntent)
                        } catch (_: Throwable) {}
                        finish()
                    },
                    onDismiss = {
                        try { preferencesManager.clearActiveDispatch() } catch (_: Throwable) {}
                        try {
                            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                            nm.cancel(1001)
                        } catch (_: Throwable) {}
                        if (cad.isNotBlank() && myMemberId > 0) {
                            lifecycleScope.launch {
                                try { firebaseResponderRepository.leaveCall(cad, myMemberId) } catch (_: Throwable) {}
                            }
                        }
                        try {
                            val mainIntent = Intent(this@DispatchAlertActivity, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            }
                            startActivity(mainIntent)
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
        recreate()
    }

    private fun dialNumber(phone: String) {
        if (phone.isBlank()) return
        try {
            // Prefer ACTION_CALL if we have permission; fall back to ACTION_DIAL
            val hasCallPerm = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
            val action = if (hasCallPerm) Intent.ACTION_CALL else Intent.ACTION_DIAL
            startActivity(Intent(action, Uri.parse("tel:$phone")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Throwable) {
            try {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (_: Throwable) {}
        }
    }

    private fun navigateToAddress(address: String) {
        if (address.isBlank()) return
        var clean = address.trim()
        clean = clean.replace(Regex("\\s*#\\d+[A-Za-z]?"), "")
        clean = clean.replace(Regex("(?i)\\s*(apt|unit|suite|ste|rm|room)\\.?\\s*[A-Za-z0-9-]+"), "")
        clean = clean.replace(Regex(",\\s*,"), ",").trim().trimEnd(',', ' ')
        val encoded = Uri.encode(clean)

        val mapsIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("google.navigation:q=$encoded&mode=d")
        ).apply { setPackage("com.google.android.apps.maps") }

        val geoIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$encoded"))
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

/** A resolved unit on the alert screen — maps a code to member+status if possible. */
data class UnitEntry(
    val code: String,
    val classification: UnitClassifier.Classification,
    val member: Member?,
    val status: String // BLUE | GREEN | RED | "" (unknown / not on app)
)

@Composable
fun DispatchAlertScreen(
    address: String,
    callType: String,
    unitNumber: String,
    room: String = "",
    rawMessage: String,
    unitsRaw: String = "",
    age: String = "",
    cad: String = "",
    memberRepository: MemberRepository,
    firebaseResponderRepository: FirebaseResponderRepository,
    myMemberId: Long = -1,
    onNavigate: () -> Unit,
    onCallMember: (String) -> Unit,
    onOpenApp: () -> Unit,
    onDismiss: () -> Unit
) {
    val severity = getSeverity(callType)
    val bgColor = when (severity) {
        "CRITICAL" -> Color(0xFFD32F2F)
        "MODERATE" -> Color(0xFFE65100)
        else -> Color(0xFF1565C0)
    }

    val scope = rememberCoroutineScope()

    // Resolve unit codes → Member entries (one-time lookup)
    val codes = remember(unitsRaw) { UnitClassifier.splitUnits(unitsRaw) }
    val entriesState = remember { MutableStateFlow<List<UnitEntry>>(emptyList()) }

    // Live responder statuses from Firebase (keyed by memberId)
    val responderStatusState = remember { MutableStateFlow<Map<Long, String>>(emptyMap()) }

    LaunchedEffect(cad) {
        if (cad.isNotBlank()) {
            firebaseResponderRepository.observeResponders(cad).collectLatest { list ->
                responderStatusState.value = list.associate { it.memberId to it.status }
            }
        }
    }

    LaunchedEffect(codes) {
        val resolved = codes.map { code ->
            val classification = UnitClassifier.classify(code)
            val member = if (classification.kind == UnitClassifier.Kind.MEMBER) {
                try { memberRepository.getMemberByUnit(code) } catch (_: Throwable) { null }
            } else null
            UnitEntry(code, classification, member, "")
        }
        entriesState.value = resolved
    }

    val unitEntries by entriesState.collectAsState()
    val responderStatuses by responderStatusState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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

            Text(
                text = "DISPATCH",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

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

            if (age.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Patient: $age",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Restock note — tiny info icon, tappable to show supply checklist
            val suggestedSupplies = remember(callType) {
                com.hatzolah.app.util.SupplySuggestions.forCallType(callType)
            }
            var showSupplyDialog by remember { mutableStateOf(false) }

            if (suggestedSupplies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                IconButton(
                    onClick = { showSupplyDialog = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Restock note",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (showSupplyDialog) {
                AlertDialog(
                    onDismissRequest = { showSupplyDialog = false },
                    title = {
                        Text(
                            "Make sure you have",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column {
                            suggestedSupplies.forEach { supply ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 3.dp)
                                ) {
                                    Text("•", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(supply, fontSize = 15.sp)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showSupplyDialog = false }) {
                            Text("Got it")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp
                    )

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
                                            text = "APT",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            text = unitNumber.uppercase(),
                                            fontSize = 38.sp,
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
                                            fontSize = 38.sp,
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

            Spacer(modifier = Modifier.height(12.dp))

            // Responders / units
            if (unitEntries.isNotEmpty()) {
                Text(
                    text = "RESPONDERS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f),
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(unitEntries) { entry ->
                        val liveStatus = entry.member?.let { responderStatuses[it.id] } ?: ""
                        UnitChip(
                            entry = entry,
                            liveStatus = liveStatus,
                            isMe = entry.member?.id == myMemberId && myMemberId > 0,
                            onTap = { m ->
                                if (m.phoneNumber.isNotBlank()) onCallMember(m.phoneNumber)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onNavigate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    Icons.Default.Navigation,
                    contentDescription = null,
                    tint = bgColor,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "NAVIGATE",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = bgColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onOpenApp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "OPEN APP",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "DISMISS",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun UnitChip(
    entry: UnitEntry,
    liveStatus: String,
    isMe: Boolean,
    onTap: (Member) -> Unit
) {
    val kind = entry.classification.kind
    val effectiveStatus = if (isMe) {
        // "me" uses my own local status (if set)
        liveStatus.ifBlank { "BLUE" }
    } else {
        liveStatus // empty string = gray (not on app)
    }

    val (bgColor, fgColor, icon) = when {
        kind == UnitClassifier.Kind.AMBULANCE ->
            Triple(Color(0xFF455A64), Color.White, Icons.Default.LocalHospital)
        kind == UnitClassifier.Kind.NIGHT_VEHICLE ->
            Triple(Color(0xFF37474F), Color.White, Icons.Default.NightsStay)
        kind == UnitClassifier.Kind.SHABBOS_DRIVER ->
            Triple(Color(0xFF5D4037), Color.White, Icons.Default.DirectionsCar)
        entry.member == null ->
            Triple(Color(0xFF9E9E9E), Color.White, Icons.Default.Person) // unknown member = gray
        effectiveStatus == "GREEN" ->
            Triple(Color(0xFF2E7D32), Color.White, Icons.Default.Person) // on scene
        effectiveStatus == "RED" ->
            Triple(Color(0xFFC62828), Color.White, Icons.Default.Person) // left
        effectiveStatus == "BLUE" ->
            Triple(Color(0xFF1565C0), Color.White, Icons.Default.Person) // en route
        else ->
            Triple(Color(0xFF9E9E9E), Color.White, Icons.Default.Person) // not on app = gray
    }

    val tappable = entry.member != null && entry.member.phoneNumber.isNotBlank() && !isMe
    val borderColor = if (isMe) Color(0xFFFFEB3B) else Color.Transparent

    Card(
        modifier = Modifier
            .then(
                if (tappable) Modifier.clickable { onTap(entry.member!!) } else Modifier
            ),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(14.dp),
        border = if (isMe) androidx.compose.foundation.BorderStroke(3.dp, borderColor) else null
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fgColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Column {
                Text(
                    text = entry.code,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = fgColor
                )
                val subtitle = when {
                    isMe -> "me"
                    entry.member != null -> entry.member.name.take(12)
                    kind == UnitClassifier.Kind.AMBULANCE -> "Ambulance"
                    kind == UnitClassifier.Kind.NIGHT_VEHICLE -> "Night Vehicle"
                    kind == UnitClassifier.Kind.SHABBOS_DRIVER -> "Shabbos Driver"
                    else -> ""
                }
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        fontSize = 10.sp,
                        color = fgColor.copy(alpha = 0.85f)
                    )
                }
            }
            if (tappable) {
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Call",
                    tint = fgColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
