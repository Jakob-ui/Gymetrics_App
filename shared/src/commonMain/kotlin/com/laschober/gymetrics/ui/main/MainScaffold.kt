package com.laschober.gymetrics.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.laschober.gymetrics.data.local.SettingStore
import com.laschober.gymetrics.ui.main.home.HomeScreen
import com.laschober.gymetrics.ui.main.profile.ProfileScreen
import com.laschober.gymetrics.ui.main.settings.SettingScreen
import com.laschober.gymetrics.ui.main.templates.TemplateScreen
import com.laschober.gymetrics.ui.navigation.Destinations
import org.koin.compose.koinInject

private val tabs = listOf(
    Tab(Destinations.HomeRoute, "Home"),
    Tab(Destinations.LogbookRoute, "Logbook"),
    Tab(Destinations.PlanningRoute, "Planning"),
    Tab(Destinations.TemplateRoute, "Templates"),
)

private data class Tab(val route: Any, val label: String)

@Composable
fun MainScaffold(
    navController: NavHostController = rememberNavController(),
    settingStore: SettingStore = koinInject(),
    onLoggedOut: () -> Unit = {},
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val name = settingStore.getName().orEmpty()

    var showProfile by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val title = tabs.firstOrNull {
        currentDestination?.hasRoute(it.route::class) == true
    }?.label ?: "Home"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                actions = {
                    IconButton(onClick = { showProfile = true }) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(name.take(1).uppercase().ifBlank { "?" }, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                },
            )
        },
        bottomBar = {
            FloatingNavBar(
                currentDestination = currentDestination,
                onSelect = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                },
            )
        },
    ) { innerPadding ->
        NavHost(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
            navController = navController,
            startDestination = Destinations.HomeRoute,
            // Only reserve space for the top bar. The content runs full-height to
            // the bottom edge so scrolling lists pass BEHIND the floating pill;
            // each screen adds its own bottom contentPadding to clear it.
            modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
        ) {
            composable<Destinations.HomeRoute> { HomeScreen() }
            composable<Destinations.LogbookRoute> { Text("Logbook – coming soon") }
            composable<Destinations.PlanningRoute> { Text("Planning – coming soon") }
            composable<Destinations.TemplateRoute> { TemplateScreen() }
        }

        if (showProfile) {
            Dialog(
                onDismissRequest = { showProfile = false },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    ProfileScreen(
                        onBack = { showProfile = false },
                        onLogoutClick = {
                            showProfile = false
                            onLoggedOut()
                        },
                        // Profile stays open underneath; Settings slides over it.
                        onOpenSettings = { showSettings = true },
                    )
                }
            }
        }

        if (showSettings) {
            SettingsOverlay(onClose = { showSettings = false })
        }
    }
}

// Custom bottom bar: a rounded pill that floats over the content. The Row fills
// the width (so Scaffold reserves the right height), but only the clipped inner
// area is painted - the transparent margins let the content show around it.
@Composable
private fun FloatingNavBar(
    currentDestination: androidx.navigation.NavDestination?,
    onSelect: (Any) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp, vertical = 12.dp)   // outside the pill (transparent)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(vertical = 8.dp),                        // inside the pill
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { tab ->
            val selected = currentDestination
                ?.hierarchy
                ?.any { it.hasRoute(tab.route::class) } == true

            val color = if (selected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelect(tab.route) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(tab.label.take(1), color = color, style = MaterialTheme.typography.titleMedium)
                Text(tab.label, color = color, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun SettingsOverlay(onClose: () -> Unit) {
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }

    Dialog(
        onDismissRequest = { visible.targetState = false },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        AnimatedVisibility(
            visibleState = visible,
            enter = slideInHorizontally(animationSpec = tween(260), initialOffsetX = { it }),
            exit = slideOutHorizontally(animationSpec = tween(150), targetOffsetX = { it }),
        ) {
            SettingScreen(onBack = { visible.targetState = false })
        }
    }

    LaunchedEffect(visible.isIdle) {
        if (visible.isIdle && !visible.currentState) onClose()
    }
}
