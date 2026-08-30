package com.laschober.gymetrics.ui.serverconnection

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.painterResource
import gymetrics.shared.generated.resources.OnlyLogo
import gymetrics.shared.generated.resources.Res

@Preview
@Composable
fun ServerConnectionScreen(viewModel: ServerConnectionViewModel = viewModel(), onContinue: () -> Unit = {}) {
    var serverUrl by remember { mutableStateOf(viewModel.getSavedUrl()) }
    val state = viewModel.state
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(30.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(Res.drawable.OnlyLogo),
            contentDescription = "Gymetrics logo",
            modifier = Modifier.size(120.dp).padding(bottom = 20.dp)

        )
        Column (
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
            ){
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(15.dp, Alignment.CenterHorizontally),
            modifier = Modifier.height(IntrinsicSize.Min),
        ) {
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text("Server URL") },
                placeholder = { Text("e.g.: gymetrics.at")},
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { viewModel.checkConnection(serverUrl) },
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
            )

            Button(
                onClick = { viewModel.checkConnection(serverUrl) },
                modifier = Modifier
                    .width(88.dp)
                    .fillMaxHeight()
                    .padding(top = 6.dp, bottom = 1.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (state) {
                        is ServerConnectionState.Success -> MaterialTheme.colorScheme.onSecondary
                        is ServerConnectionState.Error   -> MaterialTheme.colorScheme.error
                        else                             -> MaterialTheme.colorScheme.primary
                    },
                ),
            ) {
                when (state) {
                    ServerConnectionState.Idle    -> Text("Check")
                    ServerConnectionState.Loading -> CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    is ServerConnectionState.Success -> Text("✓")
                    is ServerConnectionState.Error   -> Text("Check")
                }
            }
        }

        Button(
            onClick = {viewModel.storeServerUrl(); onContinue()},
            enabled = state is ServerConnectionState.Success,
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
            Text("Continue")
        }

        when (state) {
            ServerConnectionState.Idle -> {
                Text(
                    text = ""
                )
            }

            ServerConnectionState.Loading -> {
                Text(
                    text = ""
                )
            }

            is ServerConnectionState.Success -> {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            is ServerConnectionState.Error -> {
                Text(
                    text =  state.message,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        }
    }
}
