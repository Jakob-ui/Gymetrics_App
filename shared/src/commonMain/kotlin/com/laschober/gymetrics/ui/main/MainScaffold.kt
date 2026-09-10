package com.laschober.gymetrics.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import androidx.navigation.toRoute
import com.laschober.gymetrics.data.local.SettingStore
import com.laschober.gymetrics.data.network.ConnectivityObserver
import com.laschober.gymetrics.ui.main.home.HomeScreen
import com.laschober.gymetrics.ui.main.logbook.TrainingScreen
import com.laschober.gymetrics.ui.main.profile.ProfileScreen
import com.laschober.gymetrics.ui.main.settings.SettingScreen
import com.laschober.gymetrics.ui.main.templates.TemplateScreen
import com.laschober.gymetrics.ui.main.templates.TemplateScreenViewModel
import com.laschober.gymetrics.ui.main.templates.detail.TemplateFormScreen
import com.laschober.gymetrics.ui.navigation.Destinations
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private val tabs = listOf(
    Tab(Destinations.HomeRoute, "Home"),
    Tab(Destinations.PlanningRoute, "Planning"),
    Tab(Destinations.LogbookRoute, "Logbook"),
    Tab(Destinations.TemplateRoute, "Templates"),
)

private data class Tab(val route: Any, val label: String)

@Composable
fun MainScaffold(
    navController: NavHostController = rememberNavController(),
    settingStore: SettingStore = koinInject(),
    connectivityObserver: ConnectivityObserver = koinInject(),
    onLoggedOut: () -> Unit = {},
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val name = settingStore.getName().orEmpty()

    var showProfile by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val isCreateTemplate = currentDestination?.hasRoute(Destinations.CreateTemplateRoute::class) == true
    val isTemplateDetail = currentDestination?.hasRoute(Destinations.TemplateDetailRoute::class) == true

    val title = when {
        isCreateTemplate -> "New Template"
        isTemplateDetail -> backStackEntry?.toRoute<Destinations.TemplateDetailRoute>()?.title ?: "Template"
        else -> tabs.firstOrNull { currentDestination?.hasRoute(it.route::class) == true }?.label ?: "Home"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (isCreateTemplate || isTemplateDetail) {
                        IconButton(onClick = { navController.popBackStack() }) { Text("←") }
                    }
                },
                actions = {
                    val isOnline by connectivityObserver.isOnline.collectAsState()
                    if (!isOnline) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.padding(end = 8.dp),
                        ) {
                            Text("Offline!")
                        }
                    }
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
        // Scaffold measures FloatingNavBar's real height (including the device's navigation-bar
        // inset, which varies) into innerPadding - use that instead of a guessed dp value, so
        // list content padding actually clears the pill on every device.
        val bottomBarClearance = innerPadding.calculateBottomPadding() + 16.dp

        NavHost(
            // EnterTransition.None/ExitTransition.None together don't mean "instant, clean swap" -
            // the exiting screen just never animates away, so it stays fully visible while the
            // entering one is already fully visible too, overlapping for a few frames. A very
            // short fade avoids that while still feeling near-instant.
            enterTransition = { fadeIn(animationSpec = tween(120)) },
            exitTransition = { fadeOut(animationSpec = tween(120)) },
            popEnterTransition = { fadeIn(animationSpec = tween(120)) },
            popExitTransition = { fadeOut(animationSpec = tween(120)) },
            navController = navController,
            startDestination = Destinations.HomeRoute,
            modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
        ) {
            composable<Destinations.HomeRoute> { HomeScreen() }
            composable<Destinations.LogbookRoute> { TrainingScreen(bottomPadding = bottomBarClearance) }
            composable<Destinations.PlanningRoute> { Text("Planning – coming soon") }
            composable<Destinations.TemplateRoute> { entry ->
                val viewModel: TemplateScreenViewModel = koinViewModel()
                // TemplateFormScreen sets this flag (via previousBackStackEntry) right before
                // popping back after a save/delete. This entry stays alive the whole time we're
                // on TemplateDetailRoute/CreateTemplateRoute (it's still on the back stack), so
                // its ViewModel is never recreated - without this, the list would keep showing
                // stale data after an edit.
                val changed by entry.savedStateHandle.getStateFlow("templatesChanged", false).collectAsState()
                LaunchedEffect(changed) {
                    if (changed) {
                        viewModel.load()
                        entry.savedStateHandle["templatesChanged"] = false
                    }
                }
                TemplateScreen(
                    viewModel = viewModel,
                    onTemplateClick = { id, title -> navController.navigate(Destinations.TemplateDetailRoute(id, title)) },
                    onAddClick = { navController.navigate(Destinations.CreateTemplateRoute) },
                    bottomPadding = bottomBarClearance,
                )
            }
            composable<Destinations.TemplateDetailRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<Destinations.TemplateDetailRoute>()
                TemplateFormScreen(
                    id = route.id,
                    onBack = { navController.popBackStack() },
                    onSaved = {
                        navController.previousBackStackEntry?.savedStateHandle?.set("templatesChanged", true)
                        navController.popBackStack()
                    },
                )
            }
            composable<Destinations.CreateTemplateRoute> {
                TemplateFormScreen(
                    id = null,
                    onBack = { navController.popBackStack() },
                    onSaved = {
                        navController.previousBackStackEntry?.savedStateHandle?.set("templatesChanged", true)
                        navController.popBackStack()
                    },
                )
            }
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

@Composable
private fun FloatingNavBar(
    currentDestination: androidx.navigation.NavDestination?,
    onSelect: (Any) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(vertical = 8.dp),
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