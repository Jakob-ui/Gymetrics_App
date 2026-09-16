package com.laschober.gymetrics.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ExerciseDoneRequestDto(
    val title: String,
    val repsDone: Int? = null,
    val weightDone: Double? = null,
)

@Serializable
data class TrainingDoneRequestDto(
    val active: Boolean,
    val plan: List<ExerciseDoneRequestDto>,
)
