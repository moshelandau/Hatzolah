package com.hatzolah.app.ui.member

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun MemberDirectoryScreen(
    viewModel: MemberDirectoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::onSearchChanged,
            label = { Text("Search members by name or unit # (e.g. 89)") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true
        )

        // Sort options row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sort:",
                style = MaterialTheme.typography.labelMedium
            )
            FilterChip(
                selected = uiState.sortOption == MemberSortOption.NAME,
                onClick = { viewModel.onSortChanged(MemberSortOption.NAME) },
                label = { Text("Name") }
            )
            FilterChip(
                selected = uiState.sortOption == MemberSortOption.UNIT,
                onClick = { viewModel.onSortChanged(MemberSortOption.UNIT) },
                label = { Text("Member ID") }
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.members) { member ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Unit number badge — the operator's primary
                            // identifier, shown before the name so it's easy
                            // to scan a list by unit.
                            if (member.unitNumber.isNotBlank()) {
                                AssistChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            text = member.unitNumber,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    enabled = false
                                )
                            }
                            Text(
                                text = member.name,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // WhatsApp / email actions (phone number intentionally
                        // removed — the directory is a roster, not a dialer).
                        if (member.whatsappContact.isNotBlank() || member.email.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                if (member.whatsappContact.isNotBlank()) {
                                    AssistChip(
                                        onClick = {
                                            val url = "https://wa.me/${member.whatsappContact.replace(Regex("[^0-9]"), "")}"
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                        },
                                        label = { Text("WhatsApp") },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Chat,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }
                            }

                            if (member.email.isNotBlank()) {
                                Text(
                                    text = member.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable {
                                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${member.email}"))
                                        context.startActivity(intent)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
