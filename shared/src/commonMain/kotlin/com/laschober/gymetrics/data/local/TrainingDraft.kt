package com.laschober.gymetrics.data.local

import kotlinx.serialization.Serializable

@Serializable
data class ExerciseEntry(
    val weightDone: String = "",
    val repsDone: List<String> = emptyList(),
)

@Serializable
data class TrainingDraft(
    val trainingId: String,
    val exercises: List<ExerciseEntry>,
)
