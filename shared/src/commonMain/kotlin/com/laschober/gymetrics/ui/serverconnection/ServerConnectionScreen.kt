package com.laschober.gymetrics.ui.serverconnection

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.laschober.gymetrics.core.Config
import com.laschober.gymetrics.ui.theme.extendedColors
import compose.icons.FeatherIcons
import compose.icons.feathericons.Check
import compose.icons.feathericons.Cloud
import compose.icons.feathericons.Server
import org.jetbrains.compose.resources.painterResource
import gymetrics.shared.generated.resources.OnlyLogo
import gymetrics.shared.generated.resources.Res
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel

private enum class ServerMode { Cloud, Custom }
@Composable
fun ServerConnectionScreen(viewModel: ServerConnectionViewModel = koinViewModel(), onContinue: () -> Unit = {}) {
    val saved = remember { viewModel.getSavedUrl() }
    var mode by remember {
        mutableStateOf(
            if (saved.isEmpty() || saved == Config.OFFICIAL_SERVER_URL) ServerMode.Cloud else ServerMode.Custom
        )
    }
    var serverUrl by remember { mutableStateOf(if (mode == ServerMode.Custom) saved else "") }
    val state = viewModel.state
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(mode) {
        when (mode) {
            ServerMode.Cloud -> viewModel.checkConnection(Config.OFFICIAL_SERVER_URL)
            ServerMode.Custom -> {
                viewModel.resetCheck()
                focusRequester.requestFocus()
            }
        }
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
        ServerModeSelector(mode = mode, onModeChange = { mode = it })

        if (mode == ServerMode.Custom) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(15.dp, Alignment.CenterHorizontally),
            modifier = Modifier.height(IntrinsicSize.Min),
        ) {
            OutlinedTextField(
                value = serverUrl,
                onValueChange = {
                    serverUrl = it
                    viewModel.resetCheck()
                },
                label = { Text("Server URL") },
                placeholder = { Text("e.g.: gymetrics.at")},
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (state is ServerConnectionState.Success) {
                            viewModel.storeServerUrl()
                            onContinue()
                        } else {
                            viewModel.checkConnection(serverUrl)
                        }
                    },
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
                        is ServerConnectionState.Success -> MaterialTheme.extendedColors.success
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
                    is ServerConnectionState.Success -> Icon(imageVector =(FeatherIcons.Check), contentDescription = "checkmark")
                    is ServerConnectionState.Error   -> Text("Check")
                }
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


@Composable
private fun ServerModeSelector(mode: ServerMode, onModeChange: (ServerMode) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .selectableGroup(),
    ) {
        ModeRow(
            selected = mode == ServerMode.Cloud,
            icon = FeatherIcons.Cloud,
            onClick = { onModeChange(ServerMode.Cloud) },
            title = "Gymetrics Cloud",
            subtitle = Config.OFFICIAL_SERVER_LABEL,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        ModeRow(
            selected = mode == ServerMode.Custom,
            icon = FeatherIcons.Server,
            onClick = { onModeChange(ServerMode.Custom) },
            title = "Custom server",
            subtitle = null,
        )
    }
}

@Composable
private fun ModeRow(selected: Boolean, icon : ImageVector, onClick: () -> Unit, title: String, subtitle: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}