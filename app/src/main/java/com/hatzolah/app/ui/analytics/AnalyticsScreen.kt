package com.hatzolah.app.ui.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hatzolah.app.ui.dashboard.StatItem

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Analytics",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Team Stats (no individual breakdowns)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Team Overview",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(Icons.Default.Phone, uiState.teamTotalCalls.toString(), "Total Calls")
                    StatItem(Icons.Default.DirectionsCar, String.format("%.1f", uiState.teamTotalMiles), "Total Miles")
                    StatItem(Icons.Default.Timer, "${uiState.teamAvgResponse} min", "Avg Response")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Personal Stats
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Your Performance",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(Icons.Default.CalendarMonth, uiState.personalCallsThisMonth.toString(), "This Month")
                    StatItem(Icons.Default.Assignment, uiState.personalTotalCalls.toString(), "Total Calls")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(Icons.Default.Route, String.format("%.1f mi", uiState.personalTotalMiles), "Total Miles")
                    StatItem(Icons.Default.Schedule, String.format("%.1f hrs", uiState.personalTotalMinutes / 60.0), "Total Hours")
                }
            }
        }
    }
}
