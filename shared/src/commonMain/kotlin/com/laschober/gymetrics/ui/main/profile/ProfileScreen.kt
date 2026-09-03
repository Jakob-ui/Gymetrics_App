package com.laschober.gymetrics.ui.main.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileScreen(viewModel: ProfileScreenViewModel = koinViewModel (), onLogoutClick: () -> Unit = {}) {

        Column(modifier = Modifier.fillMaxSize()
            .safeContentPadding()
            .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(30.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,){
            Text(text = "Profil")
            Button(onClick = {
                viewModel.logout()
                onLogoutClick()
            }){Text("logout")}
        }
    }