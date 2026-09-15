package de.privat.schmuddelwetter.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.privat.schmuddelwetter.di.ServiceLocator
import de.privat.schmuddelwetter.ui.aviation.AviationScreen
import de.privat.schmuddelwetter.ui.home.HomeScreen
import de.privat.schmuddelwetter.ui.settings.SettingsScreen

private sealed class Destination(val route: String, val label: String, val icon: ImageVector) {
    object Home : Destination("home", "Wetter", Icons.Filled.Cloud)
    object Aviation : Destination("aviation", "Flugwetter", Icons.Filled.Flight)
    object Settings : Destination("settings", "Einstellungen", Icons.Filled.Settings)
}

private val destinations = listOf(Destination.Home, Destination.Aviation, Destination.Settings)

@Composable
fun SchmuddelwetterNavGraph(serviceLocator: ServiceLocator) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = backStackEntry?.destination

                destinations.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(imageVector = destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Destination.Home.route) { HomeScreen(serviceLocator = serviceLocator) }
            composable(Destination.Aviation.route) { AviationScreen(serviceLocator = serviceLocator) }
            composable(Destination.Settings.route) { SettingsScreen(serviceLocator = serviceLocator) }
        }
    }
}
