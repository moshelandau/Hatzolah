package com.hatzolah.app.ui.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hatzolah.app.ui.dashboard.StatCard

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Analytics",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Team Stats
        Text("Team Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(Modifier.weight(1f), Icons.Default.Phone, uiState.teamTotalCalls.toString(), "Total Calls", MaterialTheme.colorScheme.primary)
            StatCard(Modifier.weight(1f), Icons.Default.DirectionsCar, String.format("%.0f", uiState.teamTotalMiles), "Miles", Color(0xFF2E7D32))
            StatCard(Modifier.weight(1f), Icons.Default.Timer, "${uiState.teamAvgResponse}m", "Avg Response", Color(0xFFE65100))
        }

        // Personal Stats
        Text("Your Performance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(Modifier.weight(1f), Icons.Default.CalendarMonth, uiState.personalCallsThisMonth.toString(), "This Month", Color(0xFF6A1B9A))
            StatCard(Modifier.weight(1f), Icons.Default.Assignment, uiState.personalTotalCalls.toString(), "Total Calls", MaterialTheme.colorScheme.primary)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(Modifier.weight(1f), Icons.Default.Route, String.format("%.1f", uiState.personalTotalMiles), "Miles", Color(0xFF2E7D32))
            StatCard(Modifier.weight(1f), Icons.Default.Schedule, String.format("%.1f", uiState.personalTotalMinutes / 60.0), "Hours", Color(0xFFE65100))
        }
    }
}
