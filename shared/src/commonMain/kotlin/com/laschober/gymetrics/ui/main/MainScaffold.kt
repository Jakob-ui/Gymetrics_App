package com.laschober.gymetrics.ui.main

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.laschober.gymetrics.ui.main.home.HomeScreen
import com.laschober.gymetrics.ui.main.profile.ProfileScreen
import com.laschober.gymetrics.ui.navigation.Destinations

private val tabs = listOf(
    Tab(Destinations.HomeRoute, "Home"),
    Tab(Destinations.LogbookRoute, "Logbook"),
    Tab(Destinations.PlanningRoute, "Planning"),
    Tab(Destinations.TemplatesRoute, "Templates"),
)

private data class Tab(val route: Any, val label: String)

@Preview
@Composable
fun MainScaffold(navController: NavHostController = rememberNavController(), onLoggedOut: () -> Unit = {}) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val title = when {
        currentDestination?.hasRoute(Destinations.ProfileRoute::class) == true -> "Profil"
        else -> tabs.firstOrNull {
            currentDestination?.hasRoute(it.route::class) == true
        }?.label ?: "Home"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                actions = {
                    Button(onClick = { navController.navigate(Destinations.ProfileRoute) { launchSingleTop = true }
                }) { Text("lol") }
                },
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .clip(RoundedCornerShape(28.dp)),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                tabs.forEach { tab ->
                    val selected = currentDestination
                        ?.hierarchy
                        ?.any { it.hasRoute(tab.route::class) } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Text(tab.label.take(1)) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
            navController = navController,
            startDestination = Destinations.HomeRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<Destinations.HomeRoute> { HomeScreen() }
            composable<Destinations.LogbookRoute> { Text("Logbook – kommt") }
            composable<Destinations.PlanningRoute> { Text("Planning – kommt") }
            composable<Destinations.TemplatesRoute> { Text("Templates – kommt") }
            composable<Destinations.ProfileRoute> {
                ProfileScreen(onLogoutClick = onLoggedOut)
            }
        }
    }
}