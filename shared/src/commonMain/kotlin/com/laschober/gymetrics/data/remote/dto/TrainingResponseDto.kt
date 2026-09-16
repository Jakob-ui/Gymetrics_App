package com.laschober.gymetrics.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TrainingExerciseDto(
    val title: String,
    val reps: Int,
    val sets: Int = 0,
    val repsDone: Int? = null,
    val weight: Double? = null,
    val weightDone: Double? = null,
    val factor: Double? = null,
)

@Serializable
data class TrainingResponseDto(
    @SerialName("_id") val id: String,
    val templateId: String,
    val title: String,
    val description: String = "",
    val active: Boolean = false,
    val activeDate: String = "",
    val icon: String = "",
    @SerialName("_createdAt") val createdAt: String = "",
    @SerialName("_updatedAt") val updatedAt: String = "",
    val plan: List<TrainingExerciseDto> = emptyList(),
)
