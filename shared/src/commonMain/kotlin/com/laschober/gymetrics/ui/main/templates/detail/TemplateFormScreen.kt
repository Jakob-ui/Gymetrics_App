package com.laschober.gymetrics.ui.main.templates.detail

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private val fieldShape = RoundedCornerShape(20.dp)
private val cardShape = RoundedCornerShape(20.dp)

@Composable
fun TemplateFormScreen(
    id: String?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: TemplateFormScreenViewModel = koinViewModel(),
) {
    LaunchedEffect(id) { viewModel.load(id) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val s = viewModel.state) {
            TemplateFormState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            is TemplateFormState.LoadError -> Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(s.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onBack) { Text("Back") }
            }

            is TemplateFormState.Editing -> {
                TemplateForm(
                    state = s,
                    onTitleChange = viewModel::updateTitle,
                    onDescriptionChange = viewModel::updateDescription,
                    onExerciseTitleChange = viewModel::updateExerciseTitle,
                    onExerciseRepsChange = viewModel::updateExerciseReps,
                    onExerciseWeightChange = viewModel::updateExerciseWeight,
                    onExerciseRemove = viewModel::removeExercise,
                    onExerciseMove = viewModel::moveExercise,
                    onAddExercise = viewModel::addExercise,
                    onSave = { viewModel.save(onSaved) },
                    onDelete = { viewModel.delete(onSaved) },
                )
            }
        }
    }
}

@Composable
private fun BoxScope.TemplateForm(
    state: TemplateFormState.Editing,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onExerciseTitleChange: (String, String) -> Unit,
    onExerciseRepsChange: (String, String) -> Unit,
    onExerciseWeightChange: (String, String) -> Unit,
    onExerciseRemove: (String) -> Unit,
    onExerciseMove: (Int, Int) -> Unit,
    onAddExercise: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    val lazyListState = rememberLazyListState()
    // 1 header item (title/description fields + "Exercises" label) comes before the exercises
    // in the list below, so the exercise indices need shifting by that offset - see the
    // Reorderable docs' note on "Section Headers and Footers".
    val headerItemCount = 1
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onExerciseMove(from.index - headerItemCount, to.index - headerItemCount)
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = onTitleChange,
                    label = { Text("Title") },
                    singleLine = true,
                    shape = fieldShape,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description") },
                    minLines = 2,
                    shape = fieldShape,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.error != null) {
                    Text(state.error, color = MaterialTheme.colorScheme.error)
                }
                Text("Exercises", style = MaterialTheme.typography.titleMedium)
            }
        }

        items(state.exercises, key = { it.localId }) { exercise ->
            ReorderableItem(reorderableState, key = exercise.localId) { isDragging ->
                val elevation by animateDpAsState(if (isDragging) 6.dp else 0.dp)
                Surface(shadowElevation = elevation, shape = cardShape) {
                    ExerciseFormCard(
                        scope = this@ReorderableItem,
                        exercise = exercise,
                        onTitleChange = { onExerciseTitleChange(exercise.localId, it) },
                        onRepsChange = { onExerciseRepsChange(exercise.localId, it) },
                        onWeightChange = { onExerciseWeightChange(exercise.localId, it) },
                        onRemove = { onExerciseRemove(exercise.localId) },
                    )
                }
            }
        }

        item {
            OutlinedButton(onClick = onAddExercise, shape = fieldShape, modifier = Modifier.fillMaxWidth()) {
                Text("+ Add Exercise")
            }
        }
    }

    ExtendedFloatingActionButton(
        onClick = onSave,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp).padding(bottom = 110.dp),
    ) {
        Text(if (state.saving) "Saving…" else "Save")
    }

    if (!state.isNew) {
        ExtendedFloatingActionButton(
            onClick = { showDeleteConfirm = true },
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp).padding(bottom = 110.dp),
        ) {
            Text("Delete")
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete template?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ExerciseFormCard(
    scope: ReorderableCollectionItemScope,
    exercise: ExerciseFormItem,
    onTitleChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    Card(shape = cardShape, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = exercise.title,
                    onValueChange = onTitleChange,
                    label = { Text("Exercise") },
                    singleLine = true,
                    shape = fieldShape,
                    modifier = Modifier.weight(1f),
                )
                // Only this handle is draggable, not the whole card - so text fields stay
                // tappable/editable instead of triggering a drag.
                Box(
                    modifier = with(scope) {
                        Modifier.size(40.dp).draggableHandle()
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("≡", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = exercise.weight,
                    onValueChange = onWeightChange,
                    label = { Text("Weight (kg)") },
                    singleLine = true,
                    shape = fieldShape,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = exercise.reps,
                    onValueChange = onRepsChange,
                    label = { Text("Reps") },
                    singleLine = true,
                    shape = fieldShape,
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedButton(
                onClick = onRemove,
                shape = fieldShape,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Delete")
            }
        }
    }
}
