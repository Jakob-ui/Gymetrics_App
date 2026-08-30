package com.laschober.gymetrics

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.laschober.gymetrics.ui.navigation.AppNavHost
import com.laschober.gymetrics.ui.theme.GymetricsTheme

@Composable
@Preview
fun App() {
    GymetricsTheme {
        AppNavHost()
    }
}
