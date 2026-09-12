package com.laschober.gymetrics.data.local

import kotlinx.serialization.Serializable

// A single exercise's in-progress entry: one "weight done" for the whole exercise (matches how
// the template defines a single target weight) and one "reps done" per set.
@Serializable
data class ExerciseEntry(
    val weightDone: String = "",
    val repsDone: List<String> = emptyList(),
)

// Autosaved locally while a training is being logged, so accidentally closing the app doesn't
// lose what was typed. Keyed by training id in TrainingDraftRepository since more than one
// training could theoretically be mid-entry at once.
@Serializable
data class TrainingDraft(
    val trainingId: String,
    val exercises: List<ExerciseEntry>,
)
