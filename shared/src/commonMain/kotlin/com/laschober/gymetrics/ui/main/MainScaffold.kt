package com.laschober.gymetrics.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.laschober.gymetrics.ui.main.home.HomeScreen

enum class MainTab(val label: String) {
    Home("Home"),
    Logbook("Logbook"),
    Planning("Planning"),
    Templates("Templates"),
}

@Preview
@Composable
fun MainScaffold(onProfileClick: () -> Unit = {},) {
    var selectedTab by remember { mutableStateOf(MainTab.Home) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedTab.label) },
                actions = {
                    Button(onClick = { onProfileClick() }) { Text("lol") }
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
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == selectedTab,
                        onClick = { selectedTab = tab },
                        icon = { Text(tab.label.take(1)) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            when (selectedTab) {
                MainTab.Home -> HomeScreen()
                MainTab.Planning -> Text("Planning – kommt")
                MainTab.Logbook -> Text("Logbook – kommt")
                MainTab.Templates -> Text("Templates – kommt")
            }
        }
    }
}