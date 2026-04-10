package com.hatzolah.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hatzolah.app.ui.admin.AdminScreen
import com.hatzolah.app.ui.analytics.AnalyticsScreen
import com.hatzolah.app.ui.callhistory.CallDocumentationScreen
import com.hatzolah.app.ui.callhistory.CallHistoryScreen
import com.hatzolah.app.ui.dashboard.DashboardScreen
import com.hatzolah.app.ui.hospital.HospitalDirectoryScreen
import com.hatzolah.app.ui.member.MemberDirectoryScreen
import com.hatzolah.app.ui.protocols.ProtocolsScreen
import com.hatzolah.app.ui.rma.RmaScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", "Home", Icons.Default.Home)
    data object Hospitals : Screen("hospitals", "Hospitals", Icons.Default.LocalHospital)
    data object Members : Screen("members", "Members", Icons.Default.People)
    data object Analytics : Screen("analytics", "Stats", Icons.Default.BarChart)
    data object CallHistory : Screen("call_history", "Calls", Icons.Default.History)
    data object Rma : Screen("rma", "RMA", Icons.Default.Call)
    data object Protocols : Screen("protocols", "Protocols", Icons.Default.MedicalServices)
    data object Admin : Screen("admin", "Admin", Icons.Default.AdminPanelSettings)
    data object CallDocumentation : Screen("document/{callId}", "Document", Icons.Default.Edit)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Hospitals,
    Screen.Members,
    Screen.CallHistory,
    Screen.Analytics
)

val drawerItems = listOf(
    Screen.Dashboard,
    Screen.Hospitals,
    Screen.Members,
    Screen.CallHistory,
    Screen.Analytics,
    Screen.Rma,
    Screen.Protocols,
    Screen.Admin
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(onLogout: () -> Unit = {}) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.Dashboard.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        },
        topBar = {
            val title = drawerItems.find { it.route == currentRoute }?.title ?: "Hatzolah"
            TopAppBar(
                title = { Text(title) },
                actions = {
                    // RMA quick access
                    IconButton(onClick = {
                        navController.navigate(Screen.Rma.route)
                    }) {
                        Icon(Icons.Default.Call, contentDescription = "RMA")
                    }
                    // Protocols quick access
                    IconButton(onClick = {
                        navController.navigate(Screen.Protocols.route)
                    }) {
                        Icon(Icons.Default.MedicalServices, contentDescription = "Protocols")
                    }
                    // Admin access
                    IconButton(onClick = {
                        navController.navigate(Screen.Admin.route)
                    }) {
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
            composable(Screen.Admin.route) { AdminScreen(onLogout = onLogout) }
            composable(
                route = "document/{callId}",
                arguments = listOf(navArgument("callId") { type = NavType.LongType })
            ) { backStackEntry ->
                val callId = backStackEntry.arguments?.getLong("callId") ?: -1
                CallDocumentationScreen(
                    callLogId = callId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
