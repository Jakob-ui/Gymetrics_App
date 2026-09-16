package com.laschober.gymetrics.ui.main.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.laschober.gymetrics.data.repositories.StudioRepository
import compose.icons.FeatherIcons
import compose.icons.feathericons.Check
import compose.icons.feathericons.MapPin
import compose.icons.feathericons.Search
import compose.icons.feathericons.X
import org.koin.compose.koinInject

private val fieldShape = RoundedCornerShape(16.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioPickerDialog(
    initialStudio: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    repository: StudioRepository = koinInject(),
) {
    var countries by remember { mutableStateOf<List<String>>(emptyList()) }
    var cities by remember { mutableStateOf<List<String>>(emptyList()) }
    var studios by remember { mutableStateOf<List<String>>(emptyList()) }
    var studioQuery by remember { mutableStateOf("") }

    var selectedCountry by remember { mutableStateOf<String?>(null) }
    var selectedCity by remember { mutableStateOf<String?>(null) }
    var selectedStudio by remember { mutableStateOf(initialStudio.ifBlank { null }) }

    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        error = null
        try {
            countries = repository.getCountries()
        } catch (e: Exception) {
            error = e.message ?: "Couldn't load countries"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(selectedCountry) {
        cities = emptyList()
        selectedCity = null
        studios = emptyList()
        studioQuery = ""
        val country = selectedCountry ?: return@LaunchedEffect
        loading = true
        error = null
        try {
            cities = repository.getCities(country)
        } catch (e: Exception) {
            error = e.message ?: "Couldn't load cities"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(selectedCity) {
        studios = emptyList()
        studioQuery = ""
        val country = selectedCountry
        val city = selectedCity
        if (country == null || city == null) return@LaunchedEffect
        loading = true
        error = null
        try {
            studios = repository.getStudios(country, city)
        } catch (e: Exception) {
            error = e.message ?: "Couldn't load studios"
        } finally {
            loading = false
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(max = 560.dp),
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Choose your studio", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "So AI-generated templates know what equipment you have.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (selectedCountry != null || selectedCity != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        selectedCountry?.let { country ->
                            StepChip(text = country, onClear = { selectedCountry = null })
                        }
                        selectedCity?.let { city ->
                            StepChip(text = city, onClear = { selectedCity = null })
                        }
                    }
                }

                if (selectedCountry == null) {
                    StudioDropdown(
                        label = "Country",
                        options = countries,
                        selected = null,
                        onSelect = { selectedCountry = it },
                    )
                } else if (selectedCity == null) {
                    StudioDropdown(
                        label = "City",
                        options = cities,
                        selected = null,
                        onSelect = { selectedCity = it },
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = studioQuery,
                            onValueChange = { studioQuery = it },
                            placeholder = { Text("Search studios") },
                            leadingIcon = { Icon(FeatherIcons.Search, contentDescription = null) },
                            singleLine = true,
                            shape = fieldShape,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        val filteredStudios = remember(studios, studioQuery) {
                            studios.filter { it.contains(studioQuery, ignoreCase = true) }
                        }

                        if (filteredStudios.isEmpty() && !loading) {
                            Text(
                                text = if (studios.isEmpty()) "No studios found for this city" else "No matches",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                items(filteredStudios) { studio ->
                                    val isSelected = studio == selectedStudio
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) {
                                                    MaterialTheme.colorScheme.primaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceContainer
                                                },
                                            )
                                            .clickable { selectedStudio = studio }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                    ) {
                                        Icon(
                                            imageVector = FeatherIcons.MapPin,
                                            contentDescription = null,
                                            tint = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.padding(end = 10.dp),
                                        )
                                        Text(
                                            text = studio,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f),
                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = FeatherIcons.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.padding(vertical = 8.dp))
                }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = { selectedStudio?.let(onConfirm) },
                        enabled = selectedStudio != null,
                        modifier = Modifier.weight(1f),
                    ) { Text("Confirm") }
                }
            }
        }
    }
}

@Composable
private fun StepChip(text: String, onClear: () -> Unit) {
    AssistChip(
        onClick = onClear,
        label = { Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingIcon = { Icon(FeatherIcons.X, contentDescription = "Change", modifier = Modifier.padding(0.dp)) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudioDropdown(
    label: String,
    options: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            shape = fieldShape,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
