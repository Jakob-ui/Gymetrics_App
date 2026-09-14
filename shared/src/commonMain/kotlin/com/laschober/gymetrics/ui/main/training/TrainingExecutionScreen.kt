package com.laschober.gymetrics.ui.main.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.laschober.gymetrics.data.local.ExerciseEntry
import com.laschober.gymetrics.data.local.TrainingDraft
import com.laschober.gymetrics.data.remote.dto.TrainingExerciseDto
import com.laschober.gymetrics.data.remote.dto.TrainingResponseDto
import com.laschober.gymetrics.ui.components.Pill
import compose.icons.FeatherIcons
import compose.icons.feathericons.Check
import org.koin.compose.viewmodel.koinViewModel

private val cellShape = RoundedCornerShape(12.dp)
private val cardShape = RoundedCornerShape(20.dp)

@Composable
fun TrainingExecutionScreen(
    id: String,
    onCompleted: () -> Unit = {},
    onShowMessage: (String) -> Unit = {},
    viewModel: TrainingExecutionScreenViewModel = koinViewModel(),
) {
    LaunchedEffect(id) { viewModel.load(id) }

    LaunchedEffect(viewModel.completeError) {
        viewModel.completeError?.let {
            onShowMessage(it)
            viewModel.consumeCompleteError()
        }
    }

    when (val s = viewModel.state) {
        TrainingExecutionState.Loading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        is TrainingExecutionState.Error -> Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(s.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { viewModel.load(id) }) { Text("Try again") }
        }

        is TrainingExecutionState.Success -> TrainingExecutionForm(
            training = s.training,
            restoredDraft = viewModel.restoredDraft,
            completing = viewModel.completing,
            onEntriesChanged = { entries -> viewModel.saveDraft(TrainingDraft(s.training.id, entries)) },
            onFinish = {
                viewModel.completeTraining {
                    onShowMessage("Training completed")
                    onCompleted()
                }
            },
        )
    }
}

@Composable
private fun TrainingExecutionForm(
    training: TrainingResponseDto,
    restoredDraft: TrainingDraft?,
    completing: Boolean,
    onEntriesChanged: (List<ExerciseEntry>) -> Unit,
    onFinish: () -> Unit,
) {
    val entries = remember(training.id) {
        training.plan.mapIndexed { index, exercise ->
            val setCount = exercise.sets.coerceAtLeast(1)
            val saved = restoredDraft?.exercises?.getOrNull(index)
            mutableStateOf(
                ExerciseEntry(
                    weightDone = saved?.weightDone.orEmpty(),
                    repsDone = List(setCount) { i -> saved?.repsDone?.getOrNull(i).orEmpty() },
                ),
            )
        }
    }

    fun persist() = onEntriesChanged(entries.map { it.value })

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = training.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    if (training.description.isNotBlank()) {
                        Text(
                            text = training.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            items(training.plan.size) { index ->
                val entryState = entries[index]
                ExerciseEntryCard(
                    exercise = training.plan[index],
                    entry = entryState.value,
                    onWeightDoneChange = {
                        entryState.value = entryState.value.copy(weightDone = it)
                        persist()
                    },
                    onRepsDoneChange = { setIndex, value ->
                        entryState.value = entryState.value.copy(
                            repsDone = entryState.value.repsDone.toMutableList().apply { this[setIndex] = value },
                        )
                        persist()
                    },
                )
            }
        }

        ExtendedFloatingActionButton(
            onClick = { if (!completing) onFinish() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp).padding(bottom = 160.dp),
        ) {
            Icon(FeatherIcons.Check, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (completing) "Finishing…" else "Finish training")
        }
    }
}

@Composable
private fun ExerciseEntryCard(
    exercise: TrainingExerciseDto,
    entry: ExerciseEntry,
    onWeightDoneChange: (String) -> Unit,
    onRepsDoneChange: (setIndex: Int, value: String) -> Unit,
) {
    Card(
        shape = cardShape,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = exercise.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(8.dp))

            TableHeaderRow()
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            TableEntryRow(
                label = "Weight",
                target = "${exercise.weight ?: 0} kg",
                value = entry.weightDone,
                onValueChange = onWeightDoneChange,
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next,
            )

            entry.repsDone.forEachIndexed { setIndex, value ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                TableEntryRow(
                    label = "Set ${setIndex + 1}",
                    target = "${exercise.reps} reps",
                    value = value,
                    onValueChange = { onRepsDoneChange(setIndex, it) },
                    keyboardType = KeyboardType.Number,
                    imeAction = if (setIndex == entry.repsDone.lastIndex) ImeAction.Done else ImeAction.Next,
                )
            }
        }
    }
}

@Composable
private fun TableHeaderRow() {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 6.dp)) {
        Spacer(Modifier.weight(1f))
        Text(
            text = "TARGET",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "DONE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TableEntryRow(
    label: String,
    target: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = target,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            shape = cellShape,
            placeholder = { Text("–", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            modifier = Modifier.weight(1f).padding(start = 8.dp),
        )
    }
}
