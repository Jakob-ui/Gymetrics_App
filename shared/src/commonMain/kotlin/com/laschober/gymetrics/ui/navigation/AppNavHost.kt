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
import com.laschober.gymetrics.ui.home.HomeScreen
import com.laschober.gymetrics.ui.serverconnection.ServerConnectionScreen

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Destinations.ServerConnectionRoute,
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
                onRegisterClick = {navController.navigate(Destinations.RegisterRoute)},
                onLoginSuccess = {navController.navigate(Destinations.HomeRoute) {
                    popUpTo(Destinations.ServerConnectionRoute) { inclusive = true }
                }}
            )
        }
        composable<Destinations.RegisterRoute> {
            RegisterScreen(
                onLoginClick = {navController.navigate(Destinations.LoginRoute)},
                onRegisterSuccess = {navController.navigate(Destinations.HomeRoute) {
                    popUpTo(Destinations.ServerConnectionRoute) { inclusive = true }
                }}
            )
        }
        composable<Destinations.HomeRoute> {
            HomeScreen()
        }
    }
}