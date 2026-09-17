package com.laschober.gymetrics.data.local

import kotlinx.serialization.Serializable

@Serializable
data class SetEntry(
    val weight: String = "",
    val reps: String = "",
)

@Serializable
data class ExerciseEntry(
    val sets: List<SetEntry> = emptyList(),
)

@Serializable
data class TrainingDraft(
    val trainingId: String,
    val exercises: List<ExerciseEntry>,
)
