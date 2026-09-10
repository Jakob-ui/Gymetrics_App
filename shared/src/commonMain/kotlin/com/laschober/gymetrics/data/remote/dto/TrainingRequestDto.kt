package com.laschober.gymetrics.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TrainingRequestDto(
    val templateId: String,
    val activeDate: String,
)

@Serializable
data class TrainingExerciseUpdateRequestDto(
    @SerialName("_id") val id: String,
    val repsDone: Int,
    val weightDone: Double,
)
