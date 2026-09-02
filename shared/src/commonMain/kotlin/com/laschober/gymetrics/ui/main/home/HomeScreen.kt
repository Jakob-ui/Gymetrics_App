package com.laschober.gymetrics.ui.main.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laschober.gymetrics.data.auth.SessionManager

@OptIn(ExperimentalComposeUiApi::class)
@Preview
@Composable
fun HomeScreen(viewModel: HomeScreenViewModel = viewModel()) {
    //Todo maybe remove in the future in search for a better solution
    BackHandler {}
    val sessionManager = SessionManager()

    Column(modifier = Modifier.fillMaxSize()
        .safeContentPadding()
        .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(30.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,){
        Text(text = "Home")
    }
}