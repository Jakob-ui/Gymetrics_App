package com.laschober.gymetrics.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.laschober.gymetrics.ui.auth.login.LoginScreen
import com.laschober.gymetrics.ui.auth.register.RegisterScreen
import com.laschober.gymetrics.ui.main.MainScaffold
import com.laschober.gymetrics.ui.serverconnection.ServerConnectionScreen

@Composable
fun AppNavHost(
    start: Any = Destinations.ServerConnectionRoute,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = start,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        composable<Destinations.ServerConnectionRoute> {
            ServerConnectionScreen(
                onContinue = { navController.navigate(Destinations.LoginRoute) },
            )
        }
        composable<Destinations.LoginRoute> {
            LoginScreen(
                onServerAdressClick = { navController.navigate(Destinations.ServerConnectionRoute) },
                onRegisterClick = { navController.navigate(Destinations.RegisterRoute) },
                onLoginSuccess = {
                    navController.navigate(Destinations.MainRoute) {
                        popUpTo(Destinations.ServerConnectionRoute) { inclusive = true }
                    }
                },
            )
        }
        composable<Destinations.RegisterRoute> {
            RegisterScreen(
                onLoginClick = { navController.navigate(Destinations.LoginRoute) },
                onRegisterSuccess = {
                    navController.navigate(Destinations.MainRoute) {
                        popUpTo(Destinations.ServerConnectionRoute) { inclusive = true }
                    }
                },
            )
        }
        composable<Destinations.MainRoute> {
            MainScaffold(
                onLoggedOut = {
                    navController.navigate(Destinations.LoginRoute) {
                        popUpTo(Destinations.MainRoute) { inclusive = true }
                    }
                },
            )
        }
    }
}
