package com.laschober.gymetrics.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TemplateExerciseDto(
    val title: String,
    val reps: Int,
    val sets: Int = 0,
    val weight: Double,
    val factor: Double? = null,
)

@Serializable
enum class GenerationStatus {
    @SerialName("ready") READY,
    @SerialName("generating") GENERATING,
    @SerialName("failed") FAILED,
}

@Serializable
data class TemplateResponseDto(
    val id: String,
    val title: String,
    val description: String = "",
    val status: Boolean = false,
    val icon: String = "",
    val isAiGenerated: Boolean = false,
    val generationStatus: GenerationStatus = GenerationStatus.READY,
    @SerialName("created_date") val createdDate: String = "",
    @SerialName("updated_date") val updatedDate: String = "",
    val plan: List<TemplateExerciseDto> = emptyList(),
)
