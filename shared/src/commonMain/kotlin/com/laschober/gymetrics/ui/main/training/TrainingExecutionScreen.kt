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
import com.laschober.gymetrics.data.local.SetEntry
import com.laschober.gymetrics.data.local.TrainingDraft
import com.laschober.gymetrics.data.remote.dto.ExerciseDoneRequestDto
import com.laschober.gymetrics.data.remote.dto.SetDoneRequestDto
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
            onFinish = { plan ->
                viewModel.completeTraining(plan) {
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
    onFinish: (List<ExerciseDoneRequestDto>) -> Unit,
) {
    val entries = remember(training.id) {
        training.plan.mapIndexed { index, exercise ->
            val setCount = exercise.sets.coerceAtLeast(1)
            val saved = restoredDraft?.exercises?.getOrNull(index)
            val sets = List(setCount) { i ->
                val savedSet = saved?.sets?.getOrNull(i)
                val existingDone = exercise.setsDone.getOrNull(i)
                SetEntry(
                    weight = savedSet?.weight?.takeIf { it.isNotBlank() }
                        ?: existingDone?.weight?.toString().orEmpty(),
                    reps = savedSet?.reps?.takeIf { it.isNotBlank() }
                        ?: existingDone?.reps?.toString().orEmpty(),
                )
            }
            mutableStateOf(ExerciseEntry(sets = sets))
        }
    }

    fun persist() = onEntriesChanged(entries.map { it.value })

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 220.dp),
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
                val exercise = training.plan[index]
                ExerciseEntryCard(
                    exercise = exercise,
                    entry = entryState.value,
                    onWeightChange = { setIndex, value ->
                        entryState.value = entryState.value.copy(
                            sets = entryState.value.sets.toMutableList().apply {
                                this[setIndex] = this[setIndex].copy(weight = value)
                            },
                        )
                        persist()
                    },
                    onRepsChange = { setIndex, value ->
                        entryState.value = entryState.value.copy(
                            sets = entryState.value.sets.toMutableList().apply {
                                this[setIndex] = this[setIndex].copy(reps = value)
                            },
                        )
                        persist()
                    },
                    onFillTargets = {
                        entryState.value = ExerciseEntry(
                            sets = List(entryState.value.sets.size) {
                                SetEntry(weight = exercise.weight?.toString().orEmpty(), reps = exercise.reps.toString())
                            },
                        )
                        persist()
                    },
                )
            }
        }

        ExtendedFloatingActionButton(
            onClick = {
                if (!completing) {
                    val plan = training.plan.mapIndexed { index, exercise ->
                        val entry = entries[index].value
                        ExerciseDoneRequestDto(
                            title = exercise.title,
                            setsDone = entry.sets.mapNotNull { set ->
                                val reps = set.reps.toIntOrNull()
                                val weight = set.weight.toDoubleOrNull()
                                if (reps == null && weight == null) null
                                else SetDoneRequestDto(reps = reps ?: 0, weight = weight ?: 0.0)
                            },
                        )
                    }
                    onFinish(plan)
                }
            },
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
    onWeightChange: (setIndex: Int, value: String) -> Unit,
    onRepsChange: (setIndex: Int, value: String) -> Unit,
    onFillTargets: () -> Unit,
) {
    Card(
        shape = cardShape,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Target: ${exercise.weight ?: 0} kg × ${exercise.reps} reps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            SetTableHeaderRow()
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            entry.sets.forEachIndexed { setIndex, set ->
                if (setIndex > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SetEntryRow(
                    setNumber = setIndex + 1,
                    weight = set.weight,
                    reps = set.reps,
                    onWeightChange = { onWeightChange(setIndex, it) },
                    onRepsChange = { onRepsChange(setIndex, it) },
                    repsImeAction = if (setIndex == entry.sets.lastIndex) ImeAction.Done else ImeAction.Next,
                )
            }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onFillTargets, modifier = Modifier.fillMaxWidth()) {
                Text("Fill with target values")
            }
        }
    }
}

@Composable
private fun SetTableHeaderRow() {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 6.dp)) {
        Spacer(Modifier.weight(1f))
        Text(
            text = "WEIGHT",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "REPS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SetEntryRow(
    setNumber: Int,
    weight: String,
    reps: String,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    repsImeAction: ImeAction,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Set $setNumber",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = weight,
            onValueChange = onWeightChange,
            singleLine = true,
            shape = cellShape,
            placeholder = { Text("–", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
        )
        OutlinedTextField(
            value = reps,
            onValueChange = onRepsChange,
            singleLine = true,
            shape = cellShape,
            placeholder = { Text("–", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = repsImeAction),
            modifier = Modifier.weight(1f).padding(start = 4.dp),
        )
    }
}
