package com.hatzolah.app.ui.protocols

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class Protocol(
    val title: String,
    val content: String,
    val category: String
)

@Composable
fun ProtocolsScreen() {
    val protocols = remember { getEmergencyProtocols() }
    var expandedIndex by remember { mutableIntStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Emergency Protocols",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Quick-access critical medical reference",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(protocols.indices.toList()) { index ->
                val protocol = protocols[index]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        expandedIndex = if (expandedIndex == index) -1 else index
                    }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = protocol.title,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = protocol.category,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                imageVector = if (expandedIndex == index)
                                    Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }

                        if (expandedIndex == index) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = protocol.content,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getEmergencyProtocols(): List<Protocol> = listOf(
    Protocol(
        title = "Cardiac Arrest (Adult)",
        category = "Cardiac",
        content = """
            1. Confirm unresponsiveness and call for help
            2. Begin CPR: 30 compressions to 2 breaths
            3. Compression rate: 100-120 per minute
            4. Compression depth: 2-2.4 inches
            5. Apply AED as soon as available
            6. Minimize interruptions in chest compressions
            7. Switch compressors every 2 minutes
            8. Continue until EMS arrives or patient recovers
        """.trimIndent()
    ),
    Protocol(
        title = "Choking (Adult)",
        category = "Airway",
        content = """
            Conscious patient:
            1. Ask "Are you choking?"
            2. Encourage coughing if partial obstruction
            3. Perform abdominal thrusts (Heimlich maneuver)
            4. Continue until object is expelled or patient becomes unconscious

            Unconscious patient:
            1. Lower patient to ground
            2. Call for help
            3. Begin CPR - look for object before giving breaths
            4. If visible, remove object with finger sweep
        """.trimIndent()
    ),
    Protocol(
        title = "Severe Bleeding",
        category = "Trauma",
        content = """
            1. Apply direct pressure with clean cloth
            2. If wound is on a limb, apply tourniquet 2-3 inches above wound
            3. Note time of tourniquet application
            4. Do NOT remove tourniquet once applied
            5. Keep patient warm and monitor for shock
            6. Elevate injured area if possible
            7. Do NOT remove embedded objects
        """.trimIndent()
    ),
    Protocol(
        title = "Allergic Reaction / Anaphylaxis",
        category = "Medical",
        content = """
            Signs: Hives, swelling, difficulty breathing, low blood pressure

            1. Administer epinephrine auto-injector (EpiPen) to outer thigh
            2. Call 911 immediately
            3. Position patient: sitting up if breathing difficulty, lying down if low BP
            4. Monitor airway continuously
            5. Be prepared to administer second dose in 5-15 minutes
            6. Begin CPR if patient becomes unresponsive
        """.trimIndent()
    ),
    Protocol(
        title = "Seizure Management",
        category = "Neurological",
        content = """
            1. Clear area of hazards
            2. Do NOT restrain the patient
            3. Do NOT put anything in the mouth
            4. Time the seizure
            5. Place on side (recovery position) after seizure ends
            6. Monitor airway
            7. Call EMS if: first seizure, seizure > 5 minutes, repeated seizures, or injury
        """.trimIndent()
    ),
    Protocol(
        title = "Stroke Recognition (FAST)",
        category = "Neurological",
        content = """
            F - Face: Ask person to smile. Does one side droop?
            A - Arms: Ask person to raise both arms. Does one drift downward?
            S - Speech: Ask person to repeat a simple phrase. Is speech slurred?
            T - Time: If any signs present, call 911 immediately

            Note the time symptoms first appeared - critical for treatment decisions
            Do NOT give food or water
            Keep patient comfortable and monitor
        """.trimIndent()
    ),
    Protocol(
        title = "Diabetic Emergency",
        category = "Medical",
        content = """
            Low blood sugar (hypoglycemia):
            - Symptoms: Confusion, shakiness, sweating, pale skin
            - If conscious: Give sugar (juice, glucose tablets, candy)
            - If unconscious: Do NOT give food/drink, call 911, recovery position

            High blood sugar (hyperglycemia):
            - Symptoms: Frequent urination, excessive thirst, fruity breath
            - Call 911
            - Monitor airway and breathing
        """.trimIndent()
    ),
    Protocol(
        title = "OB Emergency / Active Labor",
        category = "OB/GYN",
        content = """
            1. Call dispatch and request OB-trained responder
            2. Keep patient comfortable and calm
            3. Time contractions (frequency and duration)
            4. If delivery imminent: support baby's head, do NOT pull
            5. Keep baby warm and dry
            6. Clamp cord but do NOT cut unless trained
            7. Monitor mother for excessive bleeding
            8. Transport mother and baby together
        """.trimIndent()
    )
)
