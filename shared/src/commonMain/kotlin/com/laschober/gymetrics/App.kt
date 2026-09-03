package com.laschober.gymetrics

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.laschober.gymetrics.data.auth.SessionState
import com.laschober.gymetrics.di.appModule
import com.laschober.gymetrics.ui.AppViewModel
import com.laschober.gymetrics.ui.SplashScreen
import com.laschober.gymetrics.ui.navigation.AppNavHost
import com.laschober.gymetrics.ui.navigation.Destinations
import com.laschober.gymetrics.ui.theme.GymetricsTheme
import org.koin.compose.KoinApplication
import org.koin.compose.viewmodel.koinViewModel
import org.koin.dsl.koinConfiguration

@Composable
@Preview
fun App() {
    KoinApplication(configuration = koinConfiguration { modules(appModule) }) {
        GymetricsTheme {
            val appViewModel: AppViewModel = koinViewModel()
            when (appViewModel.sessionState) {
                SessionState.Checking -> SplashScreen()
                SessionState.NeedsServer -> AppNavHost(start = Destinations.ServerConnectionRoute)
                SessionState.NeedsLogin -> AppNavHost(start = Destinations.LoginRoute)
                SessionState.Authenticated -> AppNavHost(start = Destinations.MainRoute)
            }
        }
    }
}