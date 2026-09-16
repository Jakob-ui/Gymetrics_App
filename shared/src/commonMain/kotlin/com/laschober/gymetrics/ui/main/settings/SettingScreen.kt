package com.laschober.gymetrics.ui.main.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.laschober.gymetrics.data.local.ThemeMode
import com.laschober.gymetrics.ui.main.profile.StudioPickerDialog
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import org.koin.compose.viewmodel.koinViewModel

private val fieldShape = RoundedCornerShape(20.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    viewModel: SettingScreenViewModel = koinViewModel(),
    onBack: () -> Unit = {},
) {
    val themeMode by viewModel.themeMode.collectAsState()
    var showStudioPicker by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(FeatherIcons.ArrowLeft, contentDescription = "Back")
                }
                Spacer(Modifier.width(4.dp))
                Text("Settings", style = MaterialTheme.typography.titleMedium)
            }

            HorizontalDivider()

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Appearance", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Device colors matches your wallpaper's Material You palette " +
                            "(Android 12+); falls back to Light/Dark on other devices.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val options = listOf(
                            ThemeMode.LIGHT to "Light",
                            ThemeMode.DARK to "Dark",
                            ThemeMode.DEVICE to "Device colors",
                        )
                        options.forEachIndexed { index, (mode, label) ->
                            SegmentedButton(
                                selected = themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
            }

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Studio", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Lets AI-generated templates take your gym's equipment into account.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = { showStudioPicker = true },
                        enabled = !viewModel.studioSaving,
                        shape = fieldShape,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            Text(viewModel.profile?.activeStudio?.ifBlank { "Choose a studio" } ?: "Choose a studio")
                        }
                        if (viewModel.studioSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        }
                    }
                    viewModel.studioError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    if (showStudioPicker) {
        StudioPickerDialog(
            initialStudio = viewModel.profile?.activeStudio.orEmpty(),
            onDismiss = { showStudioPicker = false },
            onConfirm = {
                viewModel.setStudio(it)
                showStudioPicker = false
            },
        )
    }
}
