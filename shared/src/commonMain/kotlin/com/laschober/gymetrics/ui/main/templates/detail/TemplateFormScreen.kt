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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Check
import compose.icons.feathericons.Menu
import compose.icons.feathericons.Plus
import compose.icons.feathericons.Trash2
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
    onShowMessage: (String) -> Unit = {},
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
                LaunchedEffect(s.error) {
                    s.error?.let { onShowMessage(it) }
                }
                TemplateForm(
                    state = s,
                    onTitleChange = viewModel::updateTitle,
                    onDescriptionChange = viewModel::updateDescription,
                    onExerciseTitleChange = viewModel::updateExerciseTitle,
                    onExerciseRepsChange = viewModel::updateExerciseReps,
                    onExerciseSetsChange = viewModel::updateExerciseSets,
                    onExerciseWeightChange = viewModel::updateExerciseWeight,
                    onExerciseRemove = viewModel::removeExercise,
                    onExerciseMove = viewModel::moveExercise,
                    onAddExercise = viewModel::addExercise,
                    onSave = { viewModel.save { onShowMessage("Template saved"); onSaved() } },
                    onDelete = { viewModel.delete { onShowMessage("Template deleted"); onSaved() } },
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
    onExerciseSetsChange: (String, String) -> Unit,
    onExerciseWeightChange: (String, String) -> Unit,
    onExerciseRemove: (String) -> Unit,
    onExerciseMove: (Int, Int) -> Unit,
    onAddExercise: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()
    val headerItemCount = 1
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onExerciseMove(from.index - headerItemCount, to.index - headerItemCount)
        haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 200.dp),
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
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                )
                OutlinedTextField(
                    value = state.description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description") },
                    minLines = 2,
                    shape = fieldShape,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                )
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
                        onSetsChange = { onExerciseSetsChange(exercise.localId, it) },
                        onWeightChange = { onExerciseWeightChange(exercise.localId, it) },
                        onRemove = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onExerciseRemove(exercise.localId) },
                    )
                }
            }
        }

        item {
            OutlinedButton(onClick = onAddExercise, shape = fieldShape, modifier = Modifier.fillMaxWidth()) {
                Icon(FeatherIcons.Plus, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add Exercise")
            }
        }
    }

    ExtendedFloatingActionButton(
        onClick = onSave,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp).padding(bottom = 110.dp),
    ) {
        Icon(FeatherIcons.Check, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(if (state.saving) "Saving…" else "Save")
    }

    if (!state.isNew) {
        ExtendedFloatingActionButton(
            onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                showDeleteConfirm = true},
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp).padding(bottom = 110.dp),
        ) {
            Icon(FeatherIcons.Trash2, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Delete")
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete template?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onDelete() }) {
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
    onSetsChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
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
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
                )
                Box(
                    modifier = with(scope) {
                        Modifier.size(40.dp).draggableHandle(
                            onDragStarted = { haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate) },
                            onDragStopped = { haptics.performHapticFeedback(HapticFeedbackType.GestureEnd) },
                        )
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = FeatherIcons.Menu,
                        contentDescription = "Drag to reorder",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                )
                OutlinedTextField(
                    value = exercise.sets,
                    onValueChange = onSetsChange,
                    label = { Text("Sets") },
                    singleLine = true,
                    shape = fieldShape,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                )
                OutlinedTextField(
                    value = exercise.reps,
                    onValueChange = onRepsChange,
                    label = { Text("Reps") },
                    singleLine = true,
                    shape = fieldShape,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                    ),
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
                Icon(FeatherIcons.Trash2, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Delete")
            }
        }
    }
}
