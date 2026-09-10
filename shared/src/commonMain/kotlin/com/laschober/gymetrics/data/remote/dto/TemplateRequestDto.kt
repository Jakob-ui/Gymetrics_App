package com.laschober.gymetrics.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TemplateRequestDto(
    val title: String,
    val description: String? = null,
    val status: Boolean? = null,
    val icon: String? = null,
    val plan: List<TemplateExerciseDto>,
)
