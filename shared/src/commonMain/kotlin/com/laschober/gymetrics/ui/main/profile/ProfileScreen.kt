package com.laschober.gymetrics.ui.main.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.laschober.gymetrics.data.remote.dto.UserProfileDto
import compose.icons.FeatherIcons
import compose.icons.feathericons.Edit
import compose.icons.feathericons.LogOut
import compose.icons.feathericons.Settings
import compose.icons.feathericons.X
import compose.icons.feathericons.XCircle
import org.koin.compose.viewmodel.koinViewModel

private val sexOptions = listOf("Male", "Female")

@Composable
fun ProfileScreen(
    viewModel: ProfileScreenViewModel = koinViewModel(),
    onLogoutClick: () -> Unit,
    onBack: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    when (val state = viewModel.state) {
        ProfileState.Loading -> Box(
            modifier = Modifier.fillMaxWidth().padding(48.dp),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator() }

        is ProfileState.Error -> Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(state.message)
            Button(onClick = { viewModel.load() }) { Text("Try again") }
        }

        is ProfileState.Success ->
            if (viewModel.editing) {
                ProfileEditContent(
                    form = viewModel.form,
                    saving = viewModel.saving,
                    saveError = viewModel.saveError,
                    onFormChange = viewModel::updateForm,
                    onSave = viewModel::save,
                    onCancel = viewModel::cancelEditing,
                )
            } else {
                ProfileContent(
                    profile = state.profile,
                    serverUrl = viewModel.serverUrl,
                    onClose = onBack,
                    onEdit = viewModel::startEditing,
                    onOpenSettings = onOpenSettings,
                    onLogout = {
                        viewModel.logout()
                        onLogoutClick()
                    },
                )
            }
    }
}

@Composable
private fun ProfileContent(
    profile: UserProfileDto,
    serverUrl: String,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = profile.name.take(1).uppercase().ifBlank { "?" },
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = profile.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onClose) {
                Icon(imageVector = FeatherIcons.X, contentDescription = "Close")
            }
        }

        HorizontalDivider()

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            InfoRow("Biological sex", profile.gender)
            InfoRow("Height", profile.height, unit = "cm")
            InfoRow("Weight", profile.weight, unit = "kg")
            InfoRow("Muscle mass", profile.muscle, unit = "%")
        }

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Connected to",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = serverUrl.ifBlank { "—" },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            ProfileMenuButton("Settings", FeatherIcons.Settings, "Settings", onClick = onOpenSettings)
            ProfileMenuButton("Edit", FeatherIcons.Edit, "Edit",onClick = onEdit)
            ProfileMenuButton("Logout", FeatherIcons.LogOut, "LogOut",onClick = onLogout)
        }
    }
}

@Composable
private fun ProfileMenuButton(label: String, icon : ImageVector, description: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(imageVector = icon, contentDescription = description, modifier = Modifier.padding(end = 15.dp).height(18.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
            Text(label)
        }
    }
}

@Composable
private fun ProfileEditContent(
    form: ProfileForm,
    saving: Boolean,
    saveError: String?,
    onFormChange: (ProfileForm) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Edit profile", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onCancel, enabled = !saving) { Text("✕") }
        }

        FormField("Name", form.name) { onFormChange(form.copy(name = it)) }

        SexField(form.gender) { onFormChange(form.copy(gender = it)) }

        FormField("Height", form.height, numeric = true, suffixText = "cm") { onFormChange(form.copy(height = it)) }
        FormField("Weight", form.weight, numeric = true, suffixText = "kg") { onFormChange(form.copy(weight = it)) }
        FormField("Muscle mass", form.muscle, numeric = true, suffixText = "%") { onFormChange(form.copy(muscle = it)) }

        if (saveError != null) {
            Text(saveError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onCancel,
                enabled = !saving,
                modifier = Modifier.weight(1f),
            ) { Text("Cancel") }

            Button(
                onClick = onSave,
                enabled = !saving,
                modifier = Modifier.weight(1f),
            ) {
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun SexField(value: String, onValueChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Biological sex",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            sexOptions.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = value == option,
                    onClick = { onValueChange(option) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = sexOptions.size),
                ) { Text(option) }
            }
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    numeric: Boolean = false,
    suffixText: String? = null,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = if (numeric) {
            KeyboardOptions(keyboardType = KeyboardType.Number)
        } else {
            KeyboardOptions.Default
        },
        suffix = if (suffixText != null) {
            { Text(suffixText) }
        } else {
            null
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun InfoRow(label: String, value: String, unit: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = when {
                value.isBlank() -> "—"
                unit != null -> "$value $unit"
                else -> value
            },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
