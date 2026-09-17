package com.laschober.gymetrics.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SetDoneRequestDto(
    val reps: Int,
    val weight: Double,
)

@Serializable
data class ExerciseDoneRequestDto(
    val title: String,
    val setsDone: List<SetDoneRequestDto> = emptyList(),
)

@Serializable
data class TrainingDoneRequestDto(
    val active: Boolean,
    val plan: List<ExerciseDoneRequestDto>,
)
