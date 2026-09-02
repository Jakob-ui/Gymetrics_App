package com.laschober.gymetrics.ui.auth.profile

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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laschober.gymetrics.data.auth.SessionManager

@Composable
fun ProfileScreen(viewModel: ProfileScreenViewModel = viewModel (), onLogoutClick: () -> Unit = {},) {

        val sessionManager = SessionManager()

        Column(modifier = Modifier.fillMaxSize()
            .safeContentPadding()
            .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(30.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,){
            Text(text = "Profil")
            Button(onClick = { onLogoutClick() }){Text("logout")}
        }
    }