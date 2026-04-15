package com.hatzolah.app.ui.member

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hatzolah.app.data.database.entity.Member

@Composable
fun MemberDirectoryScreen(
    viewModel: MemberDirectoryViewModel = hiltViewModel()
) {
    // Force LTR for the Members screen so English-only member names/unit
    // numbers always lay out left-to-right regardless of the device locale.
    // Without this, on Hebrew devices the search box and first result are
    // right-aligned which the operator reported as "the first in the search
    // move to right".
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        val uiState by viewModel.uiState.collectAsState()
        val context = LocalContext.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search box with clear button
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchChanged,
                placeholder = { Text("Search by name or unit # (e.g. 89)") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(28.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )

            // Sort row — compact segmented look
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sort by",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FilterChip(
                    selected = uiState.sortOption == MemberSortOption.NAME,
                    onClick = { viewModel.onSortChanged(MemberSortOption.NAME) },
                    label = { Text("Name") },
                    shape = RoundedCornerShape(50)
                )
                FilterChip(
                    selected = uiState.sortOption == MemberSortOption.UNIT,
                    onClick = { viewModel.onSortChanged(MemberSortOption.UNIT) },
                    label = { Text("Member ID") },
                    shape = RoundedCornerShape(50)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${uiState.members.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            if (uiState.members.isEmpty()) {
                EmptyMembersState(hasQuery = uiState.searchQuery.isNotBlank())
            } else {
                // Selected member drives the detail dialog.
                var selectedMember by remember { mutableStateOf<Member?>(null) }

                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.members, key = { it.id }) { member ->
                        MemberCard(
                            member = member,
                            onView = { selectedMember = member },
                            onCall = { dialMember(context, member) },
                            onWhatsApp = { openWhatsApp(context, member) }
                        )
                    }
                }

                selectedMember?.let { m ->
                    MemberDetailsDialog(
                        member = m,
                        onDismiss = { selectedMember = null },
                        onCall = {
                            dialMember(context, m)
                            selectedMember = null
                        },
                        onWhatsApp = {
                            openWhatsApp(context, m)
                            selectedMember = null
                        },
                        onEmail = {
                            val intent = Intent(
                                Intent.ACTION_SENDTO,
                                Uri.parse("mailto:${m.email}")
                            )
                            context.startActivity(intent)
                            selectedMember = null
                        }
                    )
                }
            }
        }
    }
}

private fun dialMember(context: android.content.Context, member: Member) {
    if (member.phoneNumber.isBlank()) return
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${member.phoneNumber}"))
    context.startActivity(intent)
}

private fun openWhatsApp(context: android.content.Context, member: Member) {
    if (member.whatsappContact.isBlank()) return
    val digits = member.whatsappContact.replace(Regex("[^0-9]"), "")
    val url = "https://wa.me/$digits"
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

@Composable
private fun MemberCard(
    member: Member,
    onView: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onView),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left: Unit badge (primary identifier) OR initials fallback
            if (member.unitNumber.isNotBlank()) {
                UnitAvatar(unitNumber = member.unitNumber)
            } else {
                InitialsAvatar(name = member.name)
            }

            // Middle: name (tap card to view details)
            Text(
                text = member.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )

            // Right: fixed action area — call + WhatsApp
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (member.phoneNumber.isNotBlank()) {
                    FilledTonalIconButton(
                        onClick = onCall,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = "Call ${member.name}",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                if (member.whatsappContact.isNotBlank()) {
                    FilledTonalIconButton(
                        onClick = onWhatsApp,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Chat,
                            contentDescription = "WhatsApp ${member.name}",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Full-details popup shown when the operator taps a member card. Shows the
 * unit number, name, phone number, admin status, plus inline Call / WhatsApp
 * / Email actions. Dismissed by tapping outside or the close button.
 */
@Composable
private fun MemberDetailsDialog(
    member: Member,
    onDismiss: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
    onEmail: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (member.unitNumber.isNotBlank()) {
                    UnitAvatar(unitNumber = member.unitNumber)
                } else {
                    InitialsAvatar(name = member.name)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2
                    )
                    if (member.isAdmin) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Admin",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (member.phoneNumber.isNotBlank()) {
                    DetailRow(
                        icon = Icons.Default.Phone,
                        label = "Phone",
                        value = formatPhone(member.phoneNumber),
                        onClick = onCall
                    )
                }
                if (member.whatsappContact.isNotBlank() &&
                    member.whatsappContact != member.phoneNumber
                ) {
                    DetailRow(
                        icon = Icons.Default.Chat,
                        label = "WhatsApp",
                        value = formatPhone(member.whatsappContact),
                        onClick = onWhatsApp
                    )
                }
                if (member.email.isNotBlank()) {
                    DetailRow(
                        icon = Icons.Default.Email,
                        label = "Email",
                        value = member.email,
                        onClick = onEmail
                    )
                }
            }
        },
        confirmButton = {
            if (member.phoneNumber.isNotBlank()) {
                FilledTonalButton(onClick = onCall) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Call")
                }
            }
        },
        dismissButton = {
            if (member.whatsappContact.isNotBlank()) {
                TextButton(onClick = onWhatsApp) {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("WhatsApp")
                }
            }
        }
    )
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** Formats a 10-digit US phone number as (xxx) xxx-xxxx for display. */
private fun formatPhone(phone: String): String {
    val digits = phone.filter { it.isDigit() }
    return when {
        digits.length == 10 ->
            "(${digits.substring(0, 3)}) ${digits.substring(3, 6)}-${digits.substring(6)}"
        digits.length == 11 && digits.startsWith("1") ->
            "(${digits.substring(1, 4)}) ${digits.substring(4, 7)}-${digits.substring(7)}"
        else -> phone
    }
}

/**
 * Avatar that shows the member's unit number (KY85, KM03, etc.) as the
 * dominant visual — primary-colored pill, bold label. Used as the leading
 * element of every member card so the operator's ID is the first thing
 * the eye lands on.
 */
@Composable
private fun UnitAvatar(unitNumber: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.defaultMinSize(minWidth = 56.dp, minHeight = 44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = unitNumber.uppercase(),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun InitialsAvatar(name: String) {
    val initials = remember(name) {
        name.split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first().uppercaseChar() }
            .joinToString("")
            .ifBlank { "?" }
    }
    val bgColor = remember(name) {
        val palette = listOf(
            Color(0xFF1976D2), Color(0xFF388E3C), Color(0xFF7B1FA2),
            Color(0xFFD32F2F), Color(0xFFF57C00), Color(0xFF0288D1),
            Color(0xFF512DA8), Color(0xFFC2185B), Color(0xFF00796B),
            Color(0xFF5D4037)
        )
        palette[(name.hashCode().let { if (it < 0) -it else it }) % palette.size]
    }
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun EmptyMembersState(hasQuery: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.People,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (hasQuery) "No members match your search" else "No members yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (hasQuery) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Try a different name or unit number",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
