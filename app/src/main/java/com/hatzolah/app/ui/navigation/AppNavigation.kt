package com.hatzolah.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hatzolah.app.ui.admin.AdminScreen
import com.hatzolah.app.ui.analytics.AnalyticsScreen
import com.hatzolah.app.ui.calendar.CalendarScreen
import com.hatzolah.app.ui.callhistory.CallDocumentationScreen
import com.hatzolah.app.ui.callhistory.CallHistoryScreen
import com.hatzolah.app.ui.dashboard.DashboardScreen
import com.hatzolah.app.ui.hospital.HospitalDirectoryScreen
import com.hatzolah.app.ui.member.MemberDirectoryScreen
import com.hatzolah.app.ui.protocols.ProtocolsScreen
import com.hatzolah.app.ui.residents.ResidentsScreen
import com.hatzolah.app.ui.rma.RmaScreen
import com.hatzolah.app.ui.supplies.SuppliesScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", "Home", Icons.Default.Home)
    data object Hospitals : Screen("hospitals", "Hospitals", Icons.Default.LocalHospital)
    data object Residents : Screen("residents", "Phones", Icons.Default.ContactPhone)
    data object Members : Screen("members", "Members", Icons.Default.People)
    data object Analytics : Screen("analytics", "Stats", Icons.Default.BarChart)
    data object CallHistory : Screen("call_history", "Calls", Icons.Default.History)
    data object Supplies : Screen("supplies", "Supplies", Icons.Default.Inventory)
    data object Rma : Screen("rma", "RMA", Icons.Default.Call)
    data object Protocols : Screen("protocols", "Protocols", Icons.Default.MedicalServices)
    data object Calendar : Screen("calendar", "Calendar", Icons.Default.CalendarMonth)
    data object Admin : Screen("admin", "Admin", Icons.Default.AdminPanelSettings)
    data object CallDocumentation : Screen("document/{callId}", "Document", Icons.Default.Edit)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Hospitals,
    Screen.Members,
    Screen.CallHistory,
    Screen.Calendar
)

val drawerItems = listOf(
    Screen.Dashboard,
    Screen.Hospitals,
    Screen.Residents,
    Screen.Members,
    Screen.CallHistory,
    Screen.Calendar,
    Screen.Supplies,
    Screen.Analytics,
    Screen.Rma,
    Screen.Protocols,
    Screen.Admin
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // Single helper used by both the bottom nav and the top-bar icons so every
    // top-level destination is popped back to Dashboard before navigating.
    // This keeps the back stack from growing unbounded when the user flips
    // between Admin / Calendar / Hospitals / etc. via the top bar, which is
    // what caused the "Home button does nothing after opening Admin" bug.
    val navigateTopLevel: (String) -> Unit = { route ->
        if (currentRoute != route) {
            navController.navigate(route) {
                popUpTo(Screen.Dashboard.route) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title, fontSize = 11.sp) },
                        selected = currentRoute == screen.route,
                        onClick = { navigateTopLevel(screen.route) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        },
        topBar = {
            val title = drawerItems.find { it.route == currentRoute }?.title ?: "Hatzolah"
            TopAppBar(
                title = {
                    Text(
                        title,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { navigateTopLevel(Screen.Analytics.route) }) {
                        Icon(Icons.Default.BarChart, contentDescription = "Stats")
                    }
                    IconButton(onClick = { navigateTopLevel(Screen.Supplies.route) }) {
                        Icon(Icons.Default.Inventory, contentDescription = "Supplies")
                    }
                    IconButton(onClick = { navigateTopLevel(Screen.Rma.route) }) {
                        Icon(Icons.Default.Call, contentDescription = "RMA")
                    }
                    IconButton(onClick = { navigateTopLevel(Screen.Protocols.route) }) {
                        Icon(Icons.Default.MedicalServices, contentDescription = "Protocols")
                    }
                    IconButton(onClick = { navigateTopLevel(Screen.Admin.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Admin")
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) { DashboardScreen() }
            composable(Screen.Hospitals.route) { HospitalDirectoryScreen() }
            composable(Screen.Residents.route) { ResidentsScreen() }
            composable(Screen.Members.route) { MemberDirectoryScreen() }
            composable(Screen.CallHistory.route) {
                CallHistoryScreen(
                    onDocumentCall = { callId ->
                        navController.navigate("document/$callId")
                    }
                )
            }
            composable(Screen.Analytics.route) { AnalyticsScreen() }
            composable(Screen.Rma.route) {
                RmaScreen(
                    onDocumentCall = { callId ->
                        navController.navigate("document/$callId")
                    }
                )
            }
            composable(Screen.Protocols.route) { ProtocolsScreen() }
            composable(Screen.Supplies.route) { SuppliesScreen() }
            composable(Screen.Calendar.route) {
                CalendarScreen(
                    onOpenCall = { callId ->
                        navController.navigate("document/$callId")
                    }
                )
            }
            composable(Screen.Admin.route) { AdminScreen() }
            composable(
                route = "document/{callId}",
                arguments = listOf(navArgument("callId") {
                    type = NavType.LongType
                    defaultValue = -1L
                })
            ) { backStackEntry ->
                val callId = backStackEntry.arguments?.getLong("callId") ?: -1L
                if (callId <= 0L) {
                    // Invalid ID - show toast and pop back
                    val context = LocalContext.current
                    LaunchedEffect(Unit) {
                        Toast.makeText(context, "Invalid call", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                } else {
                    CallDocumentationScreen(
                        callLogId = callId,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
