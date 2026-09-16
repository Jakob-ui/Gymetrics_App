package com.laschober.gymetrics.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TemplateOverviewResponseDto(
    val id: String,
    val title: String,
    val description: String = "",
    val status: Boolean = false,
    val icon: String = "",
    val isAiGenerated: Boolean = false,
    val generationStatus: GenerationStatus = GenerationStatus.READY,
    @SerialName("created_date") val createdDate: String = "",
    @SerialName("updated_date") val updatedDate: String = "",
)
