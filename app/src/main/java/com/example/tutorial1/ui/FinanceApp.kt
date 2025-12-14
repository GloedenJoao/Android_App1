package com.example.tutorial1.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.tutorial1.ui.screens.DashboardScreen
import com.example.tutorial1.ui.screens.HomeScreen
import com.example.tutorial1.ui.screens.SimulationScreen

enum class AppDestination(val label: String) {
    HOME("Painel"),
    SIMULATION("Simulação"),
    DASHBOARD("Dashboard")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceApp(viewModel: FinanceViewModel) {
    var destination by remember { mutableStateOf(AppDestination.HOME) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = destination == AppDestination.HOME,
                    onClick = { destination = AppDestination.HOME },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text(AppDestination.HOME.label) }
                )
                NavigationBarItem(
                    selected = destination == AppDestination.SIMULATION,
                    onClick = { destination = AppDestination.SIMULATION },
                    icon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                    label = { Text(AppDestination.SIMULATION.label) }
                )
                NavigationBarItem(
                    selected = destination == AppDestination.DASHBOARD,
                    onClick = { destination = AppDestination.DASHBOARD },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = null) },
                    label = { Text(AppDestination.DASHBOARD.label) }
                )
            }
        }
    ) { padding ->
        when (destination) {
            AppDestination.HOME -> HomeScreen(viewModel, padding)
            AppDestination.SIMULATION -> SimulationScreen(viewModel, padding)
            AppDestination.DASHBOARD -> DashboardScreen(viewModel, padding)
        }
    }
}
