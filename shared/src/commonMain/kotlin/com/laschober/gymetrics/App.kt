package com.laschober.gymetrics

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laschober.gymetrics.data.auth.SessionState
import com.laschober.gymetrics.ui.AppViewModel
import com.laschober.gymetrics.ui.SplashScreen
import com.laschober.gymetrics.ui.navigation.AppNavHost
import com.laschober.gymetrics.ui.navigation.Destinations
import com.laschober.gymetrics.ui.theme.GymetricsTheme

@Composable
@Preview
fun App() {
    GymetricsTheme {
        val appViewModel: AppViewModel = viewModel()
        when (appViewModel.sessionState) {
            SessionState.Checking -> SplashScreen()
            SessionState.NeedsServer -> AppNavHost(start = Destinations.ServerConnectionRoute)
            SessionState.NeedsLogin -> AppNavHost(start = Destinations.LoginRoute)
            SessionState.Authenticated -> AppNavHost(start = Destinations.HomeRoute)
        }
    }
}
